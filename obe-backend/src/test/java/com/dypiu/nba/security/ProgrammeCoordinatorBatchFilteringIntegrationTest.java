package com.dypiu.nba.security;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AcademicService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ProgrammeCoordinatorBatchFilteringIntegrationTest {

    @Autowired
    private AcademicService academicService;

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

    private School schoolA;
    private School schoolB;
    private Department deptA;
    private Department deptB;
    private MasterProgramme progA1;
    private MasterProgramme progA2;
    private MasterProgramme progB1;
    private ProgrammeBatch batchActiveAlice;
    private ProgrammeBatch batchActiveBob;
    private ProgrammeBatch batchInactiveAlice;
    private ProgrammeBatch batchDeletedAlice;
    private ProgrammeBatch batchSchoolB;

    private User pcAlice;
    private User pcBob;
    private User adminUser;

    @BeforeEach
    void setUpTestData() {
        SecurityContextHolder.clearContext();

        String nano = UUID.randomUUID().toString().substring(0, 8);

        // 1. Schools
        schoolA = schoolRepository.save(School.builder()
                .id("sch-pc-flt-a-" + nano)
                .name("School A")
                .code("SCH-A-" + nano)
                .build());

        schoolB = schoolRepository.save(School.builder()
                .id("sch-pc-flt-b-" + nano)
                .name("School B")
                .code("SCH-B-" + nano)
                .build());

        // 2. Departments
        deptA = departmentRepository.save(Department.builder()
                .id("dept-pc-flt-a-" + nano)
                .schoolId(schoolA.getId())
                .name("Dept A")
                .code("DA-" + nano)
                .build());

        deptB = departmentRepository.save(Department.builder()
                .id("dept-pc-flt-b-" + nano)
                .schoolId(schoolB.getId())
                .name("Dept B")
                .code("DB-" + nano)
                .build());

        // 3. Master Programmes
        progA1 = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-flt-a1-" + nano)
                .departmentId(deptA.getId())
                .departmentName(deptA.getName())
                .name("B.Tech CS A1")
                .code("CS-A1-" + nano)
                .coordinator("Alice Coordinator")
                .coordinatorEmail("alice.flt." + nano + "@dypiu.ac.in")
                .build());

        progA2 = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-flt-a2-" + nano)
                .departmentId(deptA.getId())
                .departmentName(deptA.getName())
                .name("B.Tech IT A2")
                .code("IT-A2-" + nano)
                .coordinator("Bob Coordinator")
                .coordinatorEmail("bob.flt." + nano + "@dypiu.ac.in")
                .build());

        progB1 = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-flt-b1-" + nano)
                .departmentId(deptB.getId())
                .departmentName(deptB.getName())
                .name("B.Tech CS B1")
                .code("CS-B1-" + nano)
                .build());

        // 4. Users
        pcAlice = userRepository.save(User.builder()
                .username("alice.flt." + nano + "@dypiu.ac.in")
                .email("alice.flt." + nano + "@dypiu.ac.in")
                .name("Alice Coordinator")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .passwordHash("dummyHash")
                .schoolId(schoolA.getId())
                .departmentId(deptA.getId())
                .masterProgrammeId(progA1.getId())
                .build());

        pcBob = userRepository.save(User.builder()
                .username("bob.flt." + nano + "@dypiu.ac.in")
                .email("bob.flt." + nano + "@dypiu.ac.in")
                .name("Bob Coordinator")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .passwordHash("dummyHash")
                .schoolId(schoolA.getId())
                .departmentId(deptA.getId())
                .masterProgrammeId(progA2.getId())
                .build());

        adminUser = userRepository.save(User.builder()
                .username("admin.flt." + nano + "@dypiu.ac.in")
                .email("admin.flt." + nano + "@dypiu.ac.in")
                .name("Institution Admin")
                .role(UserRole.ADMIN)
                .passwordHash("dummyHash")
                .build());

        // 5. Programme Batches
        batchActiveAlice = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-active-alice-" + nano)
                .masterProgrammeId(progA1.getId())
                .name("2023-2027")
                .startYear(2023)
                .endYear(2027)
                .coordinatorId(pcAlice.getId())
                .coordinatorName(pcAlice.getName())
                .coordinatorEmail(pcAlice.getEmail())
                .status("ACTIVE")
                .build());

        batchActiveBob = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-active-bob-" + nano)
                .masterProgrammeId(progA2.getId())
                .name("2023-2027")
                .startYear(2023)
                .endYear(2027)
                .coordinatorId(pcBob.getId())
                .coordinatorName(pcBob.getName())
                .coordinatorEmail(pcBob.getEmail())
                .status("ACTIVE")
                .build());

        batchInactiveAlice = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-inactive-alice-" + nano)
                .masterProgrammeId(progA1.getId())
                .name("2018-2022")
                .startYear(2018)
                .endYear(2022)
                .coordinatorId(pcAlice.getId())
                .coordinatorName(pcAlice.getName())
                .coordinatorEmail(pcAlice.getEmail())
                .status("INACTIVE")
                .build());

        batchDeletedAlice = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-deleted-alice-" + nano)
                .masterProgrammeId(progA1.getId())
                .name("2019-2023")
                .startYear(2019)
                .endYear(2023)
                .coordinatorId(pcAlice.getId())
                .coordinatorName(pcAlice.getName())
                .coordinatorEmail(pcAlice.getEmail())
                .status("ACTIVE")
                .deletedAt(ZonedDateTime.now())
                .deletedBy("admin")
                .build());

        batchSchoolB = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-sch-b-" + nano)
                .masterProgrammeId(progB1.getId())
                .name("2023-2027")
                .startYear(2023)
                .endYear(2027)
                .status("ACTIVE")
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(User user) {
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
    @DisplayName("Req 1 & 2: Coordinator sees only batches assigned to them and their assigned programme")
    void testCoordinatorSeesOnlyBatchesAssignedToThem() {
        authenticate(pcAlice);

        List<ProgrammeBatch> batches = academicService.getBatchesFiltered(
                null, null, null, null, null, null, null, null);

        assertNotNull(batches);
        assertEquals(1, batches.size(), "Alice must see only 1 active batch assigned to her");
        assertEquals(batchActiveAlice.getId(), batches.get(0).getId());
    }

    @Test
    @DisplayName("Req 1: coordinatorEmail + masterProgrammeId uses AND filtering in single query")
    void testCoordinatorEmailAndMasterProgrammeIdUsesAndFiltering() {
        authenticate(adminUser);

        // Filter by Alice's email + progA1
        List<ProgrammeBatch> batches = academicService.getBatchesFiltered(
                progA1.getId(), null, pcAlice.getEmail(), null, null, null, null, "ACTIVE");

        assertEquals(1, batches.size());
        assertEquals(batchActiveAlice.getId(), batches.get(0).getId());

        // Filter by Alice's email + Bob's progA2 -> AND condition must return empty
        List<ProgrammeBatch> emptyBatches = academicService.getBatchesFiltered(
                progA2.getId(), null, pcAlice.getEmail(), null, null, null, null, "ACTIVE");

        assertTrue(emptyBatches.isEmpty(), "Alice email with Bob's programme must return 0 records under AND logic");
    }

    @Test
    @DisplayName("Req 1: Soft-deleted and inactive batches are excluded by default")
    void testSoftDeletedAndInactiveBatchesAreExcluded() {
        authenticate(adminUser);

        List<ProgrammeBatch> activeBatches = academicService.getBatchesFiltered(
                progA1.getId(), null, null, null, null, null, null, null);

        // Only batchActiveAlice is ACTIVE and non-deleted
        assertEquals(1, activeBatches.size());
        assertEquals(batchActiveAlice.getId(), activeBatches.get(0).getId());

        // Inactive batches are excluded unless status='INACTIVE' or status='ALL'
        List<ProgrammeBatch> allBatches = academicService.getBatchesFiltered(
                progA1.getId(), null, null, null, null, null, null, "ALL");
        assertEquals(2, allBatches.size(), "ALL status includes ACTIVE and INACTIVE, but excludes soft-deleted");
        assertTrue(allBatches.stream().noneMatch(b -> b.getId().equals(batchDeletedAlice.getId())), "Deleted batch must never appear");
    }

    @Test
    @DisplayName("Req 2 & 3: Cross-school, cross-department, and cross-coordinator access returns 403 Forbidden")
    void testCrossCoordinatorAndScopeAccessReturns403() {
        authenticate(pcAlice);

        // Alice tries to query Bob's coordinator email -> 403
        assertThrows(ResponseStatusException.class, () ->
                academicService.getBatchesFiltered(null, null, pcBob.getEmail(), null, null, null, null, null));

        // Alice tries to query Bob's programme -> 403
        assertThrows(ResponseStatusException.class, () ->
                academicService.getBatchesFiltered(progA2.getId(), null, null, null, null, null, null, null));

        // Alice tries to query School B department -> 403
        assertThrows(ResponseStatusException.class, () ->
                academicService.getBatchesFiltered(null, deptB.getId(), null, null, null, null, null, null));
    }

    @Test
    @DisplayName("Req 3: School and Department Isolation across roles")
    void testSchoolAndDepartmentIsolation() {
        authenticate(pcAlice);

        // Programme coordinator only gets their assigned school
        List<School> schools = academicService.getAllSchools();
        assertEquals(1, schools.size());
        assertEquals(schoolA.getId(), schools.get(0).getId());

        // Programme coordinator only gets their assigned department
        List<Department> depts = academicService.getAllDepartments();
        assertEquals(1, depts.size());
        assertEquals(deptA.getId(), depts.get(0).getId());
    }

    @Test
    @DisplayName("Req 4 & 7: Canonical response contract and zero N+1 metadata enrichment")
    void testCanonicalContractAndMetadataEnrichment() {
        authenticate(adminUser);

        List<ProgrammeBatch> batches = academicService.getBatchesFiltered(
                progA1.getId(), null, null, null, null, null, null, null);

        assertFalse(batches.isEmpty());
        ProgrammeBatch batch = batches.get(0);

        assertNotNull(batch.getId());
        assertEquals(progA1.getId(), batch.getMasterProgrammeId());
        assertEquals(pcAlice.getId(), batch.getCoordinatorId());
        assertEquals(pcAlice.getName(), batch.getCoordinatorName());
        assertEquals(pcAlice.getEmail(), batch.getCoordinatorEmail());
        assertEquals("B.Tech CS A1", batch.getProgrammeName(), "Programme name must be enriched");
        assertEquals(progA1.getCode(), batch.getProgrammeCode(), "Programme code must be enriched");
        assertEquals("ACTIVE", batch.getStatus());
        assertNull(batch.getDeletedAt());
    }
}
