package com.dypiu.nba.reports.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class LocalReportStorageService implements ReportStorageService {

    private final Path baseStoragePath;
    private final Path assetsStoragePath;

    public LocalReportStorageService(@Value("${app.reports.storage.dir:./storage/reports}") String storageDir) {
        this.baseStoragePath = Paths.get(storageDir).toAbsolutePath().normalize();
        this.assetsStoragePath = this.baseStoragePath.resolve("assets").normalize();
        initDirectories();
    }

    private void initDirectories() {
        try {
            Files.createDirectories(this.baseStoragePath);
            Files.createDirectories(this.assetsStoragePath);
        } catch (IOException e) {
            log.warn("Could not create report storage directory at {}: {}", this.baseStoragePath, e.getMessage());
        }
    }

    @Override
    public String storeReportArtifact(String reportId, String filename, byte[] content, String mimeType) {
        try {
            Path reportDir = baseStoragePath.resolve(reportId).normalize();
            Files.createDirectories(reportDir);
            String safeFilename = sanitizeFilename(filename);
            Path targetFile = reportDir.resolve(safeFilename).normalize();
            Files.write(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return reportId + "/" + safeFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store report artifact: " + filename, e);
        }
    }

    @Override
    public byte[] loadReportArtifact(String fileReference) {
        try {
            Path targetFile = baseStoragePath.resolve(fileReference).normalize();
            if (!Files.exists(targetFile)) {
                throw new RuntimeException("Report artifact file not found: " + fileReference);
            }
            return Files.readAllBytes(targetFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read report artifact: " + fileReference, e);
        }
    }

    @Override
    public boolean deleteReportArtifact(String fileReference) {
        try {
            Path targetFile = baseStoragePath.resolve(fileReference).normalize();
            return Files.deleteIfExists(targetFile);
        } catch (IOException e) {
            log.warn("Failed to delete report artifact: {}", fileReference);
            return false;
        }
    }

    @Override
    public String storeReportAsset(String institutionId, String filename, byte[] content, String mimeType) {
        try {
            String inst = (institutionId != null && !institutionId.isBlank()) ? sanitizeFilename(institutionId) : "common";
            Path instDir = assetsStoragePath.resolve(inst).normalize();
            Files.createDirectories(instDir);
            String uniqueFilename = UUID.randomUUID().toString().substring(0, 8) + "_" + sanitizeFilename(filename);
            Path targetFile = instDir.resolve(uniqueFilename).normalize();
            Files.write(targetFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            return "assets/" + inst + "/" + uniqueFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store report asset: " + filename, e);
        }
    }

    @Override
    public byte[] loadReportAsset(String storagePath) {
        try {
            Path targetFile = baseStoragePath.resolve(storagePath).normalize();
            if (!Files.exists(targetFile)) {
                return null;
            }
            return Files.readAllBytes(targetFile);
        } catch (IOException e) {
            log.warn("Failed to load asset from {}: {}", storagePath, e.getMessage());
            return null;
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
