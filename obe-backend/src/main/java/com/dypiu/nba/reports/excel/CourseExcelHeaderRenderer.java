package com.dypiu.nba.reports.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Color;

/**
 * Authoritative Excel Header Renderer for COURSE-LEVEL Reports.
 * <p>
 * Strictly replicates the DYPIU NBA Course Attainment reference workbook
 * (Template-CO-PO-PSO-Attainment-TH-v2-Sample.xlsx) and visual screenshots.
 * <p>
 * Key Characteristics:
 * - Dynamic width: header last column EXACTLY matches the sheet content last column (totalColumns - 1).
 * - Left and Right IQAC-configurable logo slots.
 * - Center header: University Name (Row 0) and School Name (Row 1).
 * - Row 2: Header box spacer.
 * - Row 3: Cyan title band spanning full content width (0 to totalColumns - 1) with thin black border.
 * - Row 4: Spacer row (ht=14.25 pt).
 * - Next content row index: 5.
 */
@Slf4j
public class CourseExcelHeaderRenderer {

    // Cyan / Aqua title band fill: #8EC9DA (theme 8, tint 0.4 from reference)
    public static final Color COLOR_TITLE_BAND_CYAN = new Color(142, 201, 218);
    public static final Color COLOR_HEADER_TEXT = new Color(0, 0, 0);

    public static int renderHeader(
            Workbook wb,
            Sheet sheet,
            String institutionName,
            String schoolName,
            String reportTitle,
            int totalColumns,
            byte[] leftLogoBytes,
            byte[] rightLogoBytes,
            boolean isLandscape) {
        return renderHeader(wb, sheet, institutionName, schoolName, reportTitle, totalColumns, leftLogoBytes, rightLogoBytes, isLandscape, true);
    }

    public static int renderHeader(
            Workbook wb,
            Sheet sheet,
            String institutionName,
            String schoolName,
            String reportTitle,
            int totalColumns,
            byte[] leftLogoBytes,
            byte[] rightLogoBytes,
            boolean isLandscape,
            boolean includeBottomSpacer) {

        if (totalColumns < 4) {
            totalColumns = 4;
        }
        int endCol = totalColumns - 1;

        // 1. Calculate dynamic logo and center column boundaries
        int leftColCount = (totalColumns >= 12) ? 2 : 1;
        int rightColCount = (totalColumns >= 12) ? 2 : 1;

        int leftStartCol = 0;
        int leftEndCol = leftColCount - 1;

        int rightStartCol = totalColumns - rightColCount;
        int rightEndCol = endCol;

        int centerStartCol = leftEndCol + 1;
        int centerEndCol = rightStartCol - 1;

        if (centerEndCol < centerStartCol) {
            centerStartCol = 0;
            centerEndCol = endCol;
        }

        // 2. Prepare cell styles matching the reference template
        CellStyle uniStyle = createUniversityStyle(wb);
        CellStyle schoolStyle = createSchoolStyle(wb);
        CellStyle boxSpacerStyle = createHeaderBoxSpacerStyle(wb);
        CellStyle logoBoxStyle = createLogoSlotStyle(wb);
        CellStyle titleBandStyle = createTitleBandStyle(wb);

        // 3. Row 0: University Name (ht = 15 pt)
        Row r0 = getOrCreateRow(sheet, 0);
        r0.setHeightInPoints(15.0f);

        // Left logo area Row 0
        for (int c = leftStartCol; c <= leftEndCol; c++) {
            Cell cell = getOrCreateCell(r0, c);
            cell.setCellStyle(logoBoxStyle);
        }

        // Center: University Name
        for (int c = centerStartCol; c <= centerEndCol; c++) {
            Cell cell = getOrCreateCell(r0, c);
            cell.setCellStyle(uniStyle);
        }
        Cell centerCell0 = getOrCreateCell(r0, centerStartCol);
        String finalInstName = (institutionName != null && !institutionName.isBlank())
                ? institutionName.trim()
                : "D Y Patil International University, Akurdi Pune";
        centerCell0.setCellValue(finalInstName);

        // Right logo area Row 0
        for (int c = rightStartCol; c <= rightEndCol; c++) {
            Cell cell = getOrCreateCell(r0, c);
            cell.setCellStyle(logoBoxStyle);
        }

        if (centerEndCol > centerStartCol) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, centerStartCol, centerEndCol));
        }

        // 4. Row 1: School Name (ht = 15 pt)
        Row r1 = getOrCreateRow(sheet, 1);
        r1.setHeightInPoints(15.0f);

        for (int c = leftStartCol; c <= leftEndCol; c++) {
            Cell cell = getOrCreateCell(r1, c);
            cell.setCellStyle(logoBoxStyle);
        }

        for (int c = centerStartCol; c <= centerEndCol; c++) {
            Cell cell = getOrCreateCell(r1, c);
            cell.setCellStyle(schoolStyle);
        }
        Cell centerCell1 = getOrCreateCell(r1, centerStartCol);
        String finalSchoolName = (schoolName != null && !schoolName.isBlank())
                ? schoolName.trim()
                : "School of Engineering and Technology";
        centerCell1.setCellValue(finalSchoolName);

        for (int c = rightStartCol; c <= rightEndCol; c++) {
            Cell cell = getOrCreateCell(r1, c);
            cell.setCellStyle(logoBoxStyle);
        }

        if (centerEndCol > centerStartCol) {
            sheet.addMergedRegion(new CellRangeAddress(1, 1, centerStartCol, centerEndCol));
        }

        // 5. Row 2: Spacer inside header box (ht = 15 pt)
        Row r2 = getOrCreateRow(sheet, 2);
        r2.setHeightInPoints(15.0f);

        for (int c = leftStartCol; c <= leftEndCol; c++) {
            Cell cell = getOrCreateCell(r2, c);
            cell.setCellStyle(logoBoxStyle);
        }

        for (int c = centerStartCol; c <= centerEndCol; c++) {
            Cell cell = getOrCreateCell(r2, c);
            cell.setCellStyle(boxSpacerStyle);
        }

        for (int c = rightStartCol; c <= rightEndCol; c++) {
            Cell cell = getOrCreateCell(r2, c);
            cell.setCellStyle(logoBoxStyle);
        }

        if (centerEndCol > centerStartCol) {
            sheet.addMergedRegion(new CellRangeAddress(2, 2, centerStartCol, centerEndCol));
        }

        // Merge Left Logo Slot across rows 0-2 if more than 1 row/col
        if (leftEndCol >= leftStartCol) {
            sheet.addMergedRegion(new CellRangeAddress(0, 2, leftStartCol, leftEndCol));
        }

        // Merge Right Logo Slot across rows 0-2 if more than 1 row/col
        if (rightEndCol >= rightStartCol) {
            sheet.addMergedRegion(new CellRangeAddress(0, 2, rightStartCol, rightEndCol));
        }

        // 6. Embed Logos using Apache POI Drawing Patriarch
        embedLogo(wb, sheet, leftLogoBytes, leftStartCol, 0, leftEndCol + 1, 3);
        embedLogo(wb, sheet, rightLogoBytes, rightStartCol, 0, rightEndCol + 1, 3);

        // 7. Row 3: Title Band (ht = 20.5 pt, full width 0 to endCol)
        Row r3 = getOrCreateRow(sheet, 3);
        r3.setHeightInPoints(20.5f);

        for (int c = 0; c <= endCol; c++) {
            Cell cell = getOrCreateCell(r3, c);
            cell.setCellStyle(titleBandStyle);
        }
        Cell titleCell = getOrCreateCell(r3, 0);
        String finalTitle = (reportTitle != null && !reportTitle.isBlank())
                ? reportTitle.trim()
                : "Course Outcome attainment Calculations through Direct Method";
        titleCell.setCellValue(finalTitle);

        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, endCol));

        // Configure Sheet Print & Page setup
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setLandscape(isLandscape);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setDisplayGridlines(true);

        if (includeBottomSpacer) {
            // Row 4: Spacer Row (ht = 14.25 pt)
            Row r4 = getOrCreateRow(sheet, 4);
            r4.setHeightInPoints(14.25f);
            return 5; // Next row index for report content
        } else {
            return 4; // Next row index for report content
        }
    }

    private static void embedLogo(
            Workbook wb,
            Sheet sheet,
            byte[] logoBytes,
            int col1,
            int row1,
            int col2,
            int row2) {

        if (logoBytes == null || logoBytes.length == 0) {
            return;
        }

        try {
            int pictureType = Workbook.PICTURE_TYPE_PNG;
            if (logoBytes.length > 2 && (logoBytes[0] & 0xFF) == 0xFF && (logoBytes[1] & 0xFF) == 0xD8) {
                pictureType = Workbook.PICTURE_TYPE_JPEG;
            }

            int pictureIdx = wb.addPicture(logoBytes, pictureType);
            CreationHelper helper = wb.getCreationHelper();

            Drawing<?> drawing = sheet.getDrawingPatriarch();
            if (drawing == null) {
                drawing = sheet.createDrawingPatriarch();
            }

            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(col1);
            anchor.setRow1(row1);
            anchor.setCol2(col2);
            anchor.setRow2(row2);

            // Subtle padding so logo does not touch the cell edges
            if (anchor instanceof XSSFClientAnchor xAnchor) {
                xAnchor.setDx1(Units.pixelToEMU(4));
                xAnchor.setDy1(Units.pixelToEMU(2));
                xAnchor.setDx2(-Units.pixelToEMU(4));
                xAnchor.setDy2(-Units.pixelToEMU(2));
            }

            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, pictureIdx);

        } catch (Exception e) {
            log.warn("Failed to embed logo into Course Excel header: {}", e.getMessage());
        }
    }

    private static CellStyle createUniversityStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createSchoolStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createHeaderBoxSpacerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createLogoSlotStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createTitleBandStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setBold(false); // Reference template has regular weight
        if (wb instanceof XSSFWorkbook) {
            ((XSSFFont) font).setColor(new XSSFColor(COLOR_HEADER_TEXT, null));
            ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(COLOR_TITLE_BAND_CYAN, null));
        } else {
            font.setColor(IndexedColors.BLACK.getIndex());
            style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Solid thin border enclosing the title band
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());

        return style;
    }

    private static Row getOrCreateRow(Sheet sheet, int rowIdx) {
        Row r = sheet.getRow(rowIdx);
        return r != null ? r : sheet.createRow(rowIdx);
    }

    private static Cell getOrCreateCell(Row row, int colIdx) {
        Cell c = row.getCell(colIdx);
        return c != null ? c : row.createCell(colIdx);
    }
}
