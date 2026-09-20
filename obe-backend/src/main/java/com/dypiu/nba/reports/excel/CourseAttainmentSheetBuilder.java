package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.*;

/**
 * Authoritative Excel Builder for Course Attainment Sheet #1: "Attainment-main".
 * <p>
 * Strictly replicates the visual structure, layout, styles, row heights, column widths,
 * borders, fonts, fills, and spacing of Template-CO-PO-PSO-Attainment-TH-v2-Sample.xlsx.
 * <p>
 * Major Sections:
 * - Header (Rows 0-4): University/School names, Logo slots, Cyan Title Band (via CourseExcelHeaderRenderer)
 * - Section A (Rows 5-9): Course Information (Subject Name, Academic Year, Semester, Subject Code, Faculty Name)
 * - Section B (Rows 11-13+numCO): Course Outcome (Sr No, Code, Statement)
 * - Section C (Table 1): Mapping of CO to PO/PSO (dynamic PO1..PO-N, PSO1..PSO-M, Average Mapping)
 * - Section D: Overall CO Attainment Card ("Get from below reference work")
 * - Section E (Table 2): PO Attainment Values (Direct Attainment contribution)
 * - Section F: Reference Work (Examination Direct %, Survey Indirect %, Combined CO Attainments, Overall CO Attainment)
 * <p>
 * Strictly maintains: HEADER LAST COLUMN == CONTENT LAST COLUMN.
 */
public class CourseAttainmentSheetBuilder {

    // Authoritative reference colors
    public static final Color COLOR_BANNER_CYAN = new Color(218, 238, 243);    // #DAEEF3 (accent5, tint 0.8)
    public static final Color COLOR_ACCENT_BLUE = new Color(146, 205, 220);    // #92CDDC (accent5, tint 0.4)
    public static final Color COLOR_BRIGHT_BLUE = new Color(0, 176, 240);      // #00B0F0 (bright electric blue)
    public static final Color COLOR_LIGHT_GRAY = new Color(242, 242, 242);     // #F2F2F2 (background light gray)
    public static final Color COLOR_CODE_GRAY = new Color(217, 217, 217);      // #D9D9D9 (darker gray for code)
    public static final Color COLOR_SOFT_PINK = new Color(242, 219, 219);      // #F2DBDB (soft pink/rose)
    public static final Color COLOR_LAVENDER = new Color(229, 223, 236);       // #E5DFEC (soft lavender purple)

    public static Sheet build(Workbook wb, String sheetName, CourseAttainmentSnapshot snapshot) {
        return build(wb, sheetName, snapshot, null, null);
    }

    public static Sheet build(Workbook wb, String sheetName, CourseAttainmentSnapshot snapshot, byte[] leftLogo, byte[] rightLogo) {
        Sheet sheet = wb.createSheet(sheetName != null ? sheetName : "Attainment-main");

        // 1. Resolve dynamic data collections
        List<String> poCodes = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psoCodes = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();

        List<CourseAttainmentSnapshot.CoAttainmentRow> coRows = snapshot.getTable3CoAttainments();
        if ((coRows == null || coRows.isEmpty()) && snapshot.getTable1Mapping() != null) {
            coRows = snapshot.getTable1Mapping().stream()
                    .map(m -> CourseAttainmentSnapshot.CoAttainmentRow.builder().coCode(m.getCoCode()).statement("").build())
                    .toList();
        }
        if (coRows == null) coRows = List.of();
        int coCount = coRows.size();

        // 2. Calculate dynamic dimensions (HEADER LAST COLUMN == CONTENT LAST COLUMN)
        int table1Cols = 2 + poCodes.size() + psoCodes.size();
        int refWorkMinCols = 3 + coCount;
        int totalCols = Math.max(table1Cols, refWorkMinCols);
        if (totalCols < 4) totalCols = 4;
        int endCol = totalCols - 1;

        int lastCoCol = 2 + coCount; // Col D is 3, so last CO is 2 + coCount
        int refWorkEndCol = Math.min(endCol, lastCoCol + 5);

        // 3. Set authoritative column widths
        sheet.setColumnWidth(0, (int) (8.5 * 256));
        sheet.setColumnWidth(1, (int) (7.832 * 256));
        sheet.setColumnWidth(2, (int) (7.832 * 256));
        for (int c = 3; c < totalCols; c++) {
            sheet.setColumnWidth(c, (int) (6.75 * 256));
        }

        // 4. Create CellStyles matching reference workbook
        StyleBundle s = createStyles(wb);

        // 5. Render Header (Rows 0 - 4)
        int rowIdx = CourseExcelHeaderRenderer.renderHeader(
                wb, sheet,
                snapshot.getInstitutionName(),
                snapshot.getSchoolName(),
                "Programme Outcome attainment through Direct Attainment Method",
                totalCols,
                leftLogo,
                rightLogo,
                true);

        // =========================================================================
        // SECTION A: COURSE INFORMATION (Rows 5 to 9, Excel rows 6 to 10)
        // =========================================================================
        String courseName = snapshot.getCourseName() != null ? snapshot.getCourseName() : "";
        String ay = snapshot.getAcademicYear() != null ? snapshot.getAcademicYear() : "";
        if (!ay.isBlank() && !ay.startsWith("AY")) {
            ay = "AY " + ay;
        }
        String semesterStr = formatSemester(snapshot.getSemester());
        String courseCode = snapshot.getCourseCode() != null ? snapshot.getCourseCode() : "";
        String facultyName = snapshot.getGeneratedBy() != null ? snapshot.getGeneratedBy() : "";

        renderInfoRow(sheet, rowIdx++, "Subject Name : ", courseName, endCol, s.infoLabel, s.infoValue);
        renderInfoRow(sheet, rowIdx++, "Academic Year : ", ay, endCol, s.infoLabel, s.infoValue);
        renderInfoRow(sheet, rowIdx++, "Semester : ", semesterStr, endCol, s.infoLabel, s.infoValue);
        renderInfoRow(sheet, rowIdx++, "Subject Code : ", courseCode, endCol, s.infoLabel, s.infoValue);
        renderInfoRow(sheet, rowIdx++, "Faculty Name:", facultyName, endCol, s.infoLabel, s.infoValue);

        // Spacer row after Course Info (ht = 14.25)
        Row rSpacer1 = getOrCreateRow(sheet, rowIdx++);
        rSpacer1.setHeightInPoints(14.25f);

        // =========================================================================
        // SECTION B: COURSE OUTCOME (Rows 11 to 13+numCO, Excel rows 12 to 19)
        // =========================================================================
        Row rCoBanner = getOrCreateRow(sheet, rowIdx++);
        rCoBanner.setHeightInPoints(13.5f);
        mergeAndApplyStyle(sheet, rowIdx - 1, rowIdx - 1, 0, endCol, s.bannerCyanBold);
        getOrCreateCell(rCoBanner, 0).setCellValue("Course Outcome");

        Row rCoHeader = getOrCreateRow(sheet, rowIdx++);
        rCoHeader.setHeightInPoints(18.0f);
        setCellValAndStyle(rCoHeader, 0, "Sr No", s.tableHeader);
        setCellValAndStyle(rCoHeader, 1, "Code", s.tableHeader);
        mergeAndApplyStyle(sheet, rowIdx - 1, rowIdx - 1, 2, endCol, s.tableHeader);
        getOrCreateCell(rCoHeader, 2).setCellValue("Statement");

        int coSr = 1;
        for (CourseAttainmentSnapshot.CoAttainmentRow co : coRows) {
            Row rCo = getOrCreateRow(sheet, rowIdx++);
            rCo.setHeightInPoints(15.65f);
            setCellValAndStyle(rCo, 0, String.valueOf(coSr++), s.tableDataSrNo);
            setCellValAndStyle(rCo, 1, co.getCoCode() != null ? co.getCoCode() : "", s.coCodeStyle);
            mergeAndApplyStyle(sheet, rowIdx - 1, rowIdx - 1, 2, endCol, s.coStatementStyle);
            getOrCreateCell(rCo, 2).setCellValue(co.getStatement() != null ? co.getStatement() : "");
        }

        // Spacer row after Course Outcome (ht = 14.25)
        Row rSpacer2 = getOrCreateRow(sheet, rowIdx++);
        rSpacer2.setHeightInPoints(14.25f);

        // =========================================================================
        // SECTION C: TABLE 1: MAPPING OF CO TO PO/PSO
        // =========================================================================
        Row rT1Banner = getOrCreateRow(sheet, rowIdx++);
        rT1Banner.setHeightInPoints(13.5f);
        mergeAndApplyStyle(sheet, rowIdx - 1, rowIdx - 1, 0, endCol, s.bannerCyanRegular);
        getOrCreateCell(rT1Banner, 0).setCellValue("Table 1 : Mapping of CO to PO/PSO ");

        Row rT1Header = getOrCreateRow(sheet, rowIdx++);
        rT1Header.setHeightInPoints(14.25f);
        setCellValAndStyle(rT1Header, 0, "Sr No", s.tableHeader);
        setCellValAndStyle(rT1Header, 1, "Code", s.tableHeader);
        int t1c = 2;
        for (String po : poCodes) {
            setCellValAndStyle(rT1Header, t1c++, po, s.tableHeader);
        }
        for (String pso : psoCodes) {
            setCellValAndStyle(rT1Header, t1c++, pso, s.tableHeader);
        }
        // Blank fill remaining columns if totalCols > table1Cols
        for (int c = t1c; c <= endCol; c++) {
            setCellValAndStyle(rT1Header, c, "", s.tableHeader);
        }

        // Map Table 1 Rows by CO Code
        Map<String, CourseAttainmentSnapshot.CoMappingRow> t1Map = new HashMap<>();
        if (snapshot.getTable1Mapping() != null) {
            for (CourseAttainmentSnapshot.CoMappingRow r : snapshot.getTable1Mapping()) {
                if (r.getCoCode() != null) t1Map.put(r.getCoCode(), r);
            }
        }

        int t1Sr = 1;
        for (CourseAttainmentSnapshot.CoAttainmentRow co : coRows) {
            Row rT1 = getOrCreateRow(sheet, rowIdx++);
            rT1.setHeightInPoints(14.25f);
            setCellValAndStyle(rT1, 0, String.valueOf(t1Sr++), s.tableDataSrNo);
            setCellValAndStyle(rT1, 1, co.getCoCode() != null ? co.getCoCode() : "", s.tableDataCode);

            CourseAttainmentSnapshot.CoMappingRow mapRow = t1Map.get(co.getCoCode());
            int cCol = 2;
            for (String po : poCodes) {
                Integer val = (mapRow != null && mapRow.getPoMappings() != null) ? mapRow.getPoMappings().get(po) : null;
                setCellValAndStyle(rT1, cCol++, (val != null && val > 0) ? String.valueOf(val) : "-", s.table1MappingCell);
            }
            for (String pso : psoCodes) {
                Integer val = (mapRow != null && mapRow.getPsoMappings() != null) ? mapRow.getPsoMappings().get(pso) : null;
                setCellValAndStyle(rT1, cCol++, (val != null && val > 0) ? String.valueOf(val) : "-", s.table1MappingCell);
            }
            for (int c = cCol; c <= endCol; c++) {
                setCellValAndStyle(rT1, c, "", s.table1MappingCell);
            }
        }

        // Table 1 Average Row
        Row rT1Avg = getOrCreateRow(sheet, rowIdx++);
        rT1Avg.setHeightInPoints(14.25f);
        setCellValAndStyle(rT1Avg, 0, "Average", s.table1AvgLabel);
        setCellValAndStyle(rT1Avg, 1, "", s.table1AvgLabel);

        Map<String, BigDecimal> poAvgMap = new HashMap<>();
        if (snapshot.getTable2DirectPO() != null) {
            for (var r : snapshot.getTable2DirectPO()) {
                if (r.getOutcomeCode() != null && r.getAverageMapping() != null) {
                    poAvgMap.put(r.getOutcomeCode(), r.getAverageMapping());
                }
            }
        }
        Map<String, BigDecimal> psoAvgMap = new HashMap<>();
        if (snapshot.getTable2DirectPSO() != null) {
            for (var r : snapshot.getTable2DirectPSO()) {
                if (r.getOutcomeCode() != null && r.getAverageMapping() != null) {
                    psoAvgMap.put(r.getOutcomeCode(), r.getAverageMapping());
                }
            }
        }

        int avgCol = 2;
        for (String po : poCodes) {
            BigDecimal v = poAvgMap.get(po);
            if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
                setCellNumAndStyle(rT1Avg, avgCol++, v.doubleValue(), s.table1AvgCell);
            } else {
                setCellValAndStyle(rT1Avg, avgCol++, "-", s.table1AvgDash);
            }
        }
        for (String pso : psoCodes) {
            BigDecimal v = psoAvgMap.get(pso);
            if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
                setCellNumAndStyle(rT1Avg, avgCol++, v.doubleValue(), s.table1AvgCell);
            } else {
                setCellValAndStyle(rT1Avg, avgCol++, "-", s.table1AvgDash);
            }
        }
        for (int c = avgCol; c <= endCol; c++) {
            setCellValAndStyle(rT1Avg, c, "", s.table1AvgCell);
        }

        // Spacer row after Table 1 (ht = 12.0)
        Row rSpacer3 = getOrCreateRow(sheet, rowIdx++);
        rSpacer3.setHeightInPoints(12.0f);

        // =========================================================================
        // SECTION D: OVERALL CO ATTAINMENT (Rows 31 to 32 in sample)
        // =========================================================================
        int secDR1 = rowIdx++;
        int secDR2 = rowIdx++;

        Row rSecD1 = getOrCreateRow(sheet, secDR1);
        rSecD1.setHeightInPoints(13.5f);
        Row rSecD2 = getOrCreateRow(sheet, secDR2);
        rSecD2.setHeightInPoints(13.5f);

        // A..C Row 1: "Overall CO Attainment"
        mergeAndApplyStyle(sheet, secDR1, secDR1, 0, 2, s.secDTitle);
        getOrCreateCell(rSecD1, 0).setCellValue("Overall CO Attainment");

        // D..E merged across 2 rows: "Get from below reference work"
        mergeAndApplyStyle(sheet, secDR1, secDR2, 3, 4, s.secDNote);
        getOrCreateCell(rSecD1, 3).setCellValue("Get from below reference work");

        // A..C Row 2: Value from snapshot.overallCoAttainment
        mergeAndApplyStyle(sheet, secDR2, secDR2, 0, 2, s.secDValue);
        if (snapshot.getOverallCoAttainment() != null) {
            getOrCreateCell(rSecD2, 0).setCellValue(snapshot.getOverallCoAttainment().doubleValue());
        } else {
            getOrCreateCell(rSecD2, 0).setCellValue(0.0);
        }

        // Spacer row after Section D (ht = 14.25)
        Row rSpacer4 = getOrCreateRow(sheet, rowIdx++);
        rSpacer4.setHeightInPoints(14.25f);

        // =========================================================================
        // SECTION E: TABLE 2: PO ATTAINMENT VALUES (DIRECT ATTAINMENT)
        // =========================================================================
        Row rT2Banner = getOrCreateRow(sheet, rowIdx++);
        rT2Banner.setHeightInPoints(13.5f);
        mergeAndApplyStyle(sheet, rowIdx - 1, rowIdx - 1, 0, endCol, s.bannerCyanRegular);
        getOrCreateCell(rT2Banner, 0).setCellValue("Table 2: PO Attainment Values (Direct Attainment)");

        Row rT2Header = getOrCreateRow(sheet, rowIdx++);
        rT2Header.setHeightInPoints(14.25f);
        mergeAndApplyStyle(sheet, rowIdx - 1, rowIdx - 1, 0, 1, s.tableHeader);
        getOrCreateCell(rT2Header, 0).setCellValue("Code");
        int t2c = 2;
        for (String po : poCodes) {
            setCellValAndStyle(rT2Header, t2c++, po, s.tableHeader);
        }
        for (String pso : psoCodes) {
            setCellValAndStyle(rT2Header, t2c++, pso, s.tableHeader);
        }
        for (int c = t2c; c <= endCol; c++) {
            setCellValAndStyle(rT2Header, c, "", s.tableHeader);
        }

        Map<String, BigDecimal> poContMap = new HashMap<>();
        if (snapshot.getTable2DirectPO() != null) {
            for (var r : snapshot.getTable2DirectPO()) {
                if (r.getOutcomeCode() != null && r.getDirectContribution() != null) {
                    poContMap.put(r.getOutcomeCode(), r.getDirectContribution());
                }
            }
        }
        Map<String, BigDecimal> psoContMap = new HashMap<>();
        if (snapshot.getTable2DirectPSO() != null) {
            for (var r : snapshot.getTable2DirectPSO()) {
                if (r.getOutcomeCode() != null && r.getDirectContribution() != null) {
                    psoContMap.put(r.getOutcomeCode(), r.getDirectContribution());
                }
            }
        }

        Row rT2Data = getOrCreateRow(sheet, rowIdx++);
        rT2Data.setHeightInPoints(14.25f);
        mergeAndApplyStyle(sheet, rowIdx - 1, rowIdx - 1, 0, 1, s.table2Code);
        getOrCreateCell(rT2Data, 0).setCellValue(courseCode);

        int t2dc = 2;
        for (String po : poCodes) {
            BigDecimal v = poContMap.get(po);
            if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
                setCellNumAndStyle(rT2Data, t2dc++, v.doubleValue(), s.table2ValueNum);
            } else {
                setCellValAndStyle(rT2Data, t2dc++, "-", s.table2ValueDash);
            }
        }
        for (String pso : psoCodes) {
            BigDecimal v = psoContMap.get(pso);
            if (v != null && v.compareTo(BigDecimal.ZERO) > 0) {
                setCellNumAndStyle(rT2Data, t2dc++, v.doubleValue(), s.table2ValueNum);
            } else {
                setCellValAndStyle(rT2Data, t2dc++, "-", s.table2ValueDash);
            }
        }
        for (int c = t2dc; c <= endCol; c++) {
            setCellValAndStyle(rT2Data, c, "", s.table2ValueDash);
        }

        // Two spacer rows after Table 2 (ht = 13.9, ht = 5.5)
        Row rSpacer5 = getOrCreateRow(sheet, rowIdx++);
        rSpacer5.setHeightInPoints(13.9f);
        Row rSpacer6 = getOrCreateRow(sheet, rowIdx++);
        rSpacer6.setHeightInPoints(5.5f);

        // =========================================================================
        // SECTION F: REFERENCE WORK (Rows 39 to 48 in sample)
        // =========================================================================
        int refWorkStartRow = rowIdx;

        // Row 39: Banner "Reference Work"
        Row rRefBanner = getOrCreateRow(sheet, rowIdx++);
        rRefBanner.setHeightInPoints(14.25f);
        mergeAndApplyStyle(sheet, rowIdx - 1, rowIdx - 1, 0, lastCoCol, s.refBanner);
        getOrCreateCell(rRefBanner, 0).setCellValue("Reference Work");

        // Row 40: CO Headers (Col D onwards)
        Row rRefCo = getOrCreateRow(sheet, rowIdx++);
        rRefCo.setHeightInPoints(19.9f);
        for (int i = 0; i < coCount; i++) {
            CourseAttainmentSnapshot.CoAttainmentRow co = coRows.get(i);
            String label = (co.getCoCode() != null && !co.getCoCode().isBlank()) ? co.getCoCode() : ("CO" + (i + 1));
            setCellValAndStyle(rRefCo, 3 + i, label, s.refCoHeader);
        }

        // Row 41: Direct % ("% of students above threshold", "Direct through Examination", percentages)
        int r41Idx = rowIdx++;
        Row rRef41 = getOrCreateRow(sheet, r41Idx);
        rRef41.setHeightInPoints(19.9f);
        mergeAndApplyStyle(sheet, r41Idx, r41Idx, 0, 1, s.refThreshLabelTop);
        getOrCreateCell(rRef41, 0).setCellValue("% of students above threshold");

        for (int i = 0; i < coCount; i++) {
            CourseAttainmentSnapshot.CoAttainmentRow co = coRows.get(i);
            BigDecimal pct = co.getDirectPercentage();
            if (pct != null) {
                setCellNumAndStyle(rRef41, 3 + i, pct.doubleValue(), s.refPercentVal);
            } else {
                setCellValAndStyle(rRef41, 3 + i, "-", s.refPercentVal);
            }
        }
        if (lastCoCol + 1 <= refWorkEndCol) {
            setCellValAndStyle(rRef41, lastCoCol + 1, "Get values from Examination sheet", s.refNote);
        }

        // Row 42: Direct Attainment ("Attainment", levels)
        int r42Idx = rowIdx++;
        Row rRef42 = getOrCreateRow(sheet, r42Idx);
        rRef42.setHeightInPoints(19.9f);
        mergeAndApplyStyle(sheet, r42Idx, r42Idx, 0, 1, s.refAttainLabelBottom);
        getOrCreateCell(rRef42, 0).setCellValue("Attainment");

        // Merge C41:C42 for "Direct through Examination"
        mergeAndApplyStyle(sheet, r41Idx, r42Idx, 2, 2, s.refMethodDirect);
        getOrCreateCell(rRef41, 2).setCellValue("Direct through Examination");

        for (int i = 0; i < coCount; i++) {
            CourseAttainmentSnapshot.CoAttainmentRow co = coRows.get(i);
            Integer lvl = co.getDirectLevel();
            if (lvl != null) {
                setCellNumAndStyle(rRef42, 3 + i, lvl, s.refLevelVal);
            } else {
                setCellValAndStyle(rRef42, 3 + i, "-", s.refLevelVal);
            }
        }

        // Row 43: Indirect % ("% of students above threshold", "Indirect through Course End Survey", percentages)
        int r43Idx = rowIdx++;
        Row rRef43 = getOrCreateRow(sheet, r43Idx);
        rRef43.setHeightInPoints(19.9f);
        mergeAndApplyStyle(sheet, r43Idx, r43Idx, 0, 1, s.refThreshLabelTop);
        getOrCreateCell(rRef43, 0).setCellValue("% of students above threshold");

        for (int i = 0; i < coCount; i++) {
            CourseAttainmentSnapshot.CoAttainmentRow co = coRows.get(i);
            BigDecimal pct = co.getIndirectPercentage();
            if (pct != null) {
                setCellNumAndStyle(rRef43, 3 + i, pct.doubleValue(), s.refPercentVal);
            } else {
                setCellValAndStyle(rRef43, 3 + i, "-", s.refPercentVal);
            }
        }
        if (lastCoCol + 1 <= refWorkEndCol) {
            setCellValAndStyle(rRef43, lastCoCol + 1, "Get values from Course End sheet", s.refNote);
        }

        // Row 44: Indirect Attainment ("Attainment", levels)
        int r44Idx = rowIdx++;
        Row rRef44 = getOrCreateRow(sheet, r44Idx);
        rRef44.setHeightInPoints(19.9f);
        mergeAndApplyStyle(sheet, r44Idx, r44Idx, 0, 1, s.refAttainLabelBottom);
        getOrCreateCell(rRef44, 0).setCellValue("Attainment");

        // Merge C43:C44 for "Indirect through Course End Survey"
        mergeAndApplyStyle(sheet, r43Idx, r44Idx, 2, 2, s.refMethodDirect);
        getOrCreateCell(rRef43, 2).setCellValue("Indirect through Course End Survey");

        for (int i = 0; i < coCount; i++) {
            CourseAttainmentSnapshot.CoAttainmentRow co = coRows.get(i);
            Integer lvl = co.getIndirectLevel();
            if (lvl != null) {
                setCellNumAndStyle(rRef44, 3 + i, lvl, s.refLevelVal);
            } else {
                setCellValAndStyle(rRef44, 3 + i, "-", s.refLevelVal);
            }
        }

        // Row 45: Spacer inside Reference Work (ht = 8.5)
        Row rSpacerRef1 = getOrCreateRow(sheet, rowIdx++);
        rSpacerRef1.setHeightInPoints(8.5f);

        // Row 46: Attainment of CO ("Attainment of CO", combined attainment values)
        int r46Idx = rowIdx++;
        Row rRef46 = getOrCreateRow(sheet, r46Idx);
        rRef46.setHeightInPoints(19.9f);
        mergeAndApplyStyle(sheet, r46Idx, r46Idx, 0, 2, s.refAttainCoLabel);
        getOrCreateCell(rRef46, 0).setCellValue("Attainment of CO");

        for (int i = 0; i < coCount; i++) {
            CourseAttainmentSnapshot.CoAttainmentRow co = coRows.get(i);
            BigDecimal fa = co.getFinalAttainment();
            if (fa != null) {
                setCellNumAndStyle(rRef46, 3 + i, fa.doubleValue(), s.refAttainCoVal);
            } else {
                setCellValAndStyle(rRef46, 3 + i, "-", s.refAttainCoVal);
            }
        }

        // Row 47: Spacer inside Reference Work (ht = 14.25)
        Row rSpacerRef2 = getOrCreateRow(sheet, rowIdx++);
        rSpacerRef2.setHeightInPoints(14.25f);

        // Row 48: Overall CO Attainment ("Overall CO Attainment ", overall attainment value)
        int r48Idx = rowIdx++;
        Row rRef48 = getOrCreateRow(sheet, r48Idx);
        rRef48.setHeightInPoints(30.0f);
        mergeAndApplyStyle(sheet, r48Idx, r48Idx, 0, 2, s.refOverallLabel);
        getOrCreateCell(rRef48, 0).setCellValue("Overall CO Attainment ");

        mergeAndApplyStyle(sheet, r48Idx, r48Idx, 3, lastCoCol, s.refOverallVal);
        if (snapshot.getOverallCoAttainment() != null) {
            getOrCreateCell(rRef48, 3).setCellValue(snapshot.getOverallCoAttainment().doubleValue());
        } else {
            getOrCreateCell(rRef48, 3).setCellValue(0.0);
        }

        int refWorkEndRow = r48Idx;

        // Apply Reference Work outer mediumDashed boundary
        CellRangeAddress refWorkBox = new CellRangeAddress(refWorkStartRow, refWorkEndRow, 0, refWorkEndCol);
        RegionUtil.setBorderLeft(BorderStyle.MEDIUM_DASHED, refWorkBox, sheet);
        RegionUtil.setBorderRight(BorderStyle.MEDIUM_DASHED, refWorkBox, sheet);
        RegionUtil.setBorderTop(BorderStyle.MEDIUM_DASHED, refWorkBox, sheet);
        RegionUtil.setBorderBottom(BorderStyle.MEDIUM_DASHED, refWorkBox, sheet);

        return sheet;
    }

    // --- Helper Methods ---

    private static void renderInfoRow(Sheet sheet, int rowIdx, String label, String value, int endCol, CellStyle labelStyle, CellStyle valStyle) {
        Row row = getOrCreateRow(sheet, rowIdx);
        row.setHeightInPoints(14.25f);
        mergeAndApplyStyle(sheet, rowIdx, rowIdx, 0, 1, labelStyle);
        getOrCreateCell(row, 0).setCellValue(label);
        mergeAndApplyStyle(sheet, rowIdx, rowIdx, 2, endCol, valStyle);
        getOrCreateCell(row, 2).setCellValue(value != null ? value : "");
    }

    private static String formatSemester(Integer sem) {
        if (sem == null) return "";
        return switch (sem) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            default -> String.valueOf(sem);
        };
    }

    private static void mergeAndApplyStyle(Sheet sheet, int r1, int r2, int c1, int c2, CellStyle style) {
        for (int r = r1; r <= r2; r++) {
            Row row = getOrCreateRow(sheet, r);
            for (int c = c1; c <= c2; c++) {
                Cell cell = getOrCreateCell(row, c);
                cell.setCellStyle(style);
            }
        }
        if (r2 > r1 || c2 > c1) {
            sheet.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2));
        }
    }

    private static Row getOrCreateRow(Sheet sheet, int r) {
        Row row = sheet.getRow(r);
        return row != null ? row : sheet.createRow(r);
    }

    private static Cell getOrCreateCell(Row row, int c) {
        Cell cell = row.getCell(c);
        return cell != null ? cell : row.createCell(c);
    }

    private static void setCellValAndStyle(Row row, int c, String val, CellStyle style) {
        Cell cell = getOrCreateCell(row, c);
        cell.setCellValue(val != null ? val : "");
        cell.setCellStyle(style);
    }

    private static void setCellNumAndStyle(Row row, int c, double val, CellStyle style) {
        Cell cell = getOrCreateCell(row, c);
        cell.setCellValue(val);
        cell.setCellStyle(style);
    }

    // --- Style Bundle Construction ---

    private record StyleBundle(
            CellStyle infoLabel,
            CellStyle infoValue,
            CellStyle bannerCyanBold,
            CellStyle bannerCyanRegular,
            CellStyle tableHeader,
            CellStyle tableDataSrNo,
            CellStyle tableDataCode,
            CellStyle coCodeStyle,
            CellStyle coStatementStyle,
            CellStyle table1MappingCell,
            CellStyle table1AvgLabel,
            CellStyle table1AvgCell,
            CellStyle table1AvgDash,
            CellStyle secDTitle,
            CellStyle secDNote,
            CellStyle secDValue,
            CellStyle table2Code,
            CellStyle table2ValueNum,
            CellStyle table2ValueDash,
            CellStyle refBanner,
            CellStyle refCoHeader,
            CellStyle refThreshLabelTop,
            CellStyle refAttainLabelBottom,
            CellStyle refMethodDirect,
            CellStyle refPercentVal,
            CellStyle refLevelVal,
            CellStyle refNote,
            CellStyle refAttainCoLabel,
            CellStyle refAttainCoVal,
            CellStyle refOverallLabel,
            CellStyle refOverallVal
    ) {}

    private static StyleBundle createStyles(Workbook wb) {
        DataFormat df = wb.createDataFormat();
        short fmtDec2 = df.getFormat("0.00");
        short fmtDec1 = df.getFormat("0.0");

        // Section A: Info
        CellStyle infoLabel = createStyle(wb, "Verdana", 10, true, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.NONE, (short) 0);
        CellStyle infoValue = createStyle(wb, "Verdana", 11, false, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false, COLOR_LIGHT_GRAY, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);

        // Banners
        CellStyle bannerCyanBold = createStyle(wb, "Verdana", 11, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BANNER_CYAN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle bannerCyanRegular = createStyle(wb, "Verdana", 11, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BANNER_CYAN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);

        // General Table Headers & Data
        CellStyle tableHeader = createStyle(wb, "Verdana", 11, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle tableDataSrNo = createStyle(wb, "Verdana", 11, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle tableDataCode = createStyle(wb, "Verdana", 11, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_LIGHT_GRAY, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);

        // Section B CO items
        CellStyle coCodeStyle = createStyle(wb, "Verdana", 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_LIGHT_GRAY, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle coStatementStyle = createStyle(wb, "Verdana", 10, false, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, true, COLOR_LIGHT_GRAY, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);

        // Table 1 Mappings & Average
        CellStyle table1MappingCell = createStyle(wb, "Verdana", 8, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, null, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle table1AvgLabel = createStyle(wb, "Verdana", 11, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle table1AvgCell = createStyle(wb, "Verdana", 11, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BRIGHT_BLUE, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, fmtDec2);
        CellStyle table1AvgDash = createStyle(wb, "Verdana", 11, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BRIGHT_BLUE, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);

        // Section D: Overall CO Attainment
        CellStyle secDTitle = createStyle(wb, "Verdana", 11, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, (short) 0);
        CellStyle secDNote = createStyle(wb, "Verdana", 9, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, null, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, (short) 0);
        CellStyle secDValue = createStyle(wb, "Verdana", 11, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_ACCENT_BLUE, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, fmtDec2);

        // Section E: Table 2
        CellStyle table2Code = createStyle(wb, "Verdana", 11, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_CODE_GRAY, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle table2ValueNum = createStyle(wb, "Verdana", 11, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, null, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, fmtDec2);
        CellStyle table2ValueDash = createStyle(wb, "Verdana", 11, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, null, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);

        // Section F: Reference Work
        CellStyle refBanner = createStyle(wb, "Verdana", 11, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_ACCENT_BLUE, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle refCoHeader = createStyle(wb, "Verdana", 10, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BRIGHT_BLUE, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle refThreshLabelTop = createStyle(wb, "Verdana", 8, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, null, BorderStyle.THIN, BorderStyle.NONE, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle refAttainLabelBottom = createStyle(wb, "Verdana", 10, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.NONE, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle refMethodDirect = createStyle(wb, "Verdana", 8, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_SOFT_PINK, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle refPercentVal = createStyle(wb, "Verdana", 9, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_LIGHT_GRAY, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, fmtDec1);
        CellStyle refLevelVal = createStyle(wb, "Verdana", 9, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, null, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle refNote = createStyle(wb, "Verdana", 9, false, false, HorizontalAlignment.LEFT, VerticalAlignment.CENTER, false, null, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.NONE, (short) 0);
        CellStyle refAttainCoLabel = createStyle(wb, "Verdana", 10, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_LAVENDER, BorderStyle.NONE, BorderStyle.NONE, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle refAttainCoVal = createStyle(wb, "Verdana", 8, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_ACCENT_BLUE, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, fmtDec2);
        CellStyle refOverallLabel = createStyle(wb, "Verdana", 11, false, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, true, COLOR_BRIGHT_BLUE, BorderStyle.NONE, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, (short) 0);
        CellStyle refOverallVal = createStyle(wb, "Verdana", 9, true, false, HorizontalAlignment.CENTER, VerticalAlignment.CENTER, false, COLOR_BANNER_CYAN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, BorderStyle.THIN, fmtDec2);

        return new StyleBundle(
                infoLabel, infoValue, bannerCyanBold, bannerCyanRegular,
                tableHeader, tableDataSrNo, tableDataCode, coCodeStyle, coStatementStyle,
                table1MappingCell, table1AvgLabel, table1AvgCell, table1AvgDash,
                secDTitle, secDNote, secDValue,
                table2Code, table2ValueNum, table2ValueDash,
                refBanner, refCoHeader, refThreshLabelTop, refAttainLabelBottom,
                refMethodDirect, refPercentVal, refLevelVal, refNote,
                refAttainCoLabel, refAttainCoVal, refOverallLabel, refOverallVal
        );
    }

    private static CellStyle createStyle(Workbook wb, String fontName, int fontSize, boolean bold, boolean italic,
                                         HorizontalAlignment hAlign, VerticalAlignment vAlign, boolean wrapText,
                                         Color fillColor,
                                         BorderStyle borderTop, BorderStyle borderBottom, BorderStyle borderLeft, BorderStyle borderRight,
                                         short dataFormat) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName(fontName);
        font.setFontHeightInPoints((short) fontSize);
        font.setBold(bold);
        font.setItalic(italic);
        style.setFont(font);

        if (hAlign != null) style.setAlignment(hAlign);
        if (vAlign != null) style.setVerticalAlignment(vAlign);
        style.setWrapText(wrapText);

        if (fillColor != null && wb instanceof org.apache.poi.xssf.usermodel.XSSFWorkbook) {
            ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(fillColor, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        if (borderTop != null) style.setBorderTop(borderTop);
        if (borderBottom != null) style.setBorderBottom(borderBottom);
        if (borderLeft != null) style.setBorderLeft(borderLeft);
        if (borderRight != null) style.setBorderRight(borderRight);

        if (dataFormat > 0) {
            style.setDataFormat(dataFormat);
        }

        return style;
    }
}
