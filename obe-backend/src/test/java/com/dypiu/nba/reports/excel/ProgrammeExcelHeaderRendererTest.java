package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
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

    private boolean hasMergedRegion(List<CellRangeAddress> regions, int firstRow, int lastRow, int firstCol, int lastCol) {
        return regions.stream().anyMatch(r ->
                r.getFirstRow() == firstRow &&
                r.getLastRow() == lastRow &&
                r.getFirstColumn() == firstCol &&
                r.getLastColumn() == lastCol);
    }
}
