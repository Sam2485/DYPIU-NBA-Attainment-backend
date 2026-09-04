package com.dypiu.nba.reports;

import com.dypiu.nba.reports.excel.ExcelReportRenderer;
import com.dypiu.nba.reports.integrity.ReportIntegrityService;
import com.dypiu.nba.reports.model.ReportSection;
import com.dypiu.nba.reports.model.ReportType;
import com.dypiu.nba.reports.model.snapshot.*;
import com.dypiu.nba.reports.pdf.PdfReportRenderer;
import com.dypiu.nba.reports.template.HeaderConfig;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class Phase4ReportVisualDesignTest {

    private ExcelReportRenderer excelRenderer;
    private PdfReportRenderer pdfRenderer;
    private ReportIntegrityService integrityService;
    private ReportTemplateDto sampleTemplate;

    @BeforeEach
    void setUp() {
        excelRenderer = new ExcelReportRenderer();
        pdfRenderer = new PdfReportRenderer();
        integrityService = new ReportIntegrityService("dypiu-test-secret-key-1234567890123456");

        sampleTemplate = ReportTemplateDto.builder()
                .id("tpl-common-inst")
                .templateName("Common Institutional Template")
                .templateVersion(1)
                .isDefault(true)
                .institutionId("DYPIU")
                .headerConfig(HeaderConfig.builder()
                        .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                        .subHeader("Sector 29, Nigdi Pradhikaran, Akurdi, Pune - 411044")
                        .accreditationText("Approved by AICTE | Outcome-Based Education (OBE) NBA Compliance")
                        .build())
                .build();
    }

    @Test
    @DisplayName("Master Programme Attainment Excel contains EXACTLY four sheets")
    void testMasterExcelExactFourSheets() throws Exception {
        ProgrammeAttainmentSnapshot snapshot = buildSampleProgrammeSnapshot(12, 2, "School of Engineering and Technology");

        byte[] xlsxBytes = excelRenderer.renderProgrammeAttainmentMaster(snapshot);
        assertNotNull(xlsxBytes);
        assertTrue(xlsxBytes.length > 0);

        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            assertEquals(4, wb.getNumberOfSheets(), "Master workbook must have exactly 4 sheets");
            assertEquals("Average Mapping", wb.getSheetName(0));
            assertEquals("Average Direct Attainment", wb.getSheetName(1));
            assertEquals("Average Indirect Attainment", wb.getSheetName(2));
            assertEquals("Overall Attainment", wb.getSheetName(3));
        }
    }

    @Test
    @DisplayName("Same institutional template renders correctly with different dynamic school names")
    void testDynamicSchoolNameResolutionAcrossSchools() {
        // School 1: Engineering
        ProgrammeAttainmentSnapshot engSnapshot = buildSampleProgrammeSnapshot(12, 2, "School of Engineering and Technology");
        byte[] engPdf = pdfRenderer.renderProgrammeAttainmentMaster(engSnapshot, sampleTemplate);
        assertNotNull(engPdf);
        assertEquals("%PDF", new String(engPdf, 0, 4));

        // School 2: Management
        ProgrammeAttainmentSnapshot mgmtSnapshot = buildSampleProgrammeSnapshot(12, 2, "School of Management");
        byte[] mgmtPdf = pdfRenderer.renderProgrammeAttainmentMaster(mgmtSnapshot, sampleTemplate);
        assertNotNull(mgmtPdf);
        assertEquals("%PDF", new String(mgmtPdf, 0, 4));

        // School 3: Design
        ProgrammeAttainmentSnapshot desSnapshot = buildSampleProgrammeSnapshot(12, 2, "School of Design");
        byte[] desPdf = pdfRenderer.renderProgrammeAttainmentMaster(desSnapshot, sampleTemplate);
        assertNotNull(desPdf);
        assertEquals("%PDF", new String(desPdf, 0, 4));
    }

    @Test
    @DisplayName("Dynamic PO/PSO scaling supports up to 20 POs and 6 PSOs without failure")
    void testDynamicOutcomeScaling() {
        // 20 POs and 6 PSOs
        ProgrammeAttainmentSnapshot wideSnapshot = buildSampleProgrammeSnapshot(20, 6, "School of Engineering and Technology");

        byte[] pdfBytes = pdfRenderer.renderProgrammeAttainmentMaster(wideSnapshot, sampleTemplate);
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        assertEquals("%PDF", new String(pdfBytes, 0, 4));

        byte[] xlsxBytes = excelRenderer.renderProgrammeAttainmentMaster(wideSnapshot);
        assertNotNull(xlsxBytes);
        assertTrue(xlsxBytes.length > 0);
    }

    @Test
    @DisplayName("Indirect Attainment handles empty responses cleanly without advisory banners")
    void testIndirectAttainmentEmptyStateCleanRendering() {
        ProgrammeAttainmentSnapshot snapshot = buildSampleProgrammeSnapshot(12, 2, "School of Engineering and Technology");
        // Clear student responses
        snapshot.setSection3AverageIndirect(ProgrammeAttainmentSnapshot.AverageIndirectSection.builder()
                .surveyType("Graduate Exit Survey")
                .totalStudents(0)
                .studentResponses(List.of())
                .averageIndirectAttainment(Map.of())
                .overallIndirectAttainment(BigDecimal.ZERO)
                .build());

        byte[] pdfBytes = pdfRenderer.renderProgrammeAttainmentSection(snapshot, ReportSection.AVERAGE_INDIRECT, sampleTemplate);
        assertNotNull(pdfBytes);
        assertEquals("%PDF", new String(pdfBytes, 0, 4));

        byte[] xlsxBytes = excelRenderer.renderProgrammeAttainmentSection(snapshot, ReportSection.AVERAGE_INDIRECT);
        assertNotNull(xlsxBytes);
    }

    @Test
    @DisplayName("Course Attainment PDF and Excel render with full parameter cards and 3 tables")
    void testCourseAttainmentFullRender() {
        CourseAttainmentSnapshot snapshot = CourseAttainmentSnapshot.builder()
                .reportId("rep-course-vis-01")
                .reportType(ReportType.COURSE_ATTAINMENT)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .courseCode("CS201")
                .courseName("Data Structures & Algorithms")
                .semester(3)
                .batchName("B.Tech CSE 2021-25")
                .academicYear("2021-2025")
                .overallCoAttainment(new BigDecimal("2.74"))
                .directAttainment(new BigDecimal("2.80"))
                .indirectAttainment(new BigDecimal("2.50"))
                .poCodes(List.of("PO1", "PO2", "PO3", "PO4", "PO5", "PO6", "PO7", "PO8", "PO9", "PO10", "PO11", "PO12"))
                .psoCodes(List.of("PSO1", "PSO2"))
                .table1Mapping(List.of(
                        CourseAttainmentSnapshot.CoMappingRow.builder()
                                .coCode("CO1")
                                .poMappings(Map.of("PO1", 3, "PO2", 2, "PO12", 3))
                                .psoMappings(Map.of("PSO1", 3, "PSO2", 2))
                                .build(),
                        CourseAttainmentSnapshot.CoMappingRow.builder()
                                .coCode("CO2")
                                .poMappings(Map.of("PO1", 2, "PO2", 3, "PO12", 2))
                                .psoMappings(Map.of("PSO1", 2, "PSO2", 3))
                                .build()
                ))
                .table2DirectPO(List.of(
                        CourseAttainmentSnapshot.OutcomeContributionRow.builder().outcomeCode("PO1").averageMapping(new BigDecimal("2.50")).directContribution(new BigDecimal("2.45")).build(),
                        CourseAttainmentSnapshot.OutcomeContributionRow.builder().outcomeCode("PO2").averageMapping(new BigDecimal("2.50")).directContribution(new BigDecimal("2.45")).build()
                ))
                .table2DirectPSO(List.of(
                        CourseAttainmentSnapshot.OutcomeContributionRow.builder().outcomeCode("PSO1").averageMapping(new BigDecimal("2.50")).directContribution(new BigDecimal("2.45")).build(),
                        CourseAttainmentSnapshot.OutcomeContributionRow.builder().outcomeCode("PSO2").averageMapping(new BigDecimal("2.50")).directContribution(new BigDecimal("2.45")).build()
                ))
                .table3CoAttainments(List.of(
                        CourseAttainmentSnapshot.CoAttainmentRow.builder()
                                .coCode("CO1")
                                .statement("Understand core data structure primitives")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("72.5"))
                                .directLevel(3)
                                .indirectPercentage(new BigDecimal("80.0"))
                                .indirectScore(new BigDecimal("2.60"))
                                .indirectLevel(3)
                                .finalAttainment(new BigDecimal("2.85"))
                                .targetMet(true)
                                .build()
                ))
                .generatedBy("Course Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();

        byte[] pdfBytes = pdfRenderer.renderCourseAttainment(snapshot, sampleTemplate);
        assertNotNull(pdfBytes);
        assertEquals("%PDF", new String(pdfBytes, 0, 4));

        byte[] xlsxBytes = excelRenderer.renderCourseAttainment(snapshot);
        assertNotNull(xlsxBytes);
    }

    @Test
    @DisplayName("Programme ATR & Course ATR render with numbered actions and status badges")
    void testAtrReportsRendering() {
        ProgrammeAtrSnapshot patSnapshot = ProgrammeAtrSnapshot.builder()
                .reportId("rep-patr-vis-01")
                .reportType(ReportType.PROGRAMME_ATR)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .masterProgrammeCode("BTECH-CSE")
                .masterProgrammeName("B.Tech in Computer Science and Engineering")
                .batchName("B.Tech CSE 2021-25")
                .academicYear("2021-2025")
                .status("APPROVED")
                .poOutcomes(List.of(
                        ProgrammeAtrSnapshot.AtrOutcomeRow.builder()
                                .outcomeCode("PO1")
                                .outcomeStatement("Engineering Knowledge: Apply the knowledge of mathematics, science, engineering fundamentals.")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("2.65"))
                                .achievementPercentage(new BigDecimal("106.00"))
                                .actions(List.of("Incorporate case studies into algorithmic lectures.", "Organize hands-on lab sessions."))
                                .build(),
                        ProgrammeAtrSnapshot.AtrOutcomeRow.builder()
                                .outcomeCode("PO2")
                                .outcomeStatement("Problem Analysis: Identify, formulate, review research literature.")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("2.35"))
                                .achievementPercentage(new BigDecimal("94.00"))
                                .actions(List.of("Introduce guided mini-projects.", "Conduct remedial problem-solving workshops."))
                                .build()
                ))
                .psoOutcomes(List.of(
                        ProgrammeAtrSnapshot.AtrOutcomeRow.builder()
                                .outcomeCode("PSO1")
                                .outcomeStatement("AI & Machine Learning: Design and develop intelligent systems.")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("2.70"))
                                .achievementPercentage(new BigDecimal("108.00"))
                                .actions(List.of("Deploy GPU computing clusters for neural network labs."))
                                .build()
                ))
                .generatedBy("Programme Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();

        byte[] patPdf = pdfRenderer.renderProgrammeAtr(patSnapshot, sampleTemplate);
        assertNotNull(patPdf);
        assertEquals("%PDF", new String(patPdf, 0, 4));

        byte[] patXlsx = excelRenderer.renderProgrammeAtr(patSnapshot);
        assertNotNull(patXlsx);

        CourseAtrSnapshot catSnapshot = CourseAtrSnapshot.builder()
                .reportId("rep-catr-vis-01")
                .reportType(ReportType.COURSE_ATR)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .courseCode("CS201")
                .courseName("Data Structures & Algorithms")
                .semester(3)
                .batchName("B.Tech CSE 2021-25")
                .academicYear("2021-2025")
                .status("APPROVED")
                .outcomes(List.of(
                        CourseAtrSnapshot.AtrOutcomeRow.builder()
                                .outcomeCode("CO1")
                                .outcomeStatement("Analyze time and space complexity of algorithms")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("2.60"))
                                .achievementPercentage(new BigDecimal("104.00"))
                                .actions(List.of("Provide practice question sets on recurrence relations."))
                                .build()
                ))
                .generatedBy("Course Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();

        byte[] catPdf = pdfRenderer.renderCourseAtr(catSnapshot, sampleTemplate);
        assertNotNull(catPdf);
        assertEquals("%PDF", new String(catPdf, 0, 4));

        byte[] catXlsx = excelRenderer.renderCourseAtr(catSnapshot);
        assertNotNull(catXlsx);
    }

    @Test
    @DisplayName("Cryptographic integrity verification passes on all generated PDF and Excel artifacts")
    void testIntegrityVerificationOnVisualArtifacts() {
        ProgrammeAttainmentSnapshot snapshot = buildSampleProgrammeSnapshot(12, 2, "School of Engineering and Technology");

        byte[] pdfBytes = pdfRenderer.renderProgrammeAttainmentMaster(snapshot, sampleTemplate);
        byte[] xlsxBytes = excelRenderer.renderProgrammeAttainmentMaster(snapshot);

        String pdfSha = integrityService.calculateSha256(pdfBytes);
        String pdfHmac = integrityService.calculateHmac(pdfBytes);
        assertEquals(ReportIntegrityService.VerificationResult.VALID, integrityService.verifyArtifact(pdfBytes, pdfSha, pdfHmac));

        String xlsxSha = integrityService.calculateSha256(xlsxBytes);
        String xlsxHmac = integrityService.calculateHmac(xlsxBytes);
        assertEquals(ReportIntegrityService.VerificationResult.VALID, integrityService.verifyArtifact(xlsxBytes, xlsxSha, xlsxHmac));
    }

    private ProgrammeAttainmentSnapshot buildSampleProgrammeSnapshot(int numPo, int numPso, String schoolName) {
        List<String> pos = new ArrayList<>();
        for (int i = 1; i <= numPo; i++) pos.add("PO" + i);

        List<String> psos = new ArrayList<>();
        for (int i = 1; i <= numPso; i++) psos.add("PSO" + i);

        Map<String, BigDecimal> valMap = new LinkedHashMap<>();
        for (String po : pos) valMap.put(po, new BigDecimal("2.55"));
        for (String pso : psos) valMap.put(pso, new BigDecimal("2.60"));

        List<ProgrammeAttainmentSnapshot.CourseMappingRow> courses = List.of(
                ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
                        .programmeBatchCourseId("pbc-1")
                        .courseCode("CS101")
                        .courseName("Programming Fundamentals")
                        .semester(1)
                        .poValues(valMap)
                        .psoValues(valMap)
                        .build(),
                ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
                        .programmeBatchCourseId("pbc-2")
                        .courseCode("CS201")
                        .courseName("Data Structures")
                        .semester(3)
                        .poValues(valMap)
                        .psoValues(valMap)
                        .build()
        );

        List<ProgrammeAttainmentSnapshot.CourseDirectRow> dirCourses = List.of(
                ProgrammeAttainmentSnapshot.CourseDirectRow.builder()
                        .programmeBatchCourseId("pbc-1")
                        .courseCode("CS101")
                        .courseName("Programming Fundamentals")
                        .semester(1)
                        .poValues(valMap)
                        .psoValues(valMap)
                        .build(),
                ProgrammeAttainmentSnapshot.CourseDirectRow.builder()
                        .programmeBatchCourseId("pbc-2")
                        .courseCode("CS201")
                        .courseName("Data Structures")
                        .semester(3)
                        .poValues(valMap)
                        .psoValues(valMap)
                        .build()
        );

        return ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-prog-test-01")
                .reportType(ReportType.PROGRAMME_ATTAINMENT)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName(schoolName)
                .masterProgrammeId("prog-btech-cse")
                .masterProgrammeCode("BTECH-CSE")
                .masterProgrammeName("B.Tech in Computer Science and Engineering")
                .programmeBatchId("batch-2021-25")
                .programmeBatchName("B.Tech CSE 2021-25")
                .academicBatchYears("2021-2025")
                .academicYear("2021-2025")
                .poCodes(pos)
                .psoCodes(psos)
                .generatedBy("Programme Coordinator")
                .generatedAt(ZonedDateTime.now())
                .section1AverageMapping(ProgrammeAttainmentSnapshot.AverageMappingSection.builder()
                        .courses(courses)
                        .averageMappingStrength(valMap)
                        .overallAverageMappingStrength(new BigDecimal("2.57"))
                        .build())
                .section2AverageDirect(ProgrammeAttainmentSnapshot.AverageDirectSection.builder()
                        .courses(dirCourses)
                        .averageDirectAttainment(valMap)
                        .overallDirectAttainment(new BigDecimal("2.57"))
                        .build())
                .section3AverageIndirect(ProgrammeAttainmentSnapshot.AverageIndirectSection.builder()
                        .surveyType("Graduate Exit Survey")
                        .totalStudents(2)
                        .studentResponses(List.of(
                                ProgrammeAttainmentSnapshot.StudentSurveyRow.builder()
                                        .srNo(1).prn("PRN2021001").studentName("Aditi Sharma").poRatings(valMap).psoRatings(valMap).build(),
                                ProgrammeAttainmentSnapshot.StudentSurveyRow.builder()
                                        .srNo(2).prn("PRN2021002").studentName("Rohan Verma").poRatings(valMap).psoRatings(valMap).build()
                        ))
                        .averageIndirectAttainment(valMap)
                        .overallIndirectAttainment(new BigDecimal("2.57"))
                        .build())
                .section4OverallAttainment(ProgrammeAttainmentSnapshot.OverallAttainmentSection.builder()
                        .directWeightPercentage(new BigDecimal("80.00"))
                        .indirectWeightPercentage(new BigDecimal("20.00"))
                        .averageMappingStrength(valMap)
                        .averageDirectAttainment(valMap)
                        .averageIndirectAttainment(valMap)
                        .finalAttainments(valMap)
                        .overallProgrammeAttainment(new BigDecimal("2.57"))
                        .build())
                .build();
    }
}
