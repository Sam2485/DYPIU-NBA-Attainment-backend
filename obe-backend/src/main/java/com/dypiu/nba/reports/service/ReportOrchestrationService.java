package com.dypiu.nba.reports.service;

import com.dypiu.nba.reports.excel.ExcelReportRenderer;
import com.dypiu.nba.reports.integrity.ReportIntegrityService;
import com.dypiu.nba.reports.model.*;
import com.dypiu.nba.reports.model.snapshot.*;
import com.dypiu.nba.reports.pdf.PdfReportRenderer;
import com.dypiu.nba.reports.repository.ReportArtifactRepository;
import com.dypiu.nba.reports.repository.ReportAssetRepository;
import com.dypiu.nba.reports.repository.ReportRepository;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import com.dypiu.nba.reports.template.ReportTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportOrchestrationService {

    private final Map<String, byte[]> logoBytesCache = new ConcurrentHashMap<>();

    private final ReportSnapshotBuilder snapshotBuilder;
    private final ReportTemplateService templateService;
    private final ExcelReportRenderer excelRenderer;
    private final PdfReportRenderer pdfRenderer;
    private final ReportStorageService storageService;
    private final ReportIntegrityService integrityService;
    private final ReportRepository reportRepository;
    private final ReportArtifactRepository artifactRepository;
    private final ReportAssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public GeneratedReportDto generateProgrammeAttainmentReport(
            String masterProgrammeId,
            String programmeBatchId,
            ReportSection section,
            String generatedBy,
            String institutionId) {
        return generateProgrammeAttainmentReport(masterProgrammeId, programmeBatchId, section, generatedBy, institutionId, null);
    }

    @Transactional
    public GeneratedReportDto generateProgrammeAttainmentReport(
            String masterProgrammeId,
            String programmeBatchId,
            ReportSection section,
            String generatedBy,
            String institutionId,
            ArtifactType targetArtifactType) {

        ReportSection targetSection = (section != null) ? section : ReportSection.ALL;
        ReportType reportType = resolveProgrammeReportType(targetSection);

        ProgrammeAttainmentSnapshot snapshot = snapshotBuilder.buildProgrammeAttainmentSnapshot(
                masterProgrammeId, programmeBatchId, generatedBy, institutionId);

        String reportId = "rep-" + UUID.randomUUID().toString().substring(0, 10);
        snapshot.setReportId(reportId);
        snapshot.setReportType(reportType);

        ReportTemplateDto template = templateService.resolveTemplate(reportType, institutionId);

        boolean needExcel = targetArtifactType == null || targetArtifactType == ArtifactType.EXCEL;
        boolean needPdf = targetArtifactType == null || targetArtifactType == ArtifactType.PDF;

        byte[] leftLogo = needExcel || needPdf ? loadLogoBytes(template.getHeaderConfig() != null ? template.getHeaderConfig().getEffectiveLeftLogoAssetId() : null) : null;
        byte[] rightLogo = needPdf ? loadLogoBytes(template.getHeaderConfig() != null ? template.getHeaderConfig().getEffectiveRightLogoAssetId() : null) : null;

        byte[] excelBytes = null;
        byte[] pdfBytes = null;
        String baseFilename;

        String progCode = snapshot.getMasterProgrammeCode() != null && !snapshot.getMasterProgrammeCode().isBlank()
                ? snapshot.getMasterProgrammeCode() : "PROG";
        String batchYears = snapshot.getAcademicBatchYears() != null
                ? snapshot.getAcademicBatchYears().replace("–", "-") : "BATCH";

        if (targetSection == ReportSection.ALL) {
            if (needExcel) excelBytes = excelRenderer.renderProgrammeAttainmentMaster(snapshot, leftLogo, template);
            if (needPdf) pdfBytes = pdfRenderer.renderProgrammeAttainmentMaster(snapshot, template, leftLogo, rightLogo);
            baseFilename = "PROGRAMME_ATTAINMENT_MASTER_" + progCode + "_" + batchYears;
        } else {
            if (needExcel) excelBytes = excelRenderer.renderProgrammeAttainmentSection(snapshot, targetSection, leftLogo, template);
            if (needPdf) pdfBytes = pdfRenderer.renderProgrammeAttainmentSection(snapshot, targetSection, template, leftLogo, rightLogo);
            baseFilename = "PROGRAMME_ATTAINMENT_" + targetSection.name() + "_" + progCode + "_" + batchYears;
        }

        return persistAndBuildReport(
                reportId,
                reportType,
                snapshot.getInstitutionId(),
                snapshot.getMasterProgrammeId(),
                snapshot.getProgrammeBatchId(),
                null,
                null,
                template.getId(),
                template.getTemplateVersion(),
                generatedBy,
                snapshot,
                baseFilename,
                excelBytes,
                pdfBytes
        );
    }

    @Transactional
    public GeneratedReportDto generateCourseAttainmentReport(
            String programmeBatchCourseId,
            String generatedBy,
            String institutionId) {
        return generateCourseAttainmentReport(programmeBatchCourseId, generatedBy, institutionId, null);
    }

    @Transactional
    public GeneratedReportDto generateCourseAttainmentReport(
            String programmeBatchCourseId,
            String generatedBy,
            String institutionId,
            ArtifactType targetArtifactType) {

        CourseAttainmentSnapshot snapshot = snapshotBuilder.buildCourseAttainmentSnapshot(
                programmeBatchCourseId, generatedBy, institutionId);

        String reportId = "rep-" + UUID.randomUUID().toString().substring(0, 10);
        snapshot.setReportId(reportId);
        snapshot.setReportType(ReportType.COURSE_ATTAINMENT);

        ReportTemplateDto template = templateService.resolveTemplate(ReportType.COURSE_ATTAINMENT, institutionId);

        boolean needExcel = targetArtifactType == null || targetArtifactType == ArtifactType.EXCEL;
        boolean needPdf = targetArtifactType == null || targetArtifactType == ArtifactType.PDF;

        byte[] leftLogo = needExcel || needPdf ? loadLogoBytes(template.getHeaderConfig() != null ? template.getHeaderConfig().getEffectiveLeftLogoAssetId() : null) : null;
        byte[] rightLogo = needExcel || needPdf ? loadLogoBytes(template.getHeaderConfig() != null ? template.getHeaderConfig().getEffectiveRightLogoAssetId() : null) : null;

        byte[] excelBytes = needExcel ? excelRenderer.renderCourseAttainment(snapshot, leftLogo, rightLogo) : null;
        byte[] pdfBytes = needPdf ? pdfRenderer.renderCourseAttainment(snapshot, template, leftLogo, rightLogo) : null;

        String courseCode = snapshot.getCourseCode() != null ? snapshot.getCourseCode() : "COURSE";
        String baseFilename = "COURSE_ATTAINMENT_" + courseCode + "_SEM" + (snapshot.getSemester() != null ? snapshot.getSemester() : "X");

        return persistAndBuildReport(
                reportId,
                ReportType.COURSE_ATTAINMENT,
                snapshot.getInstitutionId(),
                null,
                snapshot.getProgrammeBatchId(),
                snapshot.getProgrammeBatchCourseId(),
                snapshot.getMasterCourseId(),
                template.getId(),
                template.getTemplateVersion(),
                generatedBy,
                snapshot,
                baseFilename,
                excelBytes,
                pdfBytes
        );
    }

    @Transactional
    public GeneratedReportDto generateProgrammeAtrReport(
            String programmeBatchId,
            String generatedBy,
            String institutionId) {
        return generateProgrammeAtrReport(programmeBatchId, generatedBy, institutionId, null);
    }

    @Transactional
    public GeneratedReportDto generateProgrammeAtrReport(
            String programmeBatchId,
            String generatedBy,
            String institutionId,
            ArtifactType targetArtifactType) {

        ProgrammeAtrSnapshot snapshot = snapshotBuilder.buildProgrammeAtrSnapshot(
                null, programmeBatchId, generatedBy, institutionId);

        String reportId = "rep-" + UUID.randomUUID().toString().substring(0, 10);
        snapshot.setReportId(reportId);
        snapshot.setReportType(ReportType.PROGRAMME_ATR);

        ReportTemplateDto template = templateService.resolveTemplate(ReportType.PROGRAMME_ATR, institutionId);

        boolean needExcel = targetArtifactType == null || targetArtifactType == ArtifactType.EXCEL;
        boolean needPdf = targetArtifactType == null || targetArtifactType == ArtifactType.PDF;

        byte[] leftLogo = needExcel || needPdf ? loadLogoBytes(template.getHeaderConfig() != null ? template.getHeaderConfig().getEffectiveLeftLogoAssetId() : null) : null;
        byte[] rightLogo = needPdf ? loadLogoBytes(template.getHeaderConfig() != null ? template.getHeaderConfig().getEffectiveRightLogoAssetId() : null) : null;

        byte[] excelBytes = needExcel ? excelRenderer.renderProgrammeAtr(snapshot) : null;
        byte[] pdfBytes = needPdf ? pdfRenderer.renderProgrammeAtr(snapshot, template, leftLogo, rightLogo) : null;

        String progCode = snapshot.getMasterProgrammeCode() != null ? snapshot.getMasterProgrammeCode() : "PROG";
        String baseFilename = "PROGRAMME_ATR_" + progCode + "_" + (snapshot.getBatchName() != null ? snapshot.getBatchName().replace(" ", "_") : "BATCH");

        return persistAndBuildReport(
                reportId,
                ReportType.PROGRAMME_ATR,
                snapshot.getInstitutionId(),
                snapshot.getMasterProgrammeId(),
                snapshot.getProgrammeBatchId(),
                null,
                null,
                template.getId(),
                template.getTemplateVersion(),
                generatedBy,
                snapshot,
                baseFilename,
                excelBytes,
                pdfBytes
        );
    }

    @Transactional
    public GeneratedReportDto generateCourseAtrReport(
            String programmeBatchCourseId,
            String generatedBy,
            String institutionId) {
        return generateCourseAtrReport(programmeBatchCourseId, generatedBy, institutionId, null);
    }

    @Transactional
    public GeneratedReportDto generateCourseAtrReport(
            String programmeBatchCourseId,
            String generatedBy,
            String institutionId,
            ArtifactType targetArtifactType) {

        CourseAtrSnapshot snapshot = snapshotBuilder.buildCourseAtrSnapshot(
                programmeBatchCourseId, generatedBy, institutionId);

        String reportId = "rep-" + UUID.randomUUID().toString().substring(0, 10);
        snapshot.setReportId(reportId);
        snapshot.setReportType(ReportType.COURSE_ATR);

        ReportTemplateDto template = templateService.resolveTemplate(ReportType.COURSE_ATR, institutionId);

        boolean needExcel = targetArtifactType == null || targetArtifactType == ArtifactType.EXCEL;
        boolean needPdf = targetArtifactType == null || targetArtifactType == ArtifactType.PDF;

        byte[] leftLogo = needExcel || needPdf ? loadLogoBytes(template.getHeaderConfig() != null ? template.getHeaderConfig().getEffectiveLeftLogoAssetId() : null) : null;
        byte[] rightLogo = needPdf ? loadLogoBytes(template.getHeaderConfig() != null ? template.getHeaderConfig().getEffectiveRightLogoAssetId() : null) : null;

        byte[] excelBytes = needExcel ? excelRenderer.renderCourseAtr(snapshot, leftLogo, rightLogo) : null;
        byte[] pdfBytes = needPdf ? pdfRenderer.renderCourseAtr(snapshot, template, leftLogo, rightLogo) : null;

        String courseCode = snapshot.getCourseCode() != null ? snapshot.getCourseCode() : "COURSE";
        String baseFilename = "COURSE_ATR_" + courseCode + "_SEM" + (snapshot.getSemester() != null ? snapshot.getSemester() : "X");

        return persistAndBuildReport(
                reportId,
                ReportType.COURSE_ATR,
                snapshot.getInstitutionId(),
                null,
                snapshot.getProgrammeBatchId(),
                snapshot.getProgrammeBatchCourseId(),
                snapshot.getMasterCourseId(),
                template.getId(),
                template.getTemplateVersion(),
                generatedBy,
                snapshot,
                baseFilename,
                excelBytes,
                pdfBytes
        );
    }

    public byte[] loadArtifactContent(String artifactId) {
        ReportArtifactEntity artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));
        return storageService.loadReportArtifact(artifact.getFileReference());
    }

    public VerificationResponseDto verifyReportArtifact(String reportId, ArtifactType artifactType, byte[] uploadedBytes) {
        if (reportId == null || artifactType == null || uploadedBytes == null) {
            return VerificationResponseDto.builder()
                    .reportId(reportId)
                    .artifactType(artifactType)
                    .status("INVALID_INPUT")
                    .isValid(false)
                    .verifiedAt(ZonedDateTime.now())
                    .message("Report ID, Artifact Type, and Uploaded file bytes are required for verification.")
                    .build();
        }

        ReportArtifactEntity artifact = artifactRepository.findByReportIdAndArtifactType(reportId, artifactType).orElse(null);
        if (artifact == null) {
            return VerificationResponseDto.builder()
                    .reportId(reportId)
                    .artifactType(artifactType)
                    .status("REPORT_NOT_FOUND")
                    .isValid(false)
                    .verifiedAt(ZonedDateTime.now())
                    .message("No matching report artifact found for Report ID: " + reportId + " and Type: " + artifactType)
                    .build();
        }

        String calculatedSha = integrityService.calculateSha256(uploadedBytes);
        String calculatedHmac = integrityService.calculateHmac(uploadedBytes);

        ReportIntegrityService.VerificationResult result = integrityService.verifyArtifact(
                uploadedBytes, artifact.getSha256Checksum(), artifact.getHmacSignature());

        boolean valid = result == ReportIntegrityService.VerificationResult.VALID;
        String status = result.name();
        String message = valid
                ? "Cryptographic Verification SUCCESS: Document is Authentic and Untampered (SHA-256 and HMAC match authoritative record)."
                : "Cryptographic Verification FAILED: Document content has been MODIFIED or TAMPERED with (Mismatch detected).";

        return VerificationResponseDto.builder()
                .reportId(reportId)
                .artifactId(artifact.getId())
                .artifactType(artifactType)
                .status(status)
                .isValid(valid)
                .expectedSha256(artifact.getSha256Checksum())
                .calculatedSha256(calculatedSha)
                .expectedHmac(artifact.getHmacSignature())
                .calculatedHmac(calculatedHmac)
                .fileSize((long) uploadedBytes.length)
                .verifiedAt(ZonedDateTime.now())
                .message(message)
                .build();
    }

    private byte[] loadLogoBytes(String assetId) {
        if (assetId == null || assetId.isBlank()) return null;
        return logoBytesCache.computeIfAbsent(assetId, id -> {
            try {
                ReportAssetEntity asset = assetRepository.findById(id).orElse(null);
                if (asset != null && asset.getStoragePath() != null) {
                    return storageService.loadReportArtifact(asset.getStoragePath());
                }
            } catch (Exception e) {
                log.warn("Failed to load logo asset {}: {}", id, e.getMessage());
            }
            return null;
        });
    }

    private GeneratedReportDto persistAndBuildReport(
            String reportId,
            ReportType reportType,
            String institutionId,
            String masterProgrammeId,
            String programmeBatchId,
            String programmeBatchCourseId,
            String masterCourseId,
            String templateId,
            Integer templateVersion,
            String generatedBy,
            ReportSnapshot snapshot,
            String baseFilename,
            byte[] excelBytes,
            byte[] pdfBytes) {

        String snapshotJson = null;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn("Could not serialize snapshot for report {}: {}", reportId, e.getMessage());
        }

        ZonedDateTime now = ZonedDateTime.now();

        ReportEntity reportEntity = ReportEntity.builder()
                .id(reportId)
                .reportType(reportType)
                .institutionId(institutionId)
                .masterProgrammeId(masterProgrammeId)
                .programmeBatchId(programmeBatchId)
                .programmeBatchCourseId(programmeBatchCourseId)
                .masterCourseId(masterCourseId)
                .templateId(templateId)
                .templateVersion(templateVersion)
                .generatedBy(generatedBy != null ? generatedBy : "System")
                .generatedAt(now)
                .snapshotJson(snapshotJson)
                .status("GENERATED")
                .artifacts(new ArrayList<>())
                .build();

        List<GeneratedReportDto.ArtifactSummaryDto> artifactSummaries = new ArrayList<>();

        // 1. PDF Artifact
        if (pdfBytes != null) {
            String pdfFilename = baseFilename + ".pdf";
            String pdfSha = integrityService.calculateSha256(pdfBytes);
            String pdfHmac = integrityService.calculateHmac(pdfBytes);
            String pdfRef = storageService.storeReportArtifact(reportId, pdfFilename, pdfBytes, "application/pdf");

            ReportArtifactEntity pdfArtifact = ReportArtifactEntity.builder()
                    .id("art-pdf-" + UUID.randomUUID().toString().substring(0, 8))
                    .report(reportEntity)
                    .artifactType(ArtifactType.PDF)
                    .fileReference(pdfRef)
                    .originalFilename(pdfFilename)
                    .mimeType("application/pdf")
                    .fileSize((long) pdfBytes.length)
                    .sha256Checksum(pdfSha)
                    .hmacSignature(pdfHmac)
                    .generatedAt(now)
                    .build();
            reportEntity.getArtifacts().add(pdfArtifact);
            artifactSummaries.add(toArtifactSummary(pdfArtifact));
        }

        // 2. Excel Artifact
        if (excelBytes != null) {
            String xlsxFilename = baseFilename + ".xlsx";
            String xlsxSha = integrityService.calculateSha256(excelBytes);
            String xlsxHmac = integrityService.calculateHmac(excelBytes);
            String xlsxRef = storageService.storeReportArtifact(reportId, xlsxFilename, excelBytes, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            ReportArtifactEntity xlsxArtifact = ReportArtifactEntity.builder()
                    .id("art-xls-" + UUID.randomUUID().toString().substring(0, 8))
                    .report(reportEntity)
                    .artifactType(ArtifactType.EXCEL)
                    .fileReference(xlsxRef)
                    .originalFilename(xlsxFilename)
                    .mimeType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .fileSize((long) excelBytes.length)
                    .sha256Checksum(xlsxSha)
                    .hmacSignature(xlsxHmac)
                    .generatedAt(now)
                    .build();
            reportEntity.getArtifacts().add(xlsxArtifact);
            artifactSummaries.add(toArtifactSummary(xlsxArtifact));
        }

        reportRepository.save(reportEntity);

        return GeneratedReportDto.builder()
                .reportId(reportId)
                .reportType(reportType)
                .institutionId(institutionId)
                .masterProgrammeId(masterProgrammeId)
                .programmeBatchId(programmeBatchId)
                .programmeBatchCourseId(programmeBatchCourseId)
                .masterCourseId(masterCourseId)
                .templateId(templateId)
                .templateVersion(templateVersion)
                .generatedBy(reportEntity.getGeneratedBy())
                .generatedAt(now)
                .status("GENERATED")
                .snapshot(snapshot)
                .artifacts(artifactSummaries)
                .build();
    }

    @Transactional(readOnly = true)
    public List<GeneratedReportDto> listGeneratedReports(
            ReportType reportType,
            String masterProgrammeId,
            String programmeBatchId,
            String programmeBatchCourseId,
            String masterCourseId,
            String institutionId) {

        List<ReportEntity> list = reportRepository.findAllByOrderByGeneratedAtDesc();

        return list.stream()
                .filter(r -> institutionId == null || institutionId.isBlank() || institutionId.equalsIgnoreCase(r.getInstitutionId()))
                .filter(r -> reportType == null || r.getReportType() == reportType)
                .filter(r -> masterProgrammeId == null || masterProgrammeId.isBlank() || masterProgrammeId.equalsIgnoreCase(r.getMasterProgrammeId()))
                .filter(r -> programmeBatchId == null || programmeBatchId.isBlank() || programmeBatchId.equalsIgnoreCase(r.getProgrammeBatchId()))
                .filter(r -> programmeBatchCourseId == null || programmeBatchCourseId.isBlank() || programmeBatchCourseId.equalsIgnoreCase(r.getProgrammeBatchCourseId()))
                .filter(r -> masterCourseId == null || masterCourseId.isBlank() || masterCourseId.equalsIgnoreCase(r.getMasterCourseId()))
                .map(this::mapToGeneratedReportDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public GeneratedReportDto getGeneratedReport(String reportId) {
        ReportEntity entity = reportRepository.findById(reportId)
                .orElseThrow(() -> new com.dypiu.nba.exception.ResourceNotFoundException("Report not found: " + reportId));
        return mapToGeneratedReportDto(entity);
    }

    public GeneratedReportDto mapToGeneratedReportDto(ReportEntity entity) {
        if (entity == null) return null;

        List<GeneratedReportDto.ArtifactSummaryDto> artifactSummaries = new ArrayList<>();
        if (entity.getArtifacts() != null && !entity.getArtifacts().isEmpty()) {
            artifactSummaries = entity.getArtifacts().stream()
                    .map(this::toArtifactSummary)
                    .toList();
        } else {
            List<ReportArtifactEntity> byReportId = artifactRepository.findByReportId(entity.getId());
            if (byReportId != null) {
                artifactSummaries = byReportId.stream()
                        .map(this::toArtifactSummary)
                        .toList();
            }
        }

        ReportSnapshot snapshot = null;
        if (entity.getSnapshotJson() != null && !entity.getSnapshotJson().isBlank() && entity.getReportType() != null) {
            try {
                snapshot = switch (entity.getReportType()) {
                    case COURSE_ATR -> objectMapper.readValue(entity.getSnapshotJson(), CourseAtrSnapshot.class);
                    case PROGRAMME_ATR -> objectMapper.readValue(entity.getSnapshotJson(), ProgrammeAtrSnapshot.class);
                    case COURSE_ATTAINMENT -> objectMapper.readValue(entity.getSnapshotJson(), CourseAttainmentSnapshot.class);
                    default -> objectMapper.readValue(entity.getSnapshotJson(), ProgrammeAttainmentSnapshot.class);
                };
            } catch (Exception e) {
                log.warn("Failed to deserialize snapshot for report {}: {}", entity.getId(), e.getMessage());
            }
        }

        return GeneratedReportDto.builder()
                .reportId(entity.getId())
                .reportType(entity.getReportType())
                .institutionId(entity.getInstitutionId())
                .masterProgrammeId(entity.getMasterProgrammeId())
                .programmeBatchId(entity.getProgrammeBatchId())
                .programmeBatchCourseId(entity.getProgrammeBatchCourseId())
                .masterCourseId(entity.getMasterCourseId())
                .templateId(entity.getTemplateId())
                .templateVersion(entity.getTemplateVersion())
                .generatedBy(entity.getGeneratedBy())
                .generatedAt(entity.getGeneratedAt())
                .status(entity.getStatus())
                .snapshot(snapshot)
                .artifacts(artifactSummaries)
                .build();
    }

    private GeneratedReportDto.ArtifactSummaryDto toArtifactSummary(ReportArtifactEntity a) {
        return GeneratedReportDto.ArtifactSummaryDto.builder()
                .artifactId(a.getId())
                .artifactType(a.getArtifactType())
                .fileReference(a.getFileReference())
                .originalFilename(a.getOriginalFilename())
                .mimeType(a.getMimeType())
                .fileSize(a.getFileSize())
                .sha256Checksum(a.getSha256Checksum())
                .hmacSignature(a.getHmacSignature())
                .generatedAt(a.getGeneratedAt())
                .build();
    }

    private ReportType resolveProgrammeReportType(ReportSection section) {
        return switch (section) {
            case AVERAGE_MAPPING -> ReportType.PROGRAMME_ATTAINMENT_MAPPING;
            case AVERAGE_DIRECT -> ReportType.PROGRAMME_ATTAINMENT_DIRECT;
            case AVERAGE_INDIRECT -> ReportType.PROGRAMME_ATTAINMENT_INDIRECT;
            case OVERALL -> ReportType.PROGRAMME_ATTAINMENT_OVERALL;
            case ALL -> ReportType.PROGRAMME_ATTAINMENT;
        };
    }
}
