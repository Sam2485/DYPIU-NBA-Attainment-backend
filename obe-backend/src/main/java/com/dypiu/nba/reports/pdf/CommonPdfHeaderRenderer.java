package com.dypiu.nba.reports.pdf;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class CommonPdfHeaderRenderer {

    private static final Color COLOR_PRIMARY    = new Color(30, 41, 59);   // #1E293B Navy
    private static final Color COLOR_SLATE      = new Color(51, 65, 85);   // #334155 Slate
    private static final Color COLOR_MUTED      = new Color(100, 116, 139);// #64748B
    private static final Color COLOR_BORDER     = new Color(148, 163, 184);// #94A3B8
    private static final Color COLOR_BG_HEADER  = new Color(241, 245, 249);// #F1F5F9 Light Slate Tint

    private static final Font FONT_INST_NAME  = new Font(Font.HELVETICA, 10.5f, Font.BOLD, COLOR_PRIMARY);
    private static final Font FONT_SCHOOL_NAME= new Font(Font.HELVETICA, 9.5f, Font.BOLD, COLOR_SLATE);
    private static final Font FONT_REP_TITLE  = new Font(Font.HELVETICA, 10.0f, Font.BOLD, COLOR_PRIMARY);
    private static final Font FONT_META_LABEL = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_MUTED);
    private static final Font FONT_META_VALUE = new Font(Font.HELVETICA, 8.0f, Font.BOLD, COLOR_PRIMARY);
    private static final Font FONT_SCOPE_VAL  = new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_PRIMARY);

    @Data
    @Builder
    public static class HeaderContext {
        private String institutionName;
        private String schoolName;
        private String reportTitle;
        private String academicYear;
        private String termOrSemester;
        private String scopeLabelAndValue;
        private String revision;
        private ZonedDateTime generatedAt;
        private byte[] leftLogoBytes;
        private byte[] rightLogoBytes;
        private boolean isLandscape;
    }

    public static void renderHeader(Document document, HeaderContext ctx) throws DocumentException {
        float[] colWidths = ctx.isLandscape()
                ? new float[]{110f, 582f, 110f}
                : new float[]{85f, 385f, 85f};

        PdfPTable table = new PdfPTable(colWidths);
        table.setWidthPercentage(100);
        table.setSpacingAfter(8f);

        // --- ROW 1: LEFT LOGO | INSTITUTION & SCHOOL NAME | RIGHT LOGO ---
        // 1. Left Logo
        PdfPCell leftLogoCell = buildLogoCell(ctx.getLeftLogoBytes(), ctx.isLandscape());
        table.addCell(leftLogoCell);

        // 2. Center Institution & School Title
        PdfPCell instCell = new PdfPCell();
        instCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        instCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        instCell.setPadding(4f);
        setCellBorder(instCell);

        Paragraph instPara = new Paragraph(
                ctx.getInstitutionName() != null && !ctx.getInstitutionName().isBlank()
                        ? ctx.getInstitutionName().toUpperCase()
                        : "D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE",
                FONT_INST_NAME);
        instPara.setAlignment(Element.ALIGN_CENTER);
        instCell.addElement(instPara);

        Paragraph schoolPara = new Paragraph(
                ctx.getSchoolName() != null && !ctx.getSchoolName().isBlank()
                        ? ctx.getSchoolName()
                        : "School of Engineering and Technology",
                FONT_SCHOOL_NAME);
        schoolPara.setAlignment(Element.ALIGN_CENTER);
        instCell.addElement(schoolPara);

        table.addCell(instCell);

        // 3. Right Logo
        PdfPCell rightLogoCell = buildLogoCell(ctx.getRightLogoBytes(), ctx.isLandscape());
        table.addCell(rightLogoCell);

        // --- ROW 2: ACADEMIC YEAR | REPORT TITLE | REVISION & DATED ---
        // 1. Academic Year (Tinted Background)
        PdfPCell ayCell = new PdfPCell();
        ayCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        ayCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        ayCell.setBackgroundColor(COLOR_BG_HEADER);
        ayCell.setPadding(3.5f);
        setCellBorder(ayCell);
        Paragraph ayPara = new Paragraph();
        ayPara.add(new Chunk("Academic Year:\n", FONT_META_LABEL));
        ayPara.add(new Chunk(ctx.getAcademicYear() != null && !ctx.getAcademicYear().isBlank() ? ctx.getAcademicYear() : "—", FONT_META_VALUE));
        ayPara.setAlignment(Element.ALIGN_CENTER);
        ayCell.addElement(ayPara);
        table.addCell(ayCell);

        // 2. Report Title
        PdfPCell titleCell = new PdfPCell();
        titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titleCell.setPadding(3.5f);
        setCellBorder(titleCell);
        Paragraph titlePara = new Paragraph(ctx.getReportTitle() != null ? ctx.getReportTitle().toUpperCase() : "ACADEMIC REPORT", FONT_REP_TITLE);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titleCell.addElement(titlePara);
        table.addCell(titleCell);

        // 3. Revision (Tinted Background)
        PdfPCell revCell = new PdfPCell();
        revCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        revCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        revCell.setBackgroundColor(COLOR_BG_HEADER);
        revCell.setPadding(3.5f);
        setCellBorder(revCell);
        Paragraph revPara = new Paragraph();
        revPara.add(new Chunk("Revision : ", FONT_META_LABEL));
        revPara.add(new Chunk(ctx.getRevision() != null && !ctx.getRevision().isBlank() ? ctx.getRevision() : "01", FONT_META_VALUE));
        revPara.setAlignment(Element.ALIGN_CENTER);
        revCell.addElement(revPara);
        table.addCell(revCell);

        // --- ROW 3: TERM / SEMESTER | SCOPE (PROGRAMME/COURSE) | DATED & DATE OF PREPARATION ---
        // 1. Term / Semester (Tinted Background)
        PdfPCell semCell = new PdfPCell();
        semCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        semCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        semCell.setBackgroundColor(COLOR_BG_HEADER);
        semCell.setPadding(3.5f);
        setCellBorder(semCell);
        Paragraph semPara = new Paragraph();
        semPara.add(new Chunk("Term / Semester:\n", FONT_META_LABEL));
        semPara.add(new Chunk(ctx.getTermOrSemester() != null && !ctx.getTermOrSemester().isBlank() ? ctx.getTermOrSemester() : "All Semesters", FONT_META_VALUE));
        semPara.setAlignment(Element.ALIGN_CENTER);
        semCell.addElement(semPara);
        table.addCell(semCell);

        // 2. Scope (Programme / Course)
        PdfPCell scopeCell = new PdfPCell();
        scopeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        scopeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        scopeCell.setPadding(3.5f);
        setCellBorder(scopeCell);
        Paragraph scopePara = new Paragraph(ctx.getScopeLabelAndValue() != null ? ctx.getScopeLabelAndValue() : "Programme / Course", FONT_SCOPE_VAL);
        scopePara.setAlignment(Element.ALIGN_CENTER);
        scopeCell.addElement(scopePara);
        table.addCell(scopeCell);

        // 3. Dates (Tinted Background)
        PdfPCell dateCell = new PdfPCell();
        dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        dateCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        dateCell.setBackgroundColor(COLOR_BG_HEADER);
        dateCell.setPadding(3.5f);
        setCellBorder(dateCell);
        String dateStr = (ctx.getGeneratedAt() != null ? ctx.getGeneratedAt() : ZonedDateTime.now())
                .format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
        Paragraph datePara = new Paragraph();
        datePara.add(new Chunk("Date of Prep:\n", FONT_META_LABEL));
        datePara.add(new Chunk(dateStr, FONT_META_VALUE));
        datePara.setAlignment(Element.ALIGN_CENTER);
        dateCell.addElement(datePara);
        table.addCell(dateCell);

        document.add(table);
    }

    private static PdfPCell buildLogoCell(byte[] logoBytes, boolean isLandscape) {
        PdfPCell cell = new PdfPCell();
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        setCellBorder(cell);

        if (logoBytes != null && logoBytes.length > 0) {
            try {
                Image img = Image.getInstance(logoBytes);
                img.setAlignment(Element.ALIGN_CENTER);
                float maxW = isLandscape ? 85f : 65f;
                float maxH = isLandscape ? 38f : 32f;
                img.scaleToFit(maxW, maxH);
                cell.addElement(img);
                return cell;
            } catch (Exception e) {
                log.warn("Failed to parse logo image bytes for PDF header: {}", e.getMessage());
            }
        }

        // Clean placeholder if logo is empty
        Paragraph p = new Paragraph("DYPIU", new Font(Font.HELVETICA, 8f, Font.BOLD, COLOR_MUTED));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        return cell;
    }

    private static void setCellBorder(PdfPCell cell) {
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
    }
}
