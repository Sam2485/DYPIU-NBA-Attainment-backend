package com.dypiu.nba.analytics;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
import com.dypiu.nba.dto.ProgrammeSurveyResultDto;
import com.dypiu.nba.dto.analytics.OutcomeIndirectDrilldownResponseDto;
import com.dypiu.nba.dto.analytics.OutcomeIndirectEvidenceItemDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AnalyticsService;
import com.dypiu.nba.service.AttainmentCalculationService;
import com.dypiu.nba.service.IndirectAssessmentService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class OutcomeIndirectDrilldownApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private IndirectAssessmentService indirectAssessmentService;

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
    private ProgrammeOutcomeRepository poRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository psoRepository;

    @Autowired
    private ProgrammeBatchIndirectAssessmentRepository indirectAssessmentRepository;

    @Autowired
    private ProgrammeBatchAttainmentReportRepository reportRepository;

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
    private ProgrammeBatch batchActive2022;
    private ProgrammeBatch batchFinalized2021;
    private ProgrammeBatch batchEmpty2023;

    private ProgrammeBatchIndirectAssessment assess1Event;
    private ProgrammeBatchIndirectAssessment assess2Survey;
    private ProgrammeBatchIndirectAssessment assess3Batch2023;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Setup Users for RBAC testing
        userRepository.save(User.builder()
                .id(3001L)
                .username("iqac_indirect_user")
                .email("iqac_indirect_user@dypiu.ac.in")
                .name("IQAC Officer")
                .passwordHash("test_hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(3002L)
                .username("hod_cse_indirect_user")
                .email("hod_cse_indirect_user@dypiu.ac.in")
                .name("HOD CSE")
                .passwordHash("test_hash")
                .role(UserRole.HOD)
                .schoolId("sch-soet-ind")
                .departmentId("dept-cse-ind")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(3003L)
                .username("hod_mba_indirect_user")
                .email("hod_mba_indirect_user@dypiu.ac.in")
                .name("HOD MBA")
                .passwordHash("test_hash")
                .role(UserRole.HOD)
                .schoolId("sch-som-ind")
                .departmentId("dept-mba-ind")
                .isActive(true)
                .build());

        // 2. Setup Schools & Departments
        schoolSoet = schoolRepository.save(School.builder().id("sch-soet-ind").code("SOET-IND").name("School of Engineering").build());
        schoolSom = schoolRepository.save(School.builder().id("sch-som-ind").code("SOM-IND").name("School of Management").build());

        deptCse = departmentRepository.save(Department.builder().id("dept-cse-ind").code("CSE-IND").name("Computer Science").schoolId(schoolSoet.getId()).build());
        deptMba = departmentRepository.save(Department.builder().id("dept-mba-ind").code("MBA-IND").name("Management").schoolId(schoolSom.getId()).build());

        // 3. Setup Programmes & Batches
        progBtech = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-btech-ind")
                .code("BTECH-IND")
                .name("B.Tech CSE")
                .degreeAwarded("B.Tech")
                .departmentId(deptCse.getId())
                .durationYears(4)
                .build());

        progMba = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-mba-ind")
                .code("MBA-IND")
                .name("MBA")
                .degreeAwarded("MBA")
                .departmentId(deptMba.getId())
                .durationYears(2)
                .build());

        // Active Batch
        batchActive2022 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-active-2022-ind")
                .masterProgrammeId(progBtech.getId())
                .name("2022-2026")
                .coordinatorName("Dr. Alan Turing")
                .startYear(2022)
                .endYear(2026)
                .status("ACTIVE")
                .createdAt(ZonedDateTime.now().minusYears(1))
                .build());

        // Finalized Batch
        batchFinalized2021 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-final-2021-ind")
                .masterProgrammeId(progBtech.getId())
                .name("2021-2025")
                .coordinatorName("Dr. Ada Lovelace")
                .startYear(2021)
                .endYear(2025)
                .status("FINALIZED")
                .createdAt(ZonedDateTime.now().minusYears(2))
                .build());

        // Empty Batch
        batchEmpty2023 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-empty-2023-ind")
                .masterProgrammeId(progBtech.getId())
                .name("2023-2027")
                .coordinatorName("Dr. Claude Shannon")
                .startYear(2023)
                .endYear(2027)
                .status("ACTIVE")
                .createdAt(ZonedDateTime.now().minusMonths(6))
                .build());

        // 4. Setup PO/PSO definitions
        poRepository.save(ProgrammeOutcome.builder()
                .id("po-ind-1")
                .programmeBatchId(batchActive2022.getId())
                .code("PO1")
                .statement("Engineering Knowledge: Apply math and science")
                .target(new BigDecimal("2.00"))
                .build());

        poRepository.save(ProgrammeOutcome.builder()
                .id("po-ind-7")
                .programmeBatchId(batchActive2022.getId())
                .code("PO7")
                .statement("Environment and Sustainability")
                .target(new BigDecimal("2.50"))
                .build());

        poRepository.save(ProgrammeOutcome.builder()
                .id("po-ind-12")
                .programmeBatchId(batchActive2022.getId())
                .code("PO12")
                .statement("Life-long Learning")
                .target(new BigDecimal("2.00"))
                .build());

        psoRepository.save(ProgrammeSpecificOutcome.builder()
                .id("pso-ind-1")
                .programmeBatchId(batchActive2022.getId())
                .code("PSO1")
                .statement("Software Development & Engineering")
                .target(new BigDecimal("2.00"))
                .build());

        psoRepository.save(ProgrammeSpecificOutcome.builder()
                .id("pso-ind-2")
                .programmeBatchId(batchActive2022.getId())
                .code("PSO2")
                .statement("AI and Data Intelligence")
                .target(new BigDecimal("2.20"))
                .build());

        // Also define for batchEmpty2023 and batchFinalized2021
        poRepository.save(ProgrammeOutcome.builder()
                .id("po-empty-1")
                .programmeBatchId(batchEmpty2023.getId())
                .code("PO1")
                .statement("Engineering Knowledge: Apply math and science")
                .target(new BigDecimal("2.00"))
                .build());

        poRepository.save(ProgrammeOutcome.builder()
                .id("po-final-1")
                .programmeBatchId(batchFinalized2021.getId())
                .code("PO1")
                .statement("Engineering Knowledge: Apply math and science")
                .target(new BigDecimal("2.00"))
                .build());

        // 5. Setup Indirect Assessments for batchActive2022
        // Assessment 1: Technical Hackathon (EVENT) - evaluates PO1 (2.50) and PO7 (2.10)
        assess1Event = indirectAssessmentRepository.save(ProgrammeBatchIndirectAssessment.builder()
                .id("pbia-hackathon-2022")
                .programmeBatchId(batchActive2022.getId())
                .name("Annual Tech Hackathon 2023")
                .type("EVENT")
                .description("48-hour coding hackathon evaluated by external industry mentors")
                .createdBy("Prof. Sharma")
                .scoresJson(objectMapper.writeValueAsString(Map.of(
                        "PO1", new BigDecimal("2.50"),
                        "PO7", new BigDecimal("2.10"),
                        "PSO1", new BigDecimal("2.60")
                )))
                .createdAt(ZonedDateTime.now().minusMonths(6))
                .build());

        // Assessment 2: Alumni Stakeholder Survey (SURVEY) - evaluates PO7 (2.30) and PSO1 (2.40), PO1 is missing/0.00
        assess2Survey = indirectAssessmentRepository.save(ProgrammeBatchIndirectAssessment.builder()
                .id("pbia-alumni-2022")
                .programmeBatchId(batchActive2022.getId())
                .name("Alumni Feedback Survey 2024")
                .type("SURVEY")
                .description("Triennial alumni feedback on programme outcomes")
                .createdBy("Prof. Patel")
                .scoresJson(objectMapper.writeValueAsString(Map.of(
                        "PO7", new BigDecimal("2.30"),
                        "PSO1", new BigDecimal("2.40"),
                        "PSO2", new BigDecimal("2.20")
                )))
                .createdAt(ZonedDateTime.now().minusMonths(3))
                .build());

        // Assessment 3 for batchEmpty2023 (Isolation check)
        assess3Batch2023 = indirectAssessmentRepository.save(ProgrammeBatchIndirectAssessment.builder()
                .id("pbia-freshers-2023")
                .programmeBatchId(batchEmpty2023.getId())
                .name("Freshers Induction Event")
                .type("EVENT")
                .description("Induction event for 2023 batch")
                .createdBy("Prof. Kulkarni")
                .scoresJson(objectMapper.writeValueAsString(Map.of(
                        "PO1", new BigDecimal("2.80")
                )))
                .createdAt(ZonedDateTime.now().minusMonths(2))
                .build());

        // 6. Setup Programme Exit Survey for batchActive2022
        // Evaluates PO1 (2.10) and PSO1 (2.00). PO7 is not in exit survey.
        ProgrammeSurveyResultDto exitSurveyPayload = ProgrammeSurveyResultDto.builder()
                .uploadId("exit-survey-batch-active-2022")
                .masterProgrammeId(progBtech.getId())
                .programmeBatchId(batchActive2022.getId())
                .surveyType("EXIT_SURVEY")
                .recordsProcessed(45)
                .poIndirectAttainment(List.of(
                        new ProgrammeSurveyResultDto.PoIndirectItem("PO1", new BigDecimal("2.10")),
                        new ProgrammeSurveyResultDto.PoIndirectItem("PO12", new BigDecimal("2.40"))
                ))
                .psoIndirectAttainment(List.of(
                        new ProgrammeSurveyResultDto.PsoIndirectItem("PSO1", new BigDecimal("2.00"))
                ))
                .status("SAVED")
                .build();
        attainmentCalculationService.saveProgrammeSurveyResult(progBtech.getId(), batchActive2022.getId(), exitSurveyPayload);

        // 7. Setup Finalized Attainment Report for batchFinalized2021
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> posFinal = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                        .poCode("PO1")
                        .statement("Engineering Knowledge")
                        .targetLevel(new BigDecimal("2.00"))
                        .directAttainment(new BigDecimal("2.40"))
                        .indirectAttainment(new BigDecimal("2.35"))
                        .finalAttainment(new BigDecimal("2.39"))
                        .targetMet(true)
                        .build()
        );
        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-final-2021")
                .programmeBatchId(batchFinalized2021.getId())
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(Map.of("po", posFinal, "pso", List.of())))
                .averageMappingReportJson(objectMapper.writeValueAsString(Map.of("courses", List.of())))
                .directAttainmentReportJson(objectMapper.writeValueAsString(Map.of("courses", List.of())))
                .approvedAt(ZonedDateTime.now().minusDays(10))
                .build());
    }

    // -------------------------------------------------------------------------
    // TEST 1: PO Happy Path (PO1)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 1: PO Happy Path - verifies header, arithmetic consolidation, and evidence items")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_PoHappyPath() {
        OutcomeIndirectDrilldownResponseDto result = analyticsService.getOutcomeIndirectDrilldown(
                batchActive2022.getId(), "PO1", "PO");

        assertThat(result).isNotNull();
        assertThat(result.getProgrammeBatchId()).isEqualTo(batchActive2022.getId());
        assertThat(result.getBatchName()).isEqualTo("2022-2026");
        assertThat(result.getOutcomeCode()).isEqualTo("PO1");
        assertThat(result.getOutcomeType()).isEqualTo("PO");
        assertThat(result.getOutcomeStatement()).isEqualTo("Engineering Knowledge: Apply math and science");
        assertThat(result.getTarget()).isEqualByComparingTo("2.00");

        // Expected consolidation for PO1:
        // Participating: Hackathon (2.50) + Exit Survey (2.10) = 4.60 / 2 = 2.30
        assertThat(result.getIndirectAttainment()).isEqualByComparingTo("2.30");
        assertThat(result.getIndirectGap()).isEqualByComparingTo("0.30");
        assertThat(result.isTargetMet()).isTrue();

        // Total evidence = 3 (Hackathon, Alumni Survey, Exit Survey)
        // Participating = 2 (Hackathon, Exit Survey)
        assertThat(result.getTotalEvidenceCount()).isEqualTo(3);
        assertThat(result.getParticipatingEvidenceCount()).isEqualTo(2);

        List<OutcomeIndirectEvidenceItemDto> items = result.getEvidence();
        assertThat(items).hasSize(3);

        // Item 1: Hackathon (evaluated = true)
        OutcomeIndirectEvidenceItemDto item1 = items.get(0);
        assertThat(item1.getAssessmentId()).isEqualTo("pbia-hackathon-2022");
        assertThat(item1.getType()).isEqualTo("EVENT");
        assertThat(item1.getName()).isEqualTo("Annual Tech Hackathon 2023");
        assertThat(item1.isOutcomeEvaluated()).isTrue();
        assertThat(item1.getOutcomeValue()).isEqualByComparingTo("2.50");
        assertThat(item1.getCreatedBy()).isEqualTo("Prof. Sharma");

        // Item 2: Alumni Survey (evaluated = false for PO1)
        OutcomeIndirectEvidenceItemDto item2 = items.get(1);
        assertThat(item2.getAssessmentId()).isEqualTo("pbia-alumni-2022");
        assertThat(item2.getType()).isEqualTo("SURVEY");
        assertThat(item2.getName()).isEqualTo("Alumni Feedback Survey 2024");
        assertThat(item2.isOutcomeEvaluated()).isFalse();
        assertThat(item2.getOutcomeValue()).isNull();

        // Item 3: Exit Survey (evaluated = true)
        OutcomeIndirectEvidenceItemDto item3 = items.get(2);
        assertThat(item3.getType()).isEqualTo("EXIT_SURVEY");
        assertThat(item3.getName()).isEqualTo("Programme End Exit Survey");
        assertThat(item3.isOutcomeEvaluated()).isTrue();
        assertThat(item3.getOutcomeValue()).isEqualByComparingTo("2.10");
        assertThat(item3.getResponseCount()).isEqualTo(45);
    }

    // -------------------------------------------------------------------------
    // TEST 2: PSO Happy Path (PSO1)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 2: PSO Happy Path - verifies PSO1 consolidation across all 3 evidence sources")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_PsoHappyPath() {
        OutcomeIndirectDrilldownResponseDto result = analyticsService.getOutcomeIndirectDrilldown(
                batchActive2022.getId(), "PSO1", "PSO");

        assertThat(result).isNotNull();
        assertThat(result.getOutcomeCode()).isEqualTo("PSO1");
        assertThat(result.getOutcomeType()).isEqualTo("PSO");
        assertThat(result.getOutcomeStatement()).isEqualTo("Software Development & Engineering");
        assertThat(result.getTarget()).isEqualByComparingTo("2.00");

        // Participating: Hackathon (2.60) + Alumni Survey (2.40) + Exit Survey (2.00) = 7.00 / 3 = 2.33
        assertThat(result.getIndirectAttainment()).isEqualByComparingTo("2.33");
        assertThat(result.getIndirectGap()).isEqualByComparingTo("0.33");
        assertThat(result.isTargetMet()).isTrue();
        assertThat(result.getTotalEvidenceCount()).isEqualTo(3);
        assertThat(result.getParticipatingEvidenceCount()).isEqualTo(3);

        List<OutcomeIndirectEvidenceItemDto> items = result.getEvidence();
        assertThat(items).hasSize(3);
        assertThat(items).allMatch(OutcomeIndirectEvidenceItemDto::isOutcomeEvaluated);
    }

    // -------------------------------------------------------------------------
    // TEST 3: Outcome Filtering Isolation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 3: Outcome Filtering - scores for other outcomes never leak into target outcome value")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_OutcomeFilteringIsolation() {
        // PO7 is evaluated by Hackathon (2.10) and Alumni Survey (2.30), but NOT Exit Survey
        OutcomeIndirectDrilldownResponseDto po7Result = analyticsService.getOutcomeIndirectDrilldown(
                batchActive2022.getId(), "PO7", "PO");

        assertThat(po7Result.getOutcomeCode()).isEqualTo("PO7");
        assertThat(po7Result.getTarget()).isEqualByComparingTo("2.50");
        // Participating: (2.10 + 2.30) / 2 = 2.20
        assertThat(po7Result.getIndirectAttainment()).isEqualByComparingTo("2.20");
        assertThat(po7Result.getIndirectGap()).isEqualByComparingTo("-0.30");
        assertThat(po7Result.isTargetMet()).isFalse();
        assertThat(po7Result.getTotalEvidenceCount()).isEqualTo(3);
        assertThat(po7Result.getParticipatingEvidenceCount()).isEqualTo(2);

        // Verify Exit Survey has evaluated = false and outcomeValue = null for PO7
        OutcomeIndirectEvidenceItemDto exitItem = po7Result.getEvidence().stream()
                .filter(e -> "EXIT_SURVEY".equals(e.getType()))
                .findFirst().orElseThrow();
        assertThat(exitItem.isOutcomeEvaluated()).isFalse();
        assertThat(exitItem.getOutcomeValue()).isNull();
    }

    // -------------------------------------------------------------------------
    // TEST 4: Mathematical Consolidation Consistency with Service
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 4: Consolidation Consistency - matches IndirectAssessmentService unweighted arithmetic mean")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_ConsolidationConsistency() {
        OutcomeIndirectDrilldownResponseDto drilldown = analyticsService.getOutcomeIndirectDrilldown(
                batchActive2022.getId(), "PO1", "PO");

        Map<String, BigDecimal> exitScores = Map.of("PO1", new BigDecimal("2.10"), "PO12", new BigDecimal("2.40"));
        Map<String, BigDecimal> serviceConsolidated = indirectAssessmentService.computeConsolidatedScores(
                batchActive2022.getId(), exitScores);

        BigDecimal expected = serviceConsolidated.get("PO1");
        assertThat(drilldown.getIndirectAttainment()).isEqualByComparingTo(expected);
    }

    // -------------------------------------------------------------------------
    // TEST 5: Chronological Ordering
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 5: Chronological Ordering - assessments ordered earliest first, Exit Survey at end")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_ChronologicalOrdering() {
        OutcomeIndirectDrilldownResponseDto result = analyticsService.getOutcomeIndirectDrilldown(
                batchActive2022.getId(), "PO1", "PO");

        List<OutcomeIndirectEvidenceItemDto> items = result.getEvidence();
        assertThat(items).hasSize(3);

        assertThat(items.get(0).getAssessmentId()).isEqualTo("pbia-hackathon-2022");
        assertThat(items.get(1).getAssessmentId()).isEqualTo("pbia-alumni-2022");
        assertThat(items.get(2).getType()).isEqualTo("EXIT_SURVEY");

        // Verify date monotonicity
        if (items.get(0).getDate() != null && items.get(1).getDate() != null) {
            assertThat(items.get(0).getDate()).isBeforeOrEqualTo(items.get(1).getDate());
        }
        if (items.get(1).getDate() != null && items.get(2).getDate() != null) {
            assertThat(items.get(1).getDate()).isBeforeOrEqualTo(items.get(2).getDate());
        }
    }

    // -------------------------------------------------------------------------
    // TEST 6: Zero Evidence Behavior
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 6: Zero Evidence - safe handling when no assessments or surveys exist")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_ZeroEvidence() {
        // PO12 in batchEmpty2023 has no assessments in DB and no exit survey
        poRepository.save(ProgrammeOutcome.builder()
                .id("po-empty-12")
                .programmeBatchId(batchEmpty2023.getId())
                .code("PO12")
                .statement("Life-long learning")
                .target(new BigDecimal("2.00"))
                .build());

        OutcomeIndirectDrilldownResponseDto result = analyticsService.getOutcomeIndirectDrilldown(
                batchEmpty2023.getId(), "PO12", "PO");

        assertThat(result).isNotNull();
        // batchEmpty2023 has 1 assessment in setUp (assess3Batch2023) which only evaluates PO1
        assertThat(result.getTotalEvidenceCount()).isEqualTo(1);
        assertThat(result.getParticipatingEvidenceCount()).isEqualTo(0);
        assertThat(result.getIndirectAttainment()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.isTargetMet()).isFalse();
        assertThat(result.getEvidence().get(0).isOutcomeEvaluated()).isFalse();
        assertThat(result.getEvidence().get(0).getOutcomeValue()).isNull();
    }

    // -------------------------------------------------------------------------
    // TEST 7: Cross-Batch Isolation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 7: Cross-Batch Isolation - assessments from batch 2022 never leak into batch 2023")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_CrossBatchIsolation() {
        OutcomeIndirectDrilldownResponseDto b23Result = analyticsService.getOutcomeIndirectDrilldown(
                batchEmpty2023.getId(), "PO1", "PO");

        // batchEmpty2023 only has assess3Batch2023
        assertThat(b23Result.getTotalEvidenceCount()).isEqualTo(1);
        assertThat(b23Result.getEvidence().get(0).getAssessmentId()).isEqualTo("pbia-freshers-2023");
        assertThat(b23Result.getIndirectAttainment()).isEqualByComparingTo("2.80");
        assertThat(b23Result.getEvidence()).noneMatch(e -> "pbia-hackathon-2022".equals(e.getAssessmentId()));
        assertThat(b23Result.getEvidence()).noneMatch(e -> "pbia-alumni-2022".equals(e.getAssessmentId()));
    }

    // -------------------------------------------------------------------------
    // TEST 8: Finalized Batch Handling
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 8: Finalized Batch - reads authoritative frozen snapshot from report")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_FinalizedBatchHandling() {
        OutcomeIndirectDrilldownResponseDto result = analyticsService.getOutcomeIndirectDrilldown(
                batchFinalized2021.getId(), "PO1", "PO");

        assertThat(result.getProgrammeBatchId()).isEqualTo(batchFinalized2021.getId());
        assertThat(result.getOutcomeCode()).isEqualTo("PO1");
        // Stored indirect attainment in frozen report: 2.35
        assertThat(result.getIndirectAttainment()).isEqualByComparingTo("2.35");
        assertThat(result.getTarget()).isEqualByComparingTo("2.00");
        assertThat(result.getIndirectGap()).isEqualByComparingTo("0.35");
        assertThat(result.isTargetMet()).isTrue();
    }

    // -------------------------------------------------------------------------
    // TEST 9: RBAC Authorization - HOD Scope Check
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 9: RBAC Authorization - HOD CSE can access CSE batch, HOD MBA is forbidden (403)")
    @WithMockUser(username = "hod_cse_indirect_user", roles = {"HOD"})
    void testOutcomeIndirectDrilldown_RbacAuthorizedHod() {
        OutcomeIndirectDrilldownResponseDto result = analyticsService.getOutcomeIndirectDrilldown(
                batchActive2022.getId(), "PO1", "PO");
        assertThat(result).isNotNull();
        assertThat(result.getOutcomeCode()).isEqualTo("PO1");
    }

    @Test
    @DisplayName("Test 9b: RBAC Authorization - HOD MBA is forbidden (403) from accessing CSE batch")
    @WithMockUser(username = "hod_mba_indirect_user", roles = {"HOD"})
    void testOutcomeIndirectDrilldown_RbacUnauthorizedHod() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                analyticsService.getOutcomeIndirectDrilldown(batchActive2022.getId(), "PO1", "PO")
        );
        assertThat(ex.getStatusCode().value()).isEqualTo(403);
    }

    // -------------------------------------------------------------------------
    // TEST 10: Input Validation Exceptions
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 10: Input Validation - invalid outcomeType throws BadRequestException")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_InvalidOutcomeType() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                analyticsService.getOutcomeIndirectDrilldown(batchActive2022.getId(), "PO1", "INVALID")
        );
        assertThat(ex.getMessage()).contains("Invalid outcomeType");
    }

    @Test
    @DisplayName("Test 10b: Input Validation - code-to-type mismatch throws BadRequestException")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_CodeTypeMismatch() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                analyticsService.getOutcomeIndirectDrilldown(batchActive2022.getId(), "PSO1", "PO")
        );
        assertThat(ex.getMessage()).contains("does not match outcomeType PO");
    }

    @Test
    @DisplayName("Test 10c: Unknown Outcome throws ResourceNotFoundException")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_UnknownOutcome() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () ->
                analyticsService.getOutcomeIndirectDrilldown(batchActive2022.getId(), "PO99", "PO")
        );
        assertThat(ex.getMessage()).contains("not found for programme batch");
    }

    // -------------------------------------------------------------------------
    // TEST 11: HTTP Endpoint MockMvc Test
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Test 11: MockMvc HTTP GET - verifies /api/v1/analytics/outcome-indirect-drilldown endpoint")
    @WithMockUser(username = "iqac_indirect_user", roles = {"IQAC"})
    void testOutcomeIndirectDrilldown_MockMvcHttpSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/outcome-indirect-drilldown")
                        .param("programmeBatchId", batchActive2022.getId())
                        .param("outcomeCode", "PO1")
                        .param("outcomeType", "PO")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Outcome indirect attainment drill-down retrieved successfully"))
                .andExpect(jsonPath("$.data.programmeBatchId").value(batchActive2022.getId()))
                .andExpect(jsonPath("$.data.batchName").value("2022-2026"))
                .andExpect(jsonPath("$.data.outcomeCode").value("PO1"))
                .andExpect(jsonPath("$.data.outcomeType").value("PO"))
                .andExpect(jsonPath("$.data.indirectAttainment").value(2.30))
                .andExpect(jsonPath("$.data.target").value(2.00))
                .andExpect(jsonPath("$.data.indirectGap").value(0.30))
                .andExpect(jsonPath("$.data.targetMet").value(true))
                .andExpect(jsonPath("$.data.totalEvidenceCount").value(3))
                .andExpect(jsonPath("$.data.participatingEvidenceCount").value(2))
                .andExpect(jsonPath("$.data.evidence").isArray())
                .andExpect(jsonPath("$.data.evidence.length()").value(3));
    }

    @Test
    @DisplayName("Test 11b: MockMvc HTTP 403 Forbidden for unauthorized user")
    @WithMockUser(username = "hod_mba_indirect_user", roles = {"HOD"})
    void testOutcomeIndirectDrilldown_MockMvcHttpForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/outcome-indirect-drilldown")
                        .param("programmeBatchId", batchActive2022.getId())
                        .param("outcomeCode", "PO1")
                        .param("outcomeType", "PO")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
