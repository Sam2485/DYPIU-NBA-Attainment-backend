package com.dypiu.nba.analytics;

import com.dypiu.nba.controller.AnalyticsController;
import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.dto.analytics.*;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CourseHistoricalAndComparisonAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsController analyticsController;

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
    private CourseAttainmentReportRepository courseAttainmentReportRepository;

    @Autowired
    private CourseOutcomeRepository courseOutcomeRepository;

    @Autowired
    private AttainmentConfigurationRepository attainmentConfigurationRepository;

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

    private ProgrammeBatch batch2021;
    private ProgrammeBatch batch2022;
    private ProgrammeBatch batch2023;
    private ProgrammeBatch batchDeleted;
    private ProgrammeBatch batchMba2022;

    private ProgrammeBatchCourse course2021;
    private ProgrammeBatchCourse course2022;
    private ProgrammeBatchCourse course2023;
    private ProgrammeBatchCourse courseDeleted;
    private ProgrammeBatchCourse courseMba;

    private User iqacUser;
    private User coordUser;
    private User unassignedUser;
    private User mbaFacultyUser;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Setup Users
        iqacUser = userRepository.save(User.builder()
                .id(9001L)
                .username("iqac_hist_analyst")
                .email("iqac_hist@dypiu.ac.in")
                .name("IQAC Officer")
                .passwordHash("test_hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        coordUser = userRepository.save(User.builder()
                .id(9002L)
                .username("faculty_coord_cs301")
                .email("coord_cs301@dypiu.ac.in")
                .name("Prof. Course Coordinator")
                .passwordHash("test_hash")
                .role(UserRole.FACULTY)
                .schoolId("sch-soet-hist")
                .departmentId("dept-cse-hist")
                .isActive(true)
                .build());

        unassignedUser = userRepository.save(User.builder()
                .id(9003L)
                .username("faculty_unassigned_hist")
                .email("unassigned_hist@dypiu.ac.in")
                .name("Prof. Unassigned")
                .passwordHash("test_hash")
                .role(UserRole.FACULTY)
                .schoolId("sch-soet-hist")
                .departmentId("dept-cse-hist")
                .isActive(true)
                .build());

        mbaFacultyUser = userRepository.save(User.builder()
                .id(9004L)
                .username("faculty_mba_hist")
                .email("mba_faculty@dypiu.ac.in")
                .name("Prof. MBA Instructor")
                .passwordHash("test_hash")
                .role(UserRole.FACULTY)
                .schoolId("sch-som-hist")
                .departmentId("dept-mba-hist")
                .isActive(true)
                .build());

        // 2. Hierarchy
        schoolSoet = schoolRepository.save(School.builder().id("sch-soet-hist").code("SOET-H").name("School of Engineering").build());
        schoolSom = schoolRepository.save(School.builder().id("sch-som-hist").code("SOM-H").name("School of Management").build());

        deptCse = departmentRepository.save(Department.builder().id("dept-cse-hist").code("CSE-H").name("Computer Science").schoolId(schoolSoet.getId()).build());
        deptMba = departmentRepository.save(Department.builder().id("dept-mba-hist").code("MBA-H").name("Management").schoolId(schoolSom.getId()).build());

        progBtech = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-btech-hist")
                .code("BTECH-CSE-H")
                .name("B.Tech Computer Science")
                .degreeAwarded("B.Tech")
                .departmentId(deptCse.getId())
                .durationYears(4)
                .build());

        progMba = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-mba-hist")
                .code("MBA-H")
                .name("Master of Business Administration")
                .degreeAwarded("MBA")
                .departmentId(deptMba.getId())
                .durationYears(2)
                .build());

        // 3. Batches (BTech CSE)
        batch2021 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-cse-2021")
                .masterProgrammeId(progBtech.getId())
                .name("2021-2025")
                .startYear(2021)
                .endYear(2025)
                .status("COMPLETED")
                .build());

        batch2022 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-cse-2022")
                .masterProgrammeId(progBtech.getId())
                .name("2022-2026")
                .startYear(2022)
                .endYear(2026)
                .status("COMPLETED")
                .build());

        batch2023 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-cse-2023")
                .masterProgrammeId(progBtech.getId())
                .name("2023-2027")
                .startYear(2023)
                .endYear(2027)
                .status("ACTIVE")
                .build());

        batchDeleted = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-cse-deleted")
                .masterProgrammeId(progBtech.getId())
                .name("2019-2023")
                .startYear(2019)
                .endYear(2023)
                .status("COMPLETED")
                .deletedAt(ZonedDateTime.now())
                .build());

        batchMba2022 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-mba-2022")
                .masterProgrammeId(progMba.getId())
                .name("2022-2024")
                .startYear(2022)
                .endYear(2024)
                .status("COMPLETED")
                .build());

        // 4. Course Offerings (CS301 Data Structures)
        course2021 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("off-cs301-2021")
                .programmeBatchId(batch2021.getId())
                .code("CS301")
                .name("Data Structures")
                .semester(3)
                .credits(4)
                .courseType("THEORY")
                .status("COMPLETED")
                .build());

        course2022 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("off-cs301-2022")
                .programmeBatchId(batch2022.getId())
                .code("CS301")
                .name("Data Structures")
                .semester(3)
                .credits(4)
                .courseType("THEORY")
                .status("COMPLETED")
                .build());

        course2023 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("off-cs301-2023")
                .programmeBatchId(batch2023.getId())
                .code("CS301")
                .name("Data Structures")
                .semester(3)
                .credits(4)
                .courseType("THEORY")
                .courseCoordinatorId(coordUser.getId())
                .coordinatorEmail(coordUser.getEmail())
                .courseCoordinatorName(coordUser.getName())
                .status("ACTIVE")
                .build());

        courseDeleted = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("off-cs301-deleted")
                .programmeBatchId(batch2022.getId())
                .code("CS301")
                .name("Data Structures")
                .semester(3)
                .deletedAt(ZonedDateTime.now())
                .build());

        courseMba = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("off-mba501-2022")
                .programmeBatchId(batchMba2022.getId())
                .code("MBA501")
                .name("Marketing Management")
                .semester(1)
                .credits(3)
                .courseCoordinatorId(mbaFacultyUser.getId())
                .coordinatorEmail(mbaFacultyUser.getEmail())
                .courseCoordinatorName(mbaFacultyUser.getName())
                .status("COMPLETED")
                .build());

        // 5. Attainment Configurations
        attainmentConfigurationRepository.save(AttainmentConfiguration.builder()
                .id("cfg-" + course2021.getId())
                .programmeBatchCourseId(course2021.getId())
                .directWeight(new BigDecimal("80.00"))
                .indirectWeight(new BigDecimal("20.00"))
                .approvedDirectWeight(new BigDecimal("80.00"))
                .approvedIndirectWeight(new BigDecimal("20.00"))
                .status(AttainmentConfigStatus.APPROVED)
                .build());

        attainmentConfigurationRepository.save(AttainmentConfiguration.builder()
                .id("cfg-" + course2022.getId())
                .programmeBatchCourseId(course2022.getId())
                .directWeight(new BigDecimal("80.00"))
                .indirectWeight(new BigDecimal("20.00"))
                .approvedDirectWeight(new BigDecimal("80.00"))
                .approvedIndirectWeight(new BigDecimal("20.00"))
                .status(AttainmentConfigStatus.APPROVED)
                .build());

        attainmentConfigurationRepository.save(AttainmentConfiguration.builder()
                .id("cfg-" + course2023.getId())
                .programmeBatchCourseId(course2023.getId())
                .directWeight(new BigDecimal("70.00"))
                .indirectWeight(new BigDecimal("30.00"))
                .approvedDirectWeight(new BigDecimal("70.00"))
                .approvedIndirectWeight(new BigDecimal("30.00"))
                .status(AttainmentConfigStatus.APPROVED)
                .build());

        attainmentConfigurationRepository.save(AttainmentConfiguration.builder()
                .id("cfg-" + courseMba.getId())
                .programmeBatchCourseId(courseMba.getId())
                .directWeight(new BigDecimal("80.00"))
                .indirectWeight(new BigDecimal("20.00"))
                .approvedDirectWeight(new BigDecimal("80.00"))
                .approvedIndirectWeight(new BigDecimal("20.00"))
                .status(AttainmentConfigStatus.APPROVED)
                .build());

        // 6. Course Outcomes for 2021 (CO1-CO4)
        for (int i = 1; i <= 4; i++) {
            courseOutcomeRepository.save(CourseOutcome.builder()
                    .id("co-2021-" + i)
                    .programmeBatchCourseId(course2021.getId())
                    .code("CO" + i)
                    .statement("Understand Data Structures Part " + i)
                    .targetLevel(new BigDecimal("2.50"))
                    .build());
        }

        // Course Outcomes for 2022 (CO1-CO5)
        for (int i = 1; i <= 5; i++) {
            courseOutcomeRepository.save(CourseOutcome.builder()
                    .id("co-2022-" + i)
                    .programmeBatchCourseId(course2022.getId())
                    .code("CO" + i)
                    .statement("Understand Data Structures Part " + i)
                    .targetLevel(new BigDecimal("2.50"))
                    .build());
        }

        // Course Outcomes for 2023 (CO1-CO5)
        for (int i = 1; i <= 5; i++) {
            courseOutcomeRepository.save(CourseOutcome.builder()
                    .id("co-2023-" + i)
                    .programmeBatchCourseId(course2023.getId())
                    .code("CO" + i)
                    .statement("Understand Data Structures Part " + i)
                    .targetLevel(new BigDecimal("2.50"))
                    .build());
        }

        // Course Outcomes for MBA (CO1-CO3)
        for (int i = 1; i <= 3; i++) {
            courseOutcomeRepository.save(CourseOutcome.builder()
                    .id("co-mba-" + i)
                    .programmeBatchCourseId(courseMba.getId())
                    .code("CO" + i)
                    .statement("Marketing Strategy Outcome " + i)
                    .targetLevel(new BigDecimal("2.50"))
                    .build());
        }

        // 7. Course Attainment Reports
        // 2021 Report (Finalized)
        List<CourseAttainmentReportDto.Table3Row> t3_2021 = List.of(
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO1").statement("Understand Data Structures Part 1").directLevel(3).indirectScore(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.80")).targetLevel(new BigDecimal("2.50")).targetMet(true).build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO2").statement("Understand Data Structures Part 2").directLevel(2).indirectScore(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.00")).targetLevel(new BigDecimal("2.50")).targetMet(false).build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO3").statement("Understand Data Structures Part 3").directLevel(3).indirectScore(new BigDecimal("2.50")).finalAttainment(new BigDecimal("2.90")).targetLevel(new BigDecimal("2.50")).targetMet(true).build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO4").statement("Understand Data Structures Part 4").directLevel(2).indirectScore(new BigDecimal("1.50")).finalAttainment(new BigDecimal("1.90")).targetLevel(new BigDecimal("2.50")).targetMet(false).build()
        );
        courseAttainmentReportRepository.save(CourseAttainmentReport.builder()
                .id("rep-" + course2021.getId())
                .programmeBatchCourseId(course2021.getId())
                .status(ReportStatus.FINALIZED)
                .overallCoAttainment(new BigDecimal("2.40"))
                .directAttainment(new BigDecimal("2.50"))
                .indirectAttainment(new BigDecimal("2.00"))
                .table3CoAttainmentJson(objectMapper.writeValueAsString(t3_2021))
                .build());

        // 2022 Report (Finalized)
        List<CourseAttainmentReportDto.Table3Row> t3_2022 = List.of(
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO1").statement("Understand Data Structures Part 1").directLevel(3).indirectScore(new BigDecimal("2.50")).finalAttainment(new BigDecimal("2.90")).targetLevel(new BigDecimal("2.50")).targetMet(true).build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO2").statement("Understand Data Structures Part 2").directLevel(3).indirectScore(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.80")).targetLevel(new BigDecimal("2.50")).targetMet(true).build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO3").statement("Understand Data Structures Part 3").directLevel(3).indirectScore(new BigDecimal("2.50")).finalAttainment(new BigDecimal("2.90")).targetLevel(new BigDecimal("2.50")).targetMet(true).build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO4").statement("Understand Data Structures Part 4").directLevel(2).indirectScore(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.00")).targetLevel(new BigDecimal("2.50")).targetMet(false).build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO5").statement("Understand Data Structures Part 5").directLevel(3).indirectScore(new BigDecimal("2.50")).finalAttainment(new BigDecimal("2.90")).targetLevel(new BigDecimal("2.50")).targetMet(true).build()
        );
        courseAttainmentReportRepository.save(CourseAttainmentReport.builder()
                .id("rep-" + course2022.getId())
                .programmeBatchCourseId(course2022.getId())
                .status(ReportStatus.APPROVED)
                .overallCoAttainment(new BigDecimal("2.70"))
                .directAttainment(new BigDecimal("2.80"))
                .indirectAttainment(new BigDecimal("2.30"))
                .table3CoAttainmentJson(objectMapper.writeValueAsString(t3_2022))
                .build());

        // MBA Report (Finalized)
        List<CourseAttainmentReportDto.Table3Row> t3_mba = List.of(
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO1").statement("Marketing Strategy Outcome 1").directLevel(3).indirectScore(new BigDecimal("2.50")).finalAttainment(new BigDecimal("2.90")).targetLevel(new BigDecimal("2.50")).targetMet(true).build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO2").statement("Marketing Strategy Outcome 2").directLevel(2).indirectScore(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.00")).targetLevel(new BigDecimal("2.50")).targetMet(false).build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO3").statement("Marketing Strategy Outcome 3").directLevel(3).indirectScore(new BigDecimal("2.00")).finalAttainment(new BigDecimal("2.80")).targetLevel(new BigDecimal("2.50")).targetMet(true).build()
        );
        courseAttainmentReportRepository.save(CourseAttainmentReport.builder()
                .id("rep-" + courseMba.getId())
                .programmeBatchCourseId(courseMba.getId())
                .status(ReportStatus.FINALIZED)
                .overallCoAttainment(new BigDecimal("2.57"))
                .directAttainment(new BigDecimal("2.67"))
                .indirectAttainment(new BigDecimal("2.17"))
                .table3CoAttainmentJson(objectMapper.writeValueAsString(t3_mba))
                .build());
    }

    // =========================================================================
    // FEATURE 1: COURSE HISTORICAL ATTAINMENT TESTS
    // =========================================================================

    @Test
    @DisplayName("Historical 1: Missing programmeBatchCourseId returns 400 Bad Request")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_MissingCourseId_Throws400() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/historical-course-attainment"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Historical 2: Non-existent programmeBatchCourseId returns 404 Not Found")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_NonExistentCourseId_Throws404() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/historical-course-attainment")
                        .param("programmeBatchCourseId", "off-nonexistent-999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Historical 3: Originating course resolves identity, programme, batches, and outcomes")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_SuccessfulResolution() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/historical-course-attainment")
                        .param("programmeBatchCourseId", course2023.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseCode").value("CS301"))
                .andExpect(jsonPath("$.data.courseName").value("Data Structures"))
                .andExpect(jsonPath("$.data.masterProgrammeId").value(progBtech.getId()))
                .andExpect(jsonPath("$.data.currentProgrammeBatchCourseId").value(course2023.getId()))
                .andExpect(jsonPath("$.data.currentBatchId").value(batch2023.getId()))
                .andExpect(jsonPath("$.data.batches").isArray())
                .andExpect(jsonPath("$.data.batches.length()").value(3)) // 2021, 2022, 2023
                .andExpect(jsonPath("$.data.batches[0].batchName").value("2021-2025"))
                .andExpect(jsonPath("$.data.batches[1].batchName").value("2022-2026"))
                .andExpect(jsonPath("$.data.batches[2].batchName").value("2023-2027"))
                .andExpect(jsonPath("$.data.courseOutcomes").isArray());
    }

    @Test
    @DisplayName("Historical 4: Multi-batch history sorted chronologically by startYear ASC")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_ChronologicalOrdering() {
        HistoricalCourseAttainmentResponseDto response = analyticsService.getHistoricalCourseAttainment(course2023.getId(), null);
        assertNotNull(response);
        assertEquals(3, response.getBatches().size());
        assertEquals(2021, response.getBatches().get(0).getStartYear());
        assertEquals(2022, response.getBatches().get(1).getStartYear());
        assertEquals(2023, response.getBatches().get(2).getStartYear());
    }

    @Test
    @DisplayName("Historical 5: Filter by optional coCode returns only data points for that CO")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_FilterByCoCode_ReturnsFilteredData() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/historical-course-attainment")
                        .param("programmeBatchCourseId", course2023.getId())
                        .param("coCode", "CO2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseOutcomes.length()").value(1))
                .andExpect(jsonPath("$.data.courseOutcomes[0]").value("CO2"))
                .andExpect(jsonPath("$.data.coDataPoints").isArray())
                .andExpect(jsonPath("$.data.coDataPoints[0].coCode").value("CO2"));
    }

    @Test
    @DisplayName("Historical 5b: Direct service call with coCode returns filtered outcomes")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_FilterByCoCode_DirectService() {
        HistoricalCourseAttainmentResponseDto result = analyticsService.getHistoricalCourseAttainment(course2023.getId(), "CO2");
        assertThat(result.getCourseOutcomes()).containsExactly("CO2");
        assertThat(result.getCoDataPoints()).allMatch(dp -> "CO2".equalsIgnoreCase(dp.getCoCode()));
    }

    @Test
    @DisplayName("Historical 6: Filter by non-existent coCode throws 404 Not Found")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_FilterByNonExistentCoCode_Throws404() {
        assertThrows(ResourceNotFoundException.class, () ->
                analyticsService.getHistoricalCourseAttainment(course2023.getId(), "CO99"));
    }

    @Test
    @DisplayName("Historical 7: Active/in-progress batch continuous evaluation runs without DB writes")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_ActiveBatchContinuousEvaluation() {
        long reportCountBefore = courseAttainmentReportRepository.count();
        HistoricalCourseAttainmentResponseDto result = analyticsService.getHistoricalCourseAttainment(course2023.getId(), null);
        long reportCountAfter = courseAttainmentReportRepository.count();

        // Must NOT create reports in DB (pure read-only GET)
        assertEquals(reportCountBefore, reportCountAfter, "Report count must not change on GET");
        assertNotNull(result);
        assertEquals(course2023.getId(), result.getCurrentProgrammeBatchCourseId());
    }

    @Test
    @DisplayName("Historical 8: Concluded batch finalized report metrics parsed accurately")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_ConcludedBatchFinalizedReportParsed() {
        HistoricalCourseAttainmentResponseDto result = analyticsService.getHistoricalCourseAttainment(course2021.getId(), null);
        HistoricalCourseAttainmentResponseDto.HistoricalCourseBatchDto b2021 = result.getBatches().stream()
                .filter(b -> b.getProgrammeBatchId().equals(batch2021.getId()))
                .findFirst().orElseThrow();

        assertEquals(new BigDecimal("2.40"), b2021.getOverallCourseAttainment());
        assertEquals(new BigDecimal("2.50"), b2021.getDirectAttainment());
        assertEquals(new BigDecimal("2.00"), b2021.getIndirectAttainment());
        assertEquals(new BigDecimal("80.00"), b2021.getDirectWeight());
        assertEquals(new BigDecimal("20.00"), b2021.getIndirectWeight());
    }

    @Test
    @DisplayName("Historical 9: RBAC - IQAC can access historical course attainment")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_Rbac_IqacAuthorized() {
        HistoricalCourseAttainmentResponseDto result = analyticsService.getHistoricalCourseAttainment(course2023.getId(), null);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Historical 10: RBAC - Faculty assigned to originating course can access")
    @WithMockUser(username = "faculty_coord_cs301", roles = {"FACULTY"})
    void testHistorical_Rbac_AssignedFacultyAuthorized() {
        HistoricalCourseAttainmentResponseDto result = analyticsService.getHistoricalCourseAttainment(course2023.getId(), null);
        assertNotNull(result);
        assertEquals(course2023.getId(), result.getCurrentProgrammeBatchCourseId());
    }

    @Test
    @DisplayName("Historical 11: RBAC - Faculty NOT assigned to originating course receives 403 Forbidden")
    @WithMockUser(username = "faculty_unassigned_hist", roles = {"FACULTY"})
    void testHistorical_Rbac_UnassignedFacultyForbidden() {
        assertThrows(ResponseStatusException.class, () ->
                analyticsService.getHistoricalCourseAttainment(course2023.getId(), null));
    }

    @Test
    @DisplayName("Historical 12: Soft-deleted course is ignored / 404 when requested directly")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_SoftDeletedCourse_Throws404() {
        assertThrows(ResourceNotFoundException.class, () ->
                analyticsService.getHistoricalCourseAttainment(courseDeleted.getId(), null));
    }

    @Test
    @DisplayName("Historical 13: Soft-deleted batch is excluded from historical trend")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_SoftDeletedBatch_ExcludedFromHistory() {
        HistoricalCourseAttainmentResponseDto result = analyticsService.getHistoricalCourseAttainment(course2023.getId(), null);
        assertThat(result.getBatches()).noneMatch(b -> b.getProgrammeBatchId().equals(batchDeleted.getId()));
    }

    @Test
    @DisplayName("Historical 14: Distinct course outcomes ordered correctly across batches")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_DistinctOutcomesOrdered() {
        HistoricalCourseAttainmentResponseDto result = analyticsService.getHistoricalCourseAttainment(course2023.getId(), null);
        assertThat(result.getCourseOutcomes()).containsExactly("CO1", "CO2", "CO3", "CO4", "CO5");
    }

    @Test
    @DisplayName("Historical 15: Single batch course offering returns 1 batch history")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testHistorical_SingleBatchHistory() {
        HistoricalCourseAttainmentResponseDto result = analyticsService.getHistoricalCourseAttainment(courseMba.getId(), null);
        assertNotNull(result);
        assertEquals(1, result.getBatches().size());
        assertEquals("MBA501", result.getCourseCode());
        assertEquals(courseMba.getId(), result.getCurrentProgrammeBatchCourseId());
    }

    // =========================================================================
    // FEATURE 2: COMPARE COURSE PERFORMANCE TESTS
    // =========================================================================

    @Test
    @DisplayName("Compare 1: Missing course parameters return 400 Bad Request")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_MissingParameters_Throws400() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/compare-courses"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/analytics/compare-courses")
                        .param("programmeBatchCourseId1", course2021.getId()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Compare 2: Comparing course with itself throws 400 Bad Request")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_SelfComparison_Throws400() {
        assertThrows(BadRequestException.class, () ->
                analyticsService.compareCourses(course2021.getId(), course2021.getId()));
    }

    @Test
    @DisplayName("Compare 3: Non-existent course ID throws 404 Not Found")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_NonExistentCourse_Throws404() {
        assertThrows(ResourceNotFoundException.class, () ->
                analyticsService.compareCourses(course2021.getId(), "off-nonexistent-888"));
    }

    @Test
    @DisplayName("Compare 4: Cross-batch comparison of same course produces valid comparison")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_CrossBatchSameCourse() {
        CourseComparisonAnalyticsResponseDto response = analyticsService.compareCourses(course2021.getId(), course2022.getId());

        assertNotNull(response);
        assertEquals("CS301", response.getCourse1().getCourseCode());
        assertEquals("CS301", response.getCourse2().getCourseCode());
        assertEquals("2021-2025", response.getCourse1().getBatchName());
        assertEquals("2022-2026", response.getCourse2().getBatchName());

        // Attainment metrics check
        assertEquals(new BigDecimal("2.40"), response.getCourse1().getOverallCourseAttainment());
        assertEquals(new BigDecimal("2.70"), response.getCourse2().getOverallCourseAttainment());
    }

    @Test
    @DisplayName("Compare 5: Accurate mathematical delta (course1 - course2)")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_MathematicalDelta() {
        CourseComparisonAnalyticsResponseDto response = analyticsService.compareCourses(course2021.getId(), course2022.getId());

        // Check CO1: 2021 has 2.80, 2022 has 2.90 -> delta: 2.80 - 2.90 = -0.10
        CourseComparisonAnalyticsResponseDto.CoComparisonItemDto co1 = response.getCoComparisons().stream()
                .filter(c -> "CO1".equalsIgnoreCase(c.getCoCode()))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("-0.10"), co1.getAttainmentDelta());

        // Check CO2: 2021 has 2.00, 2022 has 2.80 -> delta: 2.00 - 2.80 = -0.80
        CourseComparisonAnalyticsResponseDto.CoComparisonItemDto co2 = response.getCoComparisons().stream()
                .filter(c -> "CO2".equalsIgnoreCase(c.getCoCode()))
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("-0.80"), co2.getAttainmentDelta());
    }

    @Test
    @DisplayName("Compare 6: Asymmetric CO sets gracefully handled (Course 1 has CO1-CO4, Course 2 has CO1-CO5)")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_AsymmetricCoSets() {
        CourseComparisonAnalyticsResponseDto response = analyticsService.compareCourses(course2021.getId(), course2022.getId());

        // All distinct COs present
        assertThat(response.getCourseOutcomes()).containsExactly("CO1", "CO2", "CO3", "CO4", "CO5");

        // CO5 exists only in course 2 (2022)
        CourseComparisonAnalyticsResponseDto.CoComparisonItemDto co5 = response.getCoComparisons().stream()
                .filter(c -> "CO5".equalsIgnoreCase(c.getCoCode()))
                .findFirst().orElseThrow();

        assertNull(co5.getCourse1Metrics(), "Course 1 metrics should be null for missing CO");
        assertNotNull(co5.getCourse2Metrics(), "Course 2 metrics should be present");
        assertEquals(new BigDecimal("2.90"), co5.getCourse2Metrics().getOverallAttainment());
        assertNull(co5.getAttainmentDelta(), "Attainment delta should be null when one side is missing");
    }

    @Test
    @DisplayName("Compare 7: Cross-programme comparison of different courses allowed")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_CrossProgrammeComparison() {
        CourseComparisonAnalyticsResponseDto response = analyticsService.compareCourses(course2022.getId(), courseMba.getId());

        assertNotNull(response);
        assertEquals("CS301", response.getCourse1().getCourseCode());
        assertEquals("MBA501", response.getCourse2().getCourseCode());
        assertEquals(progBtech.getId(), response.getCourse1().getMasterProgrammeId());
        assertEquals(progMba.getId(), response.getCourse2().getMasterProgrammeId());
    }

    @Test
    @DisplayName("Compare 8: Compare active batch course with concluded batch course")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_ActiveWithConcludedCourse() {
        CourseComparisonAnalyticsResponseDto response = analyticsService.compareCourses(course2023.getId(), course2022.getId());

        assertNotNull(response);
        assertEquals("ACTIVE", response.getCourse1().getBatchStatus());
        assertEquals("COMPLETED", response.getCourse2().getBatchStatus());
    }

    @Test
    @DisplayName("Compare 9: RBAC - User authorized for both courses succeeds")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_Rbac_AuthorizedForBoth() {
        CourseComparisonAnalyticsResponseDto response = analyticsService.compareCourses(course2021.getId(), course2022.getId());
        assertNotNull(response);
    }

    @Test
    @DisplayName("Compare 10: RBAC - Faculty authorized for course 1 but NOT course 2 receives 403 Forbidden")
    @WithMockUser(username = "faculty_coord_cs301", roles = {"FACULTY"})
    void testCompare_Rbac_FacultyUnauthorizedForCourse2_Forbidden() {
        // coordUser is assigned to course2023, but NOT to courseMba
        assertThrows(ResponseStatusException.class, () ->
                analyticsService.compareCourses(course2023.getId(), courseMba.getId()));
    }

    @Test
    @DisplayName("Compare 11: RBAC - IQAC authorized across all courses")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_Rbac_IqacAuthorizedAcrossAll() {
        CourseComparisonAnalyticsResponseDto response = analyticsService.compareCourses(course2023.getId(), courseMba.getId());
        assertNotNull(response);
    }

    @Test
    @DisplayName("Compare 12: REST endpoint returns proper JSON structure")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_EndpointJsonStructure() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/compare-courses")
                        .param("programmeBatchCourseId1", course2021.getId())
                        .param("programmeBatchCourseId2", course2022.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.course1.courseCode").value("CS301"))
                .andExpect(jsonPath("$.data.course2.courseCode").value("CS301"))
                .andExpect(jsonPath("$.data.coComparisons").isArray());
    }

    @Test
    @DisplayName("Compare 13: Read-only verification - No records modified or created")
    @WithMockUser(username = "iqac_hist_analyst", roles = {"IQAC"})
    void testCompare_ReadOnlyVerification() {
        long reportsCountBefore = courseAttainmentReportRepository.count();
        long courseCountBefore = programmeBatchCourseRepository.count();

        analyticsService.compareCourses(course2021.getId(), course2022.getId());

        assertEquals(reportsCountBefore, courseAttainmentReportRepository.count());
        assertEquals(courseCountBefore, programmeBatchCourseRepository.count());
    }
}
