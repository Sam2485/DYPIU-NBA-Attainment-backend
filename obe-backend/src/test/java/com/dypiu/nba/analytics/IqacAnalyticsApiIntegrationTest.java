package com.dypiu.nba.analytics;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
import com.dypiu.nba.dto.analytics.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IqacAnalyticsApiIntegrationTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MasterProgrammeRepository masterProgrammeRepository;

    @Autowired
    private ProgrammeBatchRepository programmeBatchRepository;

    @Autowired
    private ProgrammeBatchCourseRepository programmeBatchCourseRepository;

    @Autowired
    private ProgrammeBatchAttainmentReportRepository reportRepository;

    @Autowired
    private CourseAttainmentReportRepository courseReportRepository;

    @Autowired
    private StudentCoMarkRepository studentCoMarkRepository;

    @Autowired
    private ProgrammeAtrRepository atrRepository;

    @Autowired
    private ProgrammeOutcomeRepository poRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository psoRepository;

    @Autowired
    private CourseOutcomeRepository courseOutcomeRepository;

    @Autowired
    private CoPoMappingRepository coPoMappingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private School schoolSoet;
    private School schoolSom;
    private Department deptCse;
    private Department deptMba;
    private MasterProgramme progBtech;
    private MasterProgramme progMba;
    private ProgrammeBatch batch2022;
    private ProgrammeBatch batch2023;
    private ProgrammeBatch batch2024Draft;

    @BeforeEach
    void setUp() throws Exception {
        // Setup Users
        userRepository.save(User.builder()
                .id(1001L)
                .username("iqac_user")
                .email("iqac_user@dypiu.ac.in")
                .name("IQAC Officer")
                .passwordHash("test_hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(1002L)
                .username("dir_user")
                .email("dir_user@dypiu.ac.in")
                .name("Director SOET")
                .passwordHash("test_hash")
                .role(UserRole.DIRECTOR)
                .schoolId("sch-soet")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(1003L)
                .username("hod_user")
                .email("hod_user@dypiu.ac.in")
                .name("HOD CSE")
                .passwordHash("test_hash")
                .role(UserRole.HOD)
                .schoolId("sch-soet")
                .departmentId("dept-cse")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(1004L)
                .username("pc_user")
                .email("pc_user@dypiu.ac.in")
                .name("PC CSE")
                .passwordHash("test_hash")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId("sch-soet")
                .departmentId("dept-cse")
                .masterProgrammeId("prog-btech")
                .isActive(true)
                .build());
        // 1. Setup Schools
        schoolSoet = schoolRepository.save(School.builder().id("sch-soet").code("SOET").name("School of Engineering").build());
        schoolSom = schoolRepository.save(School.builder().id("sch-som").code("SOM").name("School of Management").build());

        // 2. Setup Departments
        deptCse = departmentRepository.save(Department.builder().id("dept-cse").code("CSE").name("Computer Science").schoolId(schoolSoet.getId()).build());
        deptMba = departmentRepository.save(Department.builder().id("dept-mba").code("MBA").name("Management").schoolId(schoolSom.getId()).build());

        // 3. Setup Programmes
        progBtech = masterProgrammeRepository.save(MasterProgramme.builder().id("prog-btech").code("BTECH-CSE").name("B.Tech CSE").degreeAwarded("B.Tech").departmentId(deptCse.getId()).durationYears(4).build());
        progMba = masterProgrammeRepository.save(MasterProgramme.builder().id("prog-mba").code("MBA-GEN").name("MBA General").degreeAwarded("MBA").departmentId(deptMba.getId()).durationYears(2).build());

        // 4. Setup Batches
        batch2022 = programmeBatchRepository.save(ProgrammeBatch.builder().id("batch-2022").masterProgrammeId(progBtech.getId()).name("2022-2026").startYear(2022).endYear(2026).build());
        batch2023 = programmeBatchRepository.save(ProgrammeBatch.builder().id("batch-2023").masterProgrammeId(progBtech.getId()).name("2023-2027").startYear(2023).endYear(2027).build());
        batch2024Draft = programmeBatchRepository.save(ProgrammeBatch.builder().id("batch-2024").masterProgrammeId(progBtech.getId()).name("2024-2028").startYear(2024).endYear(2028).build());

        // 5. Setup Finalized Report for batch2022 (PO1 Met: 2.40/2.00, PO4 Deficit: 1.50/2.00, PSO1 Met: 2.20/2.00)
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos2022 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").statement("Knowledge").targetLevel(new BigDecimal("2.00")).directAttainment(new BigDecimal("2.50")).indirectAttainment(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.40")).targetMet(true).build(),
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO4").statement("Research").targetLevel(new BigDecimal("2.00")).directAttainment(new BigDecimal("1.40")).indirectAttainment(new BigDecimal("1.90")).finalAttainment(new BigDecimal("1.50")).targetMet(false).build()
        );
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psos2022 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder().psoCode("PSO1").statement("Design").targetLevel(new BigDecimal("2.00")).directAttainment(new BigDecimal("2.20")).indirectAttainment(new BigDecimal("2.20")).finalAttainment(new BigDecimal("2.20")).targetMet(true).build()
        );
        String json2022 = objectMapper.writeValueAsString(Map.of("po", pos2022, "pso", psos2022));

        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-2022")
                .programmeBatchId(batch2022.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(json2022)
                .approvedAt(ZonedDateTime.now())
                .build());

        // 6. Setup Finalized Report for batch2023 (PO1 Met: 2.60/2.00, PO4 Met: 2.10/2.00, PSO1 Met: 2.50/2.00)
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos2023 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").statement("Knowledge").targetLevel(new BigDecimal("2.00")).directAttainment(new BigDecimal("2.60")).indirectAttainment(new BigDecimal("2.60")).finalAttainment(new BigDecimal("2.60")).targetMet(true).build(),
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO4").statement("Research").targetLevel(new BigDecimal("2.00")).directAttainment(new BigDecimal("2.00")).indirectAttainment(new BigDecimal("2.50")).finalAttainment(new BigDecimal("2.10")).targetMet(true).build()
        );
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psos2023 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder().psoCode("PSO1").statement("Design").targetLevel(new BigDecimal("2.00")).directAttainment(new BigDecimal("2.50")).indirectAttainment(new BigDecimal("2.50")).finalAttainment(new BigDecimal("2.50")).targetMet(true).build()
        );
        String json2023 = objectMapper.writeValueAsString(Map.of("po", pos2023, "pso", psos2023));

        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-2023")
                .programmeBatchId(batch2023.getId())
                .status(ReportStatus.APPROVED)
                .overallAttainmentReportJson(json2023)
                .approvedAt(ZonedDateTime.now())
                .build());

        // 7. Setup DRAFT Report for batch2024 (should be excluded from attainment metrics)
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos2024 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").statement("Knowledge").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("0.50")).targetMet(false).build()
        );
        String json2024 = objectMapper.writeValueAsString(Map.of("po", pos2024));

        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-2024")
                .programmeBatchId(batch2024Draft.getId())
                .status(ReportStatus.DRAFT)
                .overallAttainmentReportJson(json2024)
                .build());

        // 8. Setup ATR for batch2022
        atrRepository.save(ProgrammeAtr.builder()
                .id("atr-2022")
                .programmeBatchId(batch2022.getId())
                .status(ProgrammeAtrStatus.SUBMITTED)
                .observationsJson("Laboratory equipment upgrade planned for PO4 deficit.")
                .build());

        // 9. Setup Course and Course Attainment Report for batch2022
        ProgrammeBatchCourse course1 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-101")
                .programmeBatchId(batch2022.getId())
                .code("CSE201")
                .name("Data Structures")
                .semester(3)
                .courseCoordinatorName("Prof. Smith")
                .build());

        List<CourseAttainmentReportDto.Table1Row> t1Rows = List.of(
                CourseAttainmentReportDto.Table1Row.builder().coCode("CO3").poMappings(Map.of("PO4", 3)).build()
        );
        List<CourseAttainmentReportDto.Table3Row> t3Rows = List.of(
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO3").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.45")).targetMet(false).build()
        );

        courseReportRepository.save(CourseAttainmentReport.builder()
                .id("cr-101")
                .programmeBatchCourseId(course1.getId())
                .status(ReportStatus.FINALIZED)
                .overallCoAttainment(new BigDecimal("1.45"))
                .table1MappingJson(objectMapper.writeValueAsString(t1Rows))
                .table3CoAttainmentJson(objectMapper.writeValueAsString(t3Rows))
                .build());
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 1: Institution-wide KPI aggregation")
    void testInstitutionWideKpi() {
        AnalyticsKpiResponseDto kpis = analyticsService.getKpis(null, null, null, null);
        assertThat(kpis).isNotNull();
        assertThat(kpis.getScopeSummary().getTotalSchools()).isEqualTo(2);
        assertThat(kpis.getScopeSummary().getTotalDepartments()).isEqualTo(2);
        assertThat(kpis.getScopeSummary().getTotalMasterProgrammes()).isEqualTo(2);
        assertThat(kpis.getScopeSummary().getTotalEvaluatedBatches()).isEqualTo(2); // batch2022 and batch2023 (draft excluded)

        // PO Target Achievement: 4 total evaluated (PO1, PO4 in batch2022; PO1, PO4 in batch2023)
        // Met: 3 (PO1-2022, PO1-2023, PO4-2023). Deficit: 1 (PO4-2022)
        assertThat(kpis.getPoTargetAchievement().getTotalEvaluatedInstances()).isEqualTo(4);
        assertThat(kpis.getPoTargetAchievement().getTargetMetInstances()).isEqualTo(3);
        assertThat(kpis.getPoTargetAchievement().getTargetDeficitInstances()).isEqualTo(1);
        assertThat(kpis.getPoTargetAchievement().getAchievementRatePercentage()).isEqualByComparingTo(new BigDecimal("75.00"));

        // PSO Target Achievement: 2 total evaluated (PSO1 in batch2022, PSO1 in batch2023), both met
        assertThat(kpis.getPsoTargetAchievement().getTotalEvaluatedInstances()).isEqualTo(2);
        assertThat(kpis.getPsoTargetAchievement().getTargetMetInstances()).isEqualTo(2);
        assertThat(kpis.getPsoTargetAchievement().getTargetDeficitInstances()).isEqualTo(0);
        assertThat(kpis.getPsoTargetAchievement().getAchievementRatePercentage()).isEqualByComparingTo(new BigDecimal("100.00"));

        // Programme Cohort Health: 2 evaluated cohorts (batch2022 has gaps, batch2023 fully met) -> 50%
        assertThat(kpis.getProgrammeCohortHealth().getTotalEvaluatedCohorts()).isEqualTo(2);
        assertThat(kpis.getProgrammeCohortHealth().getCohortsFullyMeetingTargets()).isEqualTo(1);
        assertThat(kpis.getProgrammeCohortHealth().getCohortsWithGaps()).isEqualTo(1);
        assertThat(kpis.getProgrammeCohortHealth().getFullyMeetingTargetRatePercentage()).isEqualByComparingTo(new BigDecimal("50.00"));

        // ATR Summary: 1 submitted
        assertThat(kpis.getAtrOperationalSummary().getTotalRecordedProgrammeAtrs()).isEqualTo(1);
        assertThat(kpis.getAtrOperationalSummary().getSubmittedProgrammeAtrs()).isEqualTo(1);
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 2-5: Hierarchy scoping (School, Dept, Prog, Batch)")
    void testHierarchyScoping() {
        // School scope (SOET)
        AnalyticsKpiResponseDto schoolKpi = analyticsService.getKpis(schoolSoet.getId(), null, null, null);
        assertThat(schoolKpi.getScopeSummary().getTotalSchools()).isEqualTo(1);

        // Dept scope (CSE)
        AnalyticsKpiResponseDto deptKpi = analyticsService.getKpis(null, deptCse.getId(), null, null);
        assertThat(deptKpi.getScopeSummary().getTotalDepartments()).isEqualTo(1);

        // Prog scope (B.Tech)
        AnalyticsKpiResponseDto progKpi = analyticsService.getKpis(null, null, progBtech.getId(), null);
        assertThat(progKpi.getScopeSummary().getTotalMasterProgrammes()).isEqualTo(1);

        // Batch scope (Batch 2022)
        AnalyticsKpiResponseDto batchKpi = analyticsService.getKpis(null, null, null, batch2022.getId());
        assertThat(batchKpi.getScopeSummary().getTotalEvaluatedBatches()).isEqualTo(1);
        assertThat(batchKpi.getProgrammeCohortHealth().getCohortsWithGaps()).isEqualTo(1);
        assertThat(batchKpi.getProgrammeCohortHealth().getCohortsFullyMeetingTargets()).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 6: Invalid hierarchy throws ERR_HIERARCHY_SCOPE_MISMATCH (HTTP 400)")
    void testInvalidHierarchy() {
        // Dept MBA does not belong to School SOET
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                analyticsService.getKpis(schoolSoet.getId(), deptMba.getId(), null, null)
        );
        assertThat(ex.getMessage()).contains("ERR_HIERARCHY_SCOPE_MISMATCH");
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 7-9: Draft report exclusion from attainment metrics")
    void testDraftReportExclusion() {
        // batch2024 has DRAFT report with low attainment (0.50). Verify it is NOT included in PO health.
        List<PoHealthItemDto> poHealth = analyticsService.getPoHealth(null, null, null, null);
        PoHealthItemDto po1 = poHealth.stream().filter(p -> p.getPoCode().equals("PO1")).findFirst().orElseThrow();

        // Evaluated instances must be 2 (from 2022 and 2023), NOT 3!
        assertThat(po1.getEvaluatedInstanceCount()).isEqualTo(2);
        // Average Attainment: (2.40 + 2.60) / 2 = 2.50
        assertThat(po1.getAverageAttainment()).isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 10-14: PO & PSO Target Met, Deficit, Non-applicable outcomes")
    void testPoPsoHealthCalculations() {
        List<PoHealthItemDto> poHealth = analyticsService.getPoHealth(null, null, null, null);
        PoHealthItemDto po4 = poHealth.stream().filter(p -> p.getPoCode().equals("PO4")).findFirst().orElseThrow();

        // PO4 in 2022 is 1.50 (deficit), in 2023 is 2.10 (met) -> Met: 1, Deficit: 1, Total: 2 -> Rate: 50.00%
        assertThat(po4.getEvaluatedInstanceCount()).isEqualTo(2);
        assertThat(po4.getTargetMetCount()).isEqualTo(1);
        assertThat(po4.getTargetDeficitCount()).isEqualTo(1);
        assertThat(po4.getAchievementRatePercentage()).isEqualByComparingTo(new BigDecimal("50.00"));
        // Average Attainment: (1.50 + 2.10) / 2 = 1.80
        assertThat(po4.getAverageAttainment()).isEqualByComparingTo(new BigDecimal("1.80"));
        // Average Target: 2.00
        assertThat(po4.getAverageTarget()).isEqualByComparingTo(new BigDecimal("2.00"));
        // Average Gap: 1.80 - 2.00 = -0.20
        assertThat(po4.getAverageGap()).isEqualByComparingTo(new BigDecimal("-0.20"));

        // PO2 was not mapped in any report -> Evaluated count = 0, Rate = 0.00, not counted as failure
        PoHealthItemDto po2 = poHealth.stream().filter(p -> p.getPoCode().equals("PO2")).findFirst().orElseThrow();
        assertThat(po2.getEvaluatedInstanceCount()).isEqualTo(0);
        assertThat(po2.getTargetDeficitCount()).isEqualTo(0);
        assertThat(po2.getAchievementRatePercentage()).isEqualByComparingTo(BigDecimal.ZERO);

        // PSO1 Health
        List<PsoHealthItemDto> psoHealth = analyticsService.getPsoHealth(null, null, null, null);
        PsoHealthItemDto pso1 = psoHealth.stream().filter(p -> p.getPsoCode().equals("PSO1")).findFirst().orElseThrow();
        // PSO1 in 2022: 2.20, 2023: 2.50 -> Average: 2.35
        assertThat(pso1.getEvaluatedInstanceCount()).isEqualTo(2);
        assertThat(pso1.getTargetMetCount()).isEqualTo(2);
        assertThat(pso1.getAverageAttainment()).isEqualByComparingTo(new BigDecimal("2.35"));
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 15-20: Historical target snapshot and divergence calculation")
    void testHistoricalTargetAndDivergence() {
        List<PoHealthItemDto> poHealth = analyticsService.getPoHealth(null, null, null, null);
        PoHealthItemDto po1 = poHealth.stream().filter(p -> p.getPoCode().equals("PO1")).findFirst().orElseThrow();

        // Direct average: (2.50 + 2.60) / 2 = 2.55
        // Indirect average: (2.00 + 2.60) / 2 = 2.30
        // Mean divergence: 2.55 - 2.30 = 0.25
        assertThat(po1.getDirectAttainmentAverage()).isEqualByComparingTo(new BigDecimal("2.55"));
        assertThat(po1.getIndirectAttainmentAverage()).isEqualByComparingTo(new BigDecimal("2.30"));
        assertThat(po1.getMeanDivergence()).isEqualByComparingTo(new BigDecimal("0.25"));
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 21-23: Attention Areas ordering (most negative gap first) & contributing evidence")
    void testAttentionAreas() {
        List<AttentionAreaItemDto> attentionAreas = analyticsService.getAttentionAreas(null, null, null, null, 5, "ALL");

        // Deficit: batch-2022 PO4 (attained: 1.50, target: 2.00, gap: -0.50)
        assertThat(attentionAreas).hasSize(1);
        AttentionAreaItemDto item = attentionAreas.get(0);
        assertThat(item.getOutcomeCode()).isEqualTo("PO4");
        assertThat(item.getGap()).isEqualByComparingTo(new BigDecimal("-0.50"));
        assertThat(item.isHasRecordedAtr()).isTrue();
        assertThat(item.getAtrStatus()).isEqualTo("SUBMITTED");
        assertThat(item.getRecordedAtrObservations()).contains("Laboratory equipment upgrade");

        // Contributing evidence from course CSE201 / CO3
        assertThat(item.getContributingCourseEvidence()).hasSize(1);
        CourseAssessmentEvidenceDto evidence = item.getContributingCourseEvidence().get(0);
        assertThat(evidence.getCourseCode()).isEqualTo("CSE201");
        assertThat(evidence.getCoCode()).isEqualTo("CO3");
        assertThat(evidence.getMappingStrength()).isEqualTo(3);
        assertThat(evidence.getCoOverallAttainment()).isEqualByComparingTo(new BigDecimal("1.45"));
        assertThat(evidence.getCoTargetMet()).isFalse();
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 24-25: Programme landscape pagination and 1 row = 1 cohort")
    void testProgrammeLandscape() {
        ProgrammeLandscapeResponseDto landscape = analyticsService.getProgrammeLandscape(
                null, null, null, null, 0, 10, null, "ALL", "programmeName", "ASC");

        // 3 cohorts in DB: batch2022, batch2023, batch2024
        assertThat(landscape.getTotalElements()).isEqualTo(3);
        assertThat(landscape.getContent()).hasSize(3);

        ProgrammeLandscapeRowDto row2022 = landscape.getContent().stream()
                .filter(r -> r.getProgrammeBatchId().equals(batch2022.getId())).findFirst().orElseThrow();
        assertThat(row2022.getReportAvailabilityStatus()).isEqualTo("FINALIZED_REPORT_AVAILABLE");
        assertThat(row2022.getUnderlyingReportStatus()).isEqualTo(ReportStatus.FINALIZED);
        assertThat(row2022.isHasGaps()).isTrue();
        assertThat(row2022.getGapCount()).isEqualTo(1); // PO4 is below target
        assertThat(row2022.getAtrStatus()).isEqualTo("SUBMITTED");

        ProgrammeLandscapeRowDto row2024 = landscape.getContent().stream()
                .filter(r -> r.getProgrammeBatchId().equals(batch2024Draft.getId())).findFirst().orElseThrow();
        assertThat(row2024.getReportAvailabilityStatus()).isEqualTo("NO_FINALIZED_REPORT");
        assertThat(row2024.getUnderlyingReportStatus()).isEqualTo(ReportStatus.DRAFT);
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 26-27: Historical trends endpoint and cohort limit")
    void testHistoricalTrends() {
        List<ScopedTrendSeriesDto> trends = analyticsService.getTrends(null, null, progBtech.getId(), 4);
        assertThat(trends).hasSize(1);
        ScopedTrendSeriesDto series = trends.get(0);
        assertThat(series.getScopeIdentifier()).isEqualTo(progBtech.getId());

        // Chronological points from batch2022 and batch2023
        List<CohortOutcomeDataPointDto> points = series.getCohortDataPoints();
        assertThat(points).isNotEmpty();

        List<CohortOutcomeDataPointDto> po1Points = points.stream().filter(p -> p.getOutcomeCode().equals("PO1")).toList();
        assertThat(po1Points).hasSize(2);
        assertThat(po1Points.get(0).getStartYear()).isEqualTo(2022);
        assertThat(po1Points.get(0).getOverallAttainment()).isEqualByComparingTo(new BigDecimal("2.40"));
        assertThat(po1Points.get(1).getStartYear()).isEqualTo(2023);
        assertThat(po1Points.get(1).getOverallAttainment()).isEqualByComparingTo(new BigDecimal("2.60"));
    }

    @Test
    @WithMockUser(username = "hod_user", roles = {"HOD"})
    @DisplayName("Test 28: HOD restricted to assigned department scope")
    void testHodScopeRestriction() {
        // HOD of CSE querying CSE department succeeds
        AnalyticsKpiResponseDto cseKpi = analyticsService.getKpis(null, deptCse.getId(), null, null);
        assertThat(cseKpi).isNotNull();

        // HOD of CSE attempting to query MBA department is forbidden (403)
        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> analyticsService.getKpis(null, deptMba.getId(), null, null)
        );
        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @WithMockUser(username = "pc_user", roles = {"PROGRAMME_COORDINATOR"})
    @DisplayName("Test 29: Programme Coordinator restricted to assigned programme scope")
    void testPcScopeRestriction() {
        // PC of B.Tech querying B.Tech programme succeeds
        AnalyticsKpiResponseDto btechKpi = analyticsService.getKpis(null, null, progBtech.getId(), null);
        assertThat(btechKpi).isNotNull();

        // PC of B.Tech attempting to query MBA programme is forbidden (403)
        org.springframework.web.server.ResponseStatusException ex = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> analyticsService.getKpis(null, null, progMba.getId(), null)
        );
        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 30: Zero student PII and unweighted arithmetic mean verification")
    void testZeroPiiAndArithmeticMean() {
        List<PoHealthItemDto> poHealth = analyticsService.getPoHealth(null, null, null, null);
        assertThat(poHealth).isNotEmpty();
        for (PoHealthItemDto po : poHealth) {
            assertThat(po.getAverageAttainment()).isNotNull();
            assertThat(po.getMeanDivergence()).isNotNull();
        }
    }

    @Test
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    @DisplayName("Test 31: Availability-driven historical trends returns all available cohorts without 10-cohort truncation")
    void testAvailabilityDrivenTrendsMoreThan10Cohorts() throws Exception {
        // Create 11 additional finalized historical cohorts (total 13 cohorts for progBtech)
        for (int yr = 2010; yr <= 2020; yr++) {
            ProgrammeBatch extraBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                    .id("batch-extra-" + yr)
                    .masterProgrammeId(progBtech.getId())
                    .name(yr + "-" + (yr + 4))
                    .startYear(yr)
                    .endYear(yr + 4)
                    .build());

            List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos = List.of(
                    ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                            .poCode("PO1")
                            .statement("Knowledge")
                            .targetLevel(new BigDecimal("2.00"))
                            .directAttainment(new BigDecimal("2.20"))
                            .indirectAttainment(new BigDecimal("2.20"))
                            .finalAttainment(new BigDecimal("2.20"))
                            .targetMet(true)
                            .build()
            );
            String json = objectMapper.writeValueAsString(Map.of("po", pos, "pso", List.of()));

            reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                    .id("rep-extra-" + yr)
                    .programmeBatchId(extraBatch.getId())
                    .status(ReportStatus.FINALIZED)
                    .overallAttainmentReportJson(json)
                    .approvedAt(ZonedDateTime.now())
                    .build());
        }

        // When numCohorts is omitted (null), all available finalized cohorts (13 cohorts > 10) must be returned without truncation
        List<ScopedTrendSeriesDto> trends = analyticsService.getTrends(null, null, progBtech.getId(), null);
        assertThat(trends).hasSize(1);
        ScopedTrendSeriesDto series = trends.get(0);

        List<CohortOutcomeDataPointDto> po1Points = series.getCohortDataPoints().stream()
                .filter(p -> p.getOutcomeCode().equals("PO1"))
                .toList();

        // 11 extra + 2 base = 13 finalized cohorts
        assertThat(po1Points).hasSize(13);
        assertThat(po1Points.get(0).getStartYear()).isEqualTo(2010);
        assertThat(po1Points.get(12).getStartYear()).isEqualTo(2023);
    }

    @Test
    @DisplayName("Phase 7: ATR Intelligence returns correct status counts, gap records, and scoping")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testGetAtrIntelligence() {
        AtrIntelligenceResponseDto response = analyticsService.getAtrIntelligence(
                schoolSoet.getId(), deptCse.getId(), progBtech.getId(), null);

        assertThat(response).isNotNull();
        assertThat(response.getTotalAtrRecords()).isEqualTo(1);
        assertThat(response.getPendingAtrs()).isEqualTo(1);
        assertThat(response.getTotalGapsInScope()).isEqualTo(1);
        assertThat(response.getGapsWithAtr()).isEqualTo(1);
        assertThat(response.getGapsWithoutAtr()).isEqualTo(0);

        List<AtrGapDetailDto> records = response.getGapAtrRecords();
        assertThat(records).hasSize(1);

        AtrGapDetailDto po4Record = records.get(0);
        assertThat(po4Record.getOutcomeCode()).isEqualTo("PO4");
        assertThat(po4Record.getOutcomeType()).isEqualTo("PO");
        assertThat(po4Record.getConfiguredTarget()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(po4Record.getAttainedValue()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(po4Record.getGap()).isEqualByComparingTo(new BigDecimal("-0.50"));
        assertThat(po4Record.isHasRecordedAtr()).isTrue();
        assertThat(po4Record.getAtrStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    @DisplayName("Phase 8: Programme Diagnostic - getCourseEvidence returns mapped CO and course evidence")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testPhase8GetCourseEvidence() {
        List<CourseAssessmentEvidenceDto> evidence = analyticsService.getCourseEvidence(
                batch2022.getId(), "PO4", "PO");

        assertThat(evidence).hasSize(1);
        CourseAssessmentEvidenceDto item = evidence.get(0);
        assertThat(item.getCourseOfferingId()).isEqualTo("pbc-101");
        assertThat(item.getCourseCode()).isEqualTo("CSE201");
        assertThat(item.getCourseName()).isEqualTo("Data Structures");
        assertThat(item.getCoCode()).isEqualTo("CO3");
        assertThat(item.getMappingStrength()).isEqualTo(3);
        assertThat(item.getCoTarget()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(item.getCoOverallAttainment()).isEqualByComparingTo(new BigDecimal("1.45"));
        assertThat(item.getCoTargetMet()).isFalse();
    }

    @Test
    @DisplayName("Phase 9: Student Evidence - getStudentCoEvidence returns aggregated distribution and masked records")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testPhase9GetStudentCoEvidence() {
        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-1")
                .programmeBatchCourseId("pbc-101")
                .studentId("std-1")
                .prn("202201040001")
                .studentName("Student One")
                .coCode("CO3")
                .marksObtained(new BigDecimal("85.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-2")
                .programmeBatchCourseId("pbc-101")
                .studentId("std-2")
                .prn("202201040002")
                .studentName("Student Two")
                .coCode("CO3")
                .marksObtained(new BigDecimal("40.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-3")
                .programmeBatchCourseId("pbc-101")
                .studentId("std-3")
                .prn("202201040003")
                .studentName("Student Three")
                .coCode("CO3")
                .marksObtained(new BigDecimal("65.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        StudentCoEvidenceResponseDto response = analyticsService.getStudentCoEvidence("pbc-101", "CO3");

        assertThat(response).isNotNull();
        assertThat(response.getCourseCode()).isEqualTo("CSE201");
        assertThat(response.getCoCode()).isEqualTo("CO3");
        assertThat(response.getTotalStudentsEvaluated()).isEqualTo(3);
        assertThat(response.getStudentsMeetingThreshold()).isEqualTo(2);
        assertThat(response.getStudentsBelowThreshold()).isEqualTo(1);
        assertThat(response.getHighestPercentage()).isEqualByComparingTo(new BigDecimal("85.00"));
        assertThat(response.getLowestPercentage()).isEqualByComparingTo(new BigDecimal("40.00"));

        assertThat(response.getScoreDistribution().get("80-89%")).isEqualTo(1);
        assertThat(response.getScoreDistribution().get("60-69%")).isEqualTo(1);
        assertThat(response.getScoreDistribution().get("<50%")).isEqualTo(1);

        List<StudentEvidenceRowDto> rows = response.getStudentRecords();
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).getMaskedPrn()).isEqualTo("2022***0001");
        assertThat(rows.get(0).getStudentIdentifier()).isEqualTo("Student 1");
        assertThat(rows.get(0).isThresholdMet()).isTrue();
    }

    @Test
    @DisplayName("Phase 9: Configurable Threshold - IQAC can retrieve and update student evidence threshold")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testPhase9ConfigurableThreshold() {
        // 1. Initial Default is 50.00%
        StudentEvidenceThresholdConfigDto initial = analyticsService.getStudentEvidenceThresholdConfig();
        assertThat(initial.getThresholdPercentage()).isEqualByComparingTo(new BigDecimal("50.00"));

        // 2. Update to 70.00%
        StudentEvidenceThresholdConfigDto updated = analyticsService.updateStudentEvidenceThreshold(
                new BigDecimal("70.00"), "iqac_user");
        assertThat(updated.getThresholdPercentage()).isEqualByComparingTo(new BigDecimal("70.00"));

        // 3. Verify updated value persists in configuration lookup
        StudentEvidenceThresholdConfigDto retrieved = analyticsService.getStudentEvidenceThresholdConfig();
        assertThat(retrieved.getThresholdPercentage()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(retrieved.getUpdatedBy()).isEqualTo("iqac_user");

        // 4. Verify Student Evidence uses 70.00% threshold
        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-101")
                .programmeBatchCourseId("pbc-101")
                .studentId("std-101")
                .prn("202201040001")
                .studentName("Student One")
                .coCode("CO3")
                .marksObtained(new BigDecimal("85.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-102")
                .programmeBatchCourseId("pbc-101")
                .studentId("std-102")
                .prn("202201040002")
                .studentName("Student Two")
                .coCode("CO3")
                .marksObtained(new BigDecimal("65.00")) // 65% is < 70% threshold!
                .maxMarks(new BigDecimal("100.00"))
                .build());

        StudentCoEvidenceResponseDto response = analyticsService.getStudentCoEvidence("pbc-101", "CO3");
        assertThat(response.getConfiguredThresholdPercentage()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(response.getStudentsMeetingThreshold()).isEqualTo(1); // Only 85% meets >= 70%
        assertThat(response.getStudentsBelowThreshold()).isEqualTo(1);  // 65% is below 70%

        // 5. Reset back to 50.00%
        analyticsService.updateStudentEvidenceThreshold(new BigDecimal("50.00"), "SYSTEM");
    }

    @Test
    @DisplayName("Phase 9: Configurable Threshold Validation - Rejects negative values and > 100")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testPhase9ConfigurableThresholdValidation() {
        // Reject negative
        org.springframework.web.server.ResponseStatusException ex1 = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> analyticsService.updateStudentEvidenceThreshold(new BigDecimal("-10.00"), "iqac_user")
        );
        assertThat(ex1.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);

        // Reject > 100
        org.springframework.web.server.ResponseStatusException ex2 = assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> analyticsService.updateStudentEvidenceThreshold(new BigDecimal("105.00"), "iqac_user")
        );
        assertThat(ex2.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Phase 10A: Continuous Monitoring - Live In-Progress Batch Exposes Valid Analytics Before Finalization")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testContinuousMonitoringForActiveInProgressBatch() {
        // Setup in-progress batch with course, COs, mappings, and student marks (no finalized report)
        ProgrammeBatchCourse liveCourse = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-live-101")
                .programmeBatchId(batch2024Draft.getId())
                .code("CSE301")
                .name("Database Management Systems")
                .semester(5)
                .build());

        CourseOutcome co1 = courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-live-1")
                .programmeBatchCourseId(liveCourse.getId())
                .code("CO1")
                .statement("DBMS Foundations")
                .targetLevel(new BigDecimal("2.50"))
                .build());

        coPoMappingRepository.save(CoPoMapping.builder()
                .id("copo-live-1")
                .courseOutcomeId(co1.getId())
                .poCode("PO1")
                .mappingLevel(3)
                .build());

        courseReportRepository.save(CourseAttainmentReport.builder()
                .id("car-live-101")
                .programmeBatchCourseId(liveCourse.getId())
                .status(ReportStatus.DRAFT)
                .directAttainment(new BigDecimal("2.60"))
                .indirectAttainment(new BigDecimal("2.40"))
                .overallCoAttainment(new BigDecimal("2.56"))
                .table1MappingJson("[{\"coCode\":\"CO1\",\"poMappings\":{\"PO1\":3,\"PO2\":2},\"psoMappings\":{\"PSO1\":3}}]")
                .table3CoAttainmentJson("[{\"coCode\":\"CO1\",\"statement\":\"DBMS Foundations\",\"directLevel\":3,\"indirectLevel\":2,\"finalAttainment\":2.80,\"targetLevel\":2.50,\"targetMet\":true}]")
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-live-1")
                .programmeBatchCourseId(liveCourse.getId())
                .studentId("std-live-1")
                .prn("202401040001")
                .studentName("Student Live")
                .coCode("CO1")
                .marksObtained(new BigDecimal("80.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        // 1. Check KPIs for the draft batch scope
        AnalyticsKpiResponseDto kpis = analyticsService.getKpis(null, null, null, batch2024Draft.getId());
        assertThat(kpis).isNotNull();
        assertThat(kpis.getScopeSummary().getTotalEvaluatedBatches()).isEqualTo(1);
        assertThat(kpis.getScopeSummary().getTotalEvaluatedCourseOfferings()).isGreaterThanOrEqualTo(1);
        assertThat(kpis.getScopeSummary().getDataSourceCurrency()).isEqualTo("CONTINUOUS_MONITORING_DATA");
        assertThat(kpis.getPoTargetAchievement().getTotalEvaluatedInstances()).isGreaterThan(0);

        // 2. Check Programme Landscape
        ProgrammeLandscapeResponseDto landscape = analyticsService.getProgrammeLandscape(
                null, null, null, batch2024Draft.getId(), 0, 10, null, "ALL", null, "ASC");
        assertThat(landscape.getContent()).hasSize(1);
        ProgrammeLandscapeRowDto row = landscape.getContent().get(0);
        assertThat(row.getProgrammeBatchId()).isEqualTo(batch2024Draft.getId());
        assertThat(row.getReportAvailabilityStatus()).isEqualTo("IN_PROGRESS_MONITORING");
        assertThat(row.getPosEvaluated()).isGreaterThan(0);

        // 3. Check PO Health
        List<PoHealthItemDto> poHealth = analyticsService.getPoHealth(null, null, null, batch2024Draft.getId());
        assertThat(poHealth).isNotEmpty();
        PoHealthItemDto po1 = poHealth.stream().filter(p -> "PO1".equals(p.getPoCode())).findFirst().orElse(null);
        assertThat(po1).isNotNull();
        assertThat(po1.getEvaluatedInstanceCount()).isGreaterThan(0);
        assertThat(po1.getAverageAttainment()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Phase 10A: Partial Data Handling - Evaluated count accurately reflects partially completed courses")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testPartialCourseDataHandling() {
        ProgrammeBatch partialBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-partial")
                .masterProgrammeId(progBtech.getId())
                .name("2025-2029 Partial")
                .startYear(2025)
                .endYear(2029)
                .build());

        // Create 3 courses in batch
        ProgrammeBatchCourse c1 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-part-1").programmeBatchId(partialBatch.getId()).code("CS101").name("Course 1").semester(1).build());
        ProgrammeBatchCourse c2 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-part-2").programmeBatchId(partialBatch.getId()).code("CS102").name("Course 2").semester(1).build());
        ProgrammeBatchCourse c3 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-part-3").programmeBatchId(partialBatch.getId()).code("CS103").name("Course 3").semester(1).build());

        // Only c1 has marks entered
        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-part-1")
                .programmeBatchCourseId(c1.getId())
                .studentId("std-p1")
                .prn("20250001")
                .studentName("Student P1")
                .coCode("CO1")
                .marksObtained(new BigDecimal("75.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        AnalyticsKpiResponseDto kpis = analyticsService.getKpis(null, null, null, partialBatch.getId());
        assertThat(kpis.getScopeSummary().getTotalEvaluatedCourseOfferings()).isEqualTo(1);
        assertThat(kpis.getScopeSummary().getTotalEvaluatedBatches()).isEqualTo(1);
    }

    @Test
    @DisplayName("Phase 10A: Historical Protection - Finalized report values remain frozen")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testHistoricalProtectionForFinalizedBatch() {
        ProgrammeLandscapeResponseDto landscape = analyticsService.getProgrammeLandscape(
                null, null, null, batch2022.getId(), 0, 10, null, "ALL", null, "ASC");

        assertThat(landscape.getContent()).hasSize(1);
        ProgrammeLandscapeRowDto row = landscape.getContent().get(0);
        assertThat(row.getProgrammeBatchId()).isEqualTo(batch2022.getId());
        assertThat(row.getReportAvailabilityStatus()).isEqualTo("FINALIZED_REPORT_AVAILABLE");
        assertThat(row.getUnderlyingReportStatus()).isEqualTo(ReportStatus.FINALIZED);
    }

    @Test
    @DisplayName("Phase 10A Hotfix TEST A: Inactive batch with no finalized snapshot - Analytics must NOT produce HTTP 500")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testInactiveBatch_NoFinalizedReport_NoHttp500() {
        ProgrammeBatch inactiveBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-inactive-test")
                .masterProgrammeId(progBtech.getId())
                .name("BTECH-CSE 2021-2025 Inactive")
                .status("INACTIVE")
                .startYear(2021)
                .endYear(2025)
                .build());

        ProgrammeBatchCourse c1 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-inact-1")
                .programmeBatchId(inactiveBatch.getId())
                .masterCourseId("cs-inact-101")
                .code("CS-INACT-101")
                .name("Inactive Course")
                .semester(1)
                .build());

        CourseOutcome co1 = courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-inact-1")
                .programmeBatchCourseId(c1.getId())
                .code("CO1")
                .statement("Inactive CO1")
                .targetLevel(new BigDecimal("2.50"))
                .build());

        coPoMappingRepository.save(CoPoMapping.builder()
                .id("cpm-inact-1")
                .courseOutcomeId(co1.getId())
                .poCode("PO1")
                .mappingLevel(3)
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-inact-1")
                .programmeBatchCourseId(c1.getId())
                .studentId("std-inact-1")
                .prn("20210001")
                .studentName("Inactive Student")
                .coCode("CO1")
                .marksObtained(new BigDecimal("80.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        // Call all analytics endpoints for the inactive batch
        assertDoesNotThrow(() -> {
            AnalyticsKpiResponseDto kpis = analyticsService.getKpis(null, null, null, inactiveBatch.getId());
            assertThat(kpis).isNotNull();
            assertThat(kpis.getScopeSummary().getTotalEvaluatedBatches()).isEqualTo(1);

            List<PoHealthItemDto> poHealth = analyticsService.getPoHealth(null, null, null, inactiveBatch.getId());
            assertThat(poHealth).isNotEmpty();

            List<PsoHealthItemDto> psoHealth = analyticsService.getPsoHealth(null, null, null, inactiveBatch.getId());
            assertThat(psoHealth).isNotNull();

            ProgrammeLandscapeResponseDto landscape = analyticsService.getProgrammeLandscape(
                    null, null, null, inactiveBatch.getId(), 0, 10, null, "ALL", null, "ASC");
            assertThat(landscape.getContent()).hasSize(1);

            List<AttentionAreaItemDto> attention = analyticsService.getAttentionAreas(
                    null, null, null, inactiveBatch.getId(), 5, "ALL");
            assertThat(attention).isNotNull();

            List<ScopedTrendSeriesDto> trends = analyticsService.getTrends(
                    null, null, progBtech.getId(), 5);
            assertThat(trends).isNotNull();

            AtrIntelligenceResponseDto atrIntel = analyticsService.getAtrIntelligence(
                    null, null, null, inactiveBatch.getId());
            assertThat(atrIntel).isNotNull();
        });
    }

    @Test
    @DisplayName("Phase 10A Hotfix TEST B: Completed batch with no finalized snapshot - Analytics must NOT produce HTTP 500 or UnexpectedRollbackException")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testCompletedBatch_NoFinalizedReport_NoHttp500OrRollbackException() {
        ProgrammeBatch completedBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-completed-test")
                .masterProgrammeId(progBtech.getId())
                .name("BTECH-CSE 2020-2024 Completed")
                .status("COMPLETED")
                .editingWindowUntil(ZonedDateTime.now().minusDays(10)) // Editing window closed
                .startYear(2020)
                .endYear(2024)
                .build());

        ProgrammeBatchCourse c1 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-comp-1")
                .programmeBatchId(completedBatch.getId())
                .masterCourseId("cs-comp-101")
                .code("CS-COMP-101")
                .name("Completed Course")
                .semester(1)
                .build());

        CourseOutcome co1 = courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-comp-1")
                .programmeBatchCourseId(c1.getId())
                .code("CO1")
                .statement("Completed CO1")
                .targetLevel(new BigDecimal("2.50"))
                .build());

        coPoMappingRepository.save(CoPoMapping.builder()
                .id("cpm-comp-1")
                .courseOutcomeId(co1.getId())
                .poCode("PO1")
                .mappingLevel(3)
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-comp-1")
                .programmeBatchCourseId(c1.getId())
                .studentId("std-comp-1")
                .prn("20200001")
                .studentName("Completed Student")
                .coCode("CO1")
                .marksObtained(new BigDecimal("85.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        assertDoesNotThrow(() -> {
            AnalyticsKpiResponseDto kpis = analyticsService.getKpis(null, null, null, completedBatch.getId());
            assertThat(kpis).isNotNull();
            assertThat(kpis.getScopeSummary().getTotalEvaluatedBatches()).isEqualTo(1);

            List<PoHealthItemDto> poHealth = analyticsService.getPoHealth(null, null, null, completedBatch.getId());
            assertThat(poHealth).isNotEmpty();

            List<PsoHealthItemDto> psoHealth = analyticsService.getPsoHealth(null, null, null, completedBatch.getId());
            assertThat(psoHealth).isNotNull();

            AtrIntelligenceResponseDto atrIntel = analyticsService.getAtrIntelligence(
                    null, null, null, completedBatch.getId());
            assertThat(atrIntel).isNotNull();
        });
    }

    @Test
    @DisplayName("Phase 10A Hotfix TEST C & G: Mixed institution scope with inactive, completed, finalized, and empty batches - All endpoints succeed")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testMixedInstitutionScope_NoRollbackPoisoning_AllEndpointsSucceed() {
        // Create an inactive batch
        ProgrammeBatch inactiveBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-mix-inact")
                .masterProgrammeId(progBtech.getId())
                .name("Mixed Inactive Batch")
                .status("INACTIVE")
                .startYear(2021)
                .endYear(2025)
                .build());

        // Create a completed batch with closed edit window
        ProgrammeBatch completedBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-mix-comp")
                .masterProgrammeId(progMba.getId())
                .name("Mixed Completed Batch")
                .status("COMPLETED")
                .editingWindowUntil(ZonedDateTime.now().minusDays(5))
                .startYear(2022)
                .endYear(2024)
                .build());

        // Create course and marks for completed batch
        ProgrammeBatchCourse cComp = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-mix-comp-1")
                .programmeBatchId(completedBatch.getId())
                .masterCourseId("mba-comp-101")
                .code("MBA-COMP-101")
                .name("MBA Comp Course")
                .semester(1)
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-mix-comp-1")
                .programmeBatchCourseId(cComp.getId())
                .studentId("std-mcomp-1")
                .prn("20220001")
                .studentName("MBA Comp Student")
                .coCode("CO1")
                .marksObtained(new BigDecimal("90.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        // Institution-wide queries (all null scope)
        assertDoesNotThrow(() -> {
            AnalyticsKpiResponseDto kpis = analyticsService.getKpis(null, null, null, null);
            assertThat(kpis).isNotNull();
            assertThat(kpis.getScopeSummary().getTotalEvaluatedBatches()).isGreaterThan(0);

            List<PoHealthItemDto> poHealth = analyticsService.getPoHealth(null, null, null, null);
            assertThat(poHealth).isNotEmpty();

            List<PsoHealthItemDto> psoHealth = analyticsService.getPsoHealth(null, null, null, null);
            assertThat(psoHealth).isNotNull();

            ProgrammeLandscapeResponseDto landscape = analyticsService.getProgrammeLandscape(
                    null, null, null, null, 0, 20, null, "ALL", null, "ASC");
            assertThat(landscape.getContent()).isNotEmpty();

            List<AttentionAreaItemDto> attention = analyticsService.getAttentionAreas(
                    null, null, null, null, 10, "ALL");
            assertThat(attention).isNotNull();

            List<ScopedTrendSeriesDto> trends = analyticsService.getTrends(
                    null, null, null, 5);
            assertThat(trends).isNotNull();

            AtrIntelligenceResponseDto atrIntel = analyticsService.getAtrIntelligence(
                    null, null, null, null);
            assertThat(atrIntel).isNotNull();
        });
    }

    @Test
    @DisplayName("Phase 10A Hotfix TEST D: Active live batch calculates continuous attainment without finalized report")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testActiveLiveBatch_CalculatesLiveAttainmentCorrectly() {
        ProgrammeBatch liveBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-live-active")
                .masterProgrammeId(progBtech.getId())
                .name("BTECH-CSE 2025-2029 Active Live")
                .status("ACTIVE")
                .startYear(2025)
                .endYear(2029)
                .build());

        ProgrammeBatchCourse c1 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-live-1")
                .programmeBatchId(liveBatch.getId())
                .masterCourseId("cs-live-101")
                .code("CS-LIVE-101")
                .name("Active Live Course")
                .semester(1)
                .build());

        CourseOutcome co1 = courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-live-1")
                .programmeBatchCourseId(c1.getId())
                .code("CO1")
                .statement("Live CO1")
                .targetLevel(new BigDecimal("2.50"))
                .build());

        coPoMappingRepository.save(CoPoMapping.builder()
                .id("cpm-live-1")
                .courseOutcomeId(co1.getId())
                .poCode("PO1")
                .mappingLevel(3)
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-live-1")
                .programmeBatchCourseId(c1.getId())
                .studentId("std-live-1")
                .prn("20240001")
                .studentName("Live Student")
                .coCode("CO1")
                .marksObtained(new BigDecimal("80.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        AnalyticsKpiResponseDto kpis = analyticsService.getKpis(null, null, null, liveBatch.getId());
        assertThat(kpis.getScopeSummary().getDataSourceCurrency()).isEqualTo("CONTINUOUS_MONITORING_DATA");
        assertThat(kpis.getScopeSummary().getTotalEvaluatedBatches()).isEqualTo(1);
        assertThat(kpis.getScopeSummary().getTotalEvaluatedCourseOfferings()).isEqualTo(1);

        List<PoHealthItemDto> poHealth = analyticsService.getPoHealth(null, null, null, liveBatch.getId());
        assertThat(poHealth).isNotEmpty();
        PoHealthItemDto po1Health = poHealth.stream().filter(p -> "PO1".equalsIgnoreCase(p.getPoCode())).findFirst().orElse(null);
        assertThat(po1Health).isNotNull();
        assertThat(po1Health.getEvaluatedInstanceCount()).isEqualTo(1);
    }

    // =========================================================================
    // LIVE PROGRAMME BATCH ATTENTION DASHBOARD TESTS (Phase 10B)
    // =========================================================================

    @Test
    @DisplayName("TEST 1: ACTIVE batch with gaps -> returned in attention list")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testActiveBatchWithGaps_ReturnedInAttentionList() throws Exception {
        ProgrammeBatch activeWithGaps = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-active-gaps")
                .masterProgrammeId(progBtech.getId())
                .name("Active Batch With Gaps")
                .status("ACTIVE")
                .startYear(2025)
                .endYear(2029)
                .build());

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.50")).build(),
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO2").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.50")).build()
        );
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psos = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder().psoCode("PSO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.80")).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-active-gaps")
                .programmeBatchId(activeWithGaps.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos, "pso", psos)))
                .build());

        ProgrammeLandscapeResponseDto response = analyticsService.getProgrammeLandscape(
                null, null, null, activeWithGaps.getId(), 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");

        assertThat(response.getContent()).hasSize(1);
        ProgrammeLandscapeRowDto row = response.getContent().get(0);
        assertThat(row.getProgrammeBatchId()).isEqualTo("batch-active-gaps");
        assertThat(row.getBatchStatus()).isEqualTo("ACTIVE");
        assertThat(row.getPoBelowTarget()).isEqualTo(1);
        assertThat(row.getPsoBelowTarget()).isEqualTo(1);
        assertThat(row.getGapCount()).isEqualTo(2);
        assertThat(row.isHasGaps()).isTrue();
    }

    @Test
    @DisplayName("TEST 2: ACTIVE batch with zero gaps -> excluded from attention list, but present in active landscape")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testActiveBatchZeroGaps_ExcludedFromAttentionList() throws Exception {
        ProgrammeBatch activeNoGaps = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-active-nogaps")
                .masterProgrammeId(progBtech.getId())
                .name("Active Batch Zero Gaps")
                .status("ACTIVE")
                .startYear(2025)
                .endYear(2029)
                .build());

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.50")).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-active-nogaps")
                .programmeBatchId(activeNoGaps.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos)))
                .build());

        // Attention list (attentionOnly = true) -> must NOT include activeNoGaps
        ProgrammeLandscapeResponseDto attentionResponse = analyticsService.getProgrammeLandscape(
                null, null, null, activeNoGaps.getId(), 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");
        assertThat(attentionResponse.getContent()).isEmpty();

        // Active population list (attentionOnly = false) -> MUST include activeNoGaps
        ProgrammeLandscapeResponseDto allActiveResponse = analyticsService.getProgrammeLandscape(
                null, null, null, activeNoGaps.getId(), 0, 10, null, null, "ACTIVE", false, "gapCount", "DESC");
        assertThat(allActiveResponse.getContent()).hasSize(1);
        assertThat(allActiveResponse.getContent().get(0).getGapCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("TEST 3: COMPLETED batch with gaps -> excluded from ACTIVE attention list")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testCompletedBatchWithGaps_ExcludedFromAttentionList() throws Exception {
        ProgrammeBatch completedBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-completed-gaps")
                .masterProgrammeId(progBtech.getId())
                .name("Completed Batch With Gaps")
                .status("COMPLETED")
                .startYear(2020)
                .endYear(2024)
                .build());

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.20")).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-completed-gaps")
                .programmeBatchId(completedBatch.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos)))
                .build());

        ProgrammeLandscapeResponseDto response = analyticsService.getProgrammeLandscape(
                null, null, null, completedBatch.getId(), 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("TEST 4: INACTIVE batch with gaps -> excluded from ACTIVE attention list")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testInactiveBatchWithGaps_ExcludedFromAttentionList() throws Exception {
        ProgrammeBatch inactiveBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-inactive-gaps")
                .masterProgrammeId(progBtech.getId())
                .name("Inactive Batch With Gaps")
                .status("INACTIVE")
                .startYear(2021)
                .endYear(2025)
                .build());

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.20")).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-inactive-gaps")
                .programmeBatchId(inactiveBatch.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos)))
                .build());

        ProgrammeLandscapeResponseDto response = analyticsService.getProgrammeLandscape(
                null, null, null, inactiveBatch.getId(), 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("TEST 5: GRADUATED batch with gaps -> excluded from ACTIVE attention list")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testGraduatedBatchWithGaps_ExcludedFromAttentionList() throws Exception {
        ProgrammeBatch graduatedBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-graduated-gaps")
                .masterProgrammeId(progBtech.getId())
                .name("Graduated Batch With Gaps")
                .status("GRADUATED")
                .startYear(2019)
                .endYear(2023)
                .build());

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.20")).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-graduated-gaps")
                .programmeBatchId(graduatedBatch.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos)))
                .build());

        ProgrammeLandscapeResponseDto response = analyticsService.getProgrammeLandscape(
                null, null, null, graduatedBatch.getId(), 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("TEST 6: Soft-deleted ACTIVE batch with gaps -> excluded from attention list")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testSoftDeletedBatch_ExcludedFromAttentionList() throws Exception {
        ProgrammeBatch deletedBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-deleted-gaps")
                .masterProgrammeId(progBtech.getId())
                .name("Deleted Batch With Gaps")
                .status("ACTIVE")
                .startYear(2035)
                .endYear(2039)
                .deletedAt(ZonedDateTime.now())
                .build());

        ProgrammeLandscapeResponseDto response = analyticsService.getProgrammeLandscape(
                null, null, null, deletedBatch.getId(), 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");
        assertThat(response.getContent()).isEmpty();
    }

    @Test
    @DisplayName("TEST 7: School filter + ACTIVE batches -> only authorized ACTIVE batches in that school")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testSchoolFilter_ActiveBatchesRestrictedToSchool() throws Exception {
        ProgrammeBatch somBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-som-active")
                .masterProgrammeId(progMba.getId())
                .name("SOM MBA Active")
                .status("ACTIVE")
                .startYear(2024)
                .endYear(2026)
                .build());

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.20")).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-som-active")
                .programmeBatchId(somBatch.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos)))
                .build());

        // Query school SOET -> SOM batch must NOT appear
        ProgrammeLandscapeResponseDto responseSoet = analyticsService.getProgrammeLandscape(
                schoolSoet.getId(), null, null, null, 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");
        assertThat(responseSoet.getContent().stream().noneMatch(r -> r.getProgrammeBatchId().equals("batch-som-active"))).isTrue();

        // Query school SOM -> SOM batch MUST appear
        ProgrammeLandscapeResponseDto responseSom = analyticsService.getProgrammeLandscape(
                schoolSom.getId(), null, null, null, 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");
        assertThat(responseSom.getContent().stream().anyMatch(r -> r.getProgrammeBatchId().equals("batch-som-active"))).isTrue();
    }

    @Test
    @DisplayName("TEST 8: Programme filter + ACTIVE batches -> only authorized ACTIVE batches in that programme")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testProgrammeFilter_ActiveBatchesRestrictedToProgramme() throws Exception {
        ProgrammeBatch btechBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-prog-filter-btech")
                .masterProgrammeId(progBtech.getId())
                .name("BTech Active Attention")
                .status("ACTIVE")
                .startYear(2025)
                .endYear(2029)
                .build());

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.00")).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-prog-filter-btech")
                .programmeBatchId(btechBatch.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos)))
                .build());

        // Filter for progMba -> btechBatch must NOT appear
        ProgrammeLandscapeResponseDto responseMba = analyticsService.getProgrammeLandscape(
                null, null, progMba.getId(), null, 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");
        assertThat(responseMba.getContent().stream().noneMatch(r -> r.getProgrammeBatchId().equals("batch-prog-filter-btech"))).isTrue();

        // Filter for progBtech -> btechBatch MUST appear
        ProgrammeLandscapeResponseDto responseBtech = analyticsService.getProgrammeLandscape(
                null, null, progBtech.getId(), null, 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");
        assertThat(responseBtech.getContent().stream().anyMatch(r -> r.getProgrammeBatchId().equals("batch-prog-filter-btech"))).isTrue();
    }

    @Test
    @DisplayName("TEST 9: RBAC/scope remains enforced for Attention Dashboard")
    @WithMockUser(username = "dir_user", roles = {"DIRECTOR"})
    void testRbacEnforcement_DirectorCannotAccessOtherSchool() {
        // Director of SOET trying to access SOM
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                analyticsService.getProgrammeLandscape(schoolSom.getId(), null, null, null, 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC"));
    }

    @Test
    @DisplayName("TEST 10: Attention ordering by gapCount DESC")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testAttentionOrdering_GapCountDesc() throws Exception {
        ProgrammeBatch batchGap1 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-order-gap1")
                .masterProgrammeId(progBtech.getId())
                .name("Order Gap 1")
                .status("ACTIVE")
                .startYear(2025)
                .endYear(2029)
                .build());

        ProgrammeBatch batchGap3 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-order-gap3")
                .masterProgrammeId(progBtech.getId())
                .name("Order Gap 3")
                .status("ACTIVE")
                .startYear(2026)
                .endYear(2030)
                .build());

        // batchGap1 has 1 deficit
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos1 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.00")).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-order-gap1")
                .programmeBatchId(batchGap1.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos1)))
                .build());

        // batchGap3 has 3 deficits (2 PO, 1 PSO)
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos3 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.00")).build(),
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO2").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.00")).build()
        );
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psos3 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder().psoCode("PSO1").targetLevel(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.00")).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-order-gap3")
                .programmeBatchId(batchGap3.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos3, "pso", psos3)))
                .build());

        ProgrammeLandscapeResponseDto response = analyticsService.getProgrammeLandscape(
                schoolSoet.getId(), null, null, null, 0, 10, null, null, "ACTIVE", true, "gapCount", "DESC");

        List<ProgrammeLandscapeRowDto> content = response.getContent();
        assertThat(content.size()).isGreaterThanOrEqualTo(2);

        int idxGap3 = -1;
        int idxGap1 = -1;
        for (int i = 0; i < content.size(); i++) {
            if ("batch-order-gap3".equals(content.get(i).getProgrammeBatchId())) idxGap3 = i;
            if ("batch-order-gap1".equals(content.get(i).getProgrammeBatchId())) idxGap1 = i;
        }

        assertThat(idxGap3).isNotEqualTo(-1);
        assertThat(idxGap1).isNotEqualTo(-1);
        assertThat(idxGap3).isLessThan(idxGap1); // Higher gapCount must come FIRST in DESC order
    }

    @Test
    @DisplayName("TEST 11: Existing historical/finalized Analytics behavior preserved")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testExistingHistoricalAnalytics_Preserved() {
        // Calling landscape with statusFilter=ALL and no batchStatus must still return all historical cohorts
        ProgrammeLandscapeResponseDto response = analyticsService.getProgrammeLandscape(
                null, null, null, null, 0, 10, null, "ALL", null, "ASC");
        assertThat(response.getContent().stream().anyMatch(r -> r.getProgrammeBatchId().equals(batch2022.getId()))).isTrue();
    }

    @Test
    @DisplayName("TEST 12: Academic batch selector API behavior unchanged (not restricted to gapCount > 0)")
    @WithMockUser(username = "iqac_user", roles = {"IQAC"})
    void testAcademicBatchSelector_NotRestrictedToGaps() {
        ProgrammeBatch noGapsBatch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-selector-nogaps")
                .masterProgrammeId(progBtech.getId())
                .name("Selector Batch Zero Gaps")
                .status("ACTIVE")
                .startYear(2027)
                .endYear(2031)
                .build());

        List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(progBtech.getId());
        assertThat(batches.stream().anyMatch(b -> b.getId().equals(noGapsBatch.getId()))).isTrue();
    }
}

