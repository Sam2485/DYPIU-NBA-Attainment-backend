package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CourseEndSurveySheetTest {

    private final ExcelReportRenderer renderer = new ExcelReportRenderer();

    @Test
    @DisplayName("Case A: Standard 6 COs + 24 responses -> 8 columns (A to H), exact sheet structure, merges, colors, widths & values")
    void testCaseA_Standard6Cos24Responses() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(6, 24);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(5, wb.getNumberOfSheets(), "Workbook must contain exactly 5 sheets (Sheet 1..5)");
            assertEquals("Attainment-main", wb.getSheetAt(0).getSheetName());
            assertEquals("PO mapping", wb.getSheetAt(1).getSheetName());
            assertEquals("PSO mapping", wb.getSheetAt(2).getSheetName());
            assertEquals("Examination", wb.getSheetAt(3).getSheetName());

            Sheet sheet = wb.getSheetAt(4);
            assertEquals("Course End Survey", sheet.getSheetName(), "Sheet #5 must be named 'Course End Survey'");

            // 1. Column Dimensions: 8 columns (0 to 7, Col A to Col H)
            int expectedCols = 8;
            int expectedEndCol = expectedCols - 1; // 7 (Col H)

            // Invariant: Header last column == Content last column
            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand, "Title band at row 3 must exist");
            assertEquals(expectedEndCol, titleBand.getLastColumn(), "Header width must match 8 columns (Col H)");

            // 2. Column widths (+10% increased)
            assertWidthClose(11.275, sheet.getColumnWidth(0) / 256.0);
            assertWidthClose(6.875, sheet.getColumnWidth(1) / 256.0);
            for (int c = 2; c <= expectedEndCol; c++) {
                assertWidthClose(14.025, sheet.getColumnWidth(c) / 256.0);
            }

            // 3. Row 5 (Excel Row 6, ht = 18 pt): Summary Table 1 Header
            Row r5 = sheet.getRow(5);
            assertNotNull(r5);
            assertEquals(18.0f, r5.getHeightInPoints(), 0.2f);
            for (int i = 0; i < 6; i++) {
                Cell cell = r5.getCell(2 + i);
                assertNotNull(cell);
                assertEquals("CO" + (i + 1), cell.getStringCellValue());
                assertRgbEquals(CourseEndSurveySheetBuilder.COLOR_PALE_CYAN, getCellFillColor(cell));
            }

            // 4. Rows 6 to 8 (Excel Rows 7 to 9): Count of each level
            CellRangeAddress countMerge = findMergedRegion(sheet, 6, 0);
            assertNotNull(countMerge, "A7:A9 must be merged for 'Count of each level'");
            assertEquals(6, countMerge.getFirstRow());
            assertEquals(8, countMerge.getLastRow());
            assertEquals(0, countMerge.getFirstColumn());
            assertEquals(0, countMerge.getLastColumn());
            assertEquals("Count of each level", getCellString(sheet, 6, 0));

            assertEquals("1", getCellString(sheet, 6, 1));
            assertEquals("2", getCellString(sheet, 7, 1));
            assertEquals("3", getCellString(sheet, 8, 1));

            // Verify count numbers exist in data cells
            for (int r = 6; r <= 8; r++) {
                for (int c = 2; c <= expectedEndCol; c++) {
                    Cell cell = sheet.getRow(r).getCell(c);
                    assertNotNull(cell);
                    assertEquals(CellType.NUMERIC, cell.getCellType());
                }
            }

            // 5. Row 9 (Excel Row 10, ht = 14.25 pt): Spacer
            Row r9 = sheet.getRow(9);
            assertNotNull(r9);
            assertEquals(14.25f, r9.getHeightInPoints(), 0.2f);

            // 6. Rows 10 to 12 (Excel Rows 11 to 13): % of students
            CellRangeAddress pctMerge = findMergedRegion(sheet, 10, 0);
            assertNotNull(pctMerge, "% of students label must be merged A11:A13");
            assertEquals(10, pctMerge.getFirstRow());
            assertEquals(12, pctMerge.getLastRow());
            assertEquals(0, pctMerge.getFirstColumn());
            assertEquals(0, pctMerge.getLastColumn());
            assertEquals("% of students", getCellString(sheet, 10, 0));

            assertEquals("1", getCellString(sheet, 10, 1));
            assertEquals("2", getCellString(sheet, 11, 1));
            assertEquals("3", getCellString(sheet, 12, 1));

            // Verify percentage values
            for (int r = 10; r <= 12; r++) {
                for (int c = 2; c <= expectedEndCol; c++) {
                    Cell cell = sheet.getRow(r).getCell(c);
                    assertNotNull(cell);
                    assertEquals(CellType.NUMERIC, cell.getCellType());
                }
            }

            // 7. Row 13 (Excel Row 14, ht = 14.25 pt): Spacer
            Row r13 = sheet.getRow(13);
            assertNotNull(r13);
            assertEquals(14.25f, r13.getHeightInPoints(), 0.2f);

            // 8. Row 14 (Excel Row 15, ht = 23.5 pt): Overall Indirect %
            Row r14 = sheet.getRow(14);
            assertNotNull(r14);
            assertEquals(23.5f, r14.getHeightInPoints(), 0.2f);

            CellRangeAddress overallMerge = findMergedRegion(sheet, 14, 0);
            assertNotNull(overallMerge, "A15:B15 must be merged for Overall Indirect %");
            assertEquals(14, overallMerge.getFirstRow());
            assertEquals(14, overallMerge.getLastRow());
            assertEquals(0, overallMerge.getFirstColumn());
            assertEquals(1, overallMerge.getLastColumn());
            assertEquals("Overall Indirect %", getCellString(sheet, 14, 0));

            for (int c = 2; c <= expectedEndCol; c++) {
                Cell cell = r14.getCell(c);
                assertNotNull(cell);
                assertEquals(CellType.NUMERIC, cell.getCellType());
                assertRgbEquals(CourseEndSurveySheetBuilder.COLOR_BRIGHT_BLUE, getCellFillColor(cell));
            }

            // 9. Rows 15 & 16 (Excel Rows 16 & 17, ht = 14.25 pt): Spacers
            assertEquals(14.25f, sheet.getRow(15).getHeightInPoints(), 0.2f);
            assertEquals(14.25f, sheet.getRow(16).getHeightInPoints(), 0.2f);

            // 10. Row 17 (Excel Row 18, ht = 21.0 pt): Response table header
            Row r17 = sheet.getRow(17);
            assertNotNull(r17);
            assertEquals(21.0f, r17.getHeightInPoints(), 0.2f);
            assertEquals("Sr No", getCellString(sheet, 17, 0));
            for (int i = 0; i < 6; i++) {
                Cell cell = r17.getCell(2 + i);
                assertNotNull(cell);
                assertEquals("CO" + (i + 1), cell.getStringCellValue());
                assertRgbEquals(CourseEndSurveySheetBuilder.COLOR_BRIGHT_BLUE, getCellFillColor(cell));
            }

            // 11. Row 18 (Excel Row 19, ht = 52.9 pt): No. of Students with double bottom border
            Row r18 = sheet.getRow(18);
            assertNotNull(r18);
            assertEquals(52.9f, r18.getHeightInPoints(), 0.2f);
            assertEquals("No. of Students", getCellString(sheet, 18, 0));
            assertEquals(BorderStyle.DOUBLE, r18.getCell(0).getCellStyle().getBorderBottom());
            assertRgbEquals(CourseEndSurveySheetBuilder.COLOR_SOFT_BLUE, getCellFillColor(r18.getCell(0)));

            assertEquals("24", getCellString(sheet, 18, 1));
            assertEquals(BorderStyle.DOUBLE, r18.getCell(1).getCellStyle().getBorderBottom());

            for (int c = 2; c <= expectedEndCol; c++) {
                assertEquals(BorderStyle.DOUBLE, r18.getCell(c).getCellStyle().getBorderBottom());
            }

            // 12. Rows 19 to 42 (Excel Rows 20 to 43, 24 students): Response rows
            for (int sIdx = 0; sIdx < 24; sIdx++) {
                int rowNum = 19 + sIdx;
                Row rResp = sheet.getRow(rowNum);
                assertNotNull(rResp, "Response row " + rowNum + " must exist");
                assertEquals(14.25f, rResp.getHeightInPoints(), 0.2f);
                assertEquals(String.valueOf(sIdx + 1), getCellString(sheet, rowNum, 0));

                for (int c = 2; c <= expectedEndCol; c++) {
                    Cell cell = rResp.getCell(c);
                    assertNotNull(cell);
                    String val = cell.getStringCellValue();
                    assertTrue(val.equals("Slight") || val.equals("Moderate") || val.equals("Substantial"),
                            "Feedback must be Slight, Moderate, or Substantial but got: " + val);
                    assertRgbEquals(CourseEndSurveySheetBuilder.COLOR_GRAY_FILL, getCellFillColor(cell));
                }
            }
        }
    }

    @Test
    @DisplayName("Case B: 5 COs + 18 responses -> 7 columns (A to G), dynamic width matching, exact header alignment")
    void testCaseB_5Cos18Responses() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(5, 18);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(5, wb.getNumberOfSheets());
            Sheet sheet = wb.getSheetAt(4);
            assertEquals("Course End Survey", sheet.getSheetName());

            // 7 columns total (0 to 6, Col A to Col G)
            int expectedCols = 7;
            int expectedEndCol = 6; // Col G

            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand);
            assertEquals(expectedEndCol, titleBand.getLastColumn(), "Header width must match 7 columns (Col G)");

            Row r5 = sheet.getRow(5);
            for (int i = 0; i < 5; i++) {
                assertEquals("CO" + (i + 1), r5.getCell(2 + i).getStringCellValue());
            }
            assertNull(r5.getCell(7), "Column H should not have CO data in 5 COs configuration");

            // Row 18: No. of Students = 18
            assertEquals("18", getCellString(sheet, 18, 1));

            // 18 response rows (Rows 19 to 36)
            assertNotNull(sheet.getRow(36));
            assertNull(sheet.getRow(37), "Row 37 should be null after 18 response rows");
        }
    }

    @Test
    @DisplayName("Case C: 8 COs + 30 responses -> 10 columns (A to J), dynamic width matching")
    void testCaseC_8Cos30Responses() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(8, 30);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(4);
            assertEquals("Course End Survey", sheet.getSheetName());

            // 10 columns total (0 to 9, Col A to Col J)
            int expectedCols = 10;
            int expectedEndCol = 9; // Col J

            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand);
            assertEquals(expectedEndCol, titleBand.getLastColumn(), "Header width must match 10 columns (Col J)");

            Row r5 = sheet.getRow(5);
            for (int i = 0; i < 8; i++) {
                assertEquals("CO" + (i + 1), r5.getCell(2 + i).getStringCellValue());
            }

            assertEquals("30", getCellString(sheet, 18, 1));
            assertNotNull(sheet.getRow(19 + 29)); // 30th student row
            assertNull(sheet.getRow(19 + 30));
        }
    }

    @Test
    @DisplayName("Case D: Defensive fallback -> surveyData is null, renders safely without NPE")
    void testCaseD_DefensiveNullSurveyData() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(6, 24);
        snapshot.setSurveyData(null); // Null survey data

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(5, wb.getNumberOfSheets());
            Sheet sheet = wb.getSheetAt(4);
            assertEquals("Course End Survey", sheet.getSheetName());

            // Still renders 8 columns using fallback COs
            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand);
            assertEquals(7, titleBand.getLastColumn());

            assertEquals("0", getCellString(sheet, 18, 1));
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private CourseAttainmentSnapshot createSnapshot(int numCO, int studentCount) {
        List<String> coCodes = new ArrayList<>();
        for (int i = 1; i <= numCO; i++) {
            coCodes.add("CO" + i);
        }

        Map<String, Integer> level1Counts = new LinkedHashMap<>();
        Map<String, Integer> level2Counts = new LinkedHashMap<>();
        Map<String, Integer> level3Counts = new LinkedHashMap<>();
        Map<String, BigDecimal> level1Percentages = new LinkedHashMap<>();
        Map<String, BigDecimal> level2Percentages = new LinkedHashMap<>();
        Map<String, BigDecimal> level3Percentages = new LinkedHashMap<>();
        Map<String, BigDecimal> overallIndirectPercentages = new LinkedHashMap<>();
        List<CourseAttainmentSnapshot.SurveyResponseRow> responses = new ArrayList<>();

        String[] ratingOptions = new String[]{"Slight", "Moderate", "Substantial"};

        for (int s = 1; s <= studentCount; s++) {
            Map<String, String> feedbacks = new LinkedHashMap<>();
            for (int i = 0; i < numCO; i++) {
                String co = coCodes.get(i);
                feedbacks.put(co, ratingOptions[(s + i) % 3]);
            }
            responses.add(CourseAttainmentSnapshot.SurveyResponseRow.builder()
                    .srNo(s)
                    .coFeedbacks(feedbacks)
                    .build());
        }

        for (String co : coCodes) {
            int c1 = 0, c2 = 0, c3 = 0;
            for (CourseAttainmentSnapshot.SurveyResponseRow r : responses) {
                String fb = r.getCoFeedbacks().get(co);
                if ("Slight".equals(fb)) c1++;
                else if ("Moderate".equals(fb)) c2++;
                else if ("Substantial".equals(fb)) c3++;
            }
            level1Counts.put(co, c1);
            level2Counts.put(co, c2);
            level3Counts.put(co, c3);

            double p1 = (double) c1 * 100.0 / studentCount;
            double p2 = (double) c2 * 100.0 / studentCount;
            double p3 = (double) c3 * 100.0 / studentCount;
            level1Percentages.put(co, BigDecimal.valueOf(p1).setScale(2, java.math.RoundingMode.HALF_UP));
            level2Percentages.put(co, BigDecimal.valueOf(p2).setScale(2, java.math.RoundingMode.HALF_UP));
            level3Percentages.put(co, BigDecimal.valueOf(p3).setScale(2, java.math.RoundingMode.HALF_UP));

            double ind = (p1 * 0.33) + (p2 * 0.67) + (p3 * 1.0);
            overallIndirectPercentages.put(co, BigDecimal.valueOf(ind).setScale(2, java.math.RoundingMode.HALF_UP));
        }

        CourseAttainmentSnapshot.SurveySection surveySection = CourseAttainmentSnapshot.SurveySection.builder()
                .totalStudents(studentCount)
                .coCodes(coCodes)
                .level1Counts(level1Counts)
                .level2Counts(level2Counts)
                .level3Counts(level3Counts)
                .level1Percentages(level1Percentages)
                .level2Percentages(level2Percentages)
                .level3Percentages(level3Percentages)
                .overallIndirectPercentages(overallIndirectPercentages)
                .responses(responses)
                .build();

        // Also mock table3 for consistency
        List<CourseAttainmentSnapshot.CoAttainmentRow> table3 = new ArrayList<>();
        for (String co : coCodes) {
            table3.add(CourseAttainmentSnapshot.CoAttainmentRow.builder()
                    .coCode(co)
                    .statement("Statement for " + co)
                    .indirectPercentage(overallIndirectPercentages.get(co))
                    .build());
        }

        return CourseAttainmentSnapshot.builder()
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Computer Science and Engineering")
                .academicYear("2025-29")
                .programmeBatchCourseId("pbc-123")
                .courseCode("CSE301")
                .courseName("Computer Networks and Security")
                .semester(5)
                .generatedBy("Course Coordinator")
                .generatedAt(ZonedDateTime.now())
                .table3CoAttainments(table3)
                .surveyData(surveySection)
                .build();
    }

    private static CellRangeAddress findMergedRegion(Sheet sheet, int row, int col) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress r = sheet.getMergedRegion(i);
            if (r.isInRange(row, col)) {
                return r;
            }
        }
        return null;
    }

    private static String getCellString(Sheet sheet, int row, int col) {
        Row r = sheet.getRow(row);
        if (r == null) return null;
        Cell c = r.getCell(col);
        if (c == null) return null;
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue();
        if (c.getCellType() == CellType.NUMERIC) {
            double v = c.getNumericCellValue();
            if (v == Math.floor(v) && !Double.isInfinite(v)) {
                return String.valueOf((long) v);
            }
            return String.valueOf(v);
        }
        return null;
    }

    private static Color getCellFillColor(Cell cell) {
        CellStyle cs = cell.getCellStyle();
        if (cs instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle xcs) {
            XSSFColor xc = xcs.getFillForegroundColorColor();
            if (xc != null) {
                byte[] rgb = xc.getRGB();
                if (rgb != null && rgb.length >= 3) {
                    return new Color(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                }
            }
        }
        return null;
    }

    private static void assertRgbEquals(Color expected, Color actual) {
        assertNotNull(actual, "Actual color should not be null");
        assertEquals(expected.getRed(), actual.getRed(), "Red channel mismatch");
        assertEquals(expected.getGreen(), actual.getGreen(), "Green channel mismatch");
        assertEquals(expected.getBlue(), actual.getBlue(), "Blue channel mismatch");
    }

    private static void assertWidthClose(double expected, double actual) {
        assertEquals(expected, actual, 0.5, "Width mismatch: expected ~" + expected + " but got " + actual);
    }
}
