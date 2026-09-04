package com.dypiu.nba.reports;

import com.dypiu.nba.reports.excel.ExcelReportRenderer;
import com.dypiu.nba.reports.integrity.ReportIntegrityService;
import com.dypiu.nba.reports.model.ReportSection;
import com.dypiu.nba.reports.model.ReportType;
import com.dypiu.nba.reports.model.snapshot.*;
import com.dypiu.nba.reports.pdf.PdfReportRenderer;
import com.dypiu.nba.reports.template.HeaderConfig;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class VisualArtifactGenerationRunnerTest {

    @Test
    @DisplayName("Generate and dump all representative visual artifacts for inspection")
    void generateAndDumpAllVisualArtifacts() throws Exception {
        ExcelReportRenderer excelRenderer = new ExcelReportRenderer();
        PdfReportRenderer pdfRenderer = new PdfReportRenderer();
        ReportIntegrityService integrityService = new ReportIntegrityService("dypiu-test-secret-key-1234567890123456");

        ReportTemplateDto template = ReportTemplateDto.builder()
                .id("tpl-def-inst")
                .templateName("Standard Institution Template")
                .templateVersion(1)
                .institutionId("DYPIU")
                .headerConfig(HeaderConfig.builder()
                        .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                        .subHeader("Sector 29, Nigdi Pradhikaran, Akurdi, Pune - 411044")
                        .accreditationText("Approved by AICTE | Outcome-Based Education (OBE) NBA Compliance")
                        .build())
                .build();

        File outputDir = new File("/Users/rajshaikh/.gemini/antigravity-cli/brain/cb74ad2c-5c72-441a-b1eb-cb0c657c0804/visual_artifacts");
        outputDir.mkdirs();

        // 1. Programme Attainment Section 1 (Average Mapping) PDF & XLSX
        ProgrammeAttainmentSnapshot progSnapshot = buildProgrammeAttainmentSnapshot();
        byte[] pdfProgSec1 = pdfRenderer.renderProgrammeAttainmentSection(progSnapshot, ReportSection.AVERAGE_MAPPING, template);
        saveFile(new File(outputDir, "01_PROGRAMME_ATTAINMENT_AVERAGE_MAPPING.pdf"), pdfProgSec1);

        // 2. Programme Attainment Master PDF & XLSX
        byte[] pdfProgMaster = pdfRenderer.renderProgrammeAttainmentMaster(progSnapshot, template);
        saveFile(new File(outputDir, "02_PROGRAMME_ATTAINMENT_MASTER.pdf"), pdfProgMaster);

        byte[] xlsxProgMaster = excelRenderer.renderProgrammeAttainmentMaster(progSnapshot);
        saveFile(new File(outputDir, "03_PROGRAMME_ATTAINMENT_MASTER.xlsx"), xlsxProgMaster);

        // 3. Course Attainment PDF & XLSX
        CourseAttainmentSnapshot courseSnapshot = buildCourseAttainmentSnapshot();
        byte[] pdfCourse = pdfRenderer.renderCourseAttainment(courseSnapshot, template);
        saveFile(new File(outputDir, "04_COURSE_ATTAINMENT_CONSOLIDATED.pdf"), pdfCourse);

        byte[] xlsxCourse = excelRenderer.renderCourseAttainment(courseSnapshot);
        saveFile(new File(outputDir, "05_COURSE_ATTAINMENT_CONSOLIDATED.xlsx"), xlsxCourse);

        // 4. Programme ATR PDF & XLSX
        ProgrammeAtrSnapshot patSnapshot = buildProgrammeAtrSnapshot();
        byte[] pdfPatr = pdfRenderer.renderProgrammeAtr(patSnapshot, template);
        saveFile(new File(outputDir, "06_PROGRAMME_ACTION_TAKEN_REPORT.pdf"), pdfPatr);

        byte[] xlsxPatr = excelRenderer.renderProgrammeAtr(patSnapshot);
        saveFile(new File(outputDir, "07_PROGRAMME_ACTION_TAKEN_REPORT.xlsx"), xlsxPatr);

        // 5. Course ATR PDF & XLSX
        CourseAtrSnapshot catSnapshot = buildCourseAtrSnapshot();
        byte[] pdfCatr = pdfRenderer.renderCourseAtr(catSnapshot, template);
        saveFile(new File(outputDir, "08_COURSE_ACTION_TAKEN_REPORT.pdf"), pdfCatr);

        byte[] xlsxCatr = excelRenderer.renderCourseAtr(catSnapshot);
        saveFile(new File(outputDir, "09_COURSE_ACTION_TAKEN_REPORT.xlsx"), xlsxCatr);

        assertTrue(pdfProgMaster.length > 0);
        assertTrue(xlsxProgMaster.length > 0);
    }

    private void saveFile(File file, byte[] data) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    private ProgrammeAttainmentSnapshot buildProgrammeAttainmentSnapshot() {
        List<String> pos = List.of("PO1", "PO2", "PO3", "PO4", "PO5", "PO6", "PO7", "PO8", "PO9", "PO10", "PO11", "PO12");
        List<String> psos = List.of("PSO1", "PSO2");

        Map<String, BigDecimal> valMap = new LinkedHashMap<>();
        for (String po : pos) valMap.put(po, new BigDecimal("2.58"));
        for (String pso : psos) valMap.put(pso, new BigDecimal("2.65"));

        List<ProgrammeAttainmentSnapshot.CourseMappingRow> courses = List.of(
                ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
                        .programmeBatchCourseId("pbc-1").courseCode("CS101").courseName("Programming Fundamentals").semester(1)
                        .poValues(valMap).psoValues(valMap).build(),
                ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
                        .programmeBatchCourseId("pbc-2").courseCode("CS102").courseName("Digital Logic & Computer Design").semester(1)
                        .poValues(valMap).psoValues(valMap).build(),
                ProgrammeAttainmentSnapshot.CourseMappingRow.builder()
                        .programmeBatchCourseId("pbc-3").courseCode("CS201").courseName("Data Structures & Algorithms").semester(3)
                        .poValues(valMap).psoValues(valMap).build()
        );

        List<ProgrammeAttainmentSnapshot.CourseDirectRow> dirCourses = List.of(
                ProgrammeAttainmentSnapshot.CourseDirectRow.builder()
                        .programmeBatchCourseId("pbc-1").courseCode("CS101").courseName("Programming Fundamentals").semester(1)
                        .poValues(valMap).psoValues(valMap).build(),
                ProgrammeAttainmentSnapshot.CourseDirectRow.builder()
                        .programmeBatchCourseId("pbc-2").courseCode("CS102").courseName("Digital Logic & Computer Design").semester(1)
                        .poValues(valMap).psoValues(valMap).build(),
                ProgrammeAttainmentSnapshot.CourseDirectRow.builder()
                        .programmeBatchCourseId("pbc-3").courseCode("CS201").courseName("Data Structures & Algorithms").semester(3)
                        .poValues(valMap).psoValues(valMap).build()
        );

        return ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-prog-ins-01")
                .reportType(ReportType.PROGRAMME_ATTAINMENT)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
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
                        .courses(courses).averageMappingStrength(valMap).overallAverageMappingStrength(new BigDecimal("2.58")).build())
                .section2AverageDirect(ProgrammeAttainmentSnapshot.AverageDirectSection.builder()
                        .courses(dirCourses).averageDirectAttainment(valMap).overallDirectAttainment(new BigDecimal("2.58")).build())
                .section3AverageIndirect(ProgrammeAttainmentSnapshot.AverageIndirectSection.builder()
                        .surveyType("Graduate Exit Survey").totalStudents(2)
                        .studentResponses(List.of(
                                ProgrammeAttainmentSnapshot.StudentSurveyRow.builder().srNo(1).prn("PRN2021001").studentName("Aditi Sharma").poRatings(valMap).psoRatings(valMap).build(),
                                ProgrammeAttainmentSnapshot.StudentSurveyRow.builder().srNo(2).prn("PRN2021002").studentName("Rohan Verma").poRatings(valMap).psoRatings(valMap).build()
                        ))
                        .averageIndirectAttainment(valMap).overallIndirectAttainment(new BigDecimal("2.58")).build())
                .section4OverallAttainment(ProgrammeAttainmentSnapshot.OverallAttainmentSection.builder()
                        .directWeightPercentage(new BigDecimal("80.00")).indirectWeightPercentage(new BigDecimal("20.00"))
                        .averageMappingStrength(valMap).averageDirectAttainment(valMap).averageIndirectAttainment(valMap).finalAttainments(valMap)
                        .overallProgrammeAttainment(new BigDecimal("2.58")).build())
                .build();
    }

    private CourseAttainmentSnapshot buildCourseAttainmentSnapshot() {
        return CourseAttainmentSnapshot.builder()
                .reportId("rep-course-ins-01")
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
                        CourseAttainmentSnapshot.CoMappingRow.builder().coCode("CO1").poMappings(Map.of("PO1", 3, "PO2", 2, "PO12", 3)).psoMappings(Map.of("PSO1", 3, "PSO2", 2)).build(),
                        CourseAttainmentSnapshot.CoMappingRow.builder().coCode("CO2").poMappings(Map.of("PO1", 2, "PO2", 3, "PO12", 2)).psoMappings(Map.of("PSO1", 2, "PSO2", 3)).build()
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
    }

    private ProgrammeAtrSnapshot buildProgrammeAtrSnapshot() {
        return ProgrammeAtrSnapshot.builder()
                .reportId("rep-patr-ins-01")
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
                                .outcomeStatement("Engineering Knowledge: Apply knowledge of mathematics, computing, and science.")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("2.65"))
                                .achievementPercentage(new BigDecimal("106.00"))
                                .actions(List.of("Incorporate case studies into algorithmic lectures.", "Organize hands-on lab sessions."))
                                .build()
                ))
                .psoOutcomes(List.of(
                        ProgrammeAtrSnapshot.AtrOutcomeRow.builder()
                                .outcomeCode("PSO1")
                                .outcomeStatement("AI & Machine Learning: Design and develop intelligent computing systems.")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("2.70"))
                                .achievementPercentage(new BigDecimal("108.00"))
                                .actions(List.of("Deploy GPU computing clusters for neural network laboratory assignments."))
                                .build()
                ))
                .generatedBy("Programme Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();
    }

    private CourseAtrSnapshot buildCourseAtrSnapshot() {
        return CourseAtrSnapshot.builder()
                .reportId("rep-catr-ins-01")
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
                                .actions(List.of("Provide practice question sets on recurrence relations and asymptotic bounds."))
                                .build()
                ))
                .generatedBy("Course Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();
    }
}
