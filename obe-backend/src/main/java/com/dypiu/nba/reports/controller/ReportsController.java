package com.dypiu.nba.reports.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.reports.model.*;
import com.dypiu.nba.reports.repository.ReportArtifactRepository;
import com.dypiu.nba.reports.repository.ReportAssetRepository;
import com.dypiu.nba.reports.repository.ReportRepository;
import com.dypiu.nba.reports.service.ReportOrchestrationService;
import com.dypiu.nba.reports.service.ReportStorageService;
import com.dypiu.nba.reports.template.HeaderConfig;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import com.dypiu.nba.reports.template.ReportTemplateService;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping({"/reports", "/api/v1/reports"})
@RequiredArgsConstructor
@Slf4j
public class ReportsController {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".svg", ".webp");

    private final ReportOrchestrationService orchestrationService;
    private final ReportRepository reportRepository;
    private final ReportArtifactRepository artifactRepository;
    private final ReportAssetRepository assetRepository;
    private final ReportTemplateService templateService;
    private final ReportStorageService storageService;
    private final CurrentUserScopeService currentUserScopeService;

    // --- 1. Master Programme Attainment Downloads ---

    @GetMapping("/programme-attainment/{programmeBatchId}/master/excel")
    public ResponseEntity<byte[]> downloadProgrammeAttainmentMasterExcel(
            @PathVariable String programmeBatchId,
            @RequestParam(required = false) String masterProgrammeId,
            Principal principal) {

        String user = principal != null ? principal.getName() : "Academic User";
        GeneratedReportDto report = orchestrationService.generateProgrammeAttainmentReport(
                masterProgrammeId, programmeBatchId, ReportSection.ALL, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.EXCEL)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Excel artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/programme-attainment/{programmeBatchId}/master/pdf")
    public ResponseEntity<byte[]> downloadProgrammeAttainmentMasterPdf(
            @PathVariable String programmeBatchId,
            @RequestParam(required = false) String masterProgrammeId,
            Principal principal) {

        String user = principal != null ? principal.getName() : "Academic User";
        GeneratedReportDto report = orchestrationService.generateProgrammeAttainmentReport(
                masterProgrammeId, programmeBatchId, ReportSection.ALL, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.PDF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("PDF artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/pdf");
    }

    // --- 2. Individual Programme Attainment Section Downloads ---

    @GetMapping("/programme-attainment/{programmeBatchId}/section/{section}/excel")
    public ResponseEntity<byte[]> downloadProgrammeAttainmentSectionExcel(
            @PathVariable String programmeBatchId,
            @PathVariable String section,
            @RequestParam(required = false) String masterProgrammeId,
            Principal principal) {

        ReportSection reportSection = parseReportSection(section);
        String user = principal != null ? principal.getName() : "Academic User";
        GeneratedReportDto report = orchestrationService.generateProgrammeAttainmentReport(
                masterProgrammeId, programmeBatchId, reportSection, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.EXCEL)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Excel artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/programme-attainment/{programmeBatchId}/section/{section}/pdf")
    public ResponseEntity<byte[]> downloadProgrammeAttainmentSectionPdf(
            @PathVariable String programmeBatchId,
            @PathVariable String section,
            @RequestParam(required = false) String masterProgrammeId,
            Principal principal) {

        ReportSection reportSection = parseReportSection(section);
        String user = principal != null ? principal.getName() : "Academic User";
        GeneratedReportDto report = orchestrationService.generateProgrammeAttainmentReport(
                masterProgrammeId, programmeBatchId, reportSection, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.PDF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("PDF artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/pdf");
    }

    // --- 3. Course Attainment Downloads ---

    @GetMapping("/course-attainment/{programmeBatchCourseId}/excel")
    public ResponseEntity<byte[]> downloadCourseAttainmentExcel(
            @PathVariable String programmeBatchCourseId,
            Principal principal) {

        String user = principal != null ? principal.getName() : "Course Coordinator";
        GeneratedReportDto report = orchestrationService.generateCourseAttainmentReport(
                programmeBatchCourseId, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.EXCEL)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Excel artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/course-attainment/{programmeBatchCourseId}/pdf")
    public ResponseEntity<byte[]> downloadCourseAttainmentPdf(
            @PathVariable String programmeBatchCourseId,
            Principal principal) {

        String user = principal != null ? principal.getName() : "Course Coordinator";
        GeneratedReportDto report = orchestrationService.generateCourseAttainmentReport(
                programmeBatchCourseId, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.PDF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("PDF artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/pdf");
    }

    // --- 4. Programme ATR Downloads ---

    @GetMapping("/programme-atr/{programmeBatchId}/excel")
    public ResponseEntity<byte[]> downloadProgrammeAtrExcel(
            @PathVariable String programmeBatchId,
            Principal principal) {

        String user = principal != null ? principal.getName() : "Programme Coordinator";
        GeneratedReportDto report = orchestrationService.generateProgrammeAtrReport(
                programmeBatchId, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.EXCEL)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Excel artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/programme-atr/{programmeBatchId}/pdf")
    public ResponseEntity<byte[]> downloadProgrammeAtrPdf(
            @PathVariable String programmeBatchId,
            Principal principal) {

        String user = principal != null ? principal.getName() : "Programme Coordinator";
        GeneratedReportDto report = orchestrationService.generateProgrammeAtrReport(
                programmeBatchId, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.PDF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("PDF artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/pdf");
    }

    // --- 5. Course ATR Downloads ---

    @GetMapping("/course-atr/{programmeBatchCourseId}/excel")
    public ResponseEntity<byte[]> downloadCourseAtrExcel(
            @PathVariable String programmeBatchCourseId,
            Principal principal) {

        String user = principal != null ? principal.getName() : "Course Coordinator";
        GeneratedReportDto report = orchestrationService.generateCourseAtrReport(
                programmeBatchCourseId, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.EXCEL)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Excel artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/course-atr/{programmeBatchCourseId}/pdf")
    public ResponseEntity<byte[]> downloadCourseAtrPdf(
            @PathVariable String programmeBatchCourseId,
            Principal principal) {

        String user = principal != null ? principal.getName() : "Course Coordinator";
        GeneratedReportDto report = orchestrationService.generateCourseAtrReport(
                programmeBatchCourseId, user, "DYPIU");

        GeneratedReportDto.ArtifactSummaryDto artifact = report.getArtifacts().stream()
                .filter(a -> a.getArtifactType() == ArtifactType.PDF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("PDF artifact was not generated"));

        byte[] bytes = orchestrationService.loadArtifactContent(artifact.getArtifactId());
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), "application/pdf");
    }

    // --- 6. Artifact Verification Endpoint ---

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VerificationResponseDto>> verifyReportArtifact(
            @RequestParam("reportId") String reportId,
            @RequestParam("artifactType") String artifactType,
            @RequestParam("file") MultipartFile file) {

        try {
            ArtifactType type = ArtifactType.valueOf(artifactType.toUpperCase().trim());
            byte[] bytes = file.getBytes();
            VerificationResponseDto result = orchestrationService.verifyReportArtifact(reportId, type, bytes);

            return ResponseEntity.ok(ApiResponse.<VerificationResponseDto>builder()
                    .success(result.isValid())
                    .message(result.getMessage())
                    .data(result)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.<VerificationResponseDto>builder()
                    .success(false)
                    .message("Verification failed: " + e.getMessage())
                    .build());
        }
    }

    // --- 7. Generic Artifact Download by Artifact ID ---

    @GetMapping("/artifacts/{artifactId}/download")
    public ResponseEntity<byte[]> downloadArtifactById(@PathVariable String artifactId) {
        ReportArtifactEntity artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Artifact not found: " + artifactId));

        byte[] bytes = orchestrationService.loadArtifactContent(artifactId);
        return createDownloadResponse(bytes, artifact.getOriginalFilename(), artifact.getMimeType());
    }

    // --- 8. IQAC Report Template Management Endpoints ---

    @GetMapping("/template")
    public ResponseEntity<ApiResponse<ReportTemplateDto>> getInstitutionTemplate(
            @RequestParam(required = false) String institutionId) {
        String inst = resolveInstitutionId(institutionId);
        ReportTemplateDto template = templateService.getInstitutionTemplate(inst);
        return ResponseEntity.ok(ApiResponse.<ReportTemplateDto>builder()
                .success(true)
                .data(template)
                .build());
    }

    @PutMapping("/template")
    public ResponseEntity<ApiResponse<ReportTemplateDto>> updateInstitutionTemplate(
            @RequestBody ReportTemplateDto dto,
            @RequestParam(required = false) String institutionId) {
        enforceIqacAuthority("Only IQAC can modify the institutional report template.");
        String inst = resolveInstitutionId(institutionId != null ? institutionId : dto.getInstitutionId());
        ReportTemplateDto saved = templateService.saveInstitutionTemplate(dto, inst);
        return ResponseEntity.ok(ApiResponse.<ReportTemplateDto>builder()
                .success(true)
                .message("Institution report template updated successfully")
                .data(saved)
                .build());
    }

    @GetMapping("/template/header")
    public ResponseEntity<ApiResponse<HeaderConfig>> getHeaderConfig(
            @RequestParam(required = false) String institutionId) {
        String inst = resolveInstitutionId(institutionId);
        HeaderConfig header = templateService.getHeaderConfig(inst);
        return ResponseEntity.ok(ApiResponse.<HeaderConfig>builder()
                .success(true)
                .data(header)
                .build());
    }

    @PutMapping("/template/header")
    public ResponseEntity<ApiResponse<HeaderConfig>> updateHeaderConfig(
            @RequestBody HeaderConfig headerConfig,
            @RequestParam(required = false) String institutionId) {
        enforceIqacAuthority("Only IQAC can update the report header configuration.");
        String inst = resolveInstitutionId(institutionId);
        HeaderConfig saved = templateService.saveHeaderConfig(headerConfig, inst);
        return ResponseEntity.ok(ApiResponse.<HeaderConfig>builder()
                .success(true)
                .message("Report header configuration updated successfully")
                .data(saved)
                .build());
    }

    // --- 9. IQAC Report Asset / Logo Management Endpoints ---

    @PostMapping(value = "/assets/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ReportAssetDto>> uploadReportAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assetType") String assetTypeStr,
            @RequestParam(required = false) String institutionId,
            Principal principal) {
        enforceIqacAuthority("Only IQAC can upload institutional branding assets.");

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload file cannot be empty.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 5MB limit.");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = getFileExtension(originalFilename);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file format. Allowed formats: PNG, JPG, JPEG, SVG, WEBP.");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !mimeType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid MIME type. Must be an image file.");
        }

        ReportAssetType assetType;
        try {
            assetType = ReportAssetType.valueOf(assetTypeStr.trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assetType: " + assetTypeStr + ". Allowed: LEFT_LOGO, RIGHT_LOGO, WATERMARK, HEADER_IMAGE, OTHER");
        }

        String inst = resolveInstitutionId(institutionId);
        String actor = principal != null ? principal.getName() : "IQAC";

        try {
            byte[] bytes = file.getBytes();
            String storagePath = storageService.storeReportAsset(inst, originalFilename, bytes, mimeType);

            String assetId = "ast-" + UUID.randomUUID().toString().substring(0, 8);
            ReportAssetEntity assetEntity = ReportAssetEntity.builder()
                    .id(assetId)
                    .institutionId(inst)
                    .assetType(assetType)
                    .originalFilename(originalFilename)
                    .storagePath(storagePath)
                    .mimeType(mimeType)
                    .fileSize((long) bytes.length)
                    .createdBy(actor)
                    .build();

            assetRepository.save(assetEntity);

            HeaderConfig currentHeader = templateService.getHeaderConfig(inst);
            if (assetType == ReportAssetType.LEFT_LOGO) {
                currentHeader.setLeftLogoAssetId(assetId);
                templateService.saveHeaderConfig(currentHeader, inst);
            } else if (assetType == ReportAssetType.RIGHT_LOGO) {
                currentHeader.setRightLogoAssetId(assetId);
                templateService.saveHeaderConfig(currentHeader, inst);
            }

            ReportAssetDto dto = templateService.toAssetDto(assetEntity);
            return ResponseEntity.ok(ApiResponse.<ReportAssetDto>builder()
                    .success(true)
                    .message("Report asset uploaded successfully and linked to header template")
                    .data(dto)
                    .build());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store asset: " + e.getMessage());
        }
    }

    @GetMapping("/assets")
    public ResponseEntity<ApiResponse<List<ReportAssetDto>>> listAssets(
            @RequestParam(required = false) String institutionId,
            @RequestParam(required = false) String assetType) {
        String inst = resolveInstitutionId(institutionId);
        List<ReportAssetEntity> entities = (assetType != null && !assetType.isBlank())
                ? assetRepository.findByInstitutionIdAndAssetType(inst, ReportAssetType.valueOf(assetType.toUpperCase()))
                : assetRepository.findByInstitutionId(inst);

        List<ReportAssetDto> dtos = entities.stream()
                .map(templateService::toAssetDto)
                .toList();

        return ResponseEntity.ok(ApiResponse.<List<ReportAssetDto>>builder()
                .success(true)
                .data(dtos)
                .build());
    }

    @GetMapping("/assets/{assetId}")
    public ResponseEntity<ApiResponse<ReportAssetDto>> getAssetMetadata(
            @PathVariable String assetId) {
        ReportAssetEntity entity = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report asset not found: " + assetId));
        return ResponseEntity.ok(ApiResponse.<ReportAssetDto>builder()
                .success(true)
                .data(templateService.toAssetDto(entity))
                .build());
    }

    @GetMapping({"/assets/{assetId}/raw", "/assets/{assetId}/view"})
    public ResponseEntity<byte[]> viewAssetRaw(@PathVariable String assetId) {
        ReportAssetEntity entity = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report asset not found: " + assetId));

        byte[] bytes = storageService.loadReportAsset(entity.getStoragePath());
        if (bytes == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset file not found in storage: " + entity.getStoragePath());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(entity.getMimeType() != null ? entity.getMimeType() : "image/png"))
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .body(bytes);
    }

    @DeleteMapping("/assets/{assetId}")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(@PathVariable String assetId) {
        enforceIqacAuthority("Only IQAC can delete institutional branding assets.");
        ReportAssetEntity entity = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report asset not found: " + assetId));

        storageService.deleteReportArtifact(entity.getStoragePath());
        assetRepository.delete(entity);

        String inst = entity.getInstitutionId() != null ? entity.getInstitutionId() : "DYPIU";
        HeaderConfig header = templateService.getHeaderConfig(inst);
        boolean changed = false;
        if (assetId.equalsIgnoreCase(header.getLeftLogoAssetId())) {
            header.setLeftLogoAssetId(null);
            changed = true;
        }
        if (assetId.equalsIgnoreCase(header.getRightLogoAssetId())) {
            header.setRightLogoAssetId(null);
            changed = true;
        }
        if (changed) {
            templateService.saveHeaderConfig(header, inst);
        }

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Report asset deleted successfully")
                .build());
    }

    // --- 10. Generated Reports Listing & History Endpoints ---

    @GetMapping
    public ResponseEntity<ApiResponse<List<GeneratedReportDto>>> listGeneratedReports(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId,
            @RequestParam(required = false) String programmeBatchCourseId,
            @RequestParam(required = false) String masterCourseId,
            @RequestParam(required = false) String institutionId) {

        ReportType type = null;
        if (reportType != null && !reportType.isBlank()) {
            try {
                type = ReportType.valueOf(reportType.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String inst = resolveInstitutionId(institutionId);
        List<GeneratedReportDto> reports = orchestrationService.listGeneratedReports(
                type, masterProgrammeId, programmeBatchId, programmeBatchCourseId, masterCourseId, inst);

        return ResponseEntity.ok(ApiResponse.<List<GeneratedReportDto>>builder()
                .success(true)
                .data(reports)
                .build());
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<GeneratedReportDto>> getGeneratedReportDetails(
            @PathVariable String reportId) {
        GeneratedReportDto report = orchestrationService.getGeneratedReport(reportId);
        return ResponseEntity.ok(ApiResponse.<GeneratedReportDto>builder()
                .success(true)
                .data(report)
                .build());
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf("."));
    }

    private String resolveInstitutionId(String requestedInst) {
        if (requestedInst != null && !requestedInst.isBlank()) {
            return requestedInst.trim();
        }
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        return "DYPIU";
    }

    private void enforceIqacAuthority(String message) {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (scope != null && !scope.isIqac()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: " + message);
        }
    }

    private ResponseEntity<byte[]> createDownloadResponse(byte[] content, String filename, String mimeType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.length))
                .body(content);
    }

    private ReportSection parseReportSection(String sec) {
        if (sec == null) return ReportSection.ALL;
        String s = sec.toUpperCase().trim().replace("-", "_");
        return switch (s) {
            case "AVERAGE_MAPPING", "MAPPING" -> ReportSection.AVERAGE_MAPPING;
            case "AVERAGE_DIRECT", "DIRECT", "AVERAGE_DIRECT_ATTAINMENT" -> ReportSection.AVERAGE_DIRECT;
            case "AVERAGE_INDIRECT", "INDIRECT", "AVERAGE_INDIRECT_ATTAINMENT" -> ReportSection.AVERAGE_INDIRECT;
            case "OVERALL", "OVERALL_ATTAINMENT" -> ReportSection.OVERALL;
            default -> ReportSection.ALL;
        };
    }
}
