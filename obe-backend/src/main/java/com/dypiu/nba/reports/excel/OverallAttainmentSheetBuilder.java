package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OverallAttainmentSheetBuilder {

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Overall Attainment");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(wb, false);
        CellStyle psoHeaderStyle = ExcelStyles.createHeaderStyle(wb, true);
        CellStyle dataLeft = ExcelStyles.createDataStyle(wb, false, false);
        CellStyle dataCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle boldLabel = ExcelStyles.createDataStyle(wb, false, true);
        CellStyle mintLeft = ExcelStyles.createMintRowStyle(wb, false, true);
        CellStyle mintCenter = ExcelStyles.createMintRowStyle(wb, true, false);
        CellStyle blueLeft = ExcelStyles.createBlueRowStyle(wb, false, true);
        CellStyle blueCenter = ExcelStyles.createBlueRowStyle(wb, true, true);

        List<String> poCodes = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psoCodes = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();
        int totalCols = 2 + poCodes.size() + psoCodes.size();

        String progScope = "Programme: " + (snapshot.getMasterProgrammeName() != null ? snapshot.getMasterProgrammeName() : "")
                + (snapshot.getMasterProgrammeCode() != null && !snapshot.getMasterProgrammeCode().isBlank() ? " (" + snapshot.getMasterProgrammeCode() + ")" : "");

        int startRow = CommonExcelHeaderRenderer.renderHeader(
                wb, sheet, snapshot.getInstitutionName(), snapshot.getSchoolName(),
                "PROGRAMME ATTAINMENT — OVERALL ATTAINMENT (80% DIRECT + 20% INDIRECT)",
                progScope, snapshot.getAcademicYear(), "Batch Summary", snapshot.getReportId(), totalCols, true);

        // Table Header
        Row headerRow = sheet.createRow(startRow);
        headerRow.setHeightInPoints(24);

        int colIdx = 0;
        createCell(headerRow, colIdx++, "Academic Batch", headerStyle);
        createCell(headerRow, colIdx++, "Attainment Component", headerStyle);

        for (String po : poCodes) createCell(headerRow, colIdx++, po, headerStyle);
        for (String pso : psoCodes) createCell(headerRow, colIdx++, pso, psoHeaderStyle);

        ProgrammeAttainmentSnapshot.OverallAttainmentSection sec = snapshot.getSection4OverallAttainment();
        Map<String, BigDecimal> mapValues = sec != null ? sec.getAverageMappingStrength() : Map.of();
        Map<String, BigDecimal> dirValues = sec != null ? sec.getAverageDirectAttainment() : Map.of();
        Map<String, BigDecimal> indValues = sec != null ? sec.getAverageIndirectAttainment() : Map.of();
        Map<String, BigDecimal> finalValues = sec != null ? sec.getFinalAttainments() : Map.of();

        String batchLabel = snapshot.getAcademicBatchYears() != null ? snapshot.getAcademicBatchYears()
                : (snapshot.getProgrammeBatchName() != null ? snapshot.getProgrammeBatchName() : "Batch");

        int rIdx = startRow + 1;

        // Row 1: Average Mapping Values (Mint)
        Row row1 = sheet.createRow(rIdx++);
        row1.setHeightInPoints(20);
        createCell(row1, 0, batchLabel, dataCenter);
        createCell(row1, 1, "Average Mapping Values", mintLeft);
        int c1 = 2;
        for (String po : poCodes) fillNumCell(row1, c1++, mapValues != null ? mapValues.get(po) : null, mintCenter);
        for (String pso : psoCodes) fillNumCell(row1, c1++, mapValues != null ? mapValues.get(pso) : null, mintCenter);

        // Row 2: Average Attainment (Direct) (White)
        Row row2 = sheet.createRow(rIdx++);
        row2.setHeightInPoints(20);
        createCell(row2, 0, "", dataCenter);
        createCell(row2, 1, "Average Attainment (Direct)", boldLabel);
        int c2 = 2;
        for (String po : poCodes) fillNumCell(row2, c2++, dirValues != null ? dirValues.get(po) : null, dataCenter);
        for (String pso : psoCodes) fillNumCell(row2, c2++, dirValues != null ? dirValues.get(pso) : null, dataCenter);

        // Row 3: Average Attainment (Indirect) (White)
        Row row3 = sheet.createRow(rIdx++);
        row3.setHeightInPoints(20);
        createCell(row3, 0, "", dataCenter);
        createCell(row3, 1, "Average Attainment (Indirect)", boldLabel);
        int c3 = 2;
        for (String po : poCodes) fillNumCell(row3, c3++, indValues != null ? indValues.get(po) : null, dataCenter);
        for (String pso : psoCodes) fillNumCell(row3, c3++, indValues != null ? indValues.get(pso) : null, dataCenter);

        // Row 4: Overall Attainment (Sky Blue)
        Row row4 = sheet.createRow(rIdx++);
        row4.setHeightInPoints(22);
        createCell(row4, 0, "", dataCenter);
        createCell(row4, 1, "Overall Attainment (80% Direct + 20% Indirect)", blueLeft);
        int c4 = 2;
        for (String po : poCodes) fillNumCell(row4, c4++, finalValues != null ? finalValues.get(po) : null, blueCenter);
        for (String pso : psoCodes) fillNumCell(row4, c4++, finalValues != null ? finalValues.get(pso) : null, blueCenter);

        // Merge Batch column
        sheet.addMergedRegion(new CellRangeAddress(startRow + 1, startRow + 4, 0, 0));

        sheet.createFreezePane(0, startRow + 1);
        sheet.setRepeatingRows(CellRangeAddress.valueOf("1:" + (startRow + 1)));

        // Auto Sizing
        sheet.setColumnWidth(0, 18 * 256);
        sheet.setColumnWidth(1, 45 * 256);
        for (int i = 2; i < totalCols; i++) {
            sheet.setColumnWidth(i, 12 * 256);
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
        if (val != null) c.setCellValue(val.doubleValue());
        else c.setCellValue("—");
        c.setCellStyle(style);
    }
}
