package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AverageMappingSheetTest {

    private ProgrammeAttainmentSnapshot createSampleSnapshot(int poCount, int psoCount, Map<Integer, Integer> coursesPerSem) {
        List<String> poCodes = java.util.stream.IntStream.rangeClosed(1, poCount)
                .mapToObj(i -> "PO" + i).toList();
        List<String> psoCodes = java.util.stream.IntStream.rangeClosed(1, psoCount)
                .mapToObj(i -> "PSO" + i).toList();

        List<ProgrammeAttainmentSnapshot.CourseMappingRow> courseRows = new ArrayList<>();
        int courseSeq = 1;
        for (Map.Entry<Integer, Integer> entry : coursesPerSem.entrySet()) {
            int sem = entry.getKey();
            int count = entry.getValue();
            for (int i = 1; i <= count; i++) {
                Map<String, BigDecimal> poVals = new LinkedHashMap<>();
                for (String po : poCodes) {
                    poVals.put(po, new BigDecimal("2.50"));
                }
                Map<String, BigDecimal> psoVals = new LinkedHashMap<>();
                for (String pso : psoCodes) {
                    psoVals.put(pso, new BigDecimal("2.75"));
                }
                courseRows.add(ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
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

        Map<String, BigDecimal> avgMapping = new LinkedHashMap<>();
        for (String po : poCodes) avgMapping.put(po, new BigDecimal("2.50"));
        for (String pso : psoCodes) avgMapping.put(pso, new BigDecimal("2.75"));

        return ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-map-001")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .departmentName("Department of Computer Science and Engineering")
                .masterProgrammeName("B.Tech Computer Science")
                .masterProgrammeCode("BTECH-CSE")
                .academicYear("2024-2028")
                .poCodes(poCodes)
                .psoCodes(psoCodes)
                .section1AverageMapping(ProgrammeAttainmentSnapshot.AverageMappingSection.builder()
                        .courses(courseRows)
                        .averageMappingStrength(avgMapping)
                        .overallAverageMappingStrength(new BigDecimal("2.55"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("Validation Item 1 & 4: Sheet Name is Average Mapping and Table Structure has correct header and column count")
    void testAverageMappingSheetStructure() {
        // 12 POs + 3 PSOs -> 3 fixed + 12 + 3 = 18 total columns (0 to 17)
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(12, 3, Map.of(1, 3, 2, 4));
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageMappingSheetBuilder.build(wb, null, snapshot);
        assertNotNull(sheet);
        assertEquals("Average Mapping", sheet.getSheetName(), "Sheet name must be exactly 'Average Mapping'");

        // Header start is row index 8 (Excel row 9)
        Row th = sheet.getRow(8);
        assertNotNull(th, "Table header row 8 must exist");
        assertEquals("Sem", th.getCell(0).getStringCellValue());
        assertEquals("Course Code", th.getCell(1).getStringCellValue());
        assertEquals("Course Name", th.getCell(2).getStringCellValue());
        assertEquals("PO1", th.getCell(3).getStringCellValue());
        assertEquals("PO12", th.getCell(14).getStringCellValue());
        assertEquals("PSO1", th.getCell(15).getStringCellValue());
        assertEquals("PSO3", th.getCell(17).getStringCellValue());
        assertNull(th.getCell(18), "No synthetic columns beyond PSO3");

        // Column widths
        assertEquals((int) (15.82 * 256), sheet.getColumnWidth(0));
        assertEquals((int) (14.18 * 256), sheet.getColumnWidth(1));
        assertEquals((int) (41.00 * 256), sheet.getColumnWidth(2));
        assertEquals((int) (7.50 * 256), sheet.getColumnWidth(3));
    }

    @Test
    @DisplayName("Validation Item 7: Semester grouping and vertical merging across course rows")
    void testSemesterGroupingAndMerging() {
        // Sem 1: 3 courses (rows 9, 10, 11) -> merged A10:A12 (row index 9 to 11)
        // Sem 2: 4 courses (rows 12, 13, 14, 15) -> merged A13:A16 (row index 12 to 15)
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(12, 3, Map.of(1, 3, 2, 4));
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageMappingSheetBuilder.build(wb, "Average Mapping", snapshot);

        // Check Sem 1 label and merge
        Row rSem1 = sheet.getRow(9);
        assertEquals("FE Sem - I", rSem1.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 9, 11, 0, 0), "Sem 1 must be merged vertically rows 9..11");

        // Check Sem 2 label and merge
        Row rSem2 = sheet.getRow(12);
        assertEquals("FE Sem - II", rSem2.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 12, 15, 0, 0), "Sem 2 must be merged vertically rows 12..15");

        // Check single course semester does not create invalid 1-row merge
        ProgrammeAttainmentSnapshot singleCourseSnap = createSampleSnapshot(12, 3, Map.of(3, 1));
        Workbook wbSingle = new XSSFWorkbook();
        Sheet sheetSingle = AverageMappingSheetBuilder.build(wbSingle, "Average Mapping", singleCourseSnap);
        Row rSem3 = sheetSingle.getRow(9);
        assertEquals("SE Sem-III", rSem3.getCell(0).getStringCellValue());
        assertFalse(hasMergedRegion(sheetSingle.getMergedRegions(), 9, 9, 0, 0), "1-row semester should not have single-cell merge");
    }

    @Test
    @DisplayName("Validation Item 8 & 9: Course mapping values and summary row with average mapping strength")
    void testCourseMappingDataAndSummaryRow() {
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(12, 3, Map.of(1, 2));
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = AverageMappingSheetBuilder.build(wb, "Average Mapping", snapshot);

        // Data rows: index 9 and 10
        Row row9 = sheet.getRow(9);
        assertEquals("CS101", row9.getCell(1).getStringCellValue());
        assertEquals("Computer Science Course 1", row9.getCell(2).getStringCellValue());
        assertEquals(2.50, row9.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.75, row9.getCell(15).getNumericCellValue(), 0.001);

        // Summary row: index 11
        Row sumRow = sheet.getRow(11);
        assertNotNull(sumRow);
        assertEquals("Average Mapping Strength", sumRow.getCell(0).getStringCellValue());
        assertTrue(hasMergedRegion(sheet.getMergedRegions(), 11, 11, 0, 2), "A..C must be merged in summary row");
        assertEquals(2.50, sumRow.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.75, sumRow.getCell(15).getNumericCellValue(), 0.001);
    }

    @Test
    @DisplayName("Validation Item 32: Header scaling test - middle region absorbs width without logo stretching")
    void testHeaderScalingWithDifferentWidths() {
        // Case A: 12 PO + 3 PSO -> 18 cols (0 to 17)
        ProgrammeAttainmentSnapshot snapA = createSampleSnapshot(12, 3, Map.of(1, 2));
        Workbook wbA = new XSSFWorkbook();
        Sheet sheetA = AverageMappingSheetBuilder.build(wbA, "Average Mapping", snapA);

        // Case B: 20 PO + 5 PSO -> 28 cols (0 to 27)
        ProgrammeAttainmentSnapshot snapB = createSampleSnapshot(20, 5, Map.of(1, 2));
        Workbook wbB = new XSSFWorkbook();
        Sheet sheetB = AverageMappingSheetBuilder.build(wbB, "Average Mapping", snapB);

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

        // Table headers start at col 0 and end exactly at content last column
        assertNotNull(sheetA.getRow(8).getCell(17));
        assertNull(sheetA.getRow(8).getCell(18));

        assertNotNull(sheetB.getRow(8).getCell(27));
        assertNull(sheetB.getRow(8).getCell(28));
    }

    @Test
    @DisplayName("Validation Item 33: Course header scaling test - middle region absorbs width without logo stretching")
    void testCourseHeaderScalingWithDifferentWidths() {
        Workbook wb = new XSSFWorkbook();

        // Narrow: 14 columns (0 to 13)
        Sheet sheetNarrow = wb.createSheet("Narrow");
        CourseExcelHeaderRenderer.renderHeader(wb, sheetNarrow, "Inst", "School", "Title", 14, null, null, false);
        // Left logo: 2 cols (0..1)
        assertTrue(hasMergedRegion(sheetNarrow.getMergedRegions(), 0, 2, 0, 1));
        // Right logo: 2 cols (12..13)
        assertTrue(hasMergedRegion(sheetNarrow.getMergedRegions(), 0, 2, 12, 13));
        // Center: cols 2..11 (10 cols)
        assertTrue(hasMergedRegion(sheetNarrow.getMergedRegions(), 0, 0, 2, 11));

        // Wide: 25 columns (0 to 24)
        Sheet sheetWide = wb.createSheet("Wide");
        CourseExcelHeaderRenderer.renderHeader(wb, sheetWide, "Inst", "School", "Title", 25, null, null, false);
        // Left logo: STILL 2 cols (0..1)
        assertTrue(hasMergedRegion(sheetWide.getMergedRegions(), 0, 2, 0, 1));
        // Right logo: STILL 2 cols (23..24)
        assertTrue(hasMergedRegion(sheetWide.getMergedRegions(), 0, 2, 23, 24));
        // Center absorbs ALL additional columns: cols 2..22 (21 cols!)
        assertTrue(hasMergedRegion(sheetWide.getMergedRegions(), 0, 0, 2, 22));

        // Title band ends exactly at content last column
        CellRangeAddress titleBandNarrow = sheetNarrow.getMergedRegions().stream()
                .filter(r -> r.getFirstRow() == 3).findFirst().orElseThrow();
        assertEquals(13, titleBandNarrow.getLastColumn());

        CellRangeAddress titleBandWide = sheetWide.getMergedRegions().stream()
                .filter(r -> r.getFirstRow() == 3).findFirst().orElseThrow();
        assertEquals(24, titleBandWide.getLastColumn());
    }

    private boolean hasMergedRegion(List<CellRangeAddress> regions, int firstRow, int lastRow, int firstCol, int lastCol) {
        return regions.stream().anyMatch(r ->
                r.getFirstRow() == firstRow &&
                r.getLastRow() == lastRow &&
                r.getFirstColumn() == firstCol &&
                r.getLastColumn() == lastCol);
    }
}
