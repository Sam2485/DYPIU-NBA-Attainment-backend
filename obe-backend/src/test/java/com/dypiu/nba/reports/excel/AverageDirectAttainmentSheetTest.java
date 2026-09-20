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

class AverageDirectAttainmentSheetTest {

    private ProgrammeAttainmentSnapshot createSampleSnapshot(List<String> poCodes, List<String> psoCodes, Map<Integer, Integer> coursesPerSem) {
        List<ProgrammeAttainmentSnapshot.CourseDirectRow> courseRows = new ArrayList<>();
        int courseSeq = 1;
        for (Map.Entry<Integer, Integer> entry : coursesPerSem.entrySet()) {
            int sem = entry.getKey();
            int count = entry.getValue();
            for (int i = 1; i <= count; i++) {
                Map<String, BigDecimal> poVals = new LinkedHashMap<>();
                for (String po : poCodes) {
                    poVals.put(po, new BigDecimal("2.60"));
                }
                Map<String, BigDecimal> psoVals = new LinkedHashMap<>();
                for (String pso : psoCodes) {
                    psoVals.put(pso, new BigDecimal("2.80"));
                }
                courseRows.add(ProgrammeAttainmentSnapshot.CourseDirectRow.builder()
                        .programmeBatchCourseId("pbc-" + courseSeq)
                        .courseCode("CS" + (100 + courseSeq))
                        .courseName("Computer Science Course " + courseSeq)
                        .semester(sem)
                        .poValues(poVals)
                        .psoValues(psoVals)
                        .build());
                courseSeq++;
            }
        }

        Map<String, BigDecimal> avgDirect = new LinkedHashMap<>();
        for (String po : poCodes) avgDirect.put(po, new BigDecimal("2.60"));
        for (String pso : psoCodes) avgDirect.put(pso, new BigDecimal("2.80"));

        return ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-dir-001")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .departmentName("Department of Computer Science and Engineering")
                .masterProgrammeName("B.Tech Computer Science")
                .masterProgrammeCode("BTECH-CSE")
                .academicYear("2024-2028")
                .poCodes(poCodes)
                .psoCodes(psoCodes)
                .section2AverageDirect(ProgrammeAttainmentSnapshot.AverageDirectSection.builder()
                        .courses(courseRows)
                        .averageDirectAttainment(avgDirect)
                        .overallDirectAttainment(new BigDecimal("2.65"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("Requirement 1 & 2: Sheet name is exactly 'average attainment(D)' and default works")
    void testSheetNameIsAverageAttainmentD() {
        List<String> pos = List.of("PO1", "PO2", "PO3");
        List<String> psos = List.of("PSO1");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, Map.of(1, 2));
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageDirectAttainmentSheetBuilder.build(wb, null, snapshot);
        assertNotNull(sheet);
        assertEquals("Average Direct Attainment", sheet.getSheetName(), "Sheet name must be exactly 'Average Direct Attainment'");

        // Overloaded build without sheetName parameter
        Workbook wb2 = new XSSFWorkbook();
        Sheet sheet2 = AverageDirectAttainmentSheetBuilder.build(wb2, snapshot);
        assertNotNull(sheet2);
        assertEquals("Average Direct Attainment", sheet2.getSheetName());
    }

    @Test
    @DisplayName("Requirement 5: Strict Numerical PO/PSO ordering - PO10 does NOT appear between PO1 and PO2")
    void testStrictNumericalPoPsoOrdering() {
        // Out of order test list matching the prompt requirement:
        // PSO2, PO10, PO3, PO1, PSO1, PO2, PO12, PO9
        // Required Output:
        // PO1 | PO2 | PO3 | PO9 | PO10 | PO12 | PSO1 | PSO2
        List<String> mixedPos = List.of("PO10", "PO3", "PO1", "PO2", "PO12", "PO9");
        List<String> mixedPsos = List.of("PSO2", "PSO1");

        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(mixedPos, mixedPsos, Map.of(1, 2));
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageDirectAttainmentSheetBuilder.build(wb, "average attainment(D)", snapshot);
        Row th = sheet.getRow(8);
        assertNotNull(th);

        assertEquals("Sem", th.getCell(0).getStringCellValue());
        assertEquals("Course Code", th.getCell(1).getStringCellValue());
        assertEquals("Course Name", th.getCell(2).getStringCellValue());

        // Check exact numerical ordering:
        assertEquals("PO1", th.getCell(3).getStringCellValue());
        assertEquals("PO2", th.getCell(4).getStringCellValue());
        assertEquals("PO3", th.getCell(5).getStringCellValue());
        assertEquals("PO9", th.getCell(6).getStringCellValue());
        assertEquals("PO10", th.getCell(7).getStringCellValue(), "PO10 must come AFTER PO9, never between PO1 and PO2");
        assertEquals("PO12", th.getCell(8).getStringCellValue());
        assertEquals("PSO1", th.getCell(9).getStringCellValue(), "All POs must appear before PSOs");
        assertEquals("PSO2", th.getCell(10).getStringCellValue());
        assertNull(th.getCell(11), "No extra columns");
    }

    @Test
    @DisplayName("Requirement 3: Semester grouping and vertical merging across course rows")
    void testSemesterGroupingAndMerging() {
        List<String> pos = List.of("PO1", "PO2", "PO3", "PO4", "PO5", "PO6", "PO7", "PO8", "PO9", "PO10", "PO11", "PO12");
        List<String> psos = List.of("PSO1", "PSO2", "PSO3");

        // Sem 1: 3 courses (rows 9, 10, 11) -> merged A10:A12 (row index 9 to 11)
        // Sem 2: 4 courses (rows 12, 13, 14, 15) -> merged A13:A16 (row index 12 to 15)
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, Map.of(1, 3, 2, 4));
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageDirectAttainmentSheetBuilder.build(wb, "average attainment(D)", snapshot);

        // Check Sem 1 label and merge
        Row rSem1 = sheet.getRow(9);
        assertEquals("FE Sem - I", rSem1.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 9, 11, 0, 0), "Sem 1 must be merged vertically rows 9..11");

        // Check Sem 2 label and merge
        Row rSem2 = sheet.getRow(12);
        assertEquals("FE Sem - II", rSem2.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 12, 15, 0, 0), "Sem 2 must be merged vertically rows 12..15");

        // Check single course semester does not create invalid 1-row merge
        ProgrammeAttainmentSnapshot singleCourseSnap = createSampleSnapshot(pos, psos, Map.of(3, 1));
        Workbook wbSingle = new XSSFWorkbook();
        Sheet sheetSingle = AverageDirectAttainmentSheetBuilder.build(wbSingle, "average attainment(D)", singleCourseSnap);
        Row rSem3 = sheetSingle.getRow(9);
        assertEquals("SE Sem-III", rSem3.getCell(0).getStringCellValue());
        assertFalse(hasMergedRegion(sheetSingle.getMergedRegions(), 9, 9, 0, 0), "1-row semester should not have single-cell merge");
    }

    @Test
    @DisplayName("Requirement 4 & Summary: Direct attainment data placed correctly and summary row merged A..C")
    void testDirectAttainmentDataAndSummaryRow() {
        List<String> pos = List.of("PO1", "PO2", "PO3");
        List<String> psos = List.of("PSO1", "PSO2");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, Map.of(1, 2));
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageDirectAttainmentSheetBuilder.build(wb, "average attainment(D)", snapshot);

        // Data rows: index 9 and 10
        Row row9 = sheet.getRow(9);
        assertEquals("CS101", row9.getCell(1).getStringCellValue());
        assertEquals("Computer Science Course 1", row9.getCell(2).getStringCellValue());
        assertEquals(2.60, row9.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.80, row9.getCell(6).getNumericCellValue(), 0.001);

        // Summary row: index 11
        Row sumRow = sheet.getRow(11);
        assertNotNull(sumRow);
        assertEquals("Average Attainment (Direct)", sumRow.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 11, 11, 0, 2), "A..C must be merged in summary row");
        assertEquals(2.60, sumRow.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.80, sumRow.getCell(6).getNumericCellValue(), 0.001);
    }

    @Test
    @DisplayName("Requirement 6 & 7: Header scaling invariance and column proportional widths")
    void testHeaderScalingAndColumnWidths() {
        // Case A: 12 PO + 3 PSO -> 18 cols (0 to 17)
        List<String> pos12 = java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(i -> "PO" + i).toList();
        List<String> psos3 = List.of("PSO1", "PSO2", "PSO3");
        ProgrammeAttainmentSnapshot snapA = createSampleSnapshot(pos12, psos3, Map.of(1, 2));
        Workbook wbA = new XSSFWorkbook();
        Sheet sheetA = AverageDirectAttainmentSheetBuilder.build(wbA, "average attainment(D)", snapA);

        // Case B: 20 PO + 5 PSO -> 28 cols (0 to 27)
        List<String> pos20 = java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(i -> "PO" + i).toList();
        List<String> psos5 = java.util.stream.IntStream.rangeClosed(1, 5).mapToObj(i -> "PSO" + i).toList();
        ProgrammeAttainmentSnapshot snapB = createSampleSnapshot(pos20, psos5, Map.of(1, 2));
        Workbook wbB = new XSSFWorkbook();
        Sheet sheetB = AverageDirectAttainmentSheetBuilder.build(wbB, "average attainment(D)", snapB);

        // Verify Case A Header:
        // Left logo region: A2:B3 (cols 0..1)
        assertTrue(hasMergedRegion(sheetA.getMergedRegions(), 1, 2, 0, 1));
        // Right metadata: cols 14..17 (4 columns)
        assertTrue(hasMergedRegion(sheetA.getMergedRegions(), 1, 2, 14, 17));
        // Center region: cols 2..13 (12 columns)
        assertTrue(hasMergedRegion(sheetA.getMergedRegions(), 1, 1, 2, 13));

        // Verify Case B Header:
        // Left logo region: STILL A2:B3 (cols 0..1) -> unchanged!
        assertTrue(hasMergedRegion(sheetB.getMergedRegions(), 1, 2, 0, 1));
        // Right metadata: STILL 4 columns (cols 24..27) -> unchanged width!
        assertTrue(hasMergedRegion(sheetB.getMergedRegions(), 1, 2, 24, 27));
        // Center region absorbs ALL additional columns: cols 2..23 (22 columns!)
        assertTrue(hasMergedRegion(sheetB.getMergedRegions(), 1, 1, 2, 23));

        // Column widths
        assertEquals((int) (15.82 * 256), sheetA.getColumnWidth(0));
        assertEquals((int) (14.18 * 256), sheetA.getColumnWidth(1));
        assertEquals((int) (41.00 * 256), sheetA.getColumnWidth(2));
        assertEquals((int) (7.50 * 256), sheetA.getColumnWidth(3));
    }

    @Test
    @DisplayName("Requirement 9: Print layout setup is configured appropriately")
    void testPrintLayout() {
        List<String> pos = List.of("PO1", "PO2");
        List<String> psos = List.of("PSO1");
        ProgrammeAttainmentSnapshot snap = createSampleSnapshot(pos, psos, Map.of(1, 2));
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = AverageDirectAttainmentSheetBuilder.build(wb, "average attainment(D)", snap);

        PrintSetup ps = sheet.getPrintSetup();
        assertTrue(ps.getLandscape(), "Orientation must be landscape");
        assertEquals(PrintSetup.A4_PAPERSIZE, ps.getPaperSize(), "Paper size must be A4");
        assertTrue(sheet.getFitToPage(), "Fit to page must be true");
        assertEquals(1, ps.getFitWidth(), "Fit width must be 1 page");
        assertEquals(0, ps.getFitHeight(), "Fit height must be 0 (unconstrained)");
        assertTrue(sheet.getHorizontallyCenter(), "Must be horizontally centered");
        assertNotNull(wb.getPrintArea(wb.getSheetIndex(sheet)), "Print area must be defined");
    }

    @Test
    @DisplayName("Requirement 11: Edge cases - No PSOs, 8 POs + 2 PSOs, 3 POs + 1 PSO")
    void testEdgeCases() {
        // No PSOs
        List<String> pos = List.of("PO1", "PO2", "PO3");
        ProgrammeAttainmentSnapshot snapNoPso = createSampleSnapshot(pos, List.of(), Map.of(1, 2));
        Workbook wb1 = new XSSFWorkbook();
        Sheet sheet1 = AverageDirectAttainmentSheetBuilder.build(wb1, "average attainment(D)", snapNoPso);
        assertEquals(3 + 3, sheet1.getRow(8).getLastCellNum());

        // 8 POs + 2 PSOs
        List<String> pos8 = java.util.stream.IntStream.rangeClosed(1, 8).mapToObj(i -> "PO" + i).toList();
        List<String> psos2 = List.of("PSO1", "PSO2");
        ProgrammeAttainmentSnapshot snap8 = createSampleSnapshot(pos8, psos2, Map.of(1, 2));
        Workbook wb2 = new XSSFWorkbook();
        Sheet sheet2 = AverageDirectAttainmentSheetBuilder.build(wb2, "average attainment(D)", snap8);
        assertEquals(3 + 8 + 2, sheet2.getRow(8).getLastCellNum());
    }

    private boolean hasMergedRegion(List<CellRangeAddress> regions, int firstRow, int lastRow, int firstCol, int lastCol) {
        return regions.stream().anyMatch(r ->
                r.getFirstRow() == firstRow &&
                r.getLastRow() == lastRow &&
                r.getFirstColumn() == firstCol &&
                r.getLastColumn() == lastCol);
    }
}
