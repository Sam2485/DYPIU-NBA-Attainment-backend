package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Authoritative Excel Builder for Course Attainment Sheet #5: "Course End Survey".
 * <p>
 * Strictly replicates the visual structure, layout, styles, row heights, column widths,
 * borders, fonts, fills, and spacing of the reference Course End Survey sheet
 * (Template-CO-PO-PSO-Attainment-TH-v2-Sample.xlsx / survey.xlsx).
 * <p>
 * Structure:
 * - Dynamic columns: Col A (10.25), Col B (6.25), Cols C..endCol (12.75).
 * - Total columns = 2 + numCO.
 * - Invariant: HEADER LAST COLUMN == CONTENT LAST COLUMN.
 * - Standard Course Header with report title:
 *   "Course Outcome attainment Calculations through Indirect Method"
 * - Summary Table 1 (Count of each level): Rows 6-9 (0-indexed 5-8)
 * - Spacer Row 10 (0-indexed 9)
 * - Summary Table 2 (% of students): Rows 11-13 (0-indexed 10-12)
 * - Spacer Row 14 (0-indexed 13)
 * - Overall Indirect % Row: Row 15 (0-indexed 14)
 * - Spacer Rows 16 & 17 (0-indexed 15 & 16)
 * - Student/Response Table Header: Rows 18 & 19 (0-indexed 17 & 18)
 * - Student/Response Data Rows: Rows 20+ (0-indexed 19+)
 */
public class CourseEndSurveySheetBuilder {

    // Reference authoritative colors
    public static final Color COLOR_PALE_CYAN = new Color(211, 233, 239);    // #D3E9EF (theme 8, tint 0.8)
    public static final Color COLOR_BRIGHT_BLUE = new Color(0, 176, 240);    // #00B0F0 (bright electric cyan)
    public static final Color COLOR_SOFT_BLUE = new Color(232, 239, 245);    // #E8EFF5 (theme 4, tint 0.6)
    public static final Color COLOR_GRAY_FILL = new Color(216, 216, 216);    // #D8D8D8 (feedback rating fill)
    public static final Color COLOR_DOUBLE_BOTTOM_TINT = new Color(242, 242, 242); // #F2F2F2

    public static Sheet build(Workbook wb, String sheetName, CourseAttainmentSnapshot snapshot) {
        return build(wb, sheetName, snapshot, null, null);
    }

    public static Sheet build(Workbook wb, String sheetName, CourseAttainmentSnapshot snapshot, byte[] leftLogo, byte[] rightLogo) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Course End Survey");
        sheet.setDisplayGridlines(true);
        sheet.setPrintGridlines(true);

        // 1. Resolve dynamic CO list in ascending order with acronyms
        CourseOutcomeOrderHelper.CourseOutcomeRegistry registry = CourseOutcomeOrderHelper.resolveRegistry(snapshot);
        List<CourseOutcomeOrderHelper.CourseOutcomeItem> coItems = registry.getItems();
        int numCO = coItems.size();

        // 2. Dynamic column width (HEADER LAST COLUMN == CONTENT LAST COLUMN)
        int totalCols = 2 + numCO;
        if (totalCols < 4) {
            totalCols = 4;
        }
        int endCol = totalCols - 1;

        // 3. Set column widths matching reference (+10% increased)
        sheet.setColumnWidth(0, (int) (11.275 * 256)); // Col A: Sr No / Labels
        sheet.setColumnWidth(1, (int) (6.875 * 256));  // Col B: Level / No. of students
        for (int c = 2; c <= endCol; c++) {
            sheet.setColumnWidth(c, (int) (14.025 * 256)); // Cols C..endCol: CO columns
        }

        // 4. Create CellStyles bundle
        StyleBundle s = createStyles(wb);

        // 5. Render Course Header (Rows 0-4 in 0-indexed, with bottom spacer row)
        // Returns row index 5 (Excel Row 6)
        int rowIdx = CourseExcelHeaderRenderer.renderHeader(
                wb, sheet,
                snapshot != null ? snapshot.getInstitutionName() : null,
                snapshot != null ? snapshot.getSchoolName() : null,
                "Course Outcome attainment Calculations through Indirect Method",
                totalCols,
                leftLogo,
                rightLogo,
                true,
                true);

        // 6. Extract / compute survey data
        CourseAttainmentSnapshot.SurveySection surveyData = snapshot != null ? snapshot.getSurveyData() : null;
        List<CourseAttainmentSnapshot.SurveyResponseRow> responses = (surveyData != null && surveyData.getResponses() != null)
                ? surveyData.getResponses()
                : Collections.emptyList();

        int totalStudents = (surveyData != null && surveyData.getTotalStudents() != null && surveyData.getTotalStudents() > 0)
                ? surveyData.getTotalStudents()
                : responses.size();

        Map<String, Integer> level1Counts = (surveyData != null && surveyData.getLevel1Counts() != null)
                ? new LinkedHashMap<>(surveyData.getLevel1Counts())
                : new LinkedHashMap<>();
        Map<String, Integer> level2Counts = (surveyData != null && surveyData.getLevel2Counts() != null)
                ? new LinkedHashMap<>(surveyData.getLevel2Counts())
                : new LinkedHashMap<>();
        Map<String, Integer> level3Counts = (surveyData != null && surveyData.getLevel3Counts() != null)
                ? new LinkedHashMap<>(surveyData.getLevel3Counts())
                : new LinkedHashMap<>();

        Map<String, BigDecimal> level1Percentages = (surveyData != null && surveyData.getLevel1Percentages() != null)
                ? new LinkedHashMap<>(surveyData.getLevel1Percentages())
                : new LinkedHashMap<>();
        Map<String, BigDecimal> level2Percentages = (surveyData != null && surveyData.getLevel2Percentages() != null)
                ? new LinkedHashMap<>(surveyData.getLevel2Percentages())
                : new LinkedHashMap<>();
        Map<String, BigDecimal> level3Percentages = (surveyData != null && surveyData.getLevel3Percentages() != null)
                ? new LinkedHashMap<>(surveyData.getLevel3Percentages())
                : new LinkedHashMap<>();

        Map<String, BigDecimal> overallIndirectPercentages = (surveyData != null && surveyData.getOverallIndirectPercentages() != null)
                ? new LinkedHashMap<>(surveyData.getOverallIndirectPercentages())
                : new LinkedHashMap<>();

        // If counts or percentages are missing but responses exist, compute defensively
        if (responses.size() > 0 && (level1Counts.isEmpty() || level1Percentages.isEmpty() || overallIndirectPercentages.isEmpty())) {
            for (CourseOutcomeOrderHelper.CourseOutcomeItem item : coItems) {
                int c1 = 0, c2 = 0, c3 = 0;
                for (CourseAttainmentSnapshot.SurveyResponseRow r : responses) {
                    if (r.getCoFeedbacks() != null) {
                        String fb = registry.lookupValue(r.getCoFeedbacks(), item);
                        if (fb != null) {
                            String lower = fb.trim().toLowerCase();
                            if (lower.contains("subst") || "3".equals(lower)) c3++;
                            else if (lower.contains("mod") || "2".equals(lower)) c2++;
                            else if (lower.contains("slight") || "1".equals(lower)) c1++;
                        }
                    }
                }
                level1Counts.putIfAbsent(item.getActualCode(), c1);
                level1Counts.putIfAbsent(item.getAcronym(), c1);
                level2Counts.putIfAbsent(item.getActualCode(), c2);
                level2Counts.putIfAbsent(item.getAcronym(), c2);
                level3Counts.putIfAbsent(item.getActualCode(), c3);
                level3Counts.putIfAbsent(item.getAcronym(), c3);

                int divisor = (c1 + c2 + c3) > 0 ? (c1 + c2 + c3) : (totalStudents > 0 ? totalStudents : 1);
                double p1Raw = (double) c1 * 100.0 / divisor;
                double p2Raw = (double) c2 * 100.0 / divisor;
                double p3Raw = (double) c3 * 100.0 / divisor;

                BigDecimal p1 = BigDecimal.valueOf(p1Raw).setScale(2, RoundingMode.HALF_UP);
                BigDecimal p2 = BigDecimal.valueOf(p2Raw).setScale(2, RoundingMode.HALF_UP);
                BigDecimal p3 = BigDecimal.valueOf(p3Raw).setScale(2, RoundingMode.HALF_UP);

                level1Percentages.putIfAbsent(item.getActualCode(), p1);
                level1Percentages.putIfAbsent(item.getAcronym(), p1);
                level2Percentages.putIfAbsent(item.getActualCode(), p2);
                level2Percentages.putIfAbsent(item.getAcronym(), p2);
                level3Percentages.putIfAbsent(item.getActualCode(), p3);
                level3Percentages.putIfAbsent(item.getAcronym(), p3);

                double indRaw = (p1Raw * 0.33) + (p2Raw * 0.67) + (p3Raw * 1.0);
                BigDecimal ind = BigDecimal.valueOf(indRaw).setScale(2, RoundingMode.HALF_UP);
                overallIndirectPercentages.putIfAbsent(item.getActualCode(), ind);
                overallIndirectPercentages.putIfAbsent(item.getAcronym(), ind);
            }
        }

        // =========================================================================
        // SUMMARY TABLE 1: Response Level Counts (Excel Rows 6 to 9, 0-indexed rows 5 to 8)
        // =========================================================================

        // Row 5 (Excel Row 6): CO Headers (CO1..CO-N)
        Row r6 = getOrCreateRow(sheet, rowIdx++);
        r6.setHeightInPoints(18.0f);
        getOrCreateCell(r6, 0).setCellStyle(s.blankDefault);
        getOrCreateCell(r6, 1).setCellStyle(s.blankDefault);
        for (int i = 0; i < numCO; i++) {
            Cell cell = getOrCreateCell(r6, 2 + i);
            cell.setCellValue(coItems.get(i).getAcronym());
            cell.setCellStyle(s.coHeaderCyan);
        }

        // Rows 6, 7, 8 (Excel Rows 7, 8, 9): Levels 1, 2, 3 counts
        int countStartRow = rowIdx;
        for (int level = 1; level <= 3; level++) {
            Row rCount = getOrCreateRow(sheet, rowIdx++);
            rCount.setHeightInPoints(15.0f);

            Cell cellA = getOrCreateCell(rCount, 0);
            cellA.setCellStyle(s.mergedLabelCyan);

            Cell cellB = getOrCreateCell(rCount, 1);
            cellB.setCellValue(String.valueOf(level));
            cellB.setCellStyle(s.levelNumCell);

            for (int i = 0; i < numCO; i++) {
                CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
                Cell cellVal = getOrCreateCell(rCount, 2 + i);
                int count = 0;
                Integer c = null;
                if (level == 1) c = registry.lookupValue(level1Counts, item);
                else if (level == 2) c = registry.lookupValue(level2Counts, item);
                else c = registry.lookupValue(level3Counts, item);
                if (c != null) count = c;

                cellVal.setCellValue((double) count);
                cellVal.setCellStyle(s.countValueCell);
            }
        }

        // Merge A7:A9 for "Count of each level"
        getOrCreateCell(sheet.getRow(countStartRow), 0).setCellValue("Count of each level");
        CellRangeAddress rangeCountLabel = new CellRangeAddress(countStartRow, countStartRow + 2, 0, 0);
        sheet.addMergedRegion(rangeCountLabel);
        RegionUtil.setBorderTop(BorderStyle.THIN, rangeCountLabel, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, rangeCountLabel, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, rangeCountLabel, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, rangeCountLabel, sheet);

        // Row 9 (Excel Row 10): Blank spacer row
        Row rSpacer10 = getOrCreateRow(sheet, rowIdx++);
        rSpacer10.setHeightInPoints(14.25f);

        // =========================================================================
        // SUMMARY TABLE 2: % of Students (Excel Rows 11 to 13, 0-indexed rows 10 to 12)
        // =========================================================================

        int pctStartRow = rowIdx;
        for (int level = 1; level <= 3; level++) {
            Row rPct = getOrCreateRow(sheet, rowIdx++);
            rPct.setHeightInPoints(15.0f);

            Cell cellA = getOrCreateCell(rPct, 0);
            cellA.setCellStyle(s.mergedLabelCyan);

            Cell cellB = getOrCreateCell(rPct, 1);
            cellB.setCellValue(String.valueOf(level));
            cellB.setCellStyle(s.levelNumCell);

            for (int i = 0; i < numCO; i++) {
                CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
                Cell cellVal = getOrCreateCell(rPct, 2 + i);
                BigDecimal pct = BigDecimal.ZERO;
                BigDecimal p = null;
                if (level == 1) p = registry.lookupValue(level1Percentages, item);
                else if (level == 2) p = registry.lookupValue(level2Percentages, item);
                else p = registry.lookupValue(level3Percentages, item);
                if (p != null) pct = p;

                cellVal.setCellValue(pct != null ? pct.doubleValue() : 0.0);
                cellVal.setCellStyle(s.percentageValueCell);
            }
        }

        // Merge A11:A13 for "% of students"
        getOrCreateCell(sheet.getRow(pctStartRow), 0).setCellValue("% of students");
        CellRangeAddress rangePctLabel = new CellRangeAddress(pctStartRow, pctStartRow + 2, 0, 0);
        sheet.addMergedRegion(rangePctLabel);
        RegionUtil.setBorderTop(BorderStyle.THIN, rangePctLabel, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, rangePctLabel, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, rangePctLabel, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, rangePctLabel, sheet);

        // Row 13 (Excel Row 14): Blank spacer row
        Row rSpacer14 = getOrCreateRow(sheet, rowIdx++);
        rSpacer14.setHeightInPoints(14.25f);

        // =========================================================================
        // OVERALL INDIRECT % ROW: Excel Row 15 (0-indexed row 14)
        // =========================================================================

        Row r15 = getOrCreateRow(sheet, rowIdx++);
        r15.setHeightInPoints(23.5f);

        Cell cellA15 = getOrCreateCell(r15, 0);
        cellA15.setCellValue("Overall Indirect %");
        cellA15.setCellStyle(s.mergedLabelCyanBold);

        Cell cellB15 = getOrCreateCell(r15, 1);
        cellB15.setCellStyle(s.mergedLabelCyanBold);

        CellRangeAddress rangeOverallLabel = new CellRangeAddress(r15.getRowNum(), r15.getRowNum(), 0, 1);
        sheet.addMergedRegion(rangeOverallLabel);
        RegionUtil.setBorderTop(BorderStyle.THIN, rangeOverallLabel, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, rangeOverallLabel, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, rangeOverallLabel, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, rangeOverallLabel, sheet);

        for (int i = 0; i < numCO; i++) {
            CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
            Cell cellVal = getOrCreateCell(r15, 2 + i);
            BigDecimal indPct = registry.lookupValue(overallIndirectPercentages, item);
            cellVal.setCellValue(indPct != null ? indPct.doubleValue() : 0.0);
            cellVal.setCellStyle(s.overallIndirectValueCell);
        }

        // Rows 15 & 16 (Excel Rows 16 & 17): Two blank spacer rows
        Row rSpacer16 = getOrCreateRow(sheet, rowIdx++);
        rSpacer16.setHeightInPoints(14.25f);
        Row rSpacer17 = getOrCreateRow(sheet, rowIdx++);
        rSpacer17.setHeightInPoints(14.25f);

        // =========================================================================
        // STUDENT / RESPONSE TABLE HEADER (Excel Rows 18 & 19, 0-indexed rows 17 & 18)
        // =========================================================================

        // Row 17 (Excel Row 18, ht = 21.0 pt)
        Row r18 = getOrCreateRow(sheet, rowIdx++);
        r18.setHeightInPoints(21.0f);

        Cell cellSrNoH = getOrCreateCell(r18, 0);
        cellSrNoH.setCellValue("Sr No");
        cellSrNoH.setCellStyle(s.srNoHeaderCell);

        Cell cellB18 = getOrCreateCell(r18, 1);
        cellB18.setCellStyle(s.thinBlankCell);

        for (int i = 0; i < numCO; i++) {
            Cell cell = getOrCreateCell(r18, 2 + i);
            cell.setCellValue(coItems.get(i).getAcronym());
            cell.setCellStyle(s.coHeaderBrightBlue11);
        }

        // Row 18 (Excel Row 19, ht = 52.9 pt) - Header with double bottom border
        Row r19 = getOrCreateRow(sheet, rowIdx++);
        r19.setHeightInPoints(52.9f);

        Cell cellNoStudents = getOrCreateCell(r19, 0);
        cellNoStudents.setCellValue("No. of Students");
        cellNoStudents.setCellStyle(s.noOfStudentsLabelCell);

        Cell cellTotalStudents = getOrCreateCell(r19, 1);
        cellTotalStudents.setCellValue(String.valueOf(totalStudents));
        cellTotalStudents.setCellStyle(s.studentCountCell);

        for (int i = 0; i < numCO; i++) {
            Cell cell = getOrCreateCell(r19, 2 + i);
            cell.setCellStyle(s.doubleBottomBlankCell);
        }

        // =========================================================================
        // STUDENT / RESPONSE DATA ROWS (Excel Rows 20+ onwards, 0-indexed rows 19+)
        // =========================================================================

        for (int rIdx = 0; rIdx < responses.size(); rIdx++) {
            CourseAttainmentSnapshot.SurveyResponseRow resp = responses.get(rIdx);
            Row rResp = getOrCreateRow(sheet, rowIdx++);
            rResp.setHeightInPoints(14.25f);

            // Col A: Sr No (1, 2, 3...)
            Cell cellSr = getOrCreateCell(rResp, 0);
            int sr = (resp.getSrNo() != null && resp.getSrNo() > 0) ? resp.getSrNo() : (rIdx + 1);
            cellSr.setCellValue(String.valueOf(sr));
            cellSr.setCellStyle(s.responseSrNoCell);

            // Col B: blank with thin border
            Cell cellB = getOrCreateCell(rResp, 1);
            cellB.setCellStyle(s.thinBlankCell);

            // Cols C..endCol: feedback rating text ("Slight", "Moderate", "Substantial")
            for (int i = 0; i < numCO; i++) {
                CourseOutcomeOrderHelper.CourseOutcomeItem item = coItems.get(i);
                Cell cellFb = getOrCreateCell(rResp, 2 + i);
                String feedback = "";
                if (resp.getCoFeedbacks() != null) {
                    feedback = registry.lookupValue(resp.getCoFeedbacks(), item);
                }
                if (feedback != null && !feedback.isBlank()) {
                    String trimmed = feedback.trim();
                    if ("1".equals(trimmed) || "1.0".equals(trimmed)) feedback = "Slight";
                    else if ("2".equals(trimmed) || "2.0".equals(trimmed)) feedback = "Moderate";
                    else if ("3".equals(trimmed) || "3.0".equals(trimmed)) feedback = "Substantial";
                }
                cellFb.setCellValue(feedback != null ? feedback : "");
                cellFb.setCellStyle(s.responseFeedbackCell);
            }
        }

        return sheet;
    }

    // =========================================================================
    // CO CODES RESOLUTION
    // =========================================================================

    private static List<String> extractCoCodes(CourseAttainmentSnapshot snapshot) {
        return CourseOutcomeOrderHelper.resolveRegistry(snapshot).getAcronyms();
    }

    // =========================================================================
    // STYLES CREATION
    // =========================================================================

    private static class StyleBundle {
        CellStyle blankDefault;
        CellStyle coHeaderCyan;
        CellStyle mergedLabelCyan;
        CellStyle mergedLabelCyanBold;
        CellStyle levelNumCell;
        CellStyle countValueCell;
        CellStyle percentageValueCell;
        CellStyle overallIndirectValueCell;
        CellStyle srNoHeaderCell;
        CellStyle thinBlankCell;
        CellStyle coHeaderBrightBlue11;
        CellStyle noOfStudentsLabelCell;
        CellStyle studentCountCell;
        CellStyle doubleBottomBlankCell;
        CellStyle responseSrNoCell;
        CellStyle responseFeedbackCell;
    }

    private static StyleBundle createStyles(Workbook wb) {
        StyleBundle s = new StyleBundle();
        DataFormat df = wb.createDataFormat();

        s.blankDefault = wb.createCellStyle();

        // 1. CO Header in summary table: Verdana 10pt Bold, Pale Cyan, thin border, center
        s.coHeaderCyan = createStyle(wb, "Verdana", (short) 10, true,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_PALE_CYAN, BorderStyle.THIN, BorderStyle.THIN);

        // 2. Merged label "Count of each level", "% of students": Verdana 10pt Regular, Pale Cyan, wrapText, thin border, center
        s.mergedLabelCyan = createStyle(wb, "Verdana", (short) 10, false,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_PALE_CYAN, BorderStyle.THIN, BorderStyle.THIN);

        // 3. Merged label "Overall Indirect %": Verdana 10pt Bold, Pale Cyan, wrapText, thin border, center
        s.mergedLabelCyanBold = createStyle(wb, "Verdana", (short) 10, true,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_PALE_CYAN, BorderStyle.THIN, BorderStyle.THIN);

        // 4. Level numbers 1, 2, 3: Verdana 10pt Regular, thin border, center
        s.levelNumCell = createStyle(wb, "Verdana", (short) 10, false,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN);

        // 5. Counts: Verdana 10pt Regular, thin border, center, integer format
        s.countValueCell = createStyle(wb, "Verdana", (short) 10, false,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN);
        s.countValueCell.setDataFormat(df.getFormat("0"));

        // 6. Percentages: Verdana 10pt Regular, thin border, center, 0.00 format
        s.percentageValueCell = createStyle(wb, "Verdana", (short) 10, false,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN);
        s.percentageValueCell.setDataFormat(df.getFormat("0.00"));

        // 7. Overall Indirect % values: Verdana 11pt Bold, Bright Blue (#00B0F0), thin border, center, 0.00 format
        s.overallIndirectValueCell = createStyle(wb, "Verdana", (short) 11, true,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BRIGHT_BLUE, BorderStyle.THIN, BorderStyle.THIN);
        s.overallIndirectValueCell.setDataFormat(df.getFormat("0.00"));

        // 8. "Sr No" header: Verdana 10pt Bold, thin border, center
        s.srNoHeaderCell = createStyle(wb, "Verdana", (short) 10, true,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN);

        // 9. Blank cell with thin border
        s.thinBlankCell = createStyle(wb, "Verdana", (short) 10, false,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN);

        // 10. CO headers above student responses: Verdana 11pt Bold, Bright Blue (#00B0F0), thin border, center
        s.coHeaderBrightBlue11 = createStyle(wb, "Verdana", (short) 11, true,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BRIGHT_BLUE, BorderStyle.THIN, BorderStyle.THIN);

        // 11. "No. of Students" header cell: Verdana 10pt Bold, Soft Blue (#E8EFF5), double bottom border, center, wrapText
        s.noOfStudentsLabelCell = createStyle(wb, "Verdana", (short) 10, true,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_SOFT_BLUE, BorderStyle.THIN, BorderStyle.DOUBLE);

        // 12. Student count cell: Verdana 10pt Bold, double bottom border, center
        s.studentCountCell = createStyle(wb, "Verdana", (short) 10, true,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.DOUBLE);

        // 13. Blank cell with double bottom border
        s.doubleBottomBlankCell = createStyle(wb, "Verdana", (short) 10, false,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_DOUBLE_BOTTOM_TINT, BorderStyle.THIN, BorderStyle.DOUBLE);

        // 14. Response Sr No cell: Verdana 10pt Regular, thin border, center
        s.responseSrNoCell = createStyle(wb, "Verdana", (short) 10, false,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN);

        // 15. Response feedback cell: Verdana 8pt Regular, Gray (#D8D8D8), thin border, center
        s.responseFeedbackCell = createStyle(wb, "Verdana", (short) 8, false,
                HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_GRAY_FILL, BorderStyle.THIN, BorderStyle.THIN);

        return s;
    }

    private static CellStyle createStyle(
            Workbook wb,
            String fontName,
            short fontSize,
            boolean bold,
            HorizontalAlignment hAlign,
            VerticalAlignment vAlign,
            boolean wrapText,
            Color fillColor,
            BorderStyle sideBorder,
            BorderStyle bottomBorder) {

        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName(fontName);
        font.setFontHeightInPoints(fontSize);
        font.setBold(bold);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);

        style.setAlignment(hAlign);
        style.setVerticalAlignment(vAlign);
        style.setWrapText(wrapText);

        if (fillColor != null) {
            if (wb instanceof org.apache.poi.xssf.usermodel.XSSFWorkbook) {
                ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(fillColor, null));
            } else {
                style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            }
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        style.setBorderTop(sideBorder != null ? sideBorder : BorderStyle.NONE);
        style.setBorderLeft(sideBorder != null ? sideBorder : BorderStyle.NONE);
        style.setBorderRight(sideBorder != null ? sideBorder : BorderStyle.NONE);
        style.setBorderBottom(bottomBorder != null ? bottomBorder : (sideBorder != null ? sideBorder : BorderStyle.NONE));

        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());

        return style;
    }

    private static Row getOrCreateRow(Sheet sheet, int rowNum) {
        Row row = sheet.getRow(rowNum);
        return row != null ? row : sheet.createRow(rowNum);
    }

    private static Cell getOrCreateCell(Row row, int colNum) {
        Cell cell = row.getCell(colNum);
        return cell != null ? cell : row.createCell(colNum);
    }
}
