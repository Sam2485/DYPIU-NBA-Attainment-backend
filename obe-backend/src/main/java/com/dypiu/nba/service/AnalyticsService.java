package com.dypiu.nba.service;

import com.dypiu.nba.dto.ConsolidatedIndirectAttainmentDto;
import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.dto.CourseMappingMatrixDto;
import com.dypiu.nba.dto.IndirectAssessmentDto;
import com.dypiu.nba.dto.ProgrammeAtrReportDto;
import com.dypiu.nba.dto.ProgrammeAttainmentResultDto;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
import com.dypiu.nba.dto.ProgrammeSurveyResultDto;
import com.dypiu.nba.dto.ExaminationAttainmentResultDto;
import com.dypiu.nba.dto.SurveyAttainmentResultDto;
import com.dypiu.nba.dto.analytics.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
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
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, noRollbackFor = Exception.class)
public class AnalyticsService {

    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final ProgrammeBatchAttainmentReportRepository programmeBatchAttainmentReportRepository;
    private final CourseAttainmentReportRepository courseAttainmentReportRepository;
    private final StudentCoMarkRepository studentCoMarkRepository;
    private final IqacAnalyticsConfigurationRepository iqacAnalyticsConfigurationRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final AttainmentCalculationService attainmentCalculationService;
    private final OutcomeService outcomeService;
    private final CurrentUserScopeService currentUserScopeService;
    private final ObjectMapper objectMapper;
    private final CourseAtrRepository courseAtrRepository;
    private final IndirectAssessmentService indirectAssessmentService;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final AttainmentReportService attainmentReportService;
    private final AttainmentConfigurationRepository attainmentConfigurationRepository;
    private final BatchLifecycleService batchLifecycleService;

    // ==========================================
    // 1. KPI ENDPOINT
    // ==========================================
    public AnalyticsKpiResponseDto getKpis(String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId) {
        return getKpis(schoolId, departmentId, masterProgrammeId, programmeBatchId, null);
    }

    public AnalyticsKpiResponseDto getKpis(String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId, String batchStatus) {
        ResolvedScope scope = validateAndResolveScope(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        List<ProgrammeBatch> batchesInScope = getBatchesInScope(scope);
        if (batchStatus != null && !batchStatus.isBlank() && !batchStatus.equalsIgnoreCase("ALL")) {
            batchesInScope = batchesInScope.stream()
                    .filter(b -> b.getDeletedAt() == null
                            && b.getStatus() != null
                            && b.getStatus().equalsIgnoreCase(batchStatus.trim()))
                    .toList();
        } else {
            batchesInScope = batchesInScope.stream()
                    .filter(b -> b.getDeletedAt() == null)
                    .toList();
        }
        List<String> batchIds = batchesInScope.stream().map(ProgrammeBatch::getId).toList();

        // 1. Scope summary
        long totalSchools = scope.schoolId != null ? 1 : schoolRepository.count();
        long totalDepts = scope.departmentId != null ? 1 : (scope.schoolId != null ? departmentRepository.findBySchoolId(scope.schoolId).size() : departmentRepository.count());
        long totalProgs = scope.masterProgrammeId != null ? 1 : (scope.departmentId != null ? masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(scope.departmentId).size() : masterProgrammeRepository.count());
        long totalBatches = batchesInScope.size();

        Map<String, ProgrammeBatchAttainmentReport> reportMap = batchIds.isEmpty() ? Collections.emptyMap() :
                programmeBatchAttainmentReportRepository.findByProgrammeBatchIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        List<ResolvedBatchAnalyticsData> resolvedBatches = batchesInScope.stream()
                .map(b -> resolveBatchData(b, reportMap))
                .toList();

        long totalEvaluatedBatches = resolvedBatches.stream().filter(r -> r.hasActiveData).count();
        long totalEvaluatedCourses = resolvedBatches.stream().mapToLong(r -> r.totalEvaluatedCourses).sum();

        boolean allFinalized = totalEvaluatedBatches > 0 && resolvedBatches.stream().filter(r -> r.hasActiveData).allMatch(r -> r.isFinalized);
        String currency = totalEvaluatedBatches == 0 ? "NO_EVALUATED_DATA" : (allFinalized ? "FINALIZED_EVALUATED_DATA" : "CONTINUOUS_MONITORING_DATA");

        AnalyticsKpiResponseDto.ScopeSummary scopeSummary = AnalyticsKpiResponseDto.ScopeSummary.builder()
                .totalSchools(totalSchools)
                .totalDepartments(totalDepts)
                .totalMasterProgrammes(totalProgs)
                .totalBatches(totalBatches)
                .totalEvaluatedBatches(totalEvaluatedBatches)
                .totalEvaluatedCourseOfferings(totalEvaluatedCourses)
                .dataSourceCurrency(currency)
                .build();

        // 2. PO & PSO Target Achievement
        int poTotalEvaluated = 0;
        int poTargetMet = 0;
        int poTargetDeficit = 0;

        int psoTotalEvaluated = 0;
        int psoTargetMet = 0;
        int psoTargetDeficit = 0;

        int cohortsFullyMeeting = 0;
        int cohortsWithGaps = 0;
        int totalEvaluatedCohorts = 0;

        for (ResolvedBatchAnalyticsData bData : resolvedBatches) {
            if (!bData.hasActiveData) {
                continue;
            }
            totalEvaluatedCohorts++;
            boolean cohortHasGap = false;

            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : bData.pos) {
                if (po.getFinalAttainment() == null || po.getTargetLevel() == null) continue;
                poTotalEvaluated++;
                boolean met = po.getFinalAttainment().compareTo(po.getTargetLevel()) >= 0;
                if (met) {
                    poTargetMet++;
                } else {
                    poTargetDeficit++;
                    cohortHasGap = true;
                }
            }

            for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : bData.psos) {
                if (pso.getFinalAttainment() == null || pso.getTargetLevel() == null) continue;
                psoTotalEvaluated++;
                boolean met = pso.getFinalAttainment().compareTo(pso.getTargetLevel()) >= 0;
                if (met) {
                    psoTargetMet++;
                } else {
                    psoTargetDeficit++;
                    cohortHasGap = true;
                }
            }

            if (cohortHasGap) {
                cohortsWithGaps++;
            } else {
                cohortsFullyMeeting++;
            }
        }

        BigDecimal poRate = poTotalEvaluated > 0
                ? BigDecimal.valueOf((poTargetMet * 100.0) / poTotalEvaluated).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal psoRate = psoTotalEvaluated > 0
                ? BigDecimal.valueOf((psoTargetMet * 100.0) / psoTotalEvaluated).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal cohortRate = totalEvaluatedCohorts > 0
                ? BigDecimal.valueOf((cohortsFullyMeeting * 100.0) / totalEvaluatedCohorts).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        AnalyticsKpiResponseDto.OutcomeTargetAchievement poAch = AnalyticsKpiResponseDto.OutcomeTargetAchievement.builder()
                .totalEvaluatedInstances(poTotalEvaluated)
                .targetMetInstances(poTargetMet)
                .targetDeficitInstances(poTargetDeficit)
                .achievementRatePercentage(poRate)
                .build();

        AnalyticsKpiResponseDto.OutcomeTargetAchievement psoAch = AnalyticsKpiResponseDto.OutcomeTargetAchievement.builder()
                .totalEvaluatedInstances(psoTotalEvaluated)
                .targetMetInstances(psoTargetMet)
                .targetDeficitInstances(psoTargetDeficit)
                .achievementRatePercentage(psoRate)
                .build();

        AnalyticsKpiResponseDto.ProgrammeCohortHealth cohortHealth = AnalyticsKpiResponseDto.ProgrammeCohortHealth.builder()
                .totalEvaluatedCohorts(totalEvaluatedCohorts)
                .cohortsFullyMeetingTargets(cohortsFullyMeeting)
                .cohortsWithGaps(cohortsWithGaps)
                .fullyMeetingTargetRatePercentage(cohortRate)
                .build();

        // 3. ATR Operational Summary
        List<ProgrammeAtr> atrs = batchIds.isEmpty() ? Collections.emptyList() : programmeAtrRepository.findByProgrammeBatchIdIn(batchIds);
        long totalAtrs = atrs.size();
        long approvedAtrs = atrs.stream().filter(a -> a.getStatus() == ProgrammeAtrStatus.APPROVED).count();
        long submittedAtrs = atrs.stream().filter(a -> a.getStatus() == ProgrammeAtrStatus.SUBMITTED
                || a.getStatus() == ProgrammeAtrStatus.SUBMITTED_FOR_VERIFICATION
                || a.getStatus() == ProgrammeAtrStatus.PENDING_APPROVAL
                || a.getStatus() == ProgrammeAtrStatus.VERIFIED).count();
        long draftAtrs = atrs.stream().filter(a -> a.getStatus() == ProgrammeAtrStatus.DRAFT).count();
        long revAtrs = atrs.stream().filter(a -> a.getStatus() == ProgrammeAtrStatus.REVISION_REQUESTED
                || a.getStatus() == ProgrammeAtrStatus.NEEDS_REVISION).count();
        long rejAtrs = atrs.stream().filter(a -> a.getStatus() == ProgrammeAtrStatus.REJECTED).count();

        AnalyticsKpiResponseDto.AtrOperationalSummary atrSummary = AnalyticsKpiResponseDto.AtrOperationalSummary.builder()
                .totalRecordedProgrammeAtrs(totalAtrs)
                .approvedProgrammeAtrs(approvedAtrs)
                .submittedProgrammeAtrs(submittedAtrs)
                .draftProgrammeAtrs(draftAtrs)
                .revisionRequestedProgrammeAtrs(revAtrs)
                .rejectedProgrammeAtrs(rejAtrs)
                .build();

        return AnalyticsKpiResponseDto.builder()
                .scopeSummary(scopeSummary)
                .poTargetAchievement(poAch)
                .psoTargetAchievement(psoAch)
                .programmeCohortHealth(cohortHealth)
                .atrOperationalSummary(atrSummary)
                .build();
    }

    // ==========================================
    // 2. PO HEALTH ENDPOINT
    // ==========================================
    public List<PoHealthItemDto> getPoHealth(String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId) {
        ResolvedScope scope = validateAndResolveScope(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        List<ProgrammeBatch> batchesInScope = getBatchesInScope(scope);
        List<String> batchIds = batchesInScope.stream().map(ProgrammeBatch::getId).toList();

        Map<String, ProgrammeBatchAttainmentReport> reportMap = batchIds.isEmpty() ? Collections.emptyMap() :
                programmeBatchAttainmentReportRepository.findByProgrammeBatchIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        List<ResolvedBatchAnalyticsData> resolvedBatches = batchesInScope.stream()
                .map(b -> resolveBatchData(b, reportMap))
                .toList();

        // Determine max PO count across all batches in scope (minimum 12)
        int maxPo = 12;
        for (ResolvedBatchAnalyticsData bData : resolvedBatches) {
            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : bData.pos) {
                if (po.getPoCode() != null) {
                    String c = po.getPoCode().toUpperCase().trim();
                    if (c.matches("^PO\\d+$")) {
                        try {
                            int num = Integer.parseInt(c.substring(2));
                            if (num > maxPo) {
                                maxPo = num;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        // Group evaluated PO instances by poCode (PO1 .. PO{maxPo})
        Map<String, List<PoInstanceData>> poInstances = new LinkedHashMap<>();
        for (int i = 1; i <= maxPo; i++) {
            poInstances.put("PO" + i, new ArrayList<>());
        }

        Map<String, String> poStatements = new HashMap<>();

        for (ResolvedBatchAnalyticsData bData : resolvedBatches) {
            if (!bData.hasActiveData) continue;
            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : bData.pos) {
                if (po.getPoCode() == null) continue;
                String code = po.getPoCode().toUpperCase().trim();
                if (po.getStatement() != null && !po.getStatement().isBlank()) {
                    poStatements.put(code, po.getStatement());
                }
                poInstances.putIfAbsent(code, new ArrayList<>());
                if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                    poInstances.get(code).add(new PoInstanceData(
                            bData.batchId,
                            po.getFinalAttainment(),
                            po.getTargetLevel(),
                            po.getDirectAttainment() != null ? po.getDirectAttainment() : BigDecimal.ZERO,
                            po.getIndirectAttainment() != null ? po.getIndirectAttainment() : BigDecimal.ZERO
                    ));
                }
            }
        }

        List<String> sortedPoKeys = new ArrayList<>(poInstances.keySet());
        sortedPoKeys.sort((a, b) -> {
            boolean aIsNum = a.matches("^PO\\d+$");
            boolean bIsNum = b.matches("^PO\\d+$");
            if (aIsNum && bIsNum) {
                return Integer.compare(Integer.parseInt(a.substring(2)), Integer.parseInt(b.substring(2)));
            }
            return a.compareToIgnoreCase(b);
        });

        List<PoHealthItemDto> result = new ArrayList<>();
        for (String poCode : sortedPoKeys) {
            List<PoInstanceData> list = poInstances.get(poCode);
            int evaluatedCount = list.size();
            int targetMetCount = 0;
            int targetDeficitCount = 0;

            double sumAttainment = 0;
            double sumTarget = 0;
            double sumDirect = 0;
            double sumIndirect = 0;

            for (PoInstanceData inst : list) {
                if (inst.attainment.compareTo(inst.target) >= 0) {
                    targetMetCount++;
                } else {
                    targetDeficitCount++;
                }
                sumAttainment += inst.attainment.doubleValue();
                sumTarget += inst.target.doubleValue();
                sumDirect += inst.direct.doubleValue();
                sumIndirect += inst.indirect.doubleValue();
            }

            BigDecimal avgAttainment = evaluatedCount > 0
                    ? BigDecimal.valueOf(sumAttainment / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgTarget = evaluatedCount > 0
                    ? BigDecimal.valueOf(sumTarget / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgGap = avgAttainment.subtract(avgTarget).setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgDirect = evaluatedCount > 0
                    ? BigDecimal.valueOf(sumDirect / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgIndirect = evaluatedCount > 0
                    ? BigDecimal.valueOf(sumIndirect / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal meanDivergence = avgDirect.subtract(avgIndirect).setScale(2, RoundingMode.HALF_UP);

            BigDecimal rate = evaluatedCount > 0
                    ? BigDecimal.valueOf((targetMetCount * 100.0) / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            String stmt = poStatements.getOrDefault(poCode, "Programme Outcome " + poCode);

            result.add(PoHealthItemDto.builder()
                    .poCode(poCode)
                    .poStatement(stmt)
                    .applicableCohortCount(evaluatedCount)
                    .evaluatedInstanceCount(evaluatedCount)
                    .targetMetCount(targetMetCount)
                    .targetDeficitCount(targetDeficitCount)
                    .achievementRatePercentage(rate)
                    .averageAttainment(avgAttainment)
                    .averageTarget(avgTarget)
                    .averageGap(avgGap)
                    .directAttainmentAverage(avgDirect)
                    .indirectAttainmentAverage(avgIndirect)
                    .meanDivergence(meanDivergence)
                    .build());
        }

        return result;
    }

    // ==========================================
    // 3. PSO HEALTH ENDPOINT
    // ==========================================
    public List<PsoHealthItemDto> getPsoHealth(String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId) {
        ResolvedScope scope = validateAndResolveScope(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        List<ProgrammeBatch> batchesInScope = getBatchesInScope(scope);
        List<String> batchIds = batchesInScope.stream().map(ProgrammeBatch::getId).toList();

        Map<String, ProgrammeBatchAttainmentReport> reportMap = batchIds.isEmpty() ? Collections.emptyMap() :
                programmeBatchAttainmentReportRepository.findByProgrammeBatchIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        List<ResolvedBatchAnalyticsData> resolvedBatches = batchesInScope.stream()
                .map(b -> resolveBatchData(b, reportMap))
                .toList();

        Map<String, List<PoInstanceData>> psoInstances = new LinkedHashMap<>();
        Map<String, String> psoStatements = new HashMap<>();

        for (ResolvedBatchAnalyticsData bData : resolvedBatches) {
            if (!bData.hasActiveData) continue;
            for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : bData.psos) {
                if (pso.getPsoCode() == null) continue;
                String code = pso.getPsoCode().toUpperCase().trim();
                if (pso.getStatement() != null && !pso.getStatement().isBlank()) {
                    psoStatements.put(code, pso.getStatement());
                }
                psoInstances.putIfAbsent(code, new ArrayList<>());
                if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                    psoInstances.get(code).add(new PoInstanceData(
                            bData.batchId,
                            pso.getFinalAttainment(),
                            pso.getTargetLevel(),
                            pso.getDirectAttainment() != null ? pso.getDirectAttainment() : BigDecimal.ZERO,
                            pso.getIndirectAttainment() != null ? pso.getIndirectAttainment() : BigDecimal.ZERO
                    ));
                }
            }
        }

        List<String> sortedPsoKeys = new ArrayList<>(psoInstances.keySet());
        sortedPsoKeys.sort((a, b) -> {
            boolean aIsNum = a.matches("^PSO\\d+$");
            boolean bIsNum = b.matches("^PSO\\d+$");
            if (aIsNum && bIsNum) {
                return Integer.compare(Integer.parseInt(a.substring(3)), Integer.parseInt(b.substring(3)));
            }
            return a.compareToIgnoreCase(b);
        });

        List<PsoHealthItemDto> result = new ArrayList<>();
        for (String psoCode : sortedPsoKeys) {
            List<PoInstanceData> list = psoInstances.get(psoCode);
            int evaluatedCount = list.size();
            int targetMetCount = 0;
            int targetDeficitCount = 0;

            double sumAttainment = 0;
            double sumTarget = 0;
            double sumDirect = 0;
            double sumIndirect = 0;

            for (PoInstanceData inst : list) {
                if (inst.attainment.compareTo(inst.target) >= 0) {
                    targetMetCount++;
                } else {
                    targetDeficitCount++;
                }
                sumAttainment += inst.attainment.doubleValue();
                sumTarget += inst.target.doubleValue();
                sumDirect += inst.direct.doubleValue();
                sumIndirect += inst.indirect.doubleValue();
            }

            BigDecimal avgAttainment = evaluatedCount > 0
                    ? BigDecimal.valueOf(sumAttainment / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgTarget = evaluatedCount > 0
                    ? BigDecimal.valueOf(sumTarget / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgGap = avgAttainment.subtract(avgTarget).setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgDirect = evaluatedCount > 0
                    ? BigDecimal.valueOf(sumDirect / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal avgIndirect = evaluatedCount > 0
                    ? BigDecimal.valueOf(sumIndirect / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            BigDecimal meanDivergence = avgDirect.subtract(avgIndirect).setScale(2, RoundingMode.HALF_UP);

            BigDecimal rate = evaluatedCount > 0
                    ? BigDecimal.valueOf((targetMetCount * 100.0) / evaluatedCount).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            String stmt = psoStatements.getOrDefault(psoCode, "Programme Specific Outcome " + psoCode);

            result.add(PsoHealthItemDto.builder()
                    .psoCode(psoCode)
                    .psoStatement(stmt)
                    .applicableCohortCount(evaluatedCount)
                    .evaluatedInstanceCount(evaluatedCount)
                    .targetMetCount(targetMetCount)
                    .targetDeficitCount(targetDeficitCount)
                    .achievementRatePercentage(rate)
                    .averageAttainment(avgAttainment)
                    .averageTarget(avgTarget)
                    .averageGap(avgGap)
                    .directAttainmentAverage(avgDirect)
                    .indirectAttainmentAverage(avgIndirect)
                    .meanDivergence(meanDivergence)
                    .build());
        }

        return result;
    }

    // ==========================================
    // 4. PROGRAMME LANDSCAPE ENDPOINT (1 ROW = 1 COHORT)
    // ==========================================
    public ProgrammeLandscapeResponseDto getProgrammeLandscape(
            String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId,
            int page, int size, String query, String statusFilter, String sortBy, String direction) {
        return getProgrammeLandscape(schoolId, departmentId, masterProgrammeId, programmeBatchId, page, size, query, statusFilter, null, null, sortBy, direction);
    }

    public ProgrammeLandscapeResponseDto getProgrammeLandscape(
            String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId,
            int page, int size, String query, String statusFilter, String batchStatus, Boolean attentionOnly, String sortBy, String direction) {

        boolean isAttentionOnly = Boolean.TRUE.equals(attentionOnly)
                || (statusFilter != null && (statusFilter.equalsIgnoreCase("ATTENTION_ONLY") || statusFilter.equalsIgnoreCase("ACTIVE_ATTENTION")));

        final String effectiveBatchStatus = (batchStatus != null && !batchStatus.isBlank())
                ? batchStatus
                : (statusFilter != null && statusFilter.equalsIgnoreCase("ACTIVE_ATTENTION") ? "ACTIVE" : null);

        ResolvedScope scope = validateAndResolveScope(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        List<ProgrammeBatch> allBatches = getBatchesInScope(scope);
        if (effectiveBatchStatus != null && !effectiveBatchStatus.isBlank() && !effectiveBatchStatus.equalsIgnoreCase("ALL")) {
            allBatches = allBatches.stream()
                    .filter(b -> b.getDeletedAt() == null
                            && b.getStatus() != null
                            && b.getStatus().equalsIgnoreCase(effectiveBatchStatus.trim()))
                    .toList();
        } else {
            allBatches = allBatches.stream()
                    .filter(b -> b.getDeletedAt() == null)
                    .toList();
        }

        List<String> batchIds = allBatches.stream().map(ProgrammeBatch::getId).toList();

        Map<String, ProgrammeBatchAttainmentReport> reportMap = batchIds.isEmpty() ? Collections.emptyMap() :
                programmeBatchAttainmentReportRepository.findByProgrammeBatchIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        Map<String, ProgrammeAtr> atrMap = batchIds.isEmpty() ? Collections.emptyMap() :
                programmeAtrRepository.findByProgrammeBatchIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ProgrammeAtr::getProgrammeBatchId, a -> a, (a, b) -> a));

        Map<String, MasterProgramme> progMap = masterProgrammeRepository.findAll().stream()
                .collect(Collectors.toMap(MasterProgramme::getId, p -> p, (a, b) -> a));

        Map<String, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a));

        Map<String, School> schoolMap = schoolRepository.findAll().stream()
                .collect(Collectors.toMap(School::getId, s -> s, (a, b) -> a));

        List<ProgrammeLandscapeRowDto> rows = new ArrayList<>();

        for (ProgrammeBatch batch : allBatches) {
            MasterProgramme prog = progMap.get(batch.getMasterProgrammeId());
            Department dept = (prog != null && prog.getDepartmentId() != null) ? deptMap.get(prog.getDepartmentId()) : null;
            School school = (dept != null && dept.getSchoolId() != null) ? schoolMap.get(dept.getSchoolId()) : null;

            ProgrammeBatchAttainmentReport report = reportMap.get(batch.getId());
            ProgrammeAtr atr = atrMap.get(batch.getId());

            ResolvedBatchAnalyticsData bData = resolveBatchData(batch, reportMap);

            int posEvaluated = 0;
            int posMet = 0;
            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : bData.pos) {
                if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                    posEvaluated++;
                    if (po.getFinalAttainment().compareTo(po.getTargetLevel()) >= 0) {
                        posMet++;
                    }
                }
            }

            int psosEvaluated = 0;
            int psosMet = 0;
            for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : bData.psos) {
                if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                    psosEvaluated++;
                    if (pso.getFinalAttainment().compareTo(pso.getTargetLevel()) >= 0) {
                        psosMet++;
                    }
                }
            }

            int poBelowTarget = Math.max(0, posEvaluated - posMet);
            int psoBelowTarget = Math.max(0, psosEvaluated - psosMet);
            int gapCount = poBelowTarget + psoBelowTarget;
            boolean hasGaps = gapCount > 0;

            String reportAvailability = bData.isFinalized ? "FINALIZED_REPORT_AVAILABLE" : (bData.hasActiveData ? "IN_PROGRESS_MONITORING" : "NO_FINALIZED_REPORT");
            String atrStatusStr = atr != null && atr.getStatus() != null ? atr.getStatus().name() : "NOT_RECORDED";
            ZonedDateTime finalizedTimestamp = (report != null && report.getApprovedAt() != null) ? report.getApprovedAt() : (report != null ? report.getUpdatedAt() : null);

            ProgrammeLandscapeRowDto row = ProgrammeLandscapeRowDto.builder()
                    .masterProgrammeId(batch.getMasterProgrammeId())
                    .programmeName(prog != null ? prog.getName() : "Unknown Programme")
                    .programmeCode(prog != null ? prog.getCode() : "")
                    .degreeAwarded(prog != null ? prog.getDegreeAwarded() : "")
                    .departmentId(dept != null ? dept.getId() : "")
                    .departmentName(dept != null ? dept.getName() : "")
                    .schoolId(school != null ? school.getId() : "")
                    .schoolName(school != null ? school.getName() : "")
                    .programmeBatchId(batch.getId())
                    .batchName(batch.getName())
                    .batchStatus(batch.getStatus())
                    .startYear(batch.getStartYear())
                    .endYear(batch.getEndYear())
                    .posEvaluated(posEvaluated)
                    .posMet(posMet)
                    .posTotal(12)
                    .psosEvaluated(psosEvaluated)
                    .psosMet(psosMet)
                    .psosTotal(3)
                    .poBelowTarget(poBelowTarget)
                    .psoBelowTarget(psoBelowTarget)
                    .gapCount(gapCount)
                    .hasGaps(hasGaps)
                    .reportAvailabilityStatus(reportAvailability)
                    .underlyingReportStatus(bData.reportStatus)
                    .atrStatus(atrStatusStr)
                    .finalizedAt(finalizedTimestamp)
                    .build();

            // Attention Only filtering
            if (isAttentionOnly && (!hasGaps || gapCount <= 0)) {
                continue;
            }

            // Text search filtering
            if (query != null && !query.isBlank()) {
                String q = query.toLowerCase().trim();
                boolean match = (row.getProgrammeName() != null && row.getProgrammeName().toLowerCase().contains(q))
                        || (row.getProgrammeCode() != null && row.getProgrammeCode().toLowerCase().contains(q))
                        || (row.getBatchName() != null && row.getBatchName().toLowerCase().contains(q))
                        || (row.getDepartmentName() != null && row.getDepartmentName().toLowerCase().contains(q));
                if (!match) continue;
            }

            // Status filter
            if (statusFilter != null && !statusFilter.isBlank()
                    && !statusFilter.equalsIgnoreCase("ALL")
                    && !statusFilter.equalsIgnoreCase("ATTENTION_ONLY")
                    && !statusFilter.equalsIgnoreCase("ACTIVE_ATTENTION")) {
                if (statusFilter.equalsIgnoreCase("ALL_TARGETS_MET") && (!bData.hasActiveData || hasGaps)) {
                    continue;
                } else if (statusFilter.equalsIgnoreCase("HAS_GAPS") && (!bData.hasActiveData || !hasGaps)) {
                    continue;
                } else if (statusFilter.equalsIgnoreCase("NO_FINALIZED_REPORT") && bData.isFinalized) {
                    continue;
                } else if (statusFilter.equalsIgnoreCase("FINALIZED") && !bData.isFinalized) {
                    continue;
                } else if (statusFilter.equalsIgnoreCase("IN_PROGRESS") && (bData.isFinalized || !bData.hasActiveData)) {
                    continue;
                }
            }

            rows.add(row);
        }

        // Sorting
        Comparator<ProgrammeLandscapeRowDto> comparator = Comparator.comparing(r -> r.getProgrammeName() != null ? r.getProgrammeName() : "");
        if (sortBy != null) {
            if (sortBy.equalsIgnoreCase("posMet")) {
                comparator = Comparator.comparingInt(ProgrammeLandscapeRowDto::getPosMet);
            } else if (sortBy.equalsIgnoreCase("gapCount")) {
                comparator = Comparator.comparingInt(ProgrammeLandscapeRowDto::getGapCount);
            } else if (sortBy.equalsIgnoreCase("departmentName")) {
                comparator = Comparator.comparing(r -> r.getDepartmentName() != null ? r.getDepartmentName() : "");
            } else if (sortBy.equalsIgnoreCase("startYear")) {
                comparator = Comparator.comparing(r -> r.getStartYear() != null ? r.getStartYear() : 0);
            }
        }

        if (direction != null && direction.equalsIgnoreCase("DESC")) {
            comparator = comparator.reversed();
        }
        rows.sort(comparator);

        // Pagination
        int totalElements = rows.size();
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(100, size));
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        int fromIndex = safePage * safeSize;
        List<ProgrammeLandscapeRowDto> pageContent = Collections.emptyList();
        if (fromIndex < totalElements) {
            int toIndex = Math.min(fromIndex + safeSize, totalElements);
            pageContent = rows.subList(fromIndex, toIndex);
        }

        return ProgrammeLandscapeResponseDto.builder()
                .content(pageContent)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .pageNumber(safePage)
                .pageSize(safeSize)
                .first(safePage == 0)
                .last(safePage >= totalPages - 1)
                .build();
    }

    // ==========================================
    // 5. ATTENTION AREAS ENDPOINT
    // ==========================================
    public List<AttentionAreaItemDto> getAttentionAreas(
            String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId,
            int limit, String outcomeType) {

        ResolvedScope scope = validateAndResolveScope(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        List<ProgrammeBatch> batchesInScope = getBatchesInScope(scope);
        List<String> batchIds = batchesInScope.stream().map(ProgrammeBatch::getId).toList();

        Map<String, MasterProgramme> progMap = masterProgrammeRepository.findAll().stream()
                .collect(Collectors.toMap(MasterProgramme::getId, p -> p, (a, b) -> a));
        Map<String, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a));
        Map<String, ProgrammeBatch> batchMap = batchesInScope.stream()
                .collect(Collectors.toMap(ProgrammeBatch::getId, b -> b, (a, b) -> a));
        Map<String, ProgrammeAtr> atrMap = batchIds.isEmpty() ? Collections.emptyMap() :
                programmeAtrRepository.findByProgrammeBatchIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ProgrammeAtr::getProgrammeBatchId, a -> a, (a, b) -> a));

        Map<String, ProgrammeBatchAttainmentReport> reportMap = batchIds.isEmpty() ? Collections.emptyMap() :
                programmeBatchAttainmentReportRepository.findByProgrammeBatchIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        List<AttentionAreaItemDto> allDeficits = new ArrayList<>();

        for (ProgrammeBatch batch : batchesInScope) {
            ResolvedBatchAnalyticsData bData = resolveBatchData(batch, reportMap);
            if (!bData.hasActiveData) continue;

            MasterProgramme prog = progMap.get(batch.getMasterProgrammeId());
            Department dept = prog != null && prog.getDepartmentId() != null ? deptMap.get(prog.getDepartmentId()) : null;
            ProgrammeAtr atr = atrMap.get(batch.getId());

            // Process PO deficits
            if (outcomeType == null || outcomeType.equalsIgnoreCase("ALL") || outcomeType.equalsIgnoreCase("PO")) {
                for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : bData.pos) {
                    if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                        BigDecimal gap = po.getFinalAttainment().subtract(po.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                        if (gap.compareTo(BigDecimal.ZERO) < 0) {
                            BigDecimal pct = po.getTargetLevel().compareTo(BigDecimal.ZERO) > 0
                                    ? po.getFinalAttainment().divide(po.getTargetLevel(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO;

                            allDeficits.add(AttentionAreaItemDto.builder()
                                    .id(batch.getId() + "_" + po.getPoCode())
                                    .masterProgrammeId(prog != null ? prog.getId() : "")
                                    .programmeName(prog != null ? prog.getName() : "")
                                    .programmeCode(prog != null ? prog.getCode() : "")
                                    .programmeBatchId(batch.getId())
                                    .batchName(batch.getName())
                                    .departmentName(dept != null ? dept.getName() : "")
                                    .outcomeCode(po.getPoCode())
                                    .outcomeType("PO")
                                    .outcomeStatement(po.getStatement() != null ? po.getStatement() : "Programme Outcome " + po.getPoCode())
                                    .configuredTarget(po.getTargetLevel())
                                    .attainedValue(po.getFinalAttainment())
                                    .gap(gap)
                                    .achievementPercentage(pct)
                                    .hasRecordedAtr(atr != null)
                                    .atrStatus(atr != null && atr.getStatus() != null ? atr.getStatus().name() : null)
                                    .recordedAtrObservations(atr != null ? (atr.getObservationsJson() != null ? atr.getObservationsJson() : atr.getVerificationComments()) : null)
                                    .contributingCourseEvidence(Collections.emptyList()) // populated for top items below
                                    .build());
                        }
                    }
                }
            }

            // Process PSO deficits
            if (outcomeType == null || outcomeType.equalsIgnoreCase("ALL") || outcomeType.equalsIgnoreCase("PSO")) {
                for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : bData.psos) {
                    if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                        BigDecimal gap = pso.getFinalAttainment().subtract(pso.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                        if (gap.compareTo(BigDecimal.ZERO) < 0) {
                            BigDecimal pct = pso.getTargetLevel().compareTo(BigDecimal.ZERO) > 0
                                    ? pso.getFinalAttainment().divide(pso.getTargetLevel(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO;

                            allDeficits.add(AttentionAreaItemDto.builder()
                                    .id(batch.getId() + "_" + pso.getPsoCode())
                                    .masterProgrammeId(prog != null ? prog.getId() : "")
                                    .programmeName(prog != null ? prog.getName() : "")
                                    .programmeCode(prog != null ? prog.getCode() : "")
                                    .programmeBatchId(batch.getId())
                                    .batchName(batch.getName())
                                    .departmentName(dept != null ? dept.getName() : "")
                                    .outcomeCode(pso.getPsoCode())
                                    .outcomeType("PSO")
                                    .outcomeStatement(pso.getStatement() != null ? pso.getStatement() : "Programme Specific Outcome " + pso.getPsoCode())
                                    .configuredTarget(pso.getTargetLevel())
                                    .attainedValue(pso.getFinalAttainment())
                                    .gap(gap)
                                    .achievementPercentage(pct)
                                    .hasRecordedAtr(atr != null)
                                    .atrStatus(atr != null && atr.getStatus() != null ? atr.getStatus().name() : null)
                                    .recordedAtrObservations(atr != null ? (atr.getObservationsJson() != null ? atr.getObservationsJson() : atr.getVerificationComments()) : null)
                                    .contributingCourseEvidence(Collections.emptyList())
                                    .build());
                        }
                    }
                }
            }
        }

        // Rank by gap ascending (most negative first)
        allDeficits.sort(Comparator.comparing(AttentionAreaItemDto::getGap));

        int safeLimit = Math.max(1, Math.min(50, limit));
        List<AttentionAreaItemDto> topDeficits = allDeficits.stream().limit(safeLimit).collect(Collectors.toList());

        // Attach contributing course evidence for the top deficits
        for (AttentionAreaItemDto item : topDeficits) {
            List<CourseAssessmentEvidenceDto> evidenceList = findContributingCourseEvidence(item.getProgrammeBatchId(), item.getOutcomeCode(), item.getOutcomeType());
            item.setContributingCourseEvidence(evidenceList);
        }

        return topDeficits;
    }

    private List<CourseAssessmentEvidenceDto> findContributingCourseEvidence(String programmeBatchId, String outcomeCode, String outcomeType) {
        List<ProgrammeBatchCourse> courses = programmeBatchCourseRepository.findByProgrammeBatchId(programmeBatchId);
        if (courses.isEmpty()) return Collections.emptyList();

        List<String> offeringIds = courses.stream().map(ProgrammeBatchCourse::getId).toList();
        List<CourseAttainmentReport> reports = courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offeringIds);
        Map<String, CourseAttainmentReport> reportMap = reports.stream()
                .collect(Collectors.toMap(CourseAttainmentReport::getProgrammeBatchCourseId, r -> r, (a, b) -> a));

        List<CourseAssessmentEvidenceDto> evidenceList = new ArrayList<>();
        String targetCode = outcomeCode.toUpperCase().trim();

        for (ProgrammeBatchCourse course : courses) {
            CourseAttainmentReport cReport = reportMap.get(course.getId());
            List<CourseAttainmentReportDto.Table1Row> table1 = null;
            List<CourseAttainmentReportDto.Table3Row> table3 = null;

            BigDecimal defaultOverall = BigDecimal.ZERO;
            BigDecimal defaultDirect = BigDecimal.ZERO;
            BigDecimal defaultIndirect = BigDecimal.ZERO;

            if (cReport != null) {
                table1 = parseTable1Mapping(cReport.getTable1MappingJson());
                table3 = parseTable3CoAttainments(cReport.getTable3CoAttainmentJson());
                defaultOverall = cReport.getOverallCoAttainment();
                defaultDirect = cReport.getDirectAttainment();
                defaultIndirect = cReport.getIndirectAttainment();
            } else {
                try {
                    CourseMappingMatrixDto matrixDto = outcomeService.getCourseMappings(course.getId());
                    if (matrixDto != null && matrixDto.getMatrix() != null) {
                        table1 = new ArrayList<>();
                        for (Map.Entry<String, Map<String, Integer>> entry : matrixDto.getMatrix().entrySet()) {
                            String coCode = entry.getKey();
                            Map<String, Integer> rowMap = entry.getValue();
                            Map<String, Integer> poMap = new LinkedHashMap<>();
                            Map<String, Integer> psoMap = new LinkedHashMap<>();
                            if (rowMap != null) {
                                for (Map.Entry<String, Integer> mEntry : rowMap.entrySet()) {
                                    String k = mEntry.getKey().toUpperCase().trim();
                                    if (k.startsWith("PSO")) {
                                        psoMap.put(k, mEntry.getValue());
                                    } else if (k.startsWith("PO")) {
                                        poMap.put(k, mEntry.getValue());
                                    }
                                }
                            }
                            table1.add(CourseAttainmentReportDto.Table1Row.builder()
                                    .coCode(coCode)
                                    .poMappings(poMap)
                                    .psoMappings(psoMap)
                                    .build());
                        }
                    }

                    Map<String, Object> calc = attainmentCalculationService.calculateCourseCoAttainment(course.getId());
                    if (calc != null) {
                        if (calc.get("overallCoAttainment") instanceof BigDecimal b) defaultOverall = b;
                        if (calc.get("directAttainment") instanceof BigDecimal b) defaultDirect = b;
                        if (calc.get("indirectAttainment") instanceof BigDecimal b) defaultIndirect = b;

                        Object coAttObj = calc.get("coAttainment");
                        if (coAttObj instanceof List<?> list) {
                            table3 = new ArrayList<>();
                            for (Object o : list) {
                                if (o instanceof Map<?, ?> m) {
                                    String cCode = m.get("coCode") != null ? m.get("coCode").toString() : null;
                                    String cStmt = m.get("statement") != null ? m.get("statement").toString() : null;
                                    BigDecimal finAtt = m.get("finalAttainment") instanceof BigDecimal b ? b : (m.get("finalAttainment") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : null);
                                    Integer dirLvl = m.get("directLevel") instanceof Integer i ? i : (m.get("directLevel") instanceof Number n ? n.intValue() : null);
                                    Integer indLvl = m.get("indirectLevel") instanceof Integer i ? i : (m.get("indirectLevel") instanceof Number n ? n.intValue() : null);
                                    BigDecimal tgt = m.get("target") instanceof BigDecimal b ? b : (m.get("target") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : null);
                                    Boolean tgtMet = m.get("targetMet") instanceof Boolean b ? b : null;

                                    table3.add(CourseAttainmentReportDto.Table3Row.builder()
                                            .coCode(cCode)
                                            .statement(cStmt)
                                            .finalAttainment(finAtt)
                                            .directLevel(dirLvl)
                                            .indirectLevel(indLvl)
                                            .targetLevel(tgt)
                                            .targetMet(tgtMet)
                                            .build());
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            if (table1 == null || table1.isEmpty()) continue;

            Map<String, CourseAttainmentReportDto.Table3Row> t3Map = (table3 != null) ? table3.stream()
                    .collect(Collectors.toMap(t -> t.getCoCode() != null ? t.getCoCode().toUpperCase().trim() : "", t -> t, (a, b) -> a))
                    : Collections.emptyMap();

            for (CourseAttainmentReportDto.Table1Row t1 : table1) {
                if (t1.getCoCode() == null) continue;
                String coCode = t1.getCoCode().toUpperCase().trim();
                Integer strength = null;
                if ("PO".equalsIgnoreCase(outcomeType) && t1.getPoMappings() != null) {
                    strength = t1.getPoMappings().get(targetCode);
                } else if ("PSO".equalsIgnoreCase(outcomeType) && t1.getPsoMappings() != null) {
                    strength = t1.getPsoMappings().get(targetCode);
                }

                if (strength != null && strength > 0) {
                    CourseAttainmentReportDto.Table3Row t3 = t3Map.get(coCode);
                    BigDecimal coOverall = t3 != null && t3.getFinalAttainment() != null ? t3.getFinalAttainment() : defaultOverall;
                    BigDecimal coDirect = t3 != null && t3.getDirectLevel() != null ? BigDecimal.valueOf(t3.getDirectLevel()) : defaultDirect;
                    BigDecimal coIndirect = t3 != null && t3.getIndirectLevel() != null ? BigDecimal.valueOf(t3.getIndirectLevel()) : defaultIndirect;
                    BigDecimal coTarget = t3 != null && t3.getTargetLevel() != null ? t3.getTargetLevel() : BigDecimal.valueOf(2.00);
                    Boolean targetMet = t3 != null ? t3.getTargetMet() : (coOverall != null && coTarget != null && coOverall.compareTo(coTarget) >= 0);

                    evidenceList.add(CourseAssessmentEvidenceDto.builder()
                            .courseOfferingId(course.getId())
                            .masterCourseId(course.getMasterCourseId())
                            .courseCode(course.getEffectiveCourseCode() != null ? course.getEffectiveCourseCode() : course.getCode())
                            .courseName(course.getEffectiveCourseName() != null ? course.getEffectiveCourseName() : course.getName())
                            .semester(course.getSemester())
                            .courseCoordinatorName(course.getCourseCoordinatorName() != null ? course.getCourseCoordinatorName() : course.getAssignedFaculty())
                            .coCode(t1.getCoCode())
                            .coOverallAttainment(coOverall != null ? coOverall.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .coDirectAttainment(coDirect != null ? coDirect.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .coIndirectAttainment(coIndirect != null ? coIndirect.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                            .coTarget(coTarget != null ? coTarget.setScale(2, RoundingMode.HALF_UP) : BigDecimal.valueOf(2.00))
                            .mappingStrength(strength)
                            .coTargetMet(targetMet)
                            .build());
                }
            }
        }

        return evidenceList;
    }

    public List<CourseAssessmentEvidenceDto> getCourseEvidence(String programmeBatchId, String outcomeCode, String outcomeType) {
        if (programmeBatchId == null || outcomeCode == null) return Collections.emptyList();
        validateAndResolveScope(null, null, null, programmeBatchId);
        return findContributingCourseEvidence(programmeBatchId, outcomeCode, outcomeType != null ? outcomeType : "PO");
    }

    // ==========================================
    // PROGRAMME DIRECT ATTAINMENT DRILL-DOWN
    // ==========================================
    public OutcomeDirectDrilldownResponseDto getOutcomeDirectDrilldown(String programmeBatchId, String outcomeCode, String outcomeType) {
        if (programmeBatchId == null || programmeBatchId.isBlank()) {
            throw new BadRequestException("programmeBatchId is required");
        }
        if (outcomeCode == null || outcomeCode.isBlank()) {
            throw new BadRequestException("outcomeCode is required");
        }

        String type = (outcomeType != null && !outcomeType.isBlank()) ? outcomeType.trim().toUpperCase() : "PO";
        if (!"PO".equals(type) && !"PSO".equals(type)) {
            throw new BadRequestException("Invalid outcomeType: " + outcomeType + ". Must be PO or PSO");
        }

        String targetCode = outcomeCode.trim().toUpperCase();

        // 1. Authorize & Resolve batch scope (hierarchical upward check)
        ResolvedScope scope = validateAndResolveScope(null, null, null, programmeBatchId);
        ProgrammeBatch batch = programmeBatchRepository.findById(scope.programmeBatchId)
                .orElseThrow(() -> new BadRequestException("ProgrammeBatch not found: " + scope.programmeBatchId));

        // 2. Resolve courses for this batch
        List<ProgrammeBatchCourse> courses = programmeBatchCourseRepository.findByProgrammeBatchId(batch.getId());
        courses.sort(Comparator.comparing((ProgrammeBatchCourse c) -> c.getSemester() != null ? c.getSemester() : 1)
                .thenComparing(c -> c.getCourseCode() != null ? c.getCourseCode() : ""));

        List<String> offeringIds = courses.stream().map(ProgrammeBatchCourse::getId).toList();
        Map<String, CourseAttainmentReport> courseReportMap = offeringIds.isEmpty() ? Collections.emptyMap() :
                courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                        .collect(Collectors.toMap(CourseAttainmentReport::getProgrammeBatchCourseId, r -> r, (a, b) -> a));

        // 3. Resolve batch attainment report / continuous calculation data
        ProgrammeBatchAttainmentReport report = programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId()).orElse(null);
        boolean isFinalized = report != null && (report.getStatus() == ReportStatus.FINALIZED || report.getStatus() == ReportStatus.APPROVED);

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> poRows = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psoRows = new ArrayList<>();
        Map<String, ProgrammeBatchAttainmentReportDto.CourseContributionRow> mappingRowMap = new HashMap<>();
        Map<String, ProgrammeBatchAttainmentReportDto.CourseContributionRow> directRowMap = new HashMap<>();

        if (isFinalized) {
            ParsedReport parsed = parseReport(report);
            poRows = parsed.pos();
            psoRows = parsed.psos();
            try {
                if (report.getAverageMappingReportJson() != null && !report.getAverageMappingReportJson().isBlank()) {
                    Map<String, Object> r1Map = objectMapper.readValue(report.getAverageMappingReportJson(), new TypeReference<Map<String, Object>>() {});
                    if (r1Map.containsKey("courses")) {
                        List<ProgrammeBatchAttainmentReportDto.CourseContributionRow> cRows = objectMapper.convertValue(
                                r1Map.get("courses"), new TypeReference<List<ProgrammeBatchAttainmentReportDto.CourseContributionRow>>() {});
                        for (ProgrammeBatchAttainmentReportDto.CourseContributionRow row : cRows) {
                            if (row.getProgrammeBatchCourseId() != null) mappingRowMap.put(row.getProgrammeBatchCourseId(), row);
                        }
                    }
                }
                if (report.getDirectAttainmentReportJson() != null && !report.getDirectAttainmentReportJson().isBlank()) {
                    Map<String, Object> r2Map = objectMapper.readValue(report.getDirectAttainmentReportJson(), new TypeReference<Map<String, Object>>() {});
                    if (r2Map.containsKey("courses")) {
                        List<ProgrammeBatchAttainmentReportDto.CourseContributionRow> cRows = objectMapper.convertValue(
                                r2Map.get("courses"), new TypeReference<List<ProgrammeBatchAttainmentReportDto.CourseContributionRow>>() {});
                        for (ProgrammeBatchAttainmentReportDto.CourseContributionRow row : cRows) {
                            if (row.getProgrammeBatchCourseId() != null) directRowMap.put(row.getProgrammeBatchCourseId(), row);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[AnalyticsService] Error parsing finalized course contribution rows for batch {}: {}", batch.getId(), e.getMessage());
            }
        } else if (!courses.isEmpty()) {
            try {
                ProgrammeAttainmentResultDto calcResult = attainmentCalculationService.calculateProgrammeAttainment(batch.getMasterProgrammeId(), batch.getId());
                if (calcResult != null) {
                    if (calcResult.getOverallAttainment() != null) {
                        if (calcResult.getOverallAttainment().getPos() != null) {
                            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : calcResult.getOverallAttainment().getPos()) {
                                String code = it.getPoCode() != null ? it.getPoCode() : it.getOutcomeCode();
                                if (code == null) continue;
                                poRows.add(ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                                        .poCode(code)
                                        .statement(it.getOutcomeStatement())
                                        .targetLevel(it.getTarget() != null ? it.getTarget() : new BigDecimal("2.50"))
                                        .directAttainment(it.getDirectAttainment() != null ? it.getDirectAttainment() : BigDecimal.ZERO)
                                        .indirectAttainment(it.getIndirectAttainment() != null ? it.getIndirectAttainment() : BigDecimal.ZERO)
                                        .finalAttainment(it.getOverallAttainment() != null ? it.getOverallAttainment() : BigDecimal.ZERO)
                                        .observation(it.getObservation())
                                        .build());
                            }
                        }
                        if (calcResult.getOverallAttainment().getPsos() != null) {
                            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : calcResult.getOverallAttainment().getPsos()) {
                                String code = it.getPsoCode() != null ? it.getPsoCode() : it.getOutcomeCode();
                                if (code == null) continue;
                                psoRows.add(ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder()
                                        .psoCode(code)
                                        .statement(it.getOutcomeStatement())
                                        .targetLevel(it.getTarget() != null ? it.getTarget() : new BigDecimal("2.50"))
                                        .directAttainment(it.getDirectAttainment() != null ? it.getDirectAttainment() : BigDecimal.ZERO)
                                        .indirectAttainment(it.getIndirectAttainment() != null ? it.getIndirectAttainment() : BigDecimal.ZERO)
                                        .finalAttainment(it.getOverallAttainment() != null ? it.getOverallAttainment() : BigDecimal.ZERO)
                                        .observation(it.getObservation())
                                        .build());
                            }
                        }
                    }
                    if (calcResult.getCourseMappingRows() != null) {
                        for (ProgrammeAttainmentResultDto.CourseContributionRow row : calcResult.getCourseMappingRows()) {
                            if (row.getProgrammeBatchCourseId() != null) {
                                mappingRowMap.put(row.getProgrammeBatchCourseId(), ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                                        .programmeBatchCourseId(row.getProgrammeBatchCourseId())
                                        .masterCourseId(row.getMasterCourseId())
                                        .semester(row.getSemester())
                                        .courseCode(row.getCourseCode())
                                        .courseName(row.getCourseName())
                                        .resourceName(row.getResourceName())
                                        .poValues(row.getPoValues())
                                        .psoValues(row.getPsoValues())
                                        .build());
                            }
                        }
                    }
                    if (calcResult.getCourseDirectAttainmentRows() != null) {
                        for (ProgrammeAttainmentResultDto.CourseContributionRow row : calcResult.getCourseDirectAttainmentRows()) {
                            if (row.getProgrammeBatchCourseId() != null) {
                                directRowMap.put(row.getProgrammeBatchCourseId(), ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                                        .programmeBatchCourseId(row.getProgrammeBatchCourseId())
                                        .masterCourseId(row.getMasterCourseId())
                                        .semester(row.getSemester())
                                        .courseCode(row.getCourseCode())
                                        .courseName(row.getCourseName())
                                        .resourceName(row.getResourceName())
                                        .poValues(row.getPoValues())
                                        .psoValues(row.getPsoValues())
                                        .build());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[AnalyticsService] Error calculating live continuous attainment for batch {}: {}", batch.getId(), e.getMessage());
            }
        }

        // 4. Locate target outcome and extract authoritative direct attainment & target
        BigDecimal directAttainment = BigDecimal.ZERO;
        BigDecimal target = new BigDecimal("2.50");
        String statement = null;
        boolean outcomeFound = false;

        if ("PO".equals(type)) {
            ProgrammeBatchAttainmentReportDto.Report4PoRow matchedPo = poRows.stream()
                    .filter(p -> p.getPoCode() != null && p.getPoCode().equalsIgnoreCase(targetCode))
                    .findFirst()
                    .orElse(null);
            if (matchedPo != null) {
                outcomeFound = true;
                if (matchedPo.getDirectAttainment() != null) directAttainment = matchedPo.getDirectAttainment();
                if (matchedPo.getTargetLevel() != null) target = matchedPo.getTargetLevel();
                statement = matchedPo.getStatement();
            } else {
                List<ProgrammeOutcome> pos = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getId());
                if (pos.isEmpty()) {
                    pos = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getMasterProgrammeId());
                }
                ProgrammeOutcome poDef = pos.stream()
                        .filter(p -> p.getCode() != null && p.getCode().equalsIgnoreCase(targetCode))
                        .findFirst()
                        .orElse(null);
                if (poDef != null) {
                    outcomeFound = true;
                    statement = poDef.getStatement() != null ? poDef.getStatement() : "Programme Outcome " + targetCode;
                    if (poDef.getTarget() != null) target = poDef.getTarget();
                }
            }
        } else {
            ProgrammeBatchAttainmentReportDto.Report4PsoRow matchedPso = psoRows.stream()
                    .filter(p -> p.getPsoCode() != null && p.getPsoCode().equalsIgnoreCase(targetCode))
                    .findFirst()
                    .orElse(null);
            if (matchedPso != null) {
                outcomeFound = true;
                if (matchedPso.getDirectAttainment() != null) directAttainment = matchedPso.getDirectAttainment();
                if (matchedPso.getTargetLevel() != null) target = matchedPso.getTargetLevel();
                statement = matchedPso.getStatement();
            } else {
                List<ProgrammeSpecificOutcome> psos = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getId());
                if (psos.isEmpty()) {
                    psos = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getMasterProgrammeId());
                }
                ProgrammeSpecificOutcome psoDef = psos.stream()
                        .filter(p -> p.getCode() != null && p.getCode().equalsIgnoreCase(targetCode))
                        .findFirst()
                        .orElse(null);
                if (psoDef != null) {
                    outcomeFound = true;
                    statement = psoDef.getStatement() != null ? psoDef.getStatement() : "Programme Specific Outcome " + targetCode;
                    if (psoDef.getTarget() != null) target = psoDef.getTarget();
                }
            }
        }

        if (!outcomeFound) {
            throw new ResourceNotFoundException("Outcome '" + targetCode + "' of type '" + type + "' not found for programme batch: " + programmeBatchId);
        }

        if (statement == null || statement.isBlank()) {
            statement = ("PO".equals(type) ? "Programme Outcome " : "Programme Specific Outcome ") + targetCode;
        }

        directAttainment = directAttainment.setScale(2, RoundingMode.HALF_UP);
        target = target.setScale(2, RoundingMode.HALF_UP);
        BigDecimal directGap = directAttainment.subtract(target).setScale(2, RoundingMode.HALF_UP);
        boolean targetMet = directAttainment.compareTo(target) >= 0;

        // 5. Filter contributing courses (authoritative inclusion rule: contribution > 0)
        List<OutcomeDirectCourseDto> contributingCourses = new ArrayList<>();
        boolean isPo = "PO".equals(type);

        for (ProgrammeBatchCourse c : courses) {
            CourseAttainmentReport cr = courseReportMap.get(c.getId());
            ProgrammeBatchAttainmentReportDto.CourseContributionRow mRow = mappingRowMap.get(c.getId());
            ProgrammeBatchAttainmentReportDto.CourseContributionRow dRow = directRowMap.get(c.getId());

            BigDecimal contribution = null;
            if (dRow != null) {
                Map<String, BigDecimal> valMap = isPo ? dRow.getPoValues() : dRow.getPsoValues();
                if (valMap != null) {
                    contribution = valMap.get(targetCode);
                }
            }

            BigDecimal mappingStrength = null;
            if (mRow != null) {
                Map<String, BigDecimal> mapStrengthMap = isPo ? mRow.getPoValues() : mRow.getPsoValues();
                if (mapStrengthMap != null) {
                    mappingStrength = mapStrengthMap.get(targetCode);
                }
            }

            // Strictly preserve the authoritative inclusion rule: contribution > 0
            if (contribution != null && contribution.compareTo(BigDecimal.ZERO) > 0) {
                String coordinator = c.getCourseCoordinatorName() != null && !c.getCourseCoordinatorName().isBlank()
                        ? c.getCourseCoordinatorName()
                        : (c.getAssignedFaculty() != null ? c.getAssignedFaculty() : "");

                BigDecimal overallCourseAttainment = cr != null ? cr.getOverallCoAttainment() : null;

                contributingCourses.add(OutcomeDirectCourseDto.builder()
                        .programmeBatchCourseId(c.getId())
                        .courseCode(c.getCourseCode() != null ? c.getCourseCode() : c.getCode())
                        .courseName(c.getCourseName() != null ? c.getCourseName() : c.getName())
                        .semester(c.getSemester())
                        .courseCoordinator(coordinator)
                        .overallCourseAttainment(overallCourseAttainment != null ? overallCourseAttainment.setScale(2, RoundingMode.HALF_UP) : null)
                        .mappingStrength(mappingStrength != null ? mappingStrength.setScale(2, RoundingMode.HALF_UP) : null)
                        .contribution(contribution.setScale(2, RoundingMode.HALF_UP))
                        .build());
            }
        }

        // Deterministic ordering: semester ascending, then course code ascending
        contributingCourses.sort(Comparator.comparing((OutcomeDirectCourseDto c) -> c.getSemester() != null ? c.getSemester() : 1)
                .thenComparing(c -> c.getCourseCode() != null ? c.getCourseCode() : ""));

        return OutcomeDirectDrilldownResponseDto.builder()
                .programmeBatchId(batch.getId())
                .batchName(batch.getName())
                .outcomeCode(targetCode)
                .outcomeType(type)
                .outcomeStatement(statement)
                .directAttainment(directAttainment)
                .target(target)
                .directGap(directGap)
                .targetMet(targetMet)
                .contributingCourseCount(contributingCourses.size())
                .courses(contributingCourses)
                .build();
    }

    // ==========================================
    // PROGRAMME INDIRECT ATTAINMENT DRILL-DOWN
    // ==========================================
    public OutcomeIndirectDrilldownResponseDto getOutcomeIndirectDrilldown(String programmeBatchId, String outcomeCode, String outcomeType) {
        if (programmeBatchId == null || programmeBatchId.isBlank()) {
            throw new BadRequestException("programmeBatchId is required");
        }
        if (outcomeCode == null || outcomeCode.isBlank()) {
            throw new BadRequestException("outcomeCode is required");
        }
        if (outcomeType == null || outcomeType.isBlank()) {
            throw new BadRequestException("outcomeType is required");
        }

        String type = outcomeType.trim().toUpperCase();
        if (!"PO".equals(type) && !"PSO".equals(type)) {
            throw new BadRequestException("Invalid outcomeType: " + outcomeType + ". Must be PO or PSO");
        }

        String targetCode = outcomeCode.trim().toUpperCase();
        if ("PO".equals(type)) {
            if (targetCode.startsWith("PSO")) {
                throw new BadRequestException("Outcome code " + outcomeCode + " does not match outcomeType PO");
            }
        } else {
            if (!targetCode.startsWith("PSO")) {
                throw new BadRequestException("Outcome code " + outcomeCode + " does not match outcomeType PSO");
            }
        }

        // 1. Authorize & Resolve batch scope (hierarchical upward check & RBAC)
        ResolvedScope scope = validateAndResolveScope(null, null, null, programmeBatchId);
        ProgrammeBatch batch = programmeBatchRepository.findById(scope.programmeBatchId)
                .orElseThrow(() -> new BadRequestException("ProgrammeBatch not found: " + scope.programmeBatchId));

        // 2. Resolve batch attainment report / continuous calculation data
        ProgrammeBatchAttainmentReport report = programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId()).orElse(null);
        ResolvedBatchAnalyticsData bData = resolveBatchData(batch, report != null ? Map.of(batch.getId(), report) : Collections.emptyMap());

        // 3. Locate target outcome definition and extract authoritative target & statement
        BigDecimal target = new BigDecimal("2.50");
        String statement = null;
        boolean outcomeFound = false;

        if ("PO".equals(type)) {
            ProgrammeBatchAttainmentReportDto.Report4PoRow matchedPo = (bData.pos != null) ? bData.pos.stream()
                    .filter(p -> p.getPoCode() != null && p.getPoCode().equalsIgnoreCase(targetCode))
                    .findFirst()
                    .orElse(null) : null;
            if (matchedPo != null) {
                outcomeFound = true;
                if (matchedPo.getTargetLevel() != null) target = matchedPo.getTargetLevel();
                statement = matchedPo.getStatement();
            } else {
                List<ProgrammeOutcome> pos = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getId());
                if (pos.isEmpty()) {
                    pos = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getMasterProgrammeId());
                }
                ProgrammeOutcome poDef = pos.stream()
                        .filter(p -> p.getCode() != null && p.getCode().equalsIgnoreCase(targetCode))
                        .findFirst()
                        .orElse(null);
                if (poDef != null) {
                    outcomeFound = true;
                    statement = poDef.getStatement() != null ? poDef.getStatement() : "Programme Outcome " + targetCode;
                    if (poDef.getTarget() != null) target = poDef.getTarget();
                } else if (targetCode.matches("^PO([1-9]|1[0-2])$")) {
                    outcomeFound = true;
                    statement = "Programme Outcome " + targetCode;
                }
            }
        } else {
            ProgrammeBatchAttainmentReportDto.Report4PsoRow matchedPso = (bData.psos != null) ? bData.psos.stream()
                    .filter(p -> p.getPsoCode() != null && p.getPsoCode().equalsIgnoreCase(targetCode))
                    .findFirst()
                    .orElse(null) : null;
            if (matchedPso != null) {
                outcomeFound = true;
                if (matchedPso.getTargetLevel() != null) target = matchedPso.getTargetLevel();
                statement = matchedPso.getStatement();
            } else {
                List<ProgrammeSpecificOutcome> psos = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getId());
                if (psos.isEmpty()) {
                    psos = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getMasterProgrammeId());
                }
                ProgrammeSpecificOutcome psoDef = psos.stream()
                        .filter(p -> p.getCode() != null && p.getCode().equalsIgnoreCase(targetCode))
                        .findFirst()
                        .orElse(null);
                if (psoDef != null) {
                    outcomeFound = true;
                    statement = psoDef.getStatement() != null ? psoDef.getStatement() : "Programme Specific Outcome " + targetCode;
                    if (psoDef.getTarget() != null) target = psoDef.getTarget();
                } else if (targetCode.matches("^PSO[1-3]$")) {
                    outcomeFound = true;
                    statement = "Programme Specific Outcome " + targetCode;
                }
            }
        }

        if (!outcomeFound) {
            throw new ResourceNotFoundException("Outcome '" + targetCode + "' of type '" + type + "' not found for programme batch: " + programmeBatchId);
        }

        // 4. Retrieve Programme Exit Survey (if present)
        ProgrammeSurveyResultDto exitSurvey = null;
        try {
            exitSurvey = attainmentCalculationService.getProgrammeSurveyResult(batch.getMasterProgrammeId(), batch.getId());
        } catch (Exception e) {
            log.debug("[AnalyticsService] No exit survey found for batch {}: {}", batch.getId(), e.getMessage());
        }

        Map<String, BigDecimal> exitScores = new LinkedHashMap<>();
        if (exitSurvey != null && exitSurvey.getRecordsProcessed() > 0) {
            if (exitSurvey.getPoIndirectAttainment() != null) {
                for (ProgrammeSurveyResultDto.PoIndirectItem it : exitSurvey.getPoIndirectAttainment()) {
                    if (it.getPoCode() != null && it.getIndirectAttainment() != null && it.getIndirectAttainment().compareTo(BigDecimal.ZERO) > 0) {
                        exitScores.put(it.getPoCode().toUpperCase().trim(), it.getIndirectAttainment());
                    }
                }
            }
            if (exitSurvey.getPsoIndirectAttainment() != null) {
                for (ProgrammeSurveyResultDto.PsoIndirectItem it : exitSurvey.getPsoIndirectAttainment()) {
                    if (it.getPsoCode() != null && it.getIndirectAttainment() != null && it.getIndirectAttainment().compareTo(BigDecimal.ZERO) > 0) {
                        exitScores.put(it.getPsoCode().toUpperCase().trim(), it.getIndirectAttainment());
                    }
                }
            }
        }

        // 5. Retrieve authoritative consolidated indirect attainment
        BigDecimal indirectAttainment = BigDecimal.ZERO;
        if ("PO".equals(type)) {
            ProgrammeBatchAttainmentReportDto.Report4PoRow matchedPo = (bData.pos != null) ? bData.pos.stream()
                    .filter(p -> p.getPoCode() != null && p.getPoCode().equalsIgnoreCase(targetCode))
                    .findFirst()
                    .orElse(null) : null;
            if (matchedPo != null && matchedPo.getIndirectAttainment() != null) {
                indirectAttainment = matchedPo.getIndirectAttainment();
            }
        } else {
            ProgrammeBatchAttainmentReportDto.Report4PsoRow matchedPso = (bData.psos != null) ? bData.psos.stream()
                    .filter(p -> p.getPsoCode() != null && p.getPsoCode().equalsIgnoreCase(targetCode))
                    .findFirst()
                    .orElse(null) : null;
            if (matchedPso != null && matchedPso.getIndirectAttainment() != null) {
                indirectAttainment = matchedPso.getIndirectAttainment();
            }
        }

        // Fallback live consolidation if not finalized/empty in bData
        if (indirectAttainment.compareTo(BigDecimal.ZERO) == 0 && indirectAssessmentService != null) {
            Map<String, BigDecimal> liveConsolidated = indirectAssessmentService.computeConsolidatedScores(batch.getId(), exitScores);
            if (liveConsolidated != null && liveConsolidated.containsKey(targetCode)) {
                indirectAttainment = liveConsolidated.get(targetCode);
            }
        }

        // 6. Gather and filter evidence items in chronological order
        List<OutcomeIndirectEvidenceItemDto> evidenceList = new ArrayList<>();
        List<IndirectAssessmentDto> dbAssessments = indirectAssessmentService != null
                ? indirectAssessmentService.getAssessments(batch.getId())
                : Collections.emptyList();

        for (IndirectAssessmentDto a : dbAssessments) {
            Map<String, BigDecimal> scores = a.getScores();
            BigDecimal rawScore = (scores != null) ? scores.get(targetCode) : null;
            boolean evaluated = rawScore != null && rawScore.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal outcomeValue = evaluated ? rawScore.setScale(2, RoundingMode.HALF_UP) : null;

            evidenceList.add(OutcomeIndirectEvidenceItemDto.builder()
                    .assessmentId(a.getId())
                    .type(a.getType() != null ? a.getType() : "EVENT")
                    .name(a.getName())
                    .description(a.getDescription())
                    .date(a.getCreatedAt())
                    .outcomeEvaluated(evaluated)
                    .outcomeValue(outcomeValue)
                    .responseCount(null)
                    .createdBy(a.getCreatedBy())
                    .build());
        }

        // Include Exit Survey as evidence if present
        if (exitSurvey != null && exitSurvey.getRecordsProcessed() > 0) {
            BigDecimal exitSurveyScore = null;
            if ("PO".equals(type) && exitSurvey.getPoIndirectAttainment() != null) {
                exitSurveyScore = exitSurvey.getPoIndirectAttainment().stream()
                        .filter(it -> it.getPoCode() != null && it.getPoCode().equalsIgnoreCase(targetCode))
                        .map(ProgrammeSurveyResultDto.PoIndirectItem::getIndirectAttainment)
                        .findFirst()
                        .orElse(null);
            } else if ("PSO".equals(type) && exitSurvey.getPsoIndirectAttainment() != null) {
                exitSurveyScore = exitSurvey.getPsoIndirectAttainment().stream()
                        .filter(it -> it.getPsoCode() != null && it.getPsoCode().equalsIgnoreCase(targetCode))
                        .map(ProgrammeSurveyResultDto.PsoIndirectItem::getIndirectAttainment)
                        .findFirst()
                        .orElse(null);
            }

            boolean surveyEvaluated = exitSurveyScore != null && exitSurveyScore.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal surveyValue = surveyEvaluated ? exitSurveyScore.setScale(2, RoundingMode.HALF_UP) : null;

            ZonedDateTime exitDate = null;
            if (!evidenceList.isEmpty()) {
                ZonedDateTime lastDate = evidenceList.get(evidenceList.size() - 1).getDate();
                exitDate = (lastDate != null) ? lastDate.plusMinutes(1) : ZonedDateTime.now();
            } else {
                exitDate = batch.getCreatedAt() != null ? batch.getCreatedAt() : ZonedDateTime.now();
            }

            evidenceList.add(OutcomeIndirectEvidenceItemDto.builder()
                    .assessmentId(exitSurvey.getUploadId() != null && !exitSurvey.getUploadId().isBlank()
                            ? exitSurvey.getUploadId() : "exit-survey-" + batch.getId())
                    .type("EXIT_SURVEY")
                    .name("Programme End Exit Survey")
                    .description("Graduating batch comprehensive programme exit survey")
                    .date(exitDate)
                    .outcomeEvaluated(surveyEvaluated)
                    .outcomeValue(surveyValue)
                    .responseCount(exitSurvey.getRecordsProcessed())
                    .createdBy("Programme Coordinator")
                    .build());
        }

        int totalEvidenceCount = evidenceList.size();
        int participatingEvidenceCount = (int) evidenceList.stream()
                .filter(OutcomeIndirectEvidenceItemDto::isOutcomeEvaluated)
                .count();

        BigDecimal indirectGap = indirectAttainment.subtract(target).setScale(2, RoundingMode.HALF_UP);
        boolean targetMet = indirectAttainment.compareTo(target) >= 0;

        return OutcomeIndirectDrilldownResponseDto.builder()
                .programmeBatchId(batch.getId())
                .batchName(batch.getName())
                .outcomeCode(targetCode)
                .outcomeType(type)
                .outcomeStatement(statement)
                .indirectAttainment(indirectAttainment.setScale(2, RoundingMode.HALF_UP))
                .target(target.setScale(2, RoundingMode.HALF_UP))
                .indirectGap(indirectGap)
                .targetMet(targetMet)
                .totalEvidenceCount(totalEvidenceCount)
                .participatingEvidenceCount(participatingEvidenceCount)
                .evidence(evidenceList)
                .build();
    }

    // ==========================================
    // 8. STUDENT CO EVIDENCE ENDPOINT (PHASE 9)
    // ==========================================
    public StudentCoEvidenceResponseDto getStudentCoEvidence(String programmeBatchCourseId, String coCode) {
        if (programmeBatchCourseId == null || coCode == null) {
            throw new BadRequestException("programmeBatchCourseId and coCode are required");
        }

        ProgrammeBatchCourse course = programmeBatchCourseRepository.findById(programmeBatchCourseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course offering not found: " + programmeBatchCourseId));

        // Enforce role and hierarchy scope authorization
        validateAndResolveScope(null, null, null, course.getProgrammeBatchId());

        String targetCo = coCode.toUpperCase().trim();

        // 1. Fetch Course Attainment Report for Table 3 metadata
        CourseAttainmentReport cReport = courseAttainmentReportRepository.findByProgrammeBatchCourseId(programmeBatchCourseId).orElse(null);
        String coStatement = null;
        BigDecimal coTargetLevel = BigDecimal.valueOf(2.00);
        BigDecimal coDirectAttainment = BigDecimal.ZERO;
        BigDecimal coIndirectAttainment = BigDecimal.ZERO;
        BigDecimal coOverallAttainment = BigDecimal.ZERO;
        Boolean coTargetMet = false;

        if (cReport != null) {
            coDirectAttainment = cReport.getDirectAttainment() != null ? cReport.getDirectAttainment() : BigDecimal.ZERO;
            coIndirectAttainment = cReport.getIndirectAttainment() != null ? cReport.getIndirectAttainment() : BigDecimal.ZERO;
            coOverallAttainment = cReport.getOverallCoAttainment() != null ? cReport.getOverallCoAttainment() : BigDecimal.ZERO;

            List<CourseAttainmentReportDto.Table3Row> table3 = parseTable3CoAttainments(cReport.getTable3CoAttainmentJson());
            for (CourseAttainmentReportDto.Table3Row t3 : table3) {
                if (t3.getCoCode() != null && t3.getCoCode().equalsIgnoreCase(targetCo)) {
                    coStatement = t3.getStatement();
                    if (t3.getTargetLevel() != null) coTargetLevel = t3.getTargetLevel();
                    if (t3.getFinalAttainment() != null) coOverallAttainment = t3.getFinalAttainment();
                    if (t3.getDirectLevel() != null) coDirectAttainment = BigDecimal.valueOf(t3.getDirectLevel());
                    if (t3.getIndirectLevel() != null) coIndirectAttainment = BigDecimal.valueOf(t3.getIndirectLevel());
                    coTargetMet = t3.getTargetMet();
                    break;
                }
            }
        }

        // 2. Fetch Student CO Marks
        List<StudentCoMark> marks = studentCoMarkRepository.findByProgrammeBatchCourseIdAndCoCode(programmeBatchCourseId, targetCo);
        if (marks.isEmpty()) {
            List<StudentCoMark> allMarks = studentCoMarkRepository.findByProgrammeBatchCourseId(programmeBatchCourseId);
            marks = allMarks.stream()
                    .filter(m -> m.getCoCode() != null && m.getCoCode().equalsIgnoreCase(targetCo))
                    .toList();
        }

        int totalStudents = marks.size();
        int studentsAbove = 0;
        int studentsBelow = 0;
        BigDecimal sumPct = BigDecimal.ZERO;
        BigDecimal highestPct = BigDecimal.ZERO;
        BigDecimal lowestPct = totalStudents > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;

        Map<String, Integer> scoreDistribution = new LinkedHashMap<>();
        scoreDistribution.put("90-100%", 0);
        scoreDistribution.put("80-89%", 0);
        scoreDistribution.put("70-79%", 0);
        scoreDistribution.put("60-69%", 0);
        scoreDistribution.put("50-59%", 0);
        scoreDistribution.put("<50%", 0);

        List<StudentEvidenceRowDto> studentRows = new ArrayList<>();

        BigDecimal activeThreshold = getStudentEvidenceThreshold();

        for (int i = 0; i < marks.size(); i++) {
            StudentCoMark m = marks.get(i);
            BigDecimal marksObtained = m.getMarksObtained() != null ? m.getMarksObtained() : BigDecimal.ZERO;
            BigDecimal maxMarks = m.getMaxMarks() != null && m.getMaxMarks().compareTo(BigDecimal.ZERO) > 0 ? m.getMaxMarks() : BigDecimal.valueOf(100.00);

            BigDecimal pct = marksObtained.divide(maxMarks, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

            sumPct = sumPct.add(pct);
            if (pct.compareTo(highestPct) > 0) highestPct = pct;
            if (pct.compareTo(lowestPct) < 0) lowestPct = pct;

            // Student Performance Evidence Threshold (Authoritative IQAC Configured)
            boolean met = pct.compareTo(activeThreshold) >= 0;
            if (met) {
                studentsAbove++;
            } else {
                studentsBelow++;
            }

            // Bucketing
            if (pct.compareTo(BigDecimal.valueOf(90.00)) >= 0) {
                scoreDistribution.put("90-100%", scoreDistribution.get("90-100%") + 1);
            } else if (pct.compareTo(BigDecimal.valueOf(80.00)) >= 0) {
                scoreDistribution.put("80-89%", scoreDistribution.get("80-89%") + 1);
            } else if (pct.compareTo(BigDecimal.valueOf(70.00)) >= 0) {
                scoreDistribution.put("70-79%", scoreDistribution.get("70-79%") + 1);
            } else if (pct.compareTo(BigDecimal.valueOf(60.00)) >= 0) {
                scoreDistribution.put("60-69%", scoreDistribution.get("60-69%") + 1);
            } else if (pct.compareTo(BigDecimal.valueOf(50.00)) >= 0) {
                scoreDistribution.put("50-59%", scoreDistribution.get("50-59%") + 1);
            } else {
                scoreDistribution.put("<50%", scoreDistribution.get("<50%") + 1);
            }

            studentRows.add(StudentEvidenceRowDto.builder()
                    .studentIdentifier("Student " + (i + 1))
                    .maskedPrn(maskPrn(m.getPrn()))
                    .marksObtained(marksObtained.setScale(2, RoundingMode.HALF_UP))
                    .maxMarks(maxMarks.setScale(2, RoundingMode.HALF_UP))
                    .percentage(pct)
                    .thresholdMet(met)
                    .build());
        }

        BigDecimal avgPct = totalStudents > 0
                ? sumPct.divide(BigDecimal.valueOf(totalStudents), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal attainmentRate = totalStudents > 0
                ? BigDecimal.valueOf(studentsAbove).multiply(BigDecimal.valueOf(100.00)).divide(BigDecimal.valueOf(totalStudents), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return StudentCoEvidenceResponseDto.builder()
                .programmeBatchCourseId(programmeBatchCourseId)
                .courseCode(course.getEffectiveCourseCode() != null ? course.getEffectiveCourseCode() : course.getCode())
                .courseName(course.getEffectiveCourseName() != null ? course.getEffectiveCourseName() : course.getName())
                .semester(course.getSemester())
                .courseCoordinatorName(course.getCourseCoordinatorName() != null ? course.getCourseCoordinatorName() : course.getAssignedFaculty())
                .coCode(targetCo)
                .coStatement(coStatement)
                .coTargetLevel(coTargetLevel)
                .coDirectAttainment(coDirectAttainment)
                .coIndirectAttainment(coIndirectAttainment)
                .coOverallAttainment(coOverallAttainment)
                .coTargetMet(coTargetMet)
                .configuredThresholdPercentage(activeThreshold)
                .totalStudentsEvaluated(totalStudents)
                .studentsMeetingThreshold(studentsAbove)
                .studentsBelowThreshold(studentsBelow)
                .attainmentRatePercentage(attainmentRate)
                .classAveragePercentage(avgPct)
                .highestPercentage(highestPct)
                .lowestPercentage(totalStudents > 0 ? lowestPct : BigDecimal.ZERO)
                .scoreDistribution(scoreDistribution)
                .studentRecords(studentRows)
                .build();
    }

    // ==========================================
    // COURSE ANALYTICS AGGREGATED ENDPOINT
    // ==========================================
    public CourseAnalyticsResponseDto getCourseAnalytics(String programmeBatchCourseId, String outcomeCode, String outcomeType) {
        if (programmeBatchCourseId == null || programmeBatchCourseId.isBlank()) {
            throw new BadRequestException("programmeBatchCourseId is required");
        }

        // 1. Resolve Course Offering
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(programmeBatchCourseId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + programmeBatchCourseId));

        // 2. Resolve Programme Batch
        ProgrammeBatch batch = programmeBatchRepository.findById(offering.getProgrammeBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Programme batch not found: " + offering.getProgrammeBatchId()));

        // 3. Resolve Academic Hierarchy
        MasterProgramme prog = batch.getMasterProgrammeId() != null
                ? masterProgrammeRepository.findById(batch.getMasterProgrammeId()).orElse(null)
                : null;
        Department dept = prog != null && prog.getDepartmentId() != null
                ? departmentRepository.findById(prog.getDepartmentId()).orElse(null)
                : null;
        School school = dept != null && dept.getSchoolId() != null
                ? schoolRepository.findById(dept.getSchoolId()).orElse(null)
                : null;

        // 4. Enforce Scope Security (Director, HOD, Programme Coordinator)
        validateAndResolveScope(
                school != null ? school.getId() : null,
                dept != null ? dept.getId() : null,
                prog != null ? prog.getId() : null,
                batch.getId()
        );

        // Enforce Course Coordinator / Faculty Scope Security
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope();
        if (userScope != null && userScope.isFaculty()) {
            boolean isCoord = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), userScope.getUserId()))
                    || (offering.getCourseCoordinatorEmail() != null && offering.getCourseCoordinatorEmail().equalsIgnoreCase(userScope.getEmail()));
            boolean isAssigned = isCoord || (offering.getAssignedFaculty() != null
                    && (offering.getAssignedFaculty().contains(userScope.getEmail()) || offering.getAssignedFaculty().contains(userScope.getName())));
            if (!isAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
            }
        }

        // 5. Authoritative Configuration & Weights
        AttainmentConfiguration config = attainmentConfigurationRepository.findByProgrammeBatchCourseId(offering.getId()).orElse(null);
        BigDecimal directWeight = config != null && config.getEffectiveApprovedDirectWeight() != null
                ? config.getEffectiveApprovedDirectWeight() : new BigDecimal("80.00");
        BigDecimal indirectWeight = config != null && config.getEffectiveApprovedIndirectWeight() != null
                ? config.getEffectiveApprovedIndirectWeight() : new BigDecimal("20.00");
        BigDecimal directThreshold = config != null && config.getEffectiveApprovedDirectThreshold() != null
                ? config.getEffectiveApprovedDirectThreshold() : new BigDecimal("60.00");
        BigDecimal indirectThreshold = config != null && config.getEffectiveApprovedIndirectThreshold() != null
                ? config.getEffectiveApprovedIndirectThreshold() : new BigDecimal("60.00");

        // 6. Authoritative Course Attainment Report
        CourseAttainmentReportDto reportDto = null;
        try {
            reportDto = attainmentReportService.getOrCreateCourseAttainmentReport(offering.getId());
        } catch (Exception ex) {
            log.warn("[AnalyticsService] Could not get or create course attainment report for offering {}: {}", offering.getId(), ex.getMessage());
        }
        BigDecimal overallCourseAttainment = reportDto != null && reportDto.getOverallCoAttainment() != null
                ? reportDto.getOverallCoAttainment() : BigDecimal.ZERO;
        BigDecimal directAttainment = reportDto != null && reportDto.getDirectAttainment() != null
                ? reportDto.getDirectAttainment() : BigDecimal.ZERO;
        BigDecimal indirectAttainment = reportDto != null && reportDto.getIndirectAttainment() != null
                ? reportDto.getIndirectAttainment() : BigDecimal.ZERO;
        String attainmentStatus = reportDto != null && reportDto.getStatus() != null ? reportDto.getStatus().name() : "DRAFT";

        // 7. Authoritative Articulation Matrix (CO -> PO/PSO)
        CourseMappingMatrixDto matrixDto = null;
        try {
            matrixDto = outcomeService.getCourseMappings(offering.getId());
        } catch (Exception ex) {
            log.warn("[AnalyticsService] Could not resolve course mappings for offering {}: {}", offering.getId(), ex.getMessage());
        }
        Map<String, Map<String, Integer>> matrix = (matrixDto != null && matrixDto.getMatrix() != null)
                ? matrixDto.getMatrix() : Collections.emptyMap();

        // 8. Selected Outcome Preparation
        String targetOutcomeCode = (outcomeCode != null && !outcomeCode.isBlank()) ? outcomeCode.trim().toUpperCase() : null;
        String targetOutcomeType = (outcomeType != null && !outcomeType.isBlank())
                ? outcomeType.trim().toUpperCase()
                : (targetOutcomeCode != null && targetOutcomeCode.startsWith("PSO") ? "PSO" : "PO");

        // 9. Course CO Overview Table
        List<CourseCoOverviewItemDto> coItems = new ArrayList<>();
        if (reportDto != null && reportDto.getTable3CoAttainments() != null) {
            for (CourseAttainmentReportDto.Table3Row t3 : reportDto.getTable3CoAttainments()) {
                String coCode = t3.getCoCode();
                Map<String, Integer> rowMappings = matrix.getOrDefault(coCode, Collections.emptyMap());
                Map<String, Integer> poMap = new LinkedHashMap<>();
                Map<String, Integer> psoMap = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> m : rowMappings.entrySet()) {
                    if (m.getKey().toUpperCase().startsWith("PSO")) {
                        psoMap.put(m.getKey(), m.getValue());
                    } else {
                        poMap.put(m.getKey(), m.getValue());
                    }
                }

                Integer selectedMapping = (targetOutcomeCode != null) ? rowMappings.get(targetOutcomeCode) : null;

                coItems.add(CourseCoOverviewItemDto.builder()
                        .coCode(coCode)
                        .statement(t3.getStatement())
                        .target(t3.getTargetLevel())
                        .directPercentage(t3.getDirectPercentage() != null ? t3.getDirectPercentage() : BigDecimal.ZERO)
                        .directLevel(t3.getDirectLevel() != null ? t3.getDirectLevel() : 0)
                        .directAttainment(t3.getDirectLevel() != null ? BigDecimal.valueOf(t3.getDirectLevel()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                        .indirectPercentage(t3.getIndirectPercentage() != null ? t3.getIndirectPercentage() : BigDecimal.ZERO)
                        .indirectScore(t3.getIndirectScore() != null ? t3.getIndirectScore() : BigDecimal.ZERO)
                        .indirectLevel(t3.getIndirectLevel() != null ? t3.getIndirectLevel() : 0)
                        .indirectAttainment(t3.getIndirectLevel() != null ? BigDecimal.valueOf(t3.getIndirectLevel()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                        .overallAttainment(t3.getFinalAttainment() != null ? t3.getFinalAttainment() : BigDecimal.ZERO)
                        .targetMet(t3.getTargetMet())
                        .observation(t3.getObservation())
                        .directWeight(directWeight)
                        .indirectWeight(indirectWeight)
                        .poMappings(poMap)
                        .psoMappings(psoMap)
                        .selectedOutcomeMapping(selectedMapping)
                        .build());
            }
        }

        if (coItems.isEmpty()) {
            List<CourseOutcome> dbCos = courseOutcomeRepository.findByProgrammeBatchCourseId(offering.getId());
            if (dbCos.isEmpty()) {
                for (int i = 1; i <= 5; i++) {
                    String cCode = "CO" + i;
                    coItems.add(CourseCoOverviewItemDto.builder()
                            .coCode(cCode)
                            .statement("Course outcome " + cCode)
                            .target(new BigDecimal("2.50"))
                            .directPercentage(BigDecimal.ZERO)
                            .directLevel(0)
                            .directAttainment(BigDecimal.ZERO)
                            .indirectPercentage(BigDecimal.ZERO)
                            .indirectScore(BigDecimal.ZERO)
                            .indirectLevel(0)
                            .indirectAttainment(BigDecimal.ZERO)
                            .overallAttainment(BigDecimal.ZERO)
                            .targetMet(false)
                            .directWeight(directWeight)
                            .indirectWeight(indirectWeight)
                            .poMappings(Collections.emptyMap())
                            .psoMappings(Collections.emptyMap())
                            .build());
                }
            } else {
                for (CourseOutcome co : dbCos) {
                    String cCode = co.getCode();
                    Map<String, Integer> rowMappings = matrix.getOrDefault(cCode, Collections.emptyMap());
                    Map<String, Integer> poMap = new LinkedHashMap<>();
                    Map<String, Integer> psoMap = new LinkedHashMap<>();
                    for (Map.Entry<String, Integer> m : rowMappings.entrySet()) {
                        if (m.getKey().toUpperCase().startsWith("PSO")) {
                            psoMap.put(m.getKey(), m.getValue());
                        } else {
                            poMap.put(m.getKey(), m.getValue());
                        }
                    }
                    Integer selectedMapping = (targetOutcomeCode != null) ? rowMappings.get(targetOutcomeCode) : null;
                    coItems.add(CourseCoOverviewItemDto.builder()
                            .coCode(cCode)
                            .statement(co.getStatement())
                            .target(co.getTargetLevel() != null ? co.getTargetLevel() : new BigDecimal("2.50"))
                            .directPercentage(BigDecimal.ZERO)
                            .directLevel(0)
                            .directAttainment(BigDecimal.ZERO)
                            .indirectPercentage(BigDecimal.ZERO)
                            .indirectScore(BigDecimal.ZERO)
                            .indirectLevel(0)
                            .indirectAttainment(BigDecimal.ZERO)
                            .overallAttainment(BigDecimal.ZERO)
                            .targetMet(false)
                            .directWeight(directWeight)
                            .indirectWeight(indirectWeight)
                            .poMappings(poMap)
                            .psoMappings(psoMap)
                            .selectedOutcomeMapping(selectedMapping)
                            .build());
                }
            }
        }

        // 10. Authoritative PO & PSO Contributions
        List<ProgrammeOutcome> batchPOs = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getId());
        if (batchPOs.isEmpty()) {
            batchPOs = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getMasterProgrammeId());
        }
        List<ProgrammeSpecificOutcome> batchPSOs = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getId());
        if (batchPSOs.isEmpty()) {
            batchPSOs = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getMasterProgrammeId());
        }

        Map<String, CourseAttainmentReportDto.Table2PoRow> poRowMap = reportDto.getTable2DirectPO() != null
                ? reportDto.getTable2DirectPO().stream().filter(r -> r.getPoCode() != null).collect(Collectors.toMap(r -> r.getPoCode().toUpperCase(), r -> r, (a, b) -> a))
                : Collections.emptyMap();
        Map<String, CourseAttainmentReportDto.Table2PsoRow> psoRowMap = reportDto.getTable2DirectPSO() != null
                ? reportDto.getTable2DirectPSO().stream().filter(r -> r.getPsoCode() != null).collect(Collectors.toMap(r -> r.getPsoCode().toUpperCase(), r -> r, (a, b) -> a))
                : Collections.emptyMap();

        List<OutcomeContributionItemDto> poContributions = new ArrayList<>();
        for (ProgrammeOutcome po : batchPOs) {
            String code = po.getCode().toUpperCase();
            CourseAttainmentReportDto.Table2PoRow row = poRowMap.get(code);
            BigDecimal avgMap = row != null ? row.getAverageMapping() : null;
            BigDecimal contrib = row != null ? row.getDirectContribution() : null;
            boolean isMapped = avgMap != null && avgMap.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal target = po.getTarget() != null ? po.getTarget() : new BigDecimal("2.50");
            Boolean targetMet = (isMapped && contrib != null) ? (contrib.compareTo(target) >= 0) : null;

            if (isMapped) {
                poContributions.add(OutcomeContributionItemDto.builder()
                        .outcomeCode(po.getCode())
                        .outcomeType("PO")
                        .outcomeStatement(po.getStatement())
                        .mappingStrength(avgMap)
                        .contribution(contrib)
                        .target(target)
                        .targetMet(targetMet)
                        .mapped(true)
                        .build());
            }
        }

        List<OutcomeContributionItemDto> psoContributions = new ArrayList<>();
        for (ProgrammeSpecificOutcome pso : batchPSOs) {
            String code = pso.getCode().toUpperCase();
            CourseAttainmentReportDto.Table2PsoRow row = psoRowMap.get(code);
            BigDecimal avgMap = row != null ? row.getAverageMapping() : null;
            BigDecimal contrib = row != null ? row.getDirectContribution() : null;
            boolean isMapped = avgMap != null && avgMap.compareTo(BigDecimal.ZERO) > 0;
            BigDecimal target = pso.getTarget() != null ? pso.getTarget() : new BigDecimal("2.50");
            Boolean targetMet = (isMapped && contrib != null) ? (contrib.compareTo(target) >= 0) : null;

            if (isMapped) {
                psoContributions.add(OutcomeContributionItemDto.builder()
                        .outcomeCode(pso.getCode())
                        .outcomeType("PSO")
                        .outcomeStatement(pso.getStatement())
                        .mappingStrength(avgMap)
                        .contribution(contrib)
                        .target(target)
                        .targetMet(targetMet)
                        .mapped(true)
                        .build());
            }
        }

        List<OutcomeContributionItemDto> allOutcomes = new ArrayList<>();
        allOutcomes.addAll(poContributions);
        allOutcomes.addAll(psoContributions);

        // 11. Selected Outcome Resolution
        SelectedOutcomeContributionDto selectedOutcomeDto = null;
        if (targetOutcomeCode != null) {
            OutcomeContributionItemDto matched = allOutcomes.stream()
                    .filter(o -> o.getOutcomeCode().equalsIgnoreCase(targetOutcomeCode))
                    .findFirst()
                    .orElse(null);

            if (matched != null) {
                selectedOutcomeDto = SelectedOutcomeContributionDto.builder()
                        .outcomeCode(matched.getOutcomeCode())
                        .outcomeType(matched.getOutcomeType())
                        .outcomeStatement(matched.getOutcomeStatement())
                        .mappingStrength(matched.getMappingStrength())
                        .overallCourseAttainment(overallCourseAttainment)
                        .contribution(matched.getContribution())
                        .target(matched.getTarget())
                        .targetMet(matched.getTargetMet())
                        .mapped(true)
                        .build();
            } else {
                String stmt = null;
                BigDecimal tgt = new BigDecimal("2.50");
                boolean foundDef = false;
                if ("PO".equalsIgnoreCase(targetOutcomeType)) {
                    ProgrammeOutcome poDef = batchPOs.stream().filter(p -> p.getCode().equalsIgnoreCase(targetOutcomeCode)).findFirst().orElse(null);
                    if (poDef != null) {
                        foundDef = true;
                        stmt = poDef.getStatement();
                        if (poDef.getTarget() != null) tgt = poDef.getTarget();
                    }
                } else {
                    ProgrammeSpecificOutcome psoDef = batchPSOs.stream().filter(p -> p.getCode().equalsIgnoreCase(targetOutcomeCode)).findFirst().orElse(null);
                    if (psoDef != null) {
                        foundDef = true;
                        stmt = psoDef.getStatement();
                        if (psoDef.getTarget() != null) tgt = psoDef.getTarget();
                    }
                }

                if (!foundDef) {
                    throw new ResourceNotFoundException("Outcome '" + targetOutcomeCode + "' not found for programme batch: " + batch.getId());
                }

                selectedOutcomeDto = SelectedOutcomeContributionDto.builder()
                        .outcomeCode(targetOutcomeCode)
                        .outcomeType(targetOutcomeType)
                        .outcomeStatement(stmt)
                        .mappingStrength(null)
                        .overallCourseAttainment(overallCourseAttainment)
                        .contribution(null)
                        .target(tgt)
                        .targetMet(null)
                        .mapped(false)
                        .build();
            }
        }

        // 12. Course ATR Status Context
        List<CourseAtr> atrs = courseAtrRepository.findByProgrammeBatchCourseId(offering.getId());
        boolean courseAtrAvailable = !atrs.isEmpty();
        String courseAtrStatus = !atrs.isEmpty() && atrs.get(0).getStatus() != null
                ? atrs.get(0).getStatus().name() : "DRAFT";

        String coordinatorName = offering.getCourseCoordinatorName() != null && !offering.getCourseCoordinatorName().isBlank()
                ? offering.getCourseCoordinatorName()
                : (offering.getAssignedFaculty() != null ? offering.getAssignedFaculty() : "");

        return CourseAnalyticsResponseDto.builder()
                .programmeBatchId(batch.getId())
                .programmeBatchCourseId(offering.getId())
                .batchName(batch.getName())
                .masterProgrammeId(prog != null ? prog.getId() : batch.getMasterProgrammeId())
                .programmeName(prog != null ? prog.getName() : "")
                .schoolId(school != null ? school.getId() : "")
                .schoolName(school != null ? school.getName() : "")
                .departmentId(dept != null ? dept.getId() : "")
                .departmentName(dept != null ? dept.getName() : "")
                .courseCode(offering.getEffectiveCourseCode() != null ? offering.getEffectiveCourseCode() : offering.getCode())
                .courseName(offering.getEffectiveCourseName() != null ? offering.getEffectiveCourseName() : offering.getName())
                .semester(offering.getSemester())
                .courseCoordinator(coordinatorName)
                .courseCoordinatorEmail(offering.getCourseCoordinatorEmail() != null ? offering.getCourseCoordinatorEmail() : "")
                .masterCourseId(offering.getMasterCourseId() != null ? offering.getMasterCourseId() : offering.getId())
                .overallCourseAttainment(overallCourseAttainment)
                .directAttainment(directAttainment)
                .indirectAttainment(indirectAttainment)
                .directWeight(directWeight)
                .indirectWeight(indirectWeight)
                .directThreshold(directThreshold)
                .indirectThreshold(indirectThreshold)
                .attainmentStatus(attainmentStatus)
                .selectedOutcome(selectedOutcomeDto)
                .outcomes(allOutcomes)
                .poContributions(poContributions)
                .psoContributions(psoContributions)
                .courseOutcomes(coItems)
                .mappingMatrix(matrix)
                .courseAtrAvailable(courseAtrAvailable)
                .courseAtrStatus(courseAtrStatus)
                .build();
    }

    // ==========================================
    // CO ANALYTICS DETAIL ENDPOINT
    // ==========================================
    public CoAnalyticsResponseDto getCoAnalytics(String programmeBatchCourseId, String coCode) {
        if (programmeBatchCourseId == null || programmeBatchCourseId.isBlank()) {
            throw new BadRequestException("programmeBatchCourseId is required");
        }
        if (coCode == null || coCode.isBlank()) {
            throw new BadRequestException("coCode is required");
        }

        // 1. Resolve Course Offering
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(programmeBatchCourseId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + programmeBatchCourseId));

        // 2. Resolve Programme Batch
        ProgrammeBatch batch = programmeBatchRepository.findById(offering.getProgrammeBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Programme batch not found: " + offering.getProgrammeBatchId()));

        MasterProgramme prog = batch.getMasterProgrammeId() != null
                ? masterProgrammeRepository.findById(batch.getMasterProgrammeId()).orElse(null)
                : null;
        Department dept = prog != null && prog.getDepartmentId() != null
                ? departmentRepository.findById(prog.getDepartmentId()).orElse(null)
                : null;
        School school = dept != null && dept.getSchoolId() != null
                ? schoolRepository.findById(dept.getSchoolId()).orElse(null)
                : null;

        // 3. Enforce Scope Security
        validateAndResolveScope(
                school != null ? school.getId() : null,
                dept != null ? dept.getId() : null,
                prog != null ? prog.getId() : null,
                batch.getId()
        );

        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope();
        if (userScope != null && userScope.isFaculty()) {
            boolean isCoord = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), userScope.getUserId()))
                    || (offering.getCourseCoordinatorEmail() != null && offering.getCourseCoordinatorEmail().equalsIgnoreCase(userScope.getEmail()));
            boolean isAssigned = isCoord || (offering.getAssignedFaculty() != null
                    && (offering.getAssignedFaculty().contains(userScope.getEmail()) || offering.getAssignedFaculty().contains(userScope.getName())));
            if (!isAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
            }
        }

        String targetCo = coCode.trim().toUpperCase();

        // 4. Configuration & Weights
        AttainmentConfiguration config = attainmentConfigurationRepository.findByProgrammeBatchCourseId(offering.getId()).orElse(null);
        BigDecimal directWeight = config != null && config.getEffectiveApprovedDirectWeight() != null
                ? config.getEffectiveApprovedDirectWeight() : new BigDecimal("80.00");
        BigDecimal indirectWeight = config != null && config.getEffectiveApprovedIndirectWeight() != null
                ? config.getEffectiveApprovedIndirectWeight() : new BigDecimal("20.00");
        BigDecimal defaultThreshold = config != null && config.getEffectiveApprovedDirectThreshold() != null
                ? config.getEffectiveApprovedDirectThreshold() : new BigDecimal("60.00");

        // 5. Course Attainment Report for Table 3 CO details
        CourseAttainmentReportDto reportDto = null;
        try {
            reportDto = attainmentReportService.getOrCreateCourseAttainmentReport(offering.getId());
        } catch (Exception ex) {
            log.warn("[AnalyticsService] Could not get or create course attainment report for offering {}: {}", offering.getId(), ex.getMessage());
        }

        CourseAttainmentReportDto.Table3Row t3 = null;
        if (reportDto != null && reportDto.getTable3CoAttainments() != null) {
            t3 = reportDto.getTable3CoAttainments().stream()
                    .filter(r -> r.getCoCode() != null && r.getCoCode().equalsIgnoreCase(targetCo))
                    .findFirst()
                    .orElse(null);
        }

        // 6. Resolve Course Outcome Definition
        List<CourseOutcome> cos = courseOutcomeRepository.findByProgrammeBatchCourseId(offering.getId());
        if (cos.isEmpty()) {
            if (reportDto != null && reportDto.getTable3CoAttainments() != null && !reportDto.getTable3CoAttainments().isEmpty()) {
                cos = reportDto.getTable3CoAttainments().stream()
                        .map(r -> CourseOutcome.builder()
                                .id("co-" + r.getCoCode())
                                .programmeBatchCourseId(offering.getId())
                                .code(r.getCoCode())
                                .statement(r.getStatement() != null ? r.getStatement() : "Course Outcome " + r.getCoCode())
                                .targetLevel(r.getTargetLevel() != null ? r.getTargetLevel() : new BigDecimal("2.50"))
                                .build())
                        .collect(Collectors.toList());
            } else {
                cos = new ArrayList<>();
                for (int i = 1; i <= 5; i++) {
                    cos.add(CourseOutcome.builder()
                            .id("co-default-" + i)
                            .programmeBatchCourseId(offering.getId())
                            .code("CO" + i)
                            .statement("Course outcome CO" + i)
                            .targetLevel(new BigDecimal("2.50"))
                            .build());
                }
            }
        }
        CourseOutcome coDef = cos.stream()
                .filter(c -> c.getCode() != null && c.getCode().equalsIgnoreCase(targetCo))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Course Outcome '" + targetCo + "' not found for course: " + offering.getId()));

        String statement = (t3 != null && t3.getStatement() != null && !t3.getStatement().isBlank())
                ? t3.getStatement()
                : (coDef.getStatement() != null ? coDef.getStatement() : "Course Outcome " + targetCo);

        BigDecimal target = (t3 != null && t3.getTargetLevel() != null)
                ? t3.getTargetLevel()
                : (coDef.getTargetLevel() != null ? coDef.getTargetLevel() : new BigDecimal("2.50"));

        // 7. PO/PSO Mappings for this CO
        CourseMappingMatrixDto matrixDto = null;
        try {
            matrixDto = outcomeService.getCourseMappings(offering.getId());
        } catch (Exception ex) {
            log.warn("[AnalyticsService] Could not resolve course mappings for offering {}: {}", offering.getId(), ex.getMessage());
        }
        Map<String, Map<String, Integer>> matrix = (matrixDto != null && matrixDto.getMatrix() != null)
                ? matrixDto.getMatrix() : Collections.emptyMap();
        Map<String, Integer> coMappings = matrix.getOrDefault(targetCo, Collections.emptyMap());
        Map<String, Integer> poMappings = new LinkedHashMap<>();
        Map<String, Integer> psoMappings = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : coMappings.entrySet()) {
            if (entry.getKey().toUpperCase().startsWith("PSO")) {
                psoMappings.put(entry.getKey(), entry.getValue());
            } else {
                poMappings.put(entry.getKey(), entry.getValue());
            }
        }

        // 8. Direct Evidence Summary
        ExaminationAttainmentResultDto examResult = null;
        try {
            examResult = attainmentCalculationService.getExaminationAttainment(offering.getId());
        } catch (Exception ex) {
            log.warn("[AnalyticsService] Could not resolve examination attainment for offering {}: {}", offering.getId(), ex.getMessage());
        }

        int totalStudents = examResult != null && examResult.getTotalStudents() != null ? examResult.getTotalStudents() : 0;
        int evaluatedStudents = (examResult != null && examResult.getStudentMarks() != null && !examResult.getStudentMarks().isEmpty())
                ? examResult.getStudentMarks().size()
                : totalStudents;
        BigDecimal directThreshold = examResult != null && examResult.getThresholdPercentage() != null
                ? examResult.getThresholdPercentage()
                : defaultThreshold;
        BigDecimal directPct = (examResult != null && examResult.getPercentageAboveThreshold() != null && examResult.getPercentageAboveThreshold().containsKey(targetCo))
                ? examResult.getPercentageAboveThreshold().get(targetCo)
                : (t3 != null && t3.getDirectPercentage() != null ? t3.getDirectPercentage() : BigDecimal.ZERO);
        Integer directLvl = (examResult != null && examResult.getCoAttainmentLevels() != null && examResult.getCoAttainmentLevels().containsKey(targetCo))
                ? examResult.getCoAttainmentLevels().get(targetCo)
                : (t3 != null && t3.getDirectLevel() != null ? t3.getDirectLevel() : 0);
        Integer studentsMeeting = (examResult != null && examResult.getStudentsAboveThreshold() != null && examResult.getStudentsAboveThreshold().containsKey(targetCo))
                ? examResult.getStudentsAboveThreshold().get(targetCo)
                : (totalStudents > 0 && directPct != null
                        ? BigDecimal.valueOf(totalStudents).multiply(directPct).divide(new BigDecimal("100.00"), 0, RoundingMode.HALF_UP).intValue()
                        : 0);

        // 9. Indirect Evidence Summary
        SurveyAttainmentResultDto surveyResult = null;
        try {
            surveyResult = attainmentCalculationService.getSurveyAttainment(offering.getId());
        } catch (Exception ex) {
            log.warn("[AnalyticsService] Could not resolve survey attainment for offering {}: {}", offering.getId(), ex.getMessage());
        }

        int responseCount = surveyResult != null && surveyResult.getTotalStudents() != null ? surveyResult.getTotalStudents() : 0;
        Map<String, Integer> levelDist = new LinkedHashMap<>();
        if (surveyResult != null) {
            Integer l1 = surveyResult.getLevel1Counts() != null ? surveyResult.getLevel1Counts().get(targetCo) : 0;
            Integer l2 = surveyResult.getLevel2Counts() != null ? surveyResult.getLevel2Counts().get(targetCo) : 0;
            Integer l3 = surveyResult.getLevel3Counts() != null ? surveyResult.getLevel3Counts().get(targetCo) : 0;
            levelDist.put("Slight (Level 1)", l1 != null ? l1 : 0);
            levelDist.put("Moderate (Level 2)", l2 != null ? l2 : 0);
            levelDist.put("Substantial (Level 3)", l3 != null ? l3 : 0);
        }
        BigDecimal indirectScore = (surveyResult != null && surveyResult.getIndirectAttainmentScores() != null && surveyResult.getIndirectAttainmentScores().containsKey(targetCo))
                ? surveyResult.getIndirectAttainmentScores().get(targetCo)
                : (t3 != null && t3.getIndirectScore() != null ? t3.getIndirectScore() : BigDecimal.ZERO);
        Integer indirectLvl = (surveyResult != null && surveyResult.getCoAttainmentLevels() != null && surveyResult.getCoAttainmentLevels().containsKey(targetCo))
                ? surveyResult.getCoAttainmentLevels().get(targetCo)
                : (t3 != null && t3.getIndirectLevel() != null ? t3.getIndirectLevel() : 0);
        BigDecimal indirectPct = (surveyResult != null && surveyResult.getOverallIndirectPercentages() != null && surveyResult.getOverallIndirectPercentages().containsKey(targetCo))
                ? surveyResult.getOverallIndirectPercentages().get(targetCo)
                : (t3 != null && t3.getIndirectPercentage() != null ? t3.getIndirectPercentage() : BigDecimal.ZERO);

        // Compute Direct, Indirect, and Overall Attainment Levels reliably
        BigDecimal directAttainment = (t3 != null && t3.getDirectLevel() != null)
                ? BigDecimal.valueOf(t3.getDirectLevel()).setScale(2, RoundingMode.HALF_UP)
                : (directLvl != null ? BigDecimal.valueOf(directLvl).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        BigDecimal indirectAttainment = (t3 != null && t3.getIndirectLevel() != null)
                ? BigDecimal.valueOf(t3.getIndirectLevel()).setScale(2, RoundingMode.HALF_UP)
                : (indirectLvl != null ? BigDecimal.valueOf(indirectLvl).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));

        BigDecimal overallAttainment = (t3 != null && t3.getFinalAttainment() != null)
                ? t3.getFinalAttainment()
                : directAttainment.multiply(directWeight).divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP)
                        .add(indirectAttainment.multiply(indirectWeight).divide(new BigDecimal("100.00"), 4, RoundingMode.HALF_UP))
                        .setScale(2, RoundingMode.HALF_UP);

        Boolean targetMet = (overallAttainment != null && target != null)
                ? overallAttainment.compareTo(target) >= 0
                : (t3 != null && t3.getTargetMet() != null ? t3.getTargetMet() : false);

        String observation = (t3 != null && t3.getObservation() != null)
                ? t3.getObservation()
                : (targetMet ? "Target achieved (" + overallAttainment + " >= " + target + ")" : "Target not achieved (" + overallAttainment + " < " + target + ")");

        CoDirectEvidenceSummaryDto directSummary = CoDirectEvidenceSummaryDto.builder()
                .totalStudents(totalStudents)
                .evaluatedStudents(evaluatedStudents)
                .threshold(directThreshold)
                .directPercentage(directPct != null ? directPct : BigDecimal.ZERO)
                .directLevel(directLvl != null ? directLvl : 0)
                .directAttainment(directAttainment)
                .studentsMeetingThreshold(studentsMeeting)
                .build();

        CoIndirectEvidenceSummaryDto indirectSummary = CoIndirectEvidenceSummaryDto.builder()
                .responseCount(responseCount)
                .levelDistribution(levelDist)
                .indirectScore(indirectScore != null ? indirectScore : BigDecimal.ZERO)
                .indirectLevel(indirectLvl != null ? indirectLvl : 0)
                .indirectPercentage(indirectPct != null ? indirectPct : BigDecimal.ZERO)
                .build();

        String coordinatorName = offering.getCourseCoordinatorName() != null && !offering.getCourseCoordinatorName().isBlank()
                ? offering.getCourseCoordinatorName()
                : (offering.getAssignedFaculty() != null ? offering.getAssignedFaculty() : "");

        return CoAnalyticsResponseDto.builder()
                .programmeBatchId(batch.getId())
                .programmeBatchCourseId(offering.getId())
                .courseCode(offering.getEffectiveCourseCode() != null ? offering.getEffectiveCourseCode() : offering.getCode())
                .courseName(offering.getEffectiveCourseName() != null ? offering.getEffectiveCourseName() : offering.getName())
                .semester(offering.getSemester())
                .courseCoordinator(coordinatorName)
                .batchName(batch.getName())
                .masterProgrammeId(prog != null ? prog.getId() : batch.getMasterProgrammeId())
                .programmeName(prog != null ? prog.getName() : "")
                .coCode(targetCo)
                .coStatement(statement)
                .target(target)
                .targetMet(targetMet)
                .directAttainment(directAttainment)
                .indirectAttainment(indirectAttainment)
                .overallAttainment(overallAttainment)
                .directWeight(directWeight)
                .indirectWeight(indirectWeight)
                .observation(observation)
                .poMappings(poMappings)
                .psoMappings(psoMappings)
                .directEvidenceSummary(directSummary)
                .indirectEvidenceSummary(indirectSummary)
                .build();
    }

    // ==========================================
    // IQAC ANALYTICS CONFIGURATION METHODS
    // ==========================================
    public StudentEvidenceThresholdConfigDto getStudentEvidenceThresholdConfig() {
        IqacAnalyticsConfiguration config = iqacAnalyticsConfigurationRepository.findById("GLOBAL")
                .orElseGet(() -> iqacAnalyticsConfigurationRepository.save(
                        IqacAnalyticsConfiguration.builder()
                                .id("GLOBAL")
                                .studentEvidenceThreshold(new BigDecimal("50.00"))
                                .updatedBy("SYSTEM")
                                .build()
                ));
        return StudentEvidenceThresholdConfigDto.builder()
                .thresholdPercentage(config.getStudentEvidenceThreshold().setScale(2, RoundingMode.HALF_UP))
                .updatedBy(config.getUpdatedBy())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    public BigDecimal getStudentEvidenceThreshold() {
        return getStudentEvidenceThresholdConfig().getThresholdPercentage();
    }

    @Transactional
    public StudentEvidenceThresholdConfigDto updateStudentEvidenceThreshold(BigDecimal threshold, String updatedBy) {
        if (threshold == null || threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.valueOf(100.00)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student Performance Evidence Threshold must be between 0.00% and 100.00%");
        }
        BigDecimal normalized = threshold.setScale(2, RoundingMode.HALF_UP);
        IqacAnalyticsConfiguration config = iqacAnalyticsConfigurationRepository.findById("GLOBAL")
                .orElse(IqacAnalyticsConfiguration.builder().id("GLOBAL").build());

        config.setStudentEvidenceThreshold(normalized);
        config.setUpdatedBy(updatedBy != null && !updatedBy.isBlank() ? updatedBy : "IQAC Admin");
        config.setUpdatedAt(ZonedDateTime.now());

        IqacAnalyticsConfiguration saved = iqacAnalyticsConfigurationRepository.save(config);
        return StudentEvidenceThresholdConfigDto.builder()
                .thresholdPercentage(saved.getStudentEvidenceThreshold().setScale(2, RoundingMode.HALF_UP))
                .updatedBy(saved.getUpdatedBy())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    private String maskPrn(String prn) {
        if (prn == null || prn.isBlank()) return "—";
        String trimmed = prn.trim();
        if (trimmed.length() <= 4) return trimmed;
        if (trimmed.length() <= 8) {
            return trimmed.substring(0, 2) + "***" + trimmed.substring(trimmed.length() - 2);
        }
        return trimmed.substring(0, 4) + "***" + trimmed.substring(trimmed.length() - 4);
    }

    // ==========================================
    // 6. HISTORICAL TRENDS ENDPOINT
    // ==========================================
    public List<ScopedTrendSeriesDto> getTrends(String schoolId, String departmentId, String masterProgrammeId, Integer numCohorts) {
        ResolvedScope scope = validateAndResolveScope(schoolId, departmentId, masterProgrammeId, null);
        List<ProgrammeBatch> batchesInScope = getBatchesInScope(scope);

        // Group batches by MasterProgramme
        Map<String, List<ProgrammeBatch>> batchesByProg = batchesInScope.stream()
                .collect(Collectors.groupingBy(ProgrammeBatch::getMasterProgrammeId));

        Map<String, MasterProgramme> progMap = masterProgrammeRepository.findAll().stream()
                .collect(Collectors.toMap(MasterProgramme::getId, p -> p, (a, b) -> a));

        List<String> batchIds = batchesInScope.stream().map(ProgrammeBatch::getId).toList();
        Map<String, ProgrammeBatchAttainmentReport> reportMap = batchIds.isEmpty() ? Collections.emptyMap() :
                programmeBatchAttainmentReportRepository.findByProgrammeBatchIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        List<ScopedTrendSeriesDto> seriesList = new ArrayList<>();

        for (Map.Entry<String, List<ProgrammeBatch>> entry : batchesByProg.entrySet()) {
            String progId = entry.getKey();
            MasterProgramme prog = progMap.get(progId);
            String progName = prog != null ? prog.getName() : "Programme " + progId;

            List<ProgrammeBatch> pBatches = entry.getValue().stream()
                    .sorted(Comparator.comparing((ProgrammeBatch b) -> b.getStartYear() != null ? b.getStartYear() : 0))
                    .collect(Collectors.toList());

            List<ResolvedBatchAnalyticsData> resolvedProgBatches = pBatches.stream()
                    .map(b -> resolveBatchData(b, reportMap))
                    .filter(r -> r.hasActiveData)
                    .collect(Collectors.toList());

            // If numCohorts is explicitly requested, slice to the last numCohorts; otherwise return all available cohorts
            if (numCohorts != null && numCohorts > 0 && resolvedProgBatches.size() > numCohorts) {
                resolvedProgBatches = resolvedProgBatches.subList(resolvedProgBatches.size() - numCohorts, resolvedProgBatches.size());
            }

            List<CohortOutcomeDataPointDto> points = new ArrayList<>();
            for (ResolvedBatchAnalyticsData bData : resolvedProgBatches) {
                for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : bData.pos) {
                    if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                        BigDecimal gap = po.getFinalAttainment().subtract(po.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                        points.add(CohortOutcomeDataPointDto.builder()
                                .programmeBatchId(bData.batchId)
                                .batchName(bData.batchName)
                                .startYear(bData.startYear)
                                .endYear(bData.endYear)
                                .outcomeCode(po.getPoCode())
                                .configuredTarget(po.getTargetLevel())
                                .overallAttainment(po.getFinalAttainment())
                                .directAttainment(po.getDirectAttainment())
                                .indirectAttainment(po.getIndirectAttainment())
                                .gap(gap)
                                .targetMet(po.getFinalAttainment().compareTo(po.getTargetLevel()) >= 0)
                                .build());
                    }
                }

                for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : bData.psos) {
                    if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                        BigDecimal gap = pso.getFinalAttainment().subtract(pso.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                        points.add(CohortOutcomeDataPointDto.builder()
                                .programmeBatchId(bData.batchId)
                                .batchName(bData.batchName)
                                .startYear(bData.startYear)
                                .endYear(bData.endYear)
                                .outcomeCode(pso.getPsoCode())
                                .configuredTarget(pso.getTargetLevel())
                                .overallAttainment(pso.getFinalAttainment())
                                .directAttainment(pso.getDirectAttainment())
                                .indirectAttainment(pso.getIndirectAttainment())
                                .gap(gap)
                                .targetMet(pso.getFinalAttainment().compareTo(pso.getTargetLevel()) >= 0)
                                .build());
                    }
                }
            }

            if (!points.isEmpty()) {
                seriesList.add(ScopedTrendSeriesDto.builder()
                        .scopeLevel("PROGRAMME")
                        .scopeIdentifier(progId)
                        .scopeName(progName)
                        .cohortDataPoints(points)
                        .build());
            }
        }

        return seriesList;
    }

    // ==========================================
    // 6A. HISTORICAL PROGRAMME ATTAINMENT ENDPOINT
    // ==========================================
    public HistoricalProgrammeAttainmentResponseDto getHistoricalProgrammeAttainment(String masterProgrammeId, String outcomeCode) {
        return getHistoricalProgrammeAttainment(masterProgrammeId, null, outcomeCode);
    }

    public HistoricalProgrammeAttainmentResponseDto getHistoricalProgrammeAttainment(
            String masterProgrammeId, String programmeBatchId, String outcomeCode) {

        String effectiveMasterProgrammeId = (masterProgrammeId != null && !masterProgrammeId.isBlank() && !"undefined".equalsIgnoreCase(masterProgrammeId.trim()))
                ? masterProgrammeId.trim() : null;

        String resolvedCurrentBatchId = (programmeBatchId != null && !programmeBatchId.isBlank() && !"undefined".equalsIgnoreCase(programmeBatchId.trim()))
                ? programmeBatchId.trim() : null;

        if (effectiveMasterProgrammeId == null && resolvedCurrentBatchId != null) {
            ProgrammeBatch pb = programmeBatchRepository.findById(resolvedCurrentBatchId).orElse(null);
            if (pb != null) {
                effectiveMasterProgrammeId = pb.getMasterProgrammeId();
            }
        }

        // If masterProgrammeId was actually a programmeBatchId passed by caller
        if (effectiveMasterProgrammeId != null && !masterProgrammeRepository.existsById(effectiveMasterProgrammeId)) {
            ProgrammeBatch pb = programmeBatchRepository.findById(effectiveMasterProgrammeId).orElse(null);
            if (pb != null) {
                resolvedCurrentBatchId = pb.getId();
                effectiveMasterProgrammeId = pb.getMasterProgrammeId();
            }
        }

        if (effectiveMasterProgrammeId == null || effectiveMasterProgrammeId.isBlank()) {
            throw new BadRequestException("masterProgrammeId or programmeBatchId is required.");
        }

        final String finalMasterProgrammeId = effectiveMasterProgrammeId;

        validateAndResolveScope(null, null, finalMasterProgrammeId, null);

        MasterProgramme prog = masterProgrammeRepository.findById(finalMasterProgrammeId)
                .orElseThrow(() -> new ResourceNotFoundException("MasterProgramme not found: " + finalMasterProgrammeId));

        List<ProgrammeBatch> allBatches = programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(finalMasterProgrammeId);

        // Include ALL batches for the programme (active, in-progress, completed, graduated) - none excluded
        List<ProgrammeBatch> batches = allBatches.stream()
                .sorted(Comparator.comparing((ProgrammeBatch b) -> b.getStartYear() != null ? b.getStartYear() : 0)
                        .thenComparing(b -> b.getEndYear() != null ? b.getEndYear() : 0))
                .collect(Collectors.toList());

        List<HistoricalProgrammeAttainmentResponseDto.HistoricalBatchSummaryDto> batchSummaries = new ArrayList<>();
        List<HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto> allDataPoints = new ArrayList<>();
        Set<String> distinctOutcomes = new TreeSet<>(this::compareOutcomeCodes);

        if (!batches.isEmpty()) {
            List<String> batchIds = batches.stream().map(ProgrammeBatch::getId).toList();
            Map<String, ProgrammeBatchAttainmentReport> reportMap = programmeBatchAttainmentReportRepository
                    .findByProgrammeBatchIdIn(batchIds).stream()
                    .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

            for (ProgrammeBatch batch : batches) {
                batchSummaries.add(HistoricalProgrammeAttainmentResponseDto.HistoricalBatchSummaryDto.builder()
                        .batchId(batch.getId())
                        .batchName(batch.getName())
                        .startYear(batch.getStartYear())
                        .endYear(batch.getEndYear())
                        .status(batch.getStatus())
                        .build());

                Map<String, OutcomeItemMetrics> outcomeMetrics = resolveBatchOutcomeMetrics(batch, reportMap);
                for (OutcomeItemMetrics metric : outcomeMetrics.values()) {
                    distinctOutcomes.add(metric.outcomeCode());
                    allDataPoints.add(HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto.builder()
                            .batchId(batch.getId())
                            .batchName(batch.getName())
                            .startYear(batch.getStartYear())
                            .endYear(batch.getEndYear())
                            .status(batch.getStatus())
                            .outcomeCode(metric.outcomeCode())
                            .outcomeType(metric.outcomeType())
                            .directAttainment(metric.directAttainment())
                            .indirectAttainment(metric.indirectAttainment())
                            .finalAttainment(metric.finalAttainment())
                            .targetLevel(metric.targetLevel())
                            .gap(metric.gap())
                            .targetMet(metric.targetMet())
                            .build());
                }
            }
        }

        // Filter data points by outcomeCode if specified
        List<HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto> filteredDataPoints = allDataPoints;
        if (outcomeCode != null && !outcomeCode.isBlank()) {
            final String filterCode = outcomeCode.trim().toUpperCase();
            filteredDataPoints = allDataPoints.stream()
                    .filter(dp -> dp.getOutcomeCode() != null && dp.getOutcomeCode().equalsIgnoreCase(filterCode))
                    .collect(Collectors.toList());
        }

        return HistoricalProgrammeAttainmentResponseDto.builder()
                .masterProgrammeId(prog.getId())
                .programmeName(prog.getName())
                .currentBatchId(resolvedCurrentBatchId)
                .batches(batchSummaries)
                .outcomes(new ArrayList<>(distinctOutcomes))
                .dataPoints(filteredDataPoints)
                .build();
    }

    // ==========================================
    // 6B. BATCH COMPARISON ANALYTICS ENDPOINT
    // ==========================================
    public BatchComparisonAnalyticsResponseDto compareBatches(String programmeBatchId1, String programmeBatchId2) {
        if (programmeBatchId1 == null || programmeBatchId1.isBlank() || programmeBatchId2 == null || programmeBatchId2.isBlank()) {
            throw new BadRequestException("Both programmeBatchId1 and programmeBatchId2 are required.");
        }
        String id1 = programmeBatchId1.trim();
        String id2 = programmeBatchId2.trim();
        if (id1.equalsIgnoreCase(id2)) {
            throw new BadRequestException("Cannot compare a batch with itself. Please select two different batches.");
        }

        ProgrammeBatch batch1 = programmeBatchRepository.findById(id1)
                .orElseThrow(() -> new ResourceNotFoundException("ProgrammeBatch not found: " + id1));
        ProgrammeBatch batch2 = programmeBatchRepository.findById(id2)
                .orElseThrow(() -> new ResourceNotFoundException("ProgrammeBatch not found: " + id2));

        // Validate security scope for both batches independently
        validateAndResolveScope(null, null, null, batch1.getId());
        validateAndResolveScope(null, null, null, batch2.getId());

        // Resolve programmes for both batches (cross-programme comparison allowed)
        MasterProgramme prog1 = masterProgrammeRepository.findById(batch1.getMasterProgrammeId()).orElse(null);
        MasterProgramme prog2 = masterProgrammeRepository.findById(batch2.getMasterProgrammeId()).orElse(null);

        BatchComparisonAnalyticsResponseDto.BatchMetaDto meta1 = BatchComparisonAnalyticsResponseDto.BatchMetaDto.builder()
                .programmeId(batch1.getMasterProgrammeId())
                .programmeName(prog1 != null ? prog1.getName() : "Programme " + batch1.getMasterProgrammeId())
                .batchId(batch1.getId())
                .batchName(batch1.getName())
                .startYear(batch1.getStartYear())
                .endYear(batch1.getEndYear())
                .status(batch1.getStatus())
                .build();

        BatchComparisonAnalyticsResponseDto.BatchMetaDto meta2 = BatchComparisonAnalyticsResponseDto.BatchMetaDto.builder()
                .programmeId(batch2.getMasterProgrammeId())
                .programmeName(prog2 != null ? prog2.getName() : "Programme " + batch2.getMasterProgrammeId())
                .batchId(batch2.getId())
                .batchName(batch2.getName())
                .startYear(batch2.getStartYear())
                .endYear(batch2.getEndYear())
                .status(batch2.getStatus())
                .build();

        Map<String, ProgrammeBatchAttainmentReport> reportMap = programmeBatchAttainmentReportRepository
                .findByProgrammeBatchIdIn(List.of(batch1.getId(), batch2.getId())).stream()
                .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        Map<String, OutcomeItemMetrics> metricsMap1 = resolveBatchOutcomeMetrics(batch1, reportMap);
        Map<String, OutcomeItemMetrics> metricsMap2 = resolveBatchOutcomeMetrics(batch2, reportMap);

        Set<String> allOutcomeCodes = new TreeSet<>(this::compareOutcomeCodes);
        allOutcomeCodes.addAll(metricsMap1.keySet());
        allOutcomeCodes.addAll(metricsMap2.keySet());

        List<BatchComparisonAnalyticsResponseDto.OutcomeComparisonItemDto> comparisonItems = new ArrayList<>();
        for (String code : allOutcomeCodes) {
            OutcomeItemMetrics m1 = metricsMap1.get(code);
            OutcomeItemMetrics m2 = metricsMap2.get(code);

            String outcomeType = (m1 != null) ? m1.outcomeType() : (m2 != null ? m2.outcomeType() : (code.startsWith("PSO") ? "PSO" : "PO"));

            BatchComparisonAnalyticsResponseDto.OutcomeMetricsDto b1Metrics = (m1 != null)
                    ? BatchComparisonAnalyticsResponseDto.OutcomeMetricsDto.builder()
                    .directAttainment(m1.directAttainment())
                    .indirectAttainment(m1.indirectAttainment())
                    .finalAttainment(m1.finalAttainment())
                    .targetLevel(m1.targetLevel())
                    .gap(m1.gap())
                    .targetMet(m1.targetMet())
                    .build()
                    : null;

            BatchComparisonAnalyticsResponseDto.OutcomeMetricsDto b2Metrics = (m2 != null)
                    ? BatchComparisonAnalyticsResponseDto.OutcomeMetricsDto.builder()
                    .directAttainment(m2.directAttainment())
                    .indirectAttainment(m2.indirectAttainment())
                    .finalAttainment(m2.finalAttainment())
                    .targetLevel(m2.targetLevel())
                    .gap(m2.gap())
                    .targetMet(m2.targetMet())
                    .build()
                    : null;

            comparisonItems.add(BatchComparisonAnalyticsResponseDto.OutcomeComparisonItemDto.builder()
                    .outcomeCode(code)
                    .outcomeType(outcomeType)
                    .batch1(b1Metrics)
                    .batch2(b2Metrics)
                    .build());
        }

        return BatchComparisonAnalyticsResponseDto.builder()
                .batch1(meta1)
                .batch2(meta2)
                .outcomes(comparisonItems)
                .build();
    }

    // ==========================================
    // 6C. HISTORICAL COURSE ATTAINMENT ENDPOINT
    // ==========================================
    public HistoricalCourseAttainmentResponseDto getHistoricalCourseAttainment(
            String programmeBatchCourseId, String coCode) {

        if (programmeBatchCourseId == null || programmeBatchCourseId.isBlank()) {
            throw new BadRequestException("programmeBatchCourseId is required.");
        }

        ProgrammeBatchCourse originatingCourse = programmeBatchCourseRepository.findById(programmeBatchCourseId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + programmeBatchCourseId));
        if (originatingCourse.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Course offering not found: " + programmeBatchCourseId);
        }

        ProgrammeBatch originatingBatch = programmeBatchRepository.findById(originatingCourse.getProgrammeBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Programme batch not found: " + originatingCourse.getProgrammeBatchId()));
        if (originatingBatch.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Programme batch not found: " + originatingCourse.getProgrammeBatchId());
        }

        MasterProgramme prog = originatingBatch.getMasterProgrammeId() != null
                ? masterProgrammeRepository.findById(originatingBatch.getMasterProgrammeId()).orElse(null)
                : null;
        if (prog == null) {
            throw new ResourceNotFoundException("Master programme not found for batch: " + originatingBatch.getId());
        }

        Department dept = prog.getDepartmentId() != null
                ? departmentRepository.findById(prog.getDepartmentId()).orElse(null)
                : null;
        School school = dept != null && dept.getSchoolId() != null
                ? schoolRepository.findById(dept.getSchoolId()).orElse(null)
                : null;

        validateAndResolveScope(
                school != null ? school.getId() : null,
                dept != null ? dept.getId() : null,
                prog.getId(),
                originatingBatch.getId()
        );
        enforceCourseOfferingAccess(originatingCourse, originatingBatch);

        String targetCourseCode = originatingCourse.getEffectiveCourseCode();
        if (targetCourseCode == null || targetCourseCode.isBlank()) {
            throw new BadRequestException("Course code could not be determined for course offering: " + programmeBatchCourseId);
        }
        targetCourseCode = targetCourseCode.trim();

        List<ProgrammeBatch> allBatches = programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(prog.getId());
        if (allBatches == null || allBatches.isEmpty()) {
            allBatches = List.of(originatingBatch);
        }

        List<ProgrammeBatch> sortedBatches = allBatches.stream()
                .sorted(Comparator.comparing((ProgrammeBatch b) -> b.getStartYear() != null ? b.getStartYear() : 0)
                        .thenComparing(b -> b.getEndYear() != null ? b.getEndYear() : 0)
                        .thenComparing(ProgrammeBatch::getId))
                .toList();

        List<String> batchIds = sortedBatches.stream().map(ProgrammeBatch::getId).toList();
        List<ProgrammeBatchCourse> allBatchCourses = programmeBatchCourseRepository.findByProgrammeBatchIdInAndDeletedAtIsNull(batchIds);

        final String courseCodeToMatch = targetCourseCode;
        List<ProgrammeBatchCourse> matchingCourses = allBatchCourses.stream()
                .filter(c -> c.getEffectiveCourseCode() != null && c.getEffectiveCourseCode().trim().equalsIgnoreCase(courseCodeToMatch))
                .toList();

        Map<String, ProgrammeBatchCourse> courseByBatchId = new LinkedHashMap<>();
        for (ProgrammeBatchCourse c : matchingCourses) {
            courseByBatchId.putIfAbsent(c.getProgrammeBatchId(), c);
        }
        courseByBatchId.put(originatingBatch.getId(), originatingCourse);

        List<String> offeringIds = courseByBatchId.values().stream().map(ProgrammeBatchCourse::getId).toList();
        Map<String, CourseAttainmentReport> reportMap = courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                .collect(Collectors.toMap(CourseAttainmentReport::getProgrammeBatchCourseId, r -> r, (a, b) -> a));
        Map<String, AttainmentConfiguration> configMap = attainmentConfigurationRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                .collect(Collectors.toMap(AttainmentConfiguration::getProgrammeBatchCourseId, c -> c, (a, b) -> a));
        Map<String, List<CourseOutcome>> coMap = courseOutcomeRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                .collect(Collectors.groupingBy(CourseOutcome::getProgrammeBatchCourseId));

        ResolvedCourseAttainmentData originatingData = resolveCourseAttainmentData(
                originatingCourse,
                originatingBatch,
                reportMap.get(originatingCourse.getId()),
                configMap.get(originatingCourse.getId()),
                coMap.getOrDefault(originatingCourse.getId(), Collections.emptyList())
        );

        String targetCoCode = (coCode != null && !coCode.isBlank()) ? coCode.trim().toUpperCase() : null;
        if (targetCoCode != null && !originatingData.coMetrics().containsKey(targetCoCode)) {
            throw new ResourceNotFoundException("Course Outcome '" + coCode + "' not found for course offering: " + programmeBatchCourseId);
        }

        List<HistoricalCourseAttainmentResponseDto.HistoricalCourseBatchDto> batchDtos = new ArrayList<>();
        List<HistoricalCourseAttainmentResponseDto.HistoricalCoDataPointDto> coDataPoints = new ArrayList<>();
        Set<String> distinctCoCodes = new TreeSet<>(this::compareCoCodes);

        for (ProgrammeBatch batch : sortedBatches) {
            ProgrammeBatchCourse offering = courseByBatchId.get(batch.getId());
            if (offering == null) continue;

            ResolvedCourseAttainmentData data = (offering.getId().equals(originatingCourse.getId()))
                    ? originatingData
                    : resolveCourseAttainmentData(
                            offering,
                            batch,
                            reportMap.get(offering.getId()),
                            configMap.get(offering.getId()),
                            coMap.getOrDefault(offering.getId(), Collections.emptyList())
                    );

            batchDtos.add(HistoricalCourseAttainmentResponseDto.HistoricalCourseBatchDto.builder()
                    .programmeBatchCourseId(offering.getId())
                    .programmeBatchId(batch.getId())
                    .batchName(batch.getName())
                    .startYear(batch.getStartYear())
                    .endYear(batch.getEndYear())
                    .status(batch.getStatus())
                    .semester(offering.getSemester())
                    .courseCode(offering.getEffectiveCourseCode())
                    .courseName(offering.getEffectiveCourseName())
                    .directAttainment(data.directAttainment())
                    .indirectAttainment(data.indirectAttainment())
                    .overallCourseAttainment(data.overallCourseAttainment())
                    .directWeight(data.directWeight())
                    .indirectWeight(data.indirectWeight())
                    .build());

            for (Map.Entry<String, ResolvedCoMetricData> entry : data.coMetrics().entrySet()) {
                String code = entry.getKey();
                if (targetCoCode != null && !targetCoCode.equalsIgnoreCase(code)) {
                    continue;
                }
                distinctCoCodes.add(code);
                ResolvedCoMetricData metric = entry.getValue();
                coDataPoints.add(HistoricalCourseAttainmentResponseDto.HistoricalCoDataPointDto.builder()
                        .programmeBatchCourseId(offering.getId())
                        .programmeBatchId(batch.getId())
                        .batchName(batch.getName())
                        .startYear(batch.getStartYear())
                        .endYear(batch.getEndYear())
                        .status(batch.getStatus())
                        .coCode(metric.coCode())
                        .statement(metric.statement())
                        .directAttainment(metric.directAttainment())
                        .indirectAttainment(metric.indirectAttainment())
                        .overallAttainment(metric.overallAttainment())
                        .targetLevel(metric.targetLevel())
                        .targetMet(metric.targetMet())
                        .build());
            }
        }

        return HistoricalCourseAttainmentResponseDto.builder()
                .courseCode(targetCourseCode)
                .courseName(originatingCourse.getEffectiveCourseName())
                .masterProgrammeId(prog.getId())
                .programmeName(prog.getName())
                .currentProgrammeBatchCourseId(originatingCourse.getId())
                .currentBatchId(originatingBatch.getId())
                .currentBatchName(originatingBatch.getName())
                .currentBatchStatus(originatingBatch.getStatus())
                .currentSemester(originatingCourse.getSemester())
                .directWeight(originatingData.directWeight())
                .indirectWeight(originatingData.indirectWeight())
                .batches(batchDtos)
                .courseOutcomes(new ArrayList<>(distinctCoCodes))
                .coDataPoints(coDataPoints)
                .build();
    }

    // ==========================================
    // 6D. COMPARE COURSES ANALYTICS ENDPOINT
    // ==========================================
    public CourseComparisonAnalyticsResponseDto compareCourses(
            String programmeBatchCourseId1, String programmeBatchCourseId2) {

        if (programmeBatchCourseId1 == null || programmeBatchCourseId1.isBlank() ||
                programmeBatchCourseId2 == null || programmeBatchCourseId2.isBlank()) {
            throw new BadRequestException("Both programmeBatchCourseId1 and programmeBatchCourseId2 are required.");
        }
        String id1 = programmeBatchCourseId1.trim();
        String id2 = programmeBatchCourseId2.trim();
        if (id1.equalsIgnoreCase(id2)) {
            throw new BadRequestException("Cannot compare a course offering with itself. Please select two different course offerings.");
        }

        ProgrammeBatchCourse offering1 = programmeBatchCourseRepository.findById(id1)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + id1));
        if (offering1.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Course offering not found: " + id1);
        }

        ProgrammeBatchCourse offering2 = programmeBatchCourseRepository.findById(id2)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found: " + id2));
        if (offering2.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Course offering not found: " + id2);
        }

        ProgrammeBatch batch1 = programmeBatchRepository.findById(offering1.getProgrammeBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Programme batch not found: " + offering1.getProgrammeBatchId()));
        if (batch1.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Programme batch not found: " + offering1.getProgrammeBatchId());
        }

        ProgrammeBatch batch2 = programmeBatchRepository.findById(offering2.getProgrammeBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Programme batch not found: " + offering2.getProgrammeBatchId()));
        if (batch2.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Programme batch not found: " + offering2.getProgrammeBatchId());
        }

        MasterProgramme prog1 = batch1.getMasterProgrammeId() != null
                ? masterProgrammeRepository.findById(batch1.getMasterProgrammeId()).orElse(null) : null;
        Department dept1 = prog1 != null && prog1.getDepartmentId() != null
                ? departmentRepository.findById(prog1.getDepartmentId()).orElse(null) : null;
        School school1 = dept1 != null && dept1.getSchoolId() != null
                ? schoolRepository.findById(dept1.getSchoolId()).orElse(null) : null;

        MasterProgramme prog2 = batch2.getMasterProgrammeId() != null
                ? masterProgrammeRepository.findById(batch2.getMasterProgrammeId()).orElse(null) : null;
        Department dept2 = prog2 != null && prog2.getDepartmentId() != null
                ? departmentRepository.findById(prog2.getDepartmentId()).orElse(null) : null;
        School school2 = dept2 != null && dept2.getSchoolId() != null
                ? schoolRepository.findById(dept2.getSchoolId()).orElse(null) : null;

        // Validate security scope independently for both courses
        validateAndResolveScope(
                school1 != null ? school1.getId() : null,
                dept1 != null ? dept1.getId() : null,
                prog1 != null ? prog1.getId() : null,
                batch1.getId()
        );
        enforceCourseOfferingAccess(offering1, batch1);

        validateAndResolveScope(
                school2 != null ? school2.getId() : null,
                dept2 != null ? dept2.getId() : null,
                prog2 != null ? prog2.getId() : null,
                batch2.getId()
        );
        enforceCourseOfferingAccess(offering2, batch2);

        // Batch load reports, configs, cos
        List<String> offeringIds = List.of(offering1.getId(), offering2.getId());
        Map<String, CourseAttainmentReport> reportMap = courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                .collect(Collectors.toMap(CourseAttainmentReport::getProgrammeBatchCourseId, r -> r, (a, b) -> a));
        Map<String, AttainmentConfiguration> configMap = attainmentConfigurationRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                .collect(Collectors.toMap(AttainmentConfiguration::getProgrammeBatchCourseId, c -> c, (a, b) -> a));
        Map<String, List<CourseOutcome>> coMap = courseOutcomeRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                .collect(Collectors.groupingBy(CourseOutcome::getProgrammeBatchCourseId));

        ResolvedCourseAttainmentData data1 = resolveCourseAttainmentData(
                offering1, batch1, reportMap.get(offering1.getId()), configMap.get(offering1.getId()), coMap.getOrDefault(offering1.getId(), Collections.emptyList()));
        ResolvedCourseAttainmentData data2 = resolveCourseAttainmentData(
                offering2, batch2, reportMap.get(offering2.getId()), configMap.get(offering2.getId()), coMap.getOrDefault(offering2.getId(), Collections.emptyList()));

        CourseComparisonAnalyticsResponseDto.CourseMetaDto meta1 = CourseComparisonAnalyticsResponseDto.CourseMetaDto.builder()
                .programmeBatchCourseId(offering1.getId())
                .programmeBatchId(batch1.getId())
                .batchName(batch1.getName())
                .startYear(batch1.getStartYear())
                .endYear(batch1.getEndYear())
                .batchStatus(batch1.getStatus())
                .masterProgrammeId(prog1 != null ? prog1.getId() : batch1.getMasterProgrammeId())
                .programmeName(prog1 != null ? prog1.getName() : "Programme " + batch1.getMasterProgrammeId())
                .courseCode(offering1.getEffectiveCourseCode())
                .courseName(offering1.getEffectiveCourseName())
                .semester(offering1.getSemester())
                .directAttainment(data1.directAttainment())
                .indirectAttainment(data1.indirectAttainment())
                .overallCourseAttainment(data1.overallCourseAttainment())
                .directWeight(data1.directWeight())
                .indirectWeight(data1.indirectWeight())
                .build();

        CourseComparisonAnalyticsResponseDto.CourseMetaDto meta2 = CourseComparisonAnalyticsResponseDto.CourseMetaDto.builder()
                .programmeBatchCourseId(offering2.getId())
                .programmeBatchId(batch2.getId())
                .batchName(batch2.getName())
                .startYear(batch2.getStartYear())
                .endYear(batch2.getEndYear())
                .batchStatus(batch2.getStatus())
                .masterProgrammeId(prog2 != null ? prog2.getId() : batch2.getMasterProgrammeId())
                .programmeName(prog2 != null ? prog2.getName() : "Programme " + batch2.getMasterProgrammeId())
                .courseCode(offering2.getEffectiveCourseCode())
                .courseName(offering2.getEffectiveCourseName())
                .semester(offering2.getSemester())
                .directAttainment(data2.directAttainment())
                .indirectAttainment(data2.indirectAttainment())
                .overallCourseAttainment(data2.overallCourseAttainment())
                .directWeight(data2.directWeight())
                .indirectWeight(data2.indirectWeight())
                .build();

        Set<String> allCoCodes = new TreeSet<>(this::compareCoCodes);
        allCoCodes.addAll(data1.coMetrics().keySet());
        allCoCodes.addAll(data2.coMetrics().keySet());

        List<CourseComparisonAnalyticsResponseDto.CoComparisonItemDto> coComparisons = new ArrayList<>();
        for (String code : allCoCodes) {
            ResolvedCoMetricData m1 = data1.coMetrics().get(code);
            ResolvedCoMetricData m2 = data2.coMetrics().get(code);

            String stmt = (m1 != null && m1.statement() != null) ? m1.statement()
                    : (m2 != null && m2.statement() != null ? m2.statement() : "Course outcome " + code);

            CourseComparisonAnalyticsResponseDto.CoMetricsDto course1Metrics = (m1 != null)
                    ? CourseComparisonAnalyticsResponseDto.CoMetricsDto.builder()
                            .directAttainment(m1.directAttainment())
                            .indirectAttainment(m1.indirectAttainment())
                            .overallAttainment(m1.overallAttainment())
                            .targetLevel(m1.targetLevel())
                            .targetMet(m1.targetMet())
                            .build()
                    : null;

            CourseComparisonAnalyticsResponseDto.CoMetricsDto course2Metrics = (m2 != null)
                    ? CourseComparisonAnalyticsResponseDto.CoMetricsDto.builder()
                            .directAttainment(m2.directAttainment())
                            .indirectAttainment(m2.indirectAttainment())
                            .overallAttainment(m2.overallAttainment())
                            .targetLevel(m2.targetLevel())
                            .targetMet(m2.targetMet())
                            .build()
                    : null;

            BigDecimal delta = (m1 != null && m1.overallAttainment() != null && m2 != null && m2.overallAttainment() != null)
                    ? m1.overallAttainment().subtract(m2.overallAttainment()).setScale(2, RoundingMode.HALF_UP)
                    : null;

            coComparisons.add(CourseComparisonAnalyticsResponseDto.CoComparisonItemDto.builder()
                    .coCode(code)
                    .statement(stmt)
                    .course1Metrics(course1Metrics)
                    .course2Metrics(course2Metrics)
                    .attainmentDelta(delta)
                    .build());
        }

        return CourseComparisonAnalyticsResponseDto.builder()
                .course1(meta1)
                .course2(meta2)
                .courseOutcomes(new ArrayList<>(allCoCodes))
                .coComparisons(coComparisons)
                .build();
    }

    // ==========================================
    // 7. ATR INTELLIGENCE ENDPOINT
    // ==========================================
    public AtrIntelligenceResponseDto getAtrIntelligence(
            String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId) {

        ResolvedScope scope = validateAndResolveScope(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        List<ProgrammeBatch> batchesInScope = getBatchesInScope(scope);
        List<String> batchIds = batchesInScope.stream().map(ProgrammeBatch::getId).toList();

        Map<String, ProgrammeBatchAttainmentReport> reportMap = batchIds.isEmpty() ? Collections.emptyMap() :
                programmeBatchAttainmentReportRepository.findByProgrammeBatchIdIn(batchIds).stream()
                        .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        List<ProgrammeAtr> atrsInScope = batchIds.isEmpty() ? Collections.emptyList() : programmeAtrRepository.findByProgrammeBatchIdIn(batchIds);
        Map<String, ProgrammeAtr> atrMap = atrsInScope.stream()
                .collect(Collectors.toMap(ProgrammeAtr::getProgrammeBatchId, a -> a, (a, b) -> a));

        Map<String, MasterProgramme> progMap = masterProgrammeRepository.findAll().stream()
                .collect(Collectors.toMap(MasterProgramme::getId, p -> p, (a, b) -> a));
        Map<String, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a));
        Map<String, ProgrammeBatch> batchMap = batchesInScope.stream()
                .collect(Collectors.toMap(ProgrammeBatch::getId, b -> b, (a, b) -> a));

        // Count status distributions
        Map<String, Integer> statusCounts = new HashMap<>();
        int approvedAtrs = 0;
        int pendingAtrs = 0;
        int needsRevisionAtrs = 0;
        int draftAtrs = 0;

        for (ProgrammeAtr atr : atrsInScope) {
            String s = atr.getStatus() != null ? atr.getStatus().name() : "DRAFT";
            statusCounts.put(s, statusCounts.getOrDefault(s, 0) + 1);
            if (atr.getStatus() == ProgrammeAtrStatus.APPROVED) {
                approvedAtrs++;
            } else if (atr.getStatus() == ProgrammeAtrStatus.SUBMITTED ||
                       atr.getStatus() == ProgrammeAtrStatus.SUBMITTED_FOR_VERIFICATION ||
                       atr.getStatus() == ProgrammeAtrStatus.PENDING_APPROVAL ||
                       atr.getStatus() == ProgrammeAtrStatus.VERIFIED) {
                pendingAtrs++;
            } else if (atr.getStatus() == ProgrammeAtrStatus.NEEDS_REVISION ||
                       atr.getStatus() == ProgrammeAtrStatus.REVISION_REQUESTED ||
                       atr.getStatus() == ProgrammeAtrStatus.REJECTED) {
                needsRevisionAtrs++;
            } else if (atr.getStatus() == ProgrammeAtrStatus.DRAFT) {
                draftAtrs++;
            }
        }

        List<AtrGapDetailDto> gapRecords = new ArrayList<>();

        for (ProgrammeBatch batch : batchesInScope) {
            ResolvedBatchAnalyticsData bData = resolveBatchData(batch, reportMap);
            if (!bData.hasActiveData) continue;

            MasterProgramme prog = progMap.get(batch.getMasterProgrammeId());
            Department dept = prog != null && prog.getDepartmentId() != null ? deptMap.get(prog.getDepartmentId()) : null;
            ProgrammeAtr atr = atrMap.get(batch.getId());

            // Process PO gaps
            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : bData.pos) {
                if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                    BigDecimal gap = po.getFinalAttainment().subtract(po.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                    if (gap.compareTo(BigDecimal.ZERO) < 0) {
                        BigDecimal pct = po.getTargetLevel().compareTo(BigDecimal.ZERO) > 0
                                ? po.getFinalAttainment().divide(po.getTargetLevel(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        List<String> actions = atr != null ? parseAtrActions(atr.getObservationsJson(), po.getPoCode(), "PO") : Collections.emptyList();

                        gapRecords.add(AtrGapDetailDto.builder()
                                .id(batch.getId() + "_" + po.getPoCode())
                                .masterProgrammeId(prog != null ? prog.getId() : "")
                                .programmeName(prog != null ? prog.getName() : "")
                                .programmeCode(prog != null ? prog.getCode() : "")
                                .programmeBatchId(batch.getId())
                                .batchName(batch.getName())
                                .departmentName(dept != null ? dept.getName() : "")
                                .outcomeCode(po.getPoCode())
                                .outcomeType("PO")
                                .outcomeStatement(po.getStatement() != null ? po.getStatement() : "Programme Outcome " + po.getPoCode())
                                .configuredTarget(po.getTargetLevel())
                                .attainedValue(po.getFinalAttainment())
                                .gap(gap)
                                .achievementPercentage(pct)
                                .hasRecordedAtr(atr != null)
                                .atrStatus(atr != null && atr.getStatus() != null ? atr.getStatus().name() : null)
                                .recordedObservations(extractObservationsText(atr))
                                .recordedActions(actions)
                                .submittedBy(atr != null ? atr.getSubmittedBy() : null)
                                .submittedAt(atr != null ? atr.getSubmittedAt() : null)
                                .verifiedBy(atr != null ? atr.getVerifiedBy() : null)
                                .verifiedAt(atr != null ? atr.getVerifiedAt() : null)
                                .approvedBy(atr != null ? atr.getApprovedBy() : null)
                                .approvedAt(atr != null ? atr.getApprovedAt() : null)
                                .verificationComments(atr != null ? atr.getVerificationComments() : null)
                                .build());
                    }
                }
            }

            // Process PSO gaps
            for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : bData.psos) {
                if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                    BigDecimal gap = pso.getFinalAttainment().subtract(pso.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                    if (gap.compareTo(BigDecimal.ZERO) < 0) {
                        BigDecimal pct = pso.getTargetLevel().compareTo(BigDecimal.ZERO) > 0
                                ? pso.getFinalAttainment().divide(pso.getTargetLevel(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        List<String> actions = atr != null ? parseAtrActions(atr.getObservationsJson(), pso.getPsoCode(), "PSO") : Collections.emptyList();

                        gapRecords.add(AtrGapDetailDto.builder()
                                .id(batch.getId() + "_" + pso.getPsoCode())
                                .masterProgrammeId(prog != null ? prog.getId() : "")
                                .programmeName(prog != null ? prog.getName() : "")
                                .programmeCode(prog != null ? prog.getCode() : "")
                                .programmeBatchId(batch.getId())
                                .batchName(batch.getName())
                                .departmentName(dept != null ? dept.getName() : "")
                                .outcomeCode(pso.getPsoCode())
                                .outcomeType("PSO")
                                .outcomeStatement(pso.getStatement() != null ? pso.getStatement() : "Programme Specific Outcome " + pso.getPsoCode())
                                .configuredTarget(pso.getTargetLevel())
                                .attainedValue(pso.getFinalAttainment())
                                .gap(gap)
                                .achievementPercentage(pct)
                                .hasRecordedAtr(atr != null)
                                .atrStatus(atr != null && atr.getStatus() != null ? atr.getStatus().name() : null)
                                .recordedObservations(extractObservationsText(atr))
                                .recordedActions(actions)
                                .submittedBy(atr != null ? atr.getSubmittedBy() : null)
                                .submittedAt(atr != null ? atr.getSubmittedAt() : null)
                                .verifiedBy(atr != null ? atr.getVerifiedBy() : null)
                                .verifiedAt(atr != null ? atr.getVerifiedAt() : null)
                                .approvedBy(atr != null ? atr.getApprovedBy() : null)
                                .approvedAt(atr != null ? atr.getApprovedAt() : null)
                                .verificationComments(atr != null ? atr.getVerificationComments() : null)
                                .build());
                    }
                }
            }
        }

        // Sort by gap ascending (most severe deficits first)
        gapRecords.sort(Comparator.comparing(AtrGapDetailDto::getGap));

        int totalGaps = gapRecords.size();
        int gapsWithAtr = (int) gapRecords.stream().filter(AtrGapDetailDto::isHasRecordedAtr).count();
        int gapsWithoutAtr = totalGaps - gapsWithAtr;

        return AtrIntelligenceResponseDto.builder()
                .totalAtrRecords(atrsInScope.size())
                .approvedAtrs(approvedAtrs)
                .pendingAtrs(pendingAtrs)
                .needsRevisionAtrs(needsRevisionAtrs)
                .draftAtrs(draftAtrs)
                .totalGapsInScope(totalGaps)
                .gapsWithAtr(gapsWithAtr)
                .gapsWithoutAtr(gapsWithoutAtr)
                .statusCounts(statusCounts)
                .gapAtrRecords(gapRecords)
                .build();
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
                // Not JSON, return empty
            }
        }
        return Collections.emptyList();
    }

    private String extractObservationsText(ProgrammeAtr atr) {
        if (atr == null) return null;
        if (atr.getVerificationComments() != null && !atr.getVerificationComments().isBlank()) {
            return atr.getVerificationComments();
        }
        return null;
    }

    // ==========================================
    // BATCH OVERVIEW ENDPOINT
    // ==========================================
    public BatchOverviewResponseDto getBatchOverview(String programmeBatchId) {
        if (programmeBatchId == null || programmeBatchId.isBlank()) {
            throw new BadRequestException("programmeBatchId is required");
        }

        ResolvedScope scope = validateAndResolveScope(null, null, null, programmeBatchId);
        ProgrammeBatch batch = programmeBatchRepository.findById(scope.programmeBatchId)
                .orElseThrow(() -> new BadRequestException("ProgrammeBatch not found: " + scope.programmeBatchId));

        // 1. Batch Context
        MasterProgramme prog = masterProgrammeRepository.findById(batch.getMasterProgrammeId()).orElse(null);
        Department dept = prog != null && prog.getDepartmentId() != null ? departmentRepository.findById(prog.getDepartmentId()).orElse(null) : null;
        School school = dept != null && dept.getSchoolId() != null ? schoolRepository.findById(dept.getSchoolId()).orElse(null) : null;

        String academicYear = (batch.getStartYear() != null && batch.getEndYear() != null)
                ? batch.getStartYear() + "-" + batch.getEndYear()
                : null;

        BatchOverviewResponseDto.BatchContextDto contextDto = BatchOverviewResponseDto.BatchContextDto.builder()
                .programmeBatchId(batch.getId())
                .batchName(batch.getName())
                .status(batch.getStatus())
                .startYear(batch.getStartYear())
                .endYear(batch.getEndYear())
                .academicYear(academicYear)
                .currentSemester(null)
                .school(school != null ? BatchOverviewResponseDto.SchoolSummary.builder().id(school.getId()).name(school.getName()).build() : null)
                .department(dept != null ? BatchOverviewResponseDto.DepartmentSummary.builder().id(dept.getId()).name(dept.getName()).build() : null)
                .programme(prog != null ? BatchOverviewResponseDto.ProgrammeSummary.builder().id(prog.getId()).code(prog.getCode()).name(prog.getName()).build() : null)
                .coordinatorName(batch.getCoordinatorName())
                .build();

        // 2. Resolve batch attainment data (PO & PSO)
        ProgrammeBatchAttainmentReport report = programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId()).orElse(null);
        ResolvedBatchAnalyticsData bData = resolveBatchData(batch, report != null ? Map.of(batch.getId(), report) : Collections.emptyMap());

        // PO Health
        List<BatchOverviewResponseDto.PoHealthDto> poHealthList = new ArrayList<>();
        if (bData.pos != null) {
            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : bData.pos) {
                if (po.getPoCode() == null) continue;
                BigDecimal target = po.getTargetLevel() != null ? po.getTargetLevel() : new BigDecimal("2.50");
                BigDecimal attainment = po.getFinalAttainment();
                BigDecimal gap = (attainment != null) ? attainment.subtract(target).setScale(2, RoundingMode.HALF_UP) : null;
                boolean targetMet = attainment != null && attainment.compareTo(target) >= 0;

                poHealthList.add(BatchOverviewResponseDto.PoHealthDto.builder()
                        .poCode(po.getPoCode().toUpperCase().trim())
                        .poStatement(po.getStatement() != null ? po.getStatement() : "Programme Outcome " + po.getPoCode())
                        .attainment(attainment)
                        .target(target)
                        .gap(gap)
                        .targetMet(targetMet)
                        .directAttainment(po.getDirectAttainment())
                        .indirectAttainment(po.getIndirectAttainment())
                        .build());
            }
        }

        // PSO Health
        List<BatchOverviewResponseDto.PsoHealthDto> psoHealthList = new ArrayList<>();
        if (bData.psos != null) {
            for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : bData.psos) {
                if (pso.getPsoCode() == null) continue;
                BigDecimal target = pso.getTargetLevel() != null ? pso.getTargetLevel() : new BigDecimal("2.50");
                BigDecimal attainment = pso.getFinalAttainment();
                BigDecimal gap = (attainment != null) ? attainment.subtract(target).setScale(2, RoundingMode.HALF_UP) : null;
                boolean targetMet = attainment != null && attainment.compareTo(target) >= 0;

                psoHealthList.add(BatchOverviewResponseDto.PsoHealthDto.builder()
                        .psoCode(pso.getPsoCode().toUpperCase().trim())
                        .psoStatement(pso.getStatement() != null ? pso.getStatement() : "Programme Specific Outcome " + pso.getPsoCode())
                        .attainment(attainment)
                        .target(target)
                        .gap(gap)
                        .targetMet(targetMet)
                        .directAttainment(pso.getDirectAttainment())
                        .indirectAttainment(pso.getIndirectAttainment())
                        .build());
            }
        }

        // 3. Summary Counts
        int poEvaluated = 0;
        int poMet = 0;
        int poBelowTarget = 0;
        for (BatchOverviewResponseDto.PoHealthDto po : poHealthList) {
            if (po.getAttainment() != null) {
                poEvaluated++;
                if (po.isTargetMet()) {
                    poMet++;
                } else {
                    poBelowTarget++;
                }
            }
        }

        int psoEvaluated = 0;
        int psoMet = 0;
        int psoBelowTarget = 0;
        for (BatchOverviewResponseDto.PsoHealthDto pso : psoHealthList) {
            if (pso.getAttainment() != null) {
                psoEvaluated++;
                if (pso.isTargetMet()) {
                    psoMet++;
                } else {
                    psoBelowTarget++;
                }
            }
        }

        BatchOverviewResponseDto.SummaryCountsDto summaryCounts = BatchOverviewResponseDto.SummaryCountsDto.builder()
                .poEvaluated(poEvaluated)
                .poMet(poMet)
                .poBelowTarget(poBelowTarget)
                .psoEvaluated(psoEvaluated)
                .psoMet(psoMet)
                .psoBelowTarget(psoBelowTarget)
                .totalDeficits(poBelowTarget + psoBelowTarget)
                .build();

        // 4. Attention Areas (limit 5, negative gap ordered, with contributing course evidence)
        List<AttentionAreaItemDto> attentionAreas = getAttentionAreas(null, null, null, batch.getId(), 5, "ALL");

        // 5. Direct vs Indirect picture
        BigDecimal directSum = BigDecimal.ZERO;
        int directCount = 0;
        BigDecimal indirectSum = BigDecimal.ZERO;
        int indirectCount = 0;

        for (BatchOverviewResponseDto.PoHealthDto po : poHealthList) {
            if (po.getDirectAttainment() != null) {
                directSum = directSum.add(po.getDirectAttainment());
                directCount++;
            }
            if (po.getIndirectAttainment() != null) {
                indirectSum = indirectSum.add(po.getIndirectAttainment());
                indirectCount++;
            }
        }
        for (BatchOverviewResponseDto.PsoHealthDto pso : psoHealthList) {
            if (pso.getDirectAttainment() != null) {
                directSum = directSum.add(pso.getDirectAttainment());
                directCount++;
            }
            if (pso.getIndirectAttainment() != null) {
                indirectSum = indirectSum.add(pso.getIndirectAttainment());
                indirectCount++;
            }
        }

        BigDecimal programmeDirect = directCount > 0 ? directSum.divide(BigDecimal.valueOf(directCount), 2, RoundingMode.HALF_UP) : null;
        BigDecimal programmeIndirect = indirectCount > 0 ? indirectSum.divide(BigDecimal.valueOf(indirectCount), 2, RoundingMode.HALF_UP) : null;

        BatchOverviewResponseDto.DirectIndirectDto directIndirect = BatchOverviewResponseDto.DirectIndirectDto.builder()
                .programmeDirectWeight(new BigDecimal("0.80"))
                .programmeIndirectWeight(new BigDecimal("0.20"))
                .programmeDirect(programmeDirect)
                .programmeIndirect(programmeIndirect)
                .build();

        // 6. Course Contributions
        List<ProgrammeBatchCourse> courses = programmeBatchCourseRepository.findByProgrammeBatchId(batch.getId());
        courses.sort(Comparator.comparing((ProgrammeBatchCourse c) -> c.getSemester() != null ? c.getSemester() : 1)
                .thenComparing(c -> c.getCourseCode() != null ? c.getCourseCode() : ""));

        List<String> offeringIds = courses.stream().map(ProgrammeBatchCourse::getId).toList();
        Map<String, CourseAttainmentReport> courseReportMap = offeringIds.isEmpty() ? Collections.emptyMap() :
                courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offeringIds).stream()
                        .collect(Collectors.toMap(CourseAttainmentReport::getProgrammeBatchCourseId, r -> r, (a, b) -> a));

        // Get course mapping and direct rows from finalized report or continuous calculation
        Map<String, ProgrammeBatchAttainmentReportDto.CourseContributionRow> mappingRowMap = new HashMap<>();
        Map<String, ProgrammeBatchAttainmentReportDto.CourseContributionRow> directRowMap = new HashMap<>();

        boolean isFinalized = report != null && (report.getStatus() == ReportStatus.FINALIZED || report.getStatus() == ReportStatus.APPROVED);
        if (isFinalized) {
            try {
                if (report.getAverageMappingReportJson() != null && !report.getAverageMappingReportJson().isBlank()) {
                    Map<String, Object> r1Map = objectMapper.readValue(report.getAverageMappingReportJson(), new TypeReference<Map<String, Object>>() {});
                    if (r1Map.containsKey("courses")) {
                        List<ProgrammeBatchAttainmentReportDto.CourseContributionRow> cRows = objectMapper.convertValue(
                                r1Map.get("courses"), new TypeReference<List<ProgrammeBatchAttainmentReportDto.CourseContributionRow>>() {});
                        for (ProgrammeBatchAttainmentReportDto.CourseContributionRow row : cRows) {
                            if (row.getProgrammeBatchCourseId() != null) mappingRowMap.put(row.getProgrammeBatchCourseId(), row);
                        }
                    }
                }
                if (report.getDirectAttainmentReportJson() != null && !report.getDirectAttainmentReportJson().isBlank()) {
                    Map<String, Object> r2Map = objectMapper.readValue(report.getDirectAttainmentReportJson(), new TypeReference<Map<String, Object>>() {});
                    if (r2Map.containsKey("courses")) {
                        List<ProgrammeBatchAttainmentReportDto.CourseContributionRow> cRows = objectMapper.convertValue(
                                r2Map.get("courses"), new TypeReference<List<ProgrammeBatchAttainmentReportDto.CourseContributionRow>>() {});
                        for (ProgrammeBatchAttainmentReportDto.CourseContributionRow row : cRows) {
                            if (row.getProgrammeBatchCourseId() != null) directRowMap.put(row.getProgrammeBatchCourseId(), row);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[AnalyticsService] Error parsing finalized course contribution rows for batch {}: {}", batch.getId(), e.getMessage());
            }
        } else if (!courses.isEmpty() && bData.totalEvaluatedCourses > 0) {
            try {
                ProgrammeAttainmentResultDto calcResult = attainmentCalculationService.calculateProgrammeAttainment(batch.getMasterProgrammeId(), batch.getId());
                if (calcResult != null) {
                    if (calcResult.getCourseMappingRows() != null) {
                        for (ProgrammeAttainmentResultDto.CourseContributionRow row : calcResult.getCourseMappingRows()) {
                            if (row.getProgrammeBatchCourseId() != null) {
                                mappingRowMap.put(row.getProgrammeBatchCourseId(), ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                                        .programmeBatchCourseId(row.getProgrammeBatchCourseId())
                                        .masterCourseId(row.getMasterCourseId())
                                        .semester(row.getSemester())
                                        .courseCode(row.getCourseCode())
                                        .courseName(row.getCourseName())
                                        .resourceName(row.getResourceName())
                                        .poValues(row.getPoValues())
                                        .psoValues(row.getPsoValues())
                                        .build());
                            }
                        }
                    }
                    if (calcResult.getCourseDirectAttainmentRows() != null) {
                        for (ProgrammeAttainmentResultDto.CourseContributionRow row : calcResult.getCourseDirectAttainmentRows()) {
                            if (row.getProgrammeBatchCourseId() != null) {
                                directRowMap.put(row.getProgrammeBatchCourseId(), ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                                        .programmeBatchCourseId(row.getProgrammeBatchCourseId())
                                        .masterCourseId(row.getMasterCourseId())
                                        .semester(row.getSemester())
                                        .courseCode(row.getCourseCode())
                                        .courseName(row.getCourseName())
                                        .resourceName(row.getResourceName())
                                        .poValues(row.getPoValues())
                                        .psoValues(row.getPsoValues())
                                        .build());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("[AnalyticsService] Error calculating live course contributions for batch {}: {}", batch.getId(), e.getMessage());
            }
        }

        List<BatchOverviewResponseDto.CourseContributionItemDto> courseContributions = new ArrayList<>();
        for (ProgrammeBatchCourse c : courses) {
            CourseAttainmentReport cr = courseReportMap.get(c.getId());
            ProgrammeBatchAttainmentReportDto.CourseContributionRow mRow = mappingRowMap.get(c.getId());
            ProgrammeBatchAttainmentReportDto.CourseContributionRow dRow = directRowMap.get(c.getId());

            String coordinator = c.getCourseCoordinatorName() != null && !c.getCourseCoordinatorName().isBlank()
                    ? c.getCourseCoordinatorName()
                    : (c.getAssignedFaculty() != null ? c.getAssignedFaculty() : "");

            courseContributions.add(BatchOverviewResponseDto.CourseContributionItemDto.builder()
                    .programmeBatchCourseId(c.getId())
                    .courseCode(c.getCourseCode() != null ? c.getCourseCode() : c.getCode())
                    .courseName(c.getCourseName() != null ? c.getCourseName() : c.getName())
                    .semester(c.getSemester())
                    .courseCoordinator(coordinator)
                    .overallCourseAttainment(cr != null ? cr.getOverallCoAttainment() : null)
                    .poContributions(dRow != null && dRow.getPoValues() != null ? dRow.getPoValues() : Collections.emptyMap())
                    .psoContributions(dRow != null && dRow.getPsoValues() != null ? dRow.getPsoValues() : Collections.emptyMap())
                    .poMappingStrength(mRow != null && mRow.getPoValues() != null ? mRow.getPoValues() : Collections.emptyMap())
                    .psoMappingStrength(mRow != null && mRow.getPsoValues() != null ? mRow.getPsoValues() : Collections.emptyMap())
                    .build());
        }

        // 7. Programme Indirect Indication
        ProgrammeSurveyResultDto exitSurvey = null;
        try {
            exitSurvey = attainmentCalculationService.getProgrammeSurveyResult(batch.getMasterProgrammeId(), batch.getId());
        } catch (Exception e) {
            log.debug("[AnalyticsService] No exit survey found for batch {}: {}", batch.getId(), e.getMessage());
        }

        Map<String, BigDecimal> exitScores = new LinkedHashMap<>();
        boolean hasExitSurvey = false;
        if (exitSurvey != null && exitSurvey.getRecordsProcessed() > 0) {
            hasExitSurvey = true;
            if (exitSurvey.getPoIndirectAttainment() != null) {
                for (ProgrammeSurveyResultDto.PoIndirectItem it : exitSurvey.getPoIndirectAttainment()) {
                    if (it.getPoCode() != null && it.getIndirectAttainment() != null && it.getIndirectAttainment().compareTo(BigDecimal.ZERO) > 0) {
                        exitScores.put(it.getPoCode().toUpperCase().trim(), it.getIndirectAttainment());
                    }
                }
            }
            if (exitSurvey.getPsoIndirectAttainment() != null) {
                for (ProgrammeSurveyResultDto.PsoIndirectItem it : exitSurvey.getPsoIndirectAttainment()) {
                    if (it.getPsoCode() != null && it.getIndirectAttainment() != null && it.getIndirectAttainment().compareTo(BigDecimal.ZERO) > 0) {
                        exitScores.put(it.getPsoCode().toUpperCase().trim(), it.getIndirectAttainment());
                    }
                }
            }
        }

        ConsolidatedIndirectAttainmentDto consolidatedDto = null;
        try {
            consolidatedDto = indirectAssessmentService.getConsolidatedIndirectAttainment(batch.getId(), exitScores);
        } catch (Exception e) {
            log.debug("[AnalyticsService] Error computing consolidated indirect attainment for batch {}: {}", batch.getId(), e.getMessage());
        }

        BatchOverviewResponseDto.ProgrammeIndirectSummaryDto indirectSummary = BatchOverviewResponseDto.ProgrammeIndirectSummaryDto.builder()
                .assessmentCount(consolidatedDto != null && consolidatedDto.getAssessments() != null ? consolidatedDto.getAssessments().size() : 0)
                .hasExitSurvey(hasExitSurvey)
                .exitSurveyScores(exitScores)
                .consolidatedIndirectAttainment(consolidatedDto != null ? consolidatedDto.getConsolidatedIndirectAttainment() : Collections.emptyMap())
                .evaluationCounts(consolidatedDto != null ? consolidatedDto.getEvaluationCounts() : Collections.emptyMap())
                .build();

        // 8. Programme ATR Indication
        Optional<ProgrammeAtr> optAtr = programmeAtrRepository.findByProgrammeBatchId(batch.getId());
        BatchOverviewResponseDto.ProgrammeAtrSummaryDto programmeAtrSummary;
        if (optAtr.isPresent()) {
            ProgrammeAtr atr = optAtr.get();
            ProgrammeAtrStatus status = atr.getStatus();
            boolean revisionRequired = status == ProgrammeAtrStatus.NEEDS_REVISION || status == ProgrammeAtrStatus.REVISION_REQUESTED;
            programmeAtrSummary = BatchOverviewResponseDto.ProgrammeAtrSummaryDto.builder()
                    .exists(true)
                    .status(status != null ? status.name() : null)
                    .revisionRequired(revisionRequired)
                    .verificationComments(atr.getVerificationComments())
                    .observations(atr.getObservationsJson())
                    .build();
        } else {
            programmeAtrSummary = BatchOverviewResponseDto.ProgrammeAtrSummaryDto.builder()
                    .exists(false)
                    .status(null)
                    .revisionRequired(false)
                    .verificationComments(null)
                    .observations(null)
                    .build();
        }

        // 9. Course ATR Batch-level Summary
        List<CourseAtr> courseAtrs = offeringIds.isEmpty() ? Collections.emptyList() :
                courseAtrRepository.findByProgrammeBatchCourseIdIn(offeringIds);

        int totalCoursesCount = courses.size();
        int totalRecords = courseAtrs.size();
        Set<String> coursesWithAtrSet = courseAtrs.stream().map(CourseAtr::getProgrammeBatchCourseId).collect(Collectors.toSet());
        int coursesWithAtr = coursesWithAtrSet.size();

        int draftCount = 0;
        int submittedCount = 0;
        int submittedForVerificationCount = 0;
        int pendingApprovalCount = 0;
        int verifiedCount = 0;
        int approvedCount = 0;
        int needsRevisionCount = 0;
        int revisionRequestedCount = 0;
        int rejectedCount = 0;

        for (CourseAtr ca : courseAtrs) {
            if (ca.getStatus() == null) continue;
            switch (ca.getStatus()) {
                case DRAFT -> draftCount++;
                case SUBMITTED -> submittedCount++;
                case SUBMITTED_FOR_VERIFICATION -> submittedForVerificationCount++;
                case PENDING_APPROVAL -> pendingApprovalCount++;
                case VERIFIED -> verifiedCount++;
                case APPROVED -> approvedCount++;
                case NEEDS_REVISION -> needsRevisionCount++;
                case REVISION_REQUESTED -> revisionRequestedCount++;
                case REJECTED -> rejectedCount++;
            }
        }
        int revisionRequiredCount = needsRevisionCount + revisionRequestedCount;

        BatchOverviewResponseDto.CourseAtrSummaryDto courseAtrSummary = BatchOverviewResponseDto.CourseAtrSummaryDto.builder()
                .totalCourses(totalCoursesCount)
                .coursesWithAtr(coursesWithAtr)
                .totalRecords(totalRecords)
                .draftCount(draftCount)
                .submittedCount(submittedCount)
                .submittedForVerificationCount(submittedForVerificationCount)
                .pendingApprovalCount(pendingApprovalCount)
                .verifiedCount(verifiedCount)
                .approvedCount(approvedCount)
                .needsRevisionCount(needsRevisionCount)
                .revisionRequestedCount(revisionRequestedCount)
                .rejectedCount(rejectedCount)
                .revisionRequiredCount(revisionRequiredCount)
                .build();

        // 10. Historical Navigation Context
        int batchCount = batch.getMasterProgrammeId() != null
                ? programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(batch.getMasterProgrammeId()).size()
                : 0;

        BatchOverviewResponseDto.HistoricalNavigationDto historicalSummary = BatchOverviewResponseDto.HistoricalNavigationDto.builder()
                .masterProgrammeId(batch.getMasterProgrammeId())
                .available(batch.getMasterProgrammeId() != null && !batch.getMasterProgrammeId().isBlank())
                .batchCount(batchCount)
                .build();

        return BatchOverviewResponseDto.builder()
                .batch(contextDto)
                .poHealth(poHealthList)
                .psoHealth(psoHealthList)
                .summary(summaryCounts)
                .attentionAreas(attentionAreas)
                .directIndirect(directIndirect)
                .courseContributions(courseContributions)
                .programmeIndirect(indirectSummary)
                .programmeAtr(programmeAtrSummary)
                .courseAtr(courseAtrSummary)
                .historical(historicalSummary)
                .build();
    }

    // ==========================================
    // HELPER / SCOPE / PARSER METHODS
    // ==========================================
    private ResolvedScope validateAndResolveScope(String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId) {
        String reqSchoolId = schoolId != null && !schoolId.isBlank() ? schoolId.trim() : null;
        String reqDepartmentId = departmentId != null && !departmentId.isBlank() ? departmentId.trim() : null;
        String reqMasterProgrammeId = masterProgrammeId != null && !masterProgrammeId.isBlank() ? masterProgrammeId.trim() : null;
        String reqProgrammeBatchId = programmeBatchId != null && !programmeBatchId.isBlank() ? programmeBatchId.trim() : null;

        // Hierarchy validation and upward inference
        if (reqProgrammeBatchId != null) {
            ProgrammeBatch batch = programmeBatchRepository.findById(reqProgrammeBatchId)
                    .orElseThrow(() -> new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: ProgrammeBatch not found: " + reqProgrammeBatchId));
            if (reqMasterProgrammeId != null && !reqMasterProgrammeId.equalsIgnoreCase(batch.getMasterProgrammeId())) {
                throw new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: ProgrammeBatch '" + reqProgrammeBatchId + "' does not belong to masterProgramme '" + reqMasterProgrammeId + "'");
            }
            if (reqMasterProgrammeId == null) {
                reqMasterProgrammeId = batch.getMasterProgrammeId();
            }
        }

        if (reqMasterProgrammeId != null) {
            final String progId = reqMasterProgrammeId;
            MasterProgramme prog = masterProgrammeRepository.findById(progId)
                    .orElseThrow(() -> new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: MasterProgramme not found: " + progId));
            if (reqDepartmentId != null && !reqDepartmentId.equalsIgnoreCase(prog.getDepartmentId())) {
                throw new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: MasterProgramme '" + progId + "' does not belong to department '" + reqDepartmentId + "'");
            }
            if (reqDepartmentId == null) {
                reqDepartmentId = prog.getDepartmentId();
            }
        }

        if (reqDepartmentId != null) {
            final String deptId = reqDepartmentId;
            Department dept = departmentRepository.findById(deptId)
                    .orElseThrow(() -> new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: Department not found: " + deptId));
            if (reqSchoolId != null && !reqSchoolId.equalsIgnoreCase(dept.getSchoolId())) {
                throw new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: Department '" + deptId + "' does not belong to school '" + reqSchoolId + "'");
            }
            if (reqSchoolId == null) {
                reqSchoolId = dept.getSchoolId();
            }
        }

        if (reqSchoolId != null) {
            if (!schoolRepository.existsById(reqSchoolId)) {
                throw new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: School not found: " + reqSchoolId);
            }
        }

        String effectiveSchoolId = reqSchoolId;
        String effectiveDepartmentId = reqDepartmentId;
        String effectiveMasterProgrammeId = reqMasterProgrammeId;
        String effectiveProgrammeBatchId = reqProgrammeBatchId;

        // Academic Scope Authorization Enforcement
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope();
        if (userScope != null) {
            if (userScope.isDirector()) {
                String allowedSchool = userScope.getRequiredSchoolId();
                if (effectiveSchoolId != null && !effectiveSchoolId.equalsIgnoreCase(allowedSchool)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Unauthorized school scope");
                }
                effectiveSchoolId = allowedSchool;
            } else if (userScope.isHod()) {
                String allowedDept = userScope.getRequiredDepartmentId();
                if (effectiveDepartmentId != null && !effectiveDepartmentId.equalsIgnoreCase(allowedDept)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Unauthorized department scope");
                }
                effectiveDepartmentId = allowedDept;
                if (userScope.hasSchoolScope()) {
                    effectiveSchoolId = userScope.getSchoolId();
                }
            } else if (userScope.isProgrammeCoordinator()) {
                String allowedProg = userScope.getRequiredMasterProgrammeId();
                if (effectiveMasterProgrammeId != null && !effectiveMasterProgrammeId.equalsIgnoreCase(allowedProg)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Unauthorized programme scope");
                }
                effectiveMasterProgrammeId = allowedProg;
                if (userScope.hasDepartmentScope()) {
                    effectiveDepartmentId = userScope.getDepartmentId();
                }
                if (userScope.hasSchoolScope()) {
                    effectiveSchoolId = userScope.getSchoolId();
                }
            }
        }

        return new ResolvedScope(effectiveSchoolId, effectiveDepartmentId, effectiveMasterProgrammeId, effectiveProgrammeBatchId);
    }

    private List<ProgrammeBatch> getBatchesInScope(ResolvedScope scope) {
        if (scope.programmeBatchId != null) {
            return programmeBatchRepository.findById(scope.programmeBatchId).map(List::of).orElse(Collections.emptyList());
        }
        if (scope.masterProgrammeId != null) {
            return programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(scope.masterProgrammeId);
        }
        if (scope.departmentId != null) {
            List<String> progIds = masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(scope.departmentId).stream()
                    .map(MasterProgramme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : programmeBatchRepository.findByMasterProgrammeIdInAndDeletedAtIsNull(progIds);
        }
        if (scope.schoolId != null) {
            List<String> deptIds = departmentRepository.findBySchoolId(scope.schoolId).stream().map(Department::getId).toList();
            List<String> progIds = deptIds.isEmpty() ? Collections.emptyList() :
                    masterProgrammeRepository.findByDepartmentIdInAndDeletedAtIsNull(deptIds).stream().map(MasterProgramme::getId).toList();
            return progIds.isEmpty() ? Collections.emptyList() : programmeBatchRepository.findByMasterProgrammeIdInAndDeletedAtIsNull(progIds);
        }
        return programmeBatchRepository.findByDeletedAtIsNull();
    }

    private List<ProgrammeBatchAttainmentReport> getFinalizedReports(List<String> batchIds) {
        if (batchIds.isEmpty()) return Collections.emptyList();
        return programmeBatchAttainmentReportRepository.findByProgrammeBatchIdIn(batchIds).stream()
                .filter(r -> r.getStatus() == ReportStatus.FINALIZED || r.getStatus() == ReportStatus.APPROVED)
                .collect(Collectors.toList());
    }

    private ResolvedBatchAnalyticsData resolveBatchData(ProgrammeBatch batch, Map<String, ProgrammeBatchAttainmentReport> reportMap) {
        if (batch == null) {
            return new ResolvedBatchAnalyticsData(null, null, "", null, null, false, null, Collections.emptyList(), Collections.emptyList(), 0, 0, false);
        }

        ProgrammeBatchAttainmentReport report = reportMap != null ? reportMap.get(batch.getId()) :
                programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId()).orElse(null);

        boolean isFinalized = report != null && (report.getStatus() == ReportStatus.FINALIZED || report.getStatus() == ReportStatus.APPROVED);

        List<ProgrammeBatchCourse> courses = programmeBatchCourseRepository.findByProgrammeBatchId(batch.getId());
        int totalCourses = courses.size();

        if (isFinalized) {
            ParsedReport parsed = parseReport(report);
            boolean hasData = !parsed.pos.isEmpty() || !parsed.psos.isEmpty();
            List<String> offeringIds = courses.stream().map(ProgrammeBatchCourse::getId).toList();
            int evaluatedCourses = 0;
            if (!offeringIds.isEmpty()) {
                Set<String> evaluatedSet = new HashSet<>();
                courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offeringIds)
                        .forEach(r -> evaluatedSet.add(r.getProgrammeBatchCourseId()));
                studentCoMarkRepository.findByProgrammeBatchCourseIdIn(offeringIds)
                        .forEach(m -> evaluatedSet.add(m.getProgrammeBatchCourseId()));
                evaluatedCourses = evaluatedSet.size();
                if (evaluatedCourses == 0 && hasData) {
                    evaluatedCourses = totalCourses;
                }
            }
            return new ResolvedBatchAnalyticsData(
                    batch.getId(),
                    batch.getMasterProgrammeId(),
                    batch.getName(),
                    batch.getStartYear(),
                    batch.getEndYear(),
                    true,
                    report.getStatus(),
                    parsed.pos,
                    parsed.psos,
                    evaluatedCourses,
                    totalCourses,
                    hasData
            );
        }

        // In-Progress Continuous Monitoring
        if (courses.isEmpty()) {
            return new ResolvedBatchAnalyticsData(
                    batch.getId(),
                    batch.getMasterProgrammeId(),
                    batch.getName(),
                    batch.getStartYear(),
                    batch.getEndYear(),
                    false,
                    report != null ? report.getStatus() : null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
                    0,
                    false
            );
        }

        List<String> offeringIds = courses.stream().map(ProgrammeBatchCourse::getId).toList();
        Set<String> evaluatedOfferingIds = new HashSet<>();
        courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(offeringIds)
                .forEach(r -> evaluatedOfferingIds.add(r.getProgrammeBatchCourseId()));
        studentCoMarkRepository.findByProgrammeBatchCourseIdIn(offeringIds)
                .forEach(m -> evaluatedOfferingIds.add(m.getProgrammeBatchCourseId()));

        int totalEvaluatedCourses = evaluatedOfferingIds.size();

        // If no course has marks or reports, batch is genuinely not evaluated
        if (totalEvaluatedCourses == 0) {
            return new ResolvedBatchAnalyticsData(
                    batch.getId(),
                    batch.getMasterProgrammeId(),
                    batch.getName(),
                    batch.getStartYear(),
                    batch.getEndYear(),
                    false,
                    report != null ? report.getStatus() : null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
                    totalCourses,
                    false
            );
        }

        // Calculate live continuous attainment using authoritative calculation service
        try {
            ProgrammeAttainmentResultDto calcResult = attainmentCalculationService.calculateProgrammeAttainment(batch.getMasterProgrammeId(), batch.getId());
            List<ProgrammeBatchAttainmentReportDto.Report4PoRow> poRows = new ArrayList<>();
            List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psoRows = new ArrayList<>();

            if (calcResult != null && calcResult.getOverallAttainment() != null) {
                if (calcResult.getOverallAttainment().getPos() != null) {
                    for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : calcResult.getOverallAttainment().getPos()) {
                        String code = it.getPoCode() != null ? it.getPoCode() : it.getOutcomeCode();
                        if (code == null) continue;
                        poRows.add(ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                                .poCode(code)
                                .statement(it.getOutcomeStatement())
                                .targetLevel(it.getTarget() != null ? it.getTarget() : new BigDecimal("2.50"))
                                .directAttainment(it.getDirectAttainment() != null ? it.getDirectAttainment() : BigDecimal.ZERO)
                                .indirectAttainment(it.getIndirectAttainment() != null ? it.getIndirectAttainment() : BigDecimal.ZERO)
                                .finalAttainment(it.getOverallAttainment() != null ? it.getOverallAttainment() : BigDecimal.ZERO)
                                .observation(it.getObservation())
                                .build());
                    }
                }

                if (calcResult.getOverallAttainment().getPsos() != null) {
                    for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : calcResult.getOverallAttainment().getPsos()) {
                        String code = it.getPsoCode() != null ? it.getPsoCode() : it.getOutcomeCode();
                        if (code == null) continue;
                        psoRows.add(ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder()
                                .psoCode(code)
                                .statement(it.getOutcomeStatement())
                                .targetLevel(it.getTarget() != null ? it.getTarget() : new BigDecimal("2.50"))
                                .directAttainment(it.getDirectAttainment() != null ? it.getDirectAttainment() : BigDecimal.ZERO)
                                .indirectAttainment(it.getIndirectAttainment() != null ? it.getIndirectAttainment() : BigDecimal.ZERO)
                                .finalAttainment(it.getOverallAttainment() != null ? it.getOverallAttainment() : BigDecimal.ZERO)
                                .observation(it.getObservation())
                                .build());
                    }
                }
            }

            boolean hasData = !poRows.isEmpty() || !psoRows.isEmpty();

            return new ResolvedBatchAnalyticsData(
                    batch.getId(),
                    batch.getMasterProgrammeId(),
                    batch.getName(),
                    batch.getStartYear(),
                    batch.getEndYear(),
                    false,
                    report != null ? report.getStatus() : null,
                    poRows,
                    psoRows,
                    totalEvaluatedCourses,
                    totalCourses,
                    hasData
            );
        } catch (Exception e) {
            log.warn("[AnalyticsService] Error calculating live continuous attainment for batch {}: {}", batch.getId(), e.getMessage());
            return new ResolvedBatchAnalyticsData(
                    batch.getId(),
                    batch.getMasterProgrammeId(),
                    batch.getName(),
                    batch.getStartYear(),
                    batch.getEndYear(),
                    false,
                    report != null ? report.getStatus() : null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    totalEvaluatedCourses,
                    totalCourses,
                    false
            );
        }
    }

    private ParsedReport parseReport(ProgrammeBatchAttainmentReport report) {
        if (report == null || report.getOverallAttainmentReportJson() == null || report.getOverallAttainmentReportJson().isBlank()) {
            return new ParsedReport(Collections.emptyList(), Collections.emptyList());
        }
        try {
            Map<String, Object> map = objectMapper.readValue(report.getOverallAttainmentReportJson(), new TypeReference<Map<String, Object>>() {});
            List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos = Collections.emptyList();
            List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psos = Collections.emptyList();

            if (map.containsKey("po")) {
                pos = objectMapper.convertValue(map.get("po"), new TypeReference<List<ProgrammeBatchAttainmentReportDto.Report4PoRow>>() {});
            }
            if (map.containsKey("pso")) {
                psos = objectMapper.convertValue(map.get("pso"), new TypeReference<List<ProgrammeBatchAttainmentReportDto.Report4PsoRow>>() {});
            }
            return new ParsedReport(pos != null ? pos : Collections.emptyList(), psos != null ? psos : Collections.emptyList());
        } catch (Exception e) {
            log.warn("[AnalyticsService] Error parsing overallAttainmentReportJson for batch {}: {}", report.getProgrammeBatchId(), e.getMessage());
            return new ParsedReport(Collections.emptyList(), Collections.emptyList());
        }
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

    private record ResolvedScope(String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId) {}
    private record ParsedReport(List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos, List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psos) {}
    private record PoInstanceData(String batchId, BigDecimal attainment, BigDecimal target, BigDecimal direct, BigDecimal indirect) {}
    private record ResolvedBatchAnalyticsData(
            String batchId,
            String masterProgrammeId,
            String batchName,
            Integer startYear,
            Integer endYear,
            boolean isFinalized,
            ReportStatus reportStatus,
            List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos,
            List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psos,
            int totalEvaluatedCourses,
            int totalCoursesInBatch,
            boolean hasActiveData
    ) {}

    private Map<String, OutcomeItemMetrics> resolveBatchOutcomeMetrics(
            ProgrammeBatch batch,
            Map<String, ProgrammeBatchAttainmentReport> reportMap) {
        Map<String, OutcomeItemMetrics> map = new LinkedHashMap<>();
        if (batch == null) return map;

        ProgrammeBatchAttainmentReport report = (reportMap != null) ? reportMap.get(batch.getId()) :
                programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId()).orElse(null);

        boolean isFinalized = report != null &&
                (report.getStatus() == ReportStatus.FINALIZED || report.getStatus() == ReportStatus.APPROVED);

        if (isFinalized) {
            ParsedReport parsed = parseReport(report);
            if (!parsed.pos().isEmpty() || !parsed.psos().isEmpty()) {
                for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : parsed.pos()) {
                    if (po.getPoCode() == null) continue;
                    String code = po.getPoCode().trim().toUpperCase();
                    BigDecimal target = (po.getTargetLevel() != null) ? po.getTargetLevel().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                    BigDecimal direct = (po.getDirectAttainment() != null) ? po.getDirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                    BigDecimal indirect = (po.getIndirectAttainment() != null) ? po.getIndirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                    BigDecimal finalAtt = (po.getFinalAttainment() != null) ? po.getFinalAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                    BigDecimal gap = finalAtt.subtract(target).setScale(2, RoundingMode.HALF_UP);
                    boolean met = finalAtt.compareTo(target) >= 0;
                    map.put(code, new OutcomeItemMetrics(code, "PO", direct, indirect, finalAtt, target, gap, met));
                }
                for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : parsed.psos()) {
                    if (pso.getPsoCode() == null) continue;
                    String code = pso.getPsoCode().trim().toUpperCase();
                    BigDecimal target = (pso.getTargetLevel() != null) ? pso.getTargetLevel().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                    BigDecimal direct = (pso.getDirectAttainment() != null) ? pso.getDirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                    BigDecimal indirect = (pso.getIndirectAttainment() != null) ? pso.getIndirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                    BigDecimal finalAtt = (pso.getFinalAttainment() != null) ? pso.getFinalAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                    BigDecimal gap = finalAtt.subtract(target).setScale(2, RoundingMode.HALF_UP);
                    boolean met = finalAtt.compareTo(target) >= 0;
                    map.put(code, new OutcomeItemMetrics(code, "PSO", direct, indirect, finalAtt, target, gap, met));
                }
                return map;
            }
        }

        // Active / non-finalized batch: calculate live continuous attainment using authoritative service
        try {
            ProgrammeAttainmentResultDto calcResult = attainmentCalculationService
                    .calculateProgrammeAttainment(batch.getMasterProgrammeId(), batch.getId());
            if (calcResult != null && calcResult.getOverallAttainment() != null) {
                if (calcResult.getOverallAttainment().getPos() != null) {
                    for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : calcResult.getOverallAttainment().getPos()) {
                        String rawCode = it.getPoCode() != null ? it.getPoCode() : it.getOutcomeCode();
                        if (rawCode == null) continue;
                        String code = rawCode.trim().toUpperCase();
                        BigDecimal target = (it.getTarget() != null) ? it.getTarget().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                        BigDecimal direct = (it.getDirectAttainment() != null) ? it.getDirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                        BigDecimal indirect = (it.getIndirectAttainment() != null) ? it.getIndirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                        BigDecimal finalAtt = (it.getOverallAttainment() != null) ? it.getOverallAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                        BigDecimal gap = finalAtt.subtract(target).setScale(2, RoundingMode.HALF_UP);
                        boolean met = finalAtt.compareTo(target) >= 0;
                        map.put(code, new OutcomeItemMetrics(code, "PO", direct, indirect, finalAtt, target, gap, met));
                    }
                }
                if (calcResult.getOverallAttainment().getPsos() != null) {
                    for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : calcResult.getOverallAttainment().getPsos()) {
                        String rawCode = it.getPsoCode() != null ? it.getPsoCode() : it.getOutcomeCode();
                        if (rawCode == null) continue;
                        String code = rawCode.trim().toUpperCase();
                        BigDecimal target = (it.getTarget() != null) ? it.getTarget().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                        BigDecimal direct = (it.getDirectAttainment() != null) ? it.getDirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                        BigDecimal indirect = (it.getIndirectAttainment() != null) ? it.getIndirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                        BigDecimal finalAtt = (it.getOverallAttainment() != null) ? it.getOverallAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                        BigDecimal gap = finalAtt.subtract(target).setScale(2, RoundingMode.HALF_UP);
                        boolean met = finalAtt.compareTo(target) >= 0;
                        map.put(code, new OutcomeItemMetrics(code, "PSO", direct, indirect, finalAtt, target, gap, met));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[AnalyticsService] Error calculating live continuous attainment for batch {}: {}", batch.getId(), e.getMessage());
        }

        // Fallback if still empty: load configured outcomes for batch
        if (map.isEmpty()) {
            List<ProgrammeOutcome> pos = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getId());
            if (pos == null || pos.isEmpty()) {
                pos = programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getMasterProgrammeId());
            }
            if (pos != null) {
                for (ProgrammeOutcome po : pos) {
                    if (po.getCode() == null) continue;
                    String code = po.getCode().trim().toUpperCase();
                    BigDecimal target = (po.getTarget() != null) ? po.getTarget().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                    BigDecimal zero = BigDecimal.ZERO.setScale(2);
                    map.put(code, new OutcomeItemMetrics(code, "PO", zero, zero, zero, target, zero.subtract(target).setScale(2, RoundingMode.HALF_UP), false));
                }
            }
            List<ProgrammeSpecificOutcome> psos = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getId());
            if (psos == null || psos.isEmpty()) {
                psos = programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(batch.getMasterProgrammeId());
            }
            if (psos != null) {
                for (ProgrammeSpecificOutcome pso : psos) {
                    if (pso.getCode() == null) continue;
                    String code = pso.getCode().trim().toUpperCase();
                    BigDecimal target = (pso.getTarget() != null) ? pso.getTarget().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                    BigDecimal zero = BigDecimal.ZERO.setScale(2);
                    map.put(code, new OutcomeItemMetrics(code, "PSO", zero, zero, zero, target, zero.subtract(target).setScale(2, RoundingMode.HALF_UP), false));
                }
            }
        }

        return map;
    }

    private int compareOutcomeCodes(String code1, String code2) {
        if (code1 == null && code2 == null) return 0;
        if (code1 == null) return -1;
        if (code2 == null) return 1;
        String c1 = code1.trim().toUpperCase();
        String c2 = code2.trim().toUpperCase();
        boolean isPo1 = c1.startsWith("PO") && !c1.startsWith("PSO");
        boolean isPo2 = c2.startsWith("PO") && !c2.startsWith("PSO");
        if (isPo1 && !isPo2) return -1;
        if (!isPo1 && isPo2) return 1;
        int num1 = extractOutcomeDigits(c1);
        int num2 = extractOutcomeDigits(c2);
        if (num1 != num2) {
            return Integer.compare(num1, num2);
        }
        return c1.compareTo(c2);
    }

    private int extractOutcomeDigits(String s) {
        try {
            String digits = s.replaceAll("\\D+", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    private record OutcomeItemMetrics(
            String outcomeCode,
            String outcomeType,
            BigDecimal directAttainment,
            BigDecimal indirectAttainment,
            BigDecimal finalAttainment,
            BigDecimal targetLevel,
            BigDecimal gap,
            Boolean targetMet
    ) {}

    private void enforceCourseOfferingAccess(ProgrammeBatchCourse offering, ProgrammeBatch batch) {
        CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope();
        if (userScope == null || userScope.isIqac()) {
            return;
        }
        if (userScope.isFaculty()) {
            boolean isCoord = false;
            if (offering.getCourseCoordinatorId() != null && userScope.getUserId() != null) {
                isCoord = String.valueOf(offering.getCourseCoordinatorId()).equals(String.valueOf(userScope.getUserId()));
            }
            if (!isCoord && offering.getCourseCoordinatorEmail() != null && userScope.getEmail() != null) {
                isCoord = offering.getCourseCoordinatorEmail().trim().equalsIgnoreCase(userScope.getEmail().trim());
            }
            if (!isCoord && offering.getCoordinatorEmail() != null && userScope.getEmail() != null) {
                isCoord = offering.getCoordinatorEmail().trim().equalsIgnoreCase(userScope.getEmail().trim());
            }
            if (!isCoord && offering.getCourseCoordinatorName() != null && userScope.getName() != null) {
                isCoord = offering.getCourseCoordinatorName().trim().equalsIgnoreCase(userScope.getName().trim());
            }
            if (!isCoord && offering.getCourseCoordinatorName() != null && userScope.getEmail() != null) {
                isCoord = offering.getCourseCoordinatorName().trim().equalsIgnoreCase(userScope.getEmail().trim());
            }
            boolean isAssigned = isCoord;
            if (!isAssigned && offering.getAssignedFaculty() != null) {
                String assigned = offering.getAssignedFaculty().toLowerCase();
                if (userScope.getEmail() != null && assigned.contains(userScope.getEmail().trim().toLowerCase())) {
                    isAssigned = true;
                } else if (userScope.getName() != null && assigned.contains(userScope.getName().trim().toLowerCase())) {
                    isAssigned = true;
                }
            }
            if (!isAssigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
            }
        }
    }

    private int compareCoCodes(String code1, String code2) {
        if (code1 == null && code2 == null) return 0;
        if (code1 == null) return -1;
        if (code2 == null) return 1;
        String c1 = code1.trim().toUpperCase();
        String c2 = code2.trim().toUpperCase();
        int num1 = extractOutcomeDigits(c1);
        int num2 = extractOutcomeDigits(c2);
        if (num1 != num2) {
            return Integer.compare(num1, num2);
        }
        return c1.compareTo(c2);
    }

    private ResolvedCourseAttainmentData resolveCourseAttainmentData(
            ProgrammeBatchCourse offering,
            ProgrammeBatch batch,
            CourseAttainmentReport report,
            AttainmentConfiguration config,
            List<CourseOutcome> cos) {

        BigDecimal directWeight = config != null && config.getEffectiveApprovedDirectWeight() != null
                ? config.getEffectiveApprovedDirectWeight()
                : (config != null && config.getDirectWeight() != null ? config.getDirectWeight() : new BigDecimal("80.00"));
        BigDecimal indirectWeight = config != null && config.getEffectiveApprovedIndirectWeight() != null
                ? config.getEffectiveApprovedIndirectWeight()
                : (config != null && config.getIndirectWeight() != null ? config.getIndirectWeight() : new BigDecimal("20.00"));

        boolean isFinalized = report != null &&
                (report.getStatus() == ReportStatus.FINALIZED || report.getStatus() == ReportStatus.APPROVED);

        if (isFinalized && report.getTable3CoAttainmentJson() != null && !report.getTable3CoAttainmentJson().isBlank()) {
            List<CourseAttainmentReportDto.Table3Row> t3Rows = parseTable3CoAttainments(report.getTable3CoAttainmentJson());
            if (t3Rows != null && !t3Rows.isEmpty()) {
                Map<String, ResolvedCoMetricData> coMap = new LinkedHashMap<>();
                for (CourseAttainmentReportDto.Table3Row t3 : t3Rows) {
                    if (t3.getCoCode() == null) continue;
                    String code = t3.getCoCode().trim().toUpperCase();
                    BigDecimal target = t3.getTargetLevel() != null ? t3.getTargetLevel().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                    BigDecimal direct = t3.getDirectLevel() != null
                            ? BigDecimal.valueOf(t3.getDirectLevel()).setScale(2, RoundingMode.HALF_UP)
                            : (t3.getDirectPercentage() != null ? t3.getDirectPercentage().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
                    BigDecimal indirect = t3.getIndirectScore() != null
                            ? t3.getIndirectScore().setScale(2, RoundingMode.HALF_UP)
                            : (t3.getIndirectLevel() != null ? BigDecimal.valueOf(t3.getIndirectLevel()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
                    BigDecimal finalAtt = t3.getFinalAttainment() != null
                            ? t3.getFinalAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                    Boolean met = t3.getTargetMet() != null ? t3.getTargetMet() : (finalAtt.compareTo(target) >= 0);
                    String stmt = t3.getStatement() != null && !t3.getStatement().isBlank() ? t3.getStatement() : "Course outcome " + code;
                    coMap.put(code, new ResolvedCoMetricData(code, stmt, direct, indirect, finalAtt, target, met));
                }
                BigDecimal overall = report.getOverallCoAttainment() != null
                        ? report.getOverallCoAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                BigDecimal directAtt = report.getDirectAttainment() != null
                        ? report.getDirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                BigDecimal indirectAtt = report.getIndirectAttainment() != null
                        ? report.getIndirectAttainment().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
                return new ResolvedCourseAttainmentData(directAtt, indirectAtt, overall, directWeight, indirectWeight, coMap);
            }
        }

        // Active / in-progress or unfinalized course: calculate in-memory continuous attainment (read-only)
        try {
            Map<String, Object> calc = attainmentCalculationService.calculateCourseCoAttainment(offering.getId());
            if (calc != null) {
                BigDecimal overall = calc.get("overallCoAttainment") instanceof BigDecimal b ? b.setScale(2, RoundingMode.HALF_UP)
                        : (calc.get("overallCoAttainment") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
                BigDecimal direct = calc.get("directAttainment") instanceof BigDecimal b ? b.setScale(2, RoundingMode.HALF_UP)
                        : (calc.get("directAttainment") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
                BigDecimal indirect = calc.get("indirectAttainment") instanceof BigDecimal b ? b.setScale(2, RoundingMode.HALF_UP)
                        : (calc.get("indirectAttainment") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));

                Object coAttObj = calc.get("coAttainments");
                if (coAttObj == null) coAttObj = calc.get("coAttainment");
                Map<String, ResolvedCoMetricData> coMap = new LinkedHashMap<>();
                if (coAttObj instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Map<?, ?> m) {
                            String code = m.get("coCode") != null ? m.get("coCode").toString().trim().toUpperCase() : null;
                            if (code == null) continue;
                            String stmt = m.get("statement") != null ? m.get("statement").toString() : "Course outcome " + code;
                            BigDecimal target = m.get("target") instanceof BigDecimal b ? b.setScale(2, RoundingMode.HALF_UP)
                                    : (m.get("target") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50"));
                            BigDecimal dir = m.get("directScore") instanceof BigDecimal b ? b.setScale(2, RoundingMode.HALF_UP)
                                    : (m.get("directLevel") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
                            BigDecimal ind = m.get("indirectScore") instanceof BigDecimal b ? b.setScale(2, RoundingMode.HALF_UP)
                                    : (m.get("indirectLevel") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
                            BigDecimal finalAtt = m.get("finalAttainment") instanceof BigDecimal b ? b.setScale(2, RoundingMode.HALF_UP)
                                    : (m.get("combinedAttainment") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
                            Boolean met = m.get("targetMet") instanceof Boolean b ? b : (finalAtt.compareTo(target) >= 0);
                            coMap.put(code, new ResolvedCoMetricData(code, stmt, dir, ind, finalAtt, target, met));
                        }
                    }
                }
                if (!coMap.isEmpty()) {
                    return new ResolvedCourseAttainmentData(direct, indirect, overall, directWeight, indirectWeight, coMap);
                }
            }
        } catch (Exception e) {
            log.warn("[AnalyticsService] Error calculating live continuous attainment for course offering {}: {}", offering.getId(), e.getMessage());
        }

        // Fallback: load course outcomes with default/zero values
        Map<String, ResolvedCoMetricData> fallbackCoMap = new LinkedHashMap<>();
        List<CourseOutcome> outcomes = (cos != null && !cos.isEmpty()) ? cos : courseOutcomeRepository.findByProgrammeBatchCourseId(offering.getId());
        if (outcomes != null) {
            for (CourseOutcome co : outcomes) {
                if (co.getCode() == null) continue;
                String code = co.getCode().trim().toUpperCase();
                String stmt = co.getStatement() != null && !co.getStatement().isBlank() ? co.getStatement() : "Course outcome " + code;
                BigDecimal target = co.getTargetLevel() != null ? co.getTargetLevel().setScale(2, RoundingMode.HALF_UP) : new BigDecimal("2.50");
                fallbackCoMap.put(code, new ResolvedCoMetricData(code, stmt, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), target, false));
            }
        }
        return new ResolvedCourseAttainmentData(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2), directWeight, indirectWeight, fallbackCoMap);
    }

    private record ResolvedCourseAttainmentData(
            BigDecimal directAttainment,
            BigDecimal indirectAttainment,
            BigDecimal overallCourseAttainment,
            BigDecimal directWeight,
            BigDecimal indirectWeight,
            Map<String, ResolvedCoMetricData> coMetrics
    ) {}

    private record ResolvedCoMetricData(
            String coCode,
            String statement,
            BigDecimal directAttainment,
            BigDecimal indirectAttainment,
            BigDecimal overallAttainment,
            BigDecimal targetLevel,
            Boolean targetMet
    ) {}
}
