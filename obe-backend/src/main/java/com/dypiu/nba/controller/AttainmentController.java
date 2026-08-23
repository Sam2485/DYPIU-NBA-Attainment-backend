package com.dypiu.nba.controller;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.AttainmentConfiguration;
import com.dypiu.nba.service.AttainmentCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/attainment")
@RequiredArgsConstructor
public class AttainmentController {

    private final AttainmentCalculationService calculationService;
    private final com.dypiu.nba.service.AttainmentReportService attainmentReportService;
    private final com.dypiu.nba.service.AttainmentReportExportService exportService;
    private final com.dypiu.nba.repository.UploadedDocumentRepository uploadedDocumentRepository;
    private final com.dypiu.nba.service.ReportAccessService reportAccessService;
    private final com.dypiu.nba.repository.ProgrammeBatchCourseRepository programmeBatchCourseRepository;

    @GetMapping({"/config/{courseId}", "/configs/{courseId}"})
    public ResponseEntity<ApiResponse<AttainmentConfiguration>> getConfig(
            @PathVariable String courseId,
            @RequestParam(required = false) String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        if (programmeBatchCourseRepository.existsById(courseId)) {
            reportAccessService.validateCourseOfferingAccess(user, courseId);
        } else {
            reportAccessService.validateCourseAccess(user, courseId);
        }
        return ResponseEntity.ok(ApiResponse.<AttainmentConfiguration>builder()
                .success(true)
                .data(calculationService.getAttainmentConfig(courseId))
                .build());
    }

    @RequestMapping(value = {"/config/{courseId}", "/configs/{courseId}"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<AttainmentConfiguration>> saveConfig(
            @PathVariable String courseId,
            @RequestBody AttainmentConfiguration config,
            @RequestParam(required = false) String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        if (programmeBatchCourseRepository.existsById(courseId)) {
            reportAccessService.validateCourseOfferingAccess(user, courseId);
        } else {
            reportAccessService.validateCourseAccess(user, courseId);
        }
        return ResponseEntity.ok(ApiResponse.<AttainmentConfiguration>builder()
                .success(true)
                .message("Attainment configuration saved")
                .data(calculationService.saveAttainmentConfig(courseId, config))
                .build());
    }

    @GetMapping({"/course/{courseId}", "/courses/{courseId}", "/calculate/course/{courseId}"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseCoAttainment(
            @PathVariable String courseId,
            @RequestParam(required = false) String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        if (programmeBatchCourseRepository.existsById(courseId)) {
            reportAccessService.validateCourseOfferingAccess(user, courseId);
        } else {
            reportAccessService.validateCourseAccess(user, courseId);
        }
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("CO Attainment calculated successfully")
                .data(calculationService.calculateCourseCoAttainment(courseId))
                .build());
    }

    @GetMapping({"/courses/{courseId}/direct", "/course/{courseId}/direct"})
    public ResponseEntity<ApiResponse<ExaminationAttainmentResultDto>> getDirectAttainment(
            @PathVariable String courseId,
            @RequestParam(required = false) String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        if (programmeBatchCourseRepository.existsById(courseId)) {
            reportAccessService.validateCourseOfferingAccess(user, courseId);
        } else {
            reportAccessService.validateCourseAccess(user, courseId);
        }
        return ResponseEntity.ok(ApiResponse.<ExaminationAttainmentResultDto>builder()
                .success(true)
                .data(calculationService.getExaminationAttainment(courseId))
                .build());
    }

    @GetMapping({"/courses/{courseId}/indirect", "/course/{courseId}/indirect"})
    public ResponseEntity<ApiResponse<SurveyAttainmentResultDto>> getIndirectAttainment(
            @PathVariable String courseId,
            @RequestParam(required = false) String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        if (programmeBatchCourseRepository.existsById(courseId)) {
            reportAccessService.validateCourseOfferingAccess(user, courseId);
        } else {
            reportAccessService.validateCourseAccess(user, courseId);
        }
        return ResponseEntity.ok(ApiResponse.<SurveyAttainmentResultDto>builder()
                .success(true)
                .data(calculationService.getSurveyAttainment(courseId))
                .build());
    }

    @PostMapping({"/assessment/direct/upload", "/assessment/internal/upload"})
    public ResponseEntity<ApiResponse<ExaminationAttainmentResultDto>> uploadAssessmentDirect(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "courseOfferingId", required = false) String courseOfferingId,
            @RequestParam(value = "batchId", required = false) String batchId,
            @RequestParam(value = "assessmentType", required = false) String assessmentType,
            @RequestParam(value = "toolType", required = false) String toolType,
            java.security.Principal principal) {
        return uploadAssessmentDirect(file, courseId, courseOfferingId, batchId, null, assessmentType, toolType, principal);
    }

    public ResponseEntity<ApiResponse<ExaminationAttainmentResultDto>> uploadAssessmentDirect(
            MultipartFile file,
            String courseId,
            String courseOfferingId,
            String batchId,
            BigDecimal thresholdPercentage,
            String assessmentType,
            String toolType,
            java.security.Principal principal) {
        String targetId = courseOfferingId != null && !courseOfferingId.isBlank() ? courseOfferingId : courseId;
        if (targetId == null || targetId.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Course or Course Offering ID is required.");
        }
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        if (programmeBatchCourseRepository.existsById(targetId)) {
            reportAccessService.validateCourseOfferingAccess(user, targetId);
        } else {
            reportAccessService.validateCourseAccess(user, targetId);
        }
        return ResponseEntity.ok(ApiResponse.<ExaminationAttainmentResultDto>builder()
                .success(true)
                .message("Assessment marks uploaded and processed successfully.")
                .data(calculationService.processAndSaveExaminationFile(targetId, file, thresholdPercentage, user != null ? user.getName() : "Course Coordinator"))
                .build());
    }

    @PostMapping("/assessment/indirect/upload")
    public ResponseEntity<ApiResponse<SurveyAttainmentResultDto>> uploadAssessmentIndirect(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "courseOfferingId", required = false) String courseOfferingId,
            @RequestParam(value = "batchId", required = false) String batchId,
            java.security.Principal principal) {
        return uploadAssessmentIndirect(file, courseId, courseOfferingId, batchId, null, principal);
    }

    public ResponseEntity<ApiResponse<SurveyAttainmentResultDto>> uploadAssessmentIndirect(
            MultipartFile file,
            String courseId,
            String courseOfferingId,
            String batchId,
            BigDecimal thresholdPercentage,
            java.security.Principal principal) {
        String targetId = courseOfferingId != null && !courseOfferingId.isBlank() ? courseOfferingId : courseId;
        if (targetId == null || targetId.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Course or Course Offering ID is required.");
        }
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        if (programmeBatchCourseRepository.existsById(targetId)) {
            reportAccessService.validateCourseOfferingAccess(user, targetId);
        } else {
            reportAccessService.validateCourseAccess(user, targetId);
        }
        return ResponseEntity.ok(ApiResponse.<SurveyAttainmentResultDto>builder()
                .success(true)
                .message("Indirect survey responses uploaded and processed successfully.")
                .data(calculationService.processAndSaveSurveyFile(targetId, file, thresholdPercentage, user != null ? user.getName() : "Course Coordinator"))
                .build());
    }

    @GetMapping("/programme/{programmeId}/batch/{batchId}")
    public ResponseEntity<ApiResponse<ProgrammeAttainmentResultDto>> getProgrammeAttainment(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.<ProgrammeAttainmentResultDto>builder()
                .success(true)
                .message("Programme attainment calculated successfully")
                .data(calculationService.calculateProgrammeAttainment(programmeId, batchId))
                .build());
    }

    @GetMapping("/programme/{programmeId}/batch/{batchId}/dataset")
    public ResponseEntity<ApiResponse<ProgrammeAttainmentDatasetDto>> getProgrammeAttainmentDataset(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.<ProgrammeAttainmentDatasetDto>builder()
                .success(true)
                .message("Programme attainment dataset retrieved successfully")
                .data(calculationService.getProgrammeAttainmentDataset(programmeId, batchId))
                .build());
    }

    @GetMapping("/programme/{programmeId}/batch/{batchId}/average-mapping")
    public ResponseEntity<ApiResponse<Object>> getProgrammeAverageMapping(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        ProgrammeBatchAttainmentReportDto report = attainmentReportService.getOrCreateProgrammeAttainmentReport(programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(java.util.Map.of(
                        "poMappings", report.getReport1AverageMappingPO() != null ? report.getReport1AverageMappingPO() : java.util.Collections.emptyList(),
                        "psoMappings", report.getReport1AverageMappingPSO() != null ? report.getReport1AverageMappingPSO() : java.util.Collections.emptyList()
                ))
                .build());
    }

    @GetMapping("/programme/{programmeId}/batch/{batchId}/average-direct")
    public ResponseEntity<ApiResponse<Object>> getProgrammeAverageDirect(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        ProgrammeBatchAttainmentReportDto report = attainmentReportService.getOrCreateProgrammeAttainmentReport(programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(java.util.Map.of(
                        "poDirectAttainment", report.getReport2DirectAttainmentPO() != null ? report.getReport2DirectAttainmentPO() : java.util.Collections.emptyList(),
                        "psoDirectAttainment", report.getReport2DirectAttainmentPSO() != null ? report.getReport2DirectAttainmentPSO() : java.util.Collections.emptyList()
                ))
                .build());
    }

    @GetMapping("/programme/{programmeId}/batch/{batchId}/average-indirect")
    public ResponseEntity<ApiResponse<Object>> getProgrammeAverageIndirect(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        ProgrammeBatchAttainmentReportDto report = attainmentReportService.getOrCreateProgrammeAttainmentReport(programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(java.util.Map.of(
                        "poIndirectAttainment", report.getReport3IndirectAttainmentPO() != null ? report.getReport3IndirectAttainmentPO() : java.util.Collections.emptyList(),
                        "psoIndirectAttainment", report.getReport3IndirectAttainmentPSO() != null ? report.getReport3IndirectAttainmentPSO() : java.util.Collections.emptyList()
                ))
                .build());
    }

    @GetMapping("/programme/{programmeId}/batch/{batchId}/overall")
    public ResponseEntity<ApiResponse<Object>> getProgrammeOverall(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        ProgrammeBatchAttainmentReportDto report = attainmentReportService.getOrCreateProgrammeAttainmentReport(programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(java.util.Map.of(
                        "poOverallAttainment", report.getReport4OverallAttainmentPO() != null ? report.getReport4OverallAttainmentPO() : java.util.Collections.emptyList(),
                        "psoOverallAttainment", report.getReport4OverallAttainmentPSO() != null ? report.getReport4OverallAttainmentPSO() : java.util.Collections.emptyList()
                ))
                .build());
    }

    @PostMapping(value = "/programmes/{programmeId}/batches/{batchId}/programme-survey/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProgrammeSurveyResultDto>> uploadProgrammeSurvey(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        String uploader = (uploadedBy != null && !uploadedBy.isBlank()) ? uploadedBy : (principal != null ? principal.getName() : "Programme Coordinator");
        return ResponseEntity.ok(ApiResponse.<ProgrammeSurveyResultDto>builder()
                .success(true)
                .message("Programme exit survey processed successfully")
                .data(calculationService.processAndSaveProgrammeSurveyFile(programmeId, batchId, file, uploader))
                .build());
    }


    // --- Examination Attainment Endpoints (Sheet 2: Examination) ---

    @GetMapping({"/examination/{courseOfferingId}", "/course-offerings/{courseOfferingId}/examination"})
    public ResponseEntity<ApiResponse<ExaminationAttainmentResultDto>> getExaminationAttainment(
            @PathVariable String courseOfferingId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, courseOfferingId);
        return ResponseEntity.ok(ApiResponse.<ExaminationAttainmentResultDto>builder()
                .success(true)
                .message("Examination attainment fetched successfully")
                .data(calculationService.getExaminationAttainment(courseOfferingId))
                .build());
    }

    @PostMapping({"/examination/{courseOfferingId}", "/course-offerings/{courseOfferingId}/examination"})
    public ResponseEntity<ApiResponse<ExaminationAttainmentResultDto>> saveAndCalculateExaminationAttainment(
            @PathVariable String courseOfferingId,
            @RequestBody ExaminationMarksPayloadDto payload,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, courseOfferingId);
        return ResponseEntity.ok(ApiResponse.<ExaminationAttainmentResultDto>builder()
                .success(true)
                .message("Examination threshold, out-of marks, student marks saved and attainment calculated successfully")
                .data(calculationService.calculateExaminationAttainment(courseOfferingId, payload))
                .build());
    }

    @PostMapping(value = {"/examination/{courseOfferingId}/upload", "/course-offerings/{courseOfferingId}/examination/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExaminationAttainmentResultDto>> uploadAndProcessExaminationSheet(
            @PathVariable String courseOfferingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "thresholdPercentage", required = false) BigDecimal thresholdPercentage,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, courseOfferingId);
        String uploader = (uploadedBy != null && !uploadedBy.isBlank()) ? uploadedBy : (principal != null ? principal.getName() : "Course Coordinator");
        return ResponseEntity.ok(ApiResponse.<ExaminationAttainmentResultDto>builder()
                .success(true)
                .message("Examination sheet saved with audit metadata, parsed via POI, and attainment calculated successfully")
                .data(calculationService.processAndSaveExaminationFile(courseOfferingId, file, thresholdPercentage, uploader))
                .build());
    }

    // --- Course End Survey Attainment Endpoints (Sheet 3: Course End Survey) ---

    @GetMapping({"/survey/{courseOfferingId}", "/course-offerings/{courseOfferingId}/survey"})
    public ResponseEntity<ApiResponse<SurveyAttainmentResultDto>> getSurveyAttainment(
            @PathVariable String courseOfferingId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, courseOfferingId);
        return ResponseEntity.ok(ApiResponse.<SurveyAttainmentResultDto>builder()
                .success(true)
                .message("Course End Survey attainment fetched successfully")
                .data(calculationService.getSurveyAttainment(courseOfferingId))
                .build());
    }

    @PostMapping({"/survey/{courseOfferingId}", "/course-offerings/{courseOfferingId}/survey"})
    public ResponseEntity<ApiResponse<SurveyAttainmentResultDto>> saveAndCalculateSurveyAttainment(
            @PathVariable String courseOfferingId,
            @RequestBody SurveyMarksPayloadDto payload,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, courseOfferingId);
        return ResponseEntity.ok(ApiResponse.<SurveyAttainmentResultDto>builder()
                .success(true)
                .message("Course End Survey responses saved and indirect attainment calculated successfully")
                .data(calculationService.calculateSurveyAttainment(courseOfferingId, payload))
                .build());
    }

    @PostMapping(value = {"/survey/{courseOfferingId}/upload", "/course-offerings/{courseOfferingId}/survey/upload"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SurveyAttainmentResultDto>> uploadAndProcessSurveySheet(
            @PathVariable String courseOfferingId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "thresholdPercentage", required = false) BigDecimal thresholdPercentage,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, courseOfferingId);
        String uploader = (uploadedBy != null && !uploadedBy.isBlank()) ? uploadedBy : (principal != null ? principal.getName() : "Course Coordinator");
        return ResponseEntity.ok(ApiResponse.<SurveyAttainmentResultDto>builder()
                .success(true)
                .message("Course End Survey sheet saved with audit metadata, parsed via POI, and indirect attainment calculated successfully")
                .data(calculationService.processAndSaveSurveyFile(courseOfferingId, file, thresholdPercentage, uploader))
                .build());
    }

    @GetMapping("/documents/{courseOfferingOrCourseId}")
    public ResponseEntity<ApiResponse<java.util.List<com.dypiu.nba.entity.UploadedDocument>>> getUploadedDocuments(
            @PathVariable String courseOfferingOrCourseId,
            java.security.Principal principal) {
        String offeringId = calculationService.resolveOfferingId(courseOfferingOrCourseId);
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, offeringId);
        return ResponseEntity.ok(ApiResponse.<java.util.List<com.dypiu.nba.entity.UploadedDocument>>builder()
                .success(true)
                .message("Uploaded audit documents retrieved successfully")
                .data(calculationService.getUploadedDocumentsForCourse(courseOfferingOrCourseId))
                .build());
    }

    @GetMapping("/documents/{courseId}/download/{documentType}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadUploadedDocument(
            @PathVariable String courseId,
            @PathVariable String documentType,
            java.security.Principal principal) {
        String offeringId = calculationService.resolveOfferingId(courseId);
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, offeringId);

        com.dypiu.nba.entity.DocumentType dt = null;
        try {
            dt = com.dypiu.nba.entity.DocumentType.valueOf(documentType.toUpperCase());
        } catch (Exception ignored) {}

        com.dypiu.nba.entity.UploadedDocument doc = (dt != null)
                ? uploadedDocumentRepository.findFirstByProgrammeBatchCourseIdAndDocumentTypeOrderByUploadedAtDesc(offeringId, dt).orElse(null)
                : null;

        if (doc == null || doc.getSavedPath() == null) {
            return ResponseEntity.notFound().build();
        }

        java.io.File file = new java.io.File(doc.getSavedPath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        org.springframework.core.io.Resource resource = new org.springframework.core.io.FileSystemResource(file);
        String fileName = doc.getFileName() != null ? doc.getFileName() : file.getName();

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }


    // =========================================================================
    //  ATTAINMENT EXPORT ENDPOINTS (EXCEL & PDF)
    // =========================================================================

    @GetMapping(value = "/export/excel/{courseId}", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportAttainmentExcel(
            @PathVariable String courseId,
            @RequestParam(value = "batchId", required = false) String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        if (programmeBatchCourseRepository.existsById(courseId)) {
            reportAccessService.validateCourseOfferingAccess(user, courseId);
        } else {
            reportAccessService.validateCourseAccess(user, courseId);
        }
        byte[] excelBytes = exportService.generateAttainmentExcel(courseId, batchId);
        String filename = "Attainment_Sheet_" + courseId + ".xlsx";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping(value = "/export/pdf/{courseId}", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportAttainmentPdf(
            @PathVariable String courseId,
            @RequestParam(value = "batchId", required = false) String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        if (programmeBatchCourseRepository.existsById(courseId)) {
            reportAccessService.validateCourseOfferingAccess(user, courseId);
        } else {
            reportAccessService.validateCourseAccess(user, courseId);
        }
        byte[] pdfBytes = exportService.generateAttainmentPdf(courseId, batchId);
        String filename = "NBA_Attainment_Report_" + courseId + ".pdf";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping(value = "/reports/{courseId}/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportAttainmentExcelAlias(
            @PathVariable String courseId,
            @RequestParam(value = "batchId", required = false) String batchId,
            java.security.Principal principal) {
        return exportAttainmentExcel(courseId, batchId, principal);
    }

    @GetMapping(value = "/reports/{courseId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportAttainmentPdfAlias(
            @PathVariable String courseId,
            @RequestParam(value = "batchId", required = false) String batchId,
            java.security.Principal principal) {
        return exportAttainmentPdf(courseId, batchId, principal);
    }
}
