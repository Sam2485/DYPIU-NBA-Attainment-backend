package com.dypiu.nba.service;

import com.dypiu.nba.dto.CourseImportItemDto;
import com.dypiu.nba.dto.CourseImportPreviewDto;
import com.dypiu.nba.dto.CourseImportResultDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CourseExcelImportServiceTest {

    @Mock
    private AcademicService academicService;

    @Mock
    private ProgrammeBatchRepository programmeBatchRepository;

    @Mock
    private ProgrammeBatchCourseRepository programmeBatchCourseRepository;

    @Mock
    private MasterProgrammeRepository masterProgrammeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AcademicLookupCacheService academicLookupCacheService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private CourseExcelImportService importService;

    private ProgrammeBatch testBatch;
    private MasterProgramme testMasterProgramme;
    private User testFaculty;

    @BeforeEach
    void setUp() {
        testMasterProgramme = MasterProgramme.builder()
                .id("prog-1")
                .name("Computer Science and Engineering")
                .code("CSE")
                .durationYears(4)
                .build();

        testBatch = ProgrammeBatch.builder()
                .id("batch-101")
                .name("2022-2026")
                .masterProgrammeId("prog-1")
                .durationYears(4)
                .status("ACTIVE")
                .build();

        testFaculty = User.builder()
                .id(25L)
                .name("Dr. Alan Turing")
                .email("alan.turing@dypiu.ac.in")
                .username("aturing")
                .role(UserRole.FACULTY)
                .build();
    }

    private byte[] createTestWorkbook(int semStart, int semEnd, boolean includeData) throws IOException {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            for (int s = semStart; s <= semEnd; s++) {
                Sheet sheet = wb.createSheet("Sem " + s);
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("Course Code *");
                header.createCell(1).setCellValue("Course Name *");
                header.createCell(2).setCellValue("Credits *");
                header.createCell(3).setCellValue("Course Type *");
                header.createCell(4).setCellValue("Semester");
                header.createCell(5).setCellValue("Course Coordinator");

                if (includeData) {
                    Row row1 = sheet.createRow(1);
                    row1.createCell(0).setCellValue("CS" + s + "01");
                    row1.createCell(1).setCellValue("Subject " + s + "A");
                    row1.createCell(2).setCellValue(3);
                    row1.createCell(3).setCellValue("THEORY");
                    row1.createCell(4).setCellValue(s);
                    row1.createCell(5).setCellValue("alan.turing@dypiu.ac.in");

                    Row row2 = sheet.createRow(2);
                    row2.createCell(0).setCellValue("CS" + s + "02");
                    row2.createCell(1).setCellValue("Subject " + s + "B Lab");
                    row2.createCell(2).setCellValue(1.5);
                    row2.createCell(3).setCellValue("LAB");
                    row2.createCell(4).setCellValue(s);
                    row2.createCell(5).setCellValue("");
                }
            }
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    @Test
    @DisplayName("Generate template produces valid Excel workbook with correct semester sheets")
    void testGenerateTemplate() throws IOException {
        when(programmeBatchRepository.findById("batch-101")).thenReturn(Optional.of(testBatch));
        when(masterProgrammeRepository.findById("prog-1")).thenReturn(Optional.of(testMasterProgramme));

        byte[] templateBytes = importService.generateTemplate("batch-101");
        assertNotNull(templateBytes);
        assertTrue(templateBytes.length > 0);

        try (Workbook wb = WorkbookFactory.create(new java.io.ByteArrayInputStream(templateBytes))) {
            assertEquals(8, wb.getNumberOfSheets());
            assertEquals("Sem 1", wb.getSheetAt(0).getSheetName());
            assertEquals("Sem 8", wb.getSheetAt(7).getSheetName());

            Sheet sheet1 = wb.getSheetAt(0);
            Row header = sheet1.getRow(0);
            assertEquals("Course Code *", header.getCell(0).getStringCellValue());
            assertEquals("Course Name *", header.getCell(1).getStringCellValue());
            assertEquals("Credits *", header.getCell(2).getStringCellValue());
            assertEquals("Course Type *", header.getCell(3).getStringCellValue());
            assertEquals("Semester", header.getCell(4).getStringCellValue());
            assertEquals("Course Coordinator", header.getCell(5).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Preview import validates multi-semester sheets and matches coordinator")
    void testPreviewImport_Valid() throws IOException {
        when(programmeBatchRepository.findById("batch-101")).thenReturn(Optional.of(testBatch));
        when(masterProgrammeRepository.findById("prog-1")).thenReturn(Optional.of(testMasterProgramme));
        when(programmeBatchCourseRepository.findByProgrammeBatchIdAndDeletedAtIsNull("batch-101"))
                .thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(testFaculty));

        byte[] wbBytes = createTestWorkbook(1, 4, true);
        MockMultipartFile file = new MockMultipartFile("file", "courses.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", wbBytes);

        CourseImportPreviewDto preview = importService.previewImport("batch-101", file);

        assertNotNull(preview);
        assertTrue(preview.isValid());
        assertEquals(8, preview.getTotalCourses());
        assertEquals(8, preview.getValidCount());
        assertEquals(0, preview.getErrorCount());
        assertEquals(4, preview.getCoursesBySemester().size());

        List<CourseImportItemDto> sem1Courses = preview.getCoursesBySemester().get(1);
        assertEquals(2, sem1Courses.size());
        CourseImportItemDto c1 = sem1Courses.get(0);
        assertEquals("CS101", c1.getCourseCode());
        assertEquals("Subject 1A", c1.getCourseName());
        assertEquals(3, c1.getCredits());
        assertEquals("THEORY", c1.getCourseType());
        assertTrue(c1.isCoordinatorMatched());
        assertEquals(25L, c1.getCoordinatorId());
    }

    @Test
    @DisplayName("Preview import rejects workbook when sheet semester exceeds programme duration")
    void testPreviewImport_ExceedsMaxSemesters() throws IOException {
        when(programmeBatchRepository.findById("batch-101")).thenReturn(Optional.of(testBatch));
        when(masterProgrammeRepository.findById("prog-1")).thenReturn(Optional.of(testMasterProgramme));
        when(programmeBatchCourseRepository.findByProgrammeBatchIdAndDeletedAtIsNull("batch-101"))
                .thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(testFaculty));

        // Create workbook with Sem 9 (max is 8 for 4-year programme)
        byte[] wbBytes = createTestWorkbook(1, 9, true);
        MockMultipartFile file = new MockMultipartFile("file", "courses.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", wbBytes);

        CourseImportPreviewDto preview = importService.previewImport("batch-101", file);

        assertNotNull(preview);
        assertFalse(preview.isValid());
        assertTrue(preview.getErrorCount() > 0);
        assertTrue(preview.getErrors().stream().anyMatch(e -> e.contains("exceeds the maximum allowed semesters")));
    }

    @Test
    @DisplayName("Preview import flags intra-file duplicate course codes")
    void testPreviewImport_IntraFileDuplicates() throws IOException {
        when(programmeBatchRepository.findById("batch-101")).thenReturn(Optional.of(testBatch));
        when(masterProgrammeRepository.findById("prog-1")).thenReturn(Optional.of(testMasterProgramme));
        when(programmeBatchCourseRepository.findByProgrammeBatchIdAndDeletedAtIsNull("batch-101"))
                .thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(testFaculty));

        byte[] wbBytes;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Sem 1");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Course Code *");
            header.createCell(1).setCellValue("Course Name *");
            header.createCell(2).setCellValue("Credits *");
            header.createCell(3).setCellValue("Course Type *");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("CS101");
            r1.createCell(1).setCellValue("Physics");
            r1.createCell(2).setCellValue(3);
            r1.createCell(3).setCellValue("THEORY");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("CS101");
            r2.createCell(1).setCellValue("Duplicate Physics");
            r2.createCell(2).setCellValue(3);
            r2.createCell(3).setCellValue("THEORY");

            wb.write(bos);
            wbBytes = bos.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "courses.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", wbBytes);

        CourseImportPreviewDto preview = importService.previewImport("batch-101", file);

        assertNotNull(preview);
        assertFalse(preview.isValid());
        assertTrue(preview.getErrorCount() > 0);
        List<CourseImportItemDto> sem1 = preview.getCoursesBySemester().get(1);
        assertEquals("DUPLICATE", sem1.get(1).getStatus());
    }

    @Test
    @DisplayName("Preview import flags duplicate against existing active batch courses")
    void testPreviewImport_ExistingBatchCourseDuplicate() throws IOException {
        when(programmeBatchRepository.findById("batch-101")).thenReturn(Optional.of(testBatch));
        when(masterProgrammeRepository.findById("prog-1")).thenReturn(Optional.of(testMasterProgramme));

        ProgrammeBatchCourse existing = ProgrammeBatchCourse.builder()
                .id("c-existing-1")
                .programmeBatchId("batch-101")
                .code("CS101")
                .name("Existing Physics")
                .semester(1)
                .status("ACTIVE")
                .build();

        when(programmeBatchCourseRepository.findByProgrammeBatchIdAndDeletedAtIsNull("batch-101"))
                .thenReturn(List.of(existing));
        when(userRepository.findAll()).thenReturn(List.of(testFaculty));

        byte[] wbBytes = createTestWorkbook(1, 1, true);
        MockMultipartFile file = new MockMultipartFile("file", "courses.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", wbBytes);

        CourseImportPreviewDto preview = importService.previewImport("batch-101", file);

        assertNotNull(preview);
        assertFalse(preview.isValid());
        assertTrue(preview.getErrorCount() > 0);
        List<CourseImportItemDto> sem1 = preview.getCoursesBySemester().get(1);
        CourseImportItemDto duplicate = sem1.stream().filter(c -> "CS101".equals(c.getCourseCode())).findFirst().orElseThrow();
        assertEquals("DUPLICATE", duplicate.getStatus());
        assertTrue(duplicate.getIssues().stream().anyMatch(i -> i.toLowerCase().contains("already exists in this programme batch")));
    }

    @Test
    @DisplayName("Commit import persists valid courses, evicts cache and logs audit")
    void testCommitImport_Success() throws IOException {
        when(programmeBatchRepository.findById("batch-101")).thenReturn(Optional.of(testBatch));
        when(masterProgrammeRepository.findById("prog-1")).thenReturn(Optional.of(testMasterProgramme));
        when(programmeBatchCourseRepository.findByProgrammeBatchIdAndDeletedAtIsNull("batch-101"))
                .thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(testFaculty));

        byte[] wbBytes = createTestWorkbook(1, 2, true);
        MockMultipartFile file = new MockMultipartFile("file", "courses.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", wbBytes);

        CourseImportResultDto result = importService.commitImport("batch-101", file);

        assertNotNull(result);
        assertEquals(4, result.getTotalCoursesImported());
        assertEquals(2, result.getCoordinatorsMatched());

        verify(academicService, atLeastOnce()).enforceSemesterAllocationEditability("batch-101", 1);
        verify(academicService, atLeastOnce()).enforceSemesterAllocationEditability("batch-101", 2);
        verify(programmeBatchCourseRepository).saveAll(any());
        verify(academicLookupCacheService).evictCourseCache();
    }

    @Test
    @DisplayName("Commit import rejects when file contains validation errors")
    void testCommitImport_RejectsOnError() throws IOException {
        when(programmeBatchRepository.findById("batch-101")).thenReturn(Optional.of(testBatch));
        when(masterProgrammeRepository.findById("prog-1")).thenReturn(Optional.of(testMasterProgramme));
        when(programmeBatchCourseRepository.findByProgrammeBatchIdAndDeletedAtIsNull("batch-101"))
                .thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(testFaculty));

        byte[] wbBytes = createTestWorkbook(1, 9, true); // Sem 9 exceeds 8 sems
        MockMultipartFile file = new MockMultipartFile("file", "courses.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", wbBytes);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                importService.commitImport("batch-101", file));

        assertTrue(ex.getReason().contains("Excel import validation failed"));
        verify(programmeBatchCourseRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Parse actual sample Course Adittion.xlsx file successfully")
    void testParseActualSampleFile() throws IOException {
        File actualFile = new File("/Users/rajshaikh/Library/Containers/net.whatsapp.WhatsApp/Data/tmp/documents/8C6089F8-10DB-4291-928C-4768F93931EA/Course Adittion.xlsx");
        if (!actualFile.exists()) return;

        when(programmeBatchRepository.findById("batch-101")).thenReturn(Optional.of(testBatch));
        when(masterProgrammeRepository.findById("prog-1")).thenReturn(Optional.of(testMasterProgramme));
        when(programmeBatchCourseRepository.findByProgrammeBatchIdAndDeletedAtIsNull("batch-101"))
                .thenReturn(Collections.emptyList());
        when(userRepository.findAll()).thenReturn(List.of(testFaculty));

        try (FileInputStream fis = new FileInputStream(actualFile)) {
            MockMultipartFile file = new MockMultipartFile("file", "Course Adittion.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", fis);

            CourseImportPreviewDto preview = importService.previewImport("batch-101", file);

            assertNotNull(preview);
            assertEquals(8, preview.getSheetsDetected().size());
            assertEquals(0, preview.getErrorCount());
            System.out.println("Parsed sample template workbook successfully: sheets = " + preview.getSheetsDetected()
                    + ", totalCourses = " + preview.getTotalCourses());
        }
    }
}
