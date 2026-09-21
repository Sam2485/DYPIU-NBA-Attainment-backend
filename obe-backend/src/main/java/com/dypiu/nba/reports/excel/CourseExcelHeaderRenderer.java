package com.dypiu.nba.reports.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.*;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Authoritative fixed logo physical dimensions from reference template:
    // cx = 1402080 EMUs (110.4 pt = 147.2 px), cy = 524269 EMUs (41.28 pt = 55.0 px)
    public static final long FIXED_LOGO_WIDTH_EMU = 1402080L;
    public static final long FIXED_LOGO_HEIGHT_EMU = 524269L;

    // Target physical width for the fixed logo region (approx 78 pt = 1,000,000 EMUs)
    public static final long TARGET_LOGO_REGION_WIDTH_EMU = 1000000L;

    /**
     * Calculates the display academic year for Course Attainment sheets.
     * <p>
     * When a batch duration arrives (e.g. "2024-2028", "2025-2029"), converts it
     * into the single-year academic year corresponding to the course's semester of offering.
     * Each academic year consists of 2 semesters:
     * - Semester 1 or 2 -> Year 1 (offset 0): 2025 -> 2025-2026
     * - Semester 3 or 4 -> Year 2 (offset 1): 2025 -> 2026-2027
     * - Semester 5 or 6 -> Year 3 (offset 2): 2025 -> 2027-2028
     * - Semester 7 or 8 -> Year 4 (offset 3): 2025 -> 2028-2029
     * <p>
     * If the academic year is already a 1-year span (e.g. "2023-24", "2025-26", "2025-2026"),
     * it is preserved as-is.
     */
    public static String calculateCourseAcademicYear(String rawAcademicYear, Integer semester) {
        if (rawAcademicYear == null || rawAcademicYear.isBlank()) {
            return "";
        }
        String trimmed = rawAcademicYear.trim();
        String prefix = "";
        String yearSpan = trimmed;
        if (trimmed.toUpperCase().startsWith("AY ")) {
            prefix = trimmed.substring(0, 3);
            yearSpan = trimmed.substring(3).trim();
        } else if (trimmed.toUpperCase().startsWith("AY")) {
            prefix = trimmed.substring(0, 2) + " ";
            yearSpan = trimmed.substring(2).trim();
        }

        Pattern pattern = Pattern.compile("^(\\d{4})\\s*-\\s*(\\d{4})$");
        Matcher matcher = pattern.matcher(yearSpan);
        if (matcher.matches()) {
            int startYear = Integer.parseInt(matcher.group(1));
            int endYear = Integer.parseInt(matcher.group(2));
            if (endYear - startYear > 1) {
                int sem = (semester != null && semester > 0) ? semester : 1;
                int yearOffset = (sem - 1) / 2;
                int calcStart = startYear + yearOffset;
                int calcEnd = calcStart + 1;
                return prefix + calcStart + "-" + calcEnd;
            }
            return trimmed;
        }

        return trimmed;
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

        // 1. Determine Left and Right Logo Regions based on FIXED PHYSICAL WIDTH (never fixed column counts)
        // Center flexible region must always retain sufficient space and absorb width
        int minCenterCols = (totalColumns >= 6) ? 2 : 1;
        int maxSideCols = Math.max(1, (totalColumns - minCenterCols) / 2);

        int leftStartCol = 0;
        int leftEndCol = 0;
        long leftRegionWidthEmu = 0;
        while (leftEndCol < maxSideCols - 1) {
            long w = Units.columnWidthToEMU(sheet.getColumnWidth(leftEndCol));
            leftRegionWidthEmu += w;
            if (leftRegionWidthEmu >= TARGET_LOGO_REGION_WIDTH_EMU) {
                break;
            }
            leftEndCol++;
        }

        int rightEndCol = endCol;
        int rightStartCol = endCol;
        long rightRegionWidthEmu = 0;
        while (rightStartCol > endCol - (maxSideCols - 1)) {
            long w = Units.columnWidthToEMU(sheet.getColumnWidth(rightStartCol));
            rightRegionWidthEmu += w;
            if (rightRegionWidthEmu >= TARGET_LOGO_REGION_WIDTH_EMU) {
                break;
            }
            rightStartCol--;
        }

        int centerStartCol = leftEndCol + 1;
        int centerEndCol = rightStartCol - 1;

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

        // 6. Embed Logos Centered with FIXED Physical Size inside the fixed regions
        embedCenteredLogo(wb, sheet, leftLogoBytes, leftStartCol, leftEndCol, 0, 2, FIXED_LOGO_WIDTH_EMU, FIXED_LOGO_HEIGHT_EMU);
        embedCenteredLogo(wb, sheet, rightLogoBytes, rightStartCol, rightEndCol, 0, 2, FIXED_LOGO_WIDTH_EMU, FIXED_LOGO_HEIGHT_EMU);

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

            if (picture instanceof XSSFPicture xPic) {
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
            log.warn("Failed to embed centered logo into Course Excel header: {}", e.getMessage());
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
