package com.dypiu.nba.reports;

import com.dypiu.nba.reports.excel.ExcelReportRenderer;
import com.dypiu.nba.reports.model.ReportType;
import com.dypiu.nba.reports.model.snapshot.CourseAtrSnapshot;
import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import com.dypiu.nba.reports.model.snapshot.ProgrammeAtrSnapshot;
import com.dypiu.nba.reports.pdf.PdfReportRenderer;
import com.dypiu.nba.reports.template.HeaderConfig;
import com.dypiu.nba.reports.template.ReportTemplateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CourseAndAtrReportGenerationTest {

    private ExcelReportRenderer excelRenderer;
    private PdfReportRenderer pdfRenderer;
    private ReportTemplateDto sampleTemplate;

    @BeforeEach
    void setUp() {
        excelRenderer = new ExcelReportRenderer();
        pdfRenderer = new PdfReportRenderer();
        sampleTemplate = ReportTemplateDto.builder()
                .headerConfig(HeaderConfig.builder()
                        .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                        .build())
                .build();
    }

    @Test
    @DisplayName("Course Attainment Report generates valid PDF and Excel")
    void testCourseAttainmentGeneration() {
        CourseAttainmentSnapshot snapshot = CourseAttainmentSnapshot.builder()
                .reportId("rep-course-01")
                .reportType(ReportType.COURSE_ATTAINMENT)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .courseCode("CS201")
                .courseName("Data Structures & Algorithms")
                .semester(3)
                .batchName("B.Tech CSE 2021-25")
                .overallCoAttainment(new BigDecimal("2.74"))
                .directAttainment(new BigDecimal("2.80"))
                .indirectAttainment(new BigDecimal("2.50"))
                .poCodes(List.of("PO1", "PO2", "PO12"))
                .psoCodes(List.of("PSO1", "PSO2"))
                .table1Mapping(List.of(
                        CourseAttainmentSnapshot.CoMappingRow.builder()
                                .coCode("CO1")
                                .poMappings(Map.of("PO1", 3, "PO2", 2))
                                .psoMappings(Map.of("PSO1", 3))
                                .build()
                ))
                .table3CoAttainments(List.of(
                        CourseAttainmentSnapshot.CoAttainmentRow.builder()
                                .coCode("CO1")
                                .statement("Analyze algorithmic time and space complexity")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("68.0"))
                                .directLevel(3)
                                .indirectPercentage(new BigDecimal("82.0"))
                                .indirectScore(new BigDecimal("2.50"))
                                .indirectLevel(2)
                                .finalAttainment(new BigDecimal("2.80"))
                                .targetMet(true)
                                .build()
                ))
                .generatedBy("Course Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();

        byte[] excelBytes = excelRenderer.renderCourseAttainment(snapshot);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        byte[] pdfBytes = pdfRenderer.renderCourseAttainment(snapshot, sampleTemplate);
        assertNotNull(pdfBytes);
        assertEquals("%PDF", new String(pdfBytes, 0, 4));
    }

    @Test
    @DisplayName("Programme ATR generates valid PDF and Excel with multiple actions")
    void testProgrammeAtrGeneration() {
        ProgrammeAtrSnapshot snapshot = ProgrammeAtrSnapshot.builder()
                .reportId("rep-patr-01")
                .reportType(ReportType.PROGRAMME_ATR)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .masterProgrammeCode("BTECH-CSE")
                .masterProgrammeName("B.Tech Computer Science & Engineering")
                .batchName("B.Tech CSE 2021-25")
                .status("APPROVED")
                .poOutcomes(List.of(
                        ProgrammeAtrSnapshot.AtrOutcomeRow.builder()
                                .outcomeCode("PO1")
                                .outcomeStatement("Apply mathematics and engineering fundamentals.")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("2.55"))
                                .achievementPercentage(new BigDecimal("102.00"))
                                .actions(List.of("Maintain practical tutorial sessions", "Conduct coding contest"))
                                .build()
                ))
                .psoOutcomes(List.of(
                        ProgrammeAtrSnapshot.AtrOutcomeRow.builder()
                                .outcomeCode("PSO1")
                                .outcomeStatement("Develop cloud and software solutions.")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("2.58"))
                                .achievementPercentage(new BigDecimal("103.20"))
                                .actions(List.of("Expand DevOps microservices workshop"))
                                .build()
                ))
                .generatedBy("Programme Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();

        byte[] excelBytes = excelRenderer.renderProgrammeAtr(snapshot);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        byte[] pdfBytes = pdfRenderer.renderProgrammeAtr(snapshot, sampleTemplate);
        assertNotNull(pdfBytes);
        assertEquals("%PDF", new String(pdfBytes, 0, 4));
    }

    @Test
    @DisplayName("Course ATR generates valid PDF and Excel with multiple actions")
    void testCourseAtrGeneration() {
        CourseAtrSnapshot snapshot = CourseAtrSnapshot.builder()
                .reportId("rep-catr-01")
                .reportType(ReportType.COURSE_ATR)
                .institutionId("DYPIU")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .courseCode("CS201")
                .courseName("Data Structures & Algorithms")
                .semester(3)
                .batchName("B.Tech CSE 2021-25")
                .status("APPROVED")
                .outcomes(List.of(
                        CourseAtrSnapshot.AtrOutcomeRow.builder()
                                .outcomeCode("CO1")
                                .outcomeStatement("Analyze algorithmic complexity.")
                                .targetLevel(new BigDecimal("2.50"))
                                .attainmentLevel(new BigDecimal("2.80"))
                                .achievementPercentage(new BigDecimal("112.00"))
                                .actions(List.of("Maintain assignment difficulty", "Provide practice problems"))
                                .build()
                ))
                .generatedBy("Course Coordinator")
                .generatedAt(ZonedDateTime.now())
                .build();

        byte[] excelBytes = excelRenderer.renderCourseAtr(snapshot);
        assertNotNull(excelBytes);
        assertTrue(excelBytes.length > 0);

        byte[] pdfBytes = pdfRenderer.renderCourseAtr(snapshot, sampleTemplate);
        assertNotNull(pdfBytes);
        assertEquals("%PDF", new String(pdfBytes, 0, 4));
    }
}
