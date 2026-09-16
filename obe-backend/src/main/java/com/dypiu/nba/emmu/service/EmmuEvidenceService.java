package com.dypiu.nba.emmu.service;

import com.dypiu.nba.dto.CourseAtrReportDto;
import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.dto.CourseMappingMatrixDto;
import com.dypiu.nba.dto.ProgrammeAtrReportDto;
import com.dypiu.nba.dto.ProgrammeAttainmentResultDto;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
import com.dypiu.nba.dto.analytics.*;
import com.dypiu.nba.emmu.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import com.dypiu.nba.service.AnalyticsService;
import com.dypiu.nba.service.AtrService;
import com.dypiu.nba.service.AttainmentCalculationService;
import com.dypiu.nba.service.OutcomeService;
import com.dypiu.nba.service.ReportAccessService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmmuEvidenceService {

    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final ProgrammeBatchAttainmentReportRepository programmeBatchAttainmentReportRepository;
    private final CourseAttainmentReportRepository courseAttainmentReportRepository;
    private final StudentCoMarkRepository studentCoMarkRepository;
    private final ProgrammeBatchIndirectAssessmentRepository indirectAssessmentRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;
    private final CourseAtrRepository courseAtrRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;

    private final AnalyticsService analyticsService;
    private final AttainmentCalculationService attainmentCalculationService;
    private final OutcomeService outcomeService;
    private final AtrService atrService;
    private final CurrentUserScopeService currentUserScopeService;
    private final ReportAccessService reportAccessService;
    private final ObjectMapper objectMapper;

    // ==========================================
    // 1. CONTEXT SUMMARY TOOL
    // ==========================================
    public EmmuContextSummaryDto getContextSummary(
            String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId,
            Principal principal) {

        ResolvedEmmuScope resolvedScope = resolveAndAuthorizeScope(schoolId, departmentId, masterProgrammeId, programmeBatchId, null, principal);
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);

        // 1. Fetch authoritative Analytics KPIs
        AnalyticsKpiResponseDto kpis = analyticsService.getKpis(
                resolvedScope.effectiveSchoolId,
                resolvedScope.effectiveDepartmentId,
                resolvedScope.effectiveMasterProgrammeId,
                resolvedScope.effectiveProgrammeBatchId
        );

        // 2. Fetch Top Deficits (up to 5)
        List<AttentionAreaItemDto> topDeficits = analyticsService.getAttentionAreas(
                resolvedScope.effectiveSchoolId,
                resolvedScope.effectiveDepartmentId,
                resolvedScope.effectiveMasterProgrammeId,
                resolvedScope.effectiveProgrammeBatchId,
                5,
                "ALL"
        );

        // 3. Compute Data Currency & Coverage
        EmmuDataCurrency dataCurrency = EmmuDataCurrency.NO_EVALUATED_DATA;
        if (kpis != null && kpis.getScopeSummary() != null && kpis.getScopeSummary().getDataSourceCurrency() != null) {
            String currencyStr = kpis.getScopeSummary().getDataSourceCurrency();
            if ("FINALIZED_EVALUATED_DATA".equalsIgnoreCase(currencyStr)) {
                dataCurrency = EmmuDataCurrency.FINALIZED_EVALUATED_DATA;
            } else if ("CONTINUOUS_MONITORING_DATA".equalsIgnoreCase(currencyStr)) {
                dataCurrency = EmmuDataCurrency.CONTINUOUS_MONITORING_DATA;
            }
        }

        // Aggregate coverage across batches in scope
        List<ProgrammeBatch> batchesInScope = getBatchesInScope(resolvedScope);
        List<String> batchIds = batchesInScope.stream().map(ProgrammeBatch::getId).toList();

        List<ProgrammeBatchCourse> coursesInScope = batchIds.isEmpty() ? Collections.emptyList() :
                programmeBatchCourseRepository.findByProgrammeBatchIdInAndDeletedAtIsNull(batchIds);

        int totalCourses = coursesInScope.size();
        Set<String> evaluatedOfferingIds = new HashSet<>();
        if (!coursesInScope.isEmpty()) {
            List<String> offeringIds = coursesInScope.stream().map(ProgrammeBatchCourse::getId).toList();
            courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offeringIds)
                    .forEach(r -> evaluatedOfferingIds.add(r.getProgrammeBatchCourseId()));
            studentCoMarkRepository.findByProgrammeBatchCourseIdIn(offeringIds)
                    .forEach(m -> evaluatedOfferingIds.add(m.getProgrammeBatchCourseId()));
        }
        int evaluatedCourses = evaluatedOfferingIds.size();
        BigDecimal coveragePct = totalCourses > 0
                ? BigDecimal.valueOf((evaluatedCourses * 100.0) / totalCourses).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        int totalStudentsEvaluated = 0;
        if (!coursesInScope.isEmpty()) {
            List<String> offeringIds = coursesInScope.stream().map(ProgrammeBatchCourse::getId).toList();
            totalStudentsEvaluated = studentCoMarkRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                    .map(StudentCoMark::getPrn)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                    .size();
        }

        int indirectCount = batchIds.isEmpty() ? 0 :
                batchIds.stream().mapToInt(bId -> indirectAssessmentRepository.findByProgrammeBatchIdOrderByCreatedAtAsc(bId).size()).sum();

        EmmuContextSummaryDto.EmmuCoverageDto coverageDto = EmmuContextSummaryDto.EmmuCoverageDto.builder()
                .totalCourseOfferings(totalCourses)
                .evaluatedCourseOfferings(evaluatedCourses)
                .evaluationCoveragePercentage(coveragePct)
                .totalStudentsEvaluated(totalStudentsEvaluated)
                .indirectAssessmentCount(indirectCount)
                .build();

        // 4. Outcome Summary
        int poEvaluated = kpis != null && kpis.getPoTargetAchievement() != null ? kpis.getPoTargetAchievement().getTotalEvaluatedInstances() : 0;
        int poMet = kpis != null && kpis.getPoTargetAchievement() != null ? kpis.getPoTargetAchievement().getTargetMetInstances() : 0;
        int poDeficits = kpis != null && kpis.getPoTargetAchievement() != null ? kpis.getPoTargetAchievement().getTargetDeficitInstances() : 0;

        int psoEvaluated = kpis != null && kpis.getPsoTargetAchievement() != null ? kpis.getPsoTargetAchievement().getTotalEvaluatedInstances() : 0;
        int psoMet = kpis != null && kpis.getPsoTargetAchievement() != null ? kpis.getPsoTargetAchievement().getTargetMetInstances() : 0;
        int psoDeficits = kpis != null && kpis.getPsoTargetAchievement() != null ? kpis.getPsoTargetAchievement().getTargetDeficitInstances() : 0;

        EmmuContextSummaryDto.EmmuOutcomeSummaryDto outcomeSummaryDto = EmmuContextSummaryDto.EmmuOutcomeSummaryDto.builder()
                .poEvaluated(poEvaluated)
                .poMeetingTarget(poMet)
                .poDeficitCount(poDeficits)
                .psoEvaluated(psoEvaluated)
                .psoMeetingTarget(psoMet)
                .psoDeficitCount(psoDeficits)
                .build();

        // 5. Pending Approvals Summary
        Map<String, Long> pendingBreakdown = new LinkedHashMap<>();
        long totalPending = 0;
        if (resolvedScope.effectiveMasterProgrammeId != null) {
            long alloc = approvalRequestRepository.countByTypeInAndMasterProgrammeIdInAndStatus(
                    List.of(ApprovalType.COURSE_ALLOCATION, ApprovalType.COURSE_OFFERING),
                    List.of(resolvedScope.effectiveMasterProgrammeId), ApprovalStatus.PENDING);
            long targets = approvalRequestRepository.countByTypeAndMasterProgrammeIdInAndStatus(
                    ApprovalType.PO_PSO_TARGETS, List.of(resolvedScope.effectiveMasterProgrammeId), ApprovalStatus.PENDING);
            long progAtr = approvalRequestRepository.countByTypeAndMasterProgrammeIdInAndStatus(
                    ApprovalType.PROGRAMME_ATR, List.of(resolvedScope.effectiveMasterProgrammeId), ApprovalStatus.PENDING);
            pendingBreakdown.put("courseAllocations", alloc);
            pendingBreakdown.put("programmeTargets", targets);
            pendingBreakdown.put("programmeAtr", progAtr);
            totalPending = alloc + targets + progAtr;
        } else {
            totalPending = approvalRequestRepository.findByStatus(ApprovalStatus.PENDING).size();
            pendingBreakdown.put("totalPendingInScope", totalPending);
        }

        EmmuContextSummaryDto.EmmuPendingApprovalsSummaryDto pendingDto = EmmuContextSummaryDto.EmmuPendingApprovalsSummaryDto.builder()
                .totalPending(totalPending)
                .breakdown(pendingBreakdown)
                .build();

        // 6. Resolve Names for Scope
        String schoolName = resolvedScope.effectiveSchoolId != null ?
                schoolRepository.findById(resolvedScope.effectiveSchoolId).map(School::getName).orElse(null) : null;
        String deptName = resolvedScope.effectiveDepartmentId != null ?
                departmentRepository.findById(resolvedScope.effectiveDepartmentId).map(Department::getName).orElse(null) : null;
        String progName = resolvedScope.effectiveMasterProgrammeId != null ?
                masterProgrammeRepository.findById(resolvedScope.effectiveMasterProgrammeId).map(MasterProgramme::getName).orElse(null) : null;
        String batchName = resolvedScope.effectiveProgrammeBatchId != null ?
                programmeBatchRepository.findById(resolvedScope.effectiveProgrammeBatchId).map(ProgrammeBatch::getName).orElse(null) : null;

        EmmuContextSummaryDto.EmmuScopeDto scopeDto = EmmuContextSummaryDto.EmmuScopeDto.builder()
                .role(userScope != null && userScope.getRole() != null ? userScope.getRole().name() : "USER")
                .userEmail(userScope != null ? userScope.getEmail() : null)
                .schoolId(resolvedScope.effectiveSchoolId)
                .schoolName(schoolName)
                .departmentId(resolvedScope.effectiveDepartmentId)
                .departmentName(deptName)
                .masterProgrammeId(resolvedScope.effectiveMasterProgrammeId)
                .programmeName(progName)
                .programmeBatchId(resolvedScope.effectiveProgrammeBatchId)
                .batchName(batchName)
                .build();

        return EmmuContextSummaryDto.builder()
                .scope(scopeDto)
                .dataCurrency(dataCurrency)
                .coverage(coverageDto)
                .outcomeSummary(outcomeSummaryDto)
                .topDeficits(topDeficits)
                .atrSummary(kpis != null ? kpis.getAtrOperationalSummary() : null)
                .pendingApprovals(pendingDto)
                .provenance("AUTHORITATIVE_ANALYTICS_SERVICE")
                .build();
    }

    // ==========================================
    // 2. ROOT-CAUSE EVIDENCE DRILLDOWN TOOL
    // ==========================================
    public EmmuRootCauseDrilldownDto getRootCauseDrilldown(
            String programmeBatchId, String outcomeCode, String outcomeType, Principal principal) {

        if (outcomeCode == null || outcomeCode.isBlank()) {
            throw new BadRequestException("outcomeCode is required (e.g. 'PO3', 'PSO1')");
        }

        String targetOutcomeCode = outcomeCode.trim().toUpperCase();
        String targetOutcomeType = (outcomeType != null && !outcomeType.isBlank()) ? outcomeType.trim().toUpperCase() : (targetOutcomeCode.startsWith("PSO") ? "PSO" : "PO");

        ResolvedEmmuScope scope = resolveAndAuthorizeScope(null, null, null, programmeBatchId, null, principal);
        String targetBatchId = scope.effectiveProgrammeBatchId;

        if (targetBatchId == null || targetBatchId.isBlank()) {
            throw new BadRequestException("programmeBatchId could not be determined for your authorized scope.");
        }

        ProgrammeBatch batch = programmeBatchRepository.findById(targetBatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme batch not found: " + targetBatchId));

        // 1. Retrieve authoritative Outcome Attainment & Components
        ProgrammeBatchAttainmentReport report = programmeBatchAttainmentReportRepository.findByProgrammeBatchId(targetBatchId).orElse(null);
        boolean isFinalized = report != null && (report.getStatus() == ReportStatus.FINALIZED || report.getStatus() == ReportStatus.APPROVED);

        EmmuDataCurrency dataCurrency = isFinalized ? EmmuDataCurrency.FINALIZED_EVALUATED_DATA : EmmuDataCurrency.CONTINUOUS_MONITORING_DATA;

        EmmuRootCauseDrilldownDto.OutcomeDetailDto outcomeDetail = null;
        EmmuRootCauseDrilldownDto.DirectComponentDto directComp = null;
        EmmuRootCauseDrilldownDto.IndirectComponentDto indirectComp = null;

        if (isFinalized && report.getOverallAttainmentReportJson() != null && !report.getOverallAttainmentReportJson().isBlank()) {
            try {
                Map<String, Object> map = objectMapper.readValue(report.getOverallAttainmentReportJson(), new TypeReference<Map<String, Object>>() {});
                String key = "PSO".equalsIgnoreCase(targetOutcomeType) ? "pso" : "po";
                if (map.containsKey(key)) {
                    List<Map<String, Object>> rows = objectMapper.convertValue(map.get(key), new TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> r : rows) {
                        String code = r.get("poCode") != null ? r.get("poCode").toString() : (r.get("psoCode") != null ? r.get("psoCode").toString() : null);
                        if (code != null && code.equalsIgnoreCase(targetOutcomeCode)) {
                            BigDecimal att = r.get("finalAttainment") != null ? new BigDecimal(r.get("finalAttainment").toString()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                            BigDecimal tgt = r.get("targetLevel") != null ? new BigDecimal(r.get("targetLevel").toString()).setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                            BigDecimal dir = r.get("directAttainment") != null ? new BigDecimal(r.get("directAttainment").toString()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                            BigDecimal ind = r.get("indirectAttainment") != null ? new BigDecimal(r.get("indirectAttainment").toString()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                            BigDecimal gap = att.subtract(tgt).setScale(2, RoundingMode.HALF_UP);
                            BigDecimal pct = tgt.compareTo(BigDecimal.ZERO) > 0 ? att.divide(tgt, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

                            outcomeDetail = EmmuRootCauseDrilldownDto.OutcomeDetailDto.builder()
                                    .code(targetOutcomeCode)
                                    .type(targetOutcomeType)
                                    .statement(r.get("statement") != null ? r.get("statement").toString() : "Outcome " + targetOutcomeCode)
                                    .target(tgt)
                                    .attainment(att)
                                    .gap(gap)
                                    .achievementPercentage(pct)
                                    .targetMet(att.compareTo(tgt) >= 0)
                                    .build();

                            directComp = EmmuRootCauseDrilldownDto.DirectComponentDto.builder().attainment(dir).weight(new BigDecimal("0.80")).build();
                            indirectComp = EmmuRootCauseDrilldownDto.IndirectComponentDto.builder().attainment(ind).weight(new BigDecimal("0.20")).build();
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[EmmuEvidenceService] Error parsing finalized report for drilldown: {}", e.getMessage());
            }
        }

        // If not finalized or not found in report, calculate live
        if (outcomeDetail == null) {
            try {
                ProgrammeAttainmentResultDto calc = attainmentCalculationService.calculateProgrammeAttainment(batch.getMasterProgrammeId(), batch.getId());
                if (calc != null && calc.getOverallAttainment() != null) {
                    List<ProgrammeAttainmentResultDto.OutcomeAttainmentItem> list = "PSO".equalsIgnoreCase(targetOutcomeType)
                            ? calc.getOverallAttainment().getPsos()
                            : calc.getOverallAttainment().getPos();

                    if (list != null) {
                        for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : list) {
                            String code = it.getOutcomeCode() != null ? it.getOutcomeCode() : it.getPoCode();
                            if (code != null && code.equalsIgnoreCase(targetOutcomeCode)) {
                                BigDecimal att = it.getOverallAttainment() != null ? it.getOverallAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                                BigDecimal tgt = it.getTarget() != null ? it.getTarget().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                                BigDecimal dir = it.getDirectAttainment() != null ? it.getDirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                                BigDecimal ind = it.getIndirectAttainment() != null ? it.getIndirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                                BigDecimal gap = att.subtract(tgt).setScale(2, RoundingMode.HALF_UP);
                                BigDecimal pct = it.getAchievementPercentage() != null ? it.getAchievementPercentage().setScale(2, RoundingMode.HALF_UP) : (tgt.compareTo(BigDecimal.ZERO) > 0 ? att.divide(tgt, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

                                outcomeDetail = EmmuRootCauseDrilldownDto.OutcomeDetailDto.builder()
                                        .code(targetOutcomeCode)
                                        .type(targetOutcomeType)
                                        .statement(it.getOutcomeStatement())
                                        .target(tgt)
                                        .attainment(att)
                                        .gap(gap)
                                        .achievementPercentage(pct)
                                        .targetMet(att.compareTo(tgt) >= 0)
                                        .build();

                                directComp = EmmuRootCauseDrilldownDto.DirectComponentDto.builder().attainment(dir).weight(new BigDecimal("0.80")).build();
                                indirectComp = EmmuRootCauseDrilldownDto.IndirectComponentDto.builder().attainment(ind).weight(new BigDecimal("0.20")).build();
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[EmmuEvidenceService] Error calculating live attainment for drilldown: {}", e.getMessage());
            }
        }

        if (outcomeDetail == null) {
            outcomeDetail = EmmuRootCauseDrilldownDto.OutcomeDetailDto.builder()
                    .code(targetOutcomeCode)
                    .type(targetOutcomeType)
                    .statement("Outcome " + targetOutcomeCode)
                    .target(new BigDecimal("2.50"))
                    .attainment(BigDecimal.ZERO)
                    .gap(new BigDecimal("-2.50"))
                    .achievementPercentage(BigDecimal.ZERO)
                    .targetMet(false)
                    .build();
            directComp = EmmuRootCauseDrilldownDto.DirectComponentDto.builder().attainment(BigDecimal.ZERO).weight(new BigDecimal("0.80")).build();
            indirectComp = EmmuRootCauseDrilldownDto.IndirectComponentDto.builder().attainment(BigDecimal.ZERO).weight(new BigDecimal("0.20")).build();
        }

        // 2. Fetch Contributing Course Evidence
        List<CourseAssessmentEvidenceDto> courseEvidence = analyticsService.getCourseEvidence(targetBatchId, targetOutcomeCode, targetOutcomeType);

        // 3. Fetch Student Evidence Summaries for contributing courses
        List<EmmuRootCauseDrilldownDto.StudentEvidenceSummaryDto> studentEvidenceSummaries = new ArrayList<>();
        for (CourseAssessmentEvidenceDto cEv : courseEvidence) {
            if (cEv.getCourseOfferingId() != null && cEv.getCoCode() != null) {
                try {
                    StudentCoEvidenceResponseDto sResp = analyticsService.getStudentCoEvidence(cEv.getCourseOfferingId(), cEv.getCoCode());
                    if (sResp != null && sResp.getTotalStudentsEvaluated() > 0) {
                        studentEvidenceSummaries.add(EmmuRootCauseDrilldownDto.StudentEvidenceSummaryDto.builder()
                                .courseOfferingId(cEv.getCourseOfferingId())
                                .courseCode(cEv.getCourseCode())
                                .courseName(cEv.getCourseName())
                                .coCode(cEv.getCoCode())
                                .totalStudentsEvaluated(sResp.getTotalStudentsEvaluated())
                                .studentsMeetingThreshold(sResp.getStudentsMeetingThreshold())
                                .studentsBelowThreshold(sResp.getStudentsBelowThreshold())
                                .thresholdPercentage(sResp.getConfiguredThresholdPercentage())
                                .attainmentRatePercentage(sResp.getAttainmentRatePercentage())
                                .classAveragePercentage(sResp.getClassAveragePercentage())
                                .highestPercentage(sResp.getHighestPercentage())
                                .lowestPercentage(sResp.getLowestPercentage())
                                .scoreDistribution(sResp.getScoreDistribution())
                                .build());
                    }
                } catch (Exception ignored) {}
            }
        }

        // 4. Fetch ATR Evidence for this outcome
        ProgrammeAtr atr = programmeAtrRepository.findByProgrammeBatchId(targetBatchId).orElse(null);
        EmmuRootCauseDrilldownDto.AtrEvidenceDetailDto atrDetail = null;

        if (atr != null) {
            List<String> actions = parseAtrActions(atr.getObservationsJson(), targetOutcomeCode, targetOutcomeType);
            String observations = (atr.getVerificationComments() != null && !atr.getVerificationComments().isBlank()) ? atr.getVerificationComments() : "Recorded in Programme ATR";

            atrDetail = EmmuRootCauseDrilldownDto.AtrEvidenceDetailDto.builder()
                    .hasRecordedAtr(true)
                    .status(atr.getStatus() != null ? atr.getStatus().name() : "DRAFT")
                    .observations(observations)
                    .recordedActions(actions)
                    .submittedBy(atr.getSubmittedBy())
                    .submittedAt(atr.getSubmittedAt())
                    .verifiedBy(atr.getVerifiedBy())
                    .verifiedAt(atr.getVerifiedAt())
                    .approvedBy(atr.getApprovedBy())
                    .approvedAt(atr.getApprovedAt())
                    .verificationComments(atr.getVerificationComments())
                    .build();
        } else {
            atrDetail = EmmuRootCauseDrilldownDto.AtrEvidenceDetailDto.builder()
                    .hasRecordedAtr(false)
                    .status("NOT_RECORDED")
                    .observations("No Action Taken Report has been recorded for this batch.")
                    .recordedActions(Collections.emptyList())
                    .build();
        }

        // 5. Fetch Historical Trend Evidence for this outcome
        List<CohortOutcomeDataPointDto> historicalPoints = Collections.emptyList();
        try {
            List<ScopedTrendSeriesDto> trendSeries = analyticsService.getTrends(null, null, batch.getMasterProgrammeId(), 5);
            if (!trendSeries.isEmpty() && trendSeries.get(0).getCohortDataPoints() != null) {
                historicalPoints = trendSeries.get(0).getCohortDataPoints().stream()
                        .filter(p -> p.getOutcomeCode() != null && p.getOutcomeCode().equalsIgnoreCase(targetOutcomeCode))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("[EmmuEvidenceService] Historical trends retrieval skipped: {}", e.getMessage());
        }

        // 6. Coverage
        List<ProgrammeBatchCourse> courses = programmeBatchCourseRepository.findByProgrammeBatchIdAndDeletedAtIsNull(targetBatchId);
        int totalCourses = courses.size();
        Set<String> evalSet = new HashSet<>();
        if (!courses.isEmpty()) {
            List<String> offIds = courses.stream().map(ProgrammeBatchCourse::getId).toList();
            courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offIds).forEach(r -> evalSet.add(r.getProgrammeBatchCourseId()));
            studentCoMarkRepository.findByProgrammeBatchCourseIdIn(offIds).forEach(m -> evalSet.add(m.getProgrammeBatchCourseId()));
        }
        int evaluatedCourses = evalSet.size();
        BigDecimal coveragePct = totalCourses > 0
                ? BigDecimal.valueOf((evaluatedCourses * 100.0) / totalCourses).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        EmmuContextSummaryDto.EmmuCoverageDto coverageDto = EmmuContextSummaryDto.EmmuCoverageDto.builder()
                .totalCourseOfferings(totalCourses)
                .evaluatedCourseOfferings(evaluatedCourses)
                .evaluationCoveragePercentage(coveragePct)
                .indirectAssessmentCount(indirectAssessmentRepository.findByProgrammeBatchIdOrderByCreatedAtAsc(targetBatchId).size())
                .build();

        return EmmuRootCauseDrilldownDto.builder()
                .outcome(outcomeDetail)
                .dataCurrency(dataCurrency)
                .coverage(coverageDto)
                .direct(directComp)
                .indirect(indirectComp)
                .courseEvidence(courseEvidence)
                .studentEvidenceSummary(studentEvidenceSummaries)
                .atrEvidence(atrDetail)
                .historicalEvidence(historicalPoints)
                .provenance(isFinalized ? "FINALIZED_REPORT_SNAPSHOT" : "AUTHORITATIVE_OBE_CALCULATION_SERVICE")
                .build();
    }

    // ==========================================
    // 3. COURSE RANKINGS TOOL
    // ==========================================
    public EmmuCourseRankingDto getCourseRankings(
            String programmeBatchId, Integer semester, String sortBy, String direction, Principal principal) {

        ResolvedEmmuScope scope = resolveAndAuthorizeScope(null, null, null, programmeBatchId, null, principal);
        String targetBatchId = scope.effectiveProgrammeBatchId;

        if (targetBatchId == null || targetBatchId.isBlank()) {
            throw new BadRequestException("programmeBatchId could not be determined for your authorized scope.");
        }

        ProgrammeBatch batch = programmeBatchRepository.findById(targetBatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme batch not found: " + targetBatchId));

        MasterProgramme prog = masterProgrammeRepository.findById(batch.getMasterProgrammeId()).orElse(null);

        List<ProgrammeBatchCourse> courses = programmeBatchCourseRepository.findByProgrammeBatchIdAndDeletedAtIsNull(targetBatchId);
        if (semester != null && semester > 0) {
            courses = courses.stream().filter(c -> c.getSemester() != null && Objects.equals(c.getSemester(), semester)).toList();
        }

        List<String> offeringIds = courses.stream().map(ProgrammeBatchCourse::getId).toList();
        Map<String, CourseAttainmentReport> reportMap = offeringIds.isEmpty() ? Collections.emptyMap() :
                courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                        .collect(Collectors.toMap(CourseAttainmentReport::getProgrammeBatchCourseId, r -> r, (a, b) -> a));

        List<EmmuCourseRankingDto.CourseRankItemDto> items = new ArrayList<>();

        for (ProgrammeBatchCourse course : courses) {
            CourseAttainmentReport cReport = reportMap.get(course.getId());
            BigDecimal directAtt = BigDecimal.ZERO;
            BigDecimal indirectAtt = BigDecimal.ZERO;
            BigDecimal overallAtt = BigDecimal.ZERO;
            int totalCos = 0;
            int cosMet = 0;
            int cosBelow = 0;
            boolean hasMarks = false;

            if (cReport != null) {
                directAtt = cReport.getDirectAttainment() != null ? cReport.getDirectAttainment() : BigDecimal.ZERO;
                indirectAtt = cReport.getIndirectAttainment() != null ? cReport.getIndirectAttainment() : BigDecimal.ZERO;
                overallAtt = cReport.getOverallCoAttainment() != null ? cReport.getOverallCoAttainment() : BigDecimal.ZERO;

                List<CourseAttainmentReportDto.Table3Row> table3 = parseTable3CoAttainments(cReport.getTable3CoAttainmentJson());
                totalCos = table3.size();
                for (CourseAttainmentReportDto.Table3Row t3 : table3) {
                    if (Boolean.TRUE.equals(t3.getTargetMet())) {
                        cosMet++;
                    } else {
                        cosBelow++;
                    }
                }
                hasMarks = true;
            } else {
                // Try live calculation
                try {
                    Map<String, Object> calc = attainmentCalculationService.calculateCourseCoAttainment(course.getId());
                    if (calc != null) {
                        if (calc.get("directAttainment") instanceof BigDecimal b) directAtt = b;
                        if (calc.get("indirectAttainment") instanceof BigDecimal b) indirectAtt = b;
                        if (calc.get("overallCoAttainment") instanceof BigDecimal b) overallAtt = b;

                        Object coAttObj = calc.get("coAttainment");
                        if (coAttObj instanceof List<?> list) {
                            totalCos = list.size();
                            for (Object o : list) {
                                if (o instanceof Map<?, ?> m) {
                                    Boolean met = m.get("targetMet") instanceof Boolean b ? b : null;
                                    if (Boolean.TRUE.equals(met)) cosMet++;
                                    else cosBelow++;
                                }
                            }
                        }
                        hasMarks = overallAtt.compareTo(BigDecimal.ZERO) > 0 || totalCos > 0;
                    }
                } catch (Exception ignored) {}
            }

            items.add(EmmuCourseRankingDto.CourseRankItemDto.builder()
                    .programmeBatchCourseId(course.getId())
                    .masterCourseId(course.getMasterCourseId())
                    .courseCode(course.getEffectiveCourseCode() != null ? course.getEffectiveCourseCode() : course.getCode())
                    .courseName(course.getEffectiveCourseName() != null ? course.getEffectiveCourseName() : course.getName())
                    .semester(course.getSemester())
                    .coordinatorName(course.getCourseCoordinatorName() != null ? course.getCourseCoordinatorName() : course.getAssignedFaculty())
                    .directAttainment(directAtt.setScale(2, RoundingMode.HALF_UP))
                    .indirectAttainment(indirectAtt.setScale(2, RoundingMode.HALF_UP))
                    .overallAttainment(overallAtt.setScale(2, RoundingMode.HALF_UP))
                    .totalCos(totalCos)
                    .cosMeetingTarget(cosMet)
                    .cosBelowTarget(cosBelow)
                    .hasEvaluatedMarks(hasMarks)
                    .build());
        }

        // Sorting
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim().toLowerCase() : "attainment";
        boolean desc = direction == null || direction.isBlank() || "DESC".equalsIgnoreCase(direction.trim());

        Comparator<EmmuCourseRankingDto.CourseRankItemDto> comparator;
        if ("deficit".equals(sortField) || "cosbelowtarget".equals(sortField)) {
            comparator = Comparator.comparingInt(EmmuCourseRankingDto.CourseRankItemDto::getCosBelowTarget);
        } else if ("semester".equals(sortField)) {
            comparator = Comparator.comparing((EmmuCourseRankingDto.CourseRankItemDto r) -> r.getSemester() != null ? r.getSemester() : 0);
        } else if ("coursecode".equals(sortField)) {
            comparator = Comparator.comparing((EmmuCourseRankingDto.CourseRankItemDto r) -> r.getCourseCode() != null ? r.getCourseCode() : "");
        } else {
            // default: overallAttainment
            comparator = Comparator.comparing(EmmuCourseRankingDto.CourseRankItemDto::getOverallAttainment);
        }

        if (desc) {
            comparator = comparator.reversed();
        }
        items.sort(comparator);

        // Assign ranks
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setRank(i + 1);
        }

        int evaluatedCount = (int) items.stream().filter(EmmuCourseRankingDto.CourseRankItemDto::isHasEvaluatedMarks).count();

        return EmmuCourseRankingDto.builder()
                .masterProgrammeId(batch.getMasterProgrammeId())
                .programmeName(prog != null ? prog.getName() : "")
                .programmeBatchId(batch.getId())
                .batchName(batch.getName())
                .totalCourses(items.size())
                .evaluatedCourses(evaluatedCount)
                .sortBy(sortField)
                .direction(desc ? "DESC" : "ASC")
                .rankedCourses(items)
                .provenance("AUTHORITATIVE_COURSE_ATTAINMENT_RECORDS")
                .build();
    }

    // ==========================================
    // 4. COURSE INTELLIGENCE TOOL
    // ==========================================
    public EmmuCourseIntelligenceDto getCourseIntelligence(
            String programmeBatchCourseId, String masterCourseId, Principal principal) {

        String targetOfferingId = (programmeBatchCourseId != null && !programmeBatchCourseId.isBlank()) ? programmeBatchCourseId.trim() : null;

        if (targetOfferingId == null && masterCourseId != null && !masterCourseId.isBlank()) {
            List<ProgrammeBatchCourse> list = programmeBatchCourseRepository.findByMasterCourseIdAndDeletedAtIsNull(masterCourseId.trim());
            if (!list.isEmpty()) {
                targetOfferingId = list.get(0).getId();
            }
        }

        if (targetOfferingId == null || targetOfferingId.isBlank()) {
            throw new BadRequestException("programmeBatchCourseId or masterCourseId is required");
        }

        final String finalOfferingId = targetOfferingId;
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(finalOfferingId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + finalOfferingId));

        // Enforce RBAC & Scope Authorization
        User user = reportAccessService.getAuthenticatedUser(principal);
        reportAccessService.validateCourseOfferingAccess(user, offering.getId());

        ProgrammeBatch batch = offering.getProgrammeBatchId() != null ? programmeBatchRepository.findById(offering.getProgrammeBatchId()).orElse(null) : null;
        MasterProgramme prog = batch != null ? masterProgrammeRepository.findById(batch.getMasterProgrammeId()).orElse(null) : null;

        // 1. Fetch Course Attainment Report & Mappings
        CourseAttainmentReport cReport = courseAttainmentReportRepository.findByProgrammeBatchCourseId(offering.getId()).orElse(null);
        CourseMappingMatrixDto matrixDto = null;
        try {
            matrixDto = outcomeService.getCourseMappings(offering.getId());
        } catch (Exception ignored) {}

        BigDecimal directAtt = BigDecimal.ZERO;
        BigDecimal indirectAtt = BigDecimal.ZERO;
        BigDecimal overallAtt = BigDecimal.ZERO;
        List<EmmuCourseIntelligenceDto.CourseOutcomeIntelligenceDto> cosList = new ArrayList<>();

        if (cReport != null) {
            directAtt = cReport.getDirectAttainment() != null ? cReport.getDirectAttainment() : BigDecimal.ZERO;
            indirectAtt = cReport.getIndirectAttainment() != null ? cReport.getIndirectAttainment() : BigDecimal.ZERO;
            overallAtt = cReport.getOverallCoAttainment() != null ? cReport.getOverallCoAttainment() : BigDecimal.ZERO;

            List<CourseAttainmentReportDto.Table3Row> table3 = parseTable3CoAttainments(cReport.getTable3CoAttainmentJson());
            List<CourseAttainmentReportDto.Table1Row> table1 = parseTable1Mapping(cReport.getTable1MappingJson());

            Map<String, CourseAttainmentReportDto.Table1Row> t1Map = table1.stream()
                    .collect(Collectors.toMap(t -> t.getCoCode() != null ? t.getCoCode().toUpperCase().trim() : "", t -> t, (a, b) -> a));

            for (CourseAttainmentReportDto.Table3Row t3 : table3) {
                String coCode = t3.getCoCode() != null ? t3.getCoCode().toUpperCase().trim() : "";
                CourseAttainmentReportDto.Table1Row t1 = t1Map.get(coCode);

                cosList.add(EmmuCourseIntelligenceDto.CourseOutcomeIntelligenceDto.builder()
                        .coCode(t3.getCoCode())
                        .statement(t3.getStatement())
                        .targetLevel(t3.getTargetLevel() != null ? t3.getTargetLevel() : new BigDecimal("2.00"))
                        .directLevel(t3.getDirectLevel())
                        .indirectLevel(t3.getIndirectLevel())
                        .finalAttainment(t3.getFinalAttainment())
                        .targetMet(t3.getTargetMet())
                        .poMappings(t1 != null ? t1.getPoMappings() : Collections.emptyMap())
                        .psoMappings(t1 != null ? t1.getPsoMappings() : Collections.emptyMap())
                        .build());
            }
        } else {
            // Live calculation fallback
            try {
                Map<String, Object> calc = attainmentCalculationService.calculateCourseCoAttainment(offering.getId());
                if (calc != null) {
                    if (calc.get("directAttainment") instanceof BigDecimal b) directAtt = b;
                    if (calc.get("indirectAttainment") instanceof BigDecimal b) indirectAtt = b;
                    if (calc.get("overallCoAttainment") instanceof BigDecimal b) overallAtt = b;

                    Object coAttObj = calc.get("coAttainment");
                    if (coAttObj instanceof List<?> list) {
                        for (Object o : list) {
                            if (o instanceof Map<?, ?> m) {
                                String cCode = m.get("coCode") != null ? m.get("coCode").toString() : "";
                                String cStmt = m.get("statement") != null ? m.get("statement").toString() : "";
                                BigDecimal finAtt = m.get("finalAttainment") instanceof BigDecimal b ? b : (m.get("finalAttainment") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : BigDecimal.ZERO);
                                Integer dirLvl = m.get("directLevel") instanceof Integer i ? i : (m.get("directLevel") instanceof Number n ? n.intValue() : 0);
                                Integer indLvl = m.get("indirectLevel") instanceof Integer i ? i : (m.get("indirectLevel") instanceof Number n ? n.intValue() : 0);
                                BigDecimal tgt = m.get("target") instanceof BigDecimal b ? b : new BigDecimal("2.00");
                                Boolean met = m.get("targetMet") instanceof Boolean b ? b : null;

                                Map<String, Integer> poMap = new HashMap<>();
                                Map<String, Integer> psoMap = new HashMap<>();
                                if (matrixDto != null && matrixDto.getMatrix() != null && matrixDto.getMatrix().containsKey(cCode)) {
                                    Map<String, Integer> row = matrixDto.getMatrix().get(cCode);
                                    if (row != null) {
                                        for (Map.Entry<String, Integer> e : row.entrySet()) {
                                            if (e.getKey().toUpperCase().startsWith("PSO")) psoMap.put(e.getKey(), e.getValue());
                                            else if (e.getKey().toUpperCase().startsWith("PO")) poMap.put(e.getKey(), e.getValue());
                                        }
                                    }
                                }

                                cosList.add(EmmuCourseIntelligenceDto.CourseOutcomeIntelligenceDto.builder()
                                        .coCode(cCode)
                                        .statement(cStmt)
                                        .targetLevel(tgt)
                                        .directLevel(dirLvl)
                                        .indirectLevel(indLvl)
                                        .finalAttainment(finAtt)
                                        .targetMet(met)
                                        .poMappings(poMap)
                                        .psoMappings(psoMap)
                                        .build());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Student Evidence for each CO
        List<StudentCoEvidenceResponseDto> studentEvidenceList = new ArrayList<>();
        for (EmmuCourseIntelligenceDto.CourseOutcomeIntelligenceDto co : cosList) {
            try {
                StudentCoEvidenceResponseDto sEv = analyticsService.getStudentCoEvidence(offering.getId(), co.getCoCode());
                if (sEv != null) {
                    studentEvidenceList.add(sEv);
                }
            } catch (Exception ignored) {}
        }

        // 3. Course ATR
        CourseAtrReportDto courseAtr = null;
        try {
            courseAtr = atrService.getCourseAtrReport(offering.getId());
        } catch (Exception ignored) {}

        return EmmuCourseIntelligenceDto.builder()
                .programmeBatchCourseId(offering.getId())
                .masterCourseId(offering.getMasterCourseId())
                .courseCode(offering.getEffectiveCourseCode() != null ? offering.getEffectiveCourseCode() : offering.getCode())
                .courseName(offering.getEffectiveCourseName() != null ? offering.getEffectiveCourseName() : offering.getName())
                .semester(offering.getSemester())
                .courseCoordinatorName(offering.getCourseCoordinatorName() != null ? offering.getCourseCoordinatorName() : offering.getAssignedFaculty())
                .masterProgrammeId(batch != null ? batch.getMasterProgrammeId() : null)
                .programmeName(prog != null ? prog.getName() : null)
                .programmeBatchId(batch != null ? batch.getId() : null)
                .batchName(batch != null ? batch.getName() : null)
                .directAttainment(directAtt.setScale(2, RoundingMode.HALF_UP))
                .indirectAttainment(indirectAtt.setScale(2, RoundingMode.HALF_UP))
                .overallAttainment(overallAtt.setScale(2, RoundingMode.HALF_UP))
                .cos(cosList)
                .studentEvidenceList(studentEvidenceList)
                .courseAtr(courseAtr)
                .provenance("AUTHORITATIVE_COURSE_ATTAINMENT_AND_ATR_SERVICES")
                .build();
    }

    // ==========================================
    // HELPER & AUTHORIZATION SCOPE METHODS
    // ==========================================
    private ResolvedEmmuScope resolveAndAuthorizeScope(
            String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId,
            String programmeBatchCourseId, Principal principal) {

        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);

        String effSchool = schoolId != null && !schoolId.isBlank() ? schoolId.trim() : null;
        String effDept = departmentId != null && !departmentId.isBlank() ? departmentId.trim() : null;
        String effProg = masterProgrammeId != null && !masterProgrammeId.isBlank() ? masterProgrammeId.trim() : null;
        String effBatch = programmeBatchId != null && !programmeBatchId.isBlank() ? programmeBatchId.trim() : null;
        String effCourse = programmeBatchCourseId != null && !programmeBatchCourseId.isBlank() ? programmeBatchCourseId.trim() : null;

        if (userScope != null) {
            if (userScope.isDirector()) {
                String allowedSchool = userScope.getRequiredSchoolId();
                if (effSchool != null && !effSchool.equalsIgnoreCase(allowedSchool)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Unauthorized school scope.");
                }
                effSchool = allowedSchool;
            } else if (userScope.isHod()) {
                String allowedDept = userScope.getRequiredDepartmentId();
                if (effDept != null && !effDept.equalsIgnoreCase(allowedDept)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Unauthorized department scope.");
                }
                effDept = allowedDept;
                if (userScope.hasSchoolScope()) {
                    effSchool = userScope.getSchoolId();
                }
            } else if (userScope.isProgrammeCoordinator()) {
                String allowedProg = userScope.getRequiredMasterProgrammeId();
                if (effProg != null && !effProg.equalsIgnoreCase(allowedProg)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Unauthorized master programme scope.");
                }
                effProg = allowedProg;
                if (userScope.hasDepartmentScope()) {
                    effDept = userScope.getDepartmentId();
                }
                if (userScope.hasSchoolScope()) {
                    effSchool = userScope.getSchoolId();
                }
            } else if (userScope.isFaculty()) {
                // Course coordinator / faculty: verify course offering or batch assignment
                if (effCourse != null) {
                    ProgrammeBatchCourse pbc = programmeBatchCourseRepository.findById(effCourse).orElse(null);
                    if (pbc != null && pbc.getProgrammeBatchId() != null) {
                        effBatch = pbc.getProgrammeBatchId();
                    }
                }
            }
        }

        // Auto-infer missing parameters based on user scope defaults
        if (effBatch == null && effProg != null) {
            List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(effProg);
            effBatch = batches.stream().filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus())).map(ProgrammeBatch::getId).findFirst()
                    .orElse(batches.isEmpty() ? null : batches.get(0).getId());
        }

        if (effBatch == null && effDept != null) {
            List<MasterProgramme> progs = masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(effDept);
            if (!progs.isEmpty()) {
                effProg = progs.get(0).getId();
                List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(effProg);
                effBatch = batches.stream().filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus())).map(ProgrammeBatch::getId).findFirst()
                        .orElse(batches.isEmpty() ? null : batches.get(0).getId());
            }
        }

        if (effBatch == null && effSchool != null) {
            List<Department> depts = departmentRepository.findBySchoolId(effSchool);
            if (!depts.isEmpty()) {
                effDept = depts.get(0).getId();
                List<MasterProgramme> progs = masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(effDept);
                if (!progs.isEmpty()) {
                    effProg = progs.get(0).getId();
                    List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(effProg);
                    effBatch = batches.stream().filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus())).map(ProgrammeBatch::getId).findFirst()
                            .orElse(batches.isEmpty() ? null : batches.get(0).getId());
                }
            }
        }

        // For IQAC/Admin fallback to first active batch if nothing specified
        if (effBatch == null) {
            List<ProgrammeBatch> allBatches = programmeBatchRepository.findByDeletedAtIsNull();
            effBatch = allBatches.stream().filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus())).map(ProgrammeBatch::getId).findFirst()
                    .orElse(allBatches.isEmpty() ? null : allBatches.get(0).getId());
        }

        return new ResolvedEmmuScope(effSchool, effDept, effProg, effBatch, effCourse);
    }

    private List<ProgrammeBatch> getBatchesInScope(ResolvedEmmuScope scope) {
        if (scope.effectiveProgrammeBatchId != null) {
            return programmeBatchRepository.findById(scope.effectiveProgrammeBatchId).map(List::of).orElse(Collections.emptyList());
        }
        if (scope.effectiveMasterProgrammeId != null) {
            return programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(scope.effectiveMasterProgrammeId);
        }
        if (scope.effectiveDepartmentId != null) {
            List<String> progIds = masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(scope.effectiveDepartmentId).stream().map(MasterProgramme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : programmeBatchRepository.findByMasterProgrammeIdInAndDeletedAtIsNull(progIds);
        }
        if (scope.effectiveSchoolId != null) {
            List<String> deptIds = departmentRepository.findBySchoolId(scope.effectiveSchoolId).stream().map(Department::getId).toList();
            List<String> progIds = deptIds.isEmpty() ? Collections.emptyList() : masterProgrammeRepository.findByDepartmentIdInAndDeletedAtIsNull(deptIds).stream().map(MasterProgramme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : programmeBatchRepository.findByMasterProgrammeIdInAndDeletedAtIsNull(progIds);
        }
        return programmeBatchRepository.findByDeletedAtIsNull();
    }

    private List<String> parseAtrActions(String observationsJson, String outcomeCode, String outcomeType) {
        if (observationsJson == null || observationsJson.isBlank() || outcomeCode == null) {
            return Collections.emptyList();
        }
        try {
            ProgrammeAtrReportDto reportDto = objectMapper.readValue(observationsJson, ProgrammeAtrReportDto.class);
            List<ProgrammeAtrReportDto.OutcomeRow> rows = "PSO".equalsIgnoreCase(outcomeType) ? reportDto.getPsoOutcomes() : reportDto.getPoOutcomes();
            if (rows != null) {
                for (ProgrammeAtrReportDto.OutcomeRow row : rows) {
                    if (row.getOutcomeCode() != null && row.getOutcomeCode().equalsIgnoreCase(outcomeCode)) {
                        return row.getActions() != null ? row.getActions().stream().filter(a -> a != null && !a.isBlank()).toList() : Collections.emptyList();
                    }
                }
            }
        } catch (Exception ignored) {
            try {
                Map<String, Object> map = objectMapper.readValue(observationsJson, new TypeReference<Map<String, Object>>() {});
                String key = "PSO".equalsIgnoreCase(outcomeType) ? "psoOutcomes" : "poOutcomes";
                if (map.containsKey(key)) {
                    List<Map<String, Object>> outcomeList = objectMapper.convertValue(map.get(key), new TypeReference<List<Map<String, Object>>>() {});
                    if (outcomeList != null) {
                        for (Map<String, Object> row : outcomeList) {
                            Object code = row.get("outcomeCode");
                            if (code != null && code.toString().equalsIgnoreCase(outcomeCode)) {
                                Object actionsObj = row.get("actions");
                                if (actionsObj instanceof List<?> list) {
                                    return list.stream().map(Object::toString).filter(s -> !s.isBlank()).toList();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Not valid JSON
            }
        }
        return Collections.emptyList();
    }

    private List<CourseAttainmentReportDto.Table1Row> parseTable1Mapping(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<CourseAttainmentReportDto.Table1Row>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<CourseAttainmentReportDto.Table3Row> parseTable3CoAttainments(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<CourseAttainmentReportDto.Table3Row>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private record ResolvedEmmuScope(
            String effectiveSchoolId,
            String effectiveDepartmentId,
            String effectiveMasterProgrammeId,
            String effectiveProgrammeBatchId,
            String effectiveProgrammeBatchCourseId
    ) {}
}
