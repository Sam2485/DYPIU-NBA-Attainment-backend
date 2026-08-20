package com.dypiu.nba.controller;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AcademicService;
import com.dypiu.nba.service.AtrService;
import com.dypiu.nba.service.AttainmentCalculationService;
import com.dypiu.nba.service.ReportAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AtrService atrService;
    private final ReportAccessService reportAccessService;
    private final AcademicService academicService;
    private final AttainmentCalculationService attainmentCalculationService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseRepository courseRepository;
    private final ProgrammeRepository programmeRepository;
    private final BatchRepository batchRepository;
    private final CourseAtrRepository courseAtrRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;
    private final com.dypiu.nba.service.AttainmentReportExportService exportService;
    private final com.dypiu.nba.service.OutcomeService outcomeService;

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<ReportFiltersDto>> getReportFilters(Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        return ResponseEntity.ok(ApiResponse.<ReportFiltersDto>builder()
                .success(true)
                .data(reportAccessService.getReportFilters(user))
                .build());
    }

    // --- Course ATR Endpoints ---

    @GetMapping("/course-atr/{courseOfferingId}")
    public ResponseEntity<ApiResponse<CourseAtrReportDto>> getCourseAtrReport(
            @PathVariable String courseOfferingId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseAtrAccess(user, courseOfferingId);

        return ResponseEntity.ok(ApiResponse.<CourseAtrReportDto>builder()
                .success(true)
                .data(atrService.getCourseAtrReport(courseOfferingId))
                .build());
    }

    @GetMapping("/course-atr/{courseOfferingId}/export-data")
    public ResponseEntity<ApiResponse<CourseAtrReportDto>> getCourseAtrExportData(
            @PathVariable String courseOfferingId,
            Principal principal) {
        return getCourseAtrReport(courseOfferingId, principal);
    }

    @PostMapping("/course-atr")
    public ResponseEntity<ApiResponse<CourseAtrReportDto>> saveCourseAtrReport(
            @RequestBody CourseAtrReportDto dto,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        if (dto.getCourseOffering() != null) {
            reportAccessService.validateCourseAtrAccess(user, dto.getCourseOffering().getId());
        }

        return ResponseEntity.ok(ApiResponse.<CourseAtrReportDto>builder()
                .success(true)
                .message("Course ATR saved successfully")
                .data(atrService.saveCourseAtrReport(dto))
                .build());
    }

    @PostMapping("/course-atr/{courseOfferingId}/submit")
    public ResponseEntity<ApiResponse<CourseAtr>> submitCourseAtr(
            @PathVariable String courseOfferingId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseAtrAccess(user, courseOfferingId);

        String submitter = user != null ? user.getName() : "Course Coordinator";
        return ResponseEntity.ok(ApiResponse.<CourseAtr>builder()
                .success(true)
                .message("Course ATR submitted for verification")
                .data(atrService.submitCourseAtr(courseOfferingId, submitter))
                .build());
    }

    @GetMapping("/course-atrs")
    public ResponseEntity<ApiResponse<List<CourseAtrReportDto>>> listCourseAtrs(
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String programmeId,
            @RequestParam(required = false) String courseId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);

        List<CourseOffering> offerings;
        if (batchId != null && !batchId.isBlank()) {
            offerings = courseOfferingRepository.findByBatchId(batchId);
        } else if (courseId != null && !courseId.isBlank()) {
            offerings = courseOfferingRepository.findByCourseId(courseId);
        } else {
            offerings = courseOfferingRepository.findAll();
        }

        List<CourseAtrReportDto> reports = offerings.stream()
                .filter(o -> {
                    try {
                        reportAccessService.validateCourseOfferingAccess(user, o.getId());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(o -> atrService.getCourseAtrReport(o.getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<CourseAtrReportDto>>builder()
                .success(true)
                .data(reports)
                .build());
    }

    // --- Programme ATR Endpoints ---

    @GetMapping("/programme-atr/{programmeId}/batch/{batchId}")
    public ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> getProgrammeAtrReport(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);

        return ResponseEntity.ok(ApiResponse.<ProgrammeAtrReportDto>builder()
                .success(true)
                .data(atrService.getProgrammeAtrReport(programmeId, batchId))
                .build());
    }

    @GetMapping("/programme-atr/{programmeId}/batch/{batchId}/export-data")
    public ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> getProgrammeAtrExportData(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            Principal principal) {
        return getProgrammeAtrReport(programmeId, batchId, principal);
    }

    @PostMapping("/programme-atr")
    public ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> saveProgrammeAtrReport(
            @RequestBody ProgrammeAtrReportDto dto,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        if (dto.getProgramme() != null && dto.getBatch() != null) {
            reportAccessService.validateProgrammeAtrAccess(user, dto.getProgramme().getId(), dto.getBatch().getId());
        }

        return ResponseEntity.ok(ApiResponse.<ProgrammeAtrReportDto>builder()
                .success(true)
                .message("Programme ATR saved successfully")
                .data(atrService.saveProgrammeAtrReport(dto))
                .build());
    }

    @PostMapping("/programme-atr/{programmeId}/batch/{batchId}/submit")
    public ResponseEntity<ApiResponse<ProgrammeAtr>> submitProgrammeAtr(
            @PathVariable String programmeId,
            @PathVariable String batchId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAtrAccess(user, programmeId, batchId);

        String submitter = user != null ? user.getName() : "Programme Coordinator";
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtr>builder()
                .success(true)
                .message("Programme ATR submitted for verification")
                .data(atrService.submitProgrammeAtr(programmeId, batchId, submitter))
                .build());
    }

    @GetMapping("/programme-atrs")
    public ResponseEntity<ApiResponse<List<ProgrammeAtrReportDto>>> listProgrammeAtrs(
            @RequestParam(required = false) String programmeId,
            @RequestParam(required = false) String batchId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        if (user != null && user.getRole() == UserRole.FACULTY) {
            return ResponseEntity.ok(ApiResponse.<List<ProgrammeAtrReportDto>>builder().success(true).data(Collections.emptyList()).build());
        }

        List<Batch> batches;
        if (batchId != null && !batchId.isBlank()) {
            batches = batchRepository.findById(batchId).map(List::of).orElse(Collections.emptyList());
        } else if (programmeId != null && !programmeId.isBlank()) {
            batches = batchRepository.findByProgrammeId(programmeId);
        } else {
            batches = batchRepository.findAll();
        }

        List<ProgrammeAtrReportDto> reports = batches.stream()
                .filter(b -> {
                    try {
                        reportAccessService.validateProgrammeAtrAccess(user, b.getProgrammeId(), b.getId());
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .map(b -> atrService.getProgrammeAtrReport(b.getProgrammeId(), b.getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<ProgrammeAtrReportDto>>builder()
                .success(true)
                .data(reports)
                .build());
    }

    // --- Historical & Batch Summary Endpoints ---

    @GetMapping("/programmes/{programmeId}/batch-comparison")
    public ResponseEntity<ApiResponse<BatchComparisonDto>> getProgrammeBatchComparison(
            @PathVariable String programmeId,
            @RequestParam(required = false) List<String> batchIds,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAccess(user, programmeId);

        return ResponseEntity.ok(ApiResponse.<BatchComparisonDto>builder()
                .success(true)
                .data(atrService.getProgrammeBatchComparison(programmeId, batchIds))
                .build());
    }

    @GetMapping("/batch/{batchId}/summary")
    public ResponseEntity<ApiResponse<BatchContextDto>> getBatchSummary(
            @PathVariable String batchId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateBatchAccess(user, batchId);

        return ResponseEntity.ok(ApiResponse.<BatchContextDto>builder()
                .success(true)
                .data(academicService.getBatchContext(batchId))
                .build());
    }

    // --- Attainment Main High-level Report ---

    @GetMapping("/attainment-main")
    public ResponseEntity<ApiResponse<ProgrammeAttainmentDatasetDto>> getAttainmentMainReport(
            @RequestParam String programmeId,
            @RequestParam String batchId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateProgrammeAccess(user, programmeId);
        reportAccessService.validateBatchAccess(user, batchId);

        return ResponseEntity.ok(ApiResponse.<ProgrammeAttainmentDatasetDto>builder()
                .success(true)
                .data(attainmentCalculationService.getProgrammeAttainmentDataset(programmeId, batchId))
                .build());
    }

    @GetMapping("/attainment-main/course/{courseOfferingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseAttainmentMainDetail(
            @PathVariable String courseOfferingId,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, courseOfferingId);

        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
                .orElseThrow(() -> new com.dypiu.nba.exception.ResourceNotFoundException("Course Offering not found: " + courseOfferingId));

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(attainmentCalculationService.calculateCourseCoAttainment(offering.getId()))
                .build());
    }

    // --- Summary Endpoint ---
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportsSummary(
            @RequestParam(required = false) String programmeId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String batchId,
            Principal principal) {
        String pId = programmeId != null && !programmeId.isBlank() ? programmeId : "prog-1";
        String bId = batchId != null && !batchId.isBlank() ? batchId : "batch-comp-2025-29";
        String cId = courseId != null && !courseId.isBlank() ? courseId : "crs-1";

        Map<String, Object> coAtt = attainmentCalculationService.calculateCourseCoAttainment(cId);
        com.dypiu.nba.dto.CourseMappingMatrixDto matrixDto = outcomeService.getCourseMappings(cId);
        com.dypiu.nba.dto.ProgrammeAttainmentResultDto progAtt = attainmentCalculationService.calculateProgrammeAttainment(pId, bId);

        Map<String, Object> courseAttainment = new LinkedHashMap<>();
        courseAttainment.put("courseId", cId);
        courseAttainment.put("batchId", bId);
        courseAttainment.put("directAttainment", coAtt.get("directAttainment"));
        courseAttainment.put("indirectAttainment", coAtt.get("indirectAttainment"));
        courseAttainment.put("overallAttainment", coAtt.get("overallCoAttainment"));
        courseAttainment.put("coList", coAtt.get("coAttainments"));
        courseAttainment.put("matrix", matrixDto.getMatrix());

        Map<String, Object> programmeAttainment = new LinkedHashMap<>();
        programmeAttainment.put("programmeId", pId);
        programmeAttainment.put("batchId", bId);
        programmeAttainment.put("poAttainment", progAtt != null ? progAtt.getPoAttainments() : Collections.emptyMap());
        programmeAttainment.put("psoAttainment", progAtt != null ? progAtt.getPsoAttainments() : Collections.emptyMap());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("courseAttainment", courseAttainment);
        data.put("programmeAttainment", programmeAttainment);

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .data(data)
                .build());
    }

    // --- Generic Reports Export ---
    @GetMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportReportsExcel(
            @RequestParam(required = false) String programmeId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String reportType,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        if (courseId != null && !courseId.isBlank()) {
            if (courseOfferingRepository.existsById(courseId)) {
                reportAccessService.validateCourseOfferingAccess(user, courseId);
            } else {
                reportAccessService.validateCourseAccess(user, courseId);
            }
        }
        String targetCourseId = courseId != null && !courseId.isBlank() ? courseId : "crs-1";
        byte[] excelBytes = exportService.generateAttainmentExcel(targetCourseId, batchId);
        String filename = "Attainment_Report_" + targetCourseId + ".xlsx";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping(value = "/export/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportReportsPdf(
            @RequestParam(required = false) String programmeId,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String reportType,
            Principal principal) {
        User user = reportAccessService.getAuthenticatedUser(principal);
        if (courseId != null && !courseId.isBlank()) {
            if (courseOfferingRepository.existsById(courseId)) {
                reportAccessService.validateCourseOfferingAccess(user, courseId);
            } else {
                reportAccessService.validateCourseAccess(user, courseId);
            }
        }
        String targetCourseId = courseId != null && !courseId.isBlank() ? courseId : "crs-1";
        byte[] pdfBytes = exportService.generateAttainmentPdf(targetCourseId, batchId);
        String filename = "Attainment_Report_" + targetCourseId + ".pdf";

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
