package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel builder for Sheet #4: Overall Programme Attainment
 * Recreates the exact layout, row sequence, spacer rows, and styling of the reference workbook.
 */
public class OverallAttainmentSheetBuilder {

    public static final String DEFAULT_SHEET_NAME = "Overall Programme Attainment";

    public static Sheet build(Workbook wb, ProgrammeAttainmentSnapshot snapshot) {
        return build(wb, DEFAULT_SHEET_NAME, snapshot, null, null);
    }

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot) {
        return build(wb, sheetName, snapshot, null, null);
    }

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot, byte[] logoBytes, ReportTemplateDto template) {
        String resolvedSheetName = (sheetName != null && !sheetName.isBlank()) ? sheetName : DEFAULT_SHEET_NAME;
        Sheet sheet = wb.createSheet(resolvedSheetName);

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(wb, false);
        CellStyle psoHeaderStyle = ExcelStyles.createHeaderStyle(wb, true);
        CellStyle dataLeft = ExcelStyles.createDataStyle(wb, false, false);
        CellStyle dataCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle boldLabel = ExcelStyles.createDataStyle(wb, false, true);
        CellStyle mintLeft = ExcelStyles.createMintRowStyle(wb, false, true);
        CellStyle mintCenter = ExcelStyles.createMintRowStyle(wb, true, false);
        CellStyle blueLeft = ExcelStyles.createBlueRowStyle(wb, false, true);
        blueLeft.setWrapText(true);
        CellStyle blueCenter = ExcelStyles.createBlueRowStyle(wb, true, true);

        List<String> poCodes = new ArrayList<>(snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of());
        poCodes.sort(ExcelStyles.NATURAL_NUMERICAL_COMPARATOR);
        List<String> psoCodes = new ArrayList<>(snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of());
        psoCodes.sort(ExcelStyles.NATURAL_NUMERICAL_COMPARATOR);
        int totalCols = 2 + poCodes.size() + psoCodes.size();

        int startRow = CommonExcelHeaderRenderer.renderProgrammeHeader(
                wb, sheet, snapshot, "Overall Attainment", totalCols, logoBytes, template, "Term – I & II", true);

        // Table Header (Excel Row 9, startRow index 8)
        Row headerRow = sheet.createRow(startRow);
        headerRow.setHeightInPoints(18.0f);

        int colIdx = 0;
        createCell(headerRow, colIdx++, "Year", headerStyle);
        createCell(headerRow, colIdx++, "Course Name", headerStyle);

        for (String po : poCodes) createCell(headerRow, colIdx++, po, headerStyle);
        for (String pso : psoCodes) createCell(headerRow, colIdx++, pso, psoHeaderStyle);

        ProgrammeAttainmentSnapshot.OverallAttainmentSection sec = snapshot.getSection4OverallAttainment();
        Map<String, BigDecimal> mapValues = sec != null ? sec.getAverageMappingStrength() : Map.of();
        Map<String, BigDecimal> dirValues = sec != null ? sec.getAverageDirectAttainment() : Map.of();
        Map<String, BigDecimal> indValues = sec != null ? sec.getAverageIndirectAttainment() : Map.of();
        Map<String, BigDecimal> finalValues = sec != null ? sec.getFinalAttainments() : Map.of();

        String yearLabel = (snapshot.getAcademicYear() != null && !snapshot.getAcademicYear().isBlank())
                ? ("AY " + snapshot.getAcademicYear())
                : ((snapshot.getAcademicBatchYears() != null && !snapshot.getAcademicBatchYears().isBlank())
                        ? snapshot.getAcademicBatchYears()
                        : ((snapshot.getProgrammeBatchName() != null && !snapshot.getProgrammeBatchName().isBlank())
                                ? snapshot.getProgrammeBatchName()
                                : "AY ----"));

        int rIdx = startRow + 1;

        // Row 1 (Excel Row 10): Average Mapping Values (Mint)
        Row row1 = sheet.createRow(rIdx++);
        row1.setHeightInPoints(16.0f);
        createCell(row1, 0, yearLabel, dataCenter);
        createCell(row1, 1, "Average Mapping Values", mintLeft);
        int c1 = 2;
        for (String po : poCodes) fillNumCell(row1, c1++, mapValues != null ? mapValues.get(po) : null, mintCenter);
        for (String pso : psoCodes) fillNumCell(row1, c1++, mapValues != null ? mapValues.get(pso) : null, mintCenter);

        // Row 2 (Excel Row 11): Spacer Row 1 (ht=6.75)
        Row spacerRow1 = sheet.createRow(rIdx++);
        spacerRow1.setHeightInPoints(6.75f);
        createCell(spacerRow1, 0, "", dataCenter);
        createCell(spacerRow1, 1, "", dataLeft);
        for (int c = 2; c < totalCols; c++) {
            createCell(spacerRow1, c, "", dataCenter);
        }

        // Row 3 (Excel Row 12): Average Attainment (Direct) (White)
        Row row2 = sheet.createRow(rIdx++);
        row2.setHeightInPoints(16.0f);
        createCell(row2, 0, "", dataCenter);
        createCell(row2, 1, "Average Attainment (Direct)", boldLabel);
        int c2 = 2;
        for (String po : poCodes) fillNumCell(row2, c2++, dirValues != null ? dirValues.get(po) : null, dataCenter);
        for (String pso : psoCodes) fillNumCell(row2, c2++, dirValues != null ? dirValues.get(pso) : null, dataCenter);

        // Row 4 (Excel Row 13): Average Attainment (Indirect) (White)
        Row row3 = sheet.createRow(rIdx++);
        row3.setHeightInPoints(16.0f);
        createCell(row3, 0, "", dataCenter);
        createCell(row3, 1, "Average Attainment (Indirect)", boldLabel);
        int c3 = 2;
        for (String po : poCodes) fillNumCell(row3, c3++, indValues != null ? indValues.get(po) : null, dataCenter);
        for (String pso : psoCodes) fillNumCell(row3, c3++, indValues != null ? indValues.get(pso) : null, dataCenter);

        // Row 5 (Excel Row 14): Spacer Row 2 (ht=15.75)
        Row spacerRow2 = sheet.createRow(rIdx++);
        spacerRow2.setHeightInPoints(15.75f);
        createCell(spacerRow2, 0, "", dataCenter);
        createCell(spacerRow2, 1, "", dataLeft);
        for (int c = 2; c < totalCols; c++) {
            createCell(spacerRow2, c, "", dataCenter);
        }

        // Row 6 (Excel Row 15): Overall Attainment (Sky Blue, height=27.0f, wrapped label)
        Row row4 = sheet.createRow(rIdx++);
        row4.setHeightInPoints(27.0f);
        createCell(row4, 0, "", dataCenter);
        createCell(row4, 1, "Overall Attainment\n(80% of Direct + 20% of Indirect)", blueLeft);
        int c4 = 2;
        for (String po : poCodes) fillNumCell(row4, c4++, finalValues != null ? finalValues.get(po) : null, blueCenter);
        for (String pso : psoCodes) fillNumCell(row4, c4++, finalValues != null ? finalValues.get(pso) : null, blueCenter);

        // Merge Year column across all component and spacer rows (rows 10 to 15)
        CellRangeAddress yearRegion = new CellRangeAddress(startRow + 1, startRow + 6, 0, 0);
        sheet.addMergedRegion(yearRegion);
        RegionUtil.setBorderTop(BorderStyle.THIN, yearRegion, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, yearRegion, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, yearRegion, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, yearRegion, sheet);

        // Freeze Panes & Repeating Rows
        sheet.createFreezePane(0, startRow + 1);
        sheet.setRepeatingRows(CellRangeAddress.valueOf("1:" + (startRow + 1)));

        // Page & Print Setup
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        printSetup.setPaperSize(PrintSetup.A4_PAPERSIZE);
        sheet.setFitToPage(true);
        printSetup.setFitWidth((short) 1);
        printSetup.setFitHeight((short) 0);
        sheet.setAutobreaks(true);
        sheet.setHorizontallyCenter(true);
        sheet.setMargin(Sheet.LeftMargin, 0.25);
        sheet.setMargin(Sheet.RightMargin, 0.25);
        sheet.setMargin(Sheet.TopMargin, 0.5);
        sheet.setMargin(Sheet.BottomMargin, 0.5);
        wb.setPrintArea(wb.getSheetIndex(sheet), 0, totalCols - 1, 0, startRow + 6);

        // Column Proportional Widths matching reference
        sheet.setColumnWidth(0, (int) (22.0 * 256));
        sheet.setColumnWidth(1, (int) (38.0 * 256));
        for (int i = 2; i < totalCols; i++) {
            sheet.setColumnWidth(i, (int) (7.50 * 256));
        }

        return sheet;
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }

    private static void fillNumCell(Row row, int col, BigDecimal val, CellStyle style) {
        Cell c = row.createCell(col);
        if (val != null) {
            c.setCellValue(val.doubleValue());
        } else {
            c.setCellValue("—");
        }
        c.setCellStyle(style);
    }
}
