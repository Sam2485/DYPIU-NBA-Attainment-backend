package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProgrammeExcelHeaderRendererTest {

    private ProgrammeAttainmentSnapshot createSampleSnapshot(int poCount, int psoCount) {
        List<String> poCodes = java.util.stream.IntStream.rangeClosed(1, poCount)
                .mapToObj(i -> "PO" + i).toList();
        List<String> psoCodes = java.util.stream.IntStream.rangeClosed(1, psoCount)
                .mapToObj(i -> "PSO" + i).toList();

        Map<String, BigDecimal> avgMapping = Map.of("PO1", new BigDecimal("2.50"));
        Map<String, BigDecimal> avgDirect = Map.of("PO1", new BigDecimal("2.40"));
        Map<String, BigDecimal> avgIndirect = Map.of("PO1", new BigDecimal("2.60"));
        Map<String, BigDecimal> finalAttainments = Map.of("PO1", new BigDecimal("2.44"));

        return ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-test-001")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .departmentName("Department of Computer Science and Engineering")
                .masterProgrammeName("B.Tech Computer Science")
                .masterProgrammeCode("BTECH-CSE")
                .academicYear("2024-2028")
                .poCodes(poCodes)
                .psoCodes(psoCodes)
                .section1AverageMapping(ProgrammeAttainmentSnapshot.AverageMappingSection.builder()
                        .averageMappingStrength(avgMapping)
                        .overallAverageMappingStrength(new BigDecimal("2.50"))
                        .build())
                .section2AverageDirect(ProgrammeAttainmentSnapshot.AverageDirectSection.builder()
                        .averageDirectAttainment(avgDirect)
                        .overallDirectAttainment(new BigDecimal("2.40"))
                        .build())
                .section3AverageIndirect(ProgrammeAttainmentSnapshot.AverageIndirectSection.builder()
                        .surveyType("Graduate Exit Survey")
                        .totalStudents(50)
                        .averageIndirectAttainment(avgIndirect)
                        .overallIndirectAttainment(new BigDecimal("2.60"))
                        .build())
                .section4OverallAttainment(ProgrammeAttainmentSnapshot.OverallAttainmentSection.builder()
                        .directWeightPercentage(new BigDecimal("80.00"))
                        .indirectWeightPercentage(new BigDecimal("20.00"))
                        .averageMappingStrength(avgMapping)
                        .averageDirectAttainment(avgDirect)
                        .averageIndirectAttainment(avgIndirect)
                        .finalAttainments(finalAttainments)
                        .overallProgrammeAttainment(new BigDecimal("2.44"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("Shared Programme Header renders exact 3-zone layout for 17 columns (Average Mapping)")
    void testProgrammeHeader17ColumnsGeometry() {
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(12, 2);
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Average Mapping");

        // 17 columns: 0 to 16
        int nextRow = CommonExcelHeaderRenderer.renderProgrammeHeader(
                wb, sheet, snapshot, "Average Mapping Strength", 17, null, null, "Term – I & II", true);
        assertEquals(8, nextRow, "Table content must start at row index 8 (Excel Row 9)");

        // Check merged regions
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        // Upper logo: A2:B3 (firstRow=1, lastRow=2, firstCol=0, lastCol=1)
        assertTrue(hasMergedRegion(mergedRegions, 1, 2, 0, 1), "A2:B3 must be merged for logo");
        // Institution: C2:M2 (firstRow=1, lastRow=1, firstCol=2, lastCol=12)
        assertTrue(hasMergedRegion(mergedRegions, 1, 1, 2, 12), "C2:M2 must be merged for Institution");
        // School: C3:M3 (firstRow=2, lastRow=2, firstCol=2, lastCol=12)
        assertTrue(hasMergedRegion(mergedRegions, 2, 2, 2, 12), "C3:M3 must be merged for School");
        // Upper right reserved box: N2:Q3 (firstRow=1, lastRow=2, firstCol=13, lastCol=16)
        assertTrue(hasMergedRegion(mergedRegions, 1, 2, 13, 16), "N2:Q3 must be merged for Reserved Box");

        // Row 3 & 4 (Excel Row 4 & 5): Academic Year (A4:B5), Report Title (C4:M5)
        assertTrue(hasMergedRegion(mergedRegions, 3, 4, 0, 1), "A4:B5 must be merged for Academic Year");
        assertTrue(hasMergedRegion(mergedRegions, 3, 4, 2, 12), "C4:M5 must be merged for Report Title");
        // Row 3 (Excel Row 4): Revision (N4:Q4)
        assertTrue(hasMergedRegion(mergedRegions, 3, 3, 13, 16), "N4:Q4 must be merged for Revision");

        // Row 4 (Excel Row 5): Dated (N5:Q5)
        assertTrue(hasMergedRegion(mergedRegions, 4, 4, 13, 16), "N5:Q5 must be merged for Dated");

        // Row 5 (Excel Row 6): Term (A6:B6), Dept/Prog (C6:M6), Date of Prep (N6:Q6)
        assertTrue(hasMergedRegion(mergedRegions, 5, 5, 0, 1), "A6:B6 must be merged for Term");
        assertTrue(hasMergedRegion(mergedRegions, 5, 5, 2, 12), "C6:M6 must be merged for Department/Programme");
        assertTrue(hasMergedRegion(mergedRegions, 5, 5, 13, 16), "N6:Q6 must be merged for Date of Preparation");

        // Text assertions
        assertEquals("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE", sheet.getRow(1).getCell(2).getStringCellValue());
        assertEquals("School of Engineering and Technology", sheet.getRow(2).getCell(2).getStringCellValue());
        assertEquals("Academic Year: 2024-2028", sheet.getRow(3).getCell(0).getStringCellValue());
        assertEquals("Average Mapping Strength", sheet.getRow(3).getCell(2).getStringCellValue());
        assertEquals("Revision : 00", sheet.getRow(3).getCell(13).getStringCellValue());
        assertEquals("Dated : —", sheet.getRow(4).getCell(13).getStringCellValue());
        assertEquals("Term – I & II", sheet.getRow(5).getCell(0).getStringCellValue());
        assertTrue(sheet.getRow(5).getCell(2).getStringCellValue().contains("Department of Computer Science and Engineering"));
        assertTrue(sheet.getRow(5).getCell(13).getStringCellValue().contains("Date of Preparation"));
    }

    @Test
    @DisplayName("Shared Programme Header renders exact 3-zone layout for 16 columns (Overall Attainment)")
    void testProgrammeHeader16ColumnsGeometry() {
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(12, 2);
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("Overall Programme Attainment");

        // 16 columns: 0 to 15
        int nextRow = CommonExcelHeaderRenderer.renderProgrammeHeader(
                wb, sheet, snapshot, "Overall Attainment", 16, null, null, "Term – I & II", true);
        assertEquals(8, nextRow, "Table content must start at row index 8 (Excel Row 9)");

        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        // Upper logo: A2:A3 (firstRow=1, lastRow=2, firstCol=0, lastCol=0)
        assertTrue(hasMergedRegion(mergedRegions, 1, 2, 0, 0), "A2:A3 must be merged for logo");
        // Institution: B2:L2 (firstRow=1, lastRow=1, firstCol=1, lastCol=11)
        assertTrue(hasMergedRegion(mergedRegions, 1, 1, 1, 11), "B2:L2 must be merged for Institution");
        // School: B3:L3 (firstRow=2, lastRow=2, firstCol=1, lastCol=11)
        assertTrue(hasMergedRegion(mergedRegions, 2, 2, 1, 11), "B3:L3 must be merged for School");
        // Upper right reserved box: M2:P3 (firstRow=1, lastRow=2, firstCol=12, lastCol=15)
        assertTrue(hasMergedRegion(mergedRegions, 1, 2, 12, 15), "M2:P3 must be merged for Reserved Box");

        // Row 3: Report Title at col 1
        assertEquals("Overall Attainment", sheet.getRow(3).getCell(1).getStringCellValue());
        assertEquals("Academic Year: 2024-2028", sheet.getRow(3).getCell(0).getStringCellValue());
        assertEquals("Revision : 00", sheet.getRow(3).getCell(12).getStringCellValue());
    }

    @Test
    @DisplayName("Master Programme Attainment Excel contains all 4 sheets in exact order with new header")
    void testMasterWorkbookCompleteFourSheets() throws Exception {
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(12, 2);
        ExcelReportRenderer renderer = new ExcelReportRenderer();

        byte[] masterExcel = renderer.renderProgrammeAttainmentMaster(snapshot);
        assertNotNull(masterExcel);
        assertTrue(masterExcel.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(masterExcel))) {
            assertEquals(4, wb.getNumberOfSheets(), "Master workbook must have exactly 4 sheets");
            assertEquals("Average Mapping", wb.getSheetName(0));
            assertTrue(wb.getSheetName(1).equalsIgnoreCase("average attainment(D)") || wb.getSheetName(1).equalsIgnoreCase("Average Direct Attainment"));
            assertTrue(wb.getSheetName(2).equalsIgnoreCase("AVERAGE ATTAINMENT (ID)") || wb.getSheetName(2).equalsIgnoreCase("Average Indirect Attainment"));
            assertEquals("Overall Programme Attainment", wb.getSheetName(3));

            for (int i = 0; i < 4; i++) {
                Sheet sheet = wb.getSheetAt(i);
                // Verify Row 0 is spacer (height > 0)
                Row r0 = sheet.getRow(0);
                assertNotNull(r0, "Spacer row 0 must exist in sheet " + i);

                // Verify Row 1 has Institution Name
                Row r1 = sheet.getRow(1);
                assertNotNull(r1, "Row 1 must exist in sheet " + i);
                String instName = (i == 3) ? r1.getCell(1).getStringCellValue() : r1.getCell(2).getStringCellValue();
                assertEquals("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE", instName);

                // Verify Row 2 has School Name
                Row r2 = sheet.getRow(2);
                assertNotNull(r2, "Row 2 must exist in sheet " + i);
                String schName = (i == 3) ? r2.getCell(1).getStringCellValue() : r2.getCell(2).getStringCellValue();
                assertEquals("School of Engineering and Technology", schName);

                // Verify Row 8 is table header
                Row r8 = sheet.getRow(8);
                assertNotNull(r8, "Row 8 (table header) must exist in sheet " + i);
            }
        }
    }

    private static byte[] createTestPng(int width, int height, Color color) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Configuration Case 1: Both Left and Right logos configured -> Both appear")
    void testBothLogosConfigured() throws Exception {
        byte[] leftLogo = createTestPng(270, 100, Color.RED);
        byte[] rightLogo = createTestPng(270, 100, Color.BLUE);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("BothLogos");
            CommonExcelHeaderRenderer.renderProgrammeHeader(
                    wb, sheet, "Uni", "School", "Report", "2024-25", "Term 1", "Dept", "00", "01/01/2025", "01/01/2025",
                    17, leftLogo, rightLogo, false);

            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            assertNotNull(drawing);
            List<XSSFShape> shapes = drawing.getShapes();
            assertEquals(2, shapes.size(), "Both left and right logos must be embedded");

            XSSFPicture p1 = (XSSFPicture) shapes.get(0);
            XSSFPicture p2 = (XSSFPicture) shapes.get(1);
            assertEquals(CommonExcelHeaderRenderer.FIXED_LOGO_WIDTH_EMU, calculateAnchorWidthEmu(sheet, p1.getClientAnchor()));
            assertEquals(CommonExcelHeaderRenderer.FIXED_LOGO_WIDTH_EMU, calculateAnchorWidthEmu(sheet, p2.getClientAnchor()));
        }
    }

    @Test
    @DisplayName("Configuration Case 2: Left logo configured, Right not configured -> Left appears, Right clean")
    void testLeftOnlyConfigured() throws Exception {
        byte[] leftLogo = createTestPng(100, 50, Color.RED);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("LeftOnly");
            CommonExcelHeaderRenderer.renderProgrammeHeader(
                    wb, sheet, "Uni", "School", "Report", "2024-25", "Term 1", "Dept", "00", "01/01/2025", "01/01/2025",
                    17, leftLogo, null, false);

            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            assertNotNull(drawing);
            List<XSSFShape> shapes = drawing.getShapes();
            assertEquals(1, shapes.size(), "Only left logo should be embedded");
        }
    }

    @Test
    @DisplayName("Configuration Case 3: Left logo not configured, Right configured -> Right appears, Left clean")
    void testRightOnlyConfigured() throws Exception {
        byte[] rightLogo = createTestPng(100, 50, Color.BLUE);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("RightOnly");
            CommonExcelHeaderRenderer.renderProgrammeHeader(
                    wb, sheet, "Uni", "School", "Report", "2024-25", "Term 1", "Dept", "00", "01/01/2025", "01/01/2025",
                    17, null, rightLogo, false);

            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            assertNotNull(drawing);
            List<XSSFShape> shapes = drawing.getShapes();
            assertEquals(1, shapes.size(), "Only right logo should be embedded");
        }
    }

    @Test
    @DisplayName("Configuration Case 4: Neither logo configured -> Clean regions, no exceptions")
    void testNeitherLogoConfigured() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("NoLogo");
            assertDoesNotThrow(() -> CommonExcelHeaderRenderer.renderProgrammeHeader(
                    wb, sheet, "Uni", "School", "Report", "2024-25", "Term 1", "Dept", "00", "01/01/2025", "01/01/2025",
                    17, null, null, false));

            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            if (drawing != null) {
                assertEquals(0, drawing.getShapes().size());
            }
        }
    }

    @Test
    @DisplayName("Configuration Case 5: Both configured with distinct images -> Distinct pictures embedded")
    void testDistinctImagesConfigured() throws Exception {
        byte[] leftLogo = createTestPng(80, 40, Color.RED);
        byte[] rightLogo = createTestPng(120, 60, Color.GREEN);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("DistinctLogos");
            CommonExcelHeaderRenderer.renderProgrammeHeader(
                    wb, sheet, "Uni", "School", "Report", "2024-25", "Term 1", "Dept", "00", "01/01/2025", "01/01/2025",
                    17, leftLogo, rightLogo, false);

            assertEquals(2, wb.getAllPictures().size(), "Workbook must contain exactly 2 distinct picture assets");
            assertNotEquals(wb.getAllPictures().get(0).getData().length, wb.getAllPictures().get(1).getData().length,
                    "Left and right images must be separate distinct byte payloads");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 12, 16, 17, 24, 35, 42})
    @DisplayName("Physical Width Invariance & Center Region Expansion across 8 to 42 columns")
    void testWidthInvarianceAcrossColumnCounts(int totalCols) throws Exception {
        byte[] leftLogo = createTestPng(270, 100, Color.RED);
        byte[] rightLogo = createTestPng(270, 100, Color.BLUE);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Cols_" + totalCols);
            for (int c = 0; c < totalCols; c++) {
                sheet.setColumnWidth(c, 8 * 256);
            }

            int nextRow = CommonExcelHeaderRenderer.renderProgrammeHeader(
                    wb, sheet, "Uni", "School", "Report", "2024-25", "Term 1", "Dept", "00", "01/01/2025", "01/01/2025",
                    totalCols, leftLogo, rightLogo, false);

            assertEquals(8, nextRow);

            XSSFDrawing drawing = sheet.getDrawingPatriarch();
            assertNotNull(drawing);
            List<XSSFShape> shapes = drawing.getShapes();
            assertEquals(2, shapes.size());

            XSSFPicture p1 = (XSSFPicture) shapes.get(0);
            XSSFPicture p2 = (XSSFPicture) shapes.get(1);

            assertEquals(CommonExcelHeaderRenderer.FIXED_LOGO_WIDTH_EMU, calculateAnchorWidthEmu(sheet, p1.getClientAnchor()),
                    "Left logo width must remain invariant for " + totalCols + " columns");
            assertEquals(CommonExcelHeaderRenderer.FIXED_LOGO_WIDTH_EMU, calculateAnchorWidthEmu(sheet, p2.getClientAnchor()),
                    "Right logo width must remain invariant for " + totalCols + " columns");

            // Verify header ends exactly at totalCols - 1
            int maxMergedCol = 0;
            for (CellRangeAddress r : sheet.getMergedRegions()) {
                if (r.getLastColumn() > maxMergedCol) {
                    maxMergedCol = r.getLastColumn();
                }
            }
            assertEquals(totalCols - 1, maxMergedCol, "Header last column must equal totalCols - 1");
        }
    }

    @Test
    @DisplayName("All four Programme sheet builders correctly accept and render both configured logos")
    void testAllFourProgrammeSheetBuilders() throws Exception {
        byte[] leftLogo = createTestPng(100, 50, Color.RED);
        byte[] rightLogo = createTestPng(100, 50, Color.BLUE);
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(12, 2);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s1 = AverageMappingSheetBuilder.build(wb, "S1", snapshot, leftLogo, rightLogo, null);
            Sheet s2 = AverageDirectAttainmentSheetBuilder.build(wb, "S2", snapshot, leftLogo, rightLogo, null);
            Sheet s3 = AverageIndirectAttainmentSheetBuilder.build(wb, "S3", snapshot, leftLogo, rightLogo, null);
            Sheet s4 = OverallAttainmentSheetBuilder.build(wb, "S4", snapshot, leftLogo, rightLogo, null);

            for (Sheet s : List.of(s1, s2, s3, s4)) {
                XSSFSheet xs = (XSSFSheet) s;
                XSSFDrawing drawing = xs.getDrawingPatriarch();
                assertNotNull(drawing);
                assertEquals(2, drawing.getShapes().size(), "Sheet " + s.getSheetName() + " must have both logos");
            }
        }
    }

    @Test
    @DisplayName("Master Programme Attainment Excel with both logos renders all 4 sheets with both logos")
    void testMasterWorkbookWithBothLogos() throws Exception {
        byte[] leftLogo = createTestPng(100, 50, Color.RED);
        byte[] rightLogo = createTestPng(100, 50, Color.BLUE);
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(12, 2);
        ExcelReportRenderer renderer = new ExcelReportRenderer();

        byte[] masterExcel = renderer.renderProgrammeAttainmentMaster(snapshot, leftLogo, rightLogo, null);
        assertNotNull(masterExcel);

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(masterExcel))) {
            assertEquals(4, wb.getNumberOfSheets());
            for (int i = 0; i < 4; i++) {
                XSSFSheet sheet = wb.getSheetAt(i);
                XSSFDrawing drawing = sheet.getDrawingPatriarch();
                assertNotNull(drawing, "Drawing must exist on sheet " + i);
                assertEquals(2, drawing.getShapes().size(), "Both logos must be embedded on sheet " + i);
            }
        }
    }

    private static long calculateAnchorWidthEmu(XSSFSheet sheet, org.apache.poi.xssf.usermodel.XSSFClientAnchor anchor) {
        int col1 = anchor.getCol1();
        int col2 = anchor.getCol2();
        long dx1 = anchor.getDx1();
        long dx2 = anchor.getDx2();

        if (col1 == col2) {
            return dx2 - dx1;
        }

        long widthEmu = Units.columnWidthToEMU(sheet.getColumnWidth(col1)) - dx1;
        for (int c = col1 + 1; c < col2; c++) {
            widthEmu += Units.columnWidthToEMU(sheet.getColumnWidth(c));
        }
        widthEmu += dx2;
        return widthEmu;
    }

    private boolean hasMergedRegion(List<CellRangeAddress> regions, int firstRow, int lastRow, int firstCol, int lastCol) {
        return regions.stream().anyMatch(r ->
                r.getFirstRow() == firstRow &&
                r.getLastRow() == lastRow &&
                r.getFirstColumn() == firstCol &&
                r.getLastColumn() == lastCol);
    }
}
