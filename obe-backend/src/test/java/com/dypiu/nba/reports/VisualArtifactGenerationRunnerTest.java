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

        File outputDir = new File("/Users/rajshaikh/.gemini/antigravity-cli/brain/15a70cc0-2e2b-4654-ae2c-dad71a4078b1/visual_artifacts");
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
                        .otherAssessments(List.of(
                                ProgrammeAttainmentSnapshot.IndirectAssessmentRow.builder().id("ind-1").eventTitle("Alumni Survey 2025").assessmentType("Survey").poValues(valMap).psoValues(valMap).build(),
                                ProgrammeAttainmentSnapshot.IndirectAssessmentRow.builder().id("ind-2").eventTitle("Industry Interaction & Hackathon").assessmentType("Co-Curricular Event").poValues(valMap).psoValues(valMap).build(),
                                ProgrammeAttainmentSnapshot.IndirectAssessmentRow.builder().id("ind-3").eventTitle("Employer Feedback Survey").assessmentType("Survey").poValues(valMap).psoValues(valMap).build()
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
                        CourseAttainmentSnapshot.CoMappingRow.builder().coCode("CO2").poMappings(Map.of("PO1", 2, "PO2", 3, "PO12", 2)).psoMappings(Map.of("PSO1", 2, "PSO2", 3)).build(),
                        CourseAttainmentSnapshot.CoMappingRow.builder().coCode("CO3").poMappings(Map.of("PO1", 3, "PO3", 3, "PO12", 2)).psoMappings(Map.of("PSO1", 3, "PSO2", 2)).build(),
                        CourseAttainmentSnapshot.CoMappingRow.builder().coCode("CO4").poMappings(Map.of("PO2", 3, "PO4", 2, "PO12", 3)).psoMappings(Map.of("PSO1", 2, "PSO2", 3)).build(),
                        CourseAttainmentSnapshot.CoMappingRow.builder().coCode("CO5").poMappings(Map.of("PO3", 2, "PO5", 3, "PO12", 2)).psoMappings(Map.of("PSO1", 3, "PSO2", 2)).build(),
                        CourseAttainmentSnapshot.CoMappingRow.builder().coCode("CO6").poMappings(Map.of("PO1", 2, "PO6", 2, "PO12", 3)).psoMappings(Map.of("PSO1", 2, "PSO2", 3)).build()
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
                                .statement("Understand core data structure primitives and memory models")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("72.50"))
                                .directLevel(3)
                                .indirectPercentage(new BigDecimal("80.00"))
                                .indirectScore(new BigDecimal("2.60"))
                                .indirectLevel(3)
                                .finalAttainment(new BigDecimal("2.85"))
                                .targetMet(true)
                                .build(),
                        CourseAttainmentSnapshot.CoAttainmentRow.builder()
                                .coCode("CO2")
                                .statement("Implement and analyze linear data structures such as stacks, queues, and linked lists")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("75.00"))
                                .directLevel(3)
                                .indirectPercentage(new BigDecimal("82.00"))
                                .indirectScore(new BigDecimal("2.70"))
                                .indirectLevel(3)
                                .finalAttainment(new BigDecimal("2.90"))
                                .targetMet(true)
                                .build(),
                        CourseAttainmentSnapshot.CoAttainmentRow.builder()
                                .coCode("CO3")
                                .statement("Apply tree and graph structures for hierarchical data organization and traversal")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("68.00"))
                                .directLevel(2)
                                .indirectPercentage(new BigDecimal("78.00"))
                                .indirectScore(new BigDecimal("2.50"))
                                .indirectLevel(3)
                                .finalAttainment(new BigDecimal("2.70"))
                                .targetMet(true)
                                .build(),
                        CourseAttainmentSnapshot.CoAttainmentRow.builder()
                                .coCode("CO4")
                                .statement("Analyze sorting and searching algorithm complexities")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("80.00"))
                                .directLevel(3)
                                .indirectPercentage(new BigDecimal("85.00"))
                                .indirectScore(new BigDecimal("2.80"))
                                .indirectLevel(3)
                                .finalAttainment(new BigDecimal("2.95"))
                                .targetMet(true)
                                .build(),
                        CourseAttainmentSnapshot.CoAttainmentRow.builder()
                                .coCode("CO5")
                                .statement("Evaluate hashing techniques and collision resolution strategies")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("70.00"))
                                .directLevel(3)
                                .indirectPercentage(new BigDecimal("75.00"))
                                .indirectScore(new BigDecimal("2.40"))
                                .indirectLevel(2)
                                .finalAttainment(new BigDecimal("2.75"))
                                .targetMet(true)
                                .build(),
                        CourseAttainmentSnapshot.CoAttainmentRow.builder()
                                .coCode("CO6")
                                .statement("Design algorithm solutions for real-world engineering problems")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("78.00"))
                                .directLevel(3)
                                .indirectPercentage(new BigDecimal("84.00"))
                                .indirectScore(new BigDecimal("2.70"))
                                .indirectLevel(3)
                                .finalAttainment(new BigDecimal("2.90"))
                                .targetMet(true)
                                .build()
                ))
                .examinationData(CourseAttainmentSnapshot.ExaminationSection.builder()
                        .courseName("Data Structures & Algorithms")
                        .className("Semester 3")
                        .academicYear("2021-2025")
                        .totalStudents(24)
                        .thresholdPercentage(new BigDecimal("60.00"))
                        .coCodes(List.of("CO1", "CO2", "CO3", "CO4", "CO5", "CO6"))
                        .coMaxMarks(Map.of("CO1", new BigDecimal("15.00"), "CO2", new BigDecimal("15.00"), "CO3", new BigDecimal("15.00"), "CO4", new BigDecimal("15.00"), "CO5", new BigDecimal("15.00"), "CO6", new BigDecimal("15.00")))
                        .coThresholdMarks(Map.of("CO1", new BigDecimal("9.00"), "CO2", new BigDecimal("9.00"), "CO3", new BigDecimal("9.00"), "CO4", new BigDecimal("9.00"), "CO5", new BigDecimal("9.00"), "CO6", new BigDecimal("9.00")))
                        .studentsAboveThreshold(Map.of("CO1", 20, "CO2", 21, "CO3", 19, "CO4", 22, "CO5", 20, "CO6", 21))
                        .percentageAboveThreshold(Map.of("CO1", new BigDecimal("83.33"), "CO2", new BigDecimal("87.50"), "CO3", new BigDecimal("79.17"), "CO4", new BigDecimal("91.67"), "CO5", new BigDecimal("83.33"), "CO6", new BigDecimal("87.50")))
                        .students(createVisualStudentMarks(24, List.of("CO1", "CO2", "CO3", "CO4", "CO5", "CO6")))
                        .build())
                .surveyData(CourseAttainmentSnapshot.SurveySection.builder()
                        .totalStudents(24)
                        .coCodes(List.of("CO1", "CO2", "CO3", "CO4", "CO5", "CO6"))
                        .level1Counts(Map.of("CO1", 2, "CO2", 2, "CO3", 3, "CO4", 2, "CO5", 1, "CO6", 2))
                        .level2Counts(Map.of("CO1", 5, "CO2", 5, "CO3", 3, "CO4", 3, "CO5", 6, "CO6", 4))
                        .level3Counts(Map.of("CO1", 17, "CO2", 17, "CO3", 18, "CO4", 19, "CO5", 17, "CO6", 18))
                        .level1Percentages(Map.of("CO1", new BigDecimal("8.33"), "CO2", new BigDecimal("8.33"), "CO3", new BigDecimal("12.50"), "CO4", new BigDecimal("8.33"), "CO5", new BigDecimal("4.17"), "CO6", new BigDecimal("8.33")))
                        .level2Percentages(Map.of("CO1", new BigDecimal("20.83"), "CO2", new BigDecimal("20.83"), "CO3", new BigDecimal("12.50"), "CO4", new BigDecimal("12.50"), "CO5", new BigDecimal("25.00"), "CO6", new BigDecimal("16.67")))
                        .level3Percentages(Map.of("CO1", new BigDecimal("70.83"), "CO2", new BigDecimal("70.83"), "CO3", new BigDecimal("75.00"), "CO4", new BigDecimal("79.17"), "CO5", new BigDecimal("70.83"), "CO6", new BigDecimal("75.00")))
                        .overallIndirectPercentages(Map.of("CO1", new BigDecimal("87.63"), "CO2", new BigDecimal("87.63"), "CO3", new BigDecimal("88.88"), "CO4", new BigDecimal("90.25"), "CO5", new BigDecimal("88.75"), "CO6", new BigDecimal("88.88")))
                        .responses(createVisualSurveyResponses(24, List.of("CO1", "CO2", "CO3", "CO4", "CO5", "CO6")))
                        .build())
                .generatedBy("Course Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();
    }

    private List<CourseAttainmentSnapshot.StudentMarksRow> createVisualStudentMarks(int count, List<String> cos) {
        List<CourseAttainmentSnapshot.StudentMarksRow> list = new ArrayList<>();
        String[] studentNames = {
                "Aditi Sharma", "Rohan Verma", "Sneha Kulkarni", "Amit Patil",
                "Pooja Deshmukh", "Rahul Shinde", "Ananya Joshi", "Kunal Pawar",
                "Neha More", "Vikas Gaikwad", "Tanvi Jagtap", "Siddharth Shinde",
                "Divya Kadam", "Omkar Chavan", "Ishita Rane", "Aditya Sawant",
                "Shruti Kamble", "Pranav Mohite", "Riya Salunkhe", "Sanket Kale",
                "Pallavi Mane", "Abhishek Gokhale", "Mrunal Thorat", "Varun Date"
        };
        for (int i = 1; i <= count; i++) {
            Map<String, BigDecimal> marks = new LinkedHashMap<>();
            for (int j = 0; j < cos.size(); j++) {
                int m = 10 + ((i * 2 + j * 3) % 6); // marks between 10 and 15
                marks.put(cos.get(j), BigDecimal.valueOf(m));
            }
            String name = (i - 1 < studentNames.length) ? studentNames[i - 1] : ("Student " + i);
            list.add(CourseAttainmentSnapshot.StudentMarksRow.builder()
                    .srNo(i)
                    .prn("20210800" + (i < 10 ? "0" + i : String.valueOf(i)))
                    .studentName(name)
                    .coMarks(marks)
                    .build());
        }
        return list;
    }

    private List<CourseAttainmentSnapshot.SurveyResponseRow> createVisualSurveyResponses(int count, List<String> cos) {
        List<CourseAttainmentSnapshot.SurveyResponseRow> list = new ArrayList<>();
        String[] opts = {"Slight", "Moderate", "Substantial"};
        for (int i = 1; i <= count; i++) {
            Map<String, String> fb = new LinkedHashMap<>();
            for (int j = 0; j < cos.size(); j++) {
                fb.put(cos.get(j), opts[(i + j) % 3]);
            }
            list.add(CourseAttainmentSnapshot.SurveyResponseRow.builder()
                    .srNo(i)
                    .coFeedbacks(fb)
                    .build());
        }
        return list;
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
