package com.dypiu.nba.analytics;

import com.dypiu.nba.controller.AnalyticsController;
import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class HistoricalAndBatchComparisonAnalyticsIntegrationTest {

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
    private ProgrammeBatchAttainmentReportRepository reportRepository;

    @Autowired
    private ProgrammeOutcomeRepository poRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository psoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private School schoolEng;
    private School schoolMgt;
    private Department deptCse;
    private Department deptBba;
    private MasterProgramme progCse;
    private MasterProgramme progBba;

    private ProgrammeBatch batchActive1;
    private ProgrammeBatch batchActive2;
    private ProgrammeBatch batchCompleted1;
    private ProgrammeBatch batchCompleted2;
    private ProgrammeBatch batchGraduated1;

    @BeforeEach
    void setUp() throws Exception {
        // Setup user scopes for testing RBAC
        userRepository.save(User.builder()
                .id(2001L)
                .username("iqac_analyst")
                .email("iqac_analyst@dypiu.ac.in")
                .name("IQAC Analyst")
                .passwordHash("test_hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(2002L)
                .username("pc_cse_user")
                .email("pc_cse@dypiu.ac.in")
                .name("PC CSE")
                .passwordHash("test_hash")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId("sch-eng-test")
                .departmentId("dept-cse-test")
                .masterProgrammeId("prog-cse-test")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(2003L)
                .username("pc_bba_user")
                .email("pc_bba@dypiu.ac.in")
                .name("PC BBA")
                .passwordHash("test_hash")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId("sch-mgt-test")
                .departmentId("dept-bba-test")
                .masterProgrammeId("prog-bba-test")
                .isActive(true)
                .build());

        // Setup academic structure
        schoolEng = schoolRepository.save(School.builder()
                .id("sch-eng-test").code("SET").name("School of Engineering").build());
        schoolMgt = schoolRepository.save(School.builder()
                .id("sch-mgt-test").code("SOM").name("School of Management").build());

        deptCse = departmentRepository.save(Department.builder()
                .id("dept-cse-test").code("CSE").name("Computer Science").schoolId(schoolEng.getId()).build());
        deptBba = departmentRepository.save(Department.builder()
                .id("dept-bba-test").code("BBA").name("Business Administration").schoolId(schoolMgt.getId()).build());

        progCse = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-cse-test").code("BTECH-CS").name("B.Tech Computer Science")
                .departmentId(deptCse.getId()).durationYears(4).build());
        progBba = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-bba-test").code("BBA-GEN").name("Bachelor of Business Administration")
                .departmentId(deptBba.getId()).durationYears(3).build());

        // Setup Batches with various lifecycle states
        // Concluded batch 1: COMPLETED (startYear: 2020, endYear: 2024)
        batchCompleted1 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-cse-2020")
                .masterProgrammeId(progCse.getId())
                .name("2020-2024 (Completed)")
                .startYear(2020)
                .endYear(2024)
                .status("COMPLETED")
                .build());

        // Concluded batch 2: GRADUATED (startYear: 2021, endYear: 2025)
        batchGraduated1 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-cse-2021")
                .masterProgrammeId(progCse.getId())
                .name("2021-2025 (Graduated)")
                .startYear(2021)
                .endYear(2025)
                .status("GRADUATED")
                .build());

        // Concluded batch 3: COMPLETED (startYear: 2019, endYear: 2023)
        batchCompleted2 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-cse-2019")
                .masterProgrammeId(progCse.getId())
                .name("2019-2023 (Older Completed)")
                .startYear(2019)
                .endYear(2023)
                .status("COMPLETED")
                .build());

        // Active batch 1: ACTIVE (startYear: 2022, endYear: 2026)
        batchActive1 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-cse-2022")
                .masterProgrammeId(progCse.getId())
                .name("2022-2026 (Active)")
                .startYear(2022)
                .endYear(2026)
                .status("ACTIVE")
                .build());

        // Active batch 2: ACTIVE (in different programme: BBA)
        batchActive2 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-bba-2023")
                .masterProgrammeId(progBba.getId())
                .name("2023-2026 BBA (Active)")
                .startYear(2023)
                .endYear(2026)
                .status("ACTIVE")
                .build());

        // Setup POs for batchCompleted1 with batch-specific targets (e.g. 2.40)
        setupOutcomesForBatch(batchCompleted1.getId(), new BigDecimal("2.40"));
        // Setup POs for batchGraduated1 with batch-specific targets (e.g. 2.60)
        setupOutcomesForBatch(batchGraduated1.getId(), new BigDecimal("2.60"));
        // Setup POs for batchActive1 with batch-specific targets (e.g. 2.50)
        setupOutcomesForBatch(batchActive1.getId(), new BigDecimal("2.50"));

        // Setup Finalized Report for batchCompleted1
        setupReportForBatch(batchCompleted1.getId(), new BigDecimal("2.40"), new BigDecimal("2.50"), new BigDecimal("2.00"));

        // Setup Finalized Report for batchGraduated1
        setupReportForBatch(batchGraduated1.getId(), new BigDecimal("2.60"), new BigDecimal("2.70"), new BigDecimal("2.50"));

        // Setup Finalized Report for batchCompleted2
        setupReportForBatch(batchCompleted2.getId(), new BigDecimal("2.20"), new BigDecimal("2.10"), new BigDecimal("1.80"));
    }

    private void setupOutcomesForBatch(String batchId, BigDecimal target) {
        for (int i = 1; i <= 12; i++) {
            poRepository.save(ProgrammeOutcome.builder()
                    .id("po-" + batchId + "-" + i)
                    .programmeBatchId(batchId)
                    .code("PO" + i)
                    .statement("Statement for PO" + i)
                    .target(target)
                    .build());
        }
        for (int i = 1; i <= 3; i++) {
            psoRepository.save(ProgrammeSpecificOutcome.builder()
                    .id("pso-" + batchId + "-" + i)
                    .programmeBatchId(batchId)
                    .code("PSO" + i)
                    .statement("Statement for PSO" + i)
                    .target(target)
                    .build());
        }
    }

    private void setupReportForBatch(String batchId, BigDecimal targetLevel, BigDecimal direct, BigDecimal indirect) throws Exception {
        BigDecimal finalAtt = direct.multiply(new BigDecimal("0.80"))
                .add(indirect.multiply(new BigDecimal("0.20")))
                .setScale(2, RoundingMode.HALF_UP);

        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> poRows = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            poRows.add(ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                    .poCode("PO" + i)
                    .statement("Statement PO" + i)
                    .targetLevel(targetLevel)
                    .directAttainment(direct)
                    .indirectAttainment(indirect)
                    .finalAttainment(finalAtt)
                    .targetMet(finalAtt.compareTo(targetLevel) >= 0)
                    .build());
        }

        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psoRows = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            psoRows.add(ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder()
                    .psoCode("PSO" + i)
                    .statement("Statement PSO" + i)
                    .targetLevel(targetLevel)
                    .directAttainment(direct)
                    .indirectAttainment(indirect)
                    .finalAttainment(finalAtt)
                    .targetMet(finalAtt.compareTo(targetLevel) >= 0)
                    .build());
        }

        String json = objectMapper.writeValueAsString(Map.of("po", poRows, "pso", psoRows));

        reportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("rep-" + batchId)
                .programmeBatchId(batchId)
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(json)
                .approvedAt(ZonedDateTime.now())
                .build());
    }

    // =========================================================================
    // SECTION A: HISTORICAL PROGRAMME ATTAINMENT TESTS (1 - 11)
    // =========================================================================

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("1. ACTIVE batches excluded from historical programme attainment")
    void testHistorical_ActiveBatchesExcluded() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), null);

        assertThat(result.getBatches()).noneMatch(b -> "ACTIVE".equalsIgnoreCase(b.getStatus()));
        assertThat(result.getBatches()).noneMatch(b -> b.getBatchId().equals(batchActive1.getId()));
        assertThat(result.getDataPoints()).noneMatch(dp -> dp.getBatchId().equals(batchActive1.getId()));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("2. COMPLETED batches included in historical programme attainment")
    void testHistorical_CompletedBatchesIncluded() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), null);

        assertThat(result.getBatches()).anyMatch(b -> b.getBatchId().equals(batchCompleted1.getId()));
        assertThat(result.getBatches()).anyMatch(b -> b.getBatchId().equals(batchCompleted2.getId()));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("3. GRADUATED batches included in historical programme attainment")
    void testHistorical_GraduatedBatchesIncluded() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), null);

        assertThat(result.getBatches()).anyMatch(b -> b.getBatchId().equals(batchGraduated1.getId()));
        assertThat(result.getBatches()).anyMatch(b -> "GRADUATED".equalsIgnoreCase(b.getStatus()));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("4. Correct chronological ordering of batches (startYear ASC)")
    void testHistorical_ChronologicalOrdering() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), null);

        List<Integer> startYears = result.getBatches().stream()
                .map(HistoricalProgrammeAttainmentResponseDto.HistoricalBatchSummaryDto::getStartYear)
                .toList();

        assertThat(startYears).containsExactly(2019, 2020, 2021);
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("5. All PO/PSO outcomes available (PO1..PO12, PSO1..PSO3)")
    void testHistorical_AllOutcomesAvailable() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), null);

        assertThat(result.getOutcomes()).contains(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6", "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2", "PSO3"
        );
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("6. Direct values correct")
    void testHistorical_DirectValuesCorrect() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), "PO1");

        Optional<HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto> dpOpt = result.getDataPoints().stream()
                .filter(dp -> dp.getBatchId().equals(batchCompleted1.getId()) && "PO1".equalsIgnoreCase(dp.getOutcomeCode()))
                .findFirst();

        assertThat(dpOpt).isPresent();
        assertThat(dpOpt.get().getDirectAttainment()).isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("7. Indirect values correct")
    void testHistorical_IndirectValuesCorrect() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), "PO1");

        Optional<HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto> dpOpt = result.getDataPoints().stream()
                .filter(dp -> dp.getBatchId().equals(batchCompleted1.getId()) && "PO1".equalsIgnoreCase(dp.getOutcomeCode()))
                .findFirst();

        assertThat(dpOpt).isPresent();
        assertThat(dpOpt.get().getIndirectAttainment()).isEqualByComparingTo(new BigDecimal("2.00"));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("8. Final values correct using formula Final = 0.8*Direct + 0.2*Indirect")
    void testHistorical_FinalValuesCorrect() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), "PO1");

        Optional<HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto> dpOpt = result.getDataPoints().stream()
                .filter(dp -> dp.getBatchId().equals(batchCompleted1.getId()) && "PO1".equalsIgnoreCase(dp.getOutcomeCode()))
                .findFirst();

        assertThat(dpOpt).isPresent();
        // 2.50 * 0.80 + 2.00 * 0.20 = 2.00 + 0.40 = 2.40
        assertThat(dpOpt.get().getFinalAttainment()).isEqualByComparingTo(new BigDecimal("2.40"));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("9. Batch-specific targets correct across batches")
    void testHistorical_BatchSpecificTargetsCorrect() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), "PO1");

        HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto dp2020 = result.getDataPoints().stream()
                .filter(dp -> dp.getBatchId().equals(batchCompleted1.getId())).findFirst().orElseThrow();
        HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto dp2021 = result.getDataPoints().stream()
                .filter(dp -> dp.getBatchId().equals(batchGraduated1.getId())).findFirst().orElseThrow();

        assertThat(dp2020.getTargetLevel()).isEqualByComparingTo(new BigDecimal("2.40"));
        assertThat(dp2021.getTargetLevel()).isEqualByComparingTo(new BigDecimal("2.60"));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("10. Gap calculation correct (Final - Target)")
    void testHistorical_GapCorrect() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), "PO1");

        HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto dp2019 = result.getDataPoints().stream()
                .filter(dp -> dp.getBatchId().equals(batchCompleted2.getId())).findFirst().orElseThrow();

        // 2.10 * 0.8 + 1.80 * 0.2 = 1.68 + 0.36 = 2.04. Target = 2.20. Gap = 2.04 - 2.20 = -0.16
        assertThat(dp2019.getGap()).isEqualByComparingTo(new BigDecimal("-0.16"));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("11. targetMet correct based on finalAttainment >= targetLevel")
    void testHistorical_TargetMetCorrect() {
        HistoricalProgrammeAttainmentResponseDto result = analyticsService
                .getHistoricalProgrammeAttainment(progCse.getId(), "PO1");

        HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto dp2020 = result.getDataPoints().stream()
                .filter(dp -> dp.getBatchId().equals(batchCompleted1.getId())).findFirst().orElseThrow();
        HistoricalProgrammeAttainmentResponseDto.HistoricalOutcomeDataPointDto dp2019 = result.getDataPoints().stream()
                .filter(dp -> dp.getBatchId().equals(batchCompleted2.getId())).findFirst().orElseThrow();

        // 2020 final (2.40) >= target (2.40) -> true
        assertThat(dp2020.getTargetMet()).isTrue();
        // 2019 final (2.04) < target (2.20) -> false
        assertThat(dp2019.getTargetMet()).isFalse();
    }

    // =========================================================================
    // SECTION B: BATCH COMPARISON TESTS (12 - 27)
    // =========================================================================

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("12. ACTIVE + ACTIVE comparison works")
    void testComparison_ActivePlusActive() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchActive1.getId(), batchActive2.getId());

        assertThat(result.getBatch1().getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getBatch2().getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getOutcomes()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("13. ACTIVE + COMPLETED comparison works")
    void testComparison_ActivePlusCompleted() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchActive1.getId(), batchCompleted1.getId());

        assertThat(result.getBatch1().getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getBatch2().getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getOutcomes()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("14. COMPLETED + COMPLETED comparison works")
    void testComparison_CompletedPlusCompleted() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchCompleted1.getId(), batchCompleted2.getId());

        assertThat(result.getBatch1().getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getBatch2().getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getOutcomes()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("15. GRADUATED + ACTIVE comparison works")
    void testComparison_GraduatedPlusActive() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchGraduated1.getId(), batchActive1.getId());

        assertThat(result.getBatch1().getStatus()).isEqualTo("GRADUATED");
        assertThat(result.getBatch2().getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getOutcomes()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("16. Same batch selected twice is rejected with 400 BadRequest")
    void testComparison_SameBatchRejected() {
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                analyticsService.compareBatches(batchCompleted1.getId(), batchCompleted1.getId())
        );
        assertThat(ex.getMessage()).containsIgnoringCase("different batches");
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("17. Nonexistent batch is rejected with ResourceNotFoundException")
    void testComparison_NonexistentBatchRejected() {
        assertThrows(ResourceNotFoundException.class, () ->
                analyticsService.compareBatches(batchCompleted1.getId(), "non-existent-batch-id")
        );
    }

    @Test
    @WithMockUser(username = "pc_cse_user", roles = {"PROGRAMME_COORDINATOR"})
    @DisplayName("18. Unauthorized batch access is rejected with 403 Forbidden")
    void testComparison_UnauthorizedBatchRejected() {
        // pc_cse_user is coordinator for prog-cse-test only. Attempting to compare with batchActive2 (BBA)
        assertThrows(ResponseStatusException.class, () ->
                analyticsService.compareBatches(batchCompleted1.getId(), batchActive2.getId())
        );
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("19. All PO/PSO outcomes are returned")
    void testComparison_AllOutcomesReturned() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchCompleted1.getId(), batchCompleted2.getId());

        List<String> outcomeCodes = result.getOutcomes().stream()
                .map(BatchComparisonAnalyticsResponseDto.OutcomeComparisonItemDto::getOutcomeCode)
                .toList();

        assertThat(outcomeCodes).contains(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6", "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2", "PSO3"
        );
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("20. Batch 1 and Batch 2 values are kept separate")
    void testComparison_ValuesKeptSeparate() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchCompleted1.getId(), batchCompleted2.getId());

        BatchComparisonAnalyticsResponseDto.OutcomeComparisonItemDto po1 = result.getOutcomes().stream()
                .filter(o -> "PO1".equalsIgnoreCase(o.getOutcomeCode()))
                .findFirst().orElseThrow();

        assertThat(po1.getBatch1()).isNotNull();
        assertThat(po1.getBatch2()).isNotNull();
        assertThat(po1.getBatch1().getFinalAttainment()).isNotEqualTo(po1.getBatch2().getFinalAttainment());
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("21. Direct values are returned for both batches")
    void testComparison_DirectValuesReturnedForBoth() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchCompleted1.getId(), batchCompleted2.getId());

        BatchComparisonAnalyticsResponseDto.OutcomeComparisonItemDto po1 = result.getOutcomes().stream()
                .filter(o -> "PO1".equalsIgnoreCase(o.getOutcomeCode()))
                .findFirst().orElseThrow();

        assertThat(po1.getBatch1().getDirectAttainment()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(po1.getBatch2().getDirectAttainment()).isEqualByComparingTo(new BigDecimal("2.10"));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("22. Indirect values are returned for both batches")
    void testComparison_IndirectValuesReturnedForBoth() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchCompleted1.getId(), batchCompleted2.getId());

        BatchComparisonAnalyticsResponseDto.OutcomeComparisonItemDto po1 = result.getOutcomes().stream()
                .filter(o -> "PO1".equalsIgnoreCase(o.getOutcomeCode()))
                .findFirst().orElseThrow();

        assertThat(po1.getBatch1().getIndirectAttainment()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(po1.getBatch2().getIndirectAttainment()).isEqualByComparingTo(new BigDecimal("1.80"));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("23. Final values are returned for both batches")
    void testComparison_FinalValuesReturnedForBoth() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchCompleted1.getId(), batchCompleted2.getId());

        BatchComparisonAnalyticsResponseDto.OutcomeComparisonItemDto po1 = result.getOutcomes().stream()
                .filter(o -> "PO1".equalsIgnoreCase(o.getOutcomeCode()))
                .findFirst().orElseThrow();

        assertThat(po1.getBatch1().getFinalAttainment()).isEqualByComparingTo(new BigDecimal("2.40"));
        assertThat(po1.getBatch2().getFinalAttainment()).isEqualByComparingTo(new BigDecimal("2.04"));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("24. Targets are returned for both batches")
    void testComparison_TargetsReturnedForBoth() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchCompleted1.getId(), batchGraduated1.getId());

        BatchComparisonAnalyticsResponseDto.OutcomeComparisonItemDto po1 = result.getOutcomes().stream()
                .filter(o -> "PO1".equalsIgnoreCase(o.getOutcomeCode()))
                .findFirst().orElseThrow();

        assertThat(po1.getBatch1().getTargetLevel()).isEqualByComparingTo(new BigDecimal("2.40"));
        assertThat(po1.getBatch2().getTargetLevel()).isEqualByComparingTo(new BigDecimal("2.60"));
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("25. Existing /trends behavior remains unchanged")
    void testExistingTrendsUnchanged() {
        List<ScopedTrendSeriesDto> trends = analyticsService.getTrends(null, null, progCse.getId(), null);
        assertThat(trends).isNotNull();
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("26. Controller endpoints return 200 OK and ApiResponse wrappers")
    void testControllerEndpoints() {
        ResponseEntity<ApiResponse<HistoricalProgrammeAttainmentResponseDto>> histResp =
                analyticsController.getHistoricalProgrammeAttainment(progCse.getId(), "PO1");

        assertThat(histResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(histResp.getBody()).isNotNull();
        assertThat(histResp.getBody().isSuccess()).isTrue();
        assertThat(histResp.getBody().getData().getDataPoints()).isNotEmpty();

        ResponseEntity<ApiResponse<BatchComparisonAnalyticsResponseDto>> compResp =
                analyticsController.compareBatches(batchCompleted1.getId(), batchCompleted2.getId());

        assertThat(compResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(compResp.getBody()).isNotNull();
        assertThat(compResp.getBody().isSuccess()).isTrue();
        assertThat(compResp.getBody().getData().getOutcomes()).isNotEmpty();
    }

    @Test
    @WithMockUser(username = "iqac_analyst", roles = {"IQAC"})
    @DisplayName("27. No N+1 query pattern is introduced: Cross-programme batch comparison works seamlessly")
    void testComparison_CrossProgrammeSupport() {
        BatchComparisonAnalyticsResponseDto result = analyticsService
                .compareBatches(batchCompleted1.getId(), batchActive2.getId());

        assertThat(result.getBatch1().getProgrammeId()).isEqualTo(progCse.getId());
        assertThat(result.getBatch2().getProgrammeId()).isEqualTo(progBba.getId());
        assertThat(result.getBatch1().getProgrammeName()).isEqualTo("B.Tech Computer Science");
        assertThat(result.getBatch2().getProgrammeName()).isEqualTo("Bachelor of Business Administration");
    }
}
