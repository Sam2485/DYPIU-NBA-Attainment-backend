package com.dypiu.nba.service;

import com.dypiu.nba.dto.CourseMappingMatrixDto;
import com.dypiu.nba.dto.ProgrammeBatchOutcomeBundleDto;
import com.dypiu.nba.dto.ProgrammeTargetDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OutcomeImportFromPreviousBatchIntegrationTest {

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
    private ProgrammeBatchCourseRepository programmeBatchCourseRepository;

    @Autowired
    private ProgrammeOutcomeRepository poRepository;

    @Autowired
    private PoCompetencyRepository poCompetencyRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository psoRepository;

    @Autowired
    private PsoCompetencyRepository psoCompetencyRepository;

    @Autowired
    private CourseOutcomeRepository coRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApprovalRequestRepository approvalRequestRepository;

    private School school;
    private Department department;
    private MasterProgramme programme;
    private ProgrammeBatch batch2022;
    private ProgrammeBatch batch2023;
    private ProgrammeBatchCourse course2022;
    private ProgrammeBatchCourse course2023;
    private User hodUser;
    private User facultyUser;

    @BeforeEach
    void setUp() {
        poCompetencyRepository.deleteAll();
        psoCompetencyRepository.deleteAll();
        coRepository.deleteAll();
        poRepository.deleteAll();
        psoRepository.deleteAll();
        programmeBatchCourseRepository.deleteAll();
        programmeBatchRepository.deleteAll();
        userRepository.deleteAll();
        masterProgrammeRepository.deleteAll();
        departmentRepository.deleteAll();
        schoolRepository.deleteAll();

        school = schoolRepository.save(School.builder()
                .id("sch-test-01")
                .name("School of Engineering")
                .code("SOE")
                .build());

        department = departmentRepository.save(Department.builder()
                .id("dept-test-01")
                .name("Computer Science and Engineering")
                .code("CSE")
                .schoolId(school.getId())
                .build());

        programme = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-test-01")
                .name("B.Tech Computer Science")
                .code("BT-CSE")
                .departmentId(department.getId())
                .build());

        hodUser = userRepository.save(User.builder()
                .name("Prof HOD")
                .username("prof_hod")
                .email("hod.cse@dypiu.ac.in")
                .passwordHash("pwd")
                .role(UserRole.HOD)
                .schoolId(school.getId())
                .departmentId(department.getId())
                .isActive(true)
                .build());

        facultyUser = userRepository.save(User.builder()
                .name("Dr. Faculty")
                .username("dr_faculty")
                .email("faculty@dypiu.ac.in")
                .passwordHash("pwd")
                .role(UserRole.FACULTY)
                .schoolId(school.getId())
                .departmentId(department.getId())
                .isActive(true)
                .build());

        // Batch 2022 (Previous batch)
        batch2022 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-2022")
                .name("B.Tech CSE 2022-2026")
                .masterProgrammeId(programme.getId())
                .startYear(2022)
                .endYear(2026)
                .coordinatorId(hodUser.getId())
                .coordinatorEmail(hodUser.getEmail())
                .build());

        // Batch 2023 (Current batch to import into)
        batch2023 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-2023")
                .name("B.Tech CSE 2023-2027")
                .masterProgrammeId(programme.getId())
                .startYear(2023)
                .endYear(2027)
                .coordinatorId(hodUser.getId())
                .coordinatorEmail(hodUser.getEmail())
                .build());

        // Course offering in Batch 2022
        course2022 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-cs101-2022")
                .programmeBatchId(batch2022.getId())
                .code("CS101")
                .name("Data Structures")
                .semester(3)
                .credits(4)
                .courseCoordinatorId(facultyUser.getId())
                .courseCoordinatorName(facultyUser.getName())
                .assignedFaculty(facultyUser.getEmail())
                .build());

        // Course offering in Batch 2023 (target)
        course2023 = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-cs101-2023")
                .programmeBatchId(batch2023.getId())
                .code("CS101")
                .name("Data Structures")
                .semester(3)
                .credits(4)
                .courseCoordinatorId(facultyUser.getId())
                .courseCoordinatorName(facultyUser.getName())
                .assignedFaculty(facultyUser.getEmail())
                .build());

        approvalRequestRepository.deleteAll();
        approvalRequestRepository.save(ApprovalRequest.builder()
                .id("appr-alloc-" + System.nanoTime())
                .type(ApprovalType.COURSE_ALLOCATION)
                .title("Course Allocation CSE")
                .resourceId("allocation-" + programme.getId())
                .masterProgrammeId(programme.getId())
                .status(ApprovalStatus.APPROVED)
                .submittedBy(hodUser.getEmail())
                .approvedBy(hodUser.getEmail())
                .build());

        authenticateAs(hodUser);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(),
                "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("PO/PSO Import: discovers source batches and copies outcomes cleanly")
    void testDiscoverAndCopyBatchOutcomes() {
        // 1. Populate Batch 2022 with POs, PSOs and Competencies
        PoCompetency po1Comp = PoCompetency.builder()
                .code("PO1.1")
                .statement("Apply mathematical principles to solve computing problems.")
                .build();
        ProgrammeOutcome po1 = ProgrammeOutcome.builder()
                .code("PO1")
                .statement("Engineering Knowledge: Apply knowledge of mathematics and science.")
                .target(new BigDecimal("2.60"))
                .competencies(List.of(po1Comp))
                .build();
        ProgrammeOutcome po2 = ProgrammeOutcome.builder()
                .code("PO2")
                .statement("Problem Analysis: Identify, formulate, and analyze complex problems.")
                .target(new BigDecimal("2.70"))
                .build();

        PsoCompetency pso1Comp = PsoCompetency.builder()
                .code("PSO1.1")
                .statement("Design modular enterprise software architectures.")
                .build();
        ProgrammeSpecificOutcome pso1 = ProgrammeSpecificOutcome.builder()
                .code("PSO1")
                .statement("Develop efficient software solutions.")
                .target(new BigDecimal("2.50"))
                .competencies(List.of(pso1Comp))
                .build();

        ProgrammeBatchOutcomeBundleDto bundle2022 = ProgrammeBatchOutcomeBundleDto.builder()
                .programmeBatchId(batch2022.getId())
                .masterProgrammeId(programme.getId())
                .pos(List.of(po1, po2))
                .psos(List.of(pso1))
                .poTargets(Map.of("PO1", new BigDecimal("2.60"), "PO2", new BigDecimal("2.70")))
                .psoTargets(Map.of("PSO1", new BigDecimal("2.50")))
                .build();
        outcomeService.saveProgrammeBatchOutcomeBundle(batch2022.getId(), bundle2022);

        // 2. Discover available sources for Batch 2023
        List<Map<String, Object>> sources = outcomeService.getAvailableOutcomeSourceBatches(batch2023.getId());
        assertFalse(sources.isEmpty());
        assertEquals(1, sources.size());
        Map<String, Object> source = sources.get(0);
        assertEquals(batch2022.getId(), source.get("batchId"));
        assertEquals(2, source.get("poCount"));
        assertEquals(1, source.get("psoCount"));
        assertTrue((Boolean) source.get("isDirectPredecessor"));

        // 3. Copy outcomes into Batch 2023
        ProgrammeBatchOutcomeBundleDto copied = outcomeService.copyBatchOutcomes(batch2023.getId(), batch2022.getId());
        assertNotNull(copied);
        assertEquals(batch2023.getId(), copied.getProgrammeBatchId());
        assertEquals("DRAFT", copied.getStatus());
        assertEquals(2, copied.getPos().size());
        assertEquals(1, copied.getPsos().size());

        // Verify Competencies copied
        ProgrammeOutcome copiedPo1 = copied.getPos().stream().filter(p -> "PO1".equals(p.getCode())).findFirst().orElseThrow();
        assertNotNull(copiedPo1.getCompetencies());
        assertEquals(1, copiedPo1.getCompetencies().size());
        assertEquals("Apply mathematical principles to solve computing problems.", copiedPo1.getCompetencies().get(0).getStatement());

        ProgrammeSpecificOutcome copiedPso1 = copied.getPsos().stream().filter(p -> "PSO1".equals(p.getCode())).findFirst().orElseThrow();
        assertNotNull(copiedPso1.getCompetencies());
        assertEquals(1, copiedPso1.getCompetencies().size());
        assertEquals("Design modular enterprise software architectures.", copiedPso1.getCompetencies().get(0).getStatement());

        // Verify DB persistence in Batch 2023
        List<ProgrammeOutcome> batch2023Pos = poRepository.findByProgrammeBatchId(batch2023.getId());
        assertEquals(2, batch2023Pos.size());
        assertTrue(batch2023Pos.stream().anyMatch(p -> "PO1".equals(p.getCode())));
        assertTrue(batch2023Pos.stream().anyMatch(p -> "PO2".equals(p.getCode())));

        ProgrammeOutcome persistedTargetPo1 = batch2023Pos.stream().filter(p -> "PO1".equals(p.getCode())).findFirst().orElseThrow();
        List<PoCompetency> targetPo1Comps = poCompetencyRepository.findByPoIdOrderByCodeAsc(persistedTargetPo1.getId());
        assertEquals(1, targetPo1Comps.size());
        assertEquals("Apply mathematical principles to solve computing problems.", targetPo1Comps.get(0).getStatement());

        // Verify original Batch 2022 is unmodified
        assertEquals(2, poRepository.findByProgrammeBatchId(batch2022.getId()).size());
        ProgrammeOutcome persistedSrcPo1 = poRepository.findByProgrammeBatchId(batch2022.getId()).stream().filter(p -> "PO1".equals(p.getCode())).findFirst().orElseThrow();
        List<PoCompetency> srcPo1Comps = poCompetencyRepository.findByPoIdOrderByCodeAsc(persistedSrcPo1.getId());
        assertEquals(1, srcPo1Comps.size());
        assertNotEquals(srcPo1Comps.get(0).getId(), targetPo1Comps.get(0).getId(), "Target competency must have its own distinct ID");
    }

    @Test
    @DisplayName("CO Import: discovers previous course offerings and copies course outcomes")
    void testDiscoverAndCopyCourseOutcomes() {
        authenticateAs(facultyUser);

        // 1. Populate Course 2022 with COs
        CourseOutcome co1 = CourseOutcome.builder()
                .code("CO1")
                .statement("Understand core data structures and memory layouts.")
                .bloomsLevel("L2 - Understand")
                .targetLevel(new BigDecimal("2.50"))
                .build();
        CourseOutcome co2 = CourseOutcome.builder()
                .code("CO2")
                .statement("Implement tree and graph traversal algorithms.")
                .bloomsLevel("L3 - Apply")
                .targetLevel(new BigDecimal("2.70"))
                .build();

        outcomeService.saveCOs(course2022.getId(), List.of(co1, co2));

        // 2. Discover available CO sources for Course 2023
        List<Map<String, Object>> sources = outcomeService.getAvailableCoSources(course2023.getId());
        assertFalse(sources.isEmpty());
        assertEquals(1, sources.size());
        Map<String, Object> source = sources.get(0);
        assertEquals(course2022.getId(), source.get("offeringId"));
        assertEquals("CS101", source.get("courseCode"));
        assertEquals(2, source.get("coCount"));

        // 3. Copy COs into Course 2023
        List<CourseOutcome> copied = outcomeService.copyCourseOutcomes(course2023.getId(), course2022.getId(), false);
        assertNotNull(copied);
        assertEquals(2, copied.size());

        // Verify DB persistence in Course 2023
        List<CourseOutcome> batch2023Cos = coRepository.findByProgrammeBatchCourseId(course2023.getId());
        assertEquals(2, batch2023Cos.size());
        assertTrue(batch2023Cos.stream().anyMatch(c -> "CO1".equals(c.getCode()) && c.getStatement().contains("core data structures")));
        assertTrue(batch2023Cos.stream().anyMatch(c -> "CO2".equals(c.getCode()) && c.getStatement().contains("tree and graph")));

        // Verify status is DRAFT
        assertEquals(ApprovalStatus.DRAFT, batch2023Cos.get(0).getStatus());
    }
}
