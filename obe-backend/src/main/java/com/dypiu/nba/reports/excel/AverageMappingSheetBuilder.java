package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AverageMappingSheetBuilder {

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Average Mapping");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(wb, false);
        CellStyle psoHeaderStyle = ExcelStyles.createHeaderStyle(wb, true);
        CellStyle dataLeft = ExcelStyles.createDataStyle(wb, false, false);
        CellStyle dataCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle boldCode = ExcelStyles.createDataStyle(wb, true, true);
        CellStyle summaryPo = ExcelStyles.createSummaryStyle(wb, false);
        CellStyle summaryPso = ExcelStyles.createSummaryStyle(wb, true);

        List<String> poCodes = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psoCodes = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();
        int totalCols = 3 + poCodes.size() + psoCodes.size();

        String progScope = "Programme: " + (snapshot.getMasterProgrammeName() != null ? snapshot.getMasterProgrammeName() : "")
                + (snapshot.getMasterProgrammeCode() != null && !snapshot.getMasterProgrammeCode().isBlank() ? " (" + snapshot.getMasterProgrammeCode() + ")" : "");

        int startRow = CommonExcelHeaderRenderer.renderHeader(
                wb, sheet, snapshot.getInstitutionName(), snapshot.getSchoolName(),
                "PROGRAMME ATTAINMENT — AVERAGE MAPPING",
                progScope, snapshot.getAcademicYear(), "All Semesters (Sem 1–8)", snapshot.getReportId(), totalCols, true);

        // Table Header
        Row headerRow = sheet.createRow(startRow);
        headerRow.setHeightInPoints(24);

        int colIdx = 0;
        createCell(headerRow, colIdx++, "Sem", headerStyle);
        createCell(headerRow, colIdx++, "Course Code", headerStyle);
        createCell(headerRow, colIdx++, "Course Name", headerStyle);

        for (String po : poCodes) createCell(headerRow, colIdx++, po, headerStyle);
        for (String pso : psoCodes) createCell(headerRow, colIdx++, pso, psoHeaderStyle);

        // Data Rows
        ProgrammeAttainmentSnapshot.AverageMappingSection section = snapshot.getSection1AverageMapping();
        int rowIdx = startRow + 1;
        if (section != null && section.getCourses() != null) {
            for (ProgrammeAttainmentSnapshot.CourseMappingRow course : section.getCourses()) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(18);
                int cIdx = 0;
                createCell(row, cIdx++, "Sem " + (course.getSemester() != null ? course.getSemester() : "—"), dataCenter);
                createCell(row, cIdx++, course.getCourseCode() != null ? course.getCourseCode() : "", boldCode);
                createCell(row, cIdx++, course.getCourseName() != null ? course.getCourseName() : "", dataLeft);

                Map<String, BigDecimal> poMap = course.getPoValues() != null ? course.getPoValues() : Map.of();
                for (String po : poCodes) {
                    BigDecimal val = poMap.get(po);
                    if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                        Cell c = row.createCell(cIdx++);
                        c.setCellValue(val.doubleValue());
                        c.setCellStyle(dataCenter);
                    } else {
                        createCell(row, cIdx++, "—", dataCenter);
                    }
                }

                Map<String, BigDecimal> psoMap = course.getPsoValues() != null ? course.getPsoValues() : Map.of();
                for (String pso : psoCodes) {
                    BigDecimal val = psoMap.get(pso);
                    if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                        Cell c = row.createCell(cIdx++);
                        c.setCellValue(val.doubleValue());
                        c.setCellStyle(dataCenter);
                    } else {
                        createCell(row, cIdx++, "—", dataCenter);
                    }
                }
            }
        }

        // Summary Row: Average Mapping Strength
        Row sumRow = sheet.createRow(rowIdx);
        sumRow.setHeightInPoints(22);
        createCell(sumRow, 0, "", summaryPo);
        createCell(sumRow, 1, "", summaryPo);
        createCell(sumRow, 2, "Average Mapping Strength", summaryPo);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 2));

        Map<String, BigDecimal> avgMap = (section != null && section.getAverageMappingStrength() != null)
                ? section.getAverageMappingStrength() : Map.of();

        int sumColIdx = 3;
        for (String po : poCodes) {
            BigDecimal val = avgMap.get(po);
            Cell c = sumRow.createCell(sumColIdx++);
            if (val != null) c.setCellValue(val.doubleValue());
            else c.setCellValue("—");
            c.setCellStyle(summaryPo);
        }
        for (String pso : psoCodes) {
            BigDecimal val = avgMap.get(pso);
            Cell c = sumRow.createCell(sumColIdx++);
            if (val != null) c.setCellValue(val.doubleValue());
            else c.setCellValue("—");
            c.setCellStyle(summaryPso);
        }

        sheet.createFreezePane(0, startRow + 1);
        sheet.setRepeatingRows(CellRangeAddress.valueOf("1:" + (startRow + 1)));

        // Column Auto Sizing
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 16 * 256);
        sheet.setColumnWidth(2, 38 * 256);
        for (int i = 3; i < totalCols; i++) {
            sheet.setColumnWidth(i, 11 * 256);
        }

        return sheet;
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }
}
