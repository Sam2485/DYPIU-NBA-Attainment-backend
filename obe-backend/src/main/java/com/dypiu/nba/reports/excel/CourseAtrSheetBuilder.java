package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAtrSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.util.List;

public class CourseAtrSheetBuilder {

    public static Sheet build(Workbook wb, String sheetName, CourseAtrSnapshot snapshot) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Course ATR");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(wb, false);
        CellStyle dataLeft = ExcelStyles.createDataStyle(wb, false, false);
        CellStyle dataCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle boldCenter = ExcelStyles.createDataStyle(wb, true, true);
        CellStyle statusMet = ExcelStyles.createStatusMetStyle(wb);
        CellStyle statusGap = ExcelStyles.createStatusGapStyle(wb);
        CellStyle subTitleStyle = ExcelStyles.createSubTitleStyle(wb);

        String courseScope = "Course: " + (snapshot.getCourseName() != null ? snapshot.getCourseName() : "")
                + (snapshot.getCourseCode() != null && !snapshot.getCourseCode().isBlank() ? " (" + snapshot.getCourseCode() + ")" : "");
        String semStr = "Semester " + (snapshot.getSemester() != null ? snapshot.getSemester() : "—");

        int totalCols = 6;
        int rowIdx = CommonExcelHeaderRenderer.renderHeader(
                wb, sheet, snapshot.getInstitutionName(), snapshot.getSchoolName(),
                "COURSE ACTION TAKEN REPORT (ATR)",
                courseScope, snapshot.getAcademicYear(), semStr, snapshot.getReportId(), totalCols, false);

        // --- SECTION: COURSE OUTCOMES (COs) ---
        Row coTitle = sheet.createRow(rowIdx++);
        coTitle.setHeightInPoints(20);
        createCell(coTitle, 0, "Course Outcomes (COs) Action Plan", subTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, totalCols - 1));

        Row coHead = sheet.createRow(rowIdx++);
        coHead.setHeightInPoints(22);
        createCell(coHead, 0, "CO Code", headerStyle);
        createCell(coHead, 1, "Course Outcome Statement", headerStyle);
        createCell(coHead, 2, "Target", headerStyle);
        createCell(coHead, 3, "Actual", headerStyle);
        createCell(coHead, 4, "% Achieved", headerStyle);
        createCell(coHead, 5, "Status", headerStyle);

        if (snapshot.getOutcomes() != null) {
            for (CourseAtrSnapshot.AtrOutcomeRow r : snapshot.getOutcomes()) {
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
