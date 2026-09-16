package com.dypiu.nba.emmu;

import com.dypiu.nba.emmu.dto.*;
import com.dypiu.nba.emmu.resolver.AcademicEntityResolver;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EmmuEntityResolutionIntegrationTest {

    @Autowired
    private AcademicEntityResolver academicEntityResolver;

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
    private ProgrammeOutcomeRepository programmeOutcomeRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;

    @Autowired
    private UserRepository userRepository;

    private School schoolSoet;
    private School schoolSom;
    private Department deptCse;
    private Department deptMba;
    private MasterProgramme progBtech;
    private MasterProgramme progMba;
    private ProgrammeBatch batch2022;
    private ProgrammeBatch batch2023;
    private ProgrammeBatchCourse courseDs;
    private ProgrammeBatchCourse courseOs;
    private ProgrammeBatchCourse courseOsLab;

    @BeforeEach
    void setUp() {
        // 1. Users
        userRepository.save(User.builder()
                .id(8101L)
                .username("iqac_res")
                .email("iqac.res@dypiu.ac.in")
                .name("IQAC Resolver Director")
                .passwordHash("hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(8102L)
                .username("dir_soet_res")
                .email("dir.soet.res@dypiu.ac.in")
                .name("Director SOET Resolver")
                .passwordHash("hash")
                .role(UserRole.DIRECTOR)
                .schoolId("sch-soet-res")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(8103L)
                .username("hod_cse_res")
                .email("hod.cse.res@dypiu.ac.in")
                .name("HOD CSE Resolver")
                .passwordHash("hash")
                .role(UserRole.HOD)
                .schoolId("sch-soet-res")
                .departmentId("dept-cse-res")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(8104L)
                .username("pc_cse_res")
                .email("pc.cse.res@dypiu.ac.in")
                .name("PC CSE Resolver")
                .passwordHash("hash")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId("sch-soet-res")
                .departmentId("dept-cse-res")
                .masterProgrammeId("prog-btech-cse-res")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(8105L)
                .username("faculty_ds_res")
                .email("faculty.ds.res@dypiu.ac.in")
                .name("Faculty Data Structures")
                .passwordHash("hash")
                .role(UserRole.FACULTY)
                .schoolId("sch-soet-res")
                .departmentId("dept-cse-res")
                .isActive(true)
                .build());

        // 2. Schools
        schoolSoet = schoolRepository.save(School.builder()
                .id("sch-soet-res")
                .name("School of Engineering and Technology")
                .code("SOET-RES")
                .directorName("Director SOET Resolver")
                .directorEmail("dir.soet.res@dypiu.ac.in")
                .build());

        schoolSom = schoolRepository.save(School.builder()
                .id("sch-som-res")
                .name("School of Management")
                .code("SOM-RES")
                .directorName("Director SOM")
                .directorEmail("dir.som@dypiu.ac.in")
                .build());

        // 3. Departments
        deptCse = departmentRepository.save(Department.builder()
                .id("dept-cse-res")
                .schoolId("sch-soet-res")
                .name("Computer Science and Engineering")
                .code("CSE-RES")
                .hod("HOD CSE Resolver")
                .hodEmail("hod.cse.res@dypiu.ac.in")
                .status("ACTIVE")
                .build());

        deptMba = departmentRepository.save(Department.builder()
                .id("dept-mba-res")
                .schoolId("sch-som-res")
                .name("Management Studies")
                .code("MBA-RES")
                .hod("HOD MBA")
                .hodEmail("hod.mba@dypiu.ac.in")
                .status("ACTIVE")
                .build());

        // 4. Programmes
        progBtech = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-btech-cse-res")
                .departmentId("dept-cse-res")
                .name("B.Tech Computer Engineering")
                .degreeAwarded("B.Tech CSE")
                .durationYears(4)
                .status("ACTIVE")
                .build());

        progMba = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-mba-res")
                .departmentId("dept-mba-res")
                .name("Master of Business Administration")
                .degreeAwarded("MBA")
                .durationYears(2)
                .status("ACTIVE")
                .build());

        // 5. Batches
        batch2022 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-2022-cse-res")
                .masterProgrammeId("prog-btech-cse-res")
                .name("Batch 2022-2026")
                .startYear(2022)
                .endYear(2026)
                .status("ACTIVE")
                .build());

        batch2023 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-2023-cse-res")
                .masterProgrammeId("prog-btech-cse-res")
                .name("Batch 2023-2027")
                .startYear(2023)
                .endYear(2027)
                .status("ACTIVE")
                .build());

        // 6. Courses
        courseDs = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-ds-2022-res")
                .programmeBatchId("batch-2022-cse-res")
                .code("CS201")
                .name("Data Structures and Algorithms")
                .semester(3)
                .courseCoordinatorId(8105L)
                .courseCoordinatorName("Faculty Data Structures")
                .assignedFaculty("faculty.ds.res@dypiu.ac.in")
                .status("ACTIVE")
                .build());

        courseOs = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-os-2022-res")
                .programmeBatchId("batch-2022-cse-res")
                .code("CS202")
                .name("Operating Systems")
                .semester(4)
                .courseCoordinatorName("Prof. OS Coordinator")
                .assignedFaculty("faculty.os@dypiu.ac.in")
                .status("ACTIVE")
                .build());

        courseOsLab = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-oslab-2022-res")
                .programmeBatchId("batch-2022-cse-res")
                .code("CS202L")
                .name("Operating Systems Lab")
                .semester(4)
                .courseCoordinatorName("Prof. OS Coordinator")
                .assignedFaculty("faculty.os@dypiu.ac.in")
                .status("ACTIVE")
                .build());

        // 7. Outcomes
        courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-ds-1-res")
                .programmeBatchCourseId("pbc-ds-2022-res")
                .code("CO1")
                .statement("Understand Linear Data Structures")
                .targetLevel(new BigDecimal("2.00"))
                .build());

        courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-ds-2-res")
                .programmeBatchCourseId("pbc-ds-2022-res")
                .code("CO2")
                .statement("Apply Graph and Tree Algorithms")
                .targetLevel(new BigDecimal("2.00"))
                .build());

        programmeOutcomeRepository.save(ProgrammeOutcome.builder()
                .id("po-2022-1-res")
                .programmeBatchId("batch-2022-cse-res")
                .code("PO1")
                .statement("Engineering Knowledge")
                .target(new BigDecimal("2.50"))
                .build());

        programmeOutcomeRepository.save(ProgrammeOutcome.builder()
                .id("po-2022-3-res")
                .programmeBatchId("batch-2022-cse-res")
                .code("PO3")
                .statement("Design and Development of Solutions")
                .target(new BigDecimal("2.50"))
                .build());

        programmeSpecificOutcomeRepository.save(ProgrammeSpecificOutcome.builder()
                .id("pso-2022-1-res")
                .programmeBatchId("batch-2022-cse-res")
                .code("PSO1")
                .statement("Software Systems Design")
                .target(new BigDecimal("2.50"))
                .build());
    }

    private Principal createPrincipal(String email) {
        return () -> email;
    }

    // =========================================================================
    // A. EXACT, NORMALIZED & ACRONYM RESOLUTION
    // =========================================================================

    @Test
    @DisplayName("Exact & Normalized Name: Resolves programme with exact and case-insensitive strings")
    @WithMockUser(username = "iqac.res@dypiu.ac.in", roles = {"IQAC"})
    void testExactAndNormalizedProgramme() {
        Principal principal = createPrincipal("iqac.res@dypiu.ac.in");

        // Exact Name
        EntityResolutionResult res1 = academicEntityResolver.resolveProgramme("B.Tech Computer Engineering", null, principal);
        assertNotNull(res1);
        assertEquals(ResolutionStatus.RESOLVED, res1.getStatus());
        assertEquals("prog-btech-cse-res", res1.getCanonicalId());

        // Normalized Case & Punctuation
        EntityResolutionResult res2 = academicEntityResolver.resolveProgramme("b.tech - computer engineering", null, principal);
        assertNotNull(res2);
        assertEquals(ResolutionStatus.RESOLVED, res2.getStatus());
        assertEquals("prog-btech-cse-res", res2.getCanonicalId());
    }

    @Test
    @DisplayName("Acronym Matching: Resolves common dynamic abbreviations (CE / DSA / SOET)")
    @WithMockUser(username = "iqac.res@dypiu.ac.in", roles = {"IQAC"})
    void testAcronymMatching() {
        Principal principal = createPrincipal("iqac.res@dypiu.ac.in");

        // Course Acronym 'DSA' -> Data Structures and Algorithms
        EntityResolutionResult res1 = academicEntityResolver.resolveCourse("DSA", null, principal);
        assertNotNull(res1);
        assertEquals(ResolutionStatus.RESOLVED, res1.getStatus());
        assertEquals("pbc-ds-2022-res", res1.getCanonicalId());

        // School Code / Acronym 'SOET-RES'
        EntityResolutionResult res2 = academicEntityResolver.resolveSchool("SOET-RES", null, principal);
        assertNotNull(res2);
        assertEquals(ResolutionStatus.RESOLVED, res2.getStatus());
        assertEquals("sch-soet-res", res2.getCanonicalId());
    }

    // =========================================================================
    // B. FUZZY / TYPO RESOLUTION
    // =========================================================================

    @Test
    @DisplayName("Fuzzy Matching: Resolves user typos within authorized set")
    @WithMockUser(username = "iqac.res@dypiu.ac.in", roles = {"IQAC"})
    void testFuzzyTypoMatching() {
        Principal principal = createPrincipal("iqac.res@dypiu.ac.in");

        // Typo: 'Data Structres and Algoritms'
        EntityResolutionResult res1 = academicEntityResolver.resolveCourse("Data Structres and Algoritms", null, principal);
        assertNotNull(res1);
        assertEquals(ResolutionStatus.RESOLVED, res1.getStatus());
        assertEquals("pbc-ds-2022-res", res1.getCanonicalId());
        assertTrue(res1.getConfidence() >= 0.85);

        // Typo: 'Computar Science'
        EntityResolutionResult res2 = academicEntityResolver.resolveDepartment("Computar Science and Enginering", null, principal);
        assertNotNull(res2);
        assertEquals(ResolutionStatus.RESOLVED, res2.getStatus());
        assertEquals("dept-cse-res", res2.getCanonicalId());
    }

    // =========================================================================
    // C. CONVERSATIONAL & CONTEXTUAL REFERENCES
    // =========================================================================

    @Test
    @DisplayName("Conversational Reference: Resolves 'its CO2' using course context")
    @WithMockUser(username = "iqac.res@dypiu.ac.in", roles = {"IQAC"})
    void testConversationalReference_ItsCO2() {
        Principal principal = createPrincipal("iqac.res@dypiu.ac.in");

        EmmuConversationContext context = EmmuConversationContext.builder()
                .programmeBatchCourseId("pbc-ds-2022-res")
                .courseCode("CS201")
                .courseName("Data Structures and Algorithms")
                .build();

        EntityResolutionResult res = academicEntityResolver.resolveCO("its CO2", context, principal);
        assertNotNull(res);
        assertEquals(ResolutionStatus.RESOLVED, res.getStatus());
        assertEquals("co-ds-2-res", res.getCanonicalId());
        assertEquals("CO2", res.getCanonicalCode());
        assertTrue(res.isContextUsed());
    }

    @Test
    @DisplayName("Conversational Reference: Resolves 'next semester' & 'previous semester'")
    @WithMockUser(username = "iqac.res@dypiu.ac.in", roles = {"IQAC"})
    void testConversationalReference_Semesters() {
        Principal principal = createPrincipal("iqac.res@dypiu.ac.in");

        EmmuConversationContext context = EmmuConversationContext.builder()
                .semester(3)
                .build();

        // Next Semester
        EntityResolutionResult nextRes = academicEntityResolver.resolveSemester("what about the next semester?", context, principal);
        assertNotNull(nextRes);
        assertEquals(ResolutionStatus.RESOLVED, nextRes.getStatus());
        assertEquals("4", nextRes.getCanonicalId());
        assertEquals("Semester 4", nextRes.getCanonicalName());

        // Previous Semester
        EntityResolutionResult prevRes = academicEntityResolver.resolveSemester("show me the previous semester", context, principal);
        assertNotNull(prevRes);
        assertEquals(ResolutionStatus.RESOLVED, prevRes.getStatus());
        assertEquals("2", prevRes.getCanonicalId());
        assertEquals("Semester 2", prevRes.getCanonicalName());
    }

    @Test
    @DisplayName("Conversational Reference: Resolves 'next batch' using active batch context")
    @WithMockUser(username = "iqac.res@dypiu.ac.in", roles = {"IQAC"})
    void testConversationalReference_NextBatch() {
        Principal principal = createPrincipal("iqac.res@dypiu.ac.in");

        EmmuConversationContext context = EmmuConversationContext.builder()
                .programmeBatchId("batch-2022-cse-res")
                .masterProgrammeId("prog-btech-cse-res")
                .build();

        EntityResolutionResult res = academicEntityResolver.resolveBatch("what about the next batch?", context, principal);
        assertNotNull(res);
        assertEquals(ResolutionStatus.RESOLVED, res.getStatus());
        assertEquals("batch-2023-cse-res", res.getCanonicalId());
        assertEquals("Batch 2023-2027", res.getCanonicalName());
    }

    // =========================================================================
    // D. AMBIGUITY HANDLING
    // =========================================================================

    @Test
    @DisplayName("Ambiguity: Returns AMBIGUOUS with clarification prompt when multiple matches exist")
    @WithMockUser(username = "iqac.res@dypiu.ac.in", roles = {"IQAC"})
    void testAmbiguityHandling() {
        Principal principal = createPrincipal("iqac.res@dypiu.ac.in");

        // Both 'Operating Systems' (CS202) and 'Operating Systems Lab' (CS202L) exist in batch
        EntityResolutionResult res = academicEntityResolver.resolveCourse("OS", null, principal);
        assertNotNull(res);
        assertEquals(ResolutionStatus.AMBIGUOUS, res.getStatus());
        assertTrue(res.isAmbiguous());
        assertTrue(res.isRequiresClarification());
        assertNotNull(res.getClarificationPrompt());
        assertTrue(res.getClarificationPrompt().contains("Operating Systems"));
        assertEquals(2, res.getCandidateCount());
    }

    // =========================================================================
    // E. ZERO HALLUCINATION (NOT FOUND)
    // =========================================================================

    @Test
    @DisplayName("Zero Hallucination: Returns NOT_FOUND for non-existent entities without inventing IDs")
    @WithMockUser(username = "iqac.res@dypiu.ac.in", roles = {"IQAC"})
    void testNotFoundNoHallucination() {
        Principal principal = createPrincipal("iqac.res@dypiu.ac.in");

        EntityResolutionResult res = academicEntityResolver.resolveCourse("Quantum Computing and Relativity", null, principal);
        assertNotNull(res);
        assertEquals(ResolutionStatus.NOT_FOUND, res.getStatus());
        assertNull(res.getCanonicalId());
        assertEquals(0, res.getCandidateCount());
        assertFalse(res.isRequiresClarification());
    }

    // =========================================================================
    // F. STRICT RBAC & NEGATIVE SECURITY TESTS (NO CROSS-SCOPE DISCOVERY)
    // =========================================================================

    @Test
    @DisplayName("Security: Director of SOET cannot resolve SOM departments or programmes")
    @WithMockUser(username = "dir.soet.res@dypiu.ac.in", roles = {"DIRECTOR"})
    void testNegativeSecurity_DirectorCrossSchool() {
        Principal principal = createPrincipal("dir.soet.res@dypiu.ac.in");

        // Attempting to resolve MBA department (belongs to SOM)
        EntityResolutionResult res1 = academicEntityResolver.resolveDepartment("Management Studies", null, principal);
        assertEquals(ResolutionStatus.NOT_FOUND, res1.getStatus(), "Director of SOET must not resolve SOM departments");

        // Attempting to resolve MBA programme
        EntityResolutionResult res2 = academicEntityResolver.resolveProgramme("Master of Business Administration", null, principal);
        assertEquals(ResolutionStatus.NOT_FOUND, res2.getStatus(), "Director of SOET must not resolve SOM programmes");
    }

    @Test
    @DisplayName("Security: HOD of CSE cannot resolve Management programmes")
    @WithMockUser(username = "hod.cse.res@dypiu.ac.in", roles = {"HOD"})
    void testNegativeSecurity_HodCrossDepartment() {
        Principal principal = createPrincipal("hod.cse.res@dypiu.ac.in");

        EntityResolutionResult res = academicEntityResolver.resolveProgramme("MBA", null, principal);
        assertEquals(ResolutionStatus.NOT_FOUND, res.getStatus(), "HOD of CSE must not resolve MBA programmes");
    }

    @Test
    @DisplayName("Security: Faculty cannot resolve unassigned courses")
    @WithMockUser(username = "faculty.ds.res@dypiu.ac.in", roles = {"FACULTY"})
    void testNegativeSecurity_FacultyUnassignedCourse() {
        Principal principal = createPrincipal("faculty.ds.res@dypiu.ac.in");

        // faculty_ds is only assigned to Data Structures (CS201), NOT Operating Systems (CS202)
        EntityResolutionResult res = academicEntityResolver.resolveCourse("Operating Systems", null, principal);
        assertEquals(ResolutionStatus.NOT_FOUND, res.getStatus(), "Faculty must not resolve unassigned courses");
    }

    // =========================================================================
    // G. ZERO DATABASE MUTATION VERIFICATION
    // =========================================================================

    @Test
    @DisplayName("Zero Mutation: Entity resolution performs zero database writes")
    @WithMockUser(username = "iqac.res@dypiu.ac.in", roles = {"IQAC"})
    void testZeroDatabaseMutation() {
        Principal principal = createPrincipal("iqac.res@dypiu.ac.in");

        long schoolsBefore = schoolRepository.count();
        long deptsBefore = departmentRepository.count();
        long progsBefore = masterProgrammeRepository.count();
        long batchesBefore = programmeBatchRepository.count();
        long coursesBefore = programmeBatchCourseRepository.count();

        // Perform multiple resolution queries
        academicEntityResolver.resolveSchool("School of Engineering", null, principal);
        academicEntityResolver.resolveDepartment("CSE", null, principal);
        academicEntityResolver.resolveProgramme("Computer Engineering", null, principal);
        academicEntityResolver.resolveCourse("DSA", null, principal);
        academicEntityResolver.resolveSemester("Semester 4", null, principal);

        // Verify count parity
        assertEquals(schoolsBefore, schoolRepository.count(), "School count must not change");
        assertEquals(deptsBefore, departmentRepository.count(), "Department count must not change");
        assertEquals(progsBefore, masterProgrammeRepository.count(), "Programme count must not change");
        assertEquals(batchesBefore, programmeBatchRepository.count(), "Batch count must not change");
        assertEquals(coursesBefore, programmeBatchCourseRepository.count(), "Course count must not change");
    }
}
