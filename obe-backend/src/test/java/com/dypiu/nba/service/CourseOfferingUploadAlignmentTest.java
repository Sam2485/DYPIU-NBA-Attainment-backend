package com.dypiu.nba.service;

import com.dypiu.nba.dto.StudentMarksRowDto;
import com.dypiu.nba.dto.SurveyMarksPayloadDto;
import com.dypiu.nba.dto.SurveyResponseRowDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseOfferingUploadAlignmentTest {

    @Mock
    private AttainmentConfigurationRepository configRepository;

    @Mock
    private StudentCoMarkRepository studentCoMarkRepository;

    @Mock
    private CourseOutcomeRepository courseOutcomeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BatchRepository batchRepository;

    @Mock
    private UploadedDocumentRepository uploadedDocumentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private ProgrammeRepository programmeRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AttainmentCalculationService calculationService;

    private ReportAccessService reportAccessService;

    private CourseOffering offering2025;
    private CourseOffering offering2024;
    private Student student2025;
    private Student student2024;

    @BeforeEach
    void setUp() {
        reportAccessService = new ReportAccessService(
                userRepository,
                schoolRepository,
                departmentRepository,
                programmeRepository,
                batchRepository,
                courseRepository,
                courseOfferingRepository
        );

        offering2025 = CourseOffering.builder()
                .id("offering-cs301-2025")
                .courseId("crs-cs301")
                .batchId("batch-2025-29")
                .semester(3)
                .courseCoordinatorId(101L)
                .courseCoordinatorName("Dr. Alice Smith")
                .build();

        offering2024 = CourseOffering.builder()
                .id("offering-cs301-2024")
                .courseId("crs-cs301")
                .batchId("batch-2024-28")
                .semester(3)
                .courseCoordinatorId(102L)
                .courseCoordinatorName("Dr. Bob Jones")
                .build();

        student2025 = Student.builder()
                .id("st-1")
                .prn("PRN-2025-001")
                .name("Student 2025")
                .email("student2025@dypiu.edu")
                .batchId("batch-2025-29")
                .build();

        student2024 = Student.builder()
                .id("st-2")
                .prn("PRN-2024-001")
                .name("Student 2024")
                .email("student2024@dypiu.edu")
                .batchId("batch-2024-28")
                .build();
    }

    @Test
    @DisplayName("TEST 1: Correct batch student marks upload - SUCCESS")
    void testUploadMarks_CorrectBatch_Success() {
        when(courseOfferingRepository.existsById("offering-cs301-2025")).thenReturn(true);
        when(courseOfferingRepository.findById("offering-cs301-2025")).thenReturn(Optional.of(offering2025));
        when(studentRepository.findByPrn("PRN-2025-001")).thenReturn(Optional.of(student2025));

        StudentMarksRowDto row = StudentMarksRowDto.builder()
                .srNo(1)
                .prn("PRN-2025-001")
                .studentName("Student 2025")
                .coMarks(Map.of("CO1", new BigDecimal("18.5"), "CO2", new BigDecimal("19.0")))
                .build();

        assertDoesNotThrow(() -> calculationService.saveStudentCoMarksToDatabase(
                "offering-cs301-2025",
                Map.of("CO1", new BigDecimal("20"), "CO2", new BigDecimal("20")),
                List.of(row)
        ));

        verify(studentCoMarkRepository).deleteByCourseOfferingId("offering-cs301-2025");
        verify(studentCoMarkRepository).saveAll(any());
    }

    @Test
    @DisplayName("TEST 2: Wrong batch student PRN in upload - REJECT ROW / ERROR")
    void testUploadMarks_WrongBatch_Rejected() {
        when(courseOfferingRepository.existsById("offering-cs301-2025")).thenReturn(true);
        when(courseOfferingRepository.findById("offering-cs301-2025")).thenReturn(Optional.of(offering2025));
        when(studentRepository.findByPrn("PRN-2024-001")).thenReturn(Optional.of(student2024));

        StudentMarksRowDto row = StudentMarksRowDto.builder()
                .srNo(1)
                .prn("PRN-2024-001")
                .studentName("Student 2024")
                .coMarks(Map.of("CO1", new BigDecimal("18.5")))
                .build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                calculationService.saveStudentCoMarksToDatabase(
                        "offering-cs301-2025",
                        Map.of("CO1", new BigDecimal("20")),
                        List.of(row)
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("belongs to batch 'batch-2024-28', but this Course Offering belongs to batch 'batch-2025-29'"));
        // Ensure student was not moved
        assertEquals("batch-2024-28", student2024.getBatchId());
    }

    @Test
    @DisplayName("TEST 3: Same Course, Different Batch - Isolated Marks without Overwriting")
    void testUploadMarks_SameCourseDifferentBatches_Isolation() {
        when(courseOfferingRepository.existsById("offering-cs301-2025")).thenReturn(true);
        when(courseOfferingRepository.findById("offering-cs301-2025")).thenReturn(Optional.of(offering2025));
        when(studentRepository.findByPrn("PRN-2025-001")).thenReturn(Optional.of(student2025));

        when(courseOfferingRepository.existsById("offering-cs301-2024")).thenReturn(true);
        when(courseOfferingRepository.findById("offering-cs301-2024")).thenReturn(Optional.of(offering2024));
        when(studentRepository.findByPrn("PRN-2024-001")).thenReturn(Optional.of(student2024));

        // Save Offering 2025 marks
        StudentMarksRowDto row2025 = StudentMarksRowDto.builder()
                .srNo(1)
                .prn("PRN-2025-001")
                .coMarks(Map.of("CO1", new BigDecimal("18.0")))
                .build();
        calculationService.saveStudentCoMarksToDatabase("offering-cs301-2025", Map.of("CO1", new BigDecimal("20")), List.of(row2025));

        // Save Offering 2024 marks
        StudentMarksRowDto row2024 = StudentMarksRowDto.builder()
                .srNo(1)
                .prn("PRN-2024-001")
                .coMarks(Map.of("CO1", new BigDecimal("15.0")))
                .build();
        calculationService.saveStudentCoMarksToDatabase("offering-cs301-2024", Map.of("CO1", new BigDecimal("20")), List.of(row2024));

        // Verify distinct deletions and scopes
        verify(studentCoMarkRepository).deleteByCourseOfferingId("offering-cs301-2025");
        verify(studentCoMarkRepository).deleteByCourseOfferingId("offering-cs301-2024");
    }

    @Test
    @DisplayName("TEST 4: Non-existent CourseOffering - HTTP 404 NOT FOUND")
    void testUploadMarks_InvalidCourseOffering_NotFound() {
        when(courseOfferingRepository.existsById("non-existent-offering")).thenReturn(false);
        when(courseOfferingRepository.findByCourseId("non-existent-offering")).thenReturn(List.of());

        StudentMarksRowDto row = StudentMarksRowDto.builder()
                .srNo(1)
                .prn("PRN-2025-001")
                .coMarks(Map.of("CO1", new BigDecimal("18.5")))
                .build();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                calculationService.saveStudentCoMarksToDatabase(
                        "non-existent-offering",
                        Map.of("CO1", new BigDecimal("20")),
                        List.of(row)
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("TEST 5: Unauthorized Course Coordinator access - HTTP 403 FORBIDDEN")
    void testReportAccess_UnauthorizedCourseCoordinator_Forbidden() {
        User coordinatorB = User.builder()
                .id(202L)
                .username("coordB")
                .name("Dr. Bob Jones")
                .email("bob@dypiu.edu")
                .role(UserRole.FACULTY)
                .build();

        when(courseOfferingRepository.findById("offering-cs301-2025")).thenReturn(Optional.of(offering2025));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                reportAccessService.validateCourseOfferingAccess(coordinatorB, "offering-cs301-2025")
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("You are not assigned to this Course Offering"));
    }

    @Test
    @DisplayName("TEST 6: Course survey isolation across different offerings")
    void testCourseSurvey_CrossBatchIsolation() {
        when(courseOfferingRepository.existsById("offering-cs301-2025")).thenReturn(true);
        when(courseOfferingRepository.existsById("offering-cs301-2024")).thenReturn(true);

        SurveyMarksPayloadDto payload2025 = SurveyMarksPayloadDto.builder()
                .courseId("offering-cs301-2025")
                .surveyResponses(List.of(
                        SurveyResponseRowDto.builder()
                                .srNo(1)
                                .prn("PRN-2025-001")
                                .coRatings(Map.of("CO1", new BigDecimal("3.0"), "CO2", new BigDecimal("3.0")))
                                .build()
                ))
                .build();

        SurveyMarksPayloadDto payload2024 = SurveyMarksPayloadDto.builder()
                .courseId("offering-cs301-2024")
                .surveyResponses(List.of(
                        SurveyResponseRowDto.builder()
                                .srNo(1)
                                .prn("PRN-2024-001")
                                .coRatings(Map.of("CO1", new BigDecimal("2.0"), "CO2", new BigDecimal("1.0")))
                                .build()
                ))
                .build();

        var result2025 = calculationService.calculateSurveyAttainment("offering-cs301-2025", payload2025);
        var result2024 = calculationService.calculateSurveyAttainment("offering-cs301-2024", payload2024);

        assertNotNull(result2025);
        assertNotNull(result2024);

        assertEquals(new BigDecimal("3.00"), result2025.getIndirectAttainmentScores().get("CO1"));
        assertEquals(new BigDecimal("2.00"), result2024.getIndirectAttainmentScores().get("CO1"));

        var cached2025 = calculationService.getSurveyAttainment("offering-cs301-2025");
        var cached2024 = calculationService.getSurveyAttainment("offering-cs301-2024");

        assertEquals(new BigDecimal("3.00"), cached2025.getIndirectAttainmentScores().get("CO1"));
        assertEquals(new BigDecimal("2.00"), cached2024.getIndirectAttainmentScores().get("CO1"));
    }
}
