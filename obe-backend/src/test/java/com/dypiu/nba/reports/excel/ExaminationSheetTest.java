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

class ExaminationSheetTest {

    private final ExcelReportRenderer renderer = new ExcelReportRenderer();

    @Test
    @DisplayName("Case A: Standard 6 COs + 24 students -> 11 columns (A to K), exact sheet structure, merges, colors, widths & values")
    void testCaseA_Standard6Cos24Students() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(6, 24, new BigDecimal("60.00"));

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(5, wb.getNumberOfSheets(), "Workbook must contain exactly 5 sheets (Sheet 1..5)");
            assertEquals("Attainment-main", wb.getSheetAt(0).getSheetName());
            assertEquals("PO mapping", wb.getSheetAt(1).getSheetName());
            assertEquals("PSO mapping", wb.getSheetAt(2).getSheetName());
            assertEquals("Examination", wb.getSheetAt(3).getSheetName());
            assertEquals("Course End Survey", wb.getSheetAt(4).getSheetName());

            Sheet sheet = wb.getSheetAt(3);
            assertEquals("Examination", sheet.getSheetName(), "Sheet #4 must be named 'Examination'");

            // 1. Column Dimensions: 11 columns (0 to 10, A to K)
            int expectedCols = 11;
            int expectedEndCol = expectedCols - 1; // 10 (Col K)

            // Invariant: Header last column == Content last column
            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand, "Title band at row 3 must exist");
            assertEquals(expectedEndCol, titleBand.getLastColumn(), "Header width must match 11 columns (Col K)");

            // 2. Column widths (+10% increased)
            assertWidthClose(8.525, sheet.getColumnWidth(0) / 256.0);
            assertWidthClose(7.975, sheet.getColumnWidth(1) / 256.0);
            assertWidthClose(37.125, sheet.getColumnWidth(2) / 256.0);
            assertWidthClose(1.65, sheet.getColumnWidth(3) / 256.0);
            assertWidthClose(1.19, sheet.getColumnWidth(4) / 256.0);
            for (int c = 5; c <= 10; c++) {
                assertWidthClose(8.34, sheet.getColumnWidth(c) / 256.0);
            }

            // 3. Row 4 (Excel Row 5, ht = 19.15 pt): Subject Name
            Row r4 = sheet.getRow(4);
            assertNotNull(r4);
            assertEquals(19.15f, r4.getHeightInPoints(), 0.2f);
            assertEquals("Subject Name", getCellString(sheet, 4, 0));
            CellRangeAddress subjMerge = findMergedRegion(sheet, 4, 0);
            assertNotNull(subjMerge);
            assertEquals(0, subjMerge.getFirstColumn());
            assertEquals(2, subjMerge.getLastColumn());

            assertEquals("Computer Networks and Security", getCellString(sheet, 4, 5));
            CellRangeAddress subjValMerge = findMergedRegion(sheet, 4, 5);
            assertNotNull(subjValMerge);
            assertEquals(5, subjValMerge.getFirstColumn());
            assertEquals(10, subjValMerge.getLastColumn());

            // 4. Row 6 (Excel Row 7, ht = 14.25 pt): Class & Academic Year
            Row r6 = sheet.getRow(6);
            assertNotNull(r6);
            assertEquals(14.25f, r6.getHeightInPoints(), 0.2f);
            assertEquals("Class", getCellString(sheet, 6, 0));
            assertEquals("TY B.Tech", getCellString(sheet, 6, 2));
            assertEquals("Academic Year", getCellString(sheet, 6, 5));
            assertEquals("2025-29", getCellString(sheet, 6, 8));

            // 5. Row 8 (Excel Row 9, ht = 14.25 pt): Total Number of Students
            Row r8 = sheet.getRow(8);
            assertNotNull(r8);
            assertEquals("Total Number of Students", getCellString(sheet, 8, 0));
            assertEquals("24", getCellString(sheet, 8, 5));
            CellRangeAddress totalStudMerge = findMergedRegion(sheet, 8, 5);
            assertNotNull(totalStudMerge);
            assertEquals(5, totalStudMerge.getFirstColumn());
            assertEquals(10, totalStudMerge.getLastColumn());

            // 6. Row 10 (Excel Row 11, ht = 17.5 pt): Threshold
            Row r10 = sheet.getRow(10);
            assertNotNull(r10);
            assertEquals(17.5f, r10.getHeightInPoints(), 0.2f);
            assertEquals("Threshhold for attainment level", getCellString(sheet, 10, 0));
            CellRangeAddress threshMerge = findMergedRegion(sheet, 10, 0);
            assertNotNull(threshMerge);
            assertEquals(0, threshMerge.getFirstColumn());
            assertEquals(8, threshMerge.getLastColumn()); // 0 to endCol - 2

            // Col 10 (Col K): threshold value 60 with bright blue fill
            Cell cThresh = r10.getCell(10);
            assertNotNull(cThresh);
            assertEquals("60", getCellString(sheet, 10, 10));
            assertTrue(matchesColor(cThresh.getCellStyle(), ExaminationSheetBuilder.COLOR_BRIGHT_BLUE));

            // 7. Row 12 & 13 (Excel Row 13 & 14): Students above threshold
            Row r12 = sheet.getRow(12);
            assertNotNull(r12);
            assertEquals("Reference : Number of students above threshold", getCellString(sheet, 12, 2));
            Row r13 = sheet.getRow(13);
            assertNotNull(r13);
            assertEquals("# of student >= of out of marks", getCellString(sheet, 13, 2));
            for (int i = 0; i < 6; i++) {
                Cell countCell = r13.getCell(5 + i);
                assertNotNull(countCell);
                assertTrue(matchesColor(countCell.getCellStyle(), ExaminationSheetBuilder.COLOR_CYAN_TINT_LIGHT));
            }

            // 8. Row 15 & 16 (Excel Row 16 & 17): % of students above threshold
            Row r15 = sheet.getRow(15);
            assertNotNull(r15);
            assertEquals("Reference : % of Number of students above threshold", getCellString(sheet, 15, 2));
            Row r16 = sheet.getRow(16);
            assertNotNull(r16);
            assertEquals("% of students above threshhold", getCellString(sheet, 16, 2));
            for (int i = 0; i < 6; i++) {
                Cell pctCell = r16.getCell(5 + i);
                assertNotNull(pctCell);
                assertTrue(matchesColor(pctCell.getCellStyle(), ExaminationSheetBuilder.COLOR_CYAN_TINT_DEEP));
            }

            // 9. Row 18 (Excel Row 19): Out of
            Row r18 = sheet.getRow(18);
            assertNotNull(r18);
            assertEquals("Out of", getCellString(sheet, 18, 0));
            for (int i = 0; i < 6; i++) {
                assertEquals("15", getCellString(sheet, 18, 5 + i));
            }

            // 10. Row 19 (Excel Row 20): Student Table Header
            Row r19 = sheet.getRow(19);
            assertNotNull(r19);
            assertEquals("Sr No", getCellString(sheet, 19, 0));
            assertEquals("PRN No", getCellString(sheet, 19, 1));
            assertEquals("Name", getCellString(sheet, 19, 2));
            assertEquals("CO Assessment", getCellString(sheet, 19, 5));
            CellRangeAddress assessMerge = findMergedRegion(sheet, 19, 5);
            assertNotNull(assessMerge);
            assertEquals(5, assessMerge.getFirstColumn());
            assertEquals(10, assessMerge.getLastColumn());

            // 11. Row 20 (Excel Row 21): Fraction of out of marks
            Row r20 = sheet.getRow(20);
            assertNotNull(r20);
            assertEquals("Fraction of Out of marks with respect to Threshold", getCellString(sheet, 20, 0));
            for (int i = 0; i < 6; i++) {
                assertEquals("9", getCellString(sheet, 20, 5 + i));
            }

            // 12. Row 21 (Excel Row 22, ht = 23.25 pt): CO Headers
            Row r21 = sheet.getRow(21);
            assertNotNull(r21);
            assertEquals(23.25f, r21.getHeightInPoints(), 0.2f);
            for (int i = 0; i < 6; i++) {
                Cell coCell = r21.getCell(5 + i);
                assertNotNull(coCell);
                assertEquals("CO" + (i + 1), getCellString(sheet, 21, 5 + i));
                assertTrue(matchesColor(coCell.getCellStyle(), ExaminationSheetBuilder.COLOR_BRIGHT_BLUE));
            }

            // 13. Rows 22 to 45 (Excel Row 23 to 46): 24 Student rows
            for (int s = 0; s < 24; s++) {
                int rIdx = 22 + s;
                Row rStudent = sheet.getRow(rIdx);
                assertNotNull(rStudent, "Student row " + (s + 1) + " must exist");
                assertEquals(String.valueOf(s + 1), getCellString(sheet, rIdx, 0));
                assertEquals("300" + (s + 1), getCellString(sheet, rIdx, 1));
                assertEquals("STUDENT " + (s + 1), getCellString(sheet, rIdx, 2));
                for (int c = 0; c < 6; c++) {
                    assertFalse(getCellString(sheet, rIdx, 5 + c).isEmpty());
                }
            }
        }
    }

    @Test
    @DisplayName("Case B: Dynamic 5 COs + 15 students -> 10 columns (A to J), strict header parity Col J")
    void testCaseB_Dynamic5Cos15Students() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(5, 15, new BigDecimal("50.00"));

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Examination");
            assertNotNull(sheet);

            // 5 prefix cols + 5 CO cols = 10 cols (0 to 9, A to J)
            int expectedEndCol = 9;

            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand);
            assertEquals(expectedEndCol, titleBand.getLastColumn(), "Header last col must match Col J (index 9)");

            // Threshold label at Row 10: 0 to endCol - 2 (0 to 7)
            CellRangeAddress threshMerge = findMergedRegion(sheet, 10, 0);
            assertNotNull(threshMerge);
            assertEquals(7, threshMerge.getLastColumn());

            // Threshold value at Col 9
            assertEquals("50", getCellString(sheet, 10, 9));

            // CO Assessment header at Row 19: 5 to 9
            CellRangeAddress assessMerge = findMergedRegion(sheet, 19, 5);
            assertNotNull(assessMerge);
            assertEquals(5, assessMerge.getFirstColumn());
            assertEquals(9, assessMerge.getLastColumn());

            // CO Headers at Row 21: CO1 to CO5
            for (int i = 0; i < 5; i++) {
                assertEquals("CO" + (i + 1), getCellString(sheet, 21, 5 + i));
            }

            // 15 student rows
            assertNotNull(sheet.getRow(22 + 14)); // 15th student
            assertNull(sheet.getRow(22 + 15));    // no 16th student
        }
    }

    @Test
    @DisplayName("Case C: Dynamic 8 COs + 30 students -> 13 columns (A to M), strict header parity Col M")
    void testCaseC_Dynamic8Cos30Students() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(8, 30, new BigDecimal("70.00"));

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Examination");
            assertNotNull(sheet);

            // 5 prefix cols + 8 CO cols = 13 cols (0 to 12, A to M)
            int expectedEndCol = 12;

            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand);
            assertEquals(expectedEndCol, titleBand.getLastColumn(), "Header last col must match Col M (index 12)");

            // Threshold label at Row 10: 0 to 10
            CellRangeAddress threshMerge = findMergedRegion(sheet, 10, 0);
            assertNotNull(threshMerge);
            assertEquals(10, threshMerge.getLastColumn());

            // Threshold value at Col 12
            assertEquals("70", getCellString(sheet, 10, 12));

            // CO Headers at Row 21: CO1 to CO8
            for (int i = 0; i < 8; i++) {
                assertEquals("CO" + (i + 1), getCellString(sheet, 21, 5 + i));
            }

            // 30 student rows
            assertNotNull(sheet.getRow(22 + 29)); // 30th student
        }
    }

    @Test
    @DisplayName("Case D: Defensive fallback when examinationData is null -> renders cleanly without error")
    void testCaseD_DefensiveFallback() throws Exception {
        CourseAttainmentSnapshot snapshot = CourseAttainmentSnapshot.builder()
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .courseName("Operating Systems")
                .academicYear("2025-29")
                .semester(5)
                .examinationData(null) // null examinationData
                .build();

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Examination");
            assertNotNull(sheet);
            assertEquals("Subject Name", getCellString(sheet, 4, 0));
            assertEquals("Operating Systems", getCellString(sheet, 4, 5));
        }
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private CourseAttainmentSnapshot createSnapshot(int numCO, int numStudents, BigDecimal threshold) {
        List<String> coCodes = new ArrayList<>();
        Map<String, BigDecimal> coMaxMarks = new LinkedHashMap<>();
        Map<String, BigDecimal> coThresholdMarks = new LinkedHashMap<>();
        Map<String, Integer> aboveCount = new LinkedHashMap<>();
        Map<String, BigDecimal> abovePct = new LinkedHashMap<>();

        for (int i = 1; i <= numCO; i++) {
            String co = "CO" + i;
            coCodes.add(co);
            BigDecimal max = new BigDecimal("15.00");
            coMaxMarks.put(co, max);
            BigDecimal threshMark = max.multiply(threshold).divide(new BigDecimal("100.00"), 2, BigDecimal.ROUND_HALF_UP);
            coThresholdMarks.put(co, threshMark);
            int count = (int) Math.round(numStudents * 0.6);
            aboveCount.put(co, count);
            abovePct.put(co, new BigDecimal("60.00"));
        }

        List<CourseAttainmentSnapshot.StudentMarksRow> students = new ArrayList<>();
        for (int s = 1; s <= numStudents; s++) {
            Map<String, BigDecimal> marks = new LinkedHashMap<>();
            for (String co : coCodes) {
                marks.put(co, new BigDecimal(String.valueOf(8 + (s % 7))));
            }
            students.add(CourseAttainmentSnapshot.StudentMarksRow.builder()
                    .srNo(s)
                    .prn("300" + s)
                    .studentName("STUDENT " + s)
                    .coMarks(marks)
                    .build());
        }

        CourseAttainmentSnapshot.ExaminationSection examSection = CourseAttainmentSnapshot.ExaminationSection.builder()
                .courseName("Computer Networks and Security")
                .className("TY B.Tech")
                .academicYear("2025-29")
                .totalStudents(numStudents)
                .thresholdPercentage(threshold)
                .coCodes(coCodes)
                .coMaxMarks(coMaxMarks)
                .coThresholdMarks(coThresholdMarks)
                .studentsAboveThreshold(aboveCount)
                .percentageAboveThreshold(abovePct)
                .students(students)
                .build();

        return CourseAttainmentSnapshot.builder()
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .courseCode("CS310")
                .courseName("Computer Networks and Security")
                .academicYear("2025-29")
                .semester(5)
                .examinationData(examSection)
                .table3CoAttainments(Collections.emptyList())
                .table1Mapping(Collections.emptyList())
                .poCodes(List.of("PO1", "PO2"))
                .psoCodes(List.of("PSO1", "PSO2"))
                .build();
    }

    private String getCellString(Sheet sheet, int rowIdx, int colIdx) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) return "";
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private CellRangeAddress findMergedRegion(Sheet sheet, int firstRow, int firstCol) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            if (region.getFirstRow() == firstRow && region.getFirstColumn() == firstCol) {
                return region;
            }
        }
        return null;
    }

    private void assertWidthClose(double expected, double actual) {
        assertTrue(Math.abs(expected - actual) <= 1.0,
                "Expected width near " + expected + " but got " + actual);
    }

    private boolean matchesColor(CellStyle style, Color expectedColor) {
        if (style == null || expectedColor == null) return false;
        Color actual = extractFillColor(style);
        if (actual == null) return false;
        int diff = Math.abs(actual.getRed() - expectedColor.getRed())
                + Math.abs(actual.getGreen() - expectedColor.getGreen())
                + Math.abs(actual.getBlue() - expectedColor.getBlue());
        return diff < 30;
    }

    private Color extractFillColor(CellStyle style) {
        if (style instanceof org.apache.poi.xssf.usermodel.XSSFCellStyle xcs) {
            XSSFColor xCol = xcs.getFillForegroundColorColor();
            if (xCol != null) {
                byte[] rgb = xCol.getRGB();
                if (rgb != null && rgb.length >= 3) {
                    return new Color(rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
                }
            }
        }
        return null;
    }
}
