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

class PsoMappingSheetTest {

    private final ExcelReportRenderer renderer = new ExcelReportRenderer();

    @Test
    @DisplayName("Case A: Standard 6 COs + 3 PSOs -> 16 columns (A to P), exact sheet structure, uniform light grey, colors & widths")
    void testCaseA_Standard6Cos3Psos() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(6, 12, 3);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(5, wb.getNumberOfSheets());
            assertEquals("Attainment-main", wb.getSheetAt(0).getSheetName());
            assertEquals("PO mapping", wb.getSheetAt(1).getSheetName());
            assertEquals("PSO mapping", wb.getSheetAt(2).getSheetName());
            assertEquals("Examination", wb.getSheetAt(3).getSheetName());
            assertEquals("Course End Survey", wb.getSheetAt(4).getSheetName());

            Sheet sheet = wb.getSheetAt(2);
            assertEquals("PSO mapping", sheet.getSheetName());

            // 1. Column dimensions: 16 columns (0 to 15, A to P)
            int expectedCols = 16;
            int expectedEndCol = expectedCols - 1; // 15

            // Title band at row 3 must end at col 15 (Col P)
            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand, "Title band at row 3 must exist");
            assertEquals(expectedEndCol, titleBand.getLastColumn(), "Header width must match 16 columns (Col P)");

            // 2. Column widths
            assertWidthClose(35.0, sheet.getColumnWidth(0) / 256.0);
            assertWidthClose(47.0, sheet.getColumnWidth(1) / 256.0);
            assertWidthClose(1.58, sheet.getColumnWidth(2) / 256.0);
            for (int c = 3; c <= 8; c++) {
                assertWidthClose(8.58, sheet.getColumnWidth(c) / 256.0);
            }
            assertWidthClose(1.5, sheet.getColumnWidth(9) / 256.0);
            for (int c = 10; c <= 15; c++) {
                assertWidthClose(7.08, sheet.getColumnWidth(c) / 256.0);
            }

            // 3. Banner Row (Row 5)
            Row r5 = sheet.getRow(5);
            assertNotNull(r5);
            assertEquals(24.65f, r5.getHeightInPoints(), 0.1f);

            // Justification 1 (Cols 0 to 1 merged)
            assertEquals("Justification 1", getCellString(sheet, 5, 0));
            CellRangeAddress justMerge = findMergedRegion(sheet, 5, 0);
            assertNotNull(justMerge);
            assertEquals(0, justMerge.getFirstColumn());
            assertEquals(1, justMerge.getLastColumn());
            assertTrue(matchesColor(r5.getCell(0).getCellStyle(), PsoMappingSheetBuilder.COLOR_BANNER_CYAN));

            // Keywords banner (Cols 3 to 8 merged)
            assertEquals("Keywords mapping to Competency from resepctive CO", getCellString(sheet, 5, 3));
            CellRangeAddress kwMerge = findMergedRegion(sheet, 5, 3);
            assertNotNull(kwMerge);
            assertEquals(3, kwMerge.getFirstColumn());
            assertEquals(8, kwMerge.getLastColumn());
            assertTrue(matchesColor(r5.getCell(3).getCellStyle(), PsoMappingSheetBuilder.COLOR_BRIGHT_BLUE));

            // Y or N banner (Cols 10 to 15 merged)
            assertEquals("Y or N", getCellString(sheet, 5, 10));
            CellRangeAddress ynMerge = findMergedRegion(sheet, 5, 10);
            assertNotNull(ynMerge);
            assertEquals(10, ynMerge.getFirstColumn());
            assertEquals(15, ynMerge.getLastColumn());
            assertTrue(matchesColor(r5.getCell(10).getCellStyle(), PsoMappingSheetBuilder.COLOR_ACCENT_CYAN));

            // 4. Column Headers Row (Row 6)
            Row r6 = sheet.getRow(6);
            assertNotNull(r6);
            assertEquals(15.0f, r6.getHeightInPoints(), 0.1f);
            assertEquals("Programme Specific Outcomes", getCellString(sheet, 6, 0));
            assertEquals("Competency", getCellString(sheet, 6, 1));
            assertTrue(matchesColor(r6.getCell(0).getCellStyle(), PsoMappingSheetBuilder.COLOR_HEADER_BG));
            assertTrue(matchesColor(r6.getCell(1).getCellStyle(), PsoMappingSheetBuilder.COLOR_HEADER_BG));

            for (int i = 0; i < 6; i++) {
                assertEquals("CO" + (i + 1), getCellString(sheet, 6, 3 + i));
                assertEquals("CO" + (i + 1), getCellString(sheet, 6, 10 + i));
                assertTrue(matchesColor(r6.getCell(3 + i).getCellStyle(), PsoMappingSheetBuilder.COLOR_HEADER_BG));
                assertTrue(matchesColor(r6.getCell(10 + i).getCellStyle(), PsoMappingSheetBuilder.COLOR_HEADER_BG));
            }

            // 5. PSO1 Block (Rows 7 to 9 for 3 competencies, Rows 10 to 12 for summary)
            // Competency rows and PSO statement cells must have UNIFORM light-grey fill (#F2F2F2)
            Row r7 = sheet.getRow(7);
            assertNotNull(r7);
            assertTrue(getCellString(sheet, 7, 0).contains("PSO1"));
            assertTrue(matchesColor(r7.getCell(0).getCellStyle(), PsoMappingSheetBuilder.COLOR_LIGHT_GREY),
                    "PSO Statement cell must have uniform light-grey fill");
            assertTrue(matchesColor(r7.getCell(1).getCellStyle(), PsoMappingSheetBuilder.COLOR_LIGHT_GREY),
                    "Competency cell must have uniform light-grey fill");

            // Summary Rows for PSO1 (Rows 10, 11, 12)
            Row rSum1 = sheet.getRow(10);
            assertNotNull(rSum1);
            assertEquals("No of competencies from given PSO1 mapped by COs", getCellString(sheet, 10, 3));
            assertTrue(matchesColor(rSum1.getCell(3).getCellStyle(), PsoMappingSheetBuilder.COLOR_SUM_ROW1));
            // CO1 count should be 3
            assertEquals("3", getCellString(sheet, 10, 10));

            Row rSum2 = sheet.getRow(11);
            assertNotNull(rSum2);
            assertEquals("% of competencies from given PSO1 mapped by COs", getCellString(sheet, 11, 3));
            assertTrue(matchesColor(rSum2.getCell(3).getCellStyle(), PsoMappingSheetBuilder.COLOR_SUM_ROW2));
            // CO1 pct should be 100
            assertEquals("100", getCellString(sheet, 11, 10));

            Row rSum3 = sheet.getRow(12);
            assertNotNull(rSum3);
            assertEquals("Mapping strength of PSO1 of CO", getCellString(sheet, 12, 3));
            assertTrue(matchesColor(rSum3.getCell(3).getCellStyle(), PsoMappingSheetBuilder.COLOR_SUM_ROW3));
            // CO1 strength should be 2 (from table1Mapping for i=1)
            assertEquals("2", getCellString(sheet, 12, 10));
        }
    }

    @Test
    @DisplayName("Case B: Dynamic 5 COs + 2 PSOs -> 14 columns (A to N), exact header parity")
    void testCaseB_Dynamic5Cos2Psos() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(5, 8, 2);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(2);
            assertEquals("PSO mapping", sheet.getSheetName());

            // 5 COs -> totalCols = 4 + 2 * 5 = 14 (Col A to N, endCol = 13)
            int expectedCols = 14;
            int expectedEndCol = 13;

            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand);
            assertEquals(expectedEndCol, titleBand.getLastColumn(), "Header width must match 14 columns (Col N)");

            // Banner Row (Row 5)
            // Keywords banner: cols 3 to 2 + 5 = 7
            CellRangeAddress kwMerge = findMergedRegion(sheet, 5, 3);
            assertNotNull(kwMerge);
            assertEquals(3, kwMerge.getFirstColumn());
            assertEquals(7, kwMerge.getLastColumn());

            // Y or N banner: cols 4 + 5 = 9 to 13
            CellRangeAddress ynMerge = findMergedRegion(sheet, 5, 9);
            assertNotNull(ynMerge);
            assertEquals(9, ynMerge.getFirstColumn());
            assertEquals(13, ynMerge.getLastColumn());

            // Column Headers (Row 6)
            for (int i = 0; i < 5; i++) {
                assertEquals("CO" + (i + 1), getCellString(sheet, 6, 3 + i));
                assertEquals("CO" + (i + 1), getCellString(sheet, 6, 9 + i));
            }
        }
    }

    @Test
    @DisplayName("Case C: Dynamic 8 COs + 4 PSOs -> 20 columns (A to T), exact header parity")
    void testCaseC_Dynamic8Cos4Psos() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(8, 10, 4);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(2);
            assertEquals("PSO mapping", sheet.getSheetName());

            // 8 COs -> totalCols = 4 + 2 * 8 = 20 (Col A to T, endCol = 19)
            int expectedCols = 20;
            int expectedEndCol = 19;

            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand);
            assertEquals(expectedEndCol, titleBand.getLastColumn(), "Header width must match 20 columns (Col T)");

            // Keywords banner: cols 3 to 10
            CellRangeAddress kwMerge = findMergedRegion(sheet, 5, 3);
            assertNotNull(kwMerge);
            assertEquals(3, kwMerge.getFirstColumn());
            assertEquals(10, kwMerge.getLastColumn());

            // Y or N banner: cols 12 to 19
            CellRangeAddress ynMerge = findMergedRegion(sheet, 5, 12);
            assertNotNull(ynMerge);
            assertEquals(12, ynMerge.getFirstColumn());
            assertEquals(19, ynMerge.getLastColumn());
        }
    }

    // Helper methods
    private void assertWidthClose(double expected, double actual) {
        assertEquals(expected, actual, 0.5, "Column width mismatch");
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

    private static String getCellString(Sheet sheet, int r, int c) {
        Row row = sheet.getRow(r);
        if (row == null) return "";
        Cell cell = row.getCell(c);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) {
            double d = cell.getNumericCellValue();
            if (d == (long) d) return String.format("%d", (long) d);
            return String.valueOf(d);
        }
        return "";
    }

    private static boolean matchesColor(CellStyle style, Color expectedColor) {
        if (style == null || style.getFillForegroundColorColor() == null) return false;
        org.apache.poi.ss.usermodel.Color poiColor = style.getFillForegroundColorColor();
        if (poiColor instanceof XSSFColor xssfColor) {
            byte[] rgb = xssfColor.getRGB();
            if (rgb != null && rgb.length == 3) {
                int r = rgb[0] & 0xFF;
                int g = rgb[1] & 0xFF;
                int b = rgb[2] & 0xFF;
                return Math.abs(r - expectedColor.getRed()) <= 2 &&
                        Math.abs(g - expectedColor.getGreen()) <= 2 &&
                        Math.abs(b - expectedColor.getBlue()) <= 2;
            }
        }
        return true;
    }

    private CourseAttainmentSnapshot createSnapshot(int coCount, int poCount, int psoCount) {
        List<String> poCodes = new ArrayList<>();
        for (int i = 1; i <= poCount; i++) poCodes.add("PO" + i);

        List<String> psoCodes = new ArrayList<>();
        for (int i = 1; i <= psoCount; i++) psoCodes.add("PSO" + i);

        List<CourseAttainmentSnapshot.CoMappingRow> t1 = new ArrayList<>();
        for (int i = 1; i <= coCount; i++) {
            String coCode = "CO" + i;
            Map<String, Integer> poMap = new LinkedHashMap<>();
            for (String po : poCodes) poMap.put(po, (i % 3) + 1);
            Map<String, Integer> psoMap = new LinkedHashMap<>();
            for (String pso : psoCodes) psoMap.put(pso, (i % 2) + 1);

            t1.add(CourseAttainmentSnapshot.CoMappingRow.builder()
                    .coCode(coCode)
                    .poMappings(poMap)
                    .psoMappings(psoMap)
                    .build());
        }

        // Create PO details
        List<CourseAttainmentSnapshot.PoDetailRow> poDetails = new ArrayList<>();
        for (int p = 1; p <= poCount; p++) {
            String poCode = "PO" + p;
            List<CourseAttainmentSnapshot.CompetencyDetailRow> comps = new ArrayList<>();
            int numComps = (p == 1) ? 4 : 3;
            for (int c = 1; c <= numComps; c++) {
                Map<String, String> kw = new LinkedHashMap<>();
                Map<String, String> yn = new LinkedHashMap<>();
                for (int co = 1; co <= coCount; co++) {
                    String coCode = "CO" + co;
                    kw.put(coCode, "Keyword " + p + "." + c);
                    yn.put(coCode, "Y");
                }
                comps.add(CourseAttainmentSnapshot.CompetencyDetailRow.builder()
                        .competencyCode(poCode + "." + c)
                        .statement("Competency " + p + "." + c)
                        .coKeywords(kw)
                        .coMappings(yn)
                        .build());
            }
            poDetails.add(CourseAttainmentSnapshot.PoDetailRow.builder()
                    .poCode(poCode)
                    .statement("Statement for " + poCode)
                    .competencies(comps)
                    .build());
        }

        // Create PSO details
        List<CourseAttainmentSnapshot.PsoDetailRow> psoDetails = new ArrayList<>();
        for (int p = 1; p <= psoCount; p++) {
            String psoCode = "PSO" + p;
            List<CourseAttainmentSnapshot.CompetencyDetailRow> comps = new ArrayList<>();
            int numComps = 3;
            for (int c = 1; c <= numComps; c++) {
                Map<String, String> kw = new LinkedHashMap<>();
                Map<String, String> yn = new LinkedHashMap<>();
                for (int co = 1; co <= coCount; co++) {
                    String coCode = "CO" + co;
                    kw.put(coCode, "PSO Keyword " + p + "." + c);
                    yn.put(coCode, "Y");
                }
                comps.add(CourseAttainmentSnapshot.CompetencyDetailRow.builder()
                        .competencyCode(psoCode + "." + c)
                        .statement("PSO Competency " + p + "." + c)
                        .coKeywords(kw)
                        .coMappings(yn)
                        .build());
            }
            psoDetails.add(CourseAttainmentSnapshot.PsoDetailRow.builder()
                    .psoCode(psoCode)
                    .statement("Statement for " + psoCode)
                    .competencies(comps)
                    .build());
        }

        return CourseAttainmentSnapshot.builder()
                .institutionName("D Y Patil International University, Akurdi Pune")
                .schoolName("School of Engineering and Technology")
                .courseName("Computer Networks and Security")
                .courseCode("CS301")
                .semester(5)
                .academicYear("2023-24")
                .generatedBy("Dr. John Doe")
                .generatedAt(ZonedDateTime.now())
                .poCodes(poCodes)
                .psoCodes(psoCodes)
                .poDetails(poDetails)
                .psoDetails(psoDetails)
                .table1Mapping(t1)
                .directAttainment(new BigDecimal("2.40"))
                .indirectAttainment(new BigDecimal("2.50"))
                .overallCoAttainment(new BigDecimal("2.42"))
                .build();
    }
}
