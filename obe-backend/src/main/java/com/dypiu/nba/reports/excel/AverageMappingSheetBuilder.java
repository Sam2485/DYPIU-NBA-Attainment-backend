package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

import java.math.BigDecimal;
import java.util.*;

public class AverageMappingSheetBuilder {

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot) {
        return build(wb, sheetName, snapshot, null, null, null);
    }

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot, byte[] logoBytes, ReportTemplateDto template) {
        return build(wb, sheetName, snapshot, logoBytes, null, template);
    }

    public static Sheet build(Workbook wb, String sheetName, ProgrammeAttainmentSnapshot snapshot, byte[] leftLogo, byte[] rightLogo, ReportTemplateDto template) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Average Mapping");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(wb, false);
        CellStyle psoHeaderStyle = ExcelStyles.createHeaderStyle(wb, true);
        CellStyle dataLeft = ExcelStyles.createDataStyle(wb, false, false);
        CellStyle dataCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle codeCenter = ExcelStyles.createDataStyle(wb, true, false);
        CellStyle summaryPo = ExcelStyles.createSummaryStyle(wb, false);
        CellStyle summaryPso = ExcelStyles.createSummaryStyle(wb, true);
        CellStyle summaryTitle = ExcelStyles.createSummaryTitleStyle(wb);

        List<String> poCodes = new ArrayList<>(snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of());
        poCodes.sort(ExcelStyles.NATURAL_NUMERICAL_COMPARATOR);
        List<String> psoCodes = new ArrayList<>(snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of());
        psoCodes.sort(ExcelStyles.NATURAL_NUMERICAL_COMPARATOR);
        int totalCols = 3 + poCodes.size() + psoCodes.size();

        // Set column widths before header rendering so physical width geometry is accurate
        sheet.setColumnWidth(0, (int) (15.82 * 256));
        sheet.setColumnWidth(1, (int) (14.18 * 256));
        sheet.setColumnWidth(2, (int) (41.00 * 256));
        for (int i = 3; i < totalCols; i++) {
            sheet.setColumnWidth(i, (int) (7.50 * 256));
        }

        int startRow = CommonExcelHeaderRenderer.renderProgrammeHeader(
                wb, sheet, snapshot, "Average Mapping Strength", totalCols, leftLogo, rightLogo, template, "Term – I & II", true);

        // Table Header (Excel Row 9, startRow index 8)
        Row headerRow = sheet.createRow(startRow);
        headerRow.setHeightInPoints(20.0f);

        int colIdx = 0;
        createCell(headerRow, colIdx++, "Sem", headerStyle);
        createCell(headerRow, colIdx++, "Course Code", headerStyle);
        createCell(headerRow, colIdx++, "Course Name", headerStyle);

        for (String po : poCodes) createCell(headerRow, colIdx++, po, headerStyle);
        for (String pso : psoCodes) createCell(headerRow, colIdx++, pso, psoHeaderStyle);

        // Data Rows grouped by Semester
        ProgrammeAttainmentSnapshot.AverageMappingSection section = snapshot.getSection1AverageMapping();
        int rowIdx = startRow + 1;

        if (section != null && section.getCourses() != null && !section.getCourses().isEmpty()) {
            // Group courses by semester preserving natural semester order
            Map<Integer, List<ProgrammeAttainmentSnapshot.CourseMappingRow>> coursesBySem = new LinkedHashMap<>();
            for (ProgrammeAttainmentSnapshot.CourseMappingRow course : section.getCourses()) {
                Integer sem = course.getSemester();
                coursesBySem.computeIfAbsent(sem, k -> new ArrayList<>()).add(course);
            }

            // Sort semesters ascending, with nulls last
            List<Integer> sortedSemesters = new ArrayList<>(coursesBySem.keySet());
            sortedSemesters.sort(Comparator.nullsLast(Integer::compareTo));

            for (Integer sem : sortedSemesters) {
                List<ProgrammeAttainmentSnapshot.CourseMappingRow> semCourses = coursesBySem.get(sem);
                int semStartRow = rowIdx;
                int semEndRow = rowIdx + semCourses.size() - 1;
                String semLabel = formatSemesterLabel(sem);

                for (ProgrammeAttainmentSnapshot.CourseMappingRow course : semCourses) {
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(16.5f);

                    int cIdx = 0;
                    Cell semCell = row.createCell(cIdx++);
                    if (row.getRowNum() == semStartRow) {
                        semCell.setCellValue(semLabel);
                    }
                    semCell.setCellStyle(dataCenter);

                    createCell(row, cIdx++, course.getCourseCode() != null ? course.getCourseCode() : "", codeCenter);
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

                // Merge semester cell vertically across course rows if more than 1 row
                if (semEndRow > semStartRow) {
                    CellRangeAddress semRegion = new CellRangeAddress(semStartRow, semEndRow, 0, 0);
                    sheet.addMergedRegion(semRegion);
                    RegionUtil.setBorderTop(BorderStyle.THIN, semRegion, sheet);
                    RegionUtil.setBorderBottom(BorderStyle.THIN, semRegion, sheet);
                    RegionUtil.setBorderLeft(BorderStyle.THIN, semRegion, sheet);
                    RegionUtil.setBorderRight(BorderStyle.THIN, semRegion, sheet);
                }
            }
        }

        // Summary Row: Average Mapping Strength
        Row sumRow = sheet.createRow(rowIdx);
        sumRow.setHeightInPoints(22.0f);
        createCell(sumRow, 0, "Average Mapping Strength", summaryTitle);
        createCell(sumRow, 1, "", summaryTitle);
        createCell(sumRow, 2, "", summaryTitle);
        CellRangeAddress sumRegion = new CellRangeAddress(rowIdx, rowIdx, 0, 2);
        sheet.addMergedRegion(sumRegion);
        RegionUtil.setBorderTop(BorderStyle.THIN, sumRegion, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, sumRegion, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, sumRegion, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, sumRegion, sheet);

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
        sheet.setColumnWidth(0, (int) (15.82 * 256));
        sheet.setColumnWidth(1, (int) (14.18 * 256));
        sheet.setColumnWidth(2, (int) (41.00 * 256));
        for (int i = 3; i < totalCols; i++) {
            sheet.setColumnWidth(i, (int) (7.50 * 256));
        }

        return sheet;
    }

    public static String formatSemesterLabel(Integer semester) {
        if (semester == null || semester <= 0) {
            return "—";
        }
        return switch (semester) {
            case 1 -> "FE Sem - I";
            case 2 -> "FE Sem - II";
            case 3 -> "SE Sem-III";
            case 4 -> "SE Sem-IV";
            case 5 -> "TE Sem-V";
            case 6 -> "TE Sem-VI";
            case 7 -> "BE Sem-VII";
            case 8 -> "BE Sem-VIII";
            default -> "Sem - " + toRoman(semester);
        };
    }

    private static String toRoman(int n) {
        if (n <= 0) return String.valueOf(n);
        String[] romans = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI"};
        return n < romans.length ? romans[n] : String.valueOf(n);
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        c.setCellStyle(style);
    }
}
