package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAtrSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.util.List;

public class ProgrammeAtrSheetBuilder {

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAtrSnapshot snapshot) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Programme ATR");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(wb, false);
        CellStyle psoHeaderStyle = ExcelStyles.createHeaderStyle(wb, true);
        CellStyle dataLeft = ExcelStyles.createDataStyle(wb, false, false);
        CellStyle dataCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle boldCenter = ExcelStyles.createDataStyle(wb, true, true);
        CellStyle statusMet = ExcelStyles.createStatusMetStyle(wb);
        CellStyle statusGap = ExcelStyles.createStatusGapStyle(wb);
        CellStyle subTitleStyle = ExcelStyles.createSubTitleStyle(wb);

        String progScope = "Programme: " + (snapshot.getMasterProgrammeName() != null ? snapshot.getMasterProgrammeName() : "")
                + (snapshot.getMasterProgrammeCode() != null && !snapshot.getMasterProgrammeCode().isBlank() ? " (" + snapshot.getMasterProgrammeCode() + ")" : "");

        int totalCols = 6;
        int rowIdx = CommonExcelHeaderRenderer.renderHeader(
                wb, sheet, snapshot.getInstitutionName(), snapshot.getSchoolName(),
                "PROGRAMME ACTION TAKEN REPORT (ATR)",
                progScope, snapshot.getAcademicYear(), "All Semesters", snapshot.getReportId(), totalCols, false);

        // --- SECTION 1: PROGRAMME OUTCOMES (POs) ---
        Row poTitle = sheet.createRow(rowIdx++);
        poTitle.setHeightInPoints(20);
        createCell(poTitle, 0, "1. Programme Outcomes (POs) Action Plan", subTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, totalCols - 1));

        Row poHead = sheet.createRow(rowIdx++);
        poHead.setHeightInPoints(22);
        createCell(poHead, 0, "Outcome Code", headerStyle);
        createCell(poHead, 1, "Outcome Statement", headerStyle);
        createCell(poHead, 2, "Target", headerStyle);
        createCell(poHead, 3, "Actual", headerStyle);
        createCell(poHead, 4, "% Achieved", headerStyle);
        createCell(poHead, 5, "Status", headerStyle);

        if (snapshot.getPoOutcomes() != null) {
            for (ProgrammeAtrSnapshot.AtrOutcomeRow r : snapshot.getPoOutcomes()) {
                rowIdx = renderAtrOutcomeBlock(sheet, r, rowIdx, dataLeft, dataCenter, boldCenter, statusMet, statusGap, false);
            }
        }

        sheet.createRow(rowIdx++).setHeightInPoints(8); // spacer

        // --- SECTION 2: PROGRAMME SPECIFIC OUTCOMES (PSOs) ---
        Row psoTitle = sheet.createRow(rowIdx++);
        psoTitle.setHeightInPoints(20);
        createCell(psoTitle, 0, "2. Programme Specific Outcomes (PSOs) Action Plan", subTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, totalCols - 1));

        Row psoHead = sheet.createRow(rowIdx++);
        psoHead.setHeightInPoints(22);
        createCell(psoHead, 0, "Outcome Code", psoHeaderStyle);
        createCell(psoHead, 1, "Outcome Statement", psoHeaderStyle);
        createCell(psoHead, 2, "Target", psoHeaderStyle);
        createCell(psoHead, 3, "Actual", psoHeaderStyle);
        createCell(psoHead, 4, "% Achieved", psoHeaderStyle);
        createCell(psoHead, 5, "Status", psoHeaderStyle);

        if (snapshot.getPsoOutcomes() != null) {
            for (ProgrammeAtrSnapshot.AtrOutcomeRow r : snapshot.getPsoOutcomes()) {
                rowIdx = renderAtrOutcomeBlock(sheet, r, rowIdx, dataLeft, dataCenter, boldCenter, statusMet, statusGap, true);
            }
        }

        // Auto Sizing
        sheet.setColumnWidth(0, 12 * 256);
        sheet.setColumnWidth(1, 45 * 256);
        sheet.setColumnWidth(2, 10 * 256);
        sheet.setColumnWidth(3, 10 * 256);
        sheet.setColumnWidth(4, 12 * 256);
        sheet.setColumnWidth(5, 16 * 256);

        return sheet;
    }

    private static int renderAtrOutcomeBlock(
            Sheet sheet,
            ProgrammeAtrSnapshot.AtrOutcomeRow r,
            int rowIdx,
            CellStyle dataLeft,
            CellStyle dataCenter,
            CellStyle boldCenter,
            CellStyle statusMet,
            CellStyle statusGap,
            boolean isPso) {

        boolean isMet = (r.getAttainmentLevel() != null && r.getTargetLevel() != null
                && r.getAttainmentLevel().compareTo(r.getTargetLevel()) >= 0);

        Row rRow = sheet.createRow(rowIdx++);
        rRow.setHeightInPoints(20);
        createCell(rRow, 0, r.getOutcomeCode(), boldCenter);
        createCell(rRow, 1, r.getOutcomeStatement() != null ? r.getOutcomeStatement() : "", dataLeft);
        fillNumCell(rRow, 2, r.getTargetLevel(), dataCenter);
        fillNumCell(rRow, 3, r.getAttainmentLevel(), dataCenter);
        fillPctCell(rRow, 4, r.getAchievementPercentage(), dataCenter);
        createCell(rRow, 5, isMet ? "TARGET MET" : "GAP IDENTIFIED", isMet ? statusMet : statusGap);

        // Actions row
        Row actRow = sheet.createRow(rowIdx++);
        actRow.setHeightInPoints(24);
        createCell(actRow, 0, "Actions:", dataCenter);

        StringBuilder sb = new StringBuilder();
        if (r.getActions() != null && !r.getActions().isEmpty()) {
            int aIdx = 1;
            for (String a : r.getActions()) {
                if (a != null && !a.isBlank()) {
                    if (sb.length() > 0) sb.append("  |  ");
                    sb.append(aIdx++).append(". ").append(a);
                }
            }
        } else {
            sb.append("1. Maintain current instructional methodology.");
        }

        createCell(actRow, 1, sb.toString(), dataLeft);
        for (int c = 2; c <= 5; c++) createCell(actRow, c, "", dataLeft);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 1, 5));

        return rowIdx;
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

    private static void fillPctCell(Row row, int col, BigDecimal val, CellStyle style) {
        Cell c = row.createCell(col);
        if (val != null) c.setCellValue(val.toPlainString() + "%");
        else c.setCellValue("—");
        c.setCellStyle(style);
    }
}
