package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.analytics.*;
import com.dypiu.nba.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/analytics", "/api/v1/analytics"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('IQAC', 'SUPER_ADMIN', 'DIRECTOR', 'HOD', 'PROGRAMME_COORDINATOR', 'FACULTY', 'COURSE_COORDINATOR', 'ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<AnalyticsKpiResponseDto>> getKpis(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId,
            @RequestParam(required = false) String batchStatus) {

        log.debug("[AnalyticsController] getKpis: schoolId={}, departmentId={}, masterProgrammeId={}, programmeBatchId={}, batchStatus={}",
                schoolId, departmentId, masterProgrammeId, programmeBatchId, batchStatus);

        AnalyticsKpiResponseDto data = analyticsService.getKpis(schoolId, departmentId, masterProgrammeId, programmeBatchId, batchStatus);
        return ResponseEntity.ok(ApiResponse.<AnalyticsKpiResponseDto>builder()
                .success(true)
                .message("Analytics KPIs retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/batch-overview")
    public ResponseEntity<ApiResponse<BatchOverviewResponseDto>> getBatchOverview(
            @RequestParam String programmeBatchId) {

        log.debug("[AnalyticsController] getBatchOverview: programmeBatchId={}", programmeBatchId);

        BatchOverviewResponseDto data = analyticsService.getBatchOverview(programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<BatchOverviewResponseDto>builder()
                .success(true)
                .message("Batch analytics overview retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/po-health")
    public ResponseEntity<ApiResponse<List<PoHealthItemDto>>> getPoHealth(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId) {

        log.debug("[AnalyticsController] getPoHealth: schoolId={}, departmentId={}, masterProgrammeId={}, programmeBatchId={}",
                schoolId, departmentId, masterProgrammeId, programmeBatchId);

        List<PoHealthItemDto> data = analyticsService.getPoHealth(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<List<PoHealthItemDto>>builder()
                .success(true)
                .message("PO Health metrics retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/pso-health")
    public ResponseEntity<ApiResponse<List<PsoHealthItemDto>>> getPsoHealth(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId) {

        log.debug("[AnalyticsController] getPsoHealth: schoolId={}, departmentId={}, masterProgrammeId={}, programmeBatchId={}",
                schoolId, departmentId, masterProgrammeId, programmeBatchId);

        List<PsoHealthItemDto> data = analyticsService.getPsoHealth(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<List<PsoHealthItemDto>>builder()
                .success(true)
                .message("PSO Health metrics retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/programmes")
    public ResponseEntity<ApiResponse<ProgrammeLandscapeResponseDto>> getProgrammeLandscape(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "ALL") String statusFilter,
            @RequestParam(required = false) String batchStatus,
            @RequestParam(required = false) Boolean attentionOnly,
            @RequestParam(defaultValue = "programmeName") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        log.debug("[AnalyticsController] getProgrammeLandscape: schoolId={}, departmentId={}, masterProgrammeId={}, programmeBatchId={}, page={}, size={}, batchStatus={}, attentionOnly={}",
                schoolId, departmentId, masterProgrammeId, programmeBatchId, page, size, batchStatus, attentionOnly);

        ProgrammeLandscapeResponseDto data = analyticsService.getProgrammeLandscape(
                schoolId, departmentId, masterProgrammeId, programmeBatchId, page, size, query, statusFilter, batchStatus, attentionOnly, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.<ProgrammeLandscapeResponseDto>builder()
                .success(true)
                .message("Programme landscape retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/attention-areas")
    public ResponseEntity<ApiResponse<List<AttentionAreaItemDto>>> getAttentionAreas(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "ALL") String outcomeType) {

        log.debug("[AnalyticsController] getAttentionAreas: schoolId={}, departmentId={}, masterProgrammeId={}, programmeBatchId={}, limit={}, outcomeType={}",
                schoolId, departmentId, masterProgrammeId, programmeBatchId, limit, outcomeType);

        List<AttentionAreaItemDto> data = analyticsService.getAttentionAreas(
                schoolId, departmentId, masterProgrammeId, programmeBatchId, limit, outcomeType);
        return ResponseEntity.ok(ApiResponse.<List<AttentionAreaItemDto>>builder()
                .success(true)
                .message("Attention areas retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<ScopedTrendSeriesDto>>> getTrends(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) Integer numCohorts) {

        log.debug("[AnalyticsController] getTrends: schoolId={}, departmentId={}, masterProgrammeId={}, numCohorts={}",
                schoolId, departmentId, masterProgrammeId, numCohorts);

        List<ScopedTrendSeriesDto> data = analyticsService.getTrends(schoolId, departmentId, masterProgrammeId, numCohorts);
        return ResponseEntity.ok(ApiResponse.<List<ScopedTrendSeriesDto>>builder()
                .success(true)
                .message("Historical trends retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/historical-programme-attainment")
    public ResponseEntity<ApiResponse<HistoricalProgrammeAttainmentResponseDto>> getHistoricalProgrammeAttainment(
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId,
            @RequestParam(required = false) String outcomeCode) {

        log.debug("[AnalyticsController] getHistoricalProgrammeAttainment: masterProgrammeId={}, programmeBatchId={}, outcomeCode={}",
                masterProgrammeId, programmeBatchId, outcomeCode);

        HistoricalProgrammeAttainmentResponseDto data = analyticsService.getHistoricalProgrammeAttainment(masterProgrammeId, programmeBatchId, outcomeCode);
        return ResponseEntity.ok(ApiResponse.<HistoricalProgrammeAttainmentResponseDto>builder()
                .success(true)
                .message("Historical programme attainment retrieved successfully")
                .data(data)
                .build());
    }

    public ResponseEntity<ApiResponse<HistoricalProgrammeAttainmentResponseDto>> getHistoricalProgrammeAttainment(
            String masterProgrammeId, String outcomeCode) {
        return getHistoricalProgrammeAttainment(masterProgrammeId, null, outcomeCode);
    }

    @GetMapping("/compare-batches")
    public ResponseEntity<ApiResponse<BatchComparisonAnalyticsResponseDto>> compareBatches(
            @RequestParam String programmeBatchId1,
            @RequestParam String programmeBatchId2) {

        log.debug("[AnalyticsController] compareBatches: programmeBatchId1={}, programmeBatchId2={}",
                programmeBatchId1, programmeBatchId2);

        BatchComparisonAnalyticsResponseDto data = analyticsService.compareBatches(programmeBatchId1, programmeBatchId2);
        return ResponseEntity.ok(ApiResponse.<BatchComparisonAnalyticsResponseDto>builder()
                .success(true)
                .message("Batch comparison analytics retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/atr-intelligence")
    public ResponseEntity<ApiResponse<AtrIntelligenceResponseDto>> getAtrIntelligence(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId) {

        log.debug("[AnalyticsController] getAtrIntelligence: schoolId={}, departmentId={}, masterProgrammeId={}, programmeBatchId={}",
                schoolId, departmentId, masterProgrammeId, programmeBatchId);

        AtrIntelligenceResponseDto data = analyticsService.getAtrIntelligence(
                schoolId, departmentId, masterProgrammeId, programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<AtrIntelligenceResponseDto>builder()
                .success(true)
                .message("ATR intelligence retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/course-evidence")
    public ResponseEntity<ApiResponse<List<CourseAssessmentEvidenceDto>>> getCourseEvidence(
            @RequestParam String programmeBatchId,
            @RequestParam String outcomeCode,
            @RequestParam(defaultValue = "PO") String outcomeType) {

        log.debug("[AnalyticsController] getCourseEvidence: programmeBatchId={}, outcomeCode={}, outcomeType={}",
                programmeBatchId, outcomeCode, outcomeType);

        List<CourseAssessmentEvidenceDto> data = analyticsService.getCourseEvidence(
                programmeBatchId, outcomeCode, outcomeType);
        return ResponseEntity.ok(ApiResponse.<List<CourseAssessmentEvidenceDto>>builder()
                .success(true)
                .message("Course assessment evidence retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/outcome-direct-drilldown")
    public ResponseEntity<ApiResponse<OutcomeDirectDrilldownResponseDto>> getOutcomeDirectDrilldown(
            @RequestParam String programmeBatchId,
            @RequestParam String outcomeCode,
            @RequestParam(required = false, defaultValue = "PO") String outcomeType) {

        log.debug("[AnalyticsController] getOutcomeDirectDrilldown: programmeBatchId={}, outcomeCode={}, outcomeType={}",
                programmeBatchId, outcomeCode, outcomeType);

        OutcomeDirectDrilldownResponseDto data = analyticsService.getOutcomeDirectDrilldown(
                programmeBatchId, outcomeCode, outcomeType);
        return ResponseEntity.ok(ApiResponse.<OutcomeDirectDrilldownResponseDto>builder()
                .success(true)
                .message("Outcome direct attainment drill-down retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/outcome-indirect-drilldown")
    public ResponseEntity<ApiResponse<OutcomeIndirectDrilldownResponseDto>> getOutcomeIndirectDrilldown(
            @RequestParam String programmeBatchId,
            @RequestParam String outcomeCode,
            @RequestParam(required = false, defaultValue = "PO") String outcomeType) {

        log.debug("[AnalyticsController] getOutcomeIndirectDrilldown: programmeBatchId={}, outcomeCode={}, outcomeType={}",
                programmeBatchId, outcomeCode, outcomeType);

        OutcomeIndirectDrilldownResponseDto data = analyticsService.getOutcomeIndirectDrilldown(
                programmeBatchId, outcomeCode, outcomeType);
        return ResponseEntity.ok(ApiResponse.<OutcomeIndirectDrilldownResponseDto>builder()
                .success(true)
                .message("Outcome indirect attainment drill-down retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/student-evidence")
    public ResponseEntity<ApiResponse<StudentCoEvidenceResponseDto>> getStudentCoEvidence(
            @RequestParam String programmeBatchCourseId,
            @RequestParam String coCode) {

        log.debug("[AnalyticsController] getStudentCoEvidence: programmeBatchCourseId={}, coCode={}",
                programmeBatchCourseId, coCode);

        StudentCoEvidenceResponseDto data = analyticsService.getStudentCoEvidence(
                programmeBatchCourseId, coCode);
        return ResponseEntity.ok(ApiResponse.<StudentCoEvidenceResponseDto>builder()
                .success(true)
                .message("Student CO evidence retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/course-analytics")
    public ResponseEntity<ApiResponse<CourseAnalyticsResponseDto>> getCourseAnalytics(
            @RequestParam String programmeBatchCourseId,
            @RequestParam(required = false) String outcomeCode,
            @RequestParam(required = false, defaultValue = "PO") String outcomeType) {

        log.debug("[AnalyticsController] getCourseAnalytics: programmeBatchCourseId={}, outcomeCode={}, outcomeType={}",
                programmeBatchCourseId, outcomeCode, outcomeType);

        CourseAnalyticsResponseDto data = analyticsService.getCourseAnalytics(
                programmeBatchCourseId, outcomeCode, outcomeType);
        return ResponseEntity.ok(ApiResponse.<CourseAnalyticsResponseDto>builder()
                .success(true)
                .message("Course analytics retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/co-analytics")
    public ResponseEntity<ApiResponse<CoAnalyticsResponseDto>> getCoAnalytics(
            @RequestParam String programmeBatchCourseId,
            @RequestParam String coCode) {

        log.debug("[AnalyticsController] getCoAnalytics: programmeBatchCourseId={}, coCode={}",
                programmeBatchCourseId, coCode);

        CoAnalyticsResponseDto data = analyticsService.getCoAnalytics(
                programmeBatchCourseId, coCode);
        return ResponseEntity.ok(ApiResponse.<CoAnalyticsResponseDto>builder()
                .success(true)
                .message("CO analytics detail retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/historical-course-attainment")
    public ResponseEntity<ApiResponse<HistoricalCourseAttainmentResponseDto>> getHistoricalCourseAttainment(
            @RequestParam String programmeBatchCourseId,
            @RequestParam(required = false) String coCode) {

        log.debug("[AnalyticsController] getHistoricalCourseAttainment: programmeBatchCourseId={}, coCode={}",
                programmeBatchCourseId, coCode);

        HistoricalCourseAttainmentResponseDto data = analyticsService.getHistoricalCourseAttainment(
                programmeBatchCourseId, coCode);
        return ResponseEntity.ok(ApiResponse.<HistoricalCourseAttainmentResponseDto>builder()
                .success(true)
                .message("Historical course attainment retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/compare-courses")
    public ResponseEntity<ApiResponse<CourseComparisonAnalyticsResponseDto>> compareCourses(
            @RequestParam String programmeBatchCourseId1,
            @RequestParam String programmeBatchCourseId2) {

        log.debug("[AnalyticsController] compareCourses: programmeBatchCourseId1={}, programmeBatchCourseId2={}",
                programmeBatchCourseId1, programmeBatchCourseId2);

        CourseComparisonAnalyticsResponseDto data = analyticsService.compareCourses(
                programmeBatchCourseId1, programmeBatchCourseId2);
        return ResponseEntity.ok(ApiResponse.<CourseComparisonAnalyticsResponseDto>builder()
                .success(true)
                .message("Course comparison analytics retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/config/student-evidence-threshold")
    public ResponseEntity<ApiResponse<StudentEvidenceThresholdConfigDto>> getStudentEvidenceThresholdConfig() {
        StudentEvidenceThresholdConfigDto config = analyticsService.getStudentEvidenceThresholdConfig();
        return ResponseEntity.ok(ApiResponse.<StudentEvidenceThresholdConfigDto>builder()
                .success(true)
                .message("Student evidence threshold configuration retrieved successfully")
                .data(config)
                .build());
    }

    @PutMapping("/config/student-evidence-threshold")
    @PreAuthorize("hasAnyRole('IQAC', 'SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<StudentEvidenceThresholdConfigDto>> updateStudentEvidenceThresholdConfig(
            @Valid @RequestBody UpdateStudentEvidenceThresholdRequest request,
            Principal principal) {
        String user = principal != null ? principal.getName() : "IQAC Admin";
        StudentEvidenceThresholdConfigDto updated = analyticsService.updateStudentEvidenceThreshold(
                request.getThresholdPercentage(), user);
        return ResponseEntity.ok(ApiResponse.<StudentEvidenceThresholdConfigDto>builder()
                .success(true)
                .message("Student evidence threshold configuration updated successfully")
                .data(updated)
                .build());
    }
}
