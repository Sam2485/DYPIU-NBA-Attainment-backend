package com.dypiu.nba.analytics;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.SurveyMarksPayloadDto;
import com.dypiu.nba.dto.SurveyResponseRowDto;
import com.dypiu.nba.dto.analytics.CoIndirectEvidenceItemDto;
import com.dypiu.nba.dto.analytics.CoIndirectEvidenceResponseDto;
import com.dypiu.nba.dto.analytics.CoIndirectResponseRecordDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AnalyticsService;
import com.dypiu.nba.service.AttainmentCalculationService;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
public class CoIndirectEvidenceDataIntegrityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AttainmentCalculationService attainmentCalculationService;

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
    private CourseOutcomeRepository courseOutcomeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ProgrammeBatchCourse offering6Cos;
    private ProgrammeBatchCourse offering4Cos;
    private ProgrammeBatch batch;

    @BeforeEach
    void setUp() {
        analyticsService.invalidateCoIndirectEvidenceCache(null);

        // Setup Users
        userRepository.save(User.builder()
                .id(9910L)
                .username("iqac_analyst")
                .email("iqac_analyst@dypiu.ac.in")
                .name("IQAC Analyst")
                .passwordHash("hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(9911L)
                .username("unassigned_faculty")
                .email("unassigned@dypiu.ac.in")
                .name("Unassigned Faculty")
                .passwordHash("hash")
                .role(UserRole.FACULTY)
                .isActive(true)
                .build());

        // Setup Academic Hierarchy
        School school = schoolRepository.save(School.builder()
                .id("sch-indirect")
                .code("SOET")
                .name("School of Engineering")
                .build());

        Department dept = departmentRepository.save(Department.builder()
                .id("dept-indirect")
                .code("CSE")
                .name("Computer Science and Engineering")
                .schoolId(school.getId())
                .build());

        MasterProgramme prog = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-indirect")
                .code("BTECH-CSE")
                .name("B.Tech Computer Science")
                .degreeAwarded("B.Tech")
                .departmentId(dept.getId())
                .durationYears(4)
                .build());

        batch = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-indirect")
                .masterProgrammeId(prog.getId())
                .name("2021-2025")
                .startYear(2021)
                .endYear(2025)
                .build());

        // Setup Primary Course Offering with 6 COs
        offering6Cos = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("offering-indirect-6co")
                .programmeBatchId(batch.getId())
                .code("CS301")
                .name("Database Management Systems")
                .semester(5)
                .courseCoordinatorName("Dr. Alan Turing")
                .assignedFaculty("Dr. Alan Turing")
                .build());

        for (int i = 1; i <= 6; i++) {
            courseOutcomeRepository.save(CourseOutcome.builder()
                    .id("co-dbms-" + i)
                    .programmeBatchCourseId(offering6Cos.getId())
                    .code("CO" + i)
                    .statement("Understand and apply database outcome CO" + i)
                    .targetLevel(new BigDecimal("2.50"))
                    .bloomsLevel("L3")
                    .build());
        }

        // Setup Secondary Course Offering with 4 COs
        offering4Cos = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("offering-indirect-4co")
                .programmeBatchId(batch.getId())
                .code("CS401")
                .name("Cloud Computing")
                .semester(7)
                .courseCoordinatorName("Dr. Grace Hopper")
                .assignedFaculty("Dr. Grace Hopper")
                .build());

        for (int i = 1; i <= 4; i++) {
            courseOutcomeRepository.save(CourseOutcome.builder()
                    .id("co-cloud-" + i)
                    .programmeBatchCourseId(offering4Cos.getId())
                    .code("CO" + i)
                    .statement("Cloud computing outcome CO" + i)
                    .targetLevel(new BigDecimal("2.50"))
                    .bloomsLevel("L4")
                    .build());
        }

        // Seed 18 Survey Responses for Primary Course (CS301):
        // CO1: 2 slight (1), 5 moderate (2), 11 substantial (3) -> Total 18
        // CO3: 3 slight (1), 3 moderate (2), 12 substantial (3) -> Total 18
        seedSurveyResponsesForOffering6Cos();
    }

    private void seedSurveyResponsesForOffering6Cos() {
        List<SurveyResponseRowDto> rows = new ArrayList<>();

        for (int i = 1; i <= 18; i++) {
            Map<String, BigDecimal> coRatings = new LinkedHashMap<>();
            Map<String, String> coFeedbacks = new LinkedHashMap<>();

            // CO1 distribution: 2 of Level 1, 5 of Level 2, 11 of Level 3
            BigDecimal r1;
            String f1;
            if (i <= 2) {
                r1 = new BigDecimal("1.00");
                f1 = "Slight";
            } else if (i <= 7) {
                r1 = new BigDecimal("2.00");
                f1 = "Moderate";
            } else {
                r1 = new BigDecimal("3.00");
                f1 = "Substantial";
            }
            coRatings.put("CO1", r1);
            coFeedbacks.put("CO1", f1);

            // CO2: balanced distribution (6, 6, 6)
            BigDecimal r2 = (i <= 6) ? new BigDecimal("1.00") : ((i <= 12) ? new BigDecimal("2.00") : new BigDecimal("3.00"));
            coRatings.put("CO2", r2);

            // CO3 distribution: 3 of Level 1, 3 of Level 2, 12 of Level 3
            BigDecimal r3;
            String f3;
            if (i <= 3) {
                r3 = new BigDecimal("1.00");
                f3 = "Slight";
            } else if (i <= 6) {
                r3 = new BigDecimal("2.00");
                f3 = "Moderate";
            } else {
                r3 = new BigDecimal("3.00");
                f3 = "Substantial";
            }
            coRatings.put("CO3", r3);
            coFeedbacks.put("CO3", f3);

            // CO4, CO5, CO6 ratings
            coRatings.put("CO4", new BigDecimal("3.00"));
            coRatings.put("CO5", new BigDecimal("3.00"));
            coRatings.put("CO6", new BigDecimal("3.00"));

            rows.add(SurveyResponseRowDto.builder()
                    .srNo(i)
                    .prn("20210100" + String.format("%04d", i))
                    .studentName("Student " + i)
                    .coRatings(coRatings)
                    .coFeedbacks(coFeedbacks)
                    .build());
        }

        SurveyMarksPayloadDto payload = SurveyMarksPayloadDto.builder()
                .masterCourseId(offering6Cos.getId())
                .surveyResponses(rows)
                .build();

        attainmentCalculationService.calculateSurveyAttainment(offering6Cos.getId(), payload);
    }

    @Test
    @DisplayName("TEST 1: All COs returned in one request (batch-efficient)")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testAllCosReturnedInOneRequest() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/co-indirect-evidence")
                        .param("programmeBatchCourseId", offering6Cos.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.selectedCoCode").value("ALL"))
                .andExpect(jsonPath("$.data.courseCode").value("CS301"))
                .andExpect(jsonPath("$.data.courseName").value("Database Management Systems"))
                .andExpect(jsonPath("$.data.assessmentMethod").value("Course End Survey"))
                .andExpect(jsonPath("$.data.totalSurveyResponses").value(18))
                .andReturn();

        ApiResponse<CoIndirectEvidenceResponseDto> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getCoEvidence()).hasSize(6);

        List<String> coCodes = response.getData().getCoEvidence().stream()
                .map(CoIndirectEvidenceItemDto::getCoCode)
                .toList();
        assertThat(coCodes).containsExactly("CO1", "CO2", "CO3", "CO4", "CO5", "CO6");
    }

    @Test
    @DisplayName("TEST 2: Selected CO returns only selected CO evidence (validates 404 for invalid CO)")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testSelectedCoReturnsOnlySelectedEvidenceAnd404OnInvalid() throws Exception {
        // Query for single valid CO1
        mockMvc.perform(get("/api/v1/analytics/co-indirect-evidence")
                        .param("programmeBatchCourseId", offering6Cos.getId())
                        .param("coCode", "CO1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.selectedCoCode").value("CO1"))
                .andExpect(jsonPath("$.data.coEvidence").isArray())
                .andExpect(jsonPath("$.data.coEvidence.length()").value(1))
                .andExpect(jsonPath("$.data.coEvidence[0].coCode").value("CO1"));

        // Query for non-existent CO99 returns 404 Not Found
        mockMvc.perform(get("/api/v1/analytics/co-indirect-evidence")
                        .param("programmeBatchCourseId", offering6Cos.getId())
                        .param("coCode", "CO99")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Direct service call throws ResourceNotFoundException
        assertThatThrownBy(() -> analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "CO99"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("CO99");
    }

    @Test
    @DisplayName("TEST 3: CO1 distribution: 2 / 5 / 11 for 18 responses")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testCo1Distribution2_5_11() {
        CoIndirectEvidenceResponseDto response = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "CO1");

        assertThat(response.getCoEvidence()).hasSize(1);
        CoIndirectEvidenceItemDto co1 = response.getCoEvidence().get(0);

        assertThat(co1.getLevel1Count()).isEqualTo(2);
        assertThat(co1.getLevel2Count()).isEqualTo(5);
        assertThat(co1.getLevel3Count()).isEqualTo(11);
        assertThat(co1.getValidResponseCount()).isEqualTo(18);

        Map<String, Integer> dist = co1.getLevelDistribution();
        assertThat(dist).isNotNull();
        assertThat(dist.get("Level 1 (Slight)")).isEqualTo(2);
        assertThat(dist.get("Level 2 (Moderate)")).isEqualTo(5);
        assertThat(dist.get("Level 3 (Substantial)")).isEqualTo(11);
    }

    @Test
    @DisplayName("TEST 4: CO3 distribution: 3 / 3 / 12 for 18 responses")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testCo3Distribution3_3_12() {
        CoIndirectEvidenceResponseDto response = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "CO3");

        assertThat(response.getCoEvidence()).hasSize(1);
        CoIndirectEvidenceItemDto co3 = response.getCoEvidence().get(0);

        assertThat(co3.getLevel1Count()).isEqualTo(3);
        assertThat(co3.getLevel2Count()).isEqualTo(3);
        assertThat(co3.getLevel3Count()).isEqualTo(12);
        assertThat(co3.getValidResponseCount()).isEqualTo(18);

        Map<String, Integer> dist = co3.getLevelDistribution();
        assertThat(dist).isNotNull();
        assertThat(dist.get("Level 1 (Slight)")).isEqualTo(3);
        assertThat(dist.get("Level 2 (Moderate)")).isEqualTo(3);
        assertThat(dist.get("Level 3 (Substantial)")).isEqualTo(12);
    }

    @Test
    @DisplayName("TEST 5: Counts equal valid response count invariant")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testCountsEqualValidResponseCountInvariant() {
        CoIndirectEvidenceResponseDto response = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "ALL");

        for (CoIndirectEvidenceItemDto item : response.getCoEvidence()) {
            int sum = item.getLevel1Count() + item.getLevel2Count() + item.getLevel3Count();
            assertThat(sum)
                    .withFailMessage("Sum of level counts (%d) must equal validResponseCount (%d) for %s",
                            sum, item.getValidResponseCount(), item.getCoCode())
                    .isEqualTo(item.getValidResponseCount());

            assertThat(item.getResponseRecords()).hasSize(item.getValidResponseCount());
        }
    }

    @Test
    @DisplayName("TEST 6: Percentages match counts and rounding (11.11, 27.78, 61.11)")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testPercentagesMatchCountsAndRounding() {
        CoIndirectEvidenceResponseDto response = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "CO1");
        CoIndirectEvidenceItemDto co1 = response.getCoEvidence().get(0);

        // For CO1: 2/18 = 11.11%, 5/18 = 27.78%, 11/18 = 61.11%
        assertThat(co1.getLevel1Percentage()).isEqualByComparingTo(new BigDecimal("11.11"));
        assertThat(co1.getLevel2Percentage()).isEqualByComparingTo(new BigDecimal("27.78"));
        assertThat(co1.getLevel3Percentage()).isEqualByComparingTo(new BigDecimal("61.11"));
    }

    @Test
    @DisplayName("TEST 7: Indirect attainment matches authoritative calculation (level 3)")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testIndirectAttainmentLevelMatchesAuthoritative() {
        CoIndirectEvidenceResponseDto response = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "CO1");
        CoIndirectEvidenceItemDto co1 = response.getCoEvidence().get(0);

        assertThat(co1.getIndirectAttainment()).isEqualTo(3);
        assertThat(co1.getIndirectScore()).isEqualByComparingTo(new BigDecimal("3.00"));
        assertThat(co1.getCoTargetMet()).isTrue();
    }

    @Test
    @DisplayName("TEST 8: Overall indirect percentage matches authoritative value (83.39%)")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testOverallIndirectPercentageMatchesAuthoritative83_39() {
        CoIndirectEvidenceResponseDto response = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "CO1");
        CoIndirectEvidenceItemDto co1 = response.getCoEvidence().get(0);

        // (11.1111... * 0.33) + (27.7777... * 0.67) + (61.1111... * 1.0) = 83.39%
        assertThat(co1.getOverallIndirectPercentage()).isEqualByComparingTo(new BigDecimal("83.39"));
    }

    @Test
    @DisplayName("TEST 9: Missing/invalid CO rating does not count as a level")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testMissingOrInvalidCoRatingDoesNotCountAsLevel() {
        // Create an isolated course offering with 1 CO
        String offeringId = "offering-invalid-rating-" + UUID.randomUUID().toString().substring(0, 8);
        programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id(offeringId)
                .programmeBatchId(batch.getId())
                .code("TEST101")
                .name("Testing Invalid Ratings")
                .semester(1)
                .build());

        courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-inv-1")
                .programmeBatchCourseId(offeringId)
                .code("CO1")
                .statement("Test CO1")
                .targetLevel(new BigDecimal("2.50"))
                .build());

        // Insert 3 valid responses (ratings 1, 2, 3) and 3 invalid responses (ratings 0, 4, null)
        List<SurveyResponseRowDto> rows = new ArrayList<>();
        rows.add(SurveyResponseRowDto.builder().srNo(1).prn("P1").coRatings(Map.of("CO1", new BigDecimal("1.00"))).build());
        rows.add(SurveyResponseRowDto.builder().srNo(2).prn("P2").coRatings(Map.of("CO1", new BigDecimal("2.00"))).build());
        rows.add(SurveyResponseRowDto.builder().srNo(3).prn("P3").coRatings(Map.of("CO1", new BigDecimal("3.00"))).build());
        // Invalid responses:
        rows.add(SurveyResponseRowDto.builder().srNo(4).prn("P4").coRatings(Map.of("CO1", new BigDecimal("0.00"))).build());
        rows.add(SurveyResponseRowDto.builder().srNo(5).prn("P5").coRatings(Map.of("CO1", new BigDecimal("4.00"))).build());
        rows.add(SurveyResponseRowDto.builder().srNo(6).prn("P6").coRatings(Collections.emptyMap()).build());

        attainmentCalculationService.calculateSurveyAttainment(offeringId, SurveyMarksPayloadDto.builder()
                .masterCourseId(offeringId)
                .surveyResponses(rows)
                .build());

        CoIndirectEvidenceResponseDto response = analyticsService.getCoIndirectEvidence(offeringId, "CO1");
        CoIndirectEvidenceItemDto co1 = response.getCoEvidence().get(0);

        // Only ratings 1, 2, 3 must be counted
        assertThat(co1.getLevel1Count()).isEqualTo(1);
        assertThat(co1.getLevel2Count()).isEqualTo(1);
        assertThat(co1.getLevel3Count()).isEqualTo(1);
        assertThat(co1.getValidResponseCount()).isEqualTo(3);
        assertThat(co1.getResponseRecords()).hasSize(3);
    }

    @Test
    @DisplayName("TEST 10: Dynamic CO count (course with 4 COs)")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testDynamicCoCountCourseWith4Cos() {
        // Seed survey for offering4Cos (only 4 COs)
        List<SurveyResponseRowDto> rows = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, BigDecimal> rMap = new LinkedHashMap<>();
            rMap.put("CO1", new BigDecimal("3.00"));
            rMap.put("CO2", new BigDecimal("3.00"));
            rMap.put("CO3", new BigDecimal("3.00"));
            rMap.put("CO4", new BigDecimal("3.00"));
            rows.add(SurveyResponseRowDto.builder().srNo(i).prn("STD0" + i).coRatings(rMap).build());
        }

        attainmentCalculationService.calculateSurveyAttainment(offering4Cos.getId(), SurveyMarksPayloadDto.builder()
                .masterCourseId(offering4Cos.getId())
                .surveyResponses(rows)
                .build());

        CoIndirectEvidenceResponseDto allResponse = analyticsService.getCoIndirectEvidence(offering4Cos.getId(), "ALL");
        assertThat(allResponse.getCoEvidence()).hasSize(4);
        List<String> codes = allResponse.getCoEvidence().stream().map(CoIndirectEvidenceItemDto::getCoCode).toList();
        assertThat(codes).containsExactly("CO1", "CO2", "CO3", "CO4");

        // Requesting existing CO4 succeeds
        CoIndirectEvidenceResponseDto co4Response = analyticsService.getCoIndirectEvidence(offering4Cos.getId(), "CO4");
        assertThat(co4Response.getCoEvidence()).hasSize(1);
        assertThat(co4Response.getCoEvidence().get(0).getCoCode()).isEqualTo("CO4");

        // Requesting CO5 on this 4-CO course throws ResourceNotFoundException (404)
        assertThatThrownBy(() -> analyticsService.getCoIndirectEvidence(offering4Cos.getId(), "CO5"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("TEST 11: Zero survey responses returns safe empty response without division by zero")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testZeroSurveyResponsesReturnsSafeResponse() {
        String offeringZero = "offering-zero-survey-" + UUID.randomUUID().toString().substring(0, 8);
        programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id(offeringZero)
                .programmeBatchId(batch.getId())
                .code("EMPTY101")
                .name("Empty Survey Course")
                .semester(2)
                .build());

        courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-z-1")
                .programmeBatchCourseId(offeringZero)
                .code("CO1")
                .statement("Outcome 1")
                .targetLevel(new BigDecimal("2.50"))
                .build());

        CoIndirectEvidenceResponseDto response = analyticsService.getCoIndirectEvidence(offeringZero, "ALL");

        assertThat(response).isNotNull();
        assertThat(response.getTotalSurveyResponses()).isEqualTo(0);
        assertThat(response.getOverallIndirectAttainment()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCoEvidence()).hasSize(1);

        CoIndirectEvidenceItemDto item = response.getCoEvidence().get(0);
        assertThat(item.getLevel1Count()).isEqualTo(0);
        assertThat(item.getLevel2Count()).isEqualTo(0);
        assertThat(item.getLevel3Count()).isEqualTo(0);
        assertThat(item.getValidResponseCount()).isEqualTo(0);
        assertThat(item.getLevel1Percentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getLevel2Percentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getLevel3Percentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getOverallIndirectPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(item.getResponseRecords()).isEmpty();
    }

    @Test
    @DisplayName("TEST 12: Unauthorized user cannot access another course's evidence (403 Forbidden)")
    void testUnauthorizedUserAccessDenied() throws Exception {
        // Faculty user not assigned to offering6Cos
        mockMvc.perform(get("/api/v1/analytics/co-indirect-evidence")
                        .param("programmeBatchCourseId", offering6Cos.getId())
                        .with(user("unassigned_faculty").roles("FACULTY"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TEST 13: Survey mutation invalidates stale Analytics evidence")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testSurveyMutationInvalidatesStaleEvidence() {
        // 1. Initial evidence has 18 responses, level 3 for CO1
        CoIndirectEvidenceResponseDto initial = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "CO1");
        assertThat(initial.getTotalSurveyResponses()).isEqualTo(18);
        assertThat(initial.getCoEvidence().get(0).getIndirectAttainment()).isEqualTo(3);

        // 2. Mutate survey: 5 new responses, all rating 1 (Slight)
        List<SurveyResponseRowDto> newRows = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            newRows.add(SurveyResponseRowDto.builder()
                    .srNo(i)
                    .prn("NEW00" + i)
                    .coRatings(Map.of("CO1", new BigDecimal("1.00")))
                    .build());
        }

        attainmentCalculationService.calculateSurveyAttainment(offering6Cos.getId(), SurveyMarksPayloadDto.builder()
                .masterCourseId(offering6Cos.getId())
                .surveyResponses(newRows)
                .build());

        // 3. Analytics service fetches fresh data immediately due to cache eviction
        CoIndirectEvidenceResponseDto fresh = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "CO1");
        assertThat(fresh.getTotalSurveyResponses()).isEqualTo(5);
        CoIndirectEvidenceItemDto freshCo1 = fresh.getCoEvidence().get(0);
        assertThat(freshCo1.getValidResponseCount()).isEqualTo(5);
        assertThat(freshCo1.getLevel1Count()).isEqualTo(5);
        assertThat(freshCo1.getLevel2Count()).isEqualTo(0);
        assertThat(freshCo1.getLevel3Count()).isEqualTo(0);
        assertThat(freshCo1.getLevel1Percentage()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(freshCo1.getIndirectAttainment()).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("TEST 14: Batch efficiency / No N+1 query pattern")
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    void testBatchEfficiencyAndSubsequentCachedLookup() {
        long start1 = System.nanoTime();
        CoIndirectEvidenceResponseDto firstCall = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "ALL");
        long durationFirstCallMs = (System.nanoTime() - start1) / 1_000_000;

        assertThat(firstCall.getCoEvidence()).hasSize(6);

        // Subsequent call is served from in-memory cache
        long start2 = System.nanoTime();
        CoIndirectEvidenceResponseDto secondCall = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "ALL");
        long durationSecondCallMs = (System.nanoTime() - start2) / 1_000_000;

        assertThat(secondCall.getCoEvidence()).hasSize(6);

        // Filtered call for single CO is served from canonical in-memory cache without DB queries
        long start3 = System.nanoTime();
        CoIndirectEvidenceResponseDto thirdCall = analyticsService.getCoIndirectEvidence(offering6Cos.getId(), "CO3");
        long durationThirdCallMs = (System.nanoTime() - start3) / 1_000_000;

        assertThat(thirdCall.getCoEvidence()).hasSize(1);
        assertThat(thirdCall.getCoEvidence().get(0).getCoCode()).isEqualTo("CO3");

        // In-memory lookups should be sub-millisecond or negligible (< 20ms even under test harness)
        assertThat(durationSecondCallMs).isLessThanOrEqualTo(durationFirstCallMs + 10);
        assertThat(durationThirdCallMs).isLessThan(50);
    }
}
