package com.dypiu.nba.service;

import com.dypiu.nba.dto.ProgrammeCoordinatorSetupProgressDto;
import com.dypiu.nba.dto.ProgrammeTargetDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class Phase6ProgrammeCoordinatorIntegrationTest {

    @Autowired
    private AcademicService academicService;

    @Autowired
    private OutcomeService outcomeService;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MasterProgrammeRepository masterProgrammeRepository;

    @Autowired
    private ProgrammeBatchRepository programmeBatchRepository;

    @Autowired
    private MasterCourseRepository masterCourseRepository;

    @Autowired
    private ProgrammeOutcomeRepository programmeOutcomeRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;

    @Autowired
    private ApprovalRequestRepository approvalRequestRepository;

    private String progId1;
    private String progId2;
    private String batchId1;
    private String batchId2;
    private final String coordinatorEmail = "pc.test@dypiu.ac.in";

    @BeforeEach
    void setUp() {
        School school = schoolRepository.save(School.builder()
                .id("sch-pc-test-" + UUID.randomUUID().toString().substring(0, 6))
                .name("School of Computing PC Test")
                .code("SOCT-" + UUID.randomUUID().toString().substring(0, 4))
                .build());

        Department dept = departmentRepository.save(Department.builder()
                .id("dept-pc-test-" + UUID.randomUUID().toString().substring(0, 6))
                .name("Computer Science Department PC Test")
                .code("CS-PCT")
                .schoolId(school.getId())
                .build());

        MasterProgramme prog1 = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-pct-1-" + UUID.randomUUID().toString().substring(0, 6))
                .name("B.Tech Computer Science PCT 1")
                .code("BT-CS-1")
                .departmentId(dept.getId())
                .coordinator("Test PC")
                .coordinatorEmail(coordinatorEmail)
                .durationYears(4)
                .build());
        progId1 = prog1.getId();

        MasterProgramme prog2 = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-pct-2-" + UUID.randomUUID().toString().substring(0, 6))
                .name("B.Tech AI & Data Science PCT 2")
                .code("BT-AI-2")
                .departmentId(dept.getId())
                .coordinator("Test PC")
                .coordinatorEmail(coordinatorEmail)
                .durationYears(4)
                .build());
        progId2 = prog2.getId();

        ProgrammeBatch b1 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-pct-1-" + UUID.randomUUID().toString().substring(0, 6))
                .name("2022-2026 ProgrammeBatch A")
                .masterProgrammeId(progId1)
                .startYear(2022)
                .endYear(2026)
                .status("ACTIVE")
                .build());
        batchId1 = b1.getId();

        ProgrammeBatch b2 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-pct-2-" + UUID.randomUUID().toString().substring(0, 6))
                .name("2023-2027 ProgrammeBatch B")
                .masterProgrammeId(progId1)
                .startYear(2023)
                .endYear(2027)
                .status("ACTIVE")
                .build());
        batchId2 = b2.getId();
    }

    @Test
    @DisplayName("TEST A: Initial Workflow State & ProgrammeBatch Isolation")
    void testInitialWorkflowAndBatchIsolation() {
        ProgrammeCoordinatorSetupProgressDto progressB1 = academicService.getProgrammeCoordinatorSetupProgress(coordinatorEmail, progId1, batchId1);
        assertNotNull(progressB1);
        assertEquals(progId1, progressB1.getProgrammeId());
        assertEquals(batchId1, progressB1.getBatchId());
        assertEquals(0, progressB1.getCurrentStep());
        assertTrue(progressB1.getCompletedSteps().isEmpty());

        // Update progress on ProgrammeBatch 1 (Step 0 complete)
        academicService.updateProgrammeCoordinatorSetupProgress(coordinatorEmail, progId1, batchId1, 1, Map.of("completedSteps", List.of("0")));

        // Verify ProgrammeBatch 1 progress updated
        ProgrammeCoordinatorSetupProgressDto updatedB1 = academicService.getProgrammeCoordinatorSetupProgress(coordinatorEmail, progId1, batchId1);
        assertTrue(updatedB1.getCompletedSteps().contains("0"));

        // Verify ProgrammeBatch 2 progress remains unaffected (ProgrammeBatch Isolation)
        ProgrammeCoordinatorSetupProgressDto progressB2 = academicService.getProgrammeCoordinatorSetupProgress(coordinatorEmail, progId1, batchId2);
        assertFalse(progressB2.getCompletedSteps().contains("0"), "ProgrammeBatch 2 must not inherit ProgrammeBatch 1 completed steps");
    }

    @Test
    @DisplayName("TEST B: Non-Sequential Step Completion (Step 0 -> Step 2)")
    void testNonSequentialStepCompletion() {
        // Complete Step 0
        academicService.updateProgrammeCoordinatorSetupProgress(coordinatorEmail, progId1, batchId1, 1, Map.of("completedSteps", List.of("0")));

        // Jump directly to Step 2 (MasterProgramme ATR) and complete it without completing Step 1 (Targets)
        academicService.updateProgrammeCoordinatorSetupProgress(coordinatorEmail, progId1, batchId1, 3, Map.of("completedSteps", List.of("0", "2")));

        ProgrammeCoordinatorSetupProgressDto progress = academicService.getProgrammeCoordinatorSetupProgress(coordinatorEmail, progId1, batchId1);
        assertTrue(progress.getCompletedSteps().contains("0"), "Step 0 must be completed");
        assertFalse(progress.getCompletedSteps().contains("1"), "Step 1 must remain pending");
        assertTrue(progress.getCompletedSteps().contains("2"), "Step 2 must be completed");
        assertTrue(progress.getPendingSteps().contains("1"), "Pending steps must explicitly contain Step 1");
    }

    @Test
    @DisplayName("TEST C: Step 1 MasterCourse Allocation - Save (Draft) vs Submit Boundary")
    void testStep1AllocationDraftVsSubmitBoundary() {
        MasterCourse c1 = masterCourseRepository.save(MasterCourse.builder()
                .id("crs-test-1-" + UUID.randomUUID().toString().substring(0, 6))
                .code("CS301")
                .name("Data Structures")
                .masterProgrammeId(progId1)
                .credits(3)
                .courseType("THEORY")
                .status("ACTIVE")
                .build());

        int initialApprovalCount = approvalRequestRepository.findByMasterProgrammeId(progId1).size();

        // 1. Save allocations as Draft (submit = false)
        List<Map<String, Object>> allocations = List.of(Map.of(
                "courseId", c1.getId(),
                "coordinator", "Prof. Alice",
                "coordinatorEmail", "alice@dypiu.ac.in"
        ));
        Map<String, Object> draftRes = academicService.allocateCourses(progId1, null, allocations, false);
        assertTrue((Boolean) draftRes.get("success"));

        // Verify NO approval request was created
        List<ApprovalRequest> afterDraftApprovals = approvalRequestRepository.findByMasterProgrammeId(progId1);
        assertEquals(initialApprovalCount, afterDraftApprovals.size(), "Draft save must NOT create ApprovalRequest");

        // 2. Explicit Submit (submit = true)
        Map<String, Object> submitRes = academicService.allocateCourses(progId1, null, allocations, true);
        assertTrue((Boolean) submitRes.get("success"));

        List<ApprovalRequest> afterSubmitApprovals = approvalRequestRepository.findByMasterProgrammeId(progId1);
        assertEquals(initialApprovalCount + 1, afterSubmitApprovals.size(), "Explicit submit must create ApprovalRequest");
        ApprovalRequest req = afterSubmitApprovals.get(afterSubmitApprovals.size() - 1);
        assertEquals(ApprovalType.COURSE_ALLOCATION, req.getType());
        assertEquals(ApprovalStatus.PENDING, req.getStatus());
    }

    @Test
    @DisplayName("TEST D: Step 2 PO/PSO Dynamic Dimension & Target Benchmarks Persistence")
    void testStep2TargetBenchmarksPersistence() {
        // Create 3 POs and 2 PSOs dynamically
        programmeOutcomeRepository.save(ProgrammeOutcome.builder().id("po-1-" + UUID.randomUUID().toString().substring(0, 4)).programmeBatchId(batchId1).code("PO1").statement("PO1 Stmt").build());
        programmeOutcomeRepository.save(ProgrammeOutcome.builder().id("po-2-" + UUID.randomUUID().toString().substring(0, 4)).programmeBatchId(batchId1).code("PO2").statement("PO2 Stmt").build());
        programmeOutcomeRepository.save(ProgrammeOutcome.builder().id("po-3-" + UUID.randomUUID().toString().substring(0, 4)).programmeBatchId(batchId1).code("PO3").statement("PO3 Stmt").build());
        programmeSpecificOutcomeRepository.save(ProgrammeSpecificOutcome.builder().id("pso-1-" + UUID.randomUUID().toString().substring(0, 4)).programmeBatchId(batchId1).code("PSO1").statement("PSO1 Stmt").build());
        programmeSpecificOutcomeRepository.save(ProgrammeSpecificOutcome.builder().id("pso-2-" + UUID.randomUUID().toString().substring(0, 4)).programmeBatchId(batchId1).code("PSO2").statement("PSO2 Stmt").build());

        Map<String, Object> outcomes = academicService.getConsolidatedOutcomes(progId1, batchId1);
        List<?> pos = (List<?>) outcomes.get("pos");
        List<?> psos = (List<?>) outcomes.get("psos");
        assertEquals(3, pos.size());
        assertEquals(2, psos.size());

        // Save targets via OutcomeService
        Map<String, BigDecimal> poTargets = Map.of("PO1", new BigDecimal("2.20"), "PO2", new BigDecimal("2.60"), "PO3", new BigDecimal("2.80"));
        Map<String, BigDecimal> psoTargets = Map.of("PSO1", new BigDecimal("2.40"), "PSO2", new BigDecimal("2.70"));
        outcomeService.saveProgrammeTargets(progId1, ProgrammeTargetDto.builder()
                .programmeId(progId1)
                .batchId(batchId1)
                .poTargets(poTargets)
                .psoTargets(psoTargets)
                .build());

        // Fetch back and verify
        ProgrammeTargetDto targetsDto = outcomeService.getProgrammeTargets(progId1);
        assertNotNull(targetsDto);
        assertEquals(new BigDecimal("2.20"), targetsDto.getPoTargets().get("PO1"));
        assertEquals(new BigDecimal("2.60"), targetsDto.getPoTargets().get("PO2"));
        assertEquals(new BigDecimal("2.40"), targetsDto.getPsoTargets().get("PSO1"));
    }

    @Test
    @DisplayName("TEST E: Complete Workflow Transition")
    void testCompleteWorkflowTransition() {
        ProgrammeCoordinatorSetupProgressDto completed = academicService.completeProgrammeCoordinatorSetup(coordinatorEmail, progId1, batchId1);
        assertNotNull(completed);
        assertEquals(SetupStepStatus.COMPLETED, completed.getOverallStatus());
        assertTrue(completed.getCompletedSteps().containsAll(List.of("0", "1", "2", "3")));
        assertTrue(completed.getPendingSteps().isEmpty());
    }
}
