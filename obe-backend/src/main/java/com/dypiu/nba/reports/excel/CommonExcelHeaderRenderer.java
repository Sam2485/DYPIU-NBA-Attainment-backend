package com.dypiu.nba.reports.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

public class CommonExcelHeaderRenderer {

    public static int renderHeader(
            Workbook wb,
            Sheet sheet,
            String institutionName,
            String schoolName,
            String reportTitle,
            String scopeLabelAndValue,
            String academicYear,
            String termOrSemester,
            String reportId,
            int totalColumns,
            boolean isLandscape) {

        int endCol = Math.max(totalColumns - 1, 4);

        CellStyle instStyle = ExcelStyles.createTitleStyle(wb);
        CellStyle schoolStyle = ExcelStyles.createSchoolTitleStyle(wb);
        CellStyle titleStyle = ExcelStyles.createSubTitleStyle(wb);
        CellStyle metaStyle = ExcelStyles.createMetaStyle(wb);

        // Row 0: Institution Name
        Row r0 = sheet.createRow(0);
        r0.setHeightInPoints(24);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(institutionName != null && !institutionName.isBlank()
                ? institutionName.toUpperCase() : "D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE");
        c0.setCellStyle(instStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, endCol));

        // Row 1: School Name
        Row r1 = sheet.createRow(1);
        r1.setHeightInPoints(18);
        Cell c1 = r1.createCell(0);
        c1.setCellValue(schoolName != null && !schoolName.isBlank()
                ? schoolName : "School of Engineering and Technology");
        c1.setCellStyle(schoolStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, endCol));

        // Row 2: Report Title
        Row r2 = sheet.createRow(2);
        r2.setHeightInPoints(20);
        Cell c2 = r2.createCell(0);
        c2.setCellValue(reportTitle != null ? reportTitle.toUpperCase() : "ACADEMIC REPORT");
        c2.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, endCol));

        // Row 3: Metadata Summary Bar
        Row r3 = sheet.createRow(3);
        r3.setHeightInPoints(18);
        Cell c3 = r3.createCell(0);
        String metaText = (scopeLabelAndValue != null ? scopeLabelAndValue : "")
                + "  |  Academic Year: " + (academicYear != null ? academicYear : "—")
                + "  |  Term: " + (termOrSemester != null ? termOrSemester : "All Semesters")
                + (reportId != null && !reportId.isBlank() ? "  |  Report ID: " + reportId : "");
        c3.setCellValue(metaText);
        c3.setCellStyle(metaStyle);
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, endCol));

        // Row 4: Spacer
        Row r4 = sheet.createRow(4);
        r4.setHeightInPoints(6);

        // Configure Sheet Print & Page setup
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setLandscape(isLandscape);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setDisplayGridlines(true);

        return 5; // Next row index for table headers
    }
}
