package com.dypiu.nba.analytics;

import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
import com.dypiu.nba.dto.analytics.BatchOverviewResponseDto;
import com.dypiu.nba.dto.analytics.OutcomeDirectCourseDto;
import com.dypiu.nba.dto.analytics.OutcomeDirectDrilldownResponseDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OutcomeDirectDrilldownApiIntegrationTest {

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
    private ProgrammeBatchCourse c1;
    private ProgrammeBatchCourse c2;
    private ProgrammeBatchCourse c3Zero;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Setup Users for RBAC testing
        userRepository.save(User.builder()
                .id(2001L)
                .username("iqac_drilldown_user")
                .email("iqac_drilldown_user@dypiu.ac.in")
                .name("IQAC Officer")
                .passwordHash("test_hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(2002L)
                .username("hod_cse_user")
                .email("hod_cse_user@dypiu.ac.in")
                .name("HOD CSE")
                .passwordHash("test_hash")
                .role(UserRole.HOD)
                .schoolId("sch-soet-dd")
                .departmentId("dept-cse-dd")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(2003L)
                .username("hod_mba_user")
                .email("hod_mba_user@dypiu.ac.in")
                .name("HOD MBA")
                .passwordHash("test_hash")
                .role(UserRole.HOD)
                .schoolId("sch-som-dd")
                .departmentId("dept-mba-dd")
                .isActive(true)
                .build());

        // 2. Setup Schools & Departments
        schoolSoet = schoolRepository.save(School.builder().id("sch-soet-dd").code("SOET-DD").name("School of Engineering").build());
        schoolSom = schoolRepository.save(School.builder().id("sch-som-dd").code("SOM-DD").name("School of Management").build());

        deptCse = departmentRepository.save(Department.builder().id("dept-cse-dd").code("CSE-DD").name("Computer Science").schoolId(schoolSoet.getId()).build());
        deptMba = departmentRepository.save(Department.builder().id("dept-mba-dd").code("MBA-DD").name("Management").schoolId(schoolSom.getId()).build());

        // 3. Setup Programmes & Batches
        progBtech = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-btech-dd")
                .code("BTECH-DD")
                .name("B.Tech CSE")
                .degreeAwarded("B.Tech")
                .departmentId(deptCse.getId())
                .durationYears(4)
                .build());

        progMba = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-mba-dd")
                .code("MBA-DD")
                .name("MBA")
                .degreeAwarded("MBA")
                .departmentId(deptMba.getId())
                .durationYears(2)
                .build());

        batch2022 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-2022-dd")
                .masterProgrammeId(progBtech.getId())
                .name("2022-2026")
                .coordinatorName("Dr. John Doe")
                .startYear(2022)
                .endYear(2026)
                .status("ACTIVE")
                .build());

        batch2023 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-2023-dd")
                .masterProgrammeId(progBtech.getId())
                .name("2023-2027")
                .coordinatorName("Dr. Jane Smith")
                .startYear(2023)
                .endYear(2027)
                .status("ACTIVE")
                .build());

        // 4. Setup PO/PSO definitions
        poRepository.save(ProgrammeOutcome.builder()
                .id("po-dd-1")
                .programmeBatchId(batch2022.getId())
                .code("PO1")
                .statement("Engineering Knowledge: Apply math and science")
                .target(new BigDecimal("2.00"))
                .build());

        poRepository.save(ProgrammeOutcome.builder()
                .id("po-dd-4")
                .programmeBatchId(batch2022.getId())
                .code("PO4")
                .statement("Conduct investigations of complex problems")
                .target(new BigDecimal("2.00"))
                .build());

        poRepository.save(ProgrammeOutcome.builder()
                .id("po-dd-12")
                .programmeBatchId(batch2022.getId())
                .code("PO12")
                .statement("Life-long learning")
                .target(new BigDecimal("2.00"))
                .build());

        psoRepository.save(ProgrammeSpecificOutcome.builder()
                .id("pso-dd-1")
                .programmeBatchId(batch2022.getId())
                .code("PSO1")
                .statement("Software Development & System Design")
                .target(new BigDecimal("2.00"))
                .build());

        // 5. Setup Courses for batch2022
        c1 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-dd-c1")
                .programmeBatchId(batch2022.getId())
                .code("CS301")
                .name("Data Structures and Algorithms")
                .semester(3)
                .courseCoordinatorName("Prof. Ramesh Kumar")
                .status("ACTIVE")
                .build());

        c2 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-dd-c2")
                .programmeBatchId(batch2022.getId())
                .code("CS302")
                .name("Operating Systems")
                .semester(4)
                .courseCoordinatorName("Prof. Sunita Patil")
                .status("ACTIVE")
                .build());

        c3Zero = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-dd-c3")
                .programmeBatchId(batch2022.getId())
                .code("CS303")
                .name("Non-Contributing Elective")
                .semester(5)
                .courseCoordinatorName("Prof. Amit Sharma")
                .status("ACTIVE")
                .build());

        // Course reports with overallCoAttainment
        courseReportRepository.save(CourseAttainmentReport.builder()
                .id("car-dd-1")
                .programmeBatchCourseId(c1.getId())
                .overallCoAttainment(new BigDecimal("2.10"))
                .directAttainment(new BigDecimal("2.10"))
                .indirectAttainment(new BigDecimal("2.10"))
                .status(ReportStatus.FINALIZED)
                .build());

        courseReportRepository.save(CourseAttainmentReport.builder()
                .id("car-dd-2")
                .programmeBatchCourseId(c2.getId())
                .overallCoAttainment(new BigDecimal("1.80"))
                .directAttainment(new BigDecimal("1.80"))
                .indirectAttainment(new BigDecimal("1.80"))
                .status(ReportStatus.FINALIZED)
                .build());

        courseReportRepository.save(CourseAttainmentReport.builder()
                .id("car-dd-3")
                .programmeBatchCourseId(c3Zero.getId())
                .overallCoAttainment(new BigDecimal("0.00"))
                .directAttainment(new BigDecimal("0.00"))
                .indirectAttainment(new BigDecimal("0.00"))
                .status(ReportStatus.FINALIZED)
                .build());

        // 6. Setup Finalized Attainment Report for batch2022
        // c1 contributes to PO1 (val=1.63, map=2.33) and PSO1 (val=2.10, map=3.00)
        // c2 contributes to PO4 (val=1.20, map=2.00), does NOT contribute to PO1
        // c3 contributes 0.00 to PO1
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos2022 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").statement("Engineering Knowledge").targetLevel(new BigDecimal("2.00")).directAttainment(new BigDecimal("1.63")).indirectAttainment(new BigDecimal("2.00")).finalAttainment(new BigDecimal("1.70")).targetMet(false).build(),
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO4").statement("Research").targetLevel(new BigDecimal("2.00")).directAttainment(new BigDecimal("1.20")).indirectAttainment(new BigDecimal("1.50")).finalAttainment(new BigDecimal("1.26")).targetMet(false).build(),
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO12").statement("Life-long learning").targetLevel(new BigDecimal("2.00")).directAttainment(BigDecimal.ZERO).indirectAttainment(BigDecimal.ZERO).finalAttainment(BigDecimal.ZERO).targetMet(false).build()
        );
        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psos2022 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder().psoCode("PSO1").statement("Software Development").targetLevel(new BigDecimal("2.00")).directAttainment(new BigDecimal("2.10")).indirectAttainment(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.08")).targetMet(true).build()
        );

        List<ProgrammeBatchAttainmentReportDto.CourseContributionRow> mapRows2022 = List.of(
                ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                        .programmeBatchCourseId(c1.getId())
                        .courseCode("CS301")
                        .courseName("Data Structures and Algorithms")
                        .semester(3)
                        .resourceName("Prof. Ramesh Kumar")
                        .poValues(Map.of("PO1", new BigDecimal("2.33")))
                        .psoValues(Map.of("PSO1", new BigDecimal("3.00")))
                        .build(),
                ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                        .programmeBatchCourseId(c2.getId())
                        .courseCode("CS302")
                        .courseName("Operating Systems")
                        .semester(4)
                        .resourceName("Prof. Sunita Patil")
                        .poValues(Map.of("PO4", new BigDecimal("2.00")))
                        .psoValues(Collections.emptyMap())
                        .build(),
                ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                        .programmeBatchCourseId(c3Zero.getId())
                        .courseCode("CS303")
                        .courseName("Non-Contributing Elective")
                        .semester(5)
                        .resourceName("Prof. Amit Sharma")
                        .poValues(Map.of("PO1", BigDecimal.ZERO))
                        .psoValues(Collections.emptyMap())
                        .build()
        );

        List<ProgrammeBatchAttainmentReportDto.CourseContributionRow> directRows2022 = List.of(
                ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                        .programmeBatchCourseId(c1.getId())
                        .courseCode("CS301")
                        .courseName("Data Structures and Algorithms")
                        .semester(3)
                        .resourceName("Prof. Ramesh Kumar")
                        .poValues(Map.of("PO1", new BigDecimal("1.63")))
                        .psoValues(Map.of("PSO1", new BigDecimal("2.10")))
                        .build(),
                ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                        .programmeBatchCourseId(c2.getId())
                        .courseCode("CS302")
                        .courseName("Operating Systems")
                        .semester(4)
                        .resourceName("Prof. Sunita Patil")
                        .poValues(Map.of("PO4", new BigDecimal("1.20")))
                        .psoValues(Collections.emptyMap())
                        .build(),
                ProgrammeBatchAttainmentReportDto.CourseContributionRow.builder()
                        .programmeBatchCourseId(c3Zero.getId())
                        .courseCode("CS303")
                        .courseName("Non-Contributing Elective")
                        .semester(5)
                        .resourceName("Prof. Amit Sharma")
                        .poValues(Map.of("PO1", BigDecimal.ZERO))
                        .psoValues(Collections.emptyMap())
                        .build()
        );

        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-2022-dd")
                .programmeBatchId(batch2022.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos2022, "pso", psos2022)))
                .averageMappingReportJson(objectMapper.writeValueAsString(Map.of("courses", mapRows2022)))
                .directAttainmentReportJson(objectMapper.writeValueAsString(Map.of("courses", directRows2022)))
                .approvedAt(ZonedDateTime.now())
                .build());

        // 7. Setup Separate Attainment Report for batch2023 (Isolation test)
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> pos2023 = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder().poCode("PO1").statement("Engineering Knowledge").targetLevel(new BigDecimal("2.20")).directAttainment(new BigDecimal("2.75")).indirectAttainment(new BigDecimal("2.50")).finalAttainment(new BigDecimal("2.70")).targetMet(true).build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-2023-dd")
                .programmeBatchId(batch2023.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", pos2023, "pso", List.of())))
                .averageMappingReportJson(objectMapper.writeValueAsString(Map.of("courses", List.of())))
                .directAttainmentReportJson(objectMapper.writeValueAsString(Map.of("courses", List.of())))
                .approvedAt(ZonedDateTime.now())
                .build());
    }

    // -------------------------------------------------------------------------
    // TEST 1: PO Happy Path
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 1: PO Happy Path - validates header and course list")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_PoHappyPath() {
        OutcomeDirectDrilldownResponseDto result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO1", "PO");

        assertThat(result).isNotNull();
        assertThat(result.getProgrammeBatchId()).isEqualTo(batch2022.getId());
        assertThat(result.getBatchName()).isEqualTo("2022-2026");
        assertThat(result.getOutcomeCode()).isEqualTo("PO1");
        assertThat(result.getOutcomeType()).isEqualTo("PO");
        assertThat(result.getDirectAttainment()).isEqualByComparingTo("1.63");
        assertThat(result.getTarget()).isEqualByComparingTo("2.00");
        assertThat(result.getDirectGap()).isEqualByComparingTo("-0.37");
        assertThat(result.isTargetMet()).isFalse();
        assertThat(result.getContributingCourseCount()).isEqualTo(1);

        assertThat(result.getCourses()).hasSize(1);
        OutcomeDirectCourseDto course = result.getCourses().get(0);
        assertThat(course.getProgrammeBatchCourseId()).isEqualTo(c1.getId());
        assertThat(course.getCourseCode()).isEqualTo("CS301");
        assertThat(course.getCourseName()).isEqualTo("Data Structures and Algorithms");
        assertThat(course.getSemester()).isEqualTo(3);
        assertThat(course.getCourseCoordinator()).isEqualTo("Prof. Ramesh Kumar");
        assertThat(course.getOverallCourseAttainment()).isEqualByComparingTo("2.10");
        assertThat(course.getMappingStrength()).isEqualByComparingTo("2.33");
        assertThat(course.getContribution()).isEqualByComparingTo("1.63");
    }

    // -------------------------------------------------------------------------
    // TEST 2: PSO Happy Path
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 2: PSO Happy Path - validates PSO header and course list")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_PsoHappyPath() {
        OutcomeDirectDrilldownResponseDto result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PSO1", "PSO");

        assertThat(result).isNotNull();
        assertThat(result.getOutcomeCode()).isEqualTo("PSO1");
        assertThat(result.getOutcomeType()).isEqualTo("PSO");
        assertThat(result.getDirectAttainment()).isEqualByComparingTo("2.10");
        assertThat(result.getTarget()).isEqualByComparingTo("2.00");
        assertThat(result.getDirectGap()).isEqualByComparingTo("0.10");
        assertThat(result.isTargetMet()).isTrue();
        assertThat(result.getContributingCourseCount()).isEqualTo(1);

        assertThat(result.getCourses()).hasSize(1);
        OutcomeDirectCourseDto course = result.getCourses().get(0);
        assertThat(course.getCourseCode()).isEqualTo("CS301");
        assertThat(course.getContribution()).isEqualByComparingTo("2.10");
        assertThat(course.getMappingStrength()).isEqualByComparingTo("3.00");
    }

    // -------------------------------------------------------------------------
    // TEST 3: Course Filtering (PO)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 3: Course Filtering - PO1 returns only CS301, PO4 returns only CS302")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_CourseFiltering() {
        OutcomeDirectDrilldownResponseDto po1Result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO1", "PO");
        assertThat(po1Result.getCourses()).extracting(OutcomeDirectCourseDto::getCourseCode).containsExactly("CS301");

        OutcomeDirectDrilldownResponseDto po4Result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO4", "PO");
        assertThat(po4Result.getCourses()).extracting(OutcomeDirectCourseDto::getCourseCode).containsExactly("CS302");
    }

    // -------------------------------------------------------------------------
    // TEST 4: PSO Filtering
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 4: PSO Filtering - returns only courses contributing to PSO1")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_PsoFiltering() {
        OutcomeDirectDrilldownResponseDto result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PSO1", "PSO");

        assertThat(result.getCourses()).extracting(OutcomeDirectCourseDto::getCourseCode).containsExactly("CS301");
    }

    // -------------------------------------------------------------------------
    // TEST 5: Course Contribution Correctness
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 5: Course Contribution Correctness - matches stored authoritative value")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_ContributionCorrectness() {
        OutcomeDirectDrilldownResponseDto result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO1", "PO");

        OutcomeDirectCourseDto c = result.getCourses().get(0);
        // Authoritative stored contribution: 1.63
        assertThat(c.getContribution()).isEqualByComparingTo("1.63");
    }

    // -------------------------------------------------------------------------
    // TEST 6: Mapping Strength Correctness
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 6: Mapping Strength Correctness - matches authoritative mapping value")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_MappingStrengthCorrectness() {
        OutcomeDirectDrilldownResponseDto result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO1", "PO");

        OutcomeDirectCourseDto c = result.getCourses().get(0);
        assertThat(c.getMappingStrength()).isEqualByComparingTo("2.33");
    }

    // -------------------------------------------------------------------------
    // TEST 7: Overall Course Attainment Correctness
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 7: Overall Course Attainment Correctness - matches CourseAttainmentReport")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_OverallCourseAttainmentCorrectness() {
        OutcomeDirectDrilldownResponseDto result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO1", "PO");

        OutcomeDirectCourseDto c = result.getCourses().get(0);
        assertThat(c.getOverallCourseAttainment()).isEqualByComparingTo("2.10");
    }

    // -------------------------------------------------------------------------
    // TEST 8: Zero / Non-contributing Courses Excluded
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 8: Zero/Non-contributing courses - contribution <= 0 does not inflate count")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_ZeroContributionExcluded() {
        OutcomeDirectDrilldownResponseDto result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO1", "PO");

        // CS303 has contribution 0.00 for PO1, so it must be excluded
        assertThat(result.getCourses()).noneMatch(c -> "CS303".equals(c.getCourseCode()));
        assertThat(result.getContributingCourseCount()).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // TEST 9: No Contributing Courses
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 9: No Contributing Courses - safe empty response for PO12")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_NoContributingCourses() {
        OutcomeDirectDrilldownResponseDto result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO12", "PO");

        assertThat(result).isNotNull();
        assertThat(result.getContributingCourseCount()).isEqualTo(0);
        assertThat(result.getCourses()).isEmpty();
        assertThat(result.getDirectAttainment()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // -------------------------------------------------------------------------
    // TEST 10: Direct Gap Correctness
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 10: Direct Gap Correctness - verifies directAttainment minus target")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_DirectGapCorrectness() {
        OutcomeDirectDrilldownResponseDto po4Result = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO4", "PO");

        // PO4: direct=1.20, target=2.00 -> gap = 1.20 - 2.00 = -0.80
        assertThat(po4Result.getDirectAttainment()).isEqualByComparingTo("1.20");
        assertThat(po4Result.getTarget()).isEqualByComparingTo("2.00");
        assertThat(po4Result.getDirectGap()).isEqualByComparingTo("-0.80");
        assertThat(po4Result.isTargetMet()).isFalse();
    }

    // -------------------------------------------------------------------------
    // TEST 11: Invalid Outcome Type
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 11: Invalid Outcome Type - throws BadRequestException")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_InvalidOutcomeType() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                analyticsService.getOutcomeDirectDrilldown(batch2022.getId(), "PO1", "INVALID_TYPE")
        );
        assertThat(ex.getMessage()).contains("Invalid outcomeType");
    }

    // -------------------------------------------------------------------------
    // TEST 12: Unknown Outcome
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 12: Unknown Outcome - throws ResourceNotFoundException")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_UnknownOutcome() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                analyticsService.getOutcomeDirectDrilldown(batch2022.getId(), "PO999", "PO")
        );
        assertThat(ex.getMessage()).contains("Outcome 'PO999' of type 'PO' not found");
    }

    // -------------------------------------------------------------------------
    // TEST 13: Unauthorized Batch Access
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 13: Unauthorized Batch Access - HOD MBA cannot access B.Tech CSE batch")
    @WithMockUser(username = "hod_mba_user", roles = {"HOD"})
    void testOutcomeDirectDrilldown_UnauthorizedBatchAccess() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                analyticsService.getOutcomeDirectDrilldown(batch2022.getId(), "PO1", "PO")
        );
        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }

    // -------------------------------------------------------------------------
    // TEST 14: Different Authorized Batch Isolation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 14: Different Authorized Batch - batch2023 returns isolated data")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_BatchIsolation() {
        OutcomeDirectDrilldownResponseDto b23Result = analyticsService.getOutcomeDirectDrilldown(
                batch2023.getId(), "PO1", "PO");

        assertThat(b23Result.getProgrammeBatchId()).isEqualTo(batch2023.getId());
        assertThat(b23Result.getBatchName()).isEqualTo("2023-2027");
        assertThat(b23Result.getDirectAttainment()).isEqualByComparingTo("2.75");
        assertThat(b23Result.getTarget()).isEqualByComparingTo("2.20");
        assertThat(b23Result.getDirectGap()).isEqualByComparingTo("0.55");
    }

    // -------------------------------------------------------------------------
    // TEST 15: Regression Consistency
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 15: Regression Consistency - directAttainment matches batch overview health DTO")
    @WithMockUser(username = "iqac_drilldown_user", roles = {"IQAC"})
    void testOutcomeDirectDrilldown_RegressionConsistency() {
        OutcomeDirectDrilldownResponseDto drilldown = analyticsService.getOutcomeDirectDrilldown(
                batch2022.getId(), "PO1", "PO");

        BatchOverviewResponseDto overview = analyticsService.getBatchOverview(batch2022.getId());
        BatchOverviewResponseDto.PoHealthDto po1Health = overview.getPoHealth().stream()
                .filter(p -> "PO1".equals(p.getPoCode())).findFirst().orElse(null);

        assertThat(po1Health).isNotNull();
        // Exact regression parity: directAttainment must be identical
        assertThat(drilldown.getDirectAttainment()).isEqualByComparingTo(po1Health.getDirectAttainment());
    }
}
