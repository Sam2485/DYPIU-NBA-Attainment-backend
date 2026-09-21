package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.awt.Color;
import java.time.format.DateTimeFormatter;

/**
 * Authoritative Shared Excel Header Renderer for Programme Attainment reports.
 * <p>
 * Strictly replicates the visual structure, layout, styles, row heights, column proportions,
 * borders, fonts, fills, and alignments of the reference workbook
 * (Final-Mapping-Attainment Values Sheet (2) (1) (1).xlsx).
 * <p>
 * Structure:
 * - Row 0 (Excel 1, ht=15.75 pt): Empty spacer row.
 * - Row 1 (Excel 2, ht=27.75 pt): Upper header row 1 (Institution Name, Logo top, Reserved area top).
 * - Row 2 (Excel 3, ht=41.25 pt): Upper header row 2 (School Name, Logo bottom, Reserved area bottom).
 * - Row 3 (Excel 4, ht=15.75 pt): Lower header row 1 (Academic Year, Report Title, Revision).
 * - Row 4 (Excel 5, ht=15.75 pt): Lower header row 2 (Dated).
 * - Row 5 (Excel 6, ht=42.75 pt): Lower header row 3 (Term, Department/Programme, Date of Preparation).
 * - Row 6 (Excel 7, ht=15.75 pt): Empty spacer row.
 * - Row 7 (Excel 8, ht=15.75 pt): Empty spacer row.
 * - Returns Row 8 (Excel Row 9) for table headers.
 * <p>
 * Strict Invariant: HEADER LAST COLUMN == CONTENT LAST COLUMN (0 to totalColumns - 1).
 */
@Slf4j
public class CommonExcelHeaderRenderer {

    // Authoritative reference colors matching the reference workbook:
    // Upper region: pure white #FFFFFF
    // Lower region: reference grey #D9D9D9 (FFD9D9D9)
    public static final Color COLOR_GREY_HEADER = new Color(217, 217, 217);

    // Authoritative fixed logo physical dimensions for Programme Attainment (increased by 40%):
    // cx = 1402080 * 1.4 = 1962912 EMUs (154.56 pt = 206.08 px)
    // cy = 524269 * 1.4 = 733977 EMUs (57.79 pt = 77.06 px)
    public static final long FIXED_LOGO_WIDTH_EMU = 1962912L;
    public static final long FIXED_LOGO_HEIGHT_EMU = 733977L;

    // Target physical width for the left logo region (approx 110 pt = 147 px = 1,402,080 EMUs)
    public static final long TARGET_LOGO_REGION_WIDTH_EMU = 1402080L;

    // Target physical width for the right reserved metadata region (approx 140 pt = 188 px = 1,800,000 EMUs)
    public static final long TARGET_RIGHT_REGION_WIDTH_EMU = 1800000L;

    /**
     * Primary shared renderer method for Programme Attainment sheets with both left and right logos.
     */
    public static int renderProgrammeHeader(
            Workbook wb,
            Sheet sheet,
            ProgrammeAttainmentSnapshot snapshot,
            String reportTitle,
            int totalColumns,
            byte[] leftLogoBytes,
            byte[] rightLogoBytes,
            ReportTemplateDto template,
            String term,
            boolean isLandscape) {

        String institution = (snapshot != null && snapshot.getInstitutionName() != null && !snapshot.getInstitutionName().isBlank())
                ? snapshot.getInstitutionName()
                : (template != null && template.getHeaderConfig() != null && template.getHeaderConfig().getInstitutionName() != null
                    ? template.getHeaderConfig().getInstitutionName()
                    : "D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE");

        String school = (snapshot != null && snapshot.getSchoolName() != null && !snapshot.getSchoolName().isBlank())
                ? snapshot.getSchoolName()
                : "School of Engineering and Technology";

        String ay = (snapshot != null && snapshot.getAcademicYear() != null && !snapshot.getAcademicYear().isBlank())
                ? snapshot.getAcademicYear()
                : "—";

        String deptOrProg;
        if (snapshot != null && snapshot.getDepartmentName() != null && !snapshot.getDepartmentName().isBlank()) {
            deptOrProg = "Department : " + snapshot.getDepartmentName();
        } else if (snapshot != null && snapshot.getMasterProgrammeName() != null && !snapshot.getMasterProgrammeName().isBlank()) {
            deptOrProg = "Department : " + snapshot.getMasterProgrammeName();
        } else {
            deptOrProg = "Department : " + school;
        }

        String revision = (template != null && template.getTemplateVersion() != null)
                ? String.format("%02d", template.getTemplateVersion())
                : "00";

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String dated;
        if (template != null && template.getUpdatedAt() != null) {
            dated = template.getUpdatedAt().format(dtf);
        } else if (snapshot != null && snapshot.getGeneratedAt() != null) {
            dated = snapshot.getGeneratedAt().format(dtf);
        } else {
            dated = "—";
        }

        String dateOfPrep = (snapshot != null && snapshot.getGeneratedAt() != null)
                ? snapshot.getGeneratedAt().format(dtf)
                : "";

        return renderProgrammeHeader(
                wb, sheet,
                institution, school, reportTitle,
                ay, term != null ? term : "Term – I & II", deptOrProg,
                revision, dated, dateOfPrep,
                totalColumns, leftLogoBytes, rightLogoBytes, isLandscape);
    }

    /**
     * Backward-compatible method accepting single left logo bytes.
     */
    public static int renderProgrammeHeader(
            Workbook wb,
            Sheet sheet,
            ProgrammeAttainmentSnapshot snapshot,
            String reportTitle,
            int totalColumns,
            byte[] leftLogoBytes,
            ReportTemplateDto template,
            String term,
            boolean isLandscape) {
        return renderProgrammeHeader(
                wb, sheet, snapshot, reportTitle, totalColumns, leftLogoBytes, null, template, term, isLandscape);
    }

    /**
     * Parameterized renderer accepting all discrete dynamic values including both left and right logos.
     */
    public static int renderProgrammeHeader(
            Workbook wb,
            Sheet sheet,
            String institutionName,
            String schoolName,
            String reportTitle,
            String academicYear,
            String term,
            String departmentOrProgramme,
            String revision,
            String dated,
            String dateOfPrep,
            int totalColumns,
            byte[] leftLogoBytes,
            byte[] rightLogoBytes,
            boolean isLandscape) {

        int endCol = Math.max(totalColumns - 1, 5);

        // Ensure sheet has reasonable column widths if not yet configured by caller
        ensureDefaultColumnWidths(sheet, totalColumns);

        // 1. Determine Left and Right Logo Regions based on FIXED PHYSICAL WIDTH (never fixed column counts)
        // Center flexible region must always retain sufficient space and absorb width variations
        int minCenterCols = (totalColumns >= 8) ? 3 : (totalColumns >= 6 ? 2 : 1);
        int maxSideCols = Math.max(1, (totalColumns - minCenterCols) / 2);

        int colLeftStart = 0;
        int colLeftEnd = 0;
        long leftRegionWidthEmu = 0;
        while (colLeftEnd < maxSideCols - 1) {
            long w = Units.columnWidthToEMU(sheet.getColumnWidth(colLeftEnd));
            leftRegionWidthEmu += w;
            if (leftRegionWidthEmu >= TARGET_LOGO_REGION_WIDTH_EMU) {
                break;
            }
            colLeftEnd++;
        }

        int colRightEnd = endCol;
        int colRightStart = endCol;
        long rightRegionWidthEmu = 0;
        while (colRightStart > endCol - (maxSideCols - 1)) {
            long w = Units.columnWidthToEMU(sheet.getColumnWidth(colRightStart));
            rightRegionWidthEmu += w;
            if (rightRegionWidthEmu >= TARGET_RIGHT_REGION_WIDTH_EMU) {
                break;
            }
            colRightStart--;
        }

        int colCenterStart = colLeftEnd + 1;
        int colCenterEnd = colRightStart - 1;

        // 2. Prepare reference styles
        CellStyle styleInst = createInstitutionStyle(wb);
        CellStyle styleSchool = createSchoolStyle(wb);
        CellStyle styleUpperBox = createUpperBoxStyle(wb);
        CellStyle styleGreyCenter = createGreyCenterStyle(wb);
        CellStyle styleGreyTop = createGreyTopStyle(wb);

        // Row 0 (Excel Row 1, ht=15.75 pt): Empty spacer row
        Row r0 = sheet.createRow(0);
        r0.setHeightInPoints(15.75f);

        // Row 1 (Excel Row 2, ht=27.75 pt) & Row 2 (Excel Row 3, ht=41.25 pt)
        Row r1 = sheet.createRow(1);
        r1.setHeightInPoints(27.75f);
        Row r2 = sheet.createRow(2);
        r2.setHeightInPoints(41.25f);

        // Upper Left: Logo Area (A2:B3 or A2:A3)
        styleRegion(sheet, 1, 2, colLeftStart, colLeftEnd, styleUpperBox, null);

        // Upper Center Top: Institution Name (C2:M2 or B2:L2)
        styleRegion(sheet, 1, 1, colCenterStart, colCenterEnd, styleInst, institutionName != null ? institutionName.toUpperCase() : "");

        // Upper Center Bottom: School Name (C3:M3 or B3:L3)
        styleRegion(sheet, 2, 2, colCenterStart, colCenterEnd, styleSchool, schoolName != null ? schoolName : "");

        // Upper Right: Reserved Metadata Area / Right Logo Area (N2:Q3 or M2:P3)
        styleRegion(sheet, 1, 2, colRightStart, colRightEnd, styleUpperBox, null);

        // Row 3 (Excel Row 4, ht=15.75 pt) & Row 4 (Excel Row 5, ht=15.75 pt)
        Row r3 = sheet.createRow(3);
        r3.setHeightInPoints(15.75f);
        Row r4 = sheet.createRow(4);
        r4.setHeightInPoints(15.75f);

        // Lower Left Top: Academic Year (A4:B5 or A4:A5)
        String ayText = "Academic Year: " + (academicYear != null ? academicYear : "—");
        styleRegion(sheet, 3, 4, colLeftStart, colLeftEnd, styleGreyCenter, ayText);

        // Lower Center Top: Report Title (C4:M5 or B4:L5)
        styleRegion(sheet, 3, 4, colCenterStart, colCenterEnd, styleGreyCenter, reportTitle != null ? reportTitle : "");

        // Lower Right Row 4: Revision (N4:Q4 or M4:P4)
        String revText = "Revision : " + (revision != null ? revision : "00");
        styleRegion(sheet, 3, 3, colRightStart, colRightEnd, styleGreyCenter, revText);

        // Lower Right Row 5: Dated (N5:Q5 or M5:P5)
        String datedText = "Dated : " + (dated != null ? dated : "—");
        styleRegion(sheet, 4, 4, colRightStart, colRightEnd, styleGreyCenter, datedText);

        // Row 5 (Excel Row 6, ht=42.75 pt)
        Row r5 = sheet.createRow(5);
        r5.setHeightInPoints(42.75f);

        // Lower Left Bottom: Term (A6:B6 or A6:A6)
        String termText = (term != null && !term.isBlank() ? term : "Term – I & II");
        styleRegion(sheet, 5, 5, colLeftStart, colLeftEnd, styleGreyCenter, termText);

        // Lower Center Bottom: Department / Programme (C6:M6 or B6:L6)
        styleRegion(sheet, 5, 5, colCenterStart, colCenterEnd, styleGreyCenter, departmentOrProgramme != null ? departmentOrProgramme : "");

        // Lower Right Row 6: Date of Preparation (N6:Q6 or M6:P6)
        String prepText = "Date of Preparation : " + (dateOfPrep != null ? dateOfPrep : "");
        styleRegion(sheet, 5, 5, colRightStart, colRightEnd, styleGreyTop, prepText);

        // Row 6 (Excel Row 7, ht=15.75 pt) & Row 7 (Excel Row 8, ht=15.75 pt): Spacers
        Row r6 = sheet.createRow(6);
        r6.setHeightInPoints(15.75f);
        Row r7 = sheet.createRow(7);
        r7.setHeightInPoints(15.75f);

        // 3. Embed Logo into Left Area centered with FIXED Physical Size
        if (leftLogoBytes != null && leftLogoBytes.length > 0) {
            embedCenteredLogo(wb, sheet, leftLogoBytes, colLeftStart, colLeftEnd, 1, 2, FIXED_LOGO_WIDTH_EMU, FIXED_LOGO_HEIGHT_EMU);
        }

        // 4. Embed Logo into Right Area centered with FIXED Physical Size
        if (rightLogoBytes != null && rightLogoBytes.length > 0) {
            embedCenteredLogo(wb, sheet, rightLogoBytes, colRightStart, colRightEnd, 1, 2, FIXED_LOGO_WIDTH_EMU, FIXED_LOGO_HEIGHT_EMU);
        }

        // Configure Sheet Print & Page setup
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setLandscape(isLandscape);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setDisplayGridlines(true);

        return 8; // Next row index for table headers (Excel Row 9)
    }

    /**
     * Backward-compatible parameterized renderer accepting single left logo bytes.
     */
    public static int renderProgrammeHeader(
            Workbook wb,
            Sheet sheet,
            String institutionName,
            String schoolName,
            String reportTitle,
            String academicYear,
            String term,
            String departmentOrProgramme,
            String revision,
            String dated,
            String dateOfPrep,
            int totalColumns,
            byte[] leftLogoBytes,
            boolean isLandscape) {
        return renderProgrammeHeader(
                wb, sheet,
                institutionName, schoolName, reportTitle,
                academicYear, term, departmentOrProgramme,
                revision, dated, dateOfPrep,
                totalColumns, leftLogoBytes, null, isLandscape);
    }

    /**
     * Backward-compatible method preserved for callers such as ProgrammeAtrSheetBuilder.
     */
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

        if (totalColumns <= 6) {
            return renderCompactAtrHeader(wb, sheet, institutionName, schoolName, reportTitle,
                    scopeLabelAndValue, academicYear, termOrSemester, reportId, totalColumns, isLandscape);
        }

        return renderProgrammeHeader(
                wb, sheet,
                institutionName, schoolName, reportTitle,
                academicYear, termOrSemester, scopeLabelAndValue,
                "00", "—", "—",
                totalColumns, null, isLandscape);
    }

    private static int renderCompactAtrHeader(
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

        Row r0 = sheet.createRow(0);
        r0.setHeightInPoints(24);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(institutionName != null && !institutionName.isBlank()
                ? institutionName.toUpperCase() : "D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE");
        c0.setCellStyle(instStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, endCol));

        Row r1 = sheet.createRow(1);
        r1.setHeightInPoints(18);
        Cell c1 = r1.createCell(0);
        c1.setCellValue(schoolName != null && !schoolName.isBlank()
                ? schoolName : "School of Engineering and Technology");
        c1.setCellStyle(schoolStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, endCol));

        Row r2 = sheet.createRow(2);
        r2.setHeightInPoints(20);
        Cell c2 = r2.createCell(0);
        c2.setCellValue(reportTitle != null ? reportTitle.toUpperCase() : "ACADEMIC REPORT");
        c2.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, endCol));

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

        Row r4 = sheet.createRow(4);
        r4.setHeightInPoints(6);

        sheet.setFitToPage(true);
        sheet.getPrintSetup().setLandscape(isLandscape);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setDisplayGridlines(true);

        return 5;
    }

    private static void styleRegion(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol, CellStyle style, String text) {
        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) row = sheet.createRow(r);
            for (int c = firstCol; c <= lastCol; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) cell = row.createCell(c);
                cell.setCellStyle(style);
            }
        }
        if (text != null) {
            Row r0 = sheet.getRow(firstRow);
            Cell c0 = r0.getCell(firstCol);
            c0.setCellValue(text);
        }
        if (lastRow > firstRow || lastCol > firstCol) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
        }
    }

    private static CellStyle createInstitutionStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 14);
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBlackBorders(style);
        return style;
    }

    private static CellStyle createSchoolStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 16);
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBlackBorders(style);
        return style;
    }

    private static CellStyle createUpperBoxStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBlackBorders(style);
        return style;
    }

    private static CellStyle createGreyCenterStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        applyGreyFill(style);
        setBlackBorders(style);
        return style;
    }

    private static CellStyle createGreyTopStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Arial");
        font.setFontHeightInPoints((short) 12);
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        applyGreyFill(style);
        setBlackBorders(style);
        return style;
    }

    private static void applyGreyFill(CellStyle style) {
        if (style instanceof XSSFCellStyle xssf) {
            xssf.setFillForegroundColor(new XSSFColor(COLOR_GREY_HEADER, new DefaultIndexedColorMap()));
            xssf.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        } else {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
    }

    private static void setBlackBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
    }

    private static void embedCenteredLogo(
            Workbook wb,
            Sheet sheet,
            byte[] logoBytes,
            int startCol,
            int endCol,
            int startRow,
            int endRow,
            long maxWEmu,
            long maxHEmu) {

        if (logoBytes == null || logoBytes.length == 0) {
            return;
        }

        try {
            // 1. Calculate physical region width
            long regionWidthEmu = 0;
            for (int c = startCol; c <= endCol; c++) {
                regionWidthEmu += Units.columnWidthToEMU(sheet.getColumnWidth(c));
            }

            // 2. Calculate physical region height
            long regionHeightEmu = 0;
            for (int r = startRow; r <= endRow; r++) {
                Row row = sheet.getRow(r);
                float hPt = (row != null) ? row.getHeightInPoints() : sheet.getDefaultRowHeightInPoints();
                regionHeightEmu += Units.toEMU(hPt);
            }

            // 3. Determine actual image dimensions preserving aspect ratio
            long[] fittedDims = calculateFittedDimensions(logoBytes, maxWEmu, maxHEmu);
            long targetWEmu = fittedDims[0];
            long targetHEmu = Math.min(fittedDims[1], regionHeightEmu);

            // 4. Calculate centering margins (equal padding on both sides)
            long marginXEmu = Math.max(0, (regionWidthEmu - targetWEmu) / 2);
            long marginYEmu = Math.max(0, (regionHeightEmu - targetHEmu) / 2);

            // 5. Convert horizontal coordinates (targetX1, targetX2) to (col1, dx1) and (col2, dx2)
            int[] fromX = findCellAndOffset(sheet, startCol, marginXEmu, false);
            int[] toX = findCellAndOffset(sheet, startCol, marginXEmu + targetWEmu, false);

            // 6. Convert vertical coordinates (targetY1, targetY2) to (row1, dy1) and (row2, dy2)
            int[] fromY = findCellAndOffset(sheet, startRow, marginYEmu, true);
            int[] toY = findCellAndOffset(sheet, startRow, marginYEmu + targetHEmu, true);

            // 7. Add picture to workbook
            int pictureType = (logoBytes.length > 2 && (logoBytes[0] & 0xFF) == 0xFF && (logoBytes[1] & 0xFF) == 0xD8)
                    ? Workbook.PICTURE_TYPE_JPEG : Workbook.PICTURE_TYPE_PNG;
            int pictureIdx = wb.addPicture(logoBytes, pictureType);

            Drawing<?> drawing = sheet.getDrawingPatriarch();
            if (drawing == null) {
                drawing = sheet.createDrawingPatriarch();
            }

            CreationHelper helper = wb.getCreationHelper();
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(fromX[0]);
            anchor.setDx1(fromX[1]);
            anchor.setRow1(fromY[0]);
            anchor.setDy1(fromY[1]);
            anchor.setCol2(toX[0]);
            anchor.setDx2(toX[1]);
            anchor.setRow2(toY[0]);
            anchor.setDy2(toY[1]);

            Picture picture = drawing.createPicture(anchor, pictureIdx);
            if (picture.getClientAnchor() != null) {
                picture.getClientAnchor().setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
            }

            if (picture instanceof org.apache.poi.xssf.usermodel.XSSFPicture xPic) {
                try {
                    org.apache.xmlbeans.XmlCursor cursor = xPic.getCTPicture().newCursor();
                    if (cursor.toParent()) {
                        org.apache.xmlbeans.XmlObject parent = cursor.getObject();
                        if (parent instanceof org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTTwoCellAnchor ctAnchor) {
                            ctAnchor.setEditAs(org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.STEditAs.ONE_CELL);
                        }
                    }
                    cursor.dispose();

                    if (xPic.getCTPicture() != null && xPic.getCTPicture().getNvPicPr() != null) {
                        var nvPr = xPic.getCTPicture().getNvPicPr();
                        var cNvPr = nvPr.getCNvPicPr();
                        if (cNvPr != null) {
                            var locks = cNvPr.isSetPicLocks() ? cNvPr.getPicLocks() : cNvPr.addNewPicLocks();
                            locks.setNoChangeAspect(true);
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            log.warn("Failed to embed centered logo into Programme Excel header: {}", e.getMessage());
        }
    }

    private static int[] findCellAndOffset(Sheet sheet, int startIdx, long targetOffsetEmu, boolean isRow) {
        if (targetOffsetEmu <= 0) {
            return new int[]{startIdx, 0};
        }
        long accum = 0;
        int maxIdx = isRow ? 100 : Math.max(100, sheet.getRow(0) != null ? sheet.getRow(0).getLastCellNum() : 50);
        for (int i = startIdx; i <= maxIdx; i++) {
            long dim = isRow
                    ? Units.toEMU(sheet.getRow(i) != null ? sheet.getRow(i).getHeightInPoints() : sheet.getDefaultRowHeightInPoints())
                    : Units.columnWidthToEMU(sheet.getColumnWidth(i));
            if (accum + dim > targetOffsetEmu) {
                return new int[]{i, (int) (targetOffsetEmu - accum)};
            }
            accum += dim;
        }
        return new int[]{startIdx, 0};
    }

    private static long[] calculateFittedDimensions(byte[] logoBytes, long maxWEmu, long maxHEmu) {
        if (logoBytes == null || logoBytes.length == 0) {
            return new long[]{maxWEmu, maxHEmu};
        }
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(logoBytes));
            if (img != null && img.getWidth() > 0 && img.getHeight() > 0) {
                double scaleW = (double) maxWEmu / img.getWidth();
                double scaleH = (double) maxHEmu / img.getHeight();
                double scale = Math.min(scaleW, scaleH);
                long fitW = Math.round(img.getWidth() * scale);
                long fitH = Math.round(img.getHeight() * scale);
                return new long[]{fitW, fitH};
            }
        } catch (Exception ignored) {}
        return new long[]{maxWEmu, maxHEmu};
    }

    private static void ensureDefaultColumnWidths(Sheet sheet, int totalColumns) {
        if (sheet.getColumnWidth(0) <= 2048) {
            if ("Overall Programme Attainment".equalsIgnoreCase(sheet.getSheetName()) || totalColumns <= 16) {
                sheet.setColumnWidth(0, (int) (22.0 * 256));
                sheet.setColumnWidth(1, (int) (38.0 * 256));
                for (int i = 2; i < totalColumns; i++) {
                    sheet.setColumnWidth(i, (int) (7.50 * 256));
                }
            } else {
                sheet.setColumnWidth(0, (int) (15.82 * 256));
                sheet.setColumnWidth(1, (int) (14.18 * 256));
                sheet.setColumnWidth(2, (int) (41.00 * 256));
                for (int i = 3; i < totalColumns; i++) {
                    sheet.setColumnWidth(i, (int) (7.50 * 256));
                }
            }
        }
    }
}
