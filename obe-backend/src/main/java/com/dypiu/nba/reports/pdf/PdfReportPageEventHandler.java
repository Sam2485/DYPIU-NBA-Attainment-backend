package com.dypiu.nba.reports.pdf;

import com.dypiu.nba.reports.template.FooterConfig;
import com.dypiu.nba.reports.template.HeaderConfig;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class PdfReportPageEventHandler extends com.lowagie.text.pdf.PdfPageEventHelper {

    private final HeaderConfig headerConfig;
    private final FooterConfig footerConfig;
    private final String reportTitle;
    private final String reportId;
    private final ZonedDateTime generatedAt;
    private PdfTemplate totalPagesTemplate;
    private BaseFont baseFont;

    public PdfReportPageEventHandler(HeaderConfig headerConfig, FooterConfig footerConfig, String reportTitle, String reportId, ZonedDateTime generatedAt) {
        this.headerConfig = headerConfig != null ? headerConfig : HeaderConfig.builder().build();
        this.footerConfig = footerConfig != null ? footerConfig : FooterConfig.builder().build();
        this.reportTitle = reportTitle;
        this.reportId = reportId;
        this.generatedAt = generatedAt != null ? generatedAt : ZonedDateTime.now();
    }

    public PdfReportPageEventHandler(HeaderConfig headerConfig, FooterConfig footerConfig, String reportTitle, ZonedDateTime generatedAt) {
        this(headerConfig, footerConfig, reportTitle, null, generatedAt);
    }

    @Override
    public void onOpenDocument(PdfWriter writer, Document document) {
        totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
        try {
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            // fallback
        }
    }

    @Override
    public void onEndPage(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        float pageBottom = document.bottom() - 16;
        float left = document.left();
        float right = document.right();

        // Footer line
        cb.setColorStroke(new Color(203, 213, 225));
        cb.setLineWidth(0.5f);
        cb.moveTo(left, pageBottom + 12);
        cb.lineTo(right, pageBottom + 12);
        cb.stroke();

        // Footer text
        String footerText = (footerConfig != null && footerConfig.getStandardFooterText() != null)
                ? footerConfig.getStandardFooterText()
                : "NBA Attainment System · Authoritative Academic Record";

        String timestampStr = generatedAt.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm z"));
        String footerLeft = (reportId != null && !reportId.isBlank() ? "Report ID: " + reportId + "  |  " : "")
                + footerText + "  |  " + timestampStr;

        Font footerFont = new Font(Font.HELVETICA, 7.0f, Font.NORMAL, new Color(100, 116, 139));
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(footerLeft, footerFont), left, pageBottom, 0);

        if (footerConfig != null && Boolean.TRUE.equals(footerConfig.getShowPageNumbers())) {
            int pageNum = writer.getPageNumber();
            String pageStr = "Page " + pageNum + " of ";
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(pageStr, footerFont), right - 12, pageBottom, 0);
            cb.addTemplate(totalPagesTemplate, right - 12, pageBottom - 1);
        }
    }

    @Override
    public void onCloseDocument(PdfWriter writer, Document document) {
        if (totalPagesTemplate != null && baseFont != null) {
            totalPagesTemplate.beginText();
            totalPagesTemplate.setFontAndSize(baseFont, 7.0f);
            totalPagesTemplate.setColorFill(new Color(100, 116, 139));
            totalPagesTemplate.showText(String.valueOf(writer.getPageNumber() - 1));
            totalPagesTemplate.endText();
        }
    }
}
