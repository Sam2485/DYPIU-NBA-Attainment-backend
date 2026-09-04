package com.dypiu.nba.reports.service;

public interface ReportStorageService {
    String storeReportArtifact(String reportId, String filename, byte[] content, String mimeType);
    byte[] loadReportArtifact(String fileReference);
    boolean deleteReportArtifact(String fileReference);
    String storeReportAsset(String institutionId, String filename, byte[] content, String mimeType);
    byte[] loadReportAsset(String storagePath);
}
