package com.dypiu.nba.reports.pdf;

import com.dypiu.nba.reports.model.ReportSection;
import com.dypiu.nba.reports.model.snapshot.*;
import com.dypiu.nba.reports.template.FooterConfig;
import com.dypiu.nba.reports.template.HeaderConfig;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

@Component
@Slf4j
public class PdfReportRenderer {

    private static final Color COLOR_PRIMARY    = new Color(30, 41, 59);   // #1E293B Navy
    private static final Color COLOR_SLATE      = new Color(51, 65, 85);   // #334155 Slate
    private static final Color COLOR_TEXT_DARK  = new Color(15, 23, 42);   // #0F172A
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);// #64748B
    private static final Color COLOR_BG_HEADER  = new Color(241, 245, 249);// #F1F5F9
    private static final Color COLOR_BG_ZEBRA   = new Color(248, 250, 252);// #F8FAFC
    private static final Color COLOR_BG_PSO     = new Color(236, 253, 245);// #ECFDF5
    private static final Color COLOR_TEXT_PSO   = new Color(4, 120, 87);   // #047857
    private static final Color COLOR_BORDER     = new Color(203, 213, 225);// #CBD5E1
    private static final Color COLOR_ROW_MINT   = new Color(220, 240, 224);// #DCF0E0
    private static final Color COLOR_ROW_BLUE   = new Color(180, 223, 227);// #B4DFE3
    private static final Color COLOR_STATUS_MET = new Color(22, 163, 74);  // #16A34A
    private static final Color COLOR_STATUS_GAP = new Color(220, 38, 38);  // #DC2626
    private static final Color COLOR_MET_BG     = new Color(220, 252, 231);// #DCFCE7
    private static final Color COLOR_GAP_BG     = new Color(254, 226, 226);// #FEE2E2

    // --- RENDER PROGRAMME ATTAINMENT MASTER (All 4 Sections on Fresh Pages) ---
    public byte[] renderProgrammeAttainmentMaster(ProgrammeAttainmentSnapshot snapshot, ReportTemplateDto template, byte[] leftLogo, byte[] rightLogo) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 30);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            HeaderConfig header = template != null ? template.getHeaderConfig() : HeaderConfig.builder().build();
            FooterConfig footer = template != null ? template.getFooterConfig() : FooterConfig.builder().build();
            writer.setPageEvent(new PdfReportPageEventHandler(header, footer, "Programme Attainment Master Report", snapshot.getReportId(), snapshot.getGeneratedAt()));

            document.open();

            // Section 1: Average Mapping
            CommonPdfHeaderRenderer.renderHeader(document, buildHeaderContext(snapshot, template, "PROGRAMME ATTAINMENT REPORT — AVERAGE MAPPING", "All Semesters (Sem 1–8)", leftLogo, rightLogo, true));
            renderAverageMappingTable(document, snapshot);
            document.newPage();

            // Section 2: Average Direct Attainment
            CommonPdfHeaderRenderer.renderHeader(document, buildHeaderContext(snapshot, template, "PROGRAMME ATTAINMENT REPORT — AVERAGE DIRECT ATTAINMENT", "All Semesters (Sem 1–8)", leftLogo, rightLogo, true));
            renderAverageDirectTable(document, snapshot);
            document.newPage();

            // Section 3: Average Indirect Attainment
            CommonPdfHeaderRenderer.renderHeader(document, buildHeaderContext(snapshot, template, "PROGRAMME ATTAINMENT REPORT — AVERAGE INDIRECT ATTAINMENT (EXIT SURVEY)", "Graduate Exit Survey", leftLogo, rightLogo, true));
            renderAverageIndirectTable(document, snapshot);
            document.newPage();

            // Section 4: Overall Attainment
            CommonPdfHeaderRenderer.renderHeader(document, buildHeaderContext(snapshot, template, "PROGRAMME ATTAINMENT REPORT — OVERALL ATTAINMENT (80% DIRECT + 20% INDIRECT)", "Batch Summary", leftLogo, rightLogo, true));
            renderOverallAttainmentTable(document, snapshot);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render Programme Attainment Master PDF", e);
        }
    }

    public byte[] renderProgrammeAttainmentMaster(ProgrammeAttainmentSnapshot snapshot, ReportTemplateDto template) {
        return renderProgrammeAttainmentMaster(snapshot, template, null, null);
    }

    // --- RENDER PROGRAMME ATTAINMENT SECTION ---
    public byte[] renderProgrammeAttainmentSection(ProgrammeAttainmentSnapshot snapshot, ReportSection section, ReportTemplateDto template, byte[] leftLogo, byte[] rightLogo) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 30);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            HeaderConfig header = template != null ? template.getHeaderConfig() : HeaderConfig.builder().build();
            FooterConfig footer = template != null ? template.getFooterConfig() : FooterConfig.builder().build();
            String title = getSectionTitle(section);
            writer.setPageEvent(new PdfReportPageEventHandler(header, footer, title, snapshot.getReportId(), snapshot.getGeneratedAt()));

            document.open();

            String term = (section == ReportSection.AVERAGE_INDIRECT) ? "Graduate Exit Survey" : "All Semesters (Sem 1–8)";
            CommonPdfHeaderRenderer.renderHeader(document, buildHeaderContext(snapshot, template, title, term, leftLogo, rightLogo, true));

            switch (section) {
                case AVERAGE_MAPPING -> renderAverageMappingTable(document, snapshot);
                case AVERAGE_DIRECT -> renderAverageDirectTable(document, snapshot);
                case AVERAGE_INDIRECT -> renderAverageIndirectTable(document, snapshot);
                case OVERALL -> renderOverallAttainmentTable(document, snapshot);
                case ALL -> {
                    renderAverageMappingTable(document, snapshot);
                    document.newPage();
                    CommonPdfHeaderRenderer.renderHeader(document, buildHeaderContext(snapshot, template, "PROGRAMME ATTAINMENT REPORT — AVERAGE DIRECT ATTAINMENT", "All Semesters (Sem 1–8)", leftLogo, rightLogo, true));
                    renderAverageDirectTable(document, snapshot);
                    document.newPage();
                    CommonPdfHeaderRenderer.renderHeader(document, buildHeaderContext(snapshot, template, "PROGRAMME ATTAINMENT REPORT — AVERAGE INDIRECT ATTAINMENT (EXIT SURVEY)", "Graduate Exit Survey", leftLogo, rightLogo, true));
                    renderAverageIndirectTable(document, snapshot);
                    document.newPage();
                    CommonPdfHeaderRenderer.renderHeader(document, buildHeaderContext(snapshot, template, "PROGRAMME ATTAINMENT REPORT — OVERALL ATTAINMENT (80% DIRECT + 20% INDIRECT)", "Batch Summary", leftLogo, rightLogo, true));
                    renderOverallAttainmentTable(document, snapshot);
                }
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render Programme Attainment Section PDF: " + section, e);
        }
    }

    public byte[] renderProgrammeAttainmentSection(ProgrammeAttainmentSnapshot snapshot, ReportSection section, ReportTemplateDto template) {
        return renderProgrammeAttainmentSection(snapshot, section, template, null, null);
    }

    // --- RENDER COURSE ATTAINMENT ---
    public byte[] renderCourseAttainment(CourseAttainmentSnapshot snapshot, ReportTemplateDto template, byte[] leftLogo, byte[] rightLogo) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 30);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            HeaderConfig header = template != null ? template.getHeaderConfig() : HeaderConfig.builder().build();
            FooterConfig footer = template != null ? template.getFooterConfig() : FooterConfig.builder().build();
            writer.setPageEvent(new PdfReportPageEventHandler(header, footer, "Course Attainment Report", snapshot.getReportId(), snapshot.getGeneratedAt()));

            document.open();

            String termStr = "Semester " + (snapshot.getSemester() != null ? snapshot.getSemester() : "—");
            String courseScope = "Course : " + (snapshot.getCourseName() != null ? snapshot.getCourseName() : "")
                    + (snapshot.getCourseCode() != null && !snapshot.getCourseCode().isBlank() ? " (" + snapshot.getCourseCode() + ")" : "");

            CommonPdfHeaderRenderer.HeaderContext ctx = CommonPdfHeaderRenderer.HeaderContext.builder()
                    .institutionName(snapshot.getInstitutionName())
                    .schoolName(snapshot.getSchoolName())
                    .reportTitle("COURSE ATTAINMENT CONSOLIDATED REPORT")
                    .academicYear(snapshot.getAcademicYear() != null ? snapshot.getAcademicYear() : (snapshot.getBatchName() != null ? snapshot.getBatchName() : "—"))
                    .termOrSemester(termStr)
                    .scopeLabelAndValue(courseScope)
                    .revision("01")
                    .generatedAt(snapshot.getGeneratedAt())
                    .leftLogoBytes(leftLogo)
                    .rightLogoBytes(rightLogo)
                    .isLandscape(true)
                    .build();

            CommonPdfHeaderRenderer.renderHeader(document, ctx);

            // 1. Attainment Configuration Summary Box
            renderCourseAttainmentConfigSummary(document, snapshot);

            // 2. Table 1: Combined Mapping Matrix
            renderCourseTable1(document, snapshot);

            // 3. Table 2: PO & PSO Direct Attainment Contribution
            renderCourseTable2(document, snapshot);

            // 4. Table 3: CO Attainment Breakdown
            renderCourseTable3(document, snapshot);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render Course Attainment PDF", e);
        }
    }

    public byte[] renderCourseAttainment(CourseAttainmentSnapshot snapshot, ReportTemplateDto template) {
        return renderCourseAttainment(snapshot, template, null, null);
    }

    // --- RENDER PROGRAMME ATR (Portrait) ---
    public byte[] renderProgrammeAtr(ProgrammeAtrSnapshot snapshot, ReportTemplateDto template, byte[] leftLogo, byte[] rightLogo) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 20, 20, 20, 30);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            HeaderConfig header = template != null ? template.getHeaderConfig() : HeaderConfig.builder().build();
            FooterConfig footer = template != null ? template.getFooterConfig() : FooterConfig.builder().build();
            writer.setPageEvent(new PdfReportPageEventHandler(header, footer, "Programme Action Taken Report (ATR)", snapshot.getReportId(), snapshot.getGeneratedAt()));

            document.open();

            String progScope = "Programme : " + (snapshot.getMasterProgrammeName() != null ? snapshot.getMasterProgrammeName() : "")
                    + (snapshot.getMasterProgrammeCode() != null && !snapshot.getMasterProgrammeCode().isBlank() ? " (" + snapshot.getMasterProgrammeCode() + ")" : "");

            CommonPdfHeaderRenderer.HeaderContext ctx = CommonPdfHeaderRenderer.HeaderContext.builder()
                    .institutionName(snapshot.getInstitutionName())
                    .schoolName(snapshot.getSchoolName())
                    .reportTitle("PROGRAMME ACTION TAKEN REPORT (ATR)")
                    .academicYear(snapshot.getAcademicYear() != null ? snapshot.getAcademicYear() : (snapshot.getBatchName() != null ? snapshot.getBatchName() : "—"))
                    .termOrSemester("All Semesters (Sem 1–8)")
                    .scopeLabelAndValue(progScope)
                    .revision("01")
                    .generatedAt(snapshot.getGeneratedAt())
                    .leftLogoBytes(leftLogo)
                    .rightLogoBytes(rightLogo)
                    .isLandscape(false)
                    .build();

            CommonPdfHeaderRenderer.renderHeader(document, ctx);

            renderProgrammeAtrDocument(document, snapshot);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render Programme ATR PDF", e);
        }
    }

    public byte[] renderProgrammeAtr(ProgrammeAtrSnapshot snapshot, ReportTemplateDto template) {
        return renderProgrammeAtr(snapshot, template, null, null);
    }

    // --- RENDER COURSE ATR (Portrait) ---
    public byte[] renderCourseAtr(CourseAtrSnapshot snapshot, ReportTemplateDto template, byte[] leftLogo, byte[] rightLogo) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 20, 20, 20, 30);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            HeaderConfig header = template != null ? template.getHeaderConfig() : HeaderConfig.builder().build();
            FooterConfig footer = template != null ? template.getFooterConfig() : FooterConfig.builder().build();
            writer.setPageEvent(new PdfReportPageEventHandler(header, footer, "Course Action Taken Report (ATR)", snapshot.getReportId(), snapshot.getGeneratedAt()));

            document.open();

            String termStr = "Semester " + (snapshot.getSemester() != null ? snapshot.getSemester() : "—");
            String courseScope = "Course : " + (snapshot.getCourseName() != null ? snapshot.getCourseName() : "")
                    + (snapshot.getCourseCode() != null && !snapshot.getCourseCode().isBlank() ? " (" + snapshot.getCourseCode() + ")" : "");

            CommonPdfHeaderRenderer.HeaderContext ctx = CommonPdfHeaderRenderer.HeaderContext.builder()
                    .institutionName(snapshot.getInstitutionName())
                    .schoolName(snapshot.getSchoolName())
                    .reportTitle("COURSE ACTION TAKEN REPORT (ATR)")
                    .academicYear(snapshot.getAcademicYear() != null ? snapshot.getAcademicYear() : (snapshot.getBatchName() != null ? snapshot.getBatchName() : "—"))
                    .termOrSemester(termStr)
                    .scopeLabelAndValue(courseScope)
                    .revision("01")
                    .generatedAt(snapshot.getGeneratedAt())
                    .leftLogoBytes(leftLogo)
                    .rightLogoBytes(rightLogo)
                    .isLandscape(false)
                    .build();

            CommonPdfHeaderRenderer.renderHeader(document, ctx);

            renderCourseAtrDocument(document, snapshot);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render Course ATR PDF", e);
        }
    }

    public byte[] renderCourseAtr(CourseAtrSnapshot snapshot, ReportTemplateDto template) {
        return renderCourseAtr(snapshot, template, null, null);
    }

    // =========================================================================
    //  TABLE BUILDERS (PROGRAMME ATTAINMENT)
    // =========================================================================

    private void renderAverageMappingTable(Document document, ProgrammeAttainmentSnapshot snapshot) throws DocumentException {
        List<String> pos = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psos = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();
        int totalCols = 3 + pos.size() + psos.size();

        float[] widths = computeMatrixColumnWidths(pos.size(), psos.size());
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        float fs = computeDynamicFontSize(pos.size() + psos.size());
        Font fTh = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);
        Font fThPso = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_PSO);
        Font fTd = new Font(Font.HELVETICA, fs, Font.NORMAL, COLOR_TEXT_DARK);
        Font fTdBold = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);
        Font fTdPso = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_PSO);
        Font fSummary = new Font(Font.HELVETICA, fs + 0.5f, Font.BOLD, COLOR_PRIMARY);

        // Header
        addCell(table, "Sem", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Course Code", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Course Name", fTh, COLOR_BG_HEADER, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, po, fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, pso, fThPso, COLOR_BG_PSO, Element.ALIGN_CENTER);

        // Group rows by semester
        if (snapshot.getSection1AverageMapping() != null && snapshot.getSection1AverageMapping().getCourses() != null) {
            Map<Integer, List<ProgrammeAttainmentSnapshot.CourseMappingRow>> grouped = new LinkedHashMap<>();
            for (ProgrammeAttainmentSnapshot.CourseMappingRow r : snapshot.getSection1AverageMapping().getCourses()) {
                int sem = r.getSemester() != null ? r.getSemester() : 0;
                grouped.computeIfAbsent(sem, k -> new ArrayList<>()).add(r);
            }

            for (Map.Entry<Integer, List<ProgrammeAttainmentSnapshot.CourseMappingRow>> entry : grouped.entrySet()) {
                List<ProgrammeAttainmentSnapshot.CourseMappingRow> rows = entry.getValue();
                for (int i = 0; i < rows.size(); i++) {
                    ProgrammeAttainmentSnapshot.CourseMappingRow r = rows.get(i);
                    Color bg = (entry.getKey() % 2 == 0) ? COLOR_BG_ZEBRA : Color.WHITE;

                    if (i == 0) {
                        PdfPCell semCell = new PdfPCell(new Phrase(entry.getKey() > 0 ? "Sem " + entry.getKey() : "—", fTdBold));
                        semCell.setRowspan(rows.size());
                        semCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        semCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        semCell.setBackgroundColor(COLOR_BG_HEADER);
                        setCellBorder(semCell);
                        table.addCell(semCell);
                    }

                    addCell(table, r.getCourseCode(), fTdBold, bg, Element.ALIGN_CENTER);
                    addCell(table, r.getCourseName(), fTd, bg, Element.ALIGN_LEFT);

                    for (String po : pos) {
                        BigDecimal v = r.getPoValues() != null ? r.getPoValues().get(po) : null;
                        addCell(table, formatVal(v), fTd, bg, Element.ALIGN_CENTER);
                    }
                    for (String pso : psos) {
                        BigDecimal v = r.getPsoValues() != null ? r.getPsoValues().get(pso) : null;
                        addCell(table, formatVal(v), fTdPso, COLOR_BG_PSO, Element.ALIGN_CENTER);
                    }
                }
            }
        }

        // Summary Row
        PdfPCell sumLabel = new PdfPCell(new Phrase("Average Mapping Strength", fSummary));
        sumLabel.setColspan(3);
        sumLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sumLabel.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sumLabel.setBackgroundColor(COLOR_BG_HEADER);
        sumLabel.setPadding(4f);
        setCellBorder(sumLabel);
        table.addCell(sumLabel);

        Map<String, BigDecimal> avgMap = (snapshot.getSection1AverageMapping() != null)
                ? snapshot.getSection1AverageMapping().getAverageMappingStrength() : Map.of();

        for (String po : pos) {
            BigDecimal v = avgMap != null ? avgMap.get(po) : null;
            addCell(table, formatVal(v), fSummary, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        }
        for (String pso : psos) {
            BigDecimal v = avgMap != null ? avgMap.get(pso) : null;
            addCell(table, formatVal(v), fTdPso, COLOR_BG_PSO, Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    private void renderAverageDirectTable(Document document, ProgrammeAttainmentSnapshot snapshot) throws DocumentException {
        List<String> pos = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psos = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();

        float[] widths = computeMatrixColumnWidths(pos.size(), psos.size());
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        float fs = computeDynamicFontSize(pos.size() + psos.size());
        Font fTh = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);
        Font fThPso = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_PSO);
        Font fTd = new Font(Font.HELVETICA, fs, Font.NORMAL, COLOR_TEXT_DARK);
        Font fTdBold = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);
        Font fTdPso = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_PSO);
        Font fSummary = new Font(Font.HELVETICA, fs + 0.5f, Font.BOLD, COLOR_PRIMARY);

        // Header
        addCell(table, "Sem", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Course Code", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Course Name", fTh, COLOR_BG_HEADER, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, po, fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, pso, fThPso, COLOR_BG_PSO, Element.ALIGN_CENTER);

        // Group rows by semester
        if (snapshot.getSection2AverageDirect() != null && snapshot.getSection2AverageDirect().getCourses() != null) {
            Map<Integer, List<ProgrammeAttainmentSnapshot.CourseDirectRow>> grouped = new LinkedHashMap<>();
            for (ProgrammeAttainmentSnapshot.CourseDirectRow r : snapshot.getSection2AverageDirect().getCourses()) {
                int sem = r.getSemester() != null ? r.getSemester() : 0;
                grouped.computeIfAbsent(sem, k -> new ArrayList<>()).add(r);
            }

            for (Map.Entry<Integer, List<ProgrammeAttainmentSnapshot.CourseDirectRow>> entry : grouped.entrySet()) {
                List<ProgrammeAttainmentSnapshot.CourseDirectRow> rows = entry.getValue();
                for (int i = 0; i < rows.size(); i++) {
                    ProgrammeAttainmentSnapshot.CourseDirectRow r = rows.get(i);
                    Color bg = (entry.getKey() % 2 == 0) ? COLOR_BG_ZEBRA : Color.WHITE;

                    if (i == 0) {
                        PdfPCell semCell = new PdfPCell(new Phrase(entry.getKey() > 0 ? "Sem " + entry.getKey() : "—", fTdBold));
                        semCell.setRowspan(rows.size());
                        semCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        semCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        semCell.setBackgroundColor(COLOR_BG_HEADER);
                        setCellBorder(semCell);
                        table.addCell(semCell);
                    }

                    addCell(table, r.getCourseCode(), fTdBold, bg, Element.ALIGN_CENTER);
                    addCell(table, r.getCourseName(), fTd, bg, Element.ALIGN_LEFT);

                    for (String po : pos) {
                        BigDecimal v = r.getPoValues() != null ? r.getPoValues().get(po) : null;
                        addCell(table, formatVal(v), fTd, bg, Element.ALIGN_CENTER);
                    }
                    for (String pso : psos) {
                        BigDecimal v = r.getPsoValues() != null ? r.getPsoValues().get(pso) : null;
                        addCell(table, formatVal(v), fTdPso, COLOR_BG_PSO, Element.ALIGN_CENTER);
                    }
                }
            }
        }

        // Summary Row
        PdfPCell sumLabel = new PdfPCell(new Phrase("Average Attainment (Direct)", fSummary));
        sumLabel.setColspan(3);
        sumLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sumLabel.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sumLabel.setBackgroundColor(COLOR_BG_HEADER);
        sumLabel.setPadding(4f);
        setCellBorder(sumLabel);
        table.addCell(sumLabel);

        Map<String, BigDecimal> avgMap = (snapshot.getSection2AverageDirect() != null)
                ? snapshot.getSection2AverageDirect().getAverageDirectAttainment() : Map.of();

        for (String po : pos) {
            BigDecimal v = avgMap != null ? avgMap.get(po) : null;
            addCell(table, formatVal(v), fSummary, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        }
        for (String pso : psos) {
            BigDecimal v = avgMap != null ? avgMap.get(pso) : null;
            addCell(table, formatVal(v), fTdPso, COLOR_BG_PSO, Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    private void renderAverageIndirectTable(Document document, ProgrammeAttainmentSnapshot snapshot) throws DocumentException {
        List<String> pos = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psos = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();

        float[] widths = computeMatrixColumnWidths(pos.size(), psos.size());
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        float fs = computeDynamicFontSize(pos.size() + psos.size());
        Font fTh = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);
        Font fThPso = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_PSO);
        Font fTd = new Font(Font.HELVETICA, fs, Font.NORMAL, COLOR_TEXT_DARK);
        Font fTdBold = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);
        Font fTdPso = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_PSO);
        Font fSummary = new Font(Font.HELVETICA, fs + 0.5f, Font.BOLD, COLOR_PRIMARY);

        // Header
        addCell(table, "Sr No", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "PRN", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Name of the Student", fTh, COLOR_BG_HEADER, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, po, fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, pso, fThPso, COLOR_BG_PSO, Element.ALIGN_CENTER);

        List<ProgrammeAttainmentSnapshot.StudentSurveyRow> rows = (snapshot.getSection3AverageIndirect() != null)
                ? snapshot.getSection3AverageIndirect().getStudentResponses() : List.of();

        if (rows == null || rows.isEmpty()) {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No student exit survey responses recorded for this batch.", fTd));
            emptyCell.setColspan(3 + pos.size() + psos.size());
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            emptyCell.setPadding(12f);
            setCellBorder(emptyCell);
            table.addCell(emptyCell);
        } else {
            for (int i = 0; i < rows.size(); i++) {
                ProgrammeAttainmentSnapshot.StudentSurveyRow r = rows.get(i);
                Color bg = (i % 2 == 1) ? COLOR_BG_ZEBRA : Color.WHITE;
                addCell(table, String.valueOf(r.getSrNo() != null ? r.getSrNo() : (i + 1)), fTd, bg, Element.ALIGN_CENTER);
                addCell(table, r.getPrn() != null ? r.getPrn() : "—", fTdBold, bg, Element.ALIGN_CENTER);
                addCell(table, r.getStudentName() != null ? r.getStudentName() : "—", fTd, bg, Element.ALIGN_LEFT);

                for (String po : pos) {
                    BigDecimal v = r.getPoRatings() != null ? r.getPoRatings().get(po) : null;
                    addCell(table, formatVal(v), fTd, bg, Element.ALIGN_CENTER);
                }
                for (String pso : psos) {
                    BigDecimal v = r.getPsoRatings() != null ? r.getPsoRatings().get(pso) : null;
                    addCell(table, formatVal(v), fTdPso, COLOR_BG_PSO, Element.ALIGN_CENTER);
                }
            }
        }

        // Summary Row
        PdfPCell sumLabel = new PdfPCell(new Phrase("Average Attainment (Indirect)", fSummary));
        sumLabel.setColspan(3);
        sumLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sumLabel.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sumLabel.setBackgroundColor(COLOR_BG_HEADER);
        sumLabel.setPadding(4f);
        setCellBorder(sumLabel);
        table.addCell(sumLabel);

        Map<String, BigDecimal> avgMap = (snapshot.getSection3AverageIndirect() != null)
                ? snapshot.getSection3AverageIndirect().getAverageIndirectAttainment() : Map.of();

        for (String po : pos) {
            BigDecimal v = avgMap != null ? avgMap.get(po) : null;
            addCell(table, formatVal(v), fSummary, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        }
        for (String pso : psos) {
            BigDecimal v = avgMap != null ? avgMap.get(pso) : null;
            addCell(table, formatVal(v), fTdPso, COLOR_BG_PSO, Element.ALIGN_CENTER);
        }

        document.add(table);
    }

    private void renderOverallAttainmentTable(Document document, ProgrammeAttainmentSnapshot snapshot) throws DocumentException {
        List<String> pos = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psos = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();

        // 2 metadata columns (Year, Component) + outcome columns
        float[] widths = new float[2 + pos.size() + psos.size()];
        widths[0] = 75f;  // Academic Batch / Year
        widths[1] = 165f; // Attainment Component
        float outcomeW = (802f - 240f) / Math.max(1, pos.size() + psos.size());
        for (int i = 2; i < widths.length; i++) widths[i] = outcomeW;

        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        float fs = computeDynamicFontSize(pos.size() + psos.size());
        Font fTh = new Font(Font.HELVETICA, fs + 0.5f, Font.BOLD, COLOR_TEXT_DARK);
        Font fThPso = new Font(Font.HELVETICA, fs + 0.5f, Font.BOLD, COLOR_TEXT_PSO);
        Font fTd = new Font(Font.HELVETICA, fs, Font.NORMAL, COLOR_TEXT_DARK);
        Font fTdBold = new Font(Font.HELVETICA, fs + 0.5f, Font.BOLD, COLOR_TEXT_DARK);

        // Header
        addCell(table, "Academic Batch", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Attainment Component", fTh, COLOR_BG_HEADER, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, po, fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, pso, fThPso, COLOR_BG_PSO, Element.ALIGN_CENTER);

        ProgrammeAttainmentSnapshot.OverallAttainmentSection sec = snapshot.getSection4OverallAttainment();
        Map<String, BigDecimal> mapValues = sec != null ? sec.getAverageMappingStrength() : Map.of();
        Map<String, BigDecimal> dirValues = sec != null ? sec.getAverageDirectAttainment() : Map.of();
        Map<String, BigDecimal> indValues = sec != null ? sec.getAverageIndirectAttainment() : Map.of();
        Map<String, BigDecimal> finalValues = sec != null ? sec.getFinalAttainments() : Map.of();

        String batchLabel = snapshot.getAcademicBatchYears() != null ? snapshot.getAcademicBatchYears() : (snapshot.getProgrammeBatchName() != null ? snapshot.getProgrammeBatchName() : "Batch");

        // Row 1: Average Mapping Values (Mint #DCF0E0)
        PdfPCell batchCell = new PdfPCell(new Phrase(batchLabel, fTdBold));
        batchCell.setRowspan(4);
        batchCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        batchCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        batchCell.setBackgroundColor(COLOR_BG_HEADER);
        setCellBorder(batchCell);
        table.addCell(batchCell);

        addCell(table, "Average Mapping Values", fTdBold, COLOR_ROW_MINT, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, formatVal(mapValues != null ? mapValues.get(po) : null), fTd, COLOR_ROW_MINT, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, formatVal(mapValues != null ? mapValues.get(pso) : null), fTd, COLOR_ROW_MINT, Element.ALIGN_CENTER);

        // Row 2: Average Attainment (Direct) (White)
        addCell(table, "Average Attainment (Direct)", fTdBold, Color.WHITE, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, formatVal(dirValues != null ? dirValues.get(po) : null), fTd, Color.WHITE, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, formatVal(dirValues != null ? dirValues.get(pso) : null), fTd, Color.WHITE, Element.ALIGN_CENTER);

        // Row 3: Average Attainment (Indirect) (White)
        addCell(table, "Average Attainment (Indirect)", fTdBold, Color.WHITE, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, formatVal(indValues != null ? indValues.get(po) : null), fTd, Color.WHITE, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, formatVal(indValues != null ? indValues.get(pso) : null), fTd, Color.WHITE, Element.ALIGN_CENTER);

        // Row 4: Overall Attainment (Sky Blue #B4DFE3)
        addCell(table, "Overall Attainment (80% Direct + 20% Indirect)", fTdBold, COLOR_ROW_BLUE, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, formatVal(finalValues != null ? finalValues.get(po) : null), fTdBold, COLOR_ROW_BLUE, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, formatVal(finalValues != null ? finalValues.get(pso) : null), fTdBold, COLOR_ROW_BLUE, Element.ALIGN_CENTER);

        document.add(table);
    }

    // =========================================================================
    //  COURSE ATTAINMENT TABLES
    // =========================================================================

    private void renderCourseAttainmentConfigSummary(Document document, CourseAttainmentSnapshot snapshot) throws DocumentException {
        PdfPTable summaryTable = new PdfPTable(4);
        summaryTable.setWidthPercentage(100);
        summaryTable.setSpacingAfter(8f);

        Font labelFont = new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_TEXT_MUTED);
        Font valFont = new Font(Font.HELVETICA, 10.5f, Font.BOLD, COLOR_PRIMARY);

        addSummaryCard(summaryTable, "DIRECT WEIGHTAGE", "80%", labelFont, valFont);
        addSummaryCard(summaryTable, "INDIRECT WEIGHTAGE", "20%", labelFont, valFont);
        addSummaryCard(summaryTable, "TARGET THRESHOLD", "60%", labelFont, valFont);
        addSummaryCard(summaryTable, "OVERALL CO ATTAINMENT", formatVal(snapshot.getOverallCoAttainment()), labelFont, valFont);

        document.add(summaryTable);
    }

    private void addSummaryCard(PdfPTable table, String label, String val, Font labelFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(COLOR_BG_ZEBRA);
        cell.setPadding(5f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        setCellBorder(cell);

        Paragraph p1 = new Paragraph(label, labelFont);
        p1.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p1);

        Paragraph p2 = new Paragraph(val, valFont);
        p2.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p2);

        table.addCell(cell);
    }

    private void renderCourseTable1(Document document, CourseAttainmentSnapshot snapshot) throws DocumentException {
        Paragraph title = new Paragraph("1. Table 1: Combined Mapping of CO to PO / PSO", new Font(Font.HELVETICA, 9.5f, Font.BOLD, COLOR_PRIMARY));
        title.setSpacingAfter(4f);
        document.add(title);

        List<String> pos = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psos = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();

        float[] widths = new float[2 + pos.size() + psos.size()];
        widths[0] = 35f;  // Sr No
        widths[1] = 65f;  // CO Code
        float outcomeW = (802f - 100f) / Math.max(1, pos.size() + psos.size());
        for (int i = 2; i < widths.length; i++) widths[i] = outcomeW;

        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(8f);

        float fs = computeDynamicFontSize(pos.size() + psos.size());
        Font fTh = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);
        Font fThPso = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_PSO);
        Font fTd = new Font(Font.HELVETICA, fs, Font.NORMAL, COLOR_TEXT_DARK);
        Font fTdBold = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);
        Font fSummary = new Font(Font.HELVETICA, fs + 0.5f, Font.BOLD, COLOR_PRIMARY);

        // Header
        addCell(table, "#", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "CO Code", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        for (String po : pos) addCell(table, po, fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, pso, fThPso, COLOR_BG_PSO, Element.ALIGN_CENTER);

        // Rows
        if (snapshot.getTable1Mapping() != null) {
            int idx = 1;
            for (CourseAttainmentSnapshot.CoMappingRow r : snapshot.getTable1Mapping()) {
                Color bg = (idx % 2 == 0) ? COLOR_BG_ZEBRA : Color.WHITE;
                addCell(table, String.valueOf(idx++), fTd, bg, Element.ALIGN_CENTER);
                addCell(table, r.getCoCode(), fTdBold, bg, Element.ALIGN_CENTER);

                for (String po : pos) {
                    Integer v = r.getPoMappings() != null ? r.getPoMappings().get(po) : null;
                    addCell(table, v != null ? String.valueOf(v) : "—", fTd, bg, Element.ALIGN_CENTER);
                }
                for (String pso : psos) {
                    Integer v = r.getPsoMappings() != null ? r.getPsoMappings().get(pso) : null;
                    addCell(table, v != null ? String.valueOf(v) : "—", fTd, COLOR_BG_PSO, Element.ALIGN_CENTER);
                }
            }
        }

        // Summary Average Mapping Row
        PdfPCell sumLabel = new PdfPCell(new Phrase("Average Mapping Strength", fSummary));
        sumLabel.setColspan(2);
        sumLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        sumLabel.setVerticalAlignment(Element.ALIGN_MIDDLE);
        sumLabel.setBackgroundColor(COLOR_BG_HEADER);
        sumLabel.setPadding(4f);
        setCellBorder(sumLabel);
        table.addCell(sumLabel);

        Map<String, BigDecimal> poAvg = new HashMap<>();
        if (snapshot.getTable2DirectPO() != null) {
            for (CourseAttainmentSnapshot.OutcomeContributionRow r : snapshot.getTable2DirectPO()) {
                poAvg.put(r.getOutcomeCode(), r.getAverageMapping());
            }
        }
        Map<String, BigDecimal> psoAvg = new HashMap<>();
        if (snapshot.getTable2DirectPSO() != null) {
            for (CourseAttainmentSnapshot.OutcomeContributionRow r : snapshot.getTable2DirectPSO()) {
                psoAvg.put(r.getOutcomeCode(), r.getAverageMapping());
            }
        }

        for (String po : pos) addCell(table, formatVal(poAvg.get(po)), fSummary, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, formatVal(psoAvg.get(pso)), fSummary, COLOR_BG_PSO, Element.ALIGN_CENTER);

        document.add(table);
    }

    private void renderCourseTable2(Document document, CourseAttainmentSnapshot snapshot) throws DocumentException {
        Paragraph title = new Paragraph("2. Table 2: PO & PSO Attainment Values (Direct Attainment)", new Font(Font.HELVETICA, 9.5f, Font.BOLD, COLOR_PRIMARY));
        title.setSpacingAfter(4f);
        document.add(title);

        List<String> pos = snapshot.getPoCodes() != null ? snapshot.getPoCodes() : List.of();
        List<String> psos = snapshot.getPsoCodes() != null ? snapshot.getPsoCodes() : List.of();

        float[] widths = new float[2 + pos.size() + psos.size()];
        widths[0] = 75f;  // Course Code
        widths[1] = 145f; // Metric
        float outcomeW = (802f - 220f) / Math.max(1, pos.size() + psos.size());
        for (int i = 2; i < widths.length; i++) widths[i] = outcomeW;

        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(8f);

        float fs = computeDynamicFontSize(pos.size() + psos.size());
        Font fTh = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);
        Font fThPso = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_PSO);
        Font fTd = new Font(Font.HELVETICA, fs, Font.NORMAL, COLOR_TEXT_DARK);
        Font fTdBold = new Font(Font.HELVETICA, fs, Font.BOLD, COLOR_TEXT_DARK);

        // Header
        addCell(table, "Course Code", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Metric", fTh, COLOR_BG_HEADER, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, po, fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, pso, fThPso, COLOR_BG_PSO, Element.ALIGN_CENTER);

        Map<String, BigDecimal> poAvg = new HashMap<>();
        Map<String, BigDecimal> poCont = new HashMap<>();
        if (snapshot.getTable2DirectPO() != null) {
            for (CourseAttainmentSnapshot.OutcomeContributionRow r : snapshot.getTable2DirectPO()) {
                poAvg.put(r.getOutcomeCode(), r.getAverageMapping());
                poCont.put(r.getOutcomeCode(), r.getDirectContribution());
            }
        }

        Map<String, BigDecimal> psoAvg = new HashMap<>();
        Map<String, BigDecimal> psoCont = new HashMap<>();
        if (snapshot.getTable2DirectPSO() != null) {
            for (CourseAttainmentSnapshot.OutcomeContributionRow r : snapshot.getTable2DirectPSO()) {
                psoAvg.put(r.getOutcomeCode(), r.getAverageMapping());
                psoCont.put(r.getOutcomeCode(), r.getDirectContribution());
            }
        }

        // Row 1: Average Mapping Strength
        addCell(table, snapshot.getCourseCode(), fTdBold, Color.WHITE, Element.ALIGN_CENTER);
        addCell(table, "Average Mapping Strength", fTd, Color.WHITE, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, formatVal(poAvg.get(po)), fTd, Color.WHITE, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, formatVal(psoAvg.get(pso)), fTd, COLOR_BG_PSO, Element.ALIGN_CENTER);

        // Row 2: Direct Attainment Contribution
        addCell(table, snapshot.getCourseCode(), fTdBold, COLOR_BG_ZEBRA, Element.ALIGN_CENTER);
        addCell(table, "Direct Attainment Contribution", fTdBold, COLOR_BG_ZEBRA, Element.ALIGN_LEFT);
        for (String po : pos) addCell(table, formatVal(poCont.get(po)), fTdBold, COLOR_BG_ZEBRA, Element.ALIGN_CENTER);
        for (String pso : psos) addCell(table, formatVal(psoCont.get(pso)), fTdBold, COLOR_BG_PSO, Element.ALIGN_CENTER);

        document.add(table);
    }

    private void renderCourseTable3(Document document, CourseAttainmentSnapshot snapshot) throws DocumentException {
        Paragraph title = new Paragraph("3. Table 3: Course Outcome (CO) Attainment Breakdown", new Font(Font.HELVETICA, 9.5f, Font.BOLD, COLOR_PRIMARY));
        title.setSpacingAfter(4f);
        document.add(title);

        float[] widths = new float[]{10f, 32f, 10f, 10f, 9f, 10f, 9f, 10f};
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        table.setSpacingAfter(8f);

        Font fTh = new Font(Font.HELVETICA, 8f, Font.BOLD, COLOR_TEXT_DARK);
        Font fTd = new Font(Font.HELVETICA, 7.5f, Font.NORMAL, COLOR_TEXT_DARK);
        Font fTdBold = new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_TEXT_DARK);

        addCell(table, "CO Code", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Course Outcome Statement", fTh, COLOR_BG_HEADER, Element.ALIGN_LEFT);
        addCell(table, "Target", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Direct %", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Direct Lvl", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Indirect %", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Indirect Lvl", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);
        addCell(table, "Final Level", fTh, COLOR_BG_HEADER, Element.ALIGN_CENTER);

        if (snapshot.getTable3CoAttainments() != null) {
            int i = 0;
            for (CourseAttainmentSnapshot.CoAttainmentRow r : snapshot.getTable3CoAttainments()) {
                Color bg = (i++ % 2 == 1) ? COLOR_BG_ZEBRA : Color.WHITE;
                addCell(table, r.getCoCode(), fTdBold, bg, Element.ALIGN_CENTER);
                addCell(table, r.getStatement() != null ? r.getStatement() : "", fTd, bg, Element.ALIGN_LEFT);
                addCell(table, formatVal(r.getTargetLevel()), fTd, bg, Element.ALIGN_CENTER);
                addCell(table, r.getDirectPercentage() != null ? formatVal(r.getDirectPercentage()) + "%" : "—", fTd, bg, Element.ALIGN_CENTER);
                addCell(table, r.getDirectLevel() != null ? String.valueOf(r.getDirectLevel()) : "—", fTd, bg, Element.ALIGN_CENTER);
                addCell(table, r.getIndirectPercentage() != null ? formatVal(r.getIndirectPercentage()) + "%" : "—", fTd, bg, Element.ALIGN_CENTER);
                addCell(table, r.getIndirectLevel() != null ? String.valueOf(r.getIndirectLevel()) : "—", fTd, bg, Element.ALIGN_CENTER);
                addCell(table, formatVal(r.getFinalAttainment()), fTdBold, bg, Element.ALIGN_CENTER);
            }
        }
        document.add(table);
    }

    // =========================================================================
    //  ACTION TAKEN REPORT (ATR) DOCUMENT RENDERING
    // =========================================================================

    private void renderProgrammeAtrDocument(Document document, ProgrammeAtrSnapshot snapshot) throws DocumentException {
        Font secFont = new Font(Font.HELVETICA, 10f, Font.BOLD, COLOR_PRIMARY);
        Font fHeader = new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_PRIMARY);
        Font fLabel = new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_TEXT_MUTED);
        Font fVal = new Font(Font.HELVETICA, 8f, Font.BOLD, COLOR_TEXT_DARK);
        Font fAction = new Font(Font.HELVETICA, 8f, Font.NORMAL, COLOR_TEXT_DARK);

        // 1. Programme Outcomes (POs)
        Paragraph poTitle = new Paragraph("1. Programme Outcomes (POs) Action Plan", secFont);
        poTitle.setSpacingAfter(4f);
        document.add(poTitle);

        if (snapshot.getPoOutcomes() != null) {
            for (ProgrammeAtrSnapshot.AtrOutcomeRow r : snapshot.getPoOutcomes()) {
                renderAtrOutcomeBox(document, r, fHeader, fLabel, fVal, fAction);
            }
        }

        // 2. Programme Specific Outcomes (PSOs)
        Paragraph psoTitle = new Paragraph("2. Programme Specific Outcomes (PSOs) Action Plan", secFont);
        psoTitle.setSpacingBefore(6f);
        psoTitle.setSpacingAfter(4f);
        document.add(psoTitle);

        if (snapshot.getPsoOutcomes() != null) {
            for (ProgrammeAtrSnapshot.AtrOutcomeRow r : snapshot.getPsoOutcomes()) {
                renderAtrOutcomeBox(document, r, fHeader, fLabel, fVal, fAction);
            }
        }
    }

    private void renderCourseAtrDocument(Document document, CourseAtrSnapshot snapshot) throws DocumentException {
        Font secFont = new Font(Font.HELVETICA, 10f, Font.BOLD, COLOR_PRIMARY);
        Font fHeader = new Font(Font.HELVETICA, 8.5f, Font.BOLD, COLOR_PRIMARY);
        Font fLabel = new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_TEXT_MUTED);
        Font fVal = new Font(Font.HELVETICA, 8f, Font.BOLD, COLOR_TEXT_DARK);
        Font fAction = new Font(Font.HELVETICA, 8f, Font.NORMAL, COLOR_TEXT_DARK);

        Paragraph coTitle = new Paragraph("Course Outcomes (COs) Action Plan", secFont);
        coTitle.setSpacingAfter(4f);
        document.add(coTitle);

        if (snapshot.getOutcomes() != null) {
            for (CourseAtrSnapshot.AtrOutcomeRow r : snapshot.getOutcomes()) {
                renderAtrOutcomeBox(document, r, fHeader, fLabel, fVal, fAction);
            }
        }
    }

    private void renderAtrOutcomeBox(Document document, Object outcomeObj, Font fHeader, Font fLabel, Font fVal, Font fAction) throws DocumentException {
        String code = "";
        String statement = "";
        BigDecimal target = null;
        BigDecimal actual = null;
        BigDecimal pct = null;
        List<String> actions = List.of();

        if (outcomeObj instanceof ProgrammeAtrSnapshot.AtrOutcomeRow r) {
            code = r.getOutcomeCode();
            statement = r.getOutcomeStatement();
            target = r.getTargetLevel();
            actual = r.getAttainmentLevel();
            pct = r.getAchievementPercentage();
            actions = r.getActions();
        } else if (outcomeObj instanceof CourseAtrSnapshot.AtrOutcomeRow r) {
            code = r.getOutcomeCode();
            statement = r.getOutcomeStatement();
            target = r.getTargetLevel();
            actual = r.getAttainmentLevel();
            pct = r.getAchievementPercentage();
            actions = r.getActions();
        }

        boolean isMet = (actual != null && target != null && actual.compareTo(target) >= 0);

        PdfPTable box = new PdfPTable(4);
        box.setWidthPercentage(100);
        box.setSpacingAfter(6f);

        // Row 1: Code and Statement
        PdfPCell titleCell = new PdfPCell();
        titleCell.setColspan(4);
        titleCell.setBackgroundColor(COLOR_BG_HEADER);
        titleCell.setPadding(4f);
        setCellBorder(titleCell);
        Paragraph tp = new Paragraph();
        tp.add(new Chunk(code + " : ", fHeader));
        tp.add(new Chunk(statement != null ? statement : "", new Font(Font.HELVETICA, 8f, Font.NORMAL, COLOR_TEXT_DARK)));
        titleCell.addElement(tp);
        box.addCell(titleCell);

        // Row 2: Metrics
        addAtrMetricCell(box, "TARGET LEVEL", formatVal(target), fLabel, fVal);
        addAtrMetricCell(box, "ATTAINMENT LEVEL", formatVal(actual), fLabel, fVal);
        addAtrMetricCell(box, "% TARGET ACHIEVED", pct != null ? formatVal(pct) + "%" : "—", fLabel, fVal);

        PdfPCell statusCell = new PdfPCell();
        statusCell.setBackgroundColor(isMet ? COLOR_MET_BG : COLOR_GAP_BG);
        statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        statusCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        statusCell.setPadding(3f);
        setCellBorder(statusCell);
        Paragraph sp = new Paragraph(isMet ? "TARGET MET" : "GAP IDENTIFIED",
                new Font(Font.HELVETICA, 7.5f, Font.BOLD, isMet ? COLOR_STATUS_MET : COLOR_STATUS_GAP));
        sp.setAlignment(Element.ALIGN_CENTER);
        statusCell.addElement(sp);
        box.addCell(statusCell);

        // Row 3: Actions
        PdfPCell actionCell = new PdfPCell();
        actionCell.setColspan(4);
        actionCell.setBackgroundColor(Color.WHITE);
        actionCell.setPadding(4f);
        setCellBorder(actionCell);

        Paragraph actHead = new Paragraph("Actions Taken / Planned :", new Font(Font.HELVETICA, 7.5f, Font.BOLD, COLOR_SLATE));
        actHead.setSpacingAfter(2f);
        actionCell.addElement(actHead);

        if (actions != null && !actions.isEmpty()) {
            int aIdx = 1;
            for (String act : actions) {
                if (act != null && !act.isBlank()) {
                    Paragraph ap = new Paragraph((aIdx++) + ". " + act, fAction);
                    ap.setSpacingAfter(1.5f);
                    actionCell.addElement(ap);
                }
            }
        } else {
            Paragraph ap = new Paragraph("1. Maintain current instructional methodology and continuous assessment.", fAction);
            actionCell.addElement(ap);
        }

        box.addCell(actionCell);
        document.add(box);
    }

    private void addAtrMetricCell(PdfPTable table, String label, String val, Font labelFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(Color.WHITE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        setCellBorder(cell);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", labelFont));
        p.add(new Chunk(val, valFont));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);
        table.addCell(cell);
    }

    // =========================================================================
    //  UTILITIES & HELPERS
    // =========================================================================

    private CommonPdfHeaderRenderer.HeaderContext buildHeaderContext(
            ProgrammeAttainmentSnapshot snapshot,
            ReportTemplateDto template,
            String reportTitle,
            String term,
            byte[] leftLogo,
            byte[] rightLogo,
            boolean isLandscape) {

        String progScope = "Programme : " + (snapshot.getMasterProgrammeName() != null ? snapshot.getMasterProgrammeName() : "")
                + (snapshot.getMasterProgrammeCode() != null && !snapshot.getMasterProgrammeCode().isBlank() ? " (" + snapshot.getMasterProgrammeCode() + ")" : "");

        return CommonPdfHeaderRenderer.HeaderContext.builder()
                .institutionName(snapshot.getInstitutionName())
                .schoolName(snapshot.getSchoolName())
                .reportTitle(reportTitle)
                .academicYear(snapshot.getAcademicYear() != null ? snapshot.getAcademicYear() : (snapshot.getAcademicBatchYears() != null ? snapshot.getAcademicBatchYears() : "—"))
                .termOrSemester(term)
                .scopeLabelAndValue(progScope)
                .revision(template != null && template.getTemplateVersion() != null ? "0" + template.getTemplateVersion() : "01")
                .generatedAt(snapshot.getGeneratedAt())
                .leftLogoBytes(leftLogo)
                .rightLogoBytes(rightLogo)
                .isLandscape(isLandscape)
                .build();
    }

    private static float[] computeMatrixColumnWidths(int poCount, int psoCount) {
        int outcomeCount = Math.max(1, poCount + psoCount);
        float[] widths = new float[3 + outcomeCount];
        widths[0] = 35f;  // Sem / Sr No
        widths[1] = 60f;  // Course Code / PRN
        widths[2] = 145f; // Course Name / Student Name
        float outcomeW = (802f - 240f) / outcomeCount;
        for (int i = 3; i < widths.length; i++) widths[i] = outcomeW;
        return widths;
    }

    private static float computeDynamicFontSize(int outcomeCount) {
        if (outcomeCount > 20) return 6.0f;
        if (outcomeCount > 15) return 6.8f;
        return 7.5f;
    }

    private static void addCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(3.5f);
        setCellBorder(cell);
        table.addCell(cell);
    }

    private static void setCellBorder(PdfPCell cell) {
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.5f);
    }

    private static String formatVal(BigDecimal val) {
        return val != null ? val.toPlainString() : "—";
    }

    private String getSectionTitle(ReportSection section) {
        return switch (section) {
            case AVERAGE_MAPPING -> "PROGRAMME ATTAINMENT REPORT — AVERAGE MAPPING";
            case AVERAGE_DIRECT -> "PROGRAMME ATTAINMENT REPORT — AVERAGE DIRECT ATTAINMENT";
            case AVERAGE_INDIRECT -> "PROGRAMME ATTAINMENT REPORT — AVERAGE INDIRECT ATTAINMENT (EXIT SURVEY)";
            case OVERALL -> "PROGRAMME ATTAINMENT REPORT — OVERALL ATTAINMENT (80% DIRECT + 20% INDIRECT)";
            case ALL -> "PROGRAMME ATTAINMENT REPORT — CONSOLIDATED MASTER REPORT";
        };
    }
}
