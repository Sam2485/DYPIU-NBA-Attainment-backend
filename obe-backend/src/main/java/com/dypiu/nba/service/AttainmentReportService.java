package com.dypiu.nba.service;

import com.dypiu.nba.audit.AuditAction;
import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.dto.CourseMappingMatrixDto;
import com.dypiu.nba.dto.ProgrammeAttainmentResultDto;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
import com.dypiu.nba.entity.*;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AttainmentReportService {

    private final CourseAttainmentReportRepository courseAttainmentReportRepository;
    private final ProgrammeBatchAttainmentReportRepository programmeBatchAttainmentReportRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final MasterCourseRepository masterCourseRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final DepartmentRepository departmentRepository;
    private final SchoolRepository schoolRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final AttainmentCalculationService calculationService;
    private final OutcomeService outcomeService;
    private final CurrentUserScopeService currentUserScopeService;
    private final BatchLifecycleService batchLifecycleService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // 1. COURSE ATTAINMENT REPORT (TABLES 1, 2, 3)
    // =========================================================================

    @Transactional
    public CourseAttainmentReportDto getOrCreateCourseAttainmentReport(String courseOfferingId) {
        System.out.println("[AttainmentReportService] getOrCreateCourseAttainmentReport called | courseOfferingId: " + courseOfferingId);
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(courseOfferingId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found: " + courseOfferingId));

        enforceOfferingScope(offering);

        Optional<CourseAttainmentReport> existingOpt = courseAttainmentReportRepository.findByProgrammeBatchCourseId(offering.getId());
        if (existingOpt.isPresent()) {
            CourseAttainmentReport existing = existingOpt.get();
            if (existing.getStatus() == ReportStatus.APPROVED || existing.getStatus() == ReportStatus.FINALIZED) {
                return mapToDto(existing, offering);
            }
        }

        return generateAndSaveCourseReport(offering, ReportStatus.DRAFT);
    }

    @Transactional
    public CourseAttainmentReportDto finalizeCourseReport(String courseOfferingId, String actorName) {
        System.out.println("[AttainmentReportService] finalizeCourseReport called | courseOfferingId: " + courseOfferingId);
        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(courseOfferingId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found: " + courseOfferingId));

        enforceOfferingScope(offering);
        batchLifecycleService.enforceBatchEditability(offering.getProgrammeBatchId());

        CourseAttainmentReportDto dto = generateAndSaveCourseReport(offering, ReportStatus.FINALIZED);
        Optional<CourseAttainmentReport> reportOpt = courseAttainmentReportRepository.findByProgrammeBatchCourseId(offering.getId());
        if (reportOpt.isPresent()) {
            CourseAttainmentReport report = reportOpt.get();
            report.setSubmittedBy(actorName);
            report.setSubmittedAt(ZonedDateTime.now());
            courseAttainmentReportRepository.save(report);
            dto.setSubmittedBy(report.getSubmittedBy());
            dto.setSubmittedAt(report.getSubmittedAt());
        }

        auditLogService.recordSuccess(
                AuditAction.SUBMIT,
                ResourceType.COURSE_ATTAINMENT,
                offering.getId(),
                "DRAFT",
                "FINALIZED",
                "Course Attainment Report finalized for offering: " + offering.getId(),
                Map.of("overallCoAttainment", dto.getOverallCoAttainment())
        );

        return dto;
    }

    @Transactional
    public CourseAttainmentReportDto generateAndSaveCourseReport(ProgrammeBatchCourse offering, ReportStatus status) {
        MasterCourse course = masterCourseRepository.findById(offering.getMasterCourseId()).orElse(null);
        ProgrammeBatch batch = programmeBatchRepository.findById(offering.getProgrammeBatchId()).orElse(null);

        Map<String, Object> calcResult = calculationService.calculateCourseCoAttainment(offering.getId());
        CourseMappingMatrixDto matrixDto = outcomeService.getCourseMappings(offering.getId());

        // --- Table 1: CO -> PO/PSO Articulation Matrix ---
        List<CourseAttainmentReportDto.Table1Row> table1 = new ArrayList<>();
        if (matrixDto != null && matrixDto.getMatrix() != null) {
            for (Map.Entry<String, Map<String, Integer>> entry : matrixDto.getMatrix().entrySet()) {
                String coCode = entry.getKey();
                Map<String, Integer> poMap = new LinkedHashMap<>();
                Map<String, Integer> psoMap = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> m : entry.getValue().entrySet()) {
                    if (m.getKey().toUpperCase().startsWith("PSO")) {
                        psoMap.put(m.getKey(), m.getValue());
                    } else {
                        poMap.put(m.getKey(), m.getValue());
                    }
                }
                table1.add(CourseAttainmentReportDto.Table1Row.builder()
                        .coCode(coCode)
                        .poMappings(poMap)
                        .psoMappings(psoMap)
                        .build());
            }
        }

        // --- Table 2: Course PO/PSO Direct Attainment Contribution ---
        List<CourseAttainmentReportDto.Table2PoRow> table2PO = new ArrayList<>();
        List<CourseAttainmentReportDto.Table2PsoRow> table2PSO = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> poAtt = (Map<String, BigDecimal>) calcResult.getOrDefault("poAttainment", Collections.emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> psoAtt = (Map<String, BigDecimal>) calcResult.getOrDefault("psoAttainment", Collections.emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> poAvg = (Map<String, BigDecimal>) calcResult.getOrDefault("poAverages", Collections.emptyMap());
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> psoAvg = (Map<String, BigDecimal>) calcResult.getOrDefault("psoAverages", Collections.emptyMap());

        for (Map.Entry<String, BigDecimal> e : poAtt.entrySet()) {
            table2PO.add(CourseAttainmentReportDto.Table2PoRow.builder()
                    .poCode(e.getKey())
                    .averageMapping(poAvg.getOrDefault(e.getKey(), BigDecimal.ZERO))
                    .directContribution(e.getValue())
                    .build());
        }
        for (Map.Entry<String, BigDecimal> e : psoAtt.entrySet()) {
            table2PSO.add(CourseAttainmentReportDto.Table2PsoRow.builder()
                    .psoCode(e.getKey())
                    .averageMapping(psoAvg.getOrDefault(e.getKey(), BigDecimal.ZERO))
                    .directContribution(e.getValue())
                    .build());
        }

        // --- Table 3: CO Direct + Indirect + Final CO Attainment ---
        List<CourseAttainmentReportDto.Table3Row> table3 = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> coDetails = (List<Map<String, Object>>) calcResult.getOrDefault("coAttainments", Collections.emptyList());

        for (Map<String, Object> m : coDetails) {
            String coCode = (String) m.get("coCode");
            String statement = (String) m.get("statement");
            BigDecimal target = (BigDecimal) m.get("target");
            BigDecimal directPct = (BigDecimal) m.get("directPct");
            Integer directLevel = (Integer) m.get("directLevel");
            BigDecimal indirectPct = (BigDecimal) m.get("indirectPct");
            BigDecimal indirectScore = (BigDecimal) m.get("indirectScore");
            Integer indirectLevel = (Integer) m.get("indirectLevel");
            BigDecimal combined = (BigDecimal) m.get("combinedAttainment");
            Boolean targetMet = (Boolean) m.get("targetMet");

            String obs = (targetMet != null && targetMet)
                    ? "Target achieved (" + combined + " >= " + target + ")"
                    : "Target not achieved (" + combined + " < " + target + ")";

            table3.add(CourseAttainmentReportDto.Table3Row.builder()
                    .coCode(coCode)
                    .statement(statement)
                    .targetLevel(target)
                    .directPercentage(directPct)
                    .directLevel(directLevel)
                    .indirectPercentage(indirectPct)
                    .indirectScore(indirectScore)
                    .indirectLevel(indirectLevel)
                    .finalAttainment(combined)
                    .targetMet(targetMet)
                    .observation(obs)
                    .build());
        }

        BigDecimal overall = (BigDecimal) calcResult.getOrDefault("overallCoAttainment", BigDecimal.ZERO);
        BigDecimal direct = (BigDecimal) calcResult.getOrDefault("directAttainment", BigDecimal.ZERO);
        BigDecimal indirect = (BigDecimal) calcResult.getOrDefault("indirectAttainment", BigDecimal.ZERO);

        CourseAttainmentReport report = courseAttainmentReportRepository.findByProgrammeBatchCourseId(offering.getId())
                .orElse(CourseAttainmentReport.builder()
                        .id("car-" + UUID.randomUUID().toString().substring(0, 10))
                        .programmeBatchCourseId(offering.getId())
                        .build());

        report.setStatus(status);
        report.setOverallCoAttainment(overall);
        report.setDirectAttainment(direct);
        report.setIndirectAttainment(indirect);
        report.setTable1MappingJson(toJson(table1));
        report.setTable2DirectJson(toJson(java.util.Map.of("po", table2PO, "pso", table2PSO)));
        report.setTable3CoAttainmentJson(toJson(table3));
        report.setUpdatedAt(ZonedDateTime.now());

        courseAttainmentReportRepository.save(report);

        return CourseAttainmentReportDto.builder()
                .id(report.getId())
                .programmeBatchCourseId(offering.getId())
                .masterCourseId(course != null ? course.getId() : offering.getMasterCourseId())
                .courseCode(offering.getEffectiveCourseCode(course))
                .courseName(offering.getEffectiveCourseName(course))
                .programmeBatchId(batch != null ? batch.getId() : offering.getProgrammeBatchId())
                .batchName(batch != null ? batch.getName() : "Batch")
                .semester(offering.getSemester())
                .status(report.getStatus())
                .overallCoAttainment(overall)
                .directAttainment(direct)
                .indirectAttainment(indirect)
                .table1Mapping(table1)
                .table2DirectPO(table2PO).table2DirectPSO(table2PSO)
                .table3CoAttainments(table3)
                .submittedBy(report.getSubmittedBy())
                .submittedAt(report.getSubmittedAt())
                .approvedBy(report.getApprovedBy())
                .approvedAt(report.getApprovedAt())
                .build();
    }

    private CourseAttainmentReportDto mapToDto(CourseAttainmentReport report, ProgrammeBatchCourse offering) {
        MasterCourse course = masterCourseRepository.findById(offering.getMasterCourseId()).orElse(null);
        ProgrammeBatch batch = programmeBatchRepository.findById(offering.getProgrammeBatchId()).orElse(null);

        List<CourseAttainmentReportDto.Table1Row> table1 = fromJson(report.getTable1MappingJson(), new TypeReference<>() {});
        java.util.Map<String, Object> t2Map = fromJson(report.getTable2DirectJson(), new TypeReference<>() {});
        List<CourseAttainmentReportDto.Table2PoRow> table2PO = t2Map == null ? null : objectMapper.convertValue(t2Map.get("po"), new TypeReference<>() {});
        List<CourseAttainmentReportDto.Table2PsoRow> table2PSO = t2Map == null ? null : objectMapper.convertValue(t2Map.get("pso"), new TypeReference<>() {});
        List<CourseAttainmentReportDto.Table3Row> table3 = fromJson(report.getTable3CoAttainmentJson(), new TypeReference<>() {});

        return CourseAttainmentReportDto.builder()
                .id(report.getId())
                .programmeBatchCourseId(offering.getId())
                .masterCourseId(course != null ? course.getId() : offering.getMasterCourseId())
                .courseCode(offering.getEffectiveCourseCode(course))
                .courseName(offering.getEffectiveCourseName(course))
                .programmeBatchId(batch != null ? batch.getId() : offering.getProgrammeBatchId())
                .batchName(batch != null ? batch.getName() : "Batch")
                .semester(offering.getSemester())
                .status(report.getStatus())
                .overallCoAttainment(report.getOverallCoAttainment())
                .directAttainment(report.getDirectAttainment())
                .indirectAttainment(report.getIndirectAttainment())
                .table1Mapping(table1 != null ? table1 : Collections.emptyList())
                .table2DirectPO(table2PO != null ? table2PO : Collections.emptyList()).table2DirectPSO(table2PSO != null ? table2PSO : Collections.emptyList())
                .table3CoAttainments(table3 != null ? table3 : Collections.emptyList())
                .submittedBy(report.getSubmittedBy())
                .submittedAt(report.getSubmittedAt())
                .approvedBy(report.getApprovedBy())
                .approvedAt(report.getApprovedAt())
                .build();
    }

    // =========================================================================
    // 2. PROGRAMME ATTAINMENT REPORTS (REPORTS 1, 2, 3, 4)
    // =========================================================================

    @Transactional
    public ProgrammeBatchAttainmentReportDto getOrCreateProgrammeAttainmentReport(String programmeId, String batchId) {
        System.out.println("[AttainmentReportService] getOrCreateProgrammeAttainmentReport called | programmeId: " + programmeId + " | batchId: " + batchId);
        ProgrammeBatch batch = programmeBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme Batch not found: " + batchId));
        MasterProgramme prog = masterProgrammeRepository.findById(programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Master Programme not found: " + programmeId));

        enforceProgrammeBatchScope(batch);

        Optional<ProgrammeBatchAttainmentReport> existingOpt = programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId());
        if (existingOpt.isPresent()) {
            ProgrammeBatchAttainmentReport existing = existingOpt.get();
            if (existing.getStatus() == ReportStatus.APPROVED || existing.getStatus() == ReportStatus.FINALIZED) {
                return mapToDto(existing, prog, batch);
            }
        }

        return generateAndSaveProgrammeReport(prog, batch, ReportStatus.DRAFT);
    }

    @Transactional
    public ProgrammeBatchAttainmentReportDto finalizeProgrammeReport(String programmeId, String batchId, String actorName) {
        System.out.println("[AttainmentReportService] finalizeProgrammeReport called | programmeId: " + programmeId + " | batchId: " + batchId);
        ProgrammeBatch batch = programmeBatchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Programme Batch not found: " + batchId));
        MasterProgramme prog = masterProgrammeRepository.findById(programmeId)
                .orElseThrow(() -> new ResourceNotFoundException("Master Programme not found: " + programmeId));

        enforceProgrammeBatchScope(batch);
        batchLifecycleService.enforceBatchEditability(batch.getId());

        ProgrammeBatchAttainmentReportDto dto = generateAndSaveProgrammeReport(prog, batch, ReportStatus.FINALIZED);
        Optional<ProgrammeBatchAttainmentReport> reportOpt = programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId());
        if (reportOpt.isPresent()) {
            ProgrammeBatchAttainmentReport report = reportOpt.get();
            report.setSubmittedBy(actorName);
            report.setSubmittedAt(ZonedDateTime.now());
            programmeBatchAttainmentReportRepository.save(report);
            dto.setSubmittedBy(report.getSubmittedBy());
            dto.setSubmittedAt(report.getSubmittedAt());
        }

        auditLogService.recordSuccess(
                AuditAction.SUBMIT,
                ResourceType.PROGRAMME_ATTAINMENT,
                batch.getId(),
                "DRAFT",
                "FINALIZED",
                "Programme Attainment Report finalized for batch: " + batch.getId(),
                Map.of("overallProgrammeAttainment", dto.getOverallProgrammeAttainment())
        );

        return dto;
    }

    @Transactional
    public ProgrammeBatchAttainmentReportDto generateAndSaveProgrammeReport(MasterProgramme prog, ProgrammeBatch batch, ReportStatus status) {
        ProgrammeAttainmentResultDto calcResult = calculationService.calculateProgrammeAttainment(prog.getId(), batch.getId());

        List<ProgrammeBatchAttainmentReportDto.Report1PoRow> report1PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report1PsoRow> report1PSO = new ArrayList<>();
        if (calcResult.getAverageMapping() != null && calcResult.getAverageMapping().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeMappingItem item : calcResult.getAverageMapping().getPos()) {
                List<ProgrammeBatchAttainmentReportDto.SemesterContribution> sList = item.getSemesterValues() != null
                        ? item.getSemesterValues().stream().map(sv -> ProgrammeBatchAttainmentReportDto.SemesterContribution.builder()
                        .semester(sv.getSemester()).value(sv.getAverageMapping()).build()).collect(Collectors.toList()) : Collections.emptyList();
                report1PO.add(ProgrammeBatchAttainmentReportDto.Report1PoRow.builder()
                        .poCode(item.getPoCode())
                        .semesterAverages(sList)
                        .programmeAverageMapping(item.getOverallAverage())
                        .build());
            }
        }
        if (calcResult.getAverageMapping() != null && calcResult.getAverageMapping().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeMappingItem item : calcResult.getAverageMapping().getPsos()) {
                List<ProgrammeBatchAttainmentReportDto.SemesterContribution> sList = item.getSemesterValues() != null
                        ? item.getSemesterValues().stream().map(sv -> ProgrammeBatchAttainmentReportDto.SemesterContribution.builder()
                        .semester(sv.getSemester()).value(sv.getAverageMapping()).build()).collect(Collectors.toList()) : Collections.emptyList();
                report1PSO.add(ProgrammeBatchAttainmentReportDto.Report1PsoRow.builder()
                        .psoCode(item.getPsoCode())
                        .semesterAverages(sList)
                        .programmeAverageMapping(item.getOverallAverage())
                        .build());
            }
        }

        List<ProgrammeBatchAttainmentReportDto.Report2PoRow> report2PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report2PsoRow> report2PSO = new ArrayList<>();
        if (calcResult.getAverageDirectAttainment() != null && calcResult.getAverageDirectAttainment().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeDirectItem item : calcResult.getAverageDirectAttainment().getPos()) {
                List<ProgrammeBatchAttainmentReportDto.SemesterContribution> sList = item.getSemesterValues() != null
                        ? item.getSemesterValues().stream().map(sv -> ProgrammeBatchAttainmentReportDto.SemesterContribution.builder()
                        .semester(sv.getSemester()).value(sv.getAverageAttainment()).build()).collect(Collectors.toList()) : Collections.emptyList();
                report2PO.add(ProgrammeBatchAttainmentReportDto.Report2PoRow.builder()
                        .poCode(item.getPoCode())
                        .semesterDirectAttainments(sList)
                        .programmeDirectAttainment(item.getOverallAverage())
                        .build());
            }
        }
        if (calcResult.getAverageDirectAttainment() != null && calcResult.getAverageDirectAttainment().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeDirectItem item : calcResult.getAverageDirectAttainment().getPsos()) {
                List<ProgrammeBatchAttainmentReportDto.SemesterContribution> sList = item.getSemesterValues() != null
                        ? item.getSemesterValues().stream().map(sv -> ProgrammeBatchAttainmentReportDto.SemesterContribution.builder()
                        .semester(sv.getSemester()).value(sv.getAverageAttainment()).build()).collect(Collectors.toList()) : Collections.emptyList();
                report2PSO.add(ProgrammeBatchAttainmentReportDto.Report2PsoRow.builder()
                        .psoCode(item.getPsoCode())
                        .semesterDirectAttainments(sList)
                        .programmeDirectAttainment(item.getOverallAverage())
                        .build());
            }
        }

        List<ProgrammeBatchAttainmentReportDto.Report3PoRow> report3PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report3PsoRow> report3PSO = new ArrayList<>();
        if (calcResult.getAverageIndirectAttainment() != null) {
            for (Map.Entry<String, BigDecimal> entry : calcResult.getAverageIndirectAttainment().entrySet()) {
                String code = entry.getKey();
                BigDecimal indirect = entry.getValue();
                if (code.startsWith("PO")) {
                    report3PO.add(ProgrammeBatchAttainmentReportDto.Report3PoRow.builder()
                        .poCode(code).percentageSubstantial(new BigDecimal("100.00")).percentageModerate(BigDecimal.ZERO).percentageSlight(BigDecimal.ZERO).weightedScore(indirect).indirectPercentage(new BigDecimal("100.00")).indirectAttainmentLevel(indirect).build());
                } else {
                    report3PSO.add(ProgrammeBatchAttainmentReportDto.Report3PsoRow.builder()
                        .psoCode(code).percentageSubstantial(new BigDecimal("100.00")).percentageModerate(BigDecimal.ZERO).percentageSlight(BigDecimal.ZERO).weightedScore(indirect).indirectPercentage(new BigDecimal("100.00")).indirectAttainmentLevel(indirect).build());
                }
            }
        }

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> report4PO = new ArrayList<>();
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> report4PSO = new ArrayList<>();
        BigDecimal sumOverall = BigDecimal.ZERO;
        int countOverall = 0;

        if (calcResult.getOverallAttainment() != null && calcResult.getOverallAttainment().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem item : calcResult.getOverallAttainment().getPos()) {
                String code = item.getPoCode();
                BigDecimal target = item.getTarget() != null ? item.getTarget() : new BigDecimal("2.50");
                BigDecimal finalVal = item.getOverallAttainment();
                boolean targetMet = finalVal != null && finalVal.compareTo(target) >= 0;
                String obs = item.getObservation() != null ? item.getObservation() : (targetMet ? "PO target attained (" + finalVal + " >= " + target + ")" : "PO target not attained (" + finalVal + " < " + target + ")");

                report4PO.add(ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                        .poCode(code).statement(item.getOutcomeStatement() != null ? item.getOutcomeStatement() : "Programme Outcome " + code)
                        .targetLevel(target).directAttainment(item.getDirectAttainment()).indirectAttainment(item.getIndirectAttainment()).finalAttainment(finalVal).targetMet(targetMet).observation(obs).build());
                if (finalVal != null) { sumOverall = sumOverall.add(finalVal); countOverall++; }
            }
        }
        if (calcResult.getOverallAttainment() != null && calcResult.getOverallAttainment().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem item : calcResult.getOverallAttainment().getPsos()) {
                String code = item.getPsoCode();
                BigDecimal target = item.getTarget() != null ? item.getTarget() : new BigDecimal("2.50");
                BigDecimal finalVal = item.getOverallAttainment();
                boolean targetMet = finalVal != null && finalVal.compareTo(target) >= 0;
                String obs = item.getObservation() != null ? item.getObservation() : (targetMet ? "PSO target attained (" + finalVal + " >= " + target + ")" : "PSO target not attained (" + finalVal + " < " + target + ")");

                report4PSO.add(ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder()
                        .psoCode(code).statement(item.getOutcomeStatement() != null ? item.getOutcomeStatement() : "Programme Specific Outcome " + code)
                        .targetLevel(target).directAttainment(item.getDirectAttainment()).indirectAttainment(item.getIndirectAttainment()).finalAttainment(finalVal).targetMet(targetMet).observation(obs).build());
                if (finalVal != null) { sumOverall = sumOverall.add(finalVal); countOverall++; }
            }
        }

        BigDecimal overall = countOverall > 0 ? sumOverall.divide(BigDecimal.valueOf(countOverall), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        ProgrammeBatchAttainmentReport report = programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId())
                .orElse(ProgrammeBatchAttainmentReport.builder()
                        .id("pbar-" + UUID.randomUUID().toString().substring(0, 10))
                        .programmeBatchId(batch.getId())
                        .build());

        report.setStatus(status);
        report.setOverallProgrammeAttainment(overall);
        report.setAverageMappingReportJson(toJson(java.util.Map.of("po", report1PO, "pso", report1PSO)));
        report.setDirectAttainmentReportJson(toJson(java.util.Map.of("po", report2PO, "pso", report2PSO)));
        report.setIndirectAttainmentReportJson(toJson(java.util.Map.of("po", report3PO, "pso", report3PSO)));
        report.setOverallAttainmentReportJson(toJson(java.util.Map.of("po", report4PO, "pso", report4PSO)));
        report.setUpdatedAt(ZonedDateTime.now());

        programmeBatchAttainmentReportRepository.save(report);

        return ProgrammeBatchAttainmentReportDto.builder()
                .id(report.getId())
                .programmeBatchId(batch.getId())
                .batchName(batch.getName())
                .masterProgrammeId(prog.getId())
                .programmeName(prog.getName())
                .programmeCode(prog.getCode())
                .status(report.getStatus())
                .overallProgrammeAttainment(overall)
                .report1AverageMappingPO(report1PO).report1AverageMappingPSO(report1PSO)
                .report2DirectAttainmentPO(report2PO).report2DirectAttainmentPSO(report2PSO)
                .report3IndirectAttainmentPO(report3PO).report3IndirectAttainmentPSO(report3PSO)
                .report4OverallAttainmentPO(report4PO).report4OverallAttainmentPSO(report4PSO)
                .submittedBy(report.getSubmittedBy())
                .submittedAt(report.getSubmittedAt())
                .approvedBy(report.getApprovedBy())
                .approvedAt(report.getApprovedAt())
                .build();
    }

    private ProgrammeBatchAttainmentReportDto mapToDto(ProgrammeBatchAttainmentReport report, MasterProgramme prog, ProgrammeBatch batch) {
        java.util.Map<String, Object> r1Map = fromJson(report.getAverageMappingReportJson(), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report1PoRow> report1PO = r1Map == null ? null : objectMapper.convertValue(r1Map.get("po"), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report1PsoRow> report1PSO = r1Map == null ? null : objectMapper.convertValue(r1Map.get("pso"), new TypeReference<>() {});

        java.util.Map<String, Object> r2Map = fromJson(report.getDirectAttainmentReportJson(), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report2PoRow> report2PO = r2Map == null ? null : objectMapper.convertValue(r2Map.get("po"), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report2PsoRow> report2PSO = r2Map == null ? null : objectMapper.convertValue(r2Map.get("pso"), new TypeReference<>() {});

        java.util.Map<String, Object> r3Map = fromJson(report.getIndirectAttainmentReportJson(), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report3PoRow> report3PO = r3Map == null ? null : objectMapper.convertValue(r3Map.get("po"), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report3PsoRow> report3PSO = r3Map == null ? null : objectMapper.convertValue(r3Map.get("pso"), new TypeReference<>() {});

        java.util.Map<String, Object> r4Map = fromJson(report.getOverallAttainmentReportJson(), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> report4PO = r4Map == null ? null : objectMapper.convertValue(r4Map.get("po"), new TypeReference<>() {});
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> report4PSO = r4Map == null ? null : objectMapper.convertValue(r4Map.get("pso"), new TypeReference<>() {});

        return ProgrammeBatchAttainmentReportDto.builder()
                .id(report.getId())
                .programmeBatchId(batch.getId())
                .batchName(batch.getName())
                .masterProgrammeId(prog.getId())
                .programmeName(prog.getName())
                .programmeCode(prog.getCode())
                .status(report.getStatus())
                .overallProgrammeAttainment(report.getOverallProgrammeAttainment())
                .report1AverageMappingPO(report1PO != null ? report1PO : Collections.emptyList()).report1AverageMappingPSO(report1PSO != null ? report1PSO : Collections.emptyList())
                .report2DirectAttainmentPO(report2PO != null ? report2PO : Collections.emptyList()).report2DirectAttainmentPSO(report2PSO != null ? report2PSO : Collections.emptyList())
                .report3IndirectAttainmentPO(report3PO != null ? report3PO : Collections.emptyList()).report3IndirectAttainmentPSO(report3PSO != null ? report3PSO : Collections.emptyList())
                .report4OverallAttainmentPO(report4PO != null ? report4PO : Collections.emptyList()).report4OverallAttainmentPSO(report4PSO != null ? report4PSO : Collections.emptyList())
                .submittedBy(report.getSubmittedBy())
                .submittedAt(report.getSubmittedAt())
                .approvedBy(report.getApprovedBy())
                .approvedAt(report.getApprovedAt())
                .build();
    }

    // =========================================================================
    // 3. HISTORICAL REPORT DISCOVERY (ACROSS BATCHES & YEARS)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<CourseAttainmentReportDto> getHistoricalCourseAttainmentReports(String masterCourseId) {
        System.out.println("[AttainmentReportService] getHistoricalCourseAttainmentReports called | masterCourseId: " + masterCourseId);
        MasterCourse course = masterCourseRepository.findById(masterCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Master Course not found: " + masterCourseId));

        enforceCourseScope(masterCourseId);

        List<ProgrammeBatchCourse> offerings = programmeBatchCourseRepository.findByMasterCourseId(masterCourseId);
        List<CourseAttainmentReportDto> result = new ArrayList<>();

        for (ProgrammeBatchCourse off : offerings) {
            Optional<CourseAttainmentReport> repOpt = courseAttainmentReportRepository.findByProgrammeBatchCourseId(off.getId());
            if (repOpt.isPresent()) {
                result.add(mapToDto(repOpt.get(), off));
            } else {
                try {
                    result.add(generateAndSaveCourseReport(off, ReportStatus.DRAFT));
                } catch (Exception e) {
                    log.warn("Could not assemble historical report for offering {}: {}", off.getId(), e.getMessage());
                }
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<ProgrammeBatchAttainmentReportDto> getHistoricalProgrammeAttainmentReports(String masterProgrammeId) {
        System.out.println("[AttainmentReportService] getHistoricalProgrammeAttainmentReports called | masterProgrammeId: " + masterProgrammeId);
        MasterProgramme prog = masterProgrammeRepository.findById(masterProgrammeId)
                .orElseThrow(() -> new ResourceNotFoundException("Master Programme not found: " + masterProgrammeId));

        enforceProgrammeScope(masterProgrammeId);

        List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeId(masterProgrammeId);
        List<ProgrammeBatchAttainmentReportDto> result = new ArrayList<>();

        for (ProgrammeBatch batch : batches) {
            Optional<ProgrammeBatchAttainmentReport> repOpt = programmeBatchAttainmentReportRepository.findByProgrammeBatchId(batch.getId());
            if (repOpt.isPresent()) {
                result.add(mapToDto(repOpt.get(), prog, batch));
            } else {
                try {
                    result.add(generateAndSaveProgrammeReport(prog, batch, ReportStatus.DRAFT));
                } catch (Exception e) {
                    log.warn("Could not assemble historical programme report for batch {}: {}", batch.getId(), e.getMessage());
                }
            }
        }

        return result;
    }

    // =========================================================================
    // 4. SECURITY & SCOPE ENFORCEMENT
    // =========================================================================

    private void enforceOfferingScope(ProgrammeBatchCourse offering) {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (scope.isAdmin() || scope.isIqac()) return;

        MasterCourse course = masterCourseRepository.findById(offering.getMasterCourseId()).orElse(null);
        if (course != null && course.getMasterProgrammeId() != null) {
            MasterProgramme prog = masterProgrammeRepository.findById(course.getMasterProgrammeId()).orElse(null);
            if (prog != null && prog.getDepartmentId() != null) {
                if (scope.isHod() && !prog.getDepartmentId().equals(scope.getDepartmentId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Offering outside your department.");
                }
            }
        }

        if (scope.isFaculty()) {
            boolean assigned = (offering.getCourseCoordinatorId() != null && offering.getCourseCoordinatorId().equals(scope.getUserId()))
                    || (scope.getEmail() != null && scope.getEmail().equalsIgnoreCase(offering.getAssignedFaculty()))
                    || (scope.getName() != null && scope.getName().equalsIgnoreCase(offering.getCourseCoordinatorName()));
            if (!assigned) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
            }
        }
    }

    private void enforceProgrammeBatchScope(ProgrammeBatch batch) {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (scope.isAdmin() || scope.isIqac()) return;

        MasterProgramme prog = masterProgrammeRepository.findById(batch.getMasterProgrammeId()).orElse(null);
        if (prog != null && prog.getDepartmentId() != null) {
            if (scope.isHod() && !prog.getDepartmentId().equals(scope.getDepartmentId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Batch outside your department.");
            }
        }
        if (scope.isProgrammeCoordinator() && !batch.getMasterProgrammeId().equals(scope.getProgrammeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Batch outside your assigned programme.");
        }
    }

    private void enforceCourseScope(String masterCourseId) {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (scope.isAdmin() || scope.isIqac()) return;

        MasterCourse course = masterCourseRepository.findById(masterCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Master Course not found: " + masterCourseId));
        if (course.getMasterProgrammeId() != null) {
            MasterProgramme prog = masterProgrammeRepository.findById(course.getMasterProgrammeId()).orElse(null);
            if (prog != null && prog.getDepartmentId() != null) {
                if (scope.isHod() && !prog.getDepartmentId().equals(scope.getDepartmentId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Course outside your department.");
                }
            }
        }
    }

    private void enforceProgrammeScope(String masterProgrammeId) {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (scope.isAdmin() || scope.isIqac()) return;

        MasterProgramme prog = masterProgrammeRepository.findById(masterProgrammeId)
                .orElseThrow(() -> new ResourceNotFoundException("Master Programme not found: " + masterProgrammeId));
        if (prog.getDepartmentId() != null && scope.isHod() && !prog.getDepartmentId().equals(scope.getDepartmentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme outside your department.");
        }
        if (scope.isProgrammeCoordinator() && !masterProgrammeId.equals(scope.getProgrammeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Programme outside your assigned programme.");
        }
    }

    // =========================================================================
    // 5. JSON HELPER UTILITIES
    // =========================================================================

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", e.getMessage());
            return "[]";
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("Failed to deserialize JSON to object: {}", e.getMessage());
            return null;
        }
    }
}
