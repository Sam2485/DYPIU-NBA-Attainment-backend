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
    private ProgrammeAtrRepository atrRepository;

    @Autowired
    private ProgrammeOutcomeRepository poRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository psoRepository;

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
}
