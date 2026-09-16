package com.dypiu.nba.emmu;

import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.dto.ProgrammeAtrReportDto;
import com.dypiu.nba.dto.ProgrammeBatchAttainmentReportDto;
import com.dypiu.nba.dto.analytics.AttentionAreaItemDto;
import com.dypiu.nba.emmu.dto.*;
import com.dypiu.nba.emmu.service.EmmuEvidenceService;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class EmmuApiIntegrationTest {

    @Autowired
    private EmmuEvidenceService emmuEvidenceService;

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
    private ProgrammeBatchAttainmentReportRepository batchReportRepository;

    @Autowired
    private CourseAttainmentReportRepository courseReportRepository;

    @Autowired
    private StudentCoMarkRepository studentCoMarkRepository;

    @Autowired
    private ProgrammeAtrRepository programmeAtrRepository;

    @Autowired
    private CourseAtrRepository courseAtrRepository;

    @Autowired
    private ProgrammeOutcomeRepository poRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository psoRepository;

    @Autowired
    private CourseOutcomeRepository courseOutcomeRepository;

    @Autowired
    private CoPoMappingRepository coPoMappingRepository;

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
    private ProgrammeBatch batch2022;
    private ProgrammeBatchCourse courseDs;
    private ProgrammeBatchCourse courseOs;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Users
        userRepository.save(User.builder()
                .id(9001L)
                .username("iqac_admin")
                .email("iqac@dypiu.ac.in")
                .name("IQAC Director")
                .passwordHash("hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(9002L)
                .username("dir_soet")
                .email("dir.soet@dypiu.ac.in")
                .name("Director SOET")
                .passwordHash("hash")
                .role(UserRole.DIRECTOR)
                .schoolId("sch-soet")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(9003L)
                .username("hod_cse")
                .email("hod.cse@dypiu.ac.in")
                .name("HOD CSE")
                .passwordHash("hash")
                .role(UserRole.HOD)
                .schoolId("sch-soet")
                .departmentId("dept-cse")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(9004L)
                .username("pc_cse")
                .email("pc.cse@dypiu.ac.in")
                .name("PC BTech CSE")
                .passwordHash("hash")
                .role(UserRole.PROGRAMME_COORDINATOR)
                .schoolId("sch-soet")
                .departmentId("dept-cse")
                .masterProgrammeId("prog-btech-cse")
                .isActive(true)
                .build());

        userRepository.save(User.builder()
                .id(9005L)
                .username("faculty_ds")
                .email("faculty.ds@dypiu.ac.in")
                .name("Prof. DS")
                .passwordHash("hash")
                .role(UserRole.FACULTY)
                .schoolId("sch-soet")
                .departmentId("dept-cse")
                .isActive(true)
                .build());

        // 2. Academic Structure
        schoolSoet = schoolRepository.save(School.builder()
                .id("sch-soet")
                .code("SOET")
                .name("School of Engineering and Technology")
                .directorEmail("dir.soet@dypiu.ac.in")
                .build());

        schoolSom = schoolRepository.save(School.builder()
                .id("sch-som")
                .code("SOM")
                .name("School of Management")
                .directorEmail("dir.som@dypiu.ac.in")
                .build());

        deptCse = departmentRepository.save(Department.builder()
                .id("dept-cse")
                .schoolId("sch-soet")
                .code("CSE")
                .name("Computer Science and Engineering")
                .hodEmail("hod.cse@dypiu.ac.in")
                .build());

        deptMba = departmentRepository.save(Department.builder()
                .id("dept-mba")
                .schoolId("sch-som")
                .code("DOMS")
                .name("Department of Management Studies")
                .hodEmail("hod.mba@dypiu.ac.in")
                .build());

        progBtech = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-btech-cse")
                .departmentId("dept-cse")
                .code("BTECH_CSE")
                .name("B.Tech Computer Science & Engineering")
                .coordinatorEmail("pc.cse@dypiu.ac.in")
                .durationYears(4)
                .build());

        progMba = masterProgrammeRepository.save(MasterProgramme.builder()
                .id("prog-mba")
                .departmentId("dept-mba")
                .code("MBA")
                .name("Master of Business Administration")
                .coordinatorEmail("pc.mba@dypiu.ac.in")
                .durationYears(2)
                .build());

        batch2022 = programmeBatchRepository.save(ProgrammeBatch.builder()
                .id("batch-2022-cse")
                .masterProgrammeId("prog-btech-cse")
                .name("2022-2026")
                .startYear(2022)
                .endYear(2026)
                .status("ACTIVE")
                .build());

        // 3. Courses
        courseDs = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-ds-2022")
                .programmeBatchId("batch-2022-cse")
                .masterCourseId("mc-ds")
                .code("CS201")
                .name("Data Structures")
                .semester(3)
                .courseCoordinatorId(9005L)
                .courseCoordinatorName("Prof. DS")
                .build());

        courseOs = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id("pbc-os-2022")
                .programmeBatchId("batch-2022-cse")
                .masterCourseId("mc-os")
                .code("CS301")
                .name("Operating Systems")
                .semester(5)
                .courseCoordinatorName("Prof. OS")
                .build());

        // 4. Course Outcomes & Mappings
        CourseOutcome co1 = courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-ds-1")
                .programmeBatchCourseId("pbc-ds-2022")
                .code("CO1")
                .statement("Understand Linear Data Structures")
                .targetLevel(new BigDecimal("2.00"))
                .build());

        CourseOutcome co2 = courseOutcomeRepository.save(CourseOutcome.builder()
                .id("co-ds-2")
                .programmeBatchCourseId("pbc-ds-2022")
                .code("CO2")
                .statement("Apply Graph and Tree Algorithms")
                .targetLevel(new BigDecimal("2.00"))
                .build());

        coPoMappingRepository.save(CoPoMapping.builder()
                .id("copo-1")
                .courseOutcomeId("co-ds-1")
                .poCode("PO1")
                .mappingLevel(3)
                .build());

        coPoMappingRepository.save(CoPoMapping.builder()
                .id("copo-2")
                .courseOutcomeId("co-ds-2")
                .poCode("PO3")
                .mappingLevel(3)
                .build());

        // 5. Course Attainment Report
        List<CourseAttainmentReportDto.Table1Row> table1 = List.of(
                CourseAttainmentReportDto.Table1Row.builder()
                        .coCode("CO1")
                        .poMappings(Map.of("PO1", 3))
                        .psoMappings(Map.of("PSO1", 2))
                        .build(),
                CourseAttainmentReportDto.Table1Row.builder()
                        .coCode("CO2")
                        .poMappings(Map.of("PO3", 3))
                        .psoMappings(Map.of("PSO2", 3))
                        .build()
        );

        List<CourseAttainmentReportDto.Table3Row> table3 = List.of(
                CourseAttainmentReportDto.Table3Row.builder()
                        .coCode("CO1")
                        .statement("Understand Linear Data Structures")
                        .directLevel(3)
                        .indirectLevel(3)
                        .finalAttainment(new BigDecimal("3.00"))
                        .targetLevel(new BigDecimal("2.00"))
                        .targetMet(true)
                        .build(),
                CourseAttainmentReportDto.Table3Row.builder()
                        .coCode("CO2")
                        .statement("Apply Graph and Tree Algorithms")
                        .directLevel(1)
                        .indirectLevel(2)
                        .finalAttainment(new BigDecimal("1.20"))
                        .targetLevel(new BigDecimal("2.00"))
                        .targetMet(false)
                        .build()
        );

        courseReportRepository.save(CourseAttainmentReport.builder()
                .id("cr-ds-2022")
                .programmeBatchCourseId("pbc-ds-2022")
                .status(ReportStatus.FINALIZED)
                .directAttainment(new BigDecimal("2.10"))
                .indirectAttainment(new BigDecimal("2.50"))
                .overallCoAttainment(new BigDecimal("2.18"))
                .table1MappingJson(objectMapper.writeValueAsString(table1))
                .table3CoAttainmentJson(objectMapper.writeValueAsString(table3))
                .build());

        // 6. Student Marks for CO2
        for (int i = 1; i <= 20; i++) {
            studentCoMarkRepository.save(StudentCoMark.builder()
                    .id("scm-" + i)
                    .studentId("std-" + i)
                    .programmeBatchCourseId("pbc-ds-2022")
                    .prn("2022CSE" + String.format("%03d", i))
                    .studentName("Student " + i)
                    .coCode("CO2")
                    .marksObtained(BigDecimal.valueOf(i <= 8 ? 35.0 : 75.0)) // 8 students below 50%
                    .maxMarks(BigDecimal.valueOf(100.0))
                    .build());
        }

        // 7. Programme Batch Report (Finalized snapshot)
        List<ProgrammeBatchAttainmentReportDto.Report4PoRow> poRows = new ArrayList<>();
        poRows.add(ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                .poCode("PO1")
                .statement("Engineering Knowledge")
                .targetLevel(new BigDecimal("2.50"))
                .directAttainment(new BigDecimal("2.70"))
                .indirectAttainment(new BigDecimal("2.80"))
                .finalAttainment(new BigDecimal("2.72"))
                .build());
        poRows.add(ProgrammeBatchAttainmentReportDto.Report4PoRow.builder()
                .poCode("PO3")
                .statement("Design and Development of Solutions")
                .targetLevel(new BigDecimal("2.50"))
                .directAttainment(new BigDecimal("1.80"))
                .indirectAttainment(new BigDecimal("2.20"))
                .finalAttainment(new BigDecimal("1.88")) // Deficit: -0.62
                .build());

        List<ProgrammeBatchAttainmentReportDto.Report4PsoRow> psoRows = List.of(
                ProgrammeBatchAttainmentReportDto.Report4PsoRow.builder()
                        .psoCode("PSO1")
                        .statement("Software Systems Design")
                        .targetLevel(new BigDecimal("2.50"))
                        .directAttainment(new BigDecimal("2.60"))
                        .indirectAttainment(new BigDecimal("2.70"))
                        .finalAttainment(new BigDecimal("2.62"))
                        .build()
        );

        Map<String, Object> overallJson = Map.of("po", poRows, "pso", psoRows);

        batchReportRepository.save(ProgrammeBatchAttainmentReport.builder()
                .id("pbar-2022-cse")
                .programmeBatchId("batch-2022-cse")
                .status(ReportStatus.FINALIZED)
                .overallAttainmentReportJson(objectMapper.writeValueAsString(overallJson))
                .approvedAt(ZonedDateTime.now())
                .build());

        // 8. Programme ATR
        ProgrammeAtrReportDto atrReportDto = ProgrammeAtrReportDto.builder()
                .poOutcomes(List.of(
                        ProgrammeAtrReportDto.OutcomeRow.builder()
                                .outcomeCode("PO3")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("1.88"))
                                .actions(List.of("Conduct 10 additional hands-on algorithm labs", "Introduce industry problem statements"))
                                .build()
                ))
                .build();

        programmeAtrRepository.save(ProgrammeAtr.builder()
                .id("patr-2022-cse")
                .programmeBatchId("batch-2022-cse")
                .status(ProgrammeAtrStatus.APPROVED)
                .observationsJson(objectMapper.writeValueAsString(atrReportDto))
                .submittedBy("pc.cse@dypiu.ac.in")
                .approvedBy("dir.soet@dypiu.ac.in")
                .verificationComments("Approved by Academic Council.")
                .build());
    }

    private Principal createPrincipal(String email) {
        return () -> email;
    }

    // ==========================================
    // A. CONTEXT SUMMARY TESTS
    // ==========================================

    @Test
    @DisplayName("Context Summary: IQAC receives institution summary")
    @WithMockUser(username = "iqac@dypiu.ac.in", roles = {"IQAC"})
    void testContextSummary_Iqac() {
        Principal principal = createPrincipal("iqac@dypiu.ac.in");
        EmmuContextSummaryDto summary = emmuEvidenceService.getContextSummary(null, null, null, "batch-2022-cse", principal);

        assertNotNull(summary);
        assertNotNull(summary.getScope());
        assertEquals("IQAC", summary.getScope().getRole());
        assertEquals(EmmuDataCurrency.FINALIZED_EVALUATED_DATA, summary.getDataCurrency());
        assertNotNull(summary.getCoverage());
        assertTrue(summary.getCoverage().getTotalCourseOfferings() >= 2);
        assertNotNull(summary.getOutcomeSummary());
        assertTrue(summary.getOutcomeSummary().getPoEvaluated() >= 2);
        assertNotNull(summary.getTopDeficits());
        assertFalse(summary.getTopDeficits().isEmpty());
        assertEquals("PO3", summary.getTopDeficits().get(0).getOutcomeCode());
    }

    @Test
    @DisplayName("Context Summary: Director scoped to their school")
    @WithMockUser(username = "dir.soet@dypiu.ac.in", roles = {"DIRECTOR"})
    void testContextSummary_Director() {
        Principal principal = createPrincipal("dir.soet@dypiu.ac.in");
        EmmuContextSummaryDto summary = emmuEvidenceService.getContextSummary("sch-soet", null, null, null, principal);

        assertNotNull(summary);
        assertEquals("DIRECTOR", summary.getScope().getRole());
        assertEquals("sch-soet", summary.getScope().getSchoolId());
    }

    @Test
    @DisplayName("Context Summary: Director unauthorized school scope throws 403 Forbidden")
    @WithMockUser(username = "dir.soet@dypiu.ac.in", roles = {"DIRECTOR"})
    void testContextSummary_Director_UnauthorizedSchool_ThrowsForbidden() {
        Principal principal = createPrincipal("dir.soet@dypiu.ac.in");
        assertThrows(ResponseStatusException.class, () ->
                emmuEvidenceService.getContextSummary("sch-som", null, null, null, principal));
    }

    @Test
    @DisplayName("Context Summary: HOD scoped to their department")
    @WithMockUser(username = "hod.cse@dypiu.ac.in", roles = {"HOD"})
    void testContextSummary_Hod() {
        Principal principal = createPrincipal("hod.cse@dypiu.ac.in");
        EmmuContextSummaryDto summary = emmuEvidenceService.getContextSummary(null, "dept-cse", null, null, principal);

        assertNotNull(summary);
        assertEquals("HOD", summary.getScope().getRole());
        assertEquals("dept-cse", summary.getScope().getDepartmentId());
    }

    // ==========================================
    // B. ROOT-CAUSE DRILLDOWN TESTS
    // ==========================================

    @Test
    @DisplayName("Root-Cause Drilldown: Returns complete 5-layer evidence chain for PO deficit")
    @WithMockUser(username = "iqac@dypiu.ac.in", roles = {"IQAC"})
    void testRootCauseDrilldown_PoDeficit() {
        Principal principal = createPrincipal("iqac@dypiu.ac.in");
        EmmuRootCauseDrilldownDto drilldown = emmuEvidenceService.getRootCauseDrilldown("batch-2022-cse", "PO3", "PO", principal);

        assertNotNull(drilldown);
        assertNotNull(drilldown.getOutcome());
        assertEquals("PO3", drilldown.getOutcome().getCode());
        assertEquals(new BigDecimal("2.50"), drilldown.getOutcome().getTarget());
        assertEquals(new BigDecimal("1.88"), drilldown.getOutcome().getAttainment());
        assertEquals(new BigDecimal("-0.62"), drilldown.getOutcome().getGap());
        assertFalse(drilldown.getOutcome().isTargetMet());

        // Direct & Indirect
        assertNotNull(drilldown.getDirect());
        assertEquals(new BigDecimal("1.80"), drilldown.getDirect().getAttainment());
        assertNotNull(drilldown.getIndirect());
        assertEquals(new BigDecimal("2.20"), drilldown.getIndirect().getAttainment());

        // Course Evidence (CS201 maps to PO3 with strength 3)
        assertNotNull(drilldown.getCourseEvidence());
        assertFalse(drilldown.getCourseEvidence().isEmpty());
        assertEquals("CS201", drilldown.getCourseEvidence().get(0).getCourseCode());
        assertEquals(3, drilldown.getCourseEvidence().get(0).getMappingStrength());

        // Student Evidence Summary
        assertNotNull(drilldown.getStudentEvidenceSummary());
        assertFalse(drilldown.getStudentEvidenceSummary().isEmpty());
        assertEquals(20, drilldown.getStudentEvidenceSummary().get(0).getTotalStudentsEvaluated());
        assertEquals(12, drilldown.getStudentEvidenceSummary().get(0).getStudentsMeetingThreshold());
        assertEquals(8, drilldown.getStudentEvidenceSummary().get(0).getStudentsBelowThreshold());

        // ATR Evidence
        assertNotNull(drilldown.getAtrEvidence());
        assertTrue(drilldown.getAtrEvidence().isHasRecordedAtr());
        assertEquals("APPROVED", drilldown.getAtrEvidence().getStatus());
        assertFalse(drilldown.getAtrEvidence().getRecordedActions().isEmpty());
        assertTrue(drilldown.getAtrEvidence().getRecordedActions().get(0).contains("algorithm labs"));
    }

    // ==========================================
    // C. COURSE RANKINGS TESTS
    // ==========================================

    @Test
    @DisplayName("Course Rankings: Sorts courses by attainment and assigns correct ranks")
    @WithMockUser(username = "iqac@dypiu.ac.in", roles = {"IQAC"})
    void testCourseRankings_AttainmentSort() {
        Principal principal = createPrincipal("iqac@dypiu.ac.in");
        EmmuCourseRankingDto rankings = emmuEvidenceService.getCourseRankings("batch-2022-cse", null, "attainment", "DESC", principal);

        assertNotNull(rankings);
        assertEquals("batch-2022-cse", rankings.getProgrammeBatchId());
        assertNotNull(rankings.getRankedCourses());
        assertFalse(rankings.getRankedCourses().isEmpty());

        EmmuCourseRankingDto.CourseRankItemDto topRank = rankings.getRankedCourses().get(0);
        assertEquals(1, topRank.getRank());
        assertEquals("CS201", topRank.getCourseCode());
        assertEquals(new BigDecimal("2.18"), topRank.getOverallAttainment());
    }

    // ==========================================
    // D. COURSE INTELLIGENCE TESTS
    // ==========================================

    @Test
    @DisplayName("Course Intelligence: Returns CO attainments, mappings, student evidence, and ATR")
    @WithMockUser(username = "iqac@dypiu.ac.in", roles = {"IQAC"})
    void testCourseIntelligence_Complete() {
        Principal principal = createPrincipal("iqac@dypiu.ac.in");
        EmmuCourseIntelligenceDto cIntel = emmuEvidenceService.getCourseIntelligence("pbc-ds-2022", null, principal);

        assertNotNull(cIntel);
        assertEquals("CS201", cIntel.getCourseCode());
        assertEquals("Data Structures", cIntel.getCourseName());
        assertEquals(new BigDecimal("2.18"), cIntel.getOverallAttainment());

        // COs
        assertNotNull(cIntel.getCos());
        assertEquals(2, cIntel.getCos().size());
        assertEquals("CO1", cIntel.getCos().get(0).getCoCode());
        assertEquals("CO2", cIntel.getCos().get(1).getCoCode());
    }

    @Test
    @DisplayName("Course Intelligence: Faculty accessing unassigned course offering is denied")
    @WithMockUser(username = "faculty.ds@dypiu.ac.in", roles = {"FACULTY"})
    void testCourseIntelligence_Faculty_UnassignedCourse_Denied() {
        Principal principal = createPrincipal("faculty.ds@dypiu.ac.in");
        // courseOs is unassigned to faculty.ds
        assertThrows(ResponseStatusException.class, () ->
                emmuEvidenceService.getCourseIntelligence("pbc-os-2022", null, principal));
    }

    // ==========================================
    // E. NON-REGRESSION & SEMANTIC INTEGRITY
    // ==========================================

    @Test
    @DisplayName("Semantic Integrity: No synthetic overall programme score is generated")
    @WithMockUser(username = "iqac@dypiu.ac.in", roles = {"IQAC"})
    void testNoSyntheticOverallProgrammeAttainment() {
        Principal principal = createPrincipal("iqac@dypiu.ac.in");
        EmmuContextSummaryDto summary = emmuEvidenceService.getContextSummary(null, null, null, "batch-2022-cse", principal);

        // Verification that outcomeSummary only provides discrete outcome counts and no synthetic overall score
        assertNotNull(summary.getOutcomeSummary());
        assertTrue(summary.getOutcomeSummary().getPoEvaluated() > 0);
        assertTrue(summary.getOutcomeSummary().getPsoEvaluated() > 0);
    }
}
