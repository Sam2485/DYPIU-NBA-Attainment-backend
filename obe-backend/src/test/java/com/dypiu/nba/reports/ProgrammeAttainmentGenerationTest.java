package com.dypiu.nba.reports;

import com.dypiu.nba.reports.excel.ExcelReportRenderer;
import com.dypiu.nba.reports.model.ReportSection;
import com.dypiu.nba.reports.model.ReportType;
import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import com.dypiu.nba.reports.pdf.PdfReportRenderer;
import com.dypiu.nba.reports.template.HeaderConfig;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProgrammeAttainmentGenerationTest {

    private ExcelReportRenderer excelRenderer;
    private PdfReportRenderer pdfRenderer;
    private ProgrammeAttainmentSnapshot sampleSnapshot;
    private ReportTemplateDto sampleTemplate;

    @BeforeEach
    void setUp() {
        excelRenderer = new ExcelReportRenderer();
        pdfRenderer = new PdfReportRenderer();

        List<String> poCodes = List.of("PO1", "PO2", "PO3", "PO4", "PO5", "PO6", "PO7", "PO8", "PO9", "PO10", "PO11", "PO12");
        List<String> psoCodes = List.of("PSO1", "PSO2");

        Map<String, BigDecimal> avgMapping = Map.of(
                "PO1", new BigDecimal("2.65"),
                "PO2", new BigDecimal("2.45"),
                "PO12", new BigDecimal("2.60"),
                "PSO1", new BigDecimal("2.55"),
                "PSO2", new BigDecimal("2.40")
        );

        Map<String, BigDecimal> avgDirect = Map.of(
                "PO1", new BigDecimal("2.48"),
                "PO2", new BigDecimal("2.32"),
                "PO12", new BigDecimal("2.45"),
                "PSO1", new BigDecimal("2.52"),
                "PSO2", new BigDecimal("2.38")
        );

        Map<String, BigDecimal> avgIndirect = Map.of(
                "PO1", new BigDecimal("2.85"),
                "PO2", new BigDecimal("2.70"),
                "PO12", new BigDecimal("2.85"),
                "PSO1", new BigDecimal("2.80"),
                "PSO2", new BigDecimal("2.75")
        );

        Map<String, BigDecimal> finalAttainments = Map.of(
                "PO1", new BigDecimal("2.55"),
                "PO2", new BigDecimal("2.40"),
                "PO12", new BigDecimal("2.53"),
                "PSO1", new BigDecimal("2.58"),
                "PSO2", new BigDecimal("2.45")
        );

        sampleSnapshot = ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-prog-test-01")
                .reportType(ReportType.PROGRAMME_ATTAINMENT)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .masterProgrammeId("prog-btech-cse")
                .masterProgrammeCode("BTECH-CSE")
                .masterProgrammeName("B.Tech Computer Science & Engineering")
                .programmeBatchId("batch-2021-25")
                .programmeBatchName("B.Tech CSE 2021-25")
                .academicBatchYears("2021–2025")
                .poCodes(poCodes)
                .psoCodes(psoCodes)
                .generatedBy("Programme Coordinator")
                .generatedAt(ZonedDateTime.now())
                .section1AverageMapping(ProgrammeAttainmentSnapshot.AverageMappingSection.builder()
                        .courses(List.of(
                                ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
                                        .programmeBatchCourseId("pbc-1")
                                        .courseCode("CS101")
                                        .courseName("Problem Solving and Programming")
                                        .semester(1)
                                        .poValues(Map.of("PO1", new BigDecimal("3.00"), "PO2", new BigDecimal("2.00")))
                                        .psoValues(Map.of("PSO1", new BigDecimal("3.00"), "PSO2", new BigDecimal("2.00")))
                                        .build(),
                                ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
                                        .programmeBatchCourseId("pbc-2")
                                        .courseCode("CS201")
                                        .courseName("Data Structures")
                                        .semester(3)
                                        .poValues(Map.of("PO1", new BigDecimal("3.00"), "PO2", new BigDecimal("3.00")))
                                        .psoValues(Map.of("PSO1", new BigDecimal("3.00"), "PSO2", new BigDecimal("3.00")))
                                        .build()
                        ))
                        .averageMappingStrength(avgMapping)
                        .overallAverageMappingStrength(new BigDecimal("2.42"))
                        .build())
                .section2AverageDirect(ProgrammeAttainmentSnapshot.AverageDirectSection.builder()
                        .courses(List.of(
                                ProgrammeAttainmentSnapshot.CourseDirectRow.builder()
                                        .programmeBatchCourseId("pbc-1")
                                        .courseCode("CS101")
                                        .courseName("Problem Solving and Programming")
                                        .semester(1)
                                        .poValues(Map.of("PO1", new BigDecimal("2.80"), "PO2", new BigDecimal("1.87")))
                                        .psoValues(Map.of("PSO1", new BigDecimal("2.80"), "PSO2", new BigDecimal("1.87")))
                                        .build()
                        ))
                        .averageDirectAttainment(avgDirect)
                        .overallDirectAttainment(new BigDecimal("2.27"))
                        .build())
                .section3AverageIndirect(ProgrammeAttainmentSnapshot.AverageIndirectSection.builder()
                        .surveyType("Graduate Exit Survey")
                        .totalStudents(2)
                        .studentResponses(List.of(
                                ProgrammeAttainmentSnapshot.StudentSurveyRow.builder()
                                        .srNo(1)
                                        .prn("PRN2021001")
                                        .studentName("Aarav Sharma")
                                        .poRatings(Map.of("PO1", new BigDecimal("3.00"), "PO2", new BigDecimal("3.00")))
                                        .psoRatings(Map.of("PSO1", new BigDecimal("3.00"), "PSO2", new BigDecimal("3.00")))
                                        .build()
                        ))
                        .averageIndirectAttainment(avgIndirect)
                        .overallIndirectAttainment(new BigDecimal("2.71"))
                        .build())
                .section4OverallAttainment(ProgrammeAttainmentSnapshot.OverallAttainmentSection.builder()
                        .directWeightPercentage(new BigDecimal("80.00"))
                        .indirectWeightPercentage(new BigDecimal("20.00"))
                        .averageMappingStrength(avgMapping)
                        .averageDirectAttainment(avgDirect)
                        .averageIndirectAttainment(avgIndirect)
                        .finalAttainments(finalAttainments)
                        .overallProgrammeAttainment(new BigDecimal("2.36"))
                        .build())
                .build();

        sampleTemplate = ReportTemplateDto.builder()
                .headerConfig(HeaderConfig.builder()
                        .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                        .build())
                .build();
    }

    @Test
    @DisplayName("Master Programme Attainment Excel contains exactly 4 sheets in correct order")
    void testMasterExcelContainsFourSheets() throws Exception {
        byte[] masterExcel = excelRenderer.renderProgrammeAttainmentMaster(sampleSnapshot);
        assertNotNull(masterExcel);
        assertTrue(masterExcel.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(masterExcel))) {
            assertEquals(4, wb.getNumberOfSheets());
            assertEquals("Average Mapping", wb.getSheetName(0));
            assertEquals("Average Direct Attainment", wb.getSheetName(1));
            assertEquals("Average Indirect Attainment", wb.getSheetName(2));
            assertEquals("Overall Attainment", wb.getSheetName(3));
        }
    }

    @Test
    @DisplayName("Each Programme Attainment Section generates independently for both PDF and Excel")
    void testIndividualSectionDownloads() throws Exception {
        for (ReportSection section : List.of(
                ReportSection.AVERAGE_MAPPING,
                ReportSection.AVERAGE_DIRECT,
                ReportSection.AVERAGE_INDIRECT,
                ReportSection.OVERALL)) {

            byte[] excelBytes = excelRenderer.renderProgrammeAttainmentSection(sampleSnapshot, section);
            assertNotNull(excelBytes, "Excel should not be null for section: " + section);
            assertTrue(excelBytes.length > 0);

            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
                assertEquals(1, wb.getNumberOfSheets());
            }

            byte[] pdfBytes = pdfRenderer.renderProgrammeAttainmentSection(sampleSnapshot, section, sampleTemplate);
            assertNotNull(pdfBytes, "PDF should not be null for section: " + section);
            assertTrue(pdfBytes.length > 0);
            // Verify PDF header %PDF
            String pdfHeader = new String(pdfBytes, 0, 4);
            assertEquals("%PDF", pdfHeader);
        }
    }

    @Test
    @DisplayName("Master Programme Attainment PDF generates successfully")
    void testMasterPdfGeneration() {
        byte[] masterPdf = pdfRenderer.renderProgrammeAttainmentMaster(sampleSnapshot, sampleTemplate);
        assertNotNull(masterPdf);
        assertTrue(masterPdf.length > 0);
        String pdfHeader = new String(masterPdf, 0, 4);
        assertEquals("%PDF", pdfHeader);
    }
}
