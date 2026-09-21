package com.dypiu.nba.analytics;

import com.dypiu.nba.dto.analytics.StudentCoEvidenceResponseDto;
import com.dypiu.nba.dto.analytics.StudentEvidenceRowDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class StudentEvidenceDataIntegrityTest {

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
    private CourseOutcomeRepository courseOutcomeRepository;

    @Autowired
    private StudentCoMarkRepository studentCoMarkRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private ProgrammeBatchCourse offering;
    private ProgrammeBatch batch;

    @BeforeEach
    void setUp() {
        studentCoMarkRepository.deleteAll();
        studentRepository.deleteAll();
        analyticsService.updateStudentEvidenceThreshold(new BigDecimal("50.00"), "SYSTEM");

        // Setup User
        userRepository.save(User.builder()
                .id(9901L)
                .username("iqac_eval")
                .email("iqac_eval@dypiu.ac.in")
                .name("IQAC Evaluator")
                .passwordHash("hash")
                .role(UserRole.IQAC)
                .isActive(true)
                .build());

        // Setup Hierarchy
        School school = schoolRepository.save(School.builder().id("sch-integrity").code("SOET").name("Engineering").build());
        Department dept = departmentRepository.save(Department.builder().id("dept-integrity").code("MECH").name("Mechanical").schoolId(school.getId()).build());
        MasterProgramme prog = masterProgrammeRepository.save(MasterProgramme.builder().id("prog-integrity").code("BTECH-MECH").name("B.Tech Mech").degreeAwarded("B.Tech").departmentId(dept.getId()).durationYears(4).build());
        batch = programmeBatchRepository.save(ProgrammeBatch.builder().id("batch-integrity").masterProgrammeId(prog.getId()).name("2021-2025").startYear(2021).endYear(2025).build());

        String offeringId = "offering-" + UUID.randomUUID().toString().substring(0, 8);
        offering = programmeBatchCourseRepository.save(ProgrammeBatchCourse.builder()
                .id(offeringId)
                .programmeBatchId(batch.getId())
                .code("202051")
                .name("Strength of Material")
                .semester(5)
                .courseCoordinatorName("Dr. Sharma")
                .build());

        // Configured Course Outcomes: C321.1 to C321.6
        for (int i = 1; i <= 6; i++) {
            courseOutcomeRepository.save(CourseOutcome.builder()
                    .id("co-mech-" + UUID.randomUUID().toString().substring(0, 8))
                    .programmeBatchCourseId(offering.getId())
                    .code("C321." + i)
                    .statement("Analyze mechanics outcome C321." + i)
                    .targetLevel(new BigDecimal("2.50"))
                    .bloomsLevel("L3")
                    .build());
        }
    }

    @Test
    @DisplayName("TEST 1: Normal multi-student course - Verify mathematical invariants")
    @WithMockUser(username = "iqac_eval", roles = {"IQAC"})
    void testNormalMultiStudentCourseInvariants() {
        // Insert 10 students with diverse marks for CO6
        for (int i = 1; i <= 10; i++) {
            BigDecimal mark = BigDecimal.valueOf(i * 10); // 10%, 20%, ..., 100%
            studentCoMarkRepository.save(StudentCoMark.builder()
                    .id("scm-normal-" + i)
                    .programmeBatchCourseId(offering.getId())
                    .studentId("std-normal-" + i)
                    .prn("20210100" + String.format("%04d", i))
                    .studentName("Student " + i)
                    .coCode("CO6")
                    .marksObtained(mark)
                    .maxMarks(BigDecimal.valueOf(100.00))
                    .build());
        }

        StudentCoEvidenceResponseDto response = analyticsService.getStudentCoEvidence(offering.getId(), "C321.6");

        assertThat(response).isNotNull();
        assertThat(response.getCoCode()).isEqualTo("C321.6");
        assertThat(response.getCourseCode()).isEqualTo("202051");
        assertThat(response.getCourseName()).isEqualTo("Strength of Material");

        // Invariant 1: totalStudentsEvaluated == studentRecords.size()
        assertThat(response.getTotalStudentsEvaluated()).isEqualTo(10);
        assertThat(response.getStudentRecords()).hasSize(10);

        // Invariant 2: studentsMeetingThreshold + studentsBelowThreshold == totalStudentsEvaluated
        // At 50% default threshold: marks 50, 60, 70, 80, 90, 100 meet threshold (6 students). Marks 10, 20, 30, 40 are below (4 students).
        assertThat(response.getStudentsMeetingThreshold()).isEqualTo(6);
        assertThat(response.getStudentsBelowThreshold()).isEqualTo(4);
        assertThat(response.getStudentsMeetingThreshold() + response.getStudentsBelowThreshold()).isEqualTo(response.getTotalStudentsEvaluated());

        // Invariant 3: sum(scoreDistribution.values()) == totalStudentsEvaluated
        Map<String, Integer> dist = response.getScoreDistribution();
        int sumDist = dist.values().stream().mapToInt(Integer::intValue).sum();
        assertThat(sumDist).isEqualTo(response.getTotalStudentsEvaluated());

        // Check distribution counts
        assertThat(dist.get("<50%")).isEqualTo(4);   // 10, 20, 30, 40
        assertThat(dist.get("50-59%")).isEqualTo(1); // 50
        assertThat(dist.get("60-69%")).isEqualTo(1); // 60
        assertThat(dist.get("70-79%")).isEqualTo(1); // 70
        assertThat(dist.get("80-89%")).isEqualTo(1); // 80
        assertThat(dist.get("90-100%")).isEqualTo(2); // 90, 100

        // Invariant 4: Statistics consistency
        assertThat(response.getAttainmentRatePercentage()).isEqualByComparingTo(new BigDecimal("60.00")); // 6 / 10 = 60.00%
        assertThat(response.getClassAveragePercentage()).isEqualByComparingTo(new BigDecimal("55.00")); // sum(10..100) = 550 / 10 = 55.00%
        assertThat(response.getHighestPercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getLowestPercentage()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("TEST 2: 202 enrolled students with actual exam records - Distinct enrolled vs evaluated counts")
    @WithMockUser(username = "iqac_eval", roles = {"IQAC"})
    void testEnrolledVsEvaluatedCountDistinction() {
        // Create 202 enrolled students in the database for this batch
        List<Student> enrolledList = new ArrayList<>();
        for (int i = 1; i <= 202; i++) {
            enrolledList.add(Student.builder()
                    .id("std-enrolled-" + i)
                    .programmeBatchId(batch.getId())
                    .prn("20210200" + String.format("%04d", i))
                    .name("Enrolled Student " + i)
                    .email("student" + i + "@dypiu.ac.in")
                    .build());
        }
        studentRepository.saveAll(enrolledList);

        // Only 185 students have valid examination marks for CO6
        List<StudentCoMark> marks = new ArrayList<>();
        for (int i = 1; i <= 185; i++) {
            marks.add(StudentCoMark.builder()
                    .id("scm-185-" + i)
                    .programmeBatchCourseId(offering.getId())
                    .studentId("std-enrolled-" + i)
                    .prn("20210200" + String.format("%04d", i))
                    .studentName("Enrolled Student " + i)
                    .coCode("CO6")
                    .marksObtained(BigDecimal.valueOf(75.00))
                    .maxMarks(BigDecimal.valueOf(100.00))
                    .build());
        }
        studentCoMarkRepository.saveAll(marks);

        StudentCoEvidenceResponseDto response = analyticsService.getStudentCoEvidence(offering.getId(), "C321.6");

        assertThat(response).isNotNull();
        // Crucial requirement: totalStudentsEnrolled reflects 202, totalStudentsEvaluated reflects 185
        assertThat(response.getTotalStudentsEnrolled()).isEqualTo(202);
        assertThat(response.getTotalStudentsEvaluated()).isEqualTo(185);
        assertThat(response.getStudentRecords()).hasSize(185);
        assertThat(response.getStudentsMeetingThreshold()).isEqualTo(185);
        assertThat(response.getStudentsBelowThreshold()).isEqualTo(0);
        assertThat(response.getAttainmentRatePercentage()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("TEST 3: Missing marks - Enrolled students without marks for requested CO are omitted")
    @WithMockUser(username = "iqac_eval", roles = {"IQAC"})
    void testMissingMarksOmittedWithoutFabricatingZeros() {
        // Student 1 has mark for CO6 (80%)
        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-co6-1")
                .programmeBatchCourseId(offering.getId())
                .studentId("std-1")
                .prn("PRN001")
                .studentName("Alice")
                .coCode("CO6")
                .marksObtained(new BigDecimal("80.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        // Student 2 has mark for CO1 ONLY (absent/missing for CO6)
        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-co1-2")
                .programmeBatchCourseId(offering.getId())
                .studentId("std-2")
                .prn("PRN002")
                .studentName("Bob")
                .coCode("CO1")
                .marksObtained(new BigDecimal("90.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        StudentCoEvidenceResponseDto response = analyticsService.getStudentCoEvidence(offering.getId(), "C321.6");

        // Evaluated count must be 1, not 2 (Bob must not be fabricated as 0% for CO6)
        assertThat(response.getTotalStudentsEvaluated()).isEqualTo(1);
        assertThat(response.getStudentRecords()).hasSize(1);
        assertThat(response.getStudentRecords().get(0).getPrn()).isEqualTo("PRN001");
        assertThat(response.getStudentRecords().get(0).getMarksObtained()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(response.getClassAveragePercentage()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    @DisplayName("TEST 4: Boundary test at exact configured threshold (50.00%)")
    @WithMockUser(username = "iqac_eval", roles = {"IQAC"})
    void testThresholdBoundaryEvaluation() {
        // Student 1: exactly 50.00% (25 / 50) -> should be MET
        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-bnd-1")
                .programmeBatchCourseId(offering.getId())
                .studentId("std-bnd-1")
                .prn("BND001")
                .studentName("Boundary Met")
                .coCode("CO6")
                .marksObtained(new BigDecimal("25.00"))
                .maxMarks(new BigDecimal("50.00"))
                .build());

        // Student 2: 49.98% (24.99 / 50 = 49.98%) -> should be BELOW
        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-bnd-2")
                .programmeBatchCourseId(offering.getId())
                .studentId("std-bnd-2")
                .prn("BND002")
                .studentName("Boundary Below")
                .coCode("CO6")
                .marksObtained(new BigDecimal("24.99"))
                .maxMarks(new BigDecimal("50.00"))
                .build());

        StudentCoEvidenceResponseDto response = analyticsService.getStudentCoEvidence(offering.getId(), "C321.6");

        assertThat(response.getTotalStudentsEvaluated()).isEqualTo(2);
        assertThat(response.getStudentsMeetingThreshold()).isEqualTo(1);
        assertThat(response.getStudentsBelowThreshold()).isEqualTo(1);

        StudentEvidenceRowDto metRow = response.getStudentRecords().stream()
                .filter(r -> "BND001".equals(r.getPrn())).findFirst().orElseThrow();
        assertThat(metRow.isThresholdMet()).isTrue();
        assertThat(metRow.getEvaluationStatus()).isEqualTo("MET");
        assertThat(metRow.getPercentage()).isEqualByComparingTo(new BigDecimal("50.00"));

        StudentEvidenceRowDto belowRow = response.getStudentRecords().stream()
                .filter(r -> "BND002".equals(r.getPrn())).findFirst().orElseThrow();
        assertThat(belowRow.isThresholdMet()).isFalse();
        assertThat(belowRow.getEvaluationStatus()).isEqualTo("BELOW");
        assertThat(belowRow.getPercentage()).isLessThan(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("TEST 5: CO Isolation - C321.6 resolves to CO6 and does not bleed CO1/CO2 marks")
    @WithMockUser(username = "iqac_eval", roles = {"IQAC"})
    void testCoIsolationAndCodeResolution() {
        // Save marks across CO1, CO2, and CO6
        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-iso-co1")
                .programmeBatchCourseId(offering.getId())
                .studentId("std-iso-1")
                .prn("ISO001")
                .studentName("Iso 1")
                .coCode("CO1")
                .marksObtained(new BigDecimal("95.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-iso-co2")
                .programmeBatchCourseId(offering.getId())
                .studentId("std-iso-2")
                .prn("ISO002")
                .studentName("Iso 2")
                .coCode("CO2")
                .marksObtained(new BigDecimal("30.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-iso-co6")
                .programmeBatchCourseId(offering.getId())
                .studentId("std-iso-3")
                .prn("ISO003")
                .studentName("Iso 3")
                .coCode("CO6")
                .marksObtained(new BigDecimal("70.00"))
                .maxMarks(new BigDecimal("100.00"))
                .build());

        // Query with prefixed code "C321.6"
        StudentCoEvidenceResponseDto responseCo6 = analyticsService.getStudentCoEvidence(offering.getId(), "C321.6");
        assertThat(responseCo6.getTotalStudentsEvaluated()).isEqualTo(1);
        assertThat(responseCo6.getStudentRecords().get(0).getPrn()).isEqualTo("ISO003");
        assertThat(responseCo6.getStudentRecords().get(0).getMarksObtained()).isEqualByComparingTo(new BigDecimal("70.00"));

        // Query with "CO1"
        StudentCoEvidenceResponseDto responseCo1 = analyticsService.getStudentCoEvidence(offering.getId(), "CO1");
        assertThat(responseCo1.getTotalStudentsEvaluated()).isEqualTo(1);
        assertThat(responseCo1.getStudentRecords().get(0).getPrn()).isEqualTo("ISO001");
        assertThat(responseCo1.getStudentRecords().get(0).getMarksObtained()).isEqualByComparingTo(new BigDecimal("95.00"));

        // Query with "C321.2"
        StudentCoEvidenceResponseDto responseCo2 = analyticsService.getStudentCoEvidence(offering.getId(), "C321.2");
        assertThat(responseCo2.getTotalStudentsEvaluated()).isEqualTo(1);
        assertThat(responseCo2.getStudentRecords().get(0).getPrn()).isEqualTo("ISO002");
        assertThat(responseCo2.getStudentRecords().get(0).getMarksObtained()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    @DisplayName("TEST 6: Zero evidence returns safe empty response without division by zero")
    @WithMockUser(username = "iqac_eval", roles = {"IQAC"})
    void testZeroEvidenceSafeHandling() {
        // No student marks inserted for this course
        StudentCoEvidenceResponseDto response = analyticsService.getStudentCoEvidence(offering.getId(), "C321.6");

        assertThat(response).isNotNull();
        assertThat(response.getTotalStudentsEvaluated()).isEqualTo(0);
        assertThat(response.getStudentsMeetingThreshold()).isEqualTo(0);
        assertThat(response.getStudentsBelowThreshold()).isEqualTo(0);
        assertThat(response.getAttainmentRatePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getClassAveragePercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getHighestPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getLowestPercentage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getStudentRecords()).isEmpty();

        // Score distribution should contain 0 for all buckets
        Map<String, Integer> dist = response.getScoreDistribution();
        assertThat(dist.values()).containsOnly(0);
    }

    @Test
    @DisplayName("TEST 7: Summary statistics mathematical consistency")
    @WithMockUser(username = "iqac_eval", roles = {"IQAC"})
    void testSummaryStatisticsConsistency() {
        // Insert 3 students: 40%, 60%, 80% out of 50 max marks
        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-stat-1")
                .programmeBatchCourseId(offering.getId())
                .studentId("s1")
                .prn("STAT001")
                .studentName("Student 1")
                .coCode("CO6")
                .marksObtained(new BigDecimal("20.00")) // 40.00%
                .maxMarks(new BigDecimal("50.00"))
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-stat-2")
                .programmeBatchCourseId(offering.getId())
                .studentId("s2")
                .prn("STAT002")
                .studentName("Student 2")
                .coCode("CO6")
                .marksObtained(new BigDecimal("30.00")) // 60.00%
                .maxMarks(new BigDecimal("50.00"))
                .build());

        studentCoMarkRepository.save(StudentCoMark.builder()
                .id("scm-stat-3")
                .programmeBatchCourseId(offering.getId())
                .studentId("s3")
                .prn("STAT003")
                .studentName("Student 3")
                .coCode("CO6")
                .marksObtained(new BigDecimal("40.00")) // 80.00%
                .maxMarks(new BigDecimal("50.00"))
                .build());

        StudentCoEvidenceResponseDto response = analyticsService.getStudentCoEvidence(offering.getId(), "C321.6");

        assertThat(response.getTotalStudentsEvaluated()).isEqualTo(3);
        assertThat(response.getStudentsMeetingThreshold()).isEqualTo(2); // 60% and 80%
        assertThat(response.getStudentsBelowThreshold()).isEqualTo(1);  // 40%

        // Attainment rate: 2 / 3 = 66.67%
        assertThat(response.getAttainmentRatePercentage()).isEqualByComparingTo(new BigDecimal("66.67"));

        // Class average: (40 + 60 + 80) / 3 = 60.00%
        assertThat(response.getClassAveragePercentage()).isEqualByComparingTo(new BigDecimal("60.00"));

        assertThat(response.getHighestPercentage()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(response.getLowestPercentage()).isEqualByComparingTo(new BigDecimal("40.00"));
    }
}
