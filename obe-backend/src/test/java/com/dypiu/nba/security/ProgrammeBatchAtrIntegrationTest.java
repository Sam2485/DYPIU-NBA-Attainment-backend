package com.dypiu.nba.security;

import com.dypiu.nba.controller.AcademicController;
import com.dypiu.nba.controller.AtrController;
import com.dypiu.nba.controller.ProgrammeBatchController;
import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.ProgrammeAtrReportDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ProgrammeBatchAtrIntegrationTest {

    @Autowired
    private AcademicController academicController;

    @Autowired
    private ProgrammeBatchController programmeBatchController;

    @Autowired
    private AtrController atrController;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private MasterProgrammeRepository masterProgrammeRepository;

    @Autowired
    private ProgrammeBatchRepository programmeBatchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProgrammeOutcomeRepository programmeOutcomeRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;

    @Autowired
    private ProgrammeAtrRepository programmeAtrRepository;

    private School schoolA;
    private School schoolB;
    private Department deptA;
    private Department deptB;
    private MasterProgramme progA;
    private MasterProgramme progB;
    private ProgrammeBatch batchA1;
    private ProgrammeBatch batchA2;
    private ProgrammeBatch batchB1;
    private ProgrammeBatch batchDeleted;
    private User pcAlice;
    private User pcBob;

    @BeforeEach
    void setUp() {
        String uid = UUID.randomUUID().toString().substring(0, 6);

        schoolA = schoolRepository.save(School.builder()
                .id("sch-a-" + uid)
                .code("SCH_A_" + uid)
                .name("School A " + uid)
                .build());

        schoolB = schoolRepository.save(School.builder()
                .id("sch-b-" + uid)
                .code("SCH_B_" + uid)
                .name("School B " + uid)
                .build());

        deptA = departmentRepository.save(Department.builder()
                .id("dept-a-" + uid)
                .schoolId(schoolA.getId())
                .code("CSE_A_" + uid)
                .name("Computer Science A " + uid)
                .build());

        deptB = departmentRepository.save(Department.builder()
                .id("dept-b-" + uid)
                .schoolId(schoolB.getId())
                .code("CSE_B_" + uid)
                .name("Computer Science B " + uid)
                .build());

        progA = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-a-" + uid)
                .departmentId(deptA.getId())
                .code("BT_CS_A_" + uid)
                .name("B.Tech CSE A " + uid)
                .status("ACTIVE")
                .build());

        progB = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-b-" + uid)
                .departmentId(deptB.getId())
                .code("BT_CS_B_" + uid)
                .name("B.Tech CSE B " + uid)
                .status("ACTIVE")
                .build());

        pcAlice = userRepository.save(User.builder()
                .username("pc.alice." + uid)
                .email("alice." + uid + "@dypiu.ac.in")
                .name("Alice PC")
                .passwordHash("dummy_hashed_password")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId(schoolA.getId())
                .departmentId(deptA.getId())
                .masterProgrammeId(progA.getId())
                .isActive(true)
                .build());

        pcBob = userRepository.save(User.builder()
                .username("pc.bob." + uid)
                .email("bob." + uid + "@dypiu.ac.in")
                .name("Bob PC")
                .passwordHash("dummy_hashed_password")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId(schoolB.getId())
                .departmentId(deptB.getId())
                .masterProgrammeId(progB.getId())
                .isActive(true)
                .build());

        // Previous batch (2022-2026)
        batchA1 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-a1-" + uid)
                .masterProgrammeId(progA.getId())
                .coordinatorId(pcAlice.getId())
                .coordinatorName(pcAlice.getName())
                .coordinatorEmail(pcAlice.getEmail())
                .name("2022-2026")
                .startYear(2022)
                .endYear(2026)
                .status("ACTIVE")
                .build());

        // Current batch (2023-2027)
        batchA2 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-a2-" + uid)
                .masterProgrammeId(progA.getId())
                .coordinatorId(pcAlice.getId())
                .coordinatorName(pcAlice.getName())
                .coordinatorEmail(pcAlice.getEmail())
                .name("2023-2027")
                .startYear(2023)
                .endYear(2027)
                .status("ACTIVE")
                .build());

        batchB1 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-b1-" + uid)
                .masterProgrammeId(progB.getId())
                .coordinatorId(pcBob.getId())
                .coordinatorName(pcBob.getName())
                .coordinatorEmail(pcBob.getEmail())
                .name("2023-2027")
                .startYear(2023)
                .endYear(2027)
                .status("ACTIVE")
                .build());

        batchDeleted = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-del-" + uid)
                .masterProgrammeId(progA.getId())
                .name("2021-2025")
                .startYear(2021)
                .endYear(2025)
                .status("INACTIVE")
                .deletedAt(ZonedDateTime.now())
                .deletedBy("admin")
                .build());

        // Setup defined POs and PSOs for progA
        programmeOutcomeRepository.save(ProgrammeOutcome.builder()
                .id("po1-" + uid)
                .programmeBatchId(progA.getId())
                .code("PO1")
                .statement("Engineering Knowledge")
                .target(new BigDecimal("2.50"))
                .build());

        programmeOutcomeRepository.save(ProgrammeOutcome.builder()
                .id("po2-" + uid)
                .programmeBatchId(progA.getId())
                .code("PO2")
                .statement("Problem Analysis")
                .target(new BigDecimal("2.40"))
                .build());

        programmeSpecificOutcomeRepository.save(ProgrammeSpecificOutcome.builder()
                .id("pso1-" + uid)
                .programmeBatchId(progA.getId())
                .code("PSO1")
                .statement("Software Development")
                .target(new BigDecimal("2.60"))
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("1. GET returns HTTP 200 DRAFT ATR when no ATR, attainment, or targets exist yet")
    void testGetInitialDraftProgrammeAtr() {
        authenticateAs(pcAlice);

        ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> response =
                academicController.getProgrammeBatchAtr(batchA2.getId());

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());

        ProgrammeAtrReportDto data = response.getBody().getData();
        assertNotNull(data);
        assertEquals("PROGRAMME_ATR", data.getReportType());
        assertNull(data.getProgrammeAtrId(), "programmeAtrId must be null for non-persisted initial draft");
        assertEquals("DRAFT", data.getStatus());

        assertEquals(progA.getId(), data.getProgramme().getId());
        assertEquals(progA.getCode(), data.getProgramme().getCode());
        assertEquals(batchA2.getId(), data.getBatch().getId());
        assertEquals("2023", data.getBatch().getStartYear());
        assertEquals("2027", data.getBatch().getEndYear());

        assertNotNull(data.getPoOutcomes());
        assertEquals(2, data.getPoOutcomes().size());
        assertEquals("PO1", data.getPoOutcomes().get(0).getOutcomeCode());
        assertEquals("Engineering Knowledge", data.getPoOutcomes().get(0).getOutcomeStatement());
        assertEquals(0, new BigDecimal("2.50").compareTo(data.getPoOutcomes().get(0).getTargetLevel()));
        assertEquals("PO2", data.getPoOutcomes().get(1).getOutcomeCode());
        assertEquals("Problem Analysis", data.getPoOutcomes().get(1).getOutcomeStatement());
        assertEquals(0, new BigDecimal("2.40").compareTo(data.getPoOutcomes().get(1).getTargetLevel()));

        for (ProgrammeAtrReportDto.OutcomeRow po : data.getPoOutcomes()) {
            assertEquals(0, po.getAttainmentLevel().compareTo(BigDecimal.ZERO));
            assertEquals(0, po.getAchievementPercentage().compareTo(BigDecimal.ZERO));
            assertEquals("", po.getObservation());
            assertNotNull(po.getActions());
            assertTrue(po.getActions().isEmpty());
            assertNotNull(po.getTargetLevel());
        }

        assertNotNull(data.getPsoOutcomes());
        assertEquals(1, data.getPsoOutcomes().size());
        assertEquals("PSO1", data.getPsoOutcomes().get(0).getOutcomeCode());
        assertEquals("Software Development", data.getPsoOutcomes().get(0).getOutcomeStatement());
        assertEquals(0, new BigDecimal("2.60").compareTo(data.getPsoOutcomes().get(0).getTargetLevel()));

        for (ProgrammeAtrReportDto.OutcomeRow pso : data.getPsoOutcomes()) {
            assertEquals(0, pso.getAttainmentLevel().compareTo(BigDecimal.ZERO));
            assertEquals(0, pso.getAchievementPercentage().compareTo(BigDecimal.ZERO));
            assertEquals("", pso.getObservation());
            assertNotNull(pso.getActions());
            assertTrue(pso.getActions().isEmpty());
            assertNotNull(pso.getTargetLevel());
        }
    }

    @Test
    @DisplayName("2. POST saves Programme ATR draft with observations, actions, and targets")
    void testSaveProgrammeBatchAtr() {
        authenticateAs(pcAlice);

        ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> getInitial =
                academicController.getProgrammeBatchAtr(batchA2.getId());
        ProgrammeAtrReportDto draft = getInitial.getBody().getData();

        draft.getPoOutcomes().get(0).setObservation("Strong engineering foundation observed.");
        draft.getPoOutcomes().get(0).setActions(List.of("Continue standard lab assignments", "Add advanced problem sets"));

        ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> saveResponse =
                academicController.saveProgrammeBatchAtr(batchA2.getId(), draft);

        assertNotNull(saveResponse);
        assertEquals(200, saveResponse.getStatusCode().value());
        assertTrue(saveResponse.getBody().isSuccess());

        ProgrammeAtrReportDto saved = saveResponse.getBody().getData();
        assertNotNull(saved);
        assertNotNull(saved.getProgrammeAtrId(), "Saved ATR must have generated ID");
        assertEquals("Strong engineering foundation observed.", saved.getPoOutcomes().get(0).getObservation());
        assertEquals(2, saved.getPoOutcomes().get(0).getActions().size());

        // Verify fetching again returns the saved draft
        ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> getSaved =
                academicController.getProgrammeBatchAtr(batchA2.getId());
        assertEquals(saved.getProgrammeAtrId(), getSaved.getBody().getData().getProgrammeAtrId());
        assertEquals("Strong engineering foundation observed.", getSaved.getBody().getData().getPoOutcomes().get(0).getObservation());
    }

    @Test
    @DisplayName("3. POST submit transitions status to SUBMITTED_FOR_VERIFICATION with authenticated submitter")
    void testSubmitProgrammeBatchAtr() {
        authenticateAs(pcAlice);

        ResponseEntity<ApiResponse<ProgrammeAtr>> submitResponse =
                academicController.submitProgrammeBatchAtr(batchA2.getId(), () -> pcAlice.getUsername());

        assertNotNull(submitResponse);
        assertEquals(200, submitResponse.getStatusCode().value());
        assertTrue(submitResponse.getBody().isSuccess());

        ProgrammeAtr atr = submitResponse.getBody().getData();
        assertNotNull(atr);
        assertEquals(ProgrammeAtrStatus.SUBMITTED_FOR_VERIFICATION, atr.getStatus());
        assertEquals(pcAlice.getEmail(), atr.getSubmittedBy());
        assertNotNull(atr.getSubmittedAt());

        // Verify GET report reflects submitted status
        ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> getResponse =
                academicController.getProgrammeBatchAtr(batchA2.getId());
        assertEquals("SUBMITTED_FOR_VERIFICATION", getResponse.getBody().getData().getStatus());

        // Attempting to modify submitted ATR returns 409 Conflict
        ProgrammeAtrReportDto modifyAttempt = getResponse.getBody().getData();
        modifyAttempt.getPoOutcomes().get(0).setObservation("Hacked observation while locked");
        ResponseStatusException lockEx = assertThrows(ResponseStatusException.class, () ->
                academicController.saveProgrammeBatchAtr(batchA2.getId(), modifyAttempt)
        );
        assertEquals(409, lockEx.getStatusCode().value());
    }

    @Test
    @DisplayName("4. Dynamic PO/PSO reflection in DRAFT state when DB changes occur after save")
    void testDynamicReflectionInDraftState() {
        authenticateAs(pcAlice);

        // 1. Initial GET and Save Draft
        ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> initialGet =
                academicController.getProgrammeBatchAtr(batchA1.getId());
        ProgrammeAtrReportDto draft = initialGet.getBody().getData();
        draft.getPoOutcomes().get(0).setObservation("Draft observation for PO1");
        academicController.saveProgrammeBatchAtr(batchA1.getId(), draft);

        // 2. Update PO1 statement and target in DB table
        ProgrammeOutcome po1Db = programmeOutcomeRepository.findAll().stream()
                .filter(p -> p.getCode().equals("PO1") && p.getProgrammeBatchId().equals(progA.getId()))
                .findFirst().orElseThrow();
        po1Db.setStatement("Updated Engineering Fundamentals Statement");
        po1Db.setTarget(new BigDecimal("2.95"));
        programmeOutcomeRepository.save(po1Db);

        // 3. GET should dynamically reflect updated statement & target from DB while preserving observation
        ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> refreshedGet =
                academicController.getProgrammeBatchAtr(batchA1.getId());
        ProgrammeAtrReportDto refreshedDto = refreshedGet.getBody().getData();
        assertEquals("DRAFT", refreshedDto.getStatus());
        assertEquals("Updated Engineering Fundamentals Statement", refreshedDto.getPoOutcomes().get(0).getOutcomeStatement());
        assertEquals(0, new BigDecimal("2.95").compareTo(refreshedDto.getPoOutcomes().get(0).getTargetLevel()));
        assertEquals("Draft observation for PO1", refreshedDto.getPoOutcomes().get(0).getObservation());
    }

    @Test
    @DisplayName("5. GET previous-year ATR resolves historical preceding batch")
    void testGetPreviousYearProgrammeAtr() {
        authenticateAs(pcAlice);

        // Save an ATR on the previous batch (2022-2026)
        ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> getInitialA1 =
                academicController.getProgrammeBatchAtr(batchA1.getId());
        ProgrammeAtrReportDto draftA1 = getInitialA1.getBody().getData();
        draftA1.getPoOutcomes().get(0).setObservation("Historical 2022 batch observation");
        academicController.saveProgrammeBatchAtr(batchA1.getId(), draftA1);

        // Fetch previous year from current batch A2 (2023-2027)
        ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> prevYearResponse =
                atrController.getPreviousYearProgrammeAtrReport(batchA2.getId());

        assertNotNull(prevYearResponse);
        assertEquals(200, prevYearResponse.getStatusCode().value());
        assertTrue(prevYearResponse.getBody().isSuccess());

        ProgrammeAtrReportDto prevDto = prevYearResponse.getBody().getData();
        assertNotNull(prevDto);
        assertEquals(batchA1.getId(), prevDto.getBatch().getId());
        assertEquals("Historical 2022 batch observation", prevDto.getPoOutcomes().get(0).getObservation());
    }

    @Test
    @DisplayName("5. Out-of-scope coordinator receives HTTP 403 Forbidden")
    void testOutOfScopeCoordinatorAccessRejected() {
        authenticateAs(pcAlice);

        // Alice tries to access Bob's batch in School B
        ResponseStatusException exGet = assertThrows(ResponseStatusException.class, () ->
                academicController.getProgrammeBatchAtr(batchB1.getId())
        );
        assertEquals(403, exGet.getStatusCode().value());

        // Alice tries to submit Bob's batch
        ResponseStatusException exSubmit = assertThrows(ResponseStatusException.class, () ->
                academicController.submitProgrammeBatchAtr(batchB1.getId(), () -> pcAlice.getUsername())
        );
        assertEquals(403, exSubmit.getStatusCode().value());
    }

    @Test
    @DisplayName("6. Invalid or soft-deleted batch ID returns HTTP 404")
    void testInvalidOrDeletedBatchReturns404() {
        authenticateAs(pcAlice);

        assertThrows(ResourceNotFoundException.class, () ->
                academicController.getProgrammeBatchAtr("batch-non-existent-12345")
        );

        assertThrows(ResourceNotFoundException.class, () ->
                academicController.getProgrammeBatchAtr(batchDeleted.getId())
        );
    }
}
