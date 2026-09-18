package com.dypiu.nba.service;

import com.dypiu.nba.dto.ConsolidatedIndirectAttainmentDto;
import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.dto.CourseMappingMatrixDto;
import com.dypiu.nba.dto.ProgrammeAtrReportDto;
import com.dypiu.nba.dto.ProgrammeAttainmentResultDto;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
import com.dypiu.nba.dto.ProgrammeSurveyResultDto;
import com.dypiu.nba.dto.analytics.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
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
}
