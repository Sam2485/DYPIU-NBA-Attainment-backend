package com.dypiu.nba.service;

import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.dto.ProgrammeAtrReportDto;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
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
@Transactional(readOnly = true)
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
    private final CurrentUserScopeService currentUserScopeService;
    private final ObjectMapper objectMapper;

    // ==========================================
    // 1. KPI ENDPOINT
    // ==========================================
    public AnalyticsKpiResponseDto getKpis(String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId) {
        ResolvedScope scope = validateAndResolveScope(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        List<ProgrammeBatch> batchesInScope = getBatchesInScope(scope);
        List<String> batchIds = batchesInScope.stream().map(ProgrammeBatch::getId).toList();

        // 1. Scope summary
        long totalSchools = scope.schoolId != null ? 1 : schoolRepository.count();
        long totalDepts = scope.departmentId != null ? 1 : (scope.schoolId != null ? departmentRepository.findBySchoolId(scope.schoolId).size() : departmentRepository.count());
        long totalProgs = scope.masterProgrammeId != null ? 1 : (scope.departmentId != null ? masterProgrammeRepository.findByDepartmentIdAndDeletedAtIsNull(scope.departmentId).size() : masterProgrammeRepository.count());

        List<ProgrammeBatchAttainmentReport> finalizedReports = getFinalizedReports(batchIds);
        long totalEvaluatedBatches = finalizedReports.size();

        List<String> courseOfferingIds = batchIds.isEmpty() ? Collections.emptyList() :
                programmeBatchCourseRepository.findByProgrammeBatchIdIn(batchIds).stream().map(ProgrammeBatchCourse::getId).toList();
        long totalEvaluatedCourses = courseOfferingIds.isEmpty() ? 0 :
                courseAttainmentReportRepository.findByProgrammeBatchCourseIdIn(courseOfferingIds).stream()
                        .filter(r -> r.getStatus() == ReportStatus.FINALIZED || r.getStatus() == ReportStatus.APPROVED)
                        .count();

        AnalyticsKpiResponseDto.ScopeSummary scopeSummary = AnalyticsKpiResponseDto.ScopeSummary.builder()
                .totalSchools(totalSchools)
                .totalDepartments(totalDepts)
                .totalMasterProgrammes(totalProgs)
                .totalEvaluatedBatches(totalEvaluatedBatches)
                .totalEvaluatedCourseOfferings(totalEvaluatedCourses)
                .dataSourceCurrency("FINALIZED_EVALUATED_DATA")
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

        for (ProgrammeBatchAttainmentReport report : finalizedReports) {
            ParsedReport parsed = parseReport(report);
            if (parsed.pos.isEmpty() && parsed.psos.isEmpty()) {
                continue;
            }
            totalEvaluatedCohorts++;
            boolean cohortHasGap = false;

            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : parsed.pos) {
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

            for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : parsed.psos) {
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
        List<ProgrammeBatchAttainmentReport> finalizedReports = getFinalizedReports(batchIds);

        // Group evaluated PO instances by poCode (PO1 .. PO12)
        Map<String, List<PoInstanceData>> poInstances = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            poInstances.put("PO" + i, new ArrayList<>());
        }

        Map<String, String> poStatements = new HashMap<>();

        for (ProgrammeBatchAttainmentReport report : finalizedReports) {
            ParsedReport parsed = parseReport(report);
            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : parsed.pos) {
                if (po.getPoCode() == null) continue;
                String code = po.getPoCode().toUpperCase().trim();
                if (po.getStatement() != null && !po.getStatement().isBlank()) {
                    poStatements.put(code, po.getStatement());
                }
                if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                    poInstances.computeIfAbsent(code, k -> new ArrayList<>()).add(new PoInstanceData(
                            report.getProgrammeBatchId(),
                            po.getFinalAttainment(),
                            po.getTargetLevel(),
                            po.getDirectAttainment() != null ? po.getDirectAttainment() : BigDecimal.ZERO,
                            po.getIndirectAttainment() != null ? po.getIndirectAttainment() : BigDecimal.ZERO
                    ));
                }
            }
        }

        List<PoHealthItemDto> result = new ArrayList<>();
        for (Map.Entry<String, List<PoInstanceData>> entry : poInstances.entrySet()) {
            String poCode = entry.getKey();
            List<PoInstanceData> list = entry.getValue();
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
        List<ProgrammeBatchAttainmentReport> finalizedReports = getFinalizedReports(batchIds);

        Map<String, List<PoInstanceData>> psoInstances = new LinkedHashMap<>();
        Map<String, String> psoStatements = new HashMap<>();

        for (ProgrammeBatchAttainmentReport report : finalizedReports) {
            ParsedReport parsed = parseReport(report);
            for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : parsed.psos) {
                if (pso.getPsoCode() == null) continue;
                String code = pso.getPsoCode().toUpperCase().trim();
                if (pso.getStatement() != null && !pso.getStatement().isBlank()) {
                    psoStatements.put(code, pso.getStatement());
                }
                if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                    psoInstances.computeIfAbsent(code, k -> new ArrayList<>()).add(new PoInstanceData(
                            report.getProgrammeBatchId(),
                            pso.getFinalAttainment(),
                            pso.getTargetLevel(),
                            pso.getDirectAttainment() != null ? pso.getDirectAttainment() : BigDecimal.ZERO,
                            pso.getIndirectAttainment() != null ? pso.getIndirectAttainment() : BigDecimal.ZERO
                    ));
                }
            }
        }

        List<PsoHealthItemDto> result = new ArrayList<>();
        for (Map.Entry<String, List<PoInstanceData>> entry : psoInstances.entrySet()) {
            String psoCode = entry.getKey();
            List<PoInstanceData> list = entry.getValue();
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

        ResolvedScope scope = validateAndResolveScope(schoolId, departmentId, masterProgrammeId, programmeBatchId);
        List<ProgrammeBatch> allBatches = getBatchesInScope(scope);
        List<String> batchIds = allBatches.stream().map(ProgrammeBatch::getId).toList();

        Map<String, ProgrammeBatchAttainmentReport> reportMap = programmeBatchAttainmentReportRepository.findByProgrammeBatchIdIn(batchIds).stream()
                .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        Map<String, ProgrammeAtr> atrMap = programmeAtrRepository.findByProgrammeBatchIdIn(batchIds).stream()
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

            boolean isFinalized = report != null && (report.getStatus() == ReportStatus.FINALIZED || report.getStatus() == ReportStatus.APPROVED);
            ParsedReport parsed = (isFinalized && report != null) ? parseReport(report) : new ParsedReport(Collections.emptyList(), Collections.emptyList());

            int posEvaluated = 0;
            int posMet = 0;
            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : parsed.pos) {
                if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                    posEvaluated++;
                    if (po.getFinalAttainment().compareTo(po.getTargetLevel()) >= 0) {
                        posMet++;
                    }
                }
            }

            int psosEvaluated = 0;
            int psosMet = 0;
            for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : parsed.psos) {
                if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                    psosEvaluated++;
                    if (pso.getFinalAttainment().compareTo(pso.getTargetLevel()) >= 0) {
                        psosMet++;
                    }
                }
            }

            int gapCount = (posEvaluated - posMet) + (psosEvaluated - psosMet);
            boolean hasGaps = gapCount > 0;

            String reportAvailability = isFinalized ? "FINALIZED_REPORT_AVAILABLE" : "NO_FINALIZED_REPORT";
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
                    .startYear(batch.getStartYear())
                    .endYear(batch.getEndYear())
                    .posEvaluated(posEvaluated)
                    .posMet(posMet)
                    .posTotal(12)
                    .psosEvaluated(psosEvaluated)
                    .psosMet(psosMet)
                    .psosTotal(3)
                    .gapCount(gapCount)
                    .hasGaps(hasGaps)
                    .reportAvailabilityStatus(reportAvailability)
                    .underlyingReportStatus(report != null ? report.getStatus() : null)
                    .atrStatus(atrStatusStr)
                    .finalizedAt(finalizedTimestamp)
                    .build();

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
            if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
                if (statusFilter.equalsIgnoreCase("ALL_TARGETS_MET") && (!isFinalized || hasGaps)) {
                    continue;
                } else if (statusFilter.equalsIgnoreCase("HAS_GAPS") && (!isFinalized || !hasGaps)) {
                    continue;
                } else if (statusFilter.equalsIgnoreCase("NO_FINALIZED_REPORT") && isFinalized) {
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
        List<ProgrammeBatchAttainmentReport> finalizedReports = getFinalizedReports(batchIds);

        Map<String, MasterProgramme> progMap = masterProgrammeRepository.findAll().stream()
                .collect(Collectors.toMap(MasterProgramme::getId, p -> p, (a, b) -> a));
        Map<String, Department> deptMap = departmentRepository.findAll().stream()
                .collect(Collectors.toMap(Department::getId, d -> d, (a, b) -> a));
        Map<String, ProgrammeBatch> batchMap = batchesInScope.stream()
                .collect(Collectors.toMap(ProgrammeBatch::getId, b -> b, (a, b) -> a));
        Map<String, ProgrammeAtr> atrMap = programmeAtrRepository.findByProgrammeBatchIdIn(batchIds).stream()
                .collect(Collectors.toMap(ProgrammeAtr::getProgrammeBatchId, a -> a, (a, b) -> a));

        List<AttentionAreaItemDto> allDeficits = new ArrayList<>();

        for (ProgrammeBatchAttainmentReport report : finalizedReports) {
            ParsedReport parsed = parseReport(report);
            ProgrammeBatch batch = batchMap.get(report.getProgrammeBatchId());
            MasterProgramme prog = batch != null ? progMap.get(batch.getMasterProgrammeId()) : null;
            Department dept = prog != null && prog.getDepartmentId() != null ? deptMap.get(prog.getDepartmentId()) : null;
            ProgrammeAtr atr = atrMap.get(report.getProgrammeBatchId());

            // Process PO deficits
            if (outcomeType == null || outcomeType.equalsIgnoreCase("ALL") || outcomeType.equalsIgnoreCase("PO")) {
                for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : parsed.pos) {
                    if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                        BigDecimal gap = po.getFinalAttainment().subtract(po.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                        if (gap.compareTo(BigDecimal.ZERO) < 0) {
                            BigDecimal pct = po.getTargetLevel().compareTo(BigDecimal.ZERO) > 0
                                    ? po.getFinalAttainment().divide(po.getTargetLevel(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO;

                            allDeficits.add(AttentionAreaItemDto.builder()
                                    .id(report.getProgrammeBatchId() + "_" + po.getPoCode())
                                    .masterProgrammeId(prog != null ? prog.getId() : "")
                                    .programmeName(prog != null ? prog.getName() : "")
                                    .programmeCode(prog != null ? prog.getCode() : "")
                                    .programmeBatchId(report.getProgrammeBatchId())
                                    .batchName(batch != null ? batch.getName() : "")
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
                for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : parsed.psos) {
                    if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                        BigDecimal gap = pso.getFinalAttainment().subtract(pso.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                        if (gap.compareTo(BigDecimal.ZERO) < 0) {
                            BigDecimal pct = pso.getTargetLevel().compareTo(BigDecimal.ZERO) > 0
                                    ? pso.getFinalAttainment().divide(pso.getTargetLevel(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                    : BigDecimal.ZERO;

                            allDeficits.add(AttentionAreaItemDto.builder()
                                    .id(report.getProgrammeBatchId() + "_" + pso.getPsoCode())
                                    .masterProgrammeId(prog != null ? prog.getId() : "")
                                    .programmeName(prog != null ? prog.getName() : "")
                                    .programmeCode(prog != null ? prog.getCode() : "")
                                    .programmeBatchId(report.getProgrammeBatchId())
                                    .batchName(batch != null ? batch.getName() : "")
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
            if (cReport == null) continue;

            // Parse Table 1 mapping to see if this course maps to the outcome
            List<CourseAttainmentReportDto.Table1Row> table1 = parseTable1Mapping(cReport.getTable1MappingJson());
            List<CourseAttainmentReportDto.Table3Row> table3 = parseTable3CoAttainments(cReport.getTable3CoAttainmentJson());

            Map<String, CourseAttainmentReportDto.Table3Row> t3Map = table3.stream()
                    .collect(Collectors.toMap(t -> t.getCoCode() != null ? t.getCoCode().toUpperCase().trim() : "", t -> t, (a, b) -> a));

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
                    BigDecimal coOverall = t3 != null && t3.getFinalAttainment() != null ? t3.getFinalAttainment() : cReport.getOverallCoAttainment();
                    BigDecimal coDirect = t3 != null && t3.getDirectLevel() != null ? BigDecimal.valueOf(t3.getDirectLevel()) : cReport.getDirectAttainment();
                    BigDecimal coIndirect = t3 != null && t3.getIndirectLevel() != null ? BigDecimal.valueOf(t3.getIndirectLevel()) : cReport.getIndirectAttainment();
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
        Map<String, ProgrammeBatchAttainmentReport> reportMap = getFinalizedReports(batchIds).stream()
                .collect(Collectors.toMap(ProgrammeBatchAttainmentReport::getProgrammeBatchId, r -> r, (a, b) -> a));

        List<ScopedTrendSeriesDto> seriesList = new ArrayList<>();

        for (Map.Entry<String, List<ProgrammeBatch>> entry : batchesByProg.entrySet()) {
            String progId = entry.getKey();
            MasterProgramme prog = progMap.get(progId);
            String progName = prog != null ? prog.getName() : "Programme " + progId;

            List<ProgrammeBatch> pBatches = entry.getValue().stream()
                    .filter(b -> reportMap.containsKey(b.getId()))
                    .sorted(Comparator.comparing((ProgrammeBatch b) -> b.getStartYear() != null ? b.getStartYear() : 0))
                    .collect(Collectors.toList());

            // If numCohorts is explicitly requested, slice to the last numCohorts; otherwise return all available finalized cohorts
            if (numCohorts != null && numCohorts > 0 && pBatches.size() > numCohorts) {
                pBatches = pBatches.subList(pBatches.size() - numCohorts, pBatches.size());
            }

            List<CohortOutcomeDataPointDto> points = new ArrayList<>();
            for (ProgrammeBatch batch : pBatches) {
                ProgrammeBatchAttainmentReport report = reportMap.get(batch.getId());
                if (report == null) continue;
                ParsedReport parsed = parseReport(report);

                for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : parsed.pos) {
                    if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                        BigDecimal gap = po.getFinalAttainment().subtract(po.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                        points.add(CohortOutcomeDataPointDto.builder()
                                .programmeBatchId(batch.getId())
                                .batchName(batch.getName())
                                .startYear(batch.getStartYear())
                                .endYear(batch.getEndYear())
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

                for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : parsed.psos) {
                    if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                        BigDecimal gap = pso.getFinalAttainment().subtract(pso.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                        points.add(CohortOutcomeDataPointDto.builder()
                                .programmeBatchId(batch.getId())
                                .batchName(batch.getName())
                                .startYear(batch.getStartYear())
                                .endYear(batch.getEndYear())
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
        List<ProgrammeBatchAttainmentReport> finalizedReports = getFinalizedReports(batchIds);

        List<ProgrammeAtr> atrsInScope = programmeAtrRepository.findByProgrammeBatchIdIn(batchIds);
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

        for (ProgrammeBatchAttainmentReport report : finalizedReports) {
            ParsedReport parsed = parseReport(report);
            ProgrammeBatch batch = batchMap.get(report.getProgrammeBatchId());
            MasterProgramme prog = batch != null ? progMap.get(batch.getMasterProgrammeId()) : null;
            Department dept = prog != null && prog.getDepartmentId() != null ? deptMap.get(prog.getDepartmentId()) : null;
            ProgrammeAtr atr = atrMap.get(report.getProgrammeBatchId());

            // Process PO gaps
            for (ProgrammeBatchAttainmentReportDto.Report4PoRow po : parsed.pos) {
                if (po.getFinalAttainment() != null && po.getTargetLevel() != null) {
                    BigDecimal gap = po.getFinalAttainment().subtract(po.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                    if (gap.compareTo(BigDecimal.ZERO) < 0) {
                        BigDecimal pct = po.getTargetLevel().compareTo(BigDecimal.ZERO) > 0
                                ? po.getFinalAttainment().divide(po.getTargetLevel(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        List<String> actions = atr != null ? parseAtrActions(atr.getObservationsJson(), po.getPoCode(), "PO") : Collections.emptyList();

                        gapRecords.add(AtrGapDetailDto.builder()
                                .id(report.getProgrammeBatchId() + "_" + po.getPoCode())
                                .masterProgrammeId(prog != null ? prog.getId() : "")
                                .programmeName(prog != null ? prog.getName() : "")
                                .programmeCode(prog != null ? prog.getCode() : "")
                                .programmeBatchId(report.getProgrammeBatchId())
                                .batchName(batch != null ? batch.getName() : "")
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
            for (ProgrammeBatchAttainmentReportDto.Report4PsoRow pso : parsed.psos) {
                if (pso.getFinalAttainment() != null && pso.getTargetLevel() != null) {
                    BigDecimal gap = pso.getFinalAttainment().subtract(pso.getTargetLevel()).setScale(2, RoundingMode.HALF_UP);
                    if (gap.compareTo(BigDecimal.ZERO) < 0) {
                        BigDecimal pct = pso.getTargetLevel().compareTo(BigDecimal.ZERO) > 0
                                ? pso.getFinalAttainment().divide(pso.getTargetLevel(), 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        List<String> actions = atr != null ? parseAtrActions(atr.getObservationsJson(), pso.getPsoCode(), "PSO") : Collections.emptyList();

                        gapRecords.add(AtrGapDetailDto.builder()
                                .id(report.getProgrammeBatchId() + "_" + pso.getPsoCode())
                                .masterProgrammeId(prog != null ? prog.getId() : "")
                                .programmeName(prog != null ? prog.getName() : "")
                                .programmeCode(prog != null ? prog.getCode() : "")
                                .programmeBatchId(report.getProgrammeBatchId())
                                .batchName(batch != null ? batch.getName() : "")
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
    // HELPER / SCOPE / PARSER METHODS
    // ==========================================
    private ResolvedScope validateAndResolveScope(String schoolId, String departmentId, String masterProgrammeId, String programmeBatchId) {
        String reqSchoolId = schoolId != null && !schoolId.isBlank() ? schoolId.trim() : null;
        String reqDepartmentId = departmentId != null && !departmentId.isBlank() ? departmentId.trim() : null;
        String reqMasterProgrammeId = masterProgrammeId != null && !masterProgrammeId.isBlank() ? masterProgrammeId.trim() : null;
        String reqProgrammeBatchId = programmeBatchId != null && !programmeBatchId.isBlank() ? programmeBatchId.trim() : null;

        // Hierarchy validation
        if (reqSchoolId != null) {
            if (!schoolRepository.existsById(reqSchoolId)) {
                throw new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: School not found: " + reqSchoolId);
            }
        }

        if (reqDepartmentId != null) {
            Department dept = departmentRepository.findById(reqDepartmentId)
                    .orElseThrow(() -> new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: Department not found: " + reqDepartmentId));
            if (reqSchoolId != null && !reqSchoolId.equalsIgnoreCase(dept.getSchoolId())) {
                throw new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: Department '" + reqDepartmentId + "' does not belong to school '" + reqSchoolId + "'");
            }
        }

        if (reqMasterProgrammeId != null) {
            MasterProgramme prog = masterProgrammeRepository.findById(reqMasterProgrammeId)
                    .orElseThrow(() -> new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: MasterProgramme not found: " + reqMasterProgrammeId));
            if (reqDepartmentId != null && !reqDepartmentId.equalsIgnoreCase(prog.getDepartmentId())) {
                throw new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: MasterProgramme '" + reqMasterProgrammeId + "' does not belong to department '" + reqDepartmentId + "'");
            }
        }

        if (reqProgrammeBatchId != null) {
            ProgrammeBatch batch = programmeBatchRepository.findById(reqProgrammeBatchId)
                    .orElseThrow(() -> new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: ProgrammeBatch not found: " + reqProgrammeBatchId));
            if (reqMasterProgrammeId != null && !reqMasterProgrammeId.equalsIgnoreCase(batch.getMasterProgrammeId())) {
                throw new BadRequestException("ERR_HIERARCHY_SCOPE_MISMATCH: ProgrammeBatch '" + reqProgrammeBatchId + "' does not belong to masterProgramme '" + reqMasterProgrammeId + "'");
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
}
