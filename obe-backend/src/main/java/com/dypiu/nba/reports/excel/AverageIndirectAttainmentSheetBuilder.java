package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AverageIndirectAttainmentSheetBuilder {

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Average Indirect Attainment");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(wb, false);
        CellStyle psoHeaderStyle = ExcelStyles.createHeaderStyle(wb, true);
        CellStyle dataLeft = ExcelStyles.createDataStyle(wb, false, false);
        CellStyle dataCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle boldPrn = ExcelStyles.createDataStyle(wb, true, true);
        CellStyle summaryPo = ExcelStyles.createSummaryStyle(wb, false);
        CellStyle summaryPso = ExcelStyles.createSummaryStyle(wb, true);

        List<String> poCodes = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psoCodes = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();
        int totalCols = 3 + poCodes.size() + psoCodes.size();

        String progScope = "Programme: " + (snapshot.getMasterProgrammeName() != null ? snapshot.getMasterProgrammeName() : "")
                + (snapshot.getMasterProgrammeCode() != null && !snapshot.getMasterProgrammeCode().isBlank() ? " (" + snapshot.getMasterProgrammeCode() + ")" : "");

        int startRow = CommonExcelHeaderRenderer.renderHeader(
                wb, sheet, snapshot.getInstitutionName(), snapshot.getSchoolName(),
                "PROGRAMME ATTAINMENT — AVERAGE INDIRECT ATTAINMENT (EXIT SURVEY)",
                progScope, snapshot.getAcademicYear(), "Graduate Exit Survey", snapshot.getReportId(), totalCols, true);

        // Table Header
        Row headerRow = sheet.createRow(startRow);
        headerRow.setHeightInPoints(24);

        int colIdx = 0;
        createCell(headerRow, colIdx++, "Sr No", headerStyle);
        createCell(headerRow, colIdx++, "PRN", headerStyle);
        createCell(headerRow, colIdx++, "Name of the Student", headerStyle);

        for (String po : poCodes) createCell(headerRow, colIdx++, po, headerStyle);
        for (String pso : psoCodes) createCell(headerRow, colIdx++, pso, psoHeaderStyle);

        // Data Rows
        ProgrammeAttainmentSnapshot.AverageIndirectSection section = snapshot.getSection3AverageIndirect();
        int rowIdx = startRow + 1;
        List<ProgrammeAttainmentSnapshot.StudentSurveyRow> responses = (section != null) ? section.getStudentResponses() : null;

        if (responses != null && !responses.isEmpty()) {
            for (ProgrammeAttainmentSnapshot.StudentSurveyRow row : responses) {
                Row r = sheet.createRow(rowIdx++);
                r.setHeightInPoints(18);
                int cIdx = 0;
                createCell(r, cIdx++, String.valueOf(row.getSrNo() != null ? row.getSrNo() : (rowIdx - startRow)), dataCenter);
                createCell(r, cIdx++, row.getPrn() != null ? row.getPrn() : "", boldPrn);
                createCell(r, cIdx++, row.getStudentName() != null ? row.getStudentName() : "", dataLeft);

                Map<String, BigDecimal> poMap = row.getPoRatings() != null ? row.getPoRatings() : Map.of();
                for (String po : poCodes) {
                    BigDecimal val = poMap.get(po);
                    if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                        Cell c = r.createCell(cIdx++);
                        c.setCellValue(val.doubleValue());
                        c.setCellStyle(dataCenter);
                    } else {
                        createCell(r, cIdx++, "—", dataCenter);
                    }
                }

                Map<String, BigDecimal> psoMap = row.getPsoRatings() != null ? row.getPsoRatings() : Map.of();
                for (String pso : psoCodes) {
                    BigDecimal val = psoMap.get(pso);
                    if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                        Cell c = r.createCell(cIdx++);
                        c.setCellValue(val.doubleValue());
                        c.setCellStyle(dataCenter);
                    } else {
                        createCell(r, cIdx++, "—", dataCenter);
                    }
                }
            }
        } else {
            Row r = sheet.createRow(rowIdx++);
            r.setHeightInPoints(22);
            Cell emptyCell = r.createCell(0);
            emptyCell.setCellValue("No student exit survey responses recorded for this batch.");
            emptyCell.setCellStyle(dataCenter);
            sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, totalCols - 1));
        }

        // Summary Row: Average Indirect Attainment
        Row sumRow = sheet.createRow(rowIdx);
        sumRow.setHeightInPoints(22);
        createCell(sumRow, 0, "", summaryPo);
        createCell(sumRow, 1, "", summaryPo);
        createCell(sumRow, 2, "Average Attainment (Indirect)", summaryPo);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, 0, 2));

        Map<String, BigDecimal> avgMap = (section != null && section.getAverageIndirectAttainment() != null)
                ? section.getAverageIndirectAttainment() : Map.of();

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
        sheet.setColumnWidth(1, 18 * 256);
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
