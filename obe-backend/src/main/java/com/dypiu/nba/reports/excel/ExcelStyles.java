package com.dypiu.nba.reports.excel;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.awt.Color;
import java.util.Comparator;

public class ExcelStyles {

    public static final Comparator<String> NATURAL_NUMERICAL_COMPARATOR = (c1, c2) -> {
        if (c1 == null && c2 == null) return 0;
        if (c1 == null) return -1;
        if (c2 == null) return 1;
        boolean isPso1 = c1.toUpperCase().startsWith("PSO");
        boolean isPso2 = c2.toUpperCase().startsWith("PSO");
        if (!isPso1 && isPso2) return -1;
        if (isPso1 && !isPso2) return 1;
        String d1 = c1.replaceAll("\\D+", "");
        String d2 = c2.replaceAll("\\D+", "");
        if (!d1.isEmpty() && !d2.isEmpty()) {
            try {
                int n1 = Integer.parseInt(d1);
                int n2 = Integer.parseInt(d2);
                if (n1 != n2) return Integer.compare(n1, n2);
            } catch (NumberFormatException ignored) {}
        }
        return c1.compareToIgnoreCase(c2);
    };

    public static CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 13);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public static CellStyle createSchoolTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public static CellStyle createSubTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public static CellStyle createMetaStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 9);
        font.setBold(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    public static CellStyle createHeaderStyle(Workbook wb, boolean isPso) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        if (isPso) {
            if (style instanceof XSSFCellStyle xStyle) {
                xStyle.setFillForegroundColor(new XSSFColor(new Color(226, 239, 218), null));
            } else {
                style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            }
            font.setColor(IndexedColors.DARK_GREEN.getIndex());
        } else {
            if (style instanceof XSSFCellStyle xStyle) {
                xStyle.setFillForegroundColor(new XSSFColor(new Color(217, 225, 242), null));
            } else {
                style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            }
            font.setColor(IndexedColors.BLACK.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    public static CellStyle createDataStyle(Workbook wb, boolean center, boolean bold) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        font.setBold(bold);
        style.setFont(font);
        style.setAlignment(center ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        DataFormat df = wb.createDataFormat();
        style.setDataFormat(df.getFormat("0.00"));
        setBorders(style);
        return style;
    }

    public static CellStyle createSummaryStyle(Workbook wb, boolean isPso) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        if (isPso) {
            if (style instanceof XSSFCellStyle xStyle) {
                xStyle.setFillForegroundColor(new XSSFColor(new Color(226, 239, 218), null));
            } else {
                style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            }
            font.setColor(IndexedColors.DARK_GREEN.getIndex());
        } else {
            if (style instanceof XSSFCellStyle xStyle) {
                xStyle.setFillForegroundColor(new XSSFColor(new Color(217, 225, 242), null));
            } else {
                style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            }
            font.setColor(IndexedColors.DARK_BLUE.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        DataFormat df = wb.createDataFormat();
        style.setDataFormat(df.getFormat("0.00"));
        setBorders(style);
        return style;
    }

    public static CellStyle createSummaryTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        if (style instanceof XSSFCellStyle xStyle) {
            xStyle.setFillForegroundColor(new XSSFColor(new Color(217, 225, 242), null));
        } else {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    public static CellStyle createSectionHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        if (style instanceof XSSFCellStyle xStyle) {
            xStyle.setFillForegroundColor(new XSSFColor(new Color(217, 225, 242), null));
        } else {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style);
        return style;
    }

    public static CellStyle createMintRowStyle(Workbook wb, boolean center, boolean bold) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        font.setBold(bold);
        style.setFont(font);
        style.setAlignment(center ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        DataFormat df = wb.createDataFormat();
        style.setDataFormat(df.getFormat("0.00"));
        setBorders(style);
        return style;
    }

    public static CellStyle createBlueRowStyle(Workbook wb, boolean center, boolean bold) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        font.setBold(bold);
        style.setFont(font);
        style.setAlignment(center ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        DataFormat df = wb.createDataFormat();
        style.setDataFormat(df.getFormat("0.00"));
        setBorders(style);
        return style;
    }

    public static CellStyle createStatusMetStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 9);
        font.setBold(true);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style);
        return style;
    }

    public static CellStyle createStatusGapStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 9);
        font.setBold(true);
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorders(style);
        return style;
    }

    public static void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
    }
}
