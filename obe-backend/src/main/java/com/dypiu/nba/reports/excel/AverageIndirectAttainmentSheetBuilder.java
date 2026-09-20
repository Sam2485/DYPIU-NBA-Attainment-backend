package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

import java.math.BigDecimal;
import java.util.*;

public class AverageIndirectAttainmentSheetBuilder {

    public static final String DEFAULT_SHEET_NAME = "AVERAGE ATTAINMENT (ID)";

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot) {
        return build(wb, sheetName, snapshot, null, null);
    }

    public static Sheet build(Workbook wb, ProgrammeAttainmentSnapshot snapshot) {
        return build(wb, DEFAULT_SHEET_NAME, snapshot, null, null);
    }

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot, byte[] logoBytes, ReportTemplateDto template) {
        String resolvedSheetName = (sheetName != null && !sheetName.isBlank()) ? sheetName : DEFAULT_SHEET_NAME;
        Sheet sheet = wb.createSheet(resolvedSheetName);

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(wb, false);
        CellStyle psoHeaderStyle = ExcelStyles.createHeaderStyle(wb, true);
        CellStyle dataLeft = ExcelStyles.createDataStyle(wb, false, false);
        CellStyle dataCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle boldPrn = ExcelStyles.createDataStyle(wb, true, true);
        CellStyle summaryPo = ExcelStyles.createSummaryStyle(wb, false);
        CellStyle summaryPso = ExcelStyles.createSummaryStyle(wb, true);
        CellStyle summaryTitle = ExcelStyles.createSummaryTitleStyle(wb);
        CellStyle sectionHeaderStyle = ExcelStyles.createSectionHeaderStyle(wb);

        List<String> poCodes = new ArrayList<>(snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of());
        poCodes.sort(ExcelStyles.NATURAL_NUMERICAL_COMPARATOR);
        List<String> psoCodes = new ArrayList<>(snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of());
        psoCodes.sort(ExcelStyles.NATURAL_NUMERICAL_COMPARATOR);
        int totalCols = 3 + poCodes.size() + psoCodes.size();

        int startRow = CommonExcelHeaderRenderer.renderProgrammeHeader(
                wb, sheet, snapshot, "PO & PSO Attainment (Indirect)", totalCols, logoBytes, template, "Term – I & II", true);

        // ==========================================
        // SECTION A: PROGRAMME END SURVEY (Student-level evidence)
        // ==========================================
        Row headerRow = sheet.createRow(startRow);
        headerRow.setHeightInPoints(20.0f);

        int colIdx = 0;
        createCell(headerRow, colIdx++, "Sr No", headerStyle);
        createCell(headerRow, colIdx++, "PRN", headerStyle);
        createCell(headerRow, colIdx++, "Name of the Student", headerStyle);

        for (String po : poCodes) createCell(headerRow, colIdx++, po, headerStyle);
        for (String pso : psoCodes) createCell(headerRow, colIdx++, pso, psoHeaderStyle);

        ProgrammeAttainmentSnapshot.AverageIndirectSection section = snapshot.getSection3AverageIndirect();
        int rowIdx = startRow + 1;
        List<ProgrammeAttainmentSnapshot.StudentSurveyRow> responses = (section != null) ? section.getStudentResponses() : null;

        if (responses != null && !responses.isEmpty()) {
            for (ProgrammeAttainmentSnapshot.StudentSurveyRow row : responses) {
                Row r = sheet.createRow(rowIdx++);
                r.setHeightInPoints(16.5f);
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
            r.setHeightInPoints(20.0f);
            Cell emptyCell = r.createCell(0);
            emptyCell.setCellValue("No student exit survey responses recorded for this batch.");
            emptyCell.setCellStyle(dataCenter);
            for (int c = 1; c < totalCols; c++) {
                createCell(r, c, "", dataCenter);
            }
            CellRangeAddress emptyRegion = new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, totalCols - 1);
            sheet.addMergedRegion(emptyRegion);
            RegionUtil.setBorderTop(BorderStyle.THIN, emptyRegion, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, emptyRegion, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, emptyRegion, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, emptyRegion, sheet);
        }

        // ==========================================
        // SECTION B: OTHER PROGRAMME INDIRECT ASSESSMENTS (Event / Survey level)
        // ==========================================
        List<ProgrammeAttainmentSnapshot.IndirectAssessmentRow> otherAssessments = (section != null) ? section.getOtherAssessments() : null;
        if (otherAssessments != null && !otherAssessments.isEmpty()) {
            // Spacer row
            Row spacer = sheet.createRow(rowIdx++);
            spacer.setHeightInPoints(12.0f);

            // Section Banner: PROGRAMME INDIRECT ASSESSMENTS
            Row secHeaderRow = sheet.createRow(rowIdx++);
            secHeaderRow.setHeightInPoints(22.0f);
            createCell(secHeaderRow, 0, "PROGRAMME INDIRECT ASSESSMENTS", sectionHeaderStyle);
            for (int c = 1; c < totalCols; c++) {
                createCell(secHeaderRow, c, "", sectionHeaderStyle);
            }
            CellRangeAddress secRegion = new CellRangeAddress(secHeaderRow.getRowNum(), secHeaderRow.getRowNum(), 0, totalCols - 1);
            sheet.addMergedRegion(secRegion);
            RegionUtil.setBorderTop(BorderStyle.THIN, secRegion, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, secRegion, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, secRegion, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, secRegion, sheet);

            // Table Header for Other Indirect Assessments
            Row indHeaderRow = sheet.createRow(rowIdx++);
            indHeaderRow.setHeightInPoints(20.0f);
            createCell(indHeaderRow, 0, "Event Title", headerStyle);
            createCell(indHeaderRow, 1, "", headerStyle);
            CellRangeAddress thTitleRegion = new CellRangeAddress(indHeaderRow.getRowNum(), indHeaderRow.getRowNum(), 0, 1);
            sheet.addMergedRegion(thTitleRegion);
            RegionUtil.setBorderTop(BorderStyle.THIN, thTitleRegion, sheet);
            RegionUtil.setBorderBottom(BorderStyle.THIN, thTitleRegion, sheet);
            RegionUtil.setBorderLeft(BorderStyle.THIN, thTitleRegion, sheet);
            RegionUtil.setBorderRight(BorderStyle.THIN, thTitleRegion, sheet);

            createCell(indHeaderRow, 2, "Assessment Type", headerStyle);
            int hCol = 3;
            for (String po : poCodes) createCell(indHeaderRow, hCol++, po, headerStyle);
            for (String pso : psoCodes) createCell(indHeaderRow, hCol++, pso, psoHeaderStyle);

            // Data Rows for each indirect assessment
            for (ProgrammeAttainmentSnapshot.IndirectAssessmentRow assessment : otherAssessments) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(16.5f);

                createCell(row, 0, assessment.getEventTitle() != null ? assessment.getEventTitle() : "", dataLeft);
                createCell(row, 1, "", dataLeft);
                CellRangeAddress eventRegion = new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, 1);
                sheet.addMergedRegion(eventRegion);
                RegionUtil.setBorderTop(BorderStyle.THIN, eventRegion, sheet);
                RegionUtil.setBorderBottom(BorderStyle.THIN, eventRegion, sheet);
                RegionUtil.setBorderLeft(BorderStyle.THIN, eventRegion, sheet);
                RegionUtil.setBorderRight(BorderStyle.THIN, eventRegion, sheet);

                createCell(row, 2, assessment.getAssessmentType() != null ? assessment.getAssessmentType() : "", dataCenter);

                int cIdx = 3;
                for (String po : poCodes) {
                    BigDecimal val = assessment.getValue(po);
                    if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                        Cell c = row.createCell(cIdx++);
                        c.setCellValue(val.doubleValue());
                        c.setCellStyle(dataCenter);
                    } else {
                        createCell(row, cIdx++, "—", dataCenter);
                    }
                }

                for (String pso : psoCodes) {
                    BigDecimal val = assessment.getValue(pso);
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

        // ==========================================
        // SECTION C: SUMMARY ROW: Average Attainment (Indirect)
        // ==========================================
        Row sumRow = sheet.createRow(rowIdx);
        sumRow.setHeightInPoints(22.0f);
        createCell(sumRow, 0, "Average Attainment (Indirect)", summaryTitle);
        createCell(sumRow, 1, "", summaryTitle);
        createCell(sumRow, 2, "", summaryTitle);
        CellRangeAddress sumRegion = new CellRangeAddress(rowIdx, rowIdx, 0, 2);
        sheet.addMergedRegion(sumRegion);
        RegionUtil.setBorderTop(BorderStyle.THIN, sumRegion, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, sumRegion, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, sumRegion, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, sumRegion, sheet);

        Map<String, BigDecimal> avgMap = (section != null && section.getAverageIndirectAttainment() != null)
                ? section.getAverageIndirectAttainment() : Map.of();

        int sumColIdx = 3;
        for (String po : poCodes) {
            BigDecimal val = avgMap.get(po);
            Cell c = sumRow.createCell(sumColIdx++);
            if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                c.setCellValue(val.doubleValue());
            } else {
                c.setCellValue("—");
            }
            c.setCellStyle(summaryPo);
        }
        for (String pso : psoCodes) {
            BigDecimal val = avgMap.get(pso);
            Cell c = sumRow.createCell(sumColIdx++);
            if (val != null && val.compareTo(BigDecimal.ZERO) > 0) {
                c.setCellValue(val.doubleValue());
            } else {
                c.setCellValue("—");
            }
            c.setCellStyle(summaryPso);
        }

        // Freeze Pane & Repeating Rows
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
        wb.setPrintArea(wb.getSheetIndex(sheet), 0, totalCols - 1, 0, rowIdx);

        // Column Proportional Widths matching reference
        sheet.setColumnWidth(0, (int) (12.0 * 256));
        sheet.setColumnWidth(1, (int) (18.0 * 256));
        sheet.setColumnWidth(2, (int) (38.0 * 256));
        for (int i = 3; i < totalCols; i++) {
            sheet.setColumnWidth(i, (int) (7.50 * 256));
        }

        return sheet;
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }
}
