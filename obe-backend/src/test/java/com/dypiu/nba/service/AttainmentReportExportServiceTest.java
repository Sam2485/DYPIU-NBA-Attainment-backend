package com.dypiu.nba.service;

import com.dypiu.nba.dto.CourseMappingMatrixDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttainmentReportExportServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private ProgrammeRepository programmeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private SchoolRepository schoolRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private CourseOutcomeRepository courseOutcomeRepository;
    @Mock
    private StudentCoMarkRepository studentCoMarkRepository;
    @Mock
    private CourseOfferingRepository courseOfferingRepository;
    @Mock
    private AttainmentCalculationService calculationService;
    @Mock
    private OutcomeService outcomeService;

    @InjectMocks
    private AttainmentReportExportService exportService;

    private Course sampleCourse;
    private CourseOffering sampleOffering;
    private Batch sampleBatch;
    private Programme sampleProgramme;
    private List<CourseOutcome> sampleCos;

    @BeforeEach
    void setUp() {
        sampleCourse = Course.builder()
                .id("crs-1")
                .code("310244")
                .name("Computer Network and Security")
                .programmeId("prog-1")
                .build();

        sampleOffering = CourseOffering.builder()
                .id("offering-crs-1-batch-1")
                .courseId("crs-1")
                .batchId("batch-1")
                .semester(5)
                .build();

        sampleBatch = Batch.builder()
                .id("batch-1")
                .name("Batch 2025-29")
                .programmeId("prog-1")
                .startYear(2025)
                .endYear(2029)
                .build();


        sampleProgramme = Programme.builder()
                .id("prog-1")
                .code("BTECH-CSE")
                .name("B.Tech Computer Science and Engineering")
                .departmentId("dept-1")
                .build();

        sampleCos = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            sampleCos.add(CourseOutcome.builder()
                    .id("co-" + i)
                    .courseOfferingId("offering-crs-1-batch-1")
                    .code("CO" + i)
                    .statement("Statement for CO" + i)
                    .build());
        }
    }

    @Test
    void testGenerateAttainmentExcel_CreatesValidWorkbook() throws Exception {
        when(courseRepository.findById("crs-1")).thenReturn(Optional.of(sampleCourse));
        when(programmeRepository.findById("prog-1")).thenReturn(Optional.of(sampleProgramme));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(sampleBatch));
        when(courseOfferingRepository.findByCourseId("crs-1")).thenReturn(List.of(sampleOffering));

        Map<String, Object> coCalcData = new HashMap<>();
        coCalcData.put("overallCoAttainment", new BigDecimal("2.50"));
        List<Map<String, Object>> coAttList = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("coCode", "CO" + i);
            cm.put("directPct", new BigDecimal("75.00"));
            cm.put("directLevel", 3);
            cm.put("indirectPct", new BigDecimal("80.00"));
            cm.put("indirectLevel", 3);
            cm.put("combinedAttainment", new BigDecimal("2.60"));
            coAttList.add(cm);
        }
        coCalcData.put("coAttainments", coAttList);

        when(calculationService.calculateCourseCoAttainment("crs-1")).thenReturn(coCalcData);
        when(outcomeService.getCOsByCourse("crs-1")).thenReturn(sampleCos);
        when(outcomeService.getCourseMappings("crs-1")).thenReturn(CourseMappingMatrixDto.builder().build());

        byte[] excelBytes = exportService.generateAttainmentExcel("crs-1", "batch-1");

        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertNotNull(wb.getSheet("Attainment-main"));
        }
    }

    @Test
    void testGenerateAttainmentPdf_CreatesValidPdf() {
        when(courseRepository.findById("crs-1")).thenReturn(Optional.of(sampleCourse));
        when(programmeRepository.findById("prog-1")).thenReturn(Optional.of(sampleProgramme));
        when(batchRepository.findById("batch-1")).thenReturn(Optional.of(sampleBatch));
        when(courseOfferingRepository.findByCourseId("crs-1")).thenReturn(List.of(sampleOffering));

        Map<String, Object> coCalcData = new HashMap<>();
        coCalcData.put("overallCoAttainment", new BigDecimal("2.50"));
        List<Map<String, Object>> coAttList = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("coCode", "CO" + i);
            cm.put("directPct", new BigDecimal("75.00"));
            cm.put("directLevel", 3);
            cm.put("indirectPct", new BigDecimal("80.00"));
            cm.put("indirectLevel", 3);
            cm.put("combinedAttainment", new BigDecimal("2.60"));
            coAttList.add(cm);
        }
        coCalcData.put("coAttainments", coAttList);

        when(calculationService.calculateCourseCoAttainment("crs-1")).thenReturn(coCalcData);
        when(outcomeService.getCOsByCourse("crs-1")).thenReturn(sampleCos);
        when(outcomeService.getCourseMappings("crs-1")).thenReturn(CourseMappingMatrixDto.builder().build());

        byte[] pdfBytes = exportService.generateAttainmentPdf("crs-1", "batch-1");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);
        // PDF header magic bytes "%PDF"
        assertEquals('%', (char) pdfBytes[0]);
        assertEquals('P', (char) pdfBytes[1]);
        assertEquals('D', (char) pdfBytes[2]);
        assertEquals('F', (char) pdfBytes[3]);
    }
}
