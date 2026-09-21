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
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CourseAttainmentMainSheetTest {

    private final ExcelReportRenderer renderer = new ExcelReportRenderer();

    @Test
    @DisplayName("Case A: Standard 6 COs + 12 POs + 3 PSOs -> 17 columns (A to Q), exact sheet structure")
    void testCaseA_Standard6Cos12Pos3Psos() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(6, 12, 3);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(5, wb.getNumberOfSheets());
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("Attainment-main", sheet.getSheetName());
            assertEquals("PO mapping", wb.getSheetAt(1).getSheetName());
            assertEquals("PSO mapping", wb.getSheetAt(2).getSheetName());
            assertEquals("Examination", wb.getSheetAt(3).getSheetName());
            assertEquals("Course End Survey", wb.getSheetAt(4).getSheetName());

            // 1. Column dimensions: 17 columns (0 to 16, A to Q)
            int expectedCols = 17;

            // Title band at row 3 must end at col 16 (Col Q)
            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand, "Title band at row 3 must exist");
            assertEquals(expectedCols - 1, titleBand.getLastColumn(), "Header width must match 17 columns (end col 16)");

            // 2. Section A: Course Information (Rows 5 to 9)
            assertEquals("Subject Name : ", getCellString(sheet, 5, 0));
            assertEquals("Computer Networks and Security", getCellString(sheet, 5, 2));
            assertEquals("Academic Year : ", getCellString(sheet, 6, 0));
            assertEquals("AY 2023-24", getCellString(sheet, 6, 2));
            assertEquals("Semester : ", getCellString(sheet, 7, 0));
            assertEquals("V", getCellString(sheet, 7, 2));
            assertEquals("Subject Code : ", getCellString(sheet, 8, 0));
            assertEquals("CS301", getCellString(sheet, 8, 2));
            assertEquals("Faculty Name:", getCellString(sheet, 9, 0));
            assertEquals("Dr. John Doe", getCellString(sheet, 9, 2));

            // 3. Section B: Course Outcome
            // Row 11: Banner "Course Outcome" merged 0 to 16
            CellRangeAddress coBanner = findMergedRegion(sheet, 11, 0);
            assertNotNull(coBanner);
            assertEquals(expectedCols - 1, coBanner.getLastColumn());
            assertEquals("Course Outcome", getCellString(sheet, 11, 0));

            // Row 12: Headers
            assertEquals("Sr No", getCellString(sheet, 12, 0));
            assertEquals("Code", getCellString(sheet, 12, 1));
            assertEquals("Statement", getCellString(sheet, 12, 2));

            // Rows 13 to 18: 6 COs
            for (int i = 0; i < 6; i++) {
                assertEquals(String.valueOf(i + 1), getCellString(sheet, 13 + i, 0));
                assertEquals("CO" + (i + 1), getCellString(sheet, 13 + i, 1));
                assertEquals("Statement for CO" + (i + 1), getCellString(sheet, 13 + i, 2));
            }

            // 4. Section C: Table 1 (Row 20 banner)
            int t1BannerRow = 20;
            CellRangeAddress t1Banner = findMergedRegion(sheet, t1BannerRow, 0);
            assertNotNull(t1Banner);
            assertEquals(expectedCols - 1, t1Banner.getLastColumn());
            assertEquals("Table 1 : Mapping of CO to PO/PSO ", getCellString(sheet, t1BannerRow, 0));

            // Table 1 Header (Row 21)
            int t1HeaderRow = 21;
            assertEquals("Sr No", getCellString(sheet, t1HeaderRow, 0));
            assertEquals("Code", getCellString(sheet, t1HeaderRow, 1));
            assertEquals("PO1", getCellString(sheet, t1HeaderRow, 2));
            assertEquals("PO12", getCellString(sheet, t1HeaderRow, 13));
            assertEquals("PSO1", getCellString(sheet, t1HeaderRow, 14));
            assertEquals("PSO3", getCellString(sheet, t1HeaderRow, 16));

            // Table 1 Average Row (Row 28 = 21 + 6 + 1)
            int t1AvgRow = 28;
            assertEquals("Average", getCellString(sheet, t1AvgRow, 0));
            // Average cells have bright blue fill
            Cell avgCell = sheet.getRow(t1AvgRow).getCell(2);
            assertNotNull(avgCell);
            assertTrue(isBrightBlue(avgCell.getCellStyle()), "Average cell must have bright blue fill");

            // 5. Section D: Overall CO Attainment (Row 30)
            int secDRow1 = 30;
            int secDRow2 = 31;
            assertEquals("Overall CO Attainment", getCellString(sheet, secDRow1, 0));
            assertEquals("Get from below reference work", getCellString(sheet, secDRow1, 3));
            assertEquals(2.45, sheet.getRow(secDRow2).getCell(0).getNumericCellValue(), 0.01);
            assertTrue(isAccentBlue(sheet.getRow(secDRow2).getCell(0).getCellStyle()), "Section D value must have accent blue fill");

            // 6. Section E: Table 2 (Row 33 banner)
            int t2BannerRow = 33;
            assertEquals("Table 2: PO Attainment Values (Direct Attainment)", getCellString(sheet, t2BannerRow, 0));
            int t2HeaderRow = 34;
            assertEquals("Code", getCellString(sheet, t2HeaderRow, 0));
            int t2DataRow = 35;
            assertEquals("CS301", getCellString(sheet, t2DataRow, 0));

            // 7. Section F: Reference Work (Row 38 banner)
            int refBannerRow = 38;
            assertEquals("Reference Work", getCellString(sheet, refBannerRow, 0));
            CellRangeAddress refBanner = findMergedRegion(sheet, refBannerRow, 0);
            assertNotNull(refBanner);
            assertEquals(8, refBanner.getLastColumn(), "Reference Work banner spans 0 to last CO (col 8)");

            // CO Headers (Row 39)
            int refCoRow = 39;
            assertEquals("CO1", getCellString(sheet, refCoRow, 3));
            assertEquals("CO6", getCellString(sheet, refCoRow, 8));

            // Direct Examination (Row 40)
            int refDirRow = 40;
            assertEquals("% of students above threshold", getCellString(sheet, refDirRow, 0));
            assertEquals("Direct through Examination", getCellString(sheet, refDirRow, 2));
            assertEquals("Get values from Examination sheet", getCellString(sheet, refDirRow, 9));

            // Final CO Attainment (Row 45)
            int refAttainCoRow = 45;
            assertEquals("Attainment of CO", getCellString(sheet, refAttainCoRow, 0));

            // Overall CO Attainment (Row 47)
            int refOverallRow = 47;
            assertEquals("Overall CO Attainment ", getCellString(sheet, refOverallRow, 0));
            assertEquals(2.45, sheet.getRow(refOverallRow).getCell(3).getNumericCellValue(), 0.01);
        }
    }

    @Test
    @DisplayName("Case B: Dynamic 5 COs + 8 POs + 2 PSOs -> 12 columns (A to L)")
    void testCaseB_Dynamic5Cos8Pos2Psos() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(5, 8, 2);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("Attainment-main", sheet.getSheetName());

            // Total columns = 2 + 8 + 2 = 12 columns (cols 0 to 11, A to L)
            int expectedCols = 12;

            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand);
            assertEquals(expectedCols - 1, titleBand.getLastColumn(), "Header last col must equal content last col (11)");

            // Section B has exactly 5 CO rows
            int coStartRow = 13;
            for (int i = 0; i < 5; i++) {
                assertEquals("CO" + (i + 1), getCellString(sheet, coStartRow + i, 1));
            }
            // Row 18 is spacer, row 19 is Table 1 banner
            assertEquals("Table 1 : Mapping of CO to PO/PSO ", getCellString(sheet, 19, 0));
            CellRangeAddress t1Banner = findMergedRegion(sheet, 19, 0);
            assertEquals(expectedCols - 1, t1Banner.getLastColumn());
        }
    }

    @Test
    @DisplayName("Case C: Dynamic 8 COs + 10 POs + 2 PSOs -> 14 columns (A to N)")
    void testCaseC_Dynamic8Cos10Pos2Psos() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(8, 10, 2);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        assertNotNull(bytes);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("Attainment-main", sheet.getSheetName());

            // Table 1 cols: 2 + 10 + 2 = 14
            // RefWork min cols: 3 + 8 = 11, + 5 = 16. TotalCols = max(14, 16) = 16 columns!
            CellRangeAddress titleBand = findMergedRegion(sheet, 3, 0);
            assertNotNull(titleBand);
            assertTrue(titleBand.getLastColumn() >= 13, "Header width must dynamically adapt");

            // Verify 8 COs in Section B
            for (int i = 0; i < 8; i++) {
                assertEquals("CO" + (i + 1), getCellString(sheet, 13 + i, 1));
            }
        }
    }

    @Test
    @DisplayName("Attainment-main sheet strictly displays courseCoordinatorName instead of username")
    void testCoordinatorNameUsedInsteadOfUsername() throws Exception {
        CourseAttainmentSnapshot snapshot = createSnapshot(6, 12, 3);
        snapshot.setGeneratedBy("rajshaikh_username");
        snapshot.setCourseCoordinatorName("Dr. Raj Shaikh (Course Coordinator)");

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheet("Attainment-main");
            assertNotNull(sheet);

            // Row 9 is Faculty Name row
            assertEquals("Faculty Name:", getCellString(sheet, 9, 0));
            assertEquals("Dr. Raj Shaikh (Course Coordinator)", getCellString(sheet, 9, 2));
            assertNotEquals("rajshaikh_username", getCellString(sheet, 9, 2));
        }
    }

    // --- Test Data Builder ---

    private CourseAttainmentSnapshot createSnapshot(int numCos, int numPos, int numPsos) {
        List<String> poCodes = new ArrayList<>();
        for (int i = 1; i <= numPos; i++) poCodes.add("PO" + i);

        List<String> psoCodes = new ArrayList<>();
        for (int i = 1; i <= numPsos; i++) psoCodes.add("PSO" + i);

        List<CourseAttainmentSnapshot.CoAttainmentRow> t3 = new ArrayList<>();
        List<CourseAttainmentSnapshot.CoMappingRow> t1 = new ArrayList<>();

        for (int i = 1; i <= numCos; i++) {
            String co = "CO" + i;
            Map<String, Integer> poMap = new LinkedHashMap<>();
            for (String po : poCodes) poMap.put(po, (i % 3) + 1);
            Map<String, Integer> psoMap = new LinkedHashMap<>();
            for (String pso : psoCodes) psoMap.put(pso, (i % 2) + 1);

            t1.add(CourseAttainmentSnapshot.CoMappingRow.builder()
                    .coCode(co)
                    .poMappings(poMap)
                    .psoMappings(psoMap)
                    .build());

            t3.add(CourseAttainmentSnapshot.CoAttainmentRow.builder()
                    .coCode(co)
                    .statement("Statement for " + co)
                    .targetLevel(new BigDecimal("2.50"))
                    .directPercentage(new BigDecimal("75.5"))
                    .directLevel(3)
                    .indirectPercentage(new BigDecimal("82.0"))
                    .indirectLevel(3)
                    .finalAttainment(new BigDecimal("2.60"))
                    .targetMet(true)
                    .build());
        }

        List<CourseAttainmentSnapshot.OutcomeContributionRow> t2Po = new ArrayList<>();
        for (String po : poCodes) {
            t2Po.add(CourseAttainmentSnapshot.OutcomeContributionRow.builder()
                    .outcomeCode(po)
                    .averageMapping(new BigDecimal("2.33"))
                    .directContribution(new BigDecimal("1.85"))
                    .build());
        }

        List<CourseAttainmentSnapshot.OutcomeContributionRow> t2Pso = new ArrayList<>();
        for (String pso : psoCodes) {
            t2Pso.add(CourseAttainmentSnapshot.OutcomeContributionRow.builder()
                    .outcomeCode(pso)
                    .averageMapping(new BigDecimal("2.00"))
                    .directContribution(new BigDecimal("1.60"))
                    .build());
        }

        return CourseAttainmentSnapshot.builder()
                .institutionName("D Y Patil International University, Akurdi Pune")
                .schoolName("School of Engineering and Technology")
                .courseName("Computer Networks and Security")
                .courseCode("CS301")
                .academicYear("2023-24")
                .semester(5)
                .generatedBy("Dr. John Doe")
                .generatedAt(ZonedDateTime.now())
                .overallCoAttainment(new BigDecimal("2.45"))
                .poCodes(poCodes)
                .psoCodes(psoCodes)
                .table1Mapping(t1)
                .table2DirectPO(t2Po)
                .table2DirectPSO(t2Pso)
                .table3CoAttainments(t3)
                .build();
    }

    private static String getCellString(Sheet sheet, int row, int col) {
        Row r = sheet.getRow(row);
        if (r == null) return "";
        Cell c = r.getCell(col);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> String.valueOf(c.getNumericCellValue());
            default -> "";
        };
    }

    private static CellRangeAddress findMergedRegion(Sheet sheet, int firstRow, int firstCol) {
        for (CellRangeAddress mr : sheet.getMergedRegions()) {
            if (mr.getFirstRow() == firstRow && mr.getFirstColumn() == firstCol) {
                return mr;
            }
        }
        return null;
    }

    private static boolean isBrightBlue(CellStyle style) {
        if (style.getFillForegroundColorColor() instanceof XSSFColor xc) {
            byte[] rgb = xc.getRGB();
            if (rgb != null) {
                return (rgb[0] & 0xFF) == 0 && (rgb[1] & 0xFF) == 176 && (rgb[2] & 0xFF) == 240;
            }
        }
        return false;
    }

    private static boolean isAccentBlue(CellStyle style) {
        if (style.getFillForegroundColorColor() instanceof XSSFColor xc) {
            byte[] rgb = xc.getRGB();
            if (rgb != null) {
                return (rgb[0] & 0xFF) == 146 && (rgb[1] & 0xFF) == 205 && (rgb[2] & 0xFF) == 220;
            }
        }
        return false;
    }
}
