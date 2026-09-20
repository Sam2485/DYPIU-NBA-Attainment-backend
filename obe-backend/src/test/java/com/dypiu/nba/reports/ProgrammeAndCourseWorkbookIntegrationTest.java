package com.dypiu.nba.reports;

import com.dypiu.nba.reports.excel.ExcelReportRenderer;
import com.dypiu.nba.reports.model.ReportSection;
import com.dypiu.nba.reports.model.ReportType;
import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProgrammeAndCourseWorkbookIntegrationTest {

    private ExcelReportRenderer excelRenderer;
    private ProgrammeAttainmentSnapshot sampleProgSnapshot;
    private CourseAttainmentSnapshot sampleCourseSnapshot;

    @BeforeEach
    void setUp() {
        excelRenderer = new ExcelReportRenderer();

        List<String> pos = List.of("PO1", "PO2", "PO3");
        List<String> psos = List.of("PSO1", "PSO2");

        Map<String, BigDecimal> valMap = new LinkedHashMap<>();
        for (String po : pos) valMap.put(po, new BigDecimal("2.58"));
        for (String pso : psos) valMap.put(pso, new BigDecimal("2.65"));

        List<ProgrammeAttainmentSnapshot.CourseMappingRow> courses = List.of(
                ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
                        .programmeBatchCourseId("pbc-1").courseCode("CS101").courseName("Programming Fundamentals").semester(1)
                        .poValues(valMap).psoValues(valMap).build(),
                ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
                        .programmeBatchCourseId("pbc-2").courseCode("CS102").courseName("Digital Logic").semester(1)
                        .poValues(valMap).psoValues(valMap).build()
        );

        List<ProgrammeAttainmentSnapshot.CourseDirectRow> dirCourses = List.of(
                ProgrammeAttainmentSnapshot.CourseDirectRow.builder()
                        .programmeBatchCourseId("pbc-1").courseCode("CS101").courseName("Programming Fundamentals").semester(1)
                        .poValues(valMap).psoValues(valMap).build(),
                ProgrammeAttainmentSnapshot.CourseDirectRow.builder()
                        .programmeBatchCourseId("pbc-2").courseCode("CS102").courseName("Digital Logic").semester(1)
                        .poValues(valMap).psoValues(valMap).build()
        );

        sampleProgSnapshot = ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-prog-ins-01")
                .reportType(ReportType.PROGRAMME_ATTAINMENT)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .departmentName("Department of Computer Science and Engineering")
                .masterProgrammeId("prog-btech-cse")
                .masterProgrammeCode("BTECH-CSE")
                .masterProgrammeName("B.Tech in Computer Science and Engineering")
                .programmeBatchId("batch-2021-25")
                .programmeBatchName("B.Tech CSE 2021-25")
                .academicBatchYears("2021-2025")
                .academicYear("2021-2025")
                .poCodes(pos)
                .psoCodes(psos)
                .generatedBy("Programme Coordinator")
                .generatedAt(ZonedDateTime.now())
                .section1AverageMapping(ProgrammeAttainmentSnapshot.AverageMappingSection.builder()
                        .courses(courses).averageMappingStrength(valMap).overallAverageMappingStrength(new BigDecimal("2.58")).build())
                .section2AverageDirect(ProgrammeAttainmentSnapshot.AverageDirectSection.builder()
                        .courses(dirCourses).averageDirectAttainment(valMap).overallDirectAttainment(new BigDecimal("2.58")).build())
                .section3AverageIndirect(ProgrammeAttainmentSnapshot.AverageIndirectSection.builder()
                        .surveyType("Graduate Exit Survey").totalStudents(2)
                        .studentResponses(List.of(
                                ProgrammeAttainmentSnapshot.StudentSurveyRow.builder().srNo(1).prn("PRN2021001").studentName("Aditi Sharma").poRatings(valMap).psoRatings(valMap).build(),
                                ProgrammeAttainmentSnapshot.StudentSurveyRow.builder().srNo(2).prn("PRN2021002").studentName("Rohan Verma").poRatings(valMap).psoRatings(valMap).build()
                        ))
                        .otherAssessments(List.of(
                                ProgrammeAttainmentSnapshot.IndirectAssessmentRow.builder().id("ind-1").eventTitle("Alumni Survey 2025").assessmentType("Survey").poValues(valMap).psoValues(valMap).build()
                        ))
                        .averageIndirectAttainment(valMap).overallIndirectAttainment(new BigDecimal("2.58")).build())
                .section4OverallAttainment(ProgrammeAttainmentSnapshot.OverallAttainmentSection.builder()
                        .directWeightPercentage(new BigDecimal("80.00")).indirectWeightPercentage(new BigDecimal("20.00"))
                        .averageMappingStrength(valMap).averageDirectAttainment(valMap).averageIndirectAttainment(valMap).finalAttainments(valMap)
                        .overallProgrammeAttainment(new BigDecimal("2.58")).build())
                .build();

        sampleCourseSnapshot = CourseAttainmentSnapshot.builder()
                .reportId("rep-course-ins-01")
                .reportType(ReportType.COURSE_ATTAINMENT)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .courseCode("CS201")
                .courseName("Data Structures & Algorithms")
                .semester(3)
                .batchName("B.Tech CSE 2021-25")
                .academicYear("2021-2025")
                .overallCoAttainment(new BigDecimal("2.74"))
                .directAttainment(new BigDecimal("2.80"))
                .indirectAttainment(new BigDecimal("2.50"))
                .poCodes(pos)
                .psoCodes(psos)
                .table1Mapping(List.of(
                        CourseAttainmentSnapshot.CoMappingRow.builder().coCode("CO1").poMappings(Map.of("PO1", 3, "PO2", 2, "PO3", 3)).psoMappings(Map.of("PSO1", 3, "PSO2", 2)).build()
                ))
                .table2DirectPO(List.of(
                        CourseAttainmentSnapshot.OutcomeContributionRow.builder().outcomeCode("PO1").averageMapping(new BigDecimal("2.50")).directContribution(new BigDecimal("2.45")).build()
                ))
                .table2DirectPSO(List.of(
                        CourseAttainmentSnapshot.OutcomeContributionRow.builder().outcomeCode("PSO1").averageMapping(new BigDecimal("2.50")).directContribution(new BigDecimal("2.45")).build()
                ))
                .table3CoAttainments(List.of(
                        CourseAttainmentSnapshot.CoAttainmentRow.builder()
                                .coCode("CO1")
                                .statement("Understand core data structure primitives")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("72.5"))
                                .directLevel(3)
                                .indirectPercentage(new BigDecimal("80.0"))
                                .indirectScore(new BigDecimal("2.60"))
                                .indirectLevel(3)
                                .finalAttainment(new BigDecimal("2.85"))
                                .targetMet(true)
                                .build()
                ))
                .surveyData(CourseAttainmentSnapshot.SurveySection.builder()
                        .totalStudents(2)
                        .coCodes(List.of("CO1"))
                        .level1Counts(Map.of("CO1", 0))
                        .level2Counts(Map.of("CO1", 1))
                        .level3Counts(Map.of("CO1", 1))
                        .level1Percentages(Map.of("CO1", BigDecimal.ZERO))
                        .level2Percentages(Map.of("CO1", new BigDecimal("50.00")))
                        .level3Percentages(Map.of("CO1", new BigDecimal("50.00")))
                        .overallIndirectPercentages(Map.of("CO1", new BigDecimal("83.33")))
                        .responses(List.of(
                                CourseAttainmentSnapshot.SurveyResponseRow.builder().srNo(1).coFeedbacks(Map.of("CO1", "Substantial")).build(),
                                CourseAttainmentSnapshot.SurveyResponseRow.builder().srNo(2).coFeedbacks(Map.of("CO1", "Moderate")).build()
                        ))
                        .build())
                .generatedBy("Course Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();
    }

    @Test
    @DisplayName("PROGRAMME: Complete 4-sheet workbook has exactly 4 sheets with exact names and order")
    void testProgrammeCompleteWorkbookStructure() throws Exception {
        byte[] excelBytes = excelRenderer.renderProgrammeAttainmentMaster(sampleProgSnapshot);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertEquals(4, wb.getNumberOfSheets(), "Complete Programme workbook must have exactly 4 sheets");
            assertEquals("Average Mapping", wb.getSheetName(0));
            assertEquals("Average Direct Attainment", wb.getSheetName(1));
            assertEquals("AVERAGE ATTAINMENT (ID)", wb.getSheetName(2));
            assertEquals("Overall Programme Attainment", wb.getSheetName(3));
        }
    }

    @Test
    @DisplayName("PROGRAMME: Individual sheet downloads produce exactly 1 sheet with the exact finalized name")
    void testProgrammeIndividualSheetDownloads() throws Exception {
        Map<ReportSection, String> expectedNames = Map.of(
                ReportSection.AVERAGE_MAPPING, "Average Mapping",
                ReportSection.AVERAGE_DIRECT, "Average Direct Attainment",
                ReportSection.AVERAGE_INDIRECT, "AVERAGE ATTAINMENT (ID)",
                ReportSection.OVERALL, "Overall Programme Attainment"
        );

        for (Map.Entry<ReportSection, String> entry : expectedNames.entrySet()) {
            byte[] bytes = excelRenderer.renderProgrammeAttainmentSection(sampleProgSnapshot, entry.getKey());
            assertNotNull(bytes, "Section excel must not be null for " + entry.getKey());
            assertTrue(bytes.length > 0);

            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                assertEquals(1, wb.getNumberOfSheets(), "Individual section workbook must contain exactly 1 sheet");
                assertEquals(entry.getValue(), wb.getSheetName(0), "Sheet name must match exact finalized name");
            }
        }
    }

    @Test
    @DisplayName("PROGRAMME: Individual sheet download content matches corresponding sheet inside complete workbook")
    void testProgrammeContentConsistency() throws Exception {
        byte[] masterBytes = excelRenderer.renderProgrammeAttainmentMaster(sampleProgSnapshot);

        try (Workbook masterWb = new XSSFWorkbook(new ByteArrayInputStream(masterBytes))) {
            // Test Sheet 0: Average Mapping
            byte[] mappingBytes = excelRenderer.renderProgrammeAttainmentSection(sampleProgSnapshot, ReportSection.AVERAGE_MAPPING);
            try (Workbook indWb = new XSSFWorkbook(new ByteArrayInputStream(mappingBytes))) {
                Sheet sMaster = masterWb.getSheetAt(0);
                Sheet sInd = indWb.getSheetAt(0);
                assertSheetValuesMatch(sMaster, sInd);
            }

            // Test Sheet 1: Average Direct Attainment
            byte[] directBytes = excelRenderer.renderProgrammeAttainmentSection(sampleProgSnapshot, ReportSection.AVERAGE_DIRECT);
            try (Workbook indWb = new XSSFWorkbook(new ByteArrayInputStream(directBytes))) {
                Sheet sMaster = masterWb.getSheetAt(1);
                Sheet sInd = indWb.getSheetAt(0);
                assertSheetValuesMatch(sMaster, sInd);
            }

            // Test Sheet 2: AVERAGE ATTAINMENT (ID)
            byte[] indirectBytes = excelRenderer.renderProgrammeAttainmentSection(sampleProgSnapshot, ReportSection.AVERAGE_INDIRECT);
            try (Workbook indWb = new XSSFWorkbook(new ByteArrayInputStream(indirectBytes))) {
                Sheet sMaster = masterWb.getSheetAt(2);
                Sheet sInd = indWb.getSheetAt(0);
                assertSheetValuesMatch(sMaster, sInd);
            }

            // Test Sheet 3: Overall Programme Attainment
            byte[] overallBytes = excelRenderer.renderProgrammeAttainmentSection(sampleProgSnapshot, ReportSection.OVERALL);
            try (Workbook indWb = new XSSFWorkbook(new ByteArrayInputStream(overallBytes))) {
                Sheet sMaster = masterWb.getSheetAt(3);
                Sheet sInd = indWb.getSheetAt(0);
                assertSheetValuesMatch(sMaster, sInd);
            }
        }
    }

    @Test
    @DisplayName("COURSE: Complete 5-sheet workbook has exactly 5 sheets with exact names and order")
    void testCourseCompleteWorkbookStructure() throws Exception {
        byte[] excelBytes = excelRenderer.renderCourseAttainment(sampleCourseSnapshot);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            assertEquals(5, wb.getNumberOfSheets(), "Complete Course workbook must have exactly 5 sheets");
            assertEquals("Attainment-main", wb.getSheetName(0));
            assertEquals("PO mapping", wb.getSheetName(1));
            assertEquals("PSO mapping", wb.getSheetName(2));
            assertEquals("Examination", wb.getSheetName(3));
            assertEquals("Course End Survey", wb.getSheetName(4));
        }
    }

    private void assertSheetValuesMatch(Sheet expected, Sheet actual) {
        assertEquals(expected.getSheetName(), actual.getSheetName());
        int lastRow = Math.min(expected.getLastRowNum(), actual.getLastRowNum());
        for (int r = 0; r <= lastRow; r++) {
            Row rExp = expected.getRow(r);
            Row rAct = actual.getRow(r);
            if (rExp == null || rAct == null) continue;

            int lastCell = Math.min(rExp.getLastCellNum(), rAct.getLastCellNum());
            for (int c = 0; c < lastCell; c++) {
                Cell cExp = rExp.getCell(c);
                Cell cAct = rAct.getCell(c);
                if (cExp == null || cAct == null) continue;

                if (cExp.getCellType() == CellType.STRING) {
                    assertEquals(cExp.getStringCellValue(), cAct.getStringCellValue(),
                            "Mismatch at Row " + r + ", Col " + c);
                } else if (cExp.getCellType() == CellType.NUMERIC) {
                    assertEquals(cExp.getNumericCellValue(), cAct.getNumericCellValue(), 0.001,
                            "Numeric mismatch at Row " + r + ", Col " + c);
                }
            }
        }
    }
}
