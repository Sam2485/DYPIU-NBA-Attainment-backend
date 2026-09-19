package com.dypiu.nba.analytics;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.dto.analytics.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AnalyticsService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
public class CourseAndCoAnalyticsApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private ProgrammeOutcomeRepository poRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository psoRepository;

    @Autowired
    private CourseOutcomeRepository courseOutcomeRepository;

    @Autowired
    private CoPoMappingRepository coPoMappingRepository;

    @Autowired
    private CoPsoMappingRepository coPsoMappingRepository;

    @Autowired
    private CourseAttainmentReportRepository courseReportRepository;

    @Autowired
    private CourseAtrRepository courseAtrRepository;

    @Autowired
    private StudentCoMarkRepository studentCoMarkRepository;

    @Autowired
    private AttainmentConfigurationRepository attainmentConfigRepository;

    @Autowired
    private ApprovalRequestRepository approvalRequestRepository;

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
    private ProgrammeBatch batchCse;
    private ProgrammeBatch batchMba;
    private ProgrammeBatchCourse courseCs301;
    private CourseOutcome co1;
    private CourseOutcome co2;
    private CourseOutcome co3;
    private ProgrammeOutcome po1;
    private ProgrammeOutcome po2;
    private ProgrammeOutcome po3;
    private ProgrammeOutcome po4Unmapped;
    private ProgrammeSpecificOutcome pso1;
    private ProgrammeSpecificOutcome pso2Unmapped;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Setup Users
        userRepository.save(User.builder()
                .id(5001L)
                .username("iqac_test_user")
                .email("iqac_test_user@dypiu.ac.in")
                .name("IQAC Officer")
                .passwordHash("test_hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(5002L)
                .username("hod_cse_user")
                .email("hod_cse_user@dypiu.ac.in")
                .name("HOD CSE")
                .passwordHash("test_hash")
                .role(UserRole.HOD)
                .schoolId("sch-soet-test")
                .departmentId("dept-cse-test")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(5003L)
                .username("hod_mba_user")
                .email("hod_mba_user@dypiu.ac.in")
                .name("HOD MBA")
                .passwordHash("test_hash")
                .role(UserRole.HOD)
                .schoolId("sch-som-test")
                .departmentId("dept-mba-test")
                .isActive(true)
                .build());

        User pcUser = userRepository.save(User.builder()
                .username("pc_cse_user")
                .email("pc_cse_user@dypiu.ac.in")
                .name("PC CSE")
                .passwordHash("test_hash")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId("sch-soet-test")
                .departmentId("dept-cse-test")
                .masterProgrammeId("prog-btech-test")
                .isActive(true)
                .build());

        User coordUser = userRepository.save(User.builder()
                .username("coord_cs301_user")
                .email("coordinator@dypiu.ac.in")
                .name("Prof. Ramesh Kumar")
                .passwordHash("test_hash")
                .role(UserRole.FACULTY)
                .schoolId("sch-soet-test")
                .departmentId("dept-cse-test")
                .isActive(true)
                .build());

        User assignedFacultyUser = userRepository.save(User.builder()
                .username("faculty_assigned_user")
                .email("assigned@dypiu.ac.in")
                .name("Prof. Assigned Faculty")
                .passwordHash("test_hash")
                .role(UserRole.FACULTY)
                .schoolId("sch-soet-test")
                .departmentId("dept-cse-test")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .username("faculty_unassigned_user")
                .email("unassigned@dypiu.ac.in")
                .name("Prof. Unassigned Faculty")
                .passwordHash("test_hash")
                .role(UserRole.FACULTY)
                .schoolId("sch-soet-test")
                .departmentId("dept-cse-test")
                .isActive(true)
                .build());

        // 2. Setup Academic Hierarchy
        schoolSoet = schoolRepository.save(School.builder().id("sch-soet-test").code("SOET-T").name("School of Engineering").build());
        schoolSom = schoolRepository.save(School.builder().id("sch-som-test").code("SOM-T").name("School of Management").build());

        deptCse = departmentRepository.save(Department.builder().id("dept-cse-test").code("CSE-T").name("Computer Science").schoolId(schoolSoet.getId()).build());
        deptMba = departmentRepository.save(Department.builder().id("dept-mba-test").code("MBA-T").name("Management").schoolId(schoolSom.getId()).build());

        progBtech = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-btech-test")
                .code("BTECH-CSE-T")
                .name("B.Tech Computer Science and Engineering")
                .degreeAwarded("B.Tech")
                .departmentId(deptCse.getId())
                .durationYears(4)
                .build());

        progMba = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-mba-test")
                .code("MBA-T")
                .name("Master of Business Administration")
                .degreeAwarded("MBA")
                .departmentId(deptMba.getId())
                .durationYears(2)
                .build());

        batchCse = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-cse-test")
                .masterProgrammeId(progBtech.getId())
                .name("2022-2026")
                .startYear(2022)
                .endYear(2026)
                .coordinatorId(pcUser.getId())
                .coordinatorEmail(pcUser.getEmail())
                .coordinatorName(pcUser.getName())
                .status("ACTIVE")
                .build());

        batchMba = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-mba-test")
                .masterProgrammeId(progMba.getId())
                .name("2022-2024")
                .startYear(2022)
                .endYear(2024)
                .coordinatorName("PC MBA")
                .status("ACTIVE")
                .build());

        // Approved allocation request so faculty can access course
        approvalRequestRepository.save(ApprovalRequest.builder()
                .id("app-alloc-btech-test")
                .type(ApprovalType.COURSE_ALLOCATION)
                .title("Course Allocation for " + progBtech.getId())
                .masterProgrammeId(progBtech.getId())
                .resourceId("allocation-" + batchCse.getId() + "-sem-3")
                .programmeBatchId(batchCse.getId())
                .status(ApprovalStatus.APPROVED)
                .submittedBy("hod_cse_user")
                .approvedBy("HOD")
                .build());

        // 3. Setup Course Offering
        courseCs301 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-cs301-test")
                .programmeBatchId(batchCse.getId())
                .code("CS301")
                .name("Data Structures and Algorithms")
                .semester(3)
                .courseCoordinatorId(coordUser.getId())
                .courseCoordinatorName(coordUser.getName())
                .coordinatorEmail(coordUser.getEmail())
                .assignedFaculty(assignedFacultyUser.getEmail())
                .status("ACTIVE")
                .build());

        // 4. Setup POs and PSOs
        po1 = poRepository.save(ProgrammeOutcome.builder()
                .id("po-test-1")
                .programmeBatchId(batchCse.getId())
                .code("PO1")
                .statement("Apply mathematics, science, and engineering fundamentals")
                .target(new BigDecimal("2.00"))
                .build());

        po2 = poRepository.save(ProgrammeOutcome.builder()
                .id("po-test-2")
                .programmeBatchId(batchCse.getId())
                .code("PO2")
                .statement("Identify, formulate, and analyze complex engineering problems")
                .target(new BigDecimal("2.00"))
                .build());

        po3 = poRepository.save(ProgrammeOutcome.builder()
                .id("po-test-3")
                .programmeBatchId(batchCse.getId())
                .code("PO3")
                .statement("Design solutions for complex engineering problems")
                .target(new BigDecimal("2.00"))
                .build());

        po4Unmapped = poRepository.save(ProgrammeOutcome.builder()
                .id("po-test-4")
                .programmeBatchId(batchCse.getId())
                .code("PO4")
                .statement("Conduct investigations of complex problems")
                .target(new BigDecimal("2.00"))
                .build());

        pso1 = psoRepository.save(ProgrammeSpecificOutcome.builder()
                .id("pso-test-1")
                .programmeBatchId(batchCse.getId())
                .code("PSO1")
                .statement("Develop software applications using modern tech stacks")
                .target(new BigDecimal("2.00"))
                .build());

        pso2Unmapped = psoRepository.save(ProgrammeSpecificOutcome.builder()
                .id("pso-test-2")
                .programmeBatchId(batchCse.getId())
                .code("PSO2")
                .statement("Architect cloud-native infrastructure solutions")
                .target(new BigDecimal("2.00"))
                .build());

        // 5. Setup COs
        co1 = courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-test-1")
                .programmeBatchCourseId(courseCs301.getId())
                .code("CO1")
                .statement("Understand basic linear data structures")
                .targetLevel(new BigDecimal("2.00"))
                .build());

        co2 = courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-test-2")
                .programmeBatchCourseId(courseCs301.getId())
                .code("CO2")
                .statement("Implement non-linear data structures: trees and graphs")
                .targetLevel(new BigDecimal("2.00"))
                .build());

        co3 = courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-test-3")
                .programmeBatchCourseId(courseCs301.getId())
                .code("CO3")
                .statement("Analyze algorithm complexity and asymptotic bounds")
                .targetLevel(new BigDecimal("2.00"))
                .build());

        // 6. Setup CO -> PO / PSO Articulation Mappings
        coPoMappingRepository.save(CoPoMapping.builder().id("copo-1").courseOutcomeId(co1.getId()).poCode("PO1").mappingLevel(3).build());
        coPsoMappingRepository.save(CoPsoMapping.builder().id("copso-1").courseOutcomeId(co1.getId()).psoCode("PSO1").mappingLevel(2).build());

        coPoMappingRepository.save(CoPoMapping.builder().id("copo-2").courseOutcomeId(co2.getId()).poCode("PO1").mappingLevel(2).build());
        coPoMappingRepository.save(CoPoMapping.builder().id("copo-3").courseOutcomeId(co2.getId()).poCode("PO2").mappingLevel(3).build());
        coPsoMappingRepository.save(CoPsoMapping.builder().id("copso-2").courseOutcomeId(co2.getId()).psoCode("PSO1").mappingLevel(3).build());

        coPoMappingRepository.save(CoPoMapping.builder().id("copo-4").courseOutcomeId(co3.getId()).poCode("PO2").mappingLevel(2).build());
        coPoMappingRepository.save(CoPoMapping.builder().id("copo-5").courseOutcomeId(co3.getId()).poCode("PO3").mappingLevel(3).build());

        // 7. Setup Attainment Configuration (80/20 weights, 60/60 thresholds)
        attainmentConfigRepository.save(AttainmentConfiguration.builder()
                .id("config-" + courseCs301.getId())
                .programmeBatchCourseId(courseCs301.getId())
                .directWeight(new BigDecimal("80.00"))
                .indirectWeight(new BigDecimal("20.00"))
                .directThreshold(new BigDecimal("60.00"))
                .indirectThreshold(new BigDecimal("60.00"))
                .status(AttainmentConfigStatus.APPROVED)
                .build());

        // 8. Setup Authoritative Course Attainment Report
        List<CourseAttainmentReportDto.Table1Row> t1Rows = List.of(
                CourseAttainmentReportDto.Table1Row.builder().coCode("CO1").poMappings(Map.of("PO1", 3)).psoMappings(Map.of("PSO1", 2)).build(),
                CourseAttainmentReportDto.Table1Row.builder().coCode("CO2").poMappings(Map.of("PO1", 2, "PO2", 3)).psoMappings(Map.of("PSO1", 3)).build(),
                CourseAttainmentReportDto.Table1Row.builder().coCode("CO3").poMappings(Map.of("PO2", 2, "PO3", 3)).psoMappings(Collections.emptyMap()).build()
        );

        List<CourseAttainmentReportDto.Table2PoRow> t2PoRows = List.of(
                CourseAttainmentReportDto.Table2PoRow.builder().poCode("PO1").averageMapping(new BigDecimal("2.50")).directContribution(new BigDecimal("1.79")).build(),
                CourseAttainmentReportDto.Table2PoRow.builder().poCode("PO2").averageMapping(new BigDecimal("2.50")).directContribution(new BigDecimal("1.79")).build(),
                CourseAttainmentReportDto.Table2PoRow.builder().poCode("PO3").averageMapping(new BigDecimal("3.00")).directContribution(new BigDecimal("2.15")).build()
        );

        List<CourseAttainmentReportDto.Table2PsoRow> t2PsoRows = List.of(
                CourseAttainmentReportDto.Table2PsoRow.builder().psoCode("PSO1").averageMapping(new BigDecimal("2.50")).directContribution(new BigDecimal("1.79")).build()
        );

        List<CourseAttainmentReportDto.Table3Row> t3Rows = List.of(
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO1").statement("Understand basic linear data structures").targetLevel(new BigDecimal("2.00")).directPercentage(new BigDecimal("75.00")).directLevel(2).indirectPercentage(new BigDecimal("70.00")).indirectScore(new BigDecimal("2.00")).indirectLevel(2).finalAttainment(new BigDecimal("2.00")).targetMet(true).observation("Attained smoothly").build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO2").statement("Implement non-linear data structures: trees and graphs").targetLevel(new BigDecimal("2.00")).directPercentage(new BigDecimal("82.00")).directLevel(3).indirectPercentage(new BigDecimal("65.00")).indirectScore(new BigDecimal("2.00")).indirectLevel(2).finalAttainment(new BigDecimal("2.80")).targetMet(true).observation("Good performance").build(),
                CourseAttainmentReportDto.Table3Row.builder().coCode("CO3").statement("Analyze algorithm complexity and asymptotic bounds").targetLevel(new BigDecimal("2.00")).directPercentage(new BigDecimal("55.00")).directLevel(1).indirectPercentage(new BigDecimal("60.00")).indirectScore(new BigDecimal("1.00")).indirectLevel(1).finalAttainment(new BigDecimal("1.00")).targetMet(false).observation("Complexity theory requires more practice sessions").build()
        );

        courseReportRepository.save(CourseAttainmentReport.builder()
                .id("car-cs301-test")
                .programmeBatchCourseId(courseCs301.getId())
                .status(ReportStatus.FINALIZED)
                .overallCoAttainment(new BigDecimal("2.15"))
                .directAttainment(new BigDecimal("2.20"))
                .indirectAttainment(new BigDecimal("1.95"))
                .table1MappingJson(objectMapper.writeValueAsString(t1Rows))
                .table2DirectJson(objectMapper.writeValueAsString(Map.of("po", t2PoRows, "pso", t2PsoRows)))
                .table3CoAttainmentJson(objectMapper.writeValueAsString(t3Rows))
                .approvedAt(ZonedDateTime.now())
                .build());

        // 9. Setup Course ATR Context
        courseAtrRepository.save(CourseAtr.builder()
                .id("atr-cs301-test")
                .programmeBatchCourseId(courseCs301.getId())
                .coCode("CO3")
                .targetScore(new BigDecimal("2.00"))
                .actualScore(new BigDecimal("1.00"))
                .pctAchieved(new BigDecimal("50.00"))
                .status(CourseAtrStatus.SUBMITTED)
                .actionsJson("Weekly tutorials and extra assignments scheduled for CO3.")
                .build());

        // 10. Setup Student CO Marks
        studentCoMarkRepository.save(StudentCoMark.builder().id("scm-1").studentId("std-1").programmeBatchCourseId(courseCs301.getId()).prn("220101").studentName("Alice Smith").coCode("CO1").marksObtained(new BigDecimal("80.00")).maxMarks(new BigDecimal("100.00")).build());
        studentCoMarkRepository.save(StudentCoMark.builder().id("scm-2").studentId("std-2").programmeBatchCourseId(courseCs301.getId()).prn("220102").studentName("Bob Jones").coCode("CO1").marksObtained(new BigDecimal("70.00")).maxMarks(new BigDecimal("100.00")).build());
        studentCoMarkRepository.save(StudentCoMark.builder().id("scm-3").studentId("std-3").programmeBatchCourseId(courseCs301.getId()).prn("220103").studentName("Charlie Brown").coCode("CO1").marksObtained(new BigDecimal("50.00")).maxMarks(new BigDecimal("100.00")).build());
    }

    // =========================================================================
    // SECTION 1: COURSE ANALYTICS TESTS (Test 1 - Test 28)
    // =========================================================================

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 1: Valid course offering + selected PO (outcomeCode=PO1, outcomeType=PO)")
    void test1_ValidCourse_SelectedPo() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId())
                        .param("outcomeCode", "PO1")
                        .param("outcomeType", "PO")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseCode").value("CS301"))
                .andExpect(jsonPath("$.data.courseName").value("Data Structures and Algorithms"))
                .andExpect(jsonPath("$.data.programmeBatchId").value(batchCse.getId()))
                .andExpect(jsonPath("$.data.schoolName").value("School of Engineering"))
                .andExpect(jsonPath("$.data.departmentName").value("Computer Science"))
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeCode").value("PO1"))
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeType").value("PO"))
                .andExpect(jsonPath("$.data.selectedOutcome.mappingStrength").value(2.50))
                .andExpect(jsonPath("$.data.selectedOutcome.contribution").value(1.79))
                .andExpect(jsonPath("$.data.selectedOutcome.target").value(2.00))
                .andExpect(jsonPath("$.data.selectedOutcome.targetMet").value(false))
                .andExpect(jsonPath("$.data.selectedOutcome.mapped").value(true))
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("Data Structures and Algorithms");
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 2: Valid course offering + selected PSO (outcomeCode=PSO1, outcomeType=PSO)")
    void test2_ValidCourse_SelectedPso() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId())
                        .param("outcomeCode", "PSO1")
                        .param("outcomeType", "PSO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeCode").value("PSO1"))
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeType").value("PSO"))
                .andExpect(jsonPath("$.data.selectedOutcome.mappingStrength").value(2.50))
                .andExpect(jsonPath("$.data.selectedOutcome.contribution").value(1.79))
                .andExpect(jsonPath("$.data.selectedOutcome.mapped").value(true));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 3: Selected mapping strength and contribution match Table 2 authoritative values")
    void test3_SelectedMappingAndContributionFormula() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO3", "PO");
        assertThat(res.getSelectedOutcome()).isNotNull();
        assertThat(res.getSelectedOutcome().getMappingStrength()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(res.getSelectedOutcome().getContribution()).isEqualByComparingTo(new BigDecimal("2.15"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 4: Target & targetMet verification (contribution >= target -> true, else -> false)")
    void test4_TargetAndTargetMetVerification() {
        // PO3: contribution 2.15 >= target 2.00 -> targetMet = true
        CourseAnalyticsResponseDto po3Res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO3", "PO");
        assertThat(po3Res.getSelectedOutcome().getTargetMet()).isTrue();

        // PO1: contribution 1.79 < target 2.00 -> targetMet = false
        CourseAnalyticsResponseDto po1Res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO1", "PO");
        assertThat(po1Res.getSelectedOutcome().getTargetMet()).isFalse();
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 5: Course overall, direct, and indirect attainments from authoritative report")
    void test5_CourseAttainments() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO1", "PO");
        assertThat(res.getOverallCourseAttainment()).isEqualByComparingTo(new BigDecimal("2.15"));
        assertThat(res.getDirectAttainment()).isEqualByComparingTo(new BigDecimal("2.20"));
        assertThat(res.getIndirectAttainment()).isEqualByComparingTo(new BigDecimal("1.95"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 6: Dynamic weights verification (80.00 direct / 20.00 indirect)")
    void test6_DynamicWeights() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO1", "PO");
        assertThat(res.getDirectWeight()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(res.getIndirectWeight()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 7: Direct & Indirect threshold percentages verification")
    void test7_Thresholds() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO1", "PO");
        assertThat(res.getDirectThreshold()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(res.getIndirectThreshold()).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 8: poContributions list contains mapped POs and excludes unmapped PO4")
    void test8_PoContributionsList() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO1", "PO");
        List<OutcomeContributionItemDto> pos = res.getPoContributions();
        assertThat(pos).extracting(OutcomeContributionItemDto::getOutcomeCode)
                .containsExactly("PO1", "PO2", "PO3")
                .doesNotContain("PO4");
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 9: psoContributions list contains mapped PSO1 and excludes unmapped PSO2")
    void test9_PsoContributionsList() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PSO1", "PSO");
        List<OutcomeContributionItemDto> psos = res.getPsoContributions();
        assertThat(psos).extracting(OutcomeContributionItemDto::getOutcomeCode)
                .containsExactly("PSO1")
                .doesNotContain("PSO2");
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 10: All PO/PSO combined outcomes list contains PO1, PO2, PO3, PSO1")
    void test10_AllOutcomesCombinedList() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), null, null);
        List<OutcomeContributionItemDto> outcomes = res.getOutcomes();
        assertThat(outcomes).hasSize(4);
        assertThat(outcomes).extracting(OutcomeContributionItemDto::getOutcomeCode)
                .containsExactly("PO1", "PO2", "PO3", "PSO1");
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 11: Unmapped outcome handling (outcomeCode=PO4: mapped=false, mappingStrength=null, contribution=null)")
    void test11_UnmappedOutcome_Selected() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO4", "PO");
        SelectedOutcomeContributionDto sel = res.getSelectedOutcome();
        assertThat(sel).isNotNull();
        assertThat(sel.getOutcomeCode()).isEqualTo("PO4");
        assertThat(sel.getMapped()).isFalse();
        assertThat(sel.getMappingStrength()).isNull();
        assertThat(sel.getContribution()).isNull();
        assertThat(sel.getTargetMet()).isNull();
        assertThat(sel.getTarget()).isEqualByComparingTo(new BigDecimal("2.00"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 12: Default outcomeType to PO when omitted in query parameters")
    void test12_DefaultOutcomeTypeToPo() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId())
                        .param("outcomeCode", "PO2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeCode").value("PO2"))
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeType").value("PO"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 13: Auto-detect outcomeType PSO when code starts with PSO and outcomeType omitted")
    void test13_AutoDetectOutcomeTypePso() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId())
                        .param("outcomeCode", "PSO1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeCode").value("PSO1"))
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeType").value("PSO"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 14: Case-insensitive outcomeCode and outcomeType handling ('po1', 'po')")
    void test14_CaseInsensitiveOutcomeCodeAndType() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId())
                        .param("outcomeCode", "po1")
                        .param("outcomeType", "po"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeCode").value("PO1"))
                .andExpect(jsonPath("$.data.selectedOutcome.outcomeType").value("PO"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 15: No outcomeCode provided returns selectedOutcome=null with full context")
    void test15_NoOutcomeCodeProvided() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedOutcome").doesNotExist())
                .andExpect(jsonPath("$.data.outcomes").isArray())
                .andExpect(jsonPath("$.data.courseOutcomes").isArray());
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 16: Invalid course offering ID returns 404 ResourceNotFoundException")
    void test16_InvalidCourseId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", "non-existent-course-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 17: Missing programmeBatchCourseId returns 400 BadRequestException")
    void test17_MissingCourseId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 18: Invalid outcomeCode (not in batch PO/PSOs) returns 404 ResourceNotFoundException")
    void test18_InvalidOutcomeCode_ReturnsNotFound() {
        assertThrows(ResourceNotFoundException.class, () ->
                analyticsService.getCourseAnalytics(courseCs301.getId(), "PO99", "PO"));
    }

    @Test
    @WithMockUser(username = "hod_mba_user", roles = {"HOD"})
    @DisplayName("Test 19: Cross-tenant / cross-department access is forbidden (403)")
    void test19_CrossTenantAccess_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "faculty_unassigned_user", roles = {"FACULTY"})
    @DisplayName("Test 20: Faculty scope security - unassigned faculty gets 403 Forbidden")
    void test20_UnassignedFaculty_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "faculty_assigned_user", roles = {"FACULTY"})
    @DisplayName("Test 21: Faculty scope security - assigned faculty gets 200 OK")
    void test21_AssignedFaculty_Success() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCode").value("CS301"));
    }

    @Test
    @WithMockUser(username = "coord_cs301_user", roles = {"FACULTY"})
    @DisplayName("Test 22: Course Coordinator gets 200 OK for their designated course")
    void test22_CourseCoordinator_Success() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCode").value("CS301"));
    }

    @Test
    @WithMockUser(username = "pc_cse_user", roles = {"PROGRAMME_COORDINATOR"})
    @DisplayName("Test 23: Programme Coordinator gets 200 OK for courses in their programme")
    void test23_ProgrammeCoordinator_Success() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCode").value("CS301"));
    }

    @Test
    @WithMockUser(username = "hod_cse_user", roles = {"HOD"})
    @DisplayName("Test 24: HOD of the department gets 200 OK")
    void test24_Hod_Success() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCode").value("CS301"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 25: IQAC Officer gets 200 OK (institution-wide access)")
    void test25_Iqac_Success() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/course-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCode").value("CS301"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 26: Course ATR status context (available=true, status='SUBMITTED')")
    void test26_CourseAtrContext() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO1", "PO");
        assertThat(res.getCourseAtrAvailable()).isTrue();
        assertThat(res.getCourseAtrStatus()).isEqualTo("SUBMITTED");
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 27: Course CO Overview rows with direct/indirect attainment and articulation matrix")
    void test27_CourseCoOverviewItems() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO1", "PO");
        List<CourseCoOverviewItemDto> cos = res.getCourseOutcomes();
        assertThat(cos).hasSize(3);

        CourseCoOverviewItemDto itemCo1 = cos.stream().filter(c -> "CO1".equals(c.getCoCode())).findFirst().orElseThrow();
        assertThat(itemCo1.getDirectLevel()).isEqualTo(2);
        assertThat(itemCo1.getIndirectLevel()).isEqualTo(2);
        assertThat(itemCo1.getOverallAttainment()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(itemCo1.getTargetMet()).isTrue();
        assertThat(itemCo1.getPoMappings()).containsEntry("PO1", 3);
        assertThat(itemCo1.getPsoMappings()).containsEntry("PSO1", 2);

        CourseCoOverviewItemDto itemCo3 = cos.stream().filter(c -> "CO3".equals(c.getCoCode())).findFirst().orElseThrow();
        assertThat(itemCo3.getOverallAttainment()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(itemCo3.getTargetMet()).isFalse();
        assertThat(itemCo3.getObservation()).isEqualTo("Complexity theory requires more practice sessions");
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 28: Course CO overview has selectedOutcomeMapping populated for selected outcome")
    void test28_CourseCoOverview_SelectedOutcomeMapping() {
        CourseAnalyticsResponseDto res = analyticsService.getCourseAnalytics(courseCs301.getId(), "PO1", "PO");
        List<CourseCoOverviewItemDto> cos = res.getCourseOutcomes();

        CourseCoOverviewItemDto itemCo1 = cos.stream().filter(c -> "CO1".equals(c.getCoCode())).findFirst().orElseThrow();
        assertThat(itemCo1.getSelectedOutcomeMapping()).isEqualTo(3);

        CourseCoOverviewItemDto itemCo2 = cos.stream().filter(c -> "CO2".equals(c.getCoCode())).findFirst().orElseThrow();
        assertThat(itemCo2.getSelectedOutcomeMapping()).isEqualTo(2);

        CourseCoOverviewItemDto itemCo3 = cos.stream().filter(c -> "CO3".equals(c.getCoCode())).findFirst().orElseThrow();
        assertThat(itemCo3.getSelectedOutcomeMapping()).isNull(); // CO3 does not map to PO1
    }

    // =========================================================================
    // SECTION 2: CO ANALYTICS TESTS (Test 29 - Test 38)
    // =========================================================================

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 29: Valid course offering + valid CO code (CO1) returns complete CO context")
    void test29_CoAnalytics_ValidCourseAndCo_Success() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/co-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId())
                        .param("coCode", "CO1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseCode").value("CS301"))
                .andExpect(jsonPath("$.data.courseName").value("Data Structures and Algorithms"))
                .andExpect(jsonPath("$.data.coCode").value("CO1"))
                .andExpect(jsonPath("$.data.coStatement").value("Understand basic linear data structures"))
                .andExpect(jsonPath("$.data.target").value(2.00))
                .andExpect(jsonPath("$.data.overallAttainment").value(2.00))
                .andExpect(jsonPath("$.data.targetMet").value(true))
                .andExpect(jsonPath("$.data.directWeight").value(80.00))
                .andExpect(jsonPath("$.data.indirectWeight").value(20.00));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 30: CO target level and targetMet verification (CO1 Met=true, CO3 Met=false)")
    void test30_CoAnalytics_TargetAndTargetMetVerification() {
        CoAnalyticsResponseDto co1Res = analyticsService.getCoAnalytics(courseCs301.getId(), "CO1");
        assertThat(co1Res.getTargetMet()).isTrue();

        CoAnalyticsResponseDto co3Res = analyticsService.getCoAnalytics(courseCs301.getId(), "CO3");
        assertThat(co3Res.getTargetMet()).isFalse();
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 31: CO direct, indirect, and overall attainment values verification")
    void test31_CoAnalytics_Attainments() {
        CoAnalyticsResponseDto res = analyticsService.getCoAnalytics(courseCs301.getId(), "CO1");
        assertThat(res.getDirectAttainment()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(res.getIndirectAttainment()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(res.getOverallAttainment()).isEqualByComparingTo(new BigDecimal("2.00"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 32: Direct evidence summary contains evaluated student count, percentage, and level")
    void test32_CoAnalytics_DirectEvidenceSummary() {
        CoAnalyticsResponseDto res = analyticsService.getCoAnalytics(courseCs301.getId(), "CO1");
        CoDirectEvidenceSummaryDto directSummary = res.getDirectEvidenceSummary();
        assertThat(directSummary).isNotNull();
        assertThat(directSummary.getEvaluatedStudents()).isGreaterThanOrEqualTo(3);
        assertThat(directSummary.getThreshold()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(directSummary.getDirectLevel()).isNotNull();
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 33: Indirect evidence summary contains level distribution, score, and level")
    void test33_CoAnalytics_IndirectEvidenceSummary() {
        CoAnalyticsResponseDto res = analyticsService.getCoAnalytics(courseCs301.getId(), "CO1");
        CoIndirectEvidenceSummaryDto indirectSummary = res.getIndirectEvidenceSummary();
        assertThat(indirectSummary).isNotNull();
        assertThat(indirectSummary.getLevelDistribution()).isNotNull();
        assertThat(indirectSummary.getLevelDistribution()).containsKey("Slight (Level 1)");
        assertThat(indirectSummary.getLevelDistribution()).containsKey("Moderate (Level 2)");
        assertThat(indirectSummary.getLevelDistribution()).containsKey("Substantial (Level 3)");
        assertThat(indirectSummary.getIndirectLevel()).isEqualTo(2);
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 34: CO-to-PO and CO-to-PSO mappings returned in CO analytics detail")
    void test34_CoAnalytics_Mappings() {
        CoAnalyticsResponseDto res = analyticsService.getCoAnalytics(courseCs301.getId(), "CO1");
        assertThat(res.getPoMappings()).containsEntry("PO1", 3);
        assertThat(res.getPsoMappings()).containsEntry("PSO1", 2);
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 35: Invalid CO code returns 404 ResourceNotFoundException")
    void test35_CoAnalytics_InvalidCoCode_ReturnsNotFound() {
        assertThrows(ResourceNotFoundException.class, () ->
                analyticsService.getCoAnalytics(courseCs301.getId(), "CO99"));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 36: Invalid course ID in CO Analytics returns 404 ResourceNotFoundException")
    void test36_CoAnalytics_InvalidCourseId_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/co-analytics")
                        .param("programmeBatchCourseId", "non-existent-course-id")
                        .param("coCode", "CO1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("Test 37: Missing parameters in CO Analytics returns 400 BadRequestException")
    void test37_CoAnalytics_MissingParameters_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/co-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId())
                        .param("coCode", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "faculty_unassigned_user", roles = {"FACULTY"})
    @DisplayName("Test 38: RBAC & Faculty access restrictions for CO Analytics endpoint")
    void test38_CoAnalytics_RbacAndFacultyRestrictions() throws Exception {
        // Unassigned faculty is forbidden (403)
        mockMvc.perform(get("/api/v1/analytics/co-analytics")
                        .param("programmeBatchCourseId", courseCs301.getId())
                        .param("coCode", "CO1"))
                .andExpect(status().isForbidden());
    }
}
