package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CourseAttainmentSheetBuilder {

    public static Sheet build(Workbook wb, String sheetName, CourseAttainmentSnapshot snapshot) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Course Attainment");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(wb, false);
        CellStyle psoHeaderStyle = ExcelStyles.createHeaderStyle(wb, true);
        CellStyle dataLeft = ExcelStyles.createDataStyle(wb, false, false);
        CellStyle dataCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle boldCenter = ExcelStyles.createDataStyle(wb, true, true);
        CellStyle summaryPo = ExcelStyles.createSummaryStyle(wb, false);
        CellStyle summaryPso = ExcelStyles.createSummaryStyle(wb, true);
        CellStyle subTitleStyle = ExcelStyles.createSubTitleStyle(wb);

        List<String> poCodes = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psoCodes = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();
        int totalCols = Math.max(8, 2 + poCodes.size() + psoCodes.size());

        String courseScope = "Course: " + (snapshot.getCourseName() != null ? snapshot.getCourseName() : "")
                + (snapshot.getCourseCode() != null && !snapshot.getCourseCode().isBlank() ? " (" + snapshot.getCourseCode() + ")" : "");
        String semStr = "Semester " + (snapshot.getSemester() != null ? snapshot.getSemester() : "—");

        int rowIdx = CommonExcelHeaderRenderer.renderHeader(
                wb, sheet, snapshot.getInstitutionName(), snapshot.getSchoolName(),
                "COURSE ATTAINMENT CONSOLIDATED REPORT",
                courseScope, snapshot.getAcademicYear(), semStr, snapshot.getReportId(), totalCols, true);

        // --- SUMMARY PARAMETERS CARD ---
        Row paramRow = sheet.createRow(rowIdx++);
        paramRow.setHeightInPoints(20);
        createCell(paramRow, 0, "Direct Weight: 80%  |  Indirect Weight: 20%  |  Target Threshold: 60%  |  Overall CO Attainment: "
                + (snapshot.getOverallCoAttainment() != null ? snapshot.getOverallCoAttainment().toPlainString() : "—"), subTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, totalCols - 1));

        sheet.createRow(rowIdx++).setHeightInPoints(6); // spacer

        // --- TABLE 1: CO to PO/PSO Mapping ---
        Row t1Title = sheet.createRow(rowIdx++);
        t1Title.setHeightInPoints(20);
        createCell(t1Title, 0, "1. Table 1: Combined Mapping of CO to PO / PSO", subTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, totalCols - 1));

        Row t1Head = sheet.createRow(rowIdx++);
        t1Head.setHeightInPoints(22);
        int cIdx = 0;
        createCell(t1Head, cIdx++, "#", headerStyle);
        createCell(t1Head, cIdx++, "CO Code", headerStyle);
        for (String po : poCodes) createCell(t1Head, cIdx++, po, headerStyle);
        for (String pso : psoCodes) createCell(t1Head, cIdx++, pso, psoHeaderStyle);

        if (snapshot.getTable1Mapping() != null) {
            int idx = 1;
            for (CourseAttainmentSnapshot.CoMappingRow r : snapshot.getTable1Mapping()) {
                Row rRow = sheet.createRow(rowIdx++);
                rRow.setHeightInPoints(18);
                int cc = 0;
                createCell(rRow, cc++, String.valueOf(idx++), dataCenter);
                createCell(rRow, cc++, r.getCoCode(), boldCenter);

                for (String po : poCodes) {
                    Integer v = r.getPoMappings() != null ? r.getPoMappings().get(po) : null;
                    createCell(rRow, cc++, v != null ? String.valueOf(v) : "—", dataCenter);
                }
                for (String pso : psoCodes) {
                    Integer v = r.getPsoMappings() != null ? r.getPsoMappings().get(pso) : null;
                    createCell(rRow, cc++, v != null ? String.valueOf(v) : "—", dataCenter);
                }
            }
        }

        // Table 1 Average Mapping row
        Row t1Avg = sheet.createRow(rowIdx++);
        t1Avg.setHeightInPoints(20);
        createCell(t1Avg, 0, "", summaryPo);
        createCell(t1Avg, 1, "Average Mapping Strength", summaryPo);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 1));

        Map<String, BigDecimal> poAvg = new HashMap<>();
        if (snapshot.getTable2DirectPO() != null) {
            for (CourseAttainmentSnapshot.OutcomeContributionRow r : snapshot.getTable2DirectPO()) poAvg.put(r.getOutcomeCode(), r.getAverageMapping());
        }
        Map<String, BigDecimal> psoAvg = new HashMap<>();
        if (snapshot.getTable2DirectPSO() != null) {
            for (CourseAttainmentSnapshot.OutcomeContributionRow r : snapshot.getTable2DirectPSO()) psoAvg.put(r.getOutcomeCode(), r.getAverageMapping());
        }
        int avgCc = 2;
        for (String po : poCodes) fillNumCell(t1Avg, avgCc++, poAvg.get(po), summaryPo);
        for (String pso : psoCodes) fillNumCell(t1Avg, avgCc++, psoAvg.get(pso), summaryPso);

        sheet.createRow(rowIdx++).setHeightInPoints(8); // spacer

        // --- TABLE 2: PO & PSO Direct Attainment Contribution ---
        Row t2Title = sheet.createRow(rowIdx++);
        t2Title.setHeightInPoints(20);
        createCell(t2Title, 0, "2. Table 2: PO & PSO Attainment Values (Direct Attainment)", subTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, totalCols - 1));

        Row t2Head = sheet.createRow(rowIdx++);
        t2Head.setHeightInPoints(22);
        int c2Idx = 0;
        createCell(t2Head, c2Idx++, "Course Code", headerStyle);
        createCell(t2Head, c2Idx++, "Metric", headerStyle);
        for (String po : poCodes) createCell(t2Head, c2Idx++, po, headerStyle);
        for (String pso : psoCodes) createCell(t2Head, c2Idx++, pso, psoHeaderStyle);

        Map<String, BigDecimal> poCont = new HashMap<>();
        if (snapshot.getTable2DirectPO() != null) {
            for (CourseAttainmentSnapshot.OutcomeContributionRow r : snapshot.getTable2DirectPO()) poCont.put(r.getOutcomeCode(), r.getDirectContribution());
        }
        Map<String, BigDecimal> psoCont = new HashMap<>();
        if (snapshot.getTable2DirectPSO() != null) {
            for (CourseAttainmentSnapshot.OutcomeContributionRow r : snapshot.getTable2DirectPSO()) psoCont.put(r.getOutcomeCode(), r.getDirectContribution());
        }

        Row t2Row1 = sheet.createRow(rowIdx++);
        t2Row1.setHeightInPoints(18);
        createCell(t2Row1, 0, snapshot.getCourseCode(), boldCenter);
        createCell(t2Row1, 1, "Average Mapping Strength", dataLeft);
        int t2c1 = 2;
        for (String po : poCodes) fillNumCell(t2Row1, t2c1++, poAvg.get(po), dataCenter);
        for (String pso : psoCodes) fillNumCell(t2Row1, t2c1++, psoAvg.get(pso), dataCenter);

        Row t2Row2 = sheet.createRow(rowIdx++);
        t2Row2.setHeightInPoints(18);
        createCell(t2Row2, 0, snapshot.getCourseCode(), boldCenter);
        createCell(t2Row2, 1, "Direct Attainment Contribution", boldCenter);
        int t2c2 = 2;
        for (String po : poCodes) fillNumCell(t2Row2, t2c2++, poCont.get(po), boldCenter);
        for (String pso : psoCodes) fillNumCell(t2Row2, t2c2++, psoCont.get(pso), boldCenter);

        sheet.createRow(rowIdx++).setHeightInPoints(8); // spacer

        // --- TABLE 3: CO Attainment Breakdown ---
        Row t3Title = sheet.createRow(rowIdx++);
        t3Title.setHeightInPoints(20);
        createCell(t3Title, 0, "3. Table 3: Course Outcome (CO) Attainment Breakdown", subTitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, totalCols - 1));

        Row t3Head = sheet.createRow(rowIdx++);
        t3Head.setHeightInPoints(22);
        createCell(t3Head, 0, "CO Code", headerStyle);
        createCell(t3Head, 1, "Course Outcome Statement", headerStyle);
        createCell(t3Head, 2, "Target", headerStyle);
        createCell(t3Head, 3, "Direct %", headerStyle);
        createCell(t3Head, 4, "Direct Lvl", headerStyle);
        createCell(t3Head, 5, "Indirect %", headerStyle);
        createCell(t3Head, 6, "Indirect Lvl", headerStyle);
        createCell(t3Head, 7, "Final Level", headerStyle);

        if (snapshot.getTable3CoAttainments() != null) {
            for (CourseAttainmentSnapshot.CoAttainmentRow r : snapshot.getTable3CoAttainments()) {
                Row rRow = sheet.createRow(rowIdx++);
                rRow.setHeightInPoints(18);
                createCell(rRow, 0, r.getCoCode(), boldCenter);
                createCell(rRow, 1, r.getStatement() != null ? r.getStatement() : "", dataLeft);
                fillNumCell(rRow, 2, r.getTargetLevel(), dataCenter);
                fillNumCell(rRow, 3, r.getDirectPercentage(), dataCenter);
                createCell(rRow, 4, r.getDirectLevel() != null ? String.valueOf(r.getDirectLevel()) : "—", dataCenter);
                fillNumCell(rRow, 5, r.getIndirectPercentage(), dataCenter);
                createCell(rRow, 6, r.getIndirectLevel() != null ? String.valueOf(r.getIndirectLevel()) : "—", dataCenter);
                fillNumCell(rRow, 7, r.getFinalAttainment(), boldCenter);
            }
        }

        // Auto Sizing
        sheet.setColumnWidth(0, 10 * 256);
        sheet.setColumnWidth(1, 40 * 256);
        for (int i = 2; i < totalCols; i++) {
            sheet.setColumnWidth(i, 13 * 256);
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
