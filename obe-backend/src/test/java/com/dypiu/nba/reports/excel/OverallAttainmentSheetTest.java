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

class OverallAttainmentSheetTest {

    private ProgrammeAttainmentSnapshot createSampleSnapshot(
            List<String> poCodes,
            List<String> psoCodes,
            Map<String, BigDecimal> mapValues,
            Map<String, BigDecimal> dirValues,
            Map<String, BigDecimal> indValues,
            Map<String, BigDecimal> finalValues) {

        return ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-ovr-001")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .departmentName("Department of Computer Science and Engineering")
                .masterProgrammeName("B.Tech Computer Science")
                .masterProgrammeCode("BTECH-CSE")
                .academicYear("2024-2028")
                .academicBatchYears("2024-2028")
                .poCodes(poCodes)
                .psoCodes(psoCodes)
                .section4OverallAttainment(ProgrammeAttainmentSnapshot.OverallAttainmentSection.builder()
                        .directWeightPercentage(new BigDecimal("80.00"))
                        .indirectWeightPercentage(new BigDecimal("20.00"))
                        .averageMappingStrength(mapValues)
                        .averageDirectAttainment(dirValues)
                        .averageIndirectAttainment(indValues)
                        .finalAttainments(finalValues)
                        .overallProgrammeAttainment(new BigDecimal("2.64"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("Requirement: Sheet name is exactly 'Overall Programme Attainment'")
    void testSheetNameIsOverallProgrammeAttainment() {
        List<String> pos = List.of("PO1", "PO2");
        List<String> psos = List.of("PSO1");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, Map.of(), Map.of(), Map.of(), Map.of());
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = OverallAttainmentSheetBuilder.build(wb, null, snapshot);
        assertNotNull(sheet);
        assertEquals("Overall Programme Attainment", sheet.getSheetName());

        Workbook wb2 = new XSSFWorkbook();
        Sheet sheet2 = OverallAttainmentSheetBuilder.build(wb2, snapshot);
        assertNotNull(sheet2);
        assertEquals("Overall Programme Attainment", sheet2.getSheetName());
    }

    @Test
    @DisplayName("Requirement: Table header row and exact column titles")
    void testTableHeaderStructure() {
        List<String> pos = List.of("PO1", "PO2", "PO3");
        List<String> psos = List.of("PSO1", "PSO2");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, Map.of(), Map.of(), Map.of(), Map.of());
        Workbook wb = new XSSFWorkbook();

        Sheet sheet = OverallAttainmentSheetBuilder.build(wb, snapshot);
        Row th = sheet.getRow(8);
        assertNotNull(th, "Table header row index 8 must exist");

        assertEquals("Year", th.getCell(0).getStringCellValue());
        assertEquals("Course Name", th.getCell(1).getStringCellValue());
        assertEquals("PO1", th.getCell(2).getStringCellValue());
        assertEquals("PO2", th.getCell(3).getStringCellValue());
        assertEquals("PO3", th.getCell(4).getStringCellValue());
        assertEquals("PSO1", th.getCell(5).getStringCellValue());
        assertEquals("PSO2", th.getCell(6).getStringCellValue());
    }

    @Test
    @DisplayName("Requirement: Exact row sequence including spacer rows and wrapped overall label")
    void testRowSequenceAndSpacers() {
        List<String> pos = List.of("PO1", "PO2");
        List<String> psos = List.of("PSO1");

        Map<String, BigDecimal> mapVals = Map.of("PO1", new BigDecimal("2.50"), "PO2", new BigDecimal("2.70"), "PSO1", new BigDecimal("2.60"));
        Map<String, BigDecimal> dirVals = Map.of("PO1", new BigDecimal("2.40"), "PO2", new BigDecimal("2.60"), "PSO1", new BigDecimal("2.50"));
        Map<String, BigDecimal> indVals = Map.of("PO1", new BigDecimal("2.80"), "PO2", new BigDecimal("2.90"), "PSO1", new BigDecimal("2.85"));
        Map<String, BigDecimal> finalVals = Map.of("PO1", new BigDecimal("2.48"), "PO2", new BigDecimal("2.66"), "PSO1", new BigDecimal("2.57"));

        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, mapVals, dirVals, indVals, finalVals);
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = OverallAttainmentSheetBuilder.build(wb, snapshot);

        // Row 9: Average Mapping Values
        Row row9 = sheet.getRow(9);
        assertNotNull(row9);
        assertEquals("AY 2024-2028", row9.getCell(0).getStringCellValue());
        assertEquals("Average Mapping Values", row9.getCell(1).getStringCellValue());
        assertEquals(2.50, row9.getCell(2).getNumericCellValue(), 0.001);
        assertEquals(2.70, row9.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.60, row9.getCell(4).getNumericCellValue(), 0.001);

        // Row 10: Spacer row 1 (height 6.75)
        Row row10 = sheet.getRow(10);
        assertNotNull(row10);
        assertEquals(6.75f, row10.getHeightInPoints(), 0.01f);
        assertEquals("", row10.getCell(1).getStringCellValue());

        // Row 11: Average Attainment (Direct)
        Row row11 = sheet.getRow(11);
        assertNotNull(row11);
        assertEquals("Average Attainment (Direct)", row11.getCell(1).getStringCellValue());
        assertEquals(2.40, row11.getCell(2).getNumericCellValue(), 0.001);
        assertEquals(2.60, row11.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.50, row11.getCell(4).getNumericCellValue(), 0.001);

        // Row 12: Average Attainment (Indirect)
        Row row12 = sheet.getRow(12);
        assertNotNull(row12);
        assertEquals("Average Attainment (Indirect)", row12.getCell(1).getStringCellValue());
        assertEquals(2.80, row12.getCell(2).getNumericCellValue(), 0.001);
        assertEquals(2.90, row12.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.85, row12.getCell(4).getNumericCellValue(), 0.001);

        // Row 13: Spacer row 2 (height 15.75)
        Row row13 = sheet.getRow(13);
        assertNotNull(row13);
        assertEquals(15.75f, row13.getHeightInPoints(), 0.01f);
        assertEquals("", row13.getCell(1).getStringCellValue());

        // Row 14: Overall Attainment (height 27.0, wrap text)
        Row row14 = sheet.getRow(14);
        assertNotNull(row14);
        assertEquals(27.0f, row14.getHeightInPoints(), 0.01f);
        assertTrue(row14.getCell(1).getStringCellValue().contains("Overall Attainment"));
        assertTrue(row14.getCell(1).getStringCellValue().contains("80% of Direct + 20% of Indirect"));
        assertTrue(row14.getCell(1).getCellStyle().getWrapText());
        assertEquals(2.48, row14.getCell(2).getNumericCellValue(), 0.001);
        assertEquals(2.66, row14.getCell(3).getNumericCellValue(), 0.001);
        assertEquals(2.57, row14.getCell(4).getNumericCellValue(), 0.001);
    }

    @Test
    @DisplayName("Requirement: Year cell is vertically merged across rows 9 to 14 (Excel rows 10 to 15)")
    void testYearCellMerge() {
        List<String> pos = List.of("PO1", "PO2");
        List<String> psos = List.of("PSO1");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, Map.of(), Map.of(), Map.of(), Map.of());
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = OverallAttainmentSheetBuilder.build(wb, snapshot);

        boolean mergeFound = false;
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            if (range.getFirstRow() == 9 && range.getLastRow() == 14 && range.getFirstColumn() == 0 && range.getLastColumn() == 0) {
                mergeFound = true;
                break;
            }
        }
        assertTrue(mergeFound, "Year column merge (rows 9..14, col 0) must exist");
    }

    @Test
    @DisplayName("Requirement: Natural numerical sorting for POs and PSOs")
    void testNaturalNumericalSorting() {
        List<String> pos = List.of("PO12", "PO2", "PO10", "PO1");
        List<String> psos = List.of("PSO3", "PSO1", "PSO2");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, Map.of(), Map.of(), Map.of(), Map.of());
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = OverallAttainmentSheetBuilder.build(wb, snapshot);

        Row th = sheet.getRow(8);
        assertNotNull(th);
        assertEquals("PO1", th.getCell(2).getStringCellValue());
        assertEquals("PO2", th.getCell(3).getStringCellValue());
        assertEquals("PO10", th.getCell(4).getStringCellValue());
        assertEquals("PO12", th.getCell(5).getStringCellValue());
        assertEquals("PSO1", th.getCell(6).getStringCellValue());
        assertEquals("PSO2", th.getCell(7).getStringCellValue());
        assertEquals("PSO3", th.getCell(8).getStringCellValue());
    }

    @Test
    @DisplayName("Requirement: Header scaling invariance across variable PO/PSO column counts")
    void testHeaderScalingInvariance() {
        List<String> pos = List.of("PO1", "PO2", "PO3", "PO4", "PO5", "PO6", "PO7", "PO8", "PO9", "PO10", "PO11", "PO12");
        List<String> psos = List.of("PSO1", "PSO2", "PSO3");
        ProgrammeAttainmentSnapshot snapshot = createSampleSnapshot(pos, psos, Map.of(), Map.of(), Map.of(), Map.of());
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = OverallAttainmentSheetBuilder.build(wb, snapshot);

        // Header occupies rows 0 to 7. Next row is index 8 (Row 9 in Excel)
        Row th = sheet.getRow(8);
        assertNotNull(th);
        assertEquals("Year", th.getCell(0).getStringCellValue());

        // For 17 columns (0..16), colLeftEnd is 1, so institution starts at col 2
        Row row1 = sheet.getRow(1);
        assertNotNull(row1);
        assertEquals("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE", row1.getCell(2).getStringCellValue());

        // Right metadata columns start at col 13 (colRightStart = 17 - 4 = 13)
        Row row3 = sheet.getRow(3);
        assertNotNull(row3);
        Cell revCell = row3.getCell(13);
        assertNotNull(revCell);
        assertTrue(revCell.getStringCellValue().contains("Revision"));
    }
}
