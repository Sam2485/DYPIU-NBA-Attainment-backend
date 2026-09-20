package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AverageIndirectAttainmentSheetTest {

    private ProgrammeAttainmentSnapshot createSampleSnapshot(
            List<String> poCodes,
            List<String> psoCodes,
            int studentCount,
            List<ProgrammeAttainmentSnapshot.IndirectAssessmentRow> otherAssessments) {

        List<ProgrammeAttainmentSnapshot.StudentSurveyRow> studentRows = new ArrayList<>();
        for (int i = 1; i <= studentCount; i++) {
            Map<String, BigDecimal> poVals = new LinkedHashMap<>();
            for (String po : poCodes) poVals.put(po, new BigDecimal("2.65"));
            Map<String, BigDecimal> psoVals = new LinkedHashMap<>();
            for (String pso : psoCodes) psoVals.put(pso, new BigDecimal("2.85"));

            studentRows.add(ProgrammeAttainmentSnapshot.StudentSurveyRow.builder()
                    .srNo(i)
                    .prn("PRN2024" + String.format("%03d", i))
                    .studentName("Student " + i)
                    .poRatings(poVals)
                    .psoRatings(psoVals)
                    .build());
        }

        Map<String, BigDecimal> avgIndirect = new LinkedHashMap<>();
        for (String po : poCodes) avgIndirect.put(po, new BigDecimal("2.65"));
        for (String pso : psoCodes) avgIndirect.put(pso, new BigDecimal("2.85"));

        return ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-ind-001")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .departmentName("Department of Computer Science and Engineering")
                .masterProgrammeName("B.Tech Computer Science")
                .masterProgrammeCode("BTECH-CSE")
                .academicYear("2024-2028")
                .poCodes(poCodes)
                .psoCodes(psoCodes)
                .section3AverageIndirect(ProgrammeAttainmentSnapshot.AverageIndirectSection.builder()
                        .surveyType("Graduate Exit Survey")
                        .totalStudents(studentCount)
                        .studentResponses(studentRows)
                        .otherAssessments(otherAssessments)
                        .averageIndirectAttainment(avgIndirect)
                        .overallIndirectAttainment(new BigDecimal("2.70"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("Requirement 0 & 18: Sheet name is exactly 'AVERAGE ATTAINMENT (ID)'")
    void testSheetNameIsAverageAttainmentID() {
        List<String> pos = List.of("PO1", "PO2", "PO3");
        List<String> psos = List.of("PSO1");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, 2, Collections.emptyList());
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageIndirectAttainmentSheetBuilder.build(wb, null, snapshot);
        assertNotNull(sheet);
        assertEquals("AVERAGE ATTAINMENT (ID)", sheet.getSheetName(), "Sheet name must be exactly 'AVERAGE ATTAINMENT (ID)'");

        // Overloaded build without sheetName parameter
        Workbook wb2 = new XSSFWorkbook();
        Sheet sheet2 = AverageIndirectAttainmentSheetBuilder.build(wb2, snapshot);
        assertNotNull(sheet2);
        assertEquals("AVERAGE ATTAINMENT (ID)", sheet2.getSheetName());
    }

    @Test
    @DisplayName("Requirement 4, 10 & 12: Programme End Survey Section contains student-level data (Sr No, PRN, Student Name, POs, PSOs)")
    void testProgrammeEndSurveySection() {
        List<String> pos = List.of("PO1", "PO2", "PO3");
        List<String> psos = List.of("PSO1", "PSO2");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, 3, Collections.emptyList());
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageIndirectAttainmentSheetBuilder.build(wb, "AVERAGE ATTAINMENT (ID)", snapshot);
        Row th = sheet.getRow(8);
        assertNotNull(th, "Table header row 8 must exist");

        assertEquals("Sr No", th.getCell(0).getStringCellValue());
        assertEquals("PRN", th.getCell(1).getStringCellValue());
        assertEquals("Name of the Student", th.getCell(2).getStringCellValue());
        assertEquals("PO1", th.getCell(3).getStringCellValue());
        assertEquals("PO2", th.getCell(4).getStringCellValue());
        assertEquals("PO3", th.getCell(5).getStringCellValue());
        assertEquals("PSO1", th.getCell(6).getStringCellValue());
        assertEquals("PSO2", th.getCell(7).getStringCellValue());

        // First student row (index 9)
        Row s1 = sheet.getRow(9);
        assertEquals("1", s1.getCell(0).getStringCellValue());
        assertEquals("PRN2024001", s1.getCell(1).getStringCellValue());
        assertEquals("Student 1", s1.getCell(2).getStringCellValue());
        assertEquals(2.65, s1.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.85, s1.getCell(6).getNumericCellValue(), 0.001);
    }

    @Test
    @DisplayName("Requirement 4, 5, 6, 7, 8: Other Indirect Assessments rendered with Event Title, Assessment Type, and PO/PSO values (No PRN or Student Name)")
    void testOtherProgrammeIndirectAssessmentsSection() {
        List<String> pos = List.of("PO1", "PO2", "PO3");
        List<String> psos = List.of("PSO1");

        List<ProgrammeAttainmentSnapshot.IndirectAssessmentRow> others = List.of(
                ProgrammeAttainmentSnapshot.IndirectAssessmentRow.builder()
                        .id("ind-1")
                        .eventTitle("Alumni Exit Survey 2024")
                        .assessmentType("Survey")
                        .poValues(Map.of("PO1", new BigDecimal("2.40"), "PO2", new BigDecimal("2.65"), "PO3", new BigDecimal("2.30")))
                        .psoValues(Map.of("PSO1", new BigDecimal("2.55")))
                        .build(),
                ProgrammeAttainmentSnapshot.IndirectAssessmentRow.builder()
                        .id("ind-2")
                        .eventTitle("Industry Interaction - Cloud Architecture")
                        .assessmentType("Co-Curricular Event")
                        .poValues(Map.of("PO1", new BigDecimal("2.70"), "PO2", new BigDecimal("2.50"), "PO3", new BigDecimal("2.80")))
                        .psoValues(Map.of("PSO1", new BigDecimal("2.40")))
                        .build(),
                ProgrammeAttainmentSnapshot.IndirectAssessmentRow.builder()
                        .id("ind-3")
                        .eventTitle("Parent Feedback Forum")
                        .assessmentType("Survey")
                        .poValues(Map.of("PO1", new BigDecimal("2.20"), "PO2", new BigDecimal("2.45"), "PO3", new BigDecimal("2.35")))
                        .psoValues(Map.of("PSO1", new BigDecimal("2.50")))
                        .build()
        );

        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, 2, others);
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageIndirectAttainmentSheetBuilder.build(wb, "AVERAGE ATTAINMENT (ID)", snapshot);

        // Row 8: Student Survey Table Header
        // Row 9, 10: Student 1 & 2 rows
        // Row 11: Spacer
        // Row 12: PROGRAMME INDIRECT ASSESSMENTS section title
        Row secTitleRow = sheet.getRow(12);
        assertNotNull(secTitleRow);
        assertEquals("PROGRAMME INDIRECT ASSESSMENTS", secTitleRow.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 12, 12, 0, 6), "Section header must be merged across all columns");

        // Row 13: Other Indirect Assessments Table Header
        Row othHeaderRow = sheet.getRow(13);
        assertNotNull(othHeaderRow);
        assertEquals("Event Title", othHeaderRow.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 13, 13, 0, 1), "Event Title must be merged cols 0..1");
        assertEquals("Assessment Type", othHeaderRow.getCell(2).getStringCellValue());
        assertEquals("PO1", othHeaderRow.getCell(3).getStringCellValue());
        assertEquals("PO2", othHeaderRow.getCell(4).getStringCellValue());
        assertEquals("PO3", othHeaderRow.getCell(5).getStringCellValue());
        assertEquals("PSO1", othHeaderRow.getCell(6).getStringCellValue());

        // Row 14: Alumni Exit Survey 2024
        Row r1 = sheet.getRow(14);
        assertNotNull(r1);
        assertEquals("Alumni Exit Survey 2024", r1.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 14, 14, 0, 1));
        assertEquals("Survey", r1.getCell(2).getStringCellValue());
        assertEquals(2.40, r1.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.65, r1.getCell(4).getNumericCellValue(), 0.001);
        assertEquals(2.30, r1.getCell(5).getNumericCellValue(), 0.001);
        assertEquals(2.55, r1.getCell(6).getNumericCellValue(), 0.001);

        // Row 15: Industry Interaction
        Row r2 = sheet.getRow(15);
        assertEquals("Industry Interaction - Cloud Architecture", r2.getCell(0).getStringCellValue());
        assertEquals("Co-Curricular Event", r2.getCell(2).getStringCellValue());
        assertEquals(2.70, r2.getCell(3).getNumericCellValue(), 0.001);

        // Row 16: Parent Feedback Forum
        Row r3 = sheet.getRow(16);
        assertEquals("Parent Feedback Forum", r3.getCell(0).getStringCellValue());
        assertEquals("Survey", r3.getCell(2).getStringCellValue());
        assertEquals(2.20, r3.getCell(3).getNumericCellValue(), 0.001);

        // Row 17: Summary Row: Average Attainment (Indirect)
        Row sumRow = sheet.getRow(17);
        assertNotNull(sumRow);
        assertEquals("Average Attainment (Indirect)", sumRow.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 17, 17, 0, 2), "Summary row must be merged across A..C");
        assertEquals(2.65, sumRow.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.85, sumRow.getCell(6).getNumericCellValue(), 0.001);
    }

    @Test
    @DisplayName("Requirement 12: Strict Numerical PO/PSO Ordering - PO10 comes after PO9, all POs before PSOs")
    void testStrictNumericalPoPsoOrdering() {
        List<String> mixedPos = List.of("PO10", "PO3", "PO1", "PO2", "PO12", "PO9");
        List<String> mixedPsos = List.of("PSO2", "PSO1");

        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(mixedPos, mixedPsos, 1, Collections.emptyList());
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageIndirectAttainmentSheetBuilder.build(wb, "AVERAGE ATTAINMENT (ID)", snapshot);
        Row th = sheet.getRow(8);

        assertEquals("PO1", th.getCell(3).getStringCellValue());
        assertEquals("PO2", th.getCell(4).getStringCellValue());
        assertEquals("PO3", th.getCell(5).getStringCellValue());
        assertEquals("PO9", th.getCell(6).getStringCellValue());
        assertEquals("PO10", th.getCell(7).getStringCellValue(), "PO10 must come AFTER PO9, never between PO1 and PO2");
        assertEquals("PO12", th.getCell(8).getStringCellValue());
        assertEquals("PSO1", th.getCell(9).getStringCellValue(), "All POs must appear before PSOs");
        assertEquals("PSO2", th.getCell(10).getStringCellValue());
    }

    @Test
    @DisplayName("Requirement 20: Header scaling invariance test - Logo remains fixed when columns increase")
    void testHeaderScalingInvariance() {
        // Case A: 12 PO + 3 PSO -> 18 cols (0 to 17)
        List<String> pos12 = java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(i -> "PO" + i).toList();
        List<String> psos3 = List.of("PSO1", "PSO2", "PSO3");
        ProgrammeAttainmentSnapshot snapA = createSampleSnapshot(pos12, psos3, 1, Collections.emptyList());
        Workbook wbA = new XSSFWorkbook();
        Sheet sheetA = AverageIndirectAttainmentSheetBuilder.build(wbA, "AVERAGE ATTAINMENT (ID)", snapA);

        // Case B: 20 PO + 5 PSO -> 28 cols (0 to 27)
        List<String> pos20 = java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(i -> "PO" + i).toList();
        List<String> psos5 = java.util.stream.IntStream.rangeClosed(1, 5).mapToObj(i -> "PSO" + i).toList();
        ProgrammeAttainmentSnapshot snapB = createSampleSnapshot(pos20, psos5, 1, Collections.emptyList());
        Workbook wbB = new XSSFWorkbook();
        Sheet sheetB = AverageIndirectAttainmentSheetBuilder.build(wbB, "AVERAGE ATTAINMENT (ID)", snapB);

        // Case A Header:
        assertTrue(hasMergedRegion(sheetA.getMergedRegions(), 1, 2, 0, 1), "Left logo region: cols 0..1");
        assertTrue(hasMergedRegion(sheetA.getMergedRegions(), 1, 2, 14, 17), "Right metadata: cols 14..17");
        assertTrue(hasMergedRegion(sheetA.getMergedRegions(), 1, 1, 2, 13), "Center region: cols 2..13");

        // Case B Header:
        assertTrue(hasMergedRegion(sheetB.getMergedRegions(), 1, 2, 0, 1), "Left logo region STILL cols 0..1 (fixed!)");
        assertTrue(hasMergedRegion(sheetB.getMergedRegions(), 1, 2, 24, 27), "Right metadata STILL 4 cols (fixed!)");
        assertTrue(hasMergedRegion(sheetB.getMergedRegions(), 1, 1, 2, 23), "Center absorbs all 10 extra cols (cols 2..23)!");
    }

    @Test
    @DisplayName("Requirement 17: Empty other assessments does not create fake rows or crash")
    void testEmptyOtherAssessments() {
        List<String> pos = List.of("PO1", "PO2");
        List<String> psos = List.of("PSO1");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, 2, Collections.emptyList());
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageIndirectAttainmentSheetBuilder.build(wb, "AVERAGE ATTAINMENT (ID)", snapshot);
        // Student survey rows at 9 and 10, then summary row at 11
        Row sumRow = sheet.getRow(11);
        assertNotNull(sumRow);
        assertEquals("Average Attainment (Indirect)", sumRow.getCell(0).getStringCellValue());
    }

    @Test
    @DisplayName("Requirement 17: Empty student responses handled cleanly without crash")
    void testEmptyStudentResponses() {
        List<String> pos = List.of("PO1", "PO2");
        List<String> psos = List.of("PSO1");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, 0, Collections.emptyList());
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageIndirectAttainmentSheetBuilder.build(wb, "AVERAGE ATTAINMENT (ID)", snapshot);
        Row emptyRow = sheet.getRow(9);
        assertNotNull(emptyRow);
        assertTrue(emptyRow.getCell(0).getStringCellValue().contains("No student exit survey responses"));
    }

    private boolean hasMergedRegion(List<CellRangeAddress> regions, int firstRow, int lastRow, int firstCol, int lastCol) {
        return regions.stream().anyMatch(r ->
                r.getFirstRow() == firstRow &&
                r.getLastRow() == lastRow &&
                r.getFirstColumn() == firstCol &&
                r.getLastColumn() == lastCol);
    }
}
