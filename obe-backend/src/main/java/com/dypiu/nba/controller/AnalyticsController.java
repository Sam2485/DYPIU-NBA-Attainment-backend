package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.analytics.*;
import com.dypiu.nba.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ROLE_IQAC', 'ROLE_SUPER_ADMIN', 'ROLE_DIRECTOR', 'ROLE_HOD', 'ROLE_PROGRAMME_COORDINATOR', 'ROLE_FACULTY')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/kpis")
    public ResponseEntity<ApiResponse<AnalyticsKpiResponseDto>> getKpis(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId) {

        log.debug("[AnalyticsController] getKpis: schoolId={}, departmentId={}, masterProgrammeId={}, programmeBatchId={}",
                schoolId, departmentId, masterProgrammeId, programmeBatchId);

        AnalyticsKpiResponseDto data = analyticsService.getKpis(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        return ResponseEntity.ok(ApiResponse.<AnalyticsKpiResponseDto>builder()
                .success(true)
                .message("Analytics KPIs retrieved successfully")
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
            @RequestParam(defaultValue = "programmeName") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        log.debug("[AnalyticsController] getProgrammeLandscape: schoolId={}, departmentId={}, masterProgrammeId={}, programmeBatchId={}, page={}, size={}",
                schoolId, departmentId, masterProgrammeId, programmeBatchId, page, size);

        ProgrammeLandscapeResponseDto data = analyticsService.getProgrammeLandscape(
                schoolId, departmentId, masterProgrammeId, programmeBatchId, page, size, query, statusFilter, sortBy, direction);
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
            @RequestParam(defaultValue = "4") int numCohorts) {

        log.debug("[AnalyticsController] getTrends: schoolId={}, departmentId={}, masterProgrammeId={}, numCohorts={}",
                schoolId, departmentId, masterProgrammeId, numCohorts);

        List<ScopedTrendSeriesDto> data = analyticsService.getTrends(schoolId, departmentId, masterProgrammeId, numCohorts);
        return ResponseEntity.ok(ApiResponse.<List<ScopedTrendSeriesDto>>builder()
                .success(true)
                .message("Historical trends retrieved successfully")
                .data(data)
                .build());
    }
}
