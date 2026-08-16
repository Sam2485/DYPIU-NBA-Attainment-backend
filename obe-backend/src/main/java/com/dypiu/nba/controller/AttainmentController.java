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
    private final com.dypiu.nba.service.AttainmentReportExportService exportService;
    private final com.dypiu.nba.repository.UploadedDocumentRepository uploadedDocumentRepository;
    private final com.dypiu.nba.service.ReportAccessService reportAccessService;
    private final com.dypiu.nba.repository.CourseOfferingRepository courseOfferingRepository;

    @GetMapping("/config/{courseId}")
    public ResponseEntity<ApiResponse<AttainmentConfiguration>> getConfig(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<AttainmentConfiguration>builder()
                .success(true)
                .data(calculationService.getAttainmentConfig(courseId))
                .build());
    }

    @PostMapping("/config/{courseId}")
    public ResponseEntity<ApiResponse<AttainmentConfiguration>> saveConfig(@PathVariable String courseId, @RequestBody AttainmentConfiguration config) {
        return ResponseEntity.ok(ApiResponse.<AttainmentConfiguration>builder()
                .success(true)
                .message("Attainment configuration saved")
                .data(calculationService.saveAttainmentConfig(courseId, config))
                .build());
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseCoAttainment(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("CO Attainment calculated successfully")
                .data(calculationService.calculateCourseCoAttainment(courseId))
                .build());
    }

    @GetMapping("/calculate/course/{courseId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateCourseCoAttainment(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("CO Attainment calculated successfully")
                .data(calculationService.calculateCourseCoAttainment(courseId))
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
        ProgrammeAttainmentDatasetDto dataset = calculationService.getProgrammeAttainmentDataset(programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(dataset.getAverageMapping())
                .build());
    }

    @GetMapping("/programme/{programmeId}/batch/{batchId}/average-direct")
    public ResponseEntity<ApiResponse<Object>> getProgrammeAverageDirect(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        ProgrammeAttainmentDatasetDto dataset = calculationService.getProgrammeAttainmentDataset(programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(dataset.getAverageDirectAttainment())
                .build());
    }

    @GetMapping("/programme/{programmeId}/batch/{batchId}/average-indirect")
    public ResponseEntity<ApiResponse<Object>> getProgrammeAverageIndirect(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        ProgrammeAttainmentDatasetDto dataset = calculationService.getProgrammeAttainmentDataset(programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(dataset.getAverageIndirectAttainment())
                .build());
    }

    @GetMapping("/programme/{programmeId}/batch/{batchId}/overall")
    public ResponseEntity<ApiResponse<Object>> getProgrammeOverall(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            java.security.Principal principal) {
        com.dypiu.nba.entity.User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);
        ProgrammeAttainmentDatasetDto dataset = calculationService.getProgrammeAttainmentDataset(programmeId, batchId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(dataset.getOverallAttainment())
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
                ? uploadedDocumentRepository.findFirstByCourseOfferingIdAndDocumentTypeOrderByUploadedAtDesc(offeringId, dt).orElse(null)
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
            @RequestParam(value = "batchId", required = false) String batchId) {
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
            @RequestParam(value = "batchId", required = false) String batchId) {
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
            @RequestParam(value = "batchId", required = false) String batchId) {
        return exportAttainmentExcel(courseId, batchId);
    }

    @GetMapping(value = "/reports/{courseId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportAttainmentPdfAlias(
            @PathVariable String courseId,
            @RequestParam(value = "batchId", required = false) String batchId) {
        return exportAttainmentPdf(courseId, batchId);
    }
}
