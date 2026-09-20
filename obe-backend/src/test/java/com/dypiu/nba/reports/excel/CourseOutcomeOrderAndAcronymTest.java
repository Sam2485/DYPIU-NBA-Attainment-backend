package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CourseOutcomeOrderAndAcronymTest {

    private final ExcelReportRenderer renderer = new ExcelReportRenderer();

    @Test
    @DisplayName("Natural Order Comparator verifies correct ascending numerical ordering")
    void testNaturalOrderComparator() {
        List<String> list = Arrays.asList("CO10", "CO2", "CO1", "CO9");
        list.sort(CourseOutcomeOrderHelper.NATURAL_ORDER);
        assertEquals(List.of("CO1", "CO2", "CO9", "CO10"), list);

        List<String> curriculumCodes = Arrays.asList("CS201.10", "CS201.2", "CS201.1", "CS201.9");
        curriculumCodes.sort(CourseOutcomeOrderHelper.NATURAL_ORDER);
        assertEquals(List.of("CS201.1", "CS201.2", "CS201.9", "CS201.10"), curriculumCodes);

        List<String> dashCodes = Arrays.asList("CO-12", "CO-1", "CO-2");
        dashCodes.sort(CourseOutcomeOrderHelper.NATURAL_ORDER);
        assertEquals(List.of("CO-1", "CO-2", "CO-12"), dashCodes);
    }

    @Test
    @DisplayName("Course Attainment sheets: keep actual code in Course Outcome & Table 1, use CO1..CON acronyms in Reference Work and Sheets 2-5, all sorted in ascending order")
    void testActualCoCodesAndAcronymsAcrossAllSheets() throws Exception {
        // Provide actual CO codes out of order to verify ascending sorting
        List<String> actualCodesUnsorted = List.of("CS201.3", "CS201.1", "CS201.6", "CS201.2", "CS201.5", "CS201.4");
        List<String> expectedAscendingActualCodes = List.of("CS201.1", "CS201.2", "CS201.3", "CS201.4", "CS201.5", "CS201.6");
        List<String> expectedAcronyms = List.of("CO1", "CO2", "CO3", "CO4", "CO5", "CO6");

        CourseAttainmentSnapshot snapshot = createSnapshotWithActualCodes(actualCodesUnsorted);

        byte[] bytes = renderer.renderCourseAttainment(snapshot);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals(5, wb.getNumberOfSheets());

            // -------------------------------------------------------------
            // SHEET 1: Attainment-main
            // -------------------------------------------------------------
            Sheet sheet1 = wb.getSheet("Attainment-main");
            assertNotNull(sheet1);

            // Section B: Course Outcome (Row 13 to 18)
            // MUST keep actual CO code and be in correct ascending order
            for (int i = 0; i < 6; i++) {
                int rowIdx = 13 + i;
                assertEquals(String.valueOf(i + 1), getCellString(sheet1, rowIdx, 0));
                assertEquals(expectedAscendingActualCodes.get(i), getCellString(sheet1, rowIdx, 1),
                        "Section B Code column must have actual CO code in ascending order");
            }

            // Section C: Table 1: Mapping of CO to PO/PSO (Row 22 to 27)
            // MUST keep actual CO code and be in correct ascending order
            for (int i = 0; i < 6; i++) {
                int rowIdx = 22 + i;
                assertEquals(String.valueOf(i + 1), getCellString(sheet1, rowIdx, 0));
                assertEquals(expectedAscendingActualCodes.get(i), getCellString(sheet1, rowIdx, 1),
                        "Table 1 Code column must have actual CO code in ascending order");
            }

            // Section F: Reference Work (Row 39 = CO Headers)
            // MUST use acronyms CO1..CO6 aligned with actual CO codes!
            int refCoRow = 39;
            for (int i = 0; i < 6; i++) {
                assertEquals(expectedAcronyms.get(i), getCellString(sheet1, refCoRow, 3 + i),
                        "Reference Work header must use acronym CO" + (i + 1));
            }

            // -------------------------------------------------------------
            // SHEET 2: PO mapping
            // -------------------------------------------------------------
            Sheet sheet2 = wb.getSheet("PO mapping");
            assertNotNull(sheet2);
            // Row 6 (0-indexed): Keywords headers (Cols 3..8) and Y/N headers (Cols 10..15)
            int poHeaderRow = 6;
            for (int i = 0; i < 6; i++) {
                assertEquals(expectedAcronyms.get(i), getCellString(sheet2, poHeaderRow, 3 + i),
                        "PO mapping Keywords header must use acronym");
                assertEquals(expectedAcronyms.get(i), getCellString(sheet2, poHeaderRow, 10 + i),
                        "PO mapping Y/N header must use acronym");
            }

            // -------------------------------------------------------------
            // SHEET 3: PSO mapping
            // -------------------------------------------------------------
            Sheet sheet3 = wb.getSheet("PSO mapping");
            assertNotNull(sheet3);
            int psoHeaderRow = 6;
            for (int i = 0; i < 6; i++) {
                assertEquals(expectedAcronyms.get(i), getCellString(sheet3, psoHeaderRow, 3 + i),
                        "PSO mapping Keywords header must use acronym");
                assertEquals(expectedAcronyms.get(i), getCellString(sheet3, psoHeaderRow, 10 + i),
                        "PSO mapping Y/N header must use acronym");
            }

            // -------------------------------------------------------------
            // SHEET 4: Examination
            // -------------------------------------------------------------
            Sheet sheet4 = wb.getSheet("Examination");
            assertNotNull(sheet4);
            // Row 21 (0-indexed): CO headers
            int examCoRow = 21;
            for (int i = 0; i < 6; i++) {
                assertEquals(expectedAcronyms.get(i), getCellString(sheet4, examCoRow, 5 + i),
                        "Examination sheet CO header must use acronym");
            }

            // -------------------------------------------------------------
            // SHEET 5: Course End Survey
            // -------------------------------------------------------------
            Sheet sheet5 = wb.getSheet("Course End Survey");
            assertNotNull(sheet5);
            // Row 5 (Summary Table 1): CO headers
            int surveyHeaderRow1 = 5;
            for (int i = 0; i < 6; i++) {
                assertEquals(expectedAcronyms.get(i), getCellString(sheet5, surveyHeaderRow1, 2 + i),
                        "Survey sheet Table 1 header must use acronym");
            }
            // Row 17 (Student response table): CO headers
            int surveyHeaderRow2 = 17;
            for (int i = 0; i < 6; i++) {
                assertEquals(expectedAcronyms.get(i), getCellString(sheet5, surveyHeaderRow2, 2 + i),
                        "Survey sheet response header must use acronym");
            }
        }
    }

    private CourseAttainmentSnapshot createSnapshotWithActualCodes(List<String> coCodes) {
        List<String> poCodes = List.of("PO1", "PO2", "PO3", "PO4", "PO5", "PO6", "PO7", "PO8", "PO9", "PO10", "PO11", "PO12");
        List<String> psoCodes = List.of("PSO1", "PSO2", "PSO3");

        List<CourseAttainmentSnapshot.CoMappingRow> t1 = new ArrayList<>();
        List<CourseAttainmentSnapshot.CoAttainmentRow> t3 = new ArrayList<>();
        Map<String, BigDecimal> maxMarks = new LinkedHashMap<>();
        Map<String, Integer> studentsAboveThresh = new LinkedHashMap<>();
        Map<String, BigDecimal> pctAboveThresh = new LinkedHashMap<>();

        for (String code : coCodes) {
            Map<String, Integer> poMap = new LinkedHashMap<>();
            poCodes.forEach(p -> poMap.put(p, 2));
            Map<String, Integer> psoMap = new LinkedHashMap<>();
            psoCodes.forEach(p -> psoMap.put(p, 2));

            t1.add(CourseAttainmentSnapshot.CoMappingRow.builder()
                    .coCode(code)
                    .poMappings(poMap)
                    .psoMappings(psoMap)
                    .build());

            t3.add(CourseAttainmentSnapshot.CoAttainmentRow.builder()
                    .coCode(code)
                    .statement("Statement for " + code)
                    .directPercentage(new BigDecimal("75.00"))
                    .directLevel(3)
                    .indirectPercentage(new BigDecimal("80.00"))
                    .indirectLevel(3)
                    .finalAttainment(new BigDecimal("2.80"))
                    .targetMet(true)
                    .build());

            maxMarks.put(code, new BigDecimal("15.00"));
            studentsAboveThresh.put(code, 45);
            pctAboveThresh.put(code, new BigDecimal("75.00"));
        }

        List<CourseAttainmentSnapshot.StudentMarksRow> students = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            Map<String, BigDecimal> marks = new LinkedHashMap<>();
            coCodes.forEach(c -> marks.put(c, new BigDecimal("12.00")));
            students.add(CourseAttainmentSnapshot.StudentMarksRow.builder()
                    .srNo(s)
                    .prn("PRN00" + s)
                    .studentName("Student " + s)
                    .coMarks(marks)
                    .build());
        }

        CourseAttainmentSnapshot.ExaminationSection exam = CourseAttainmentSnapshot.ExaminationSection.builder()
                .courseName("Computer Networks and Security")
                .className("TY B.Tech")
                .academicYear("AY 2023-24")
                .totalStudents(60)
                .thresholdPercentage(new BigDecimal("60.00"))
                .coCodes(coCodes)
                .coMaxMarks(maxMarks)
                .studentsAboveThreshold(studentsAboveThresh)
                .percentageAboveThreshold(pctAboveThresh)
                .students(students)
                .build();

        Map<String, Integer> l1Counts = new LinkedHashMap<>();
        Map<String, Integer> l2Counts = new LinkedHashMap<>();
        Map<String, Integer> l3Counts = new LinkedHashMap<>();
        coCodes.forEach(c -> {
            l1Counts.put(c, 5);
            l2Counts.put(c, 15);
            l3Counts.put(c, 40);
        });

        List<CourseAttainmentSnapshot.SurveyResponseRow> surveyResp = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            Map<String, String> fb = new LinkedHashMap<>();
            coCodes.forEach(c -> fb.put(c, "Substantial"));
            surveyResp.add(CourseAttainmentSnapshot.SurveyResponseRow.builder()
                    .srNo(s)
                    .coFeedbacks(fb)
                    .build());
        }

        CourseAttainmentSnapshot.SurveySection survey = CourseAttainmentSnapshot.SurveySection.builder()
                .totalStudents(60)
                .coCodes(coCodes)
                .level1Counts(l1Counts)
                .level2Counts(l2Counts)
                .level3Counts(l3Counts)
                .responses(surveyResp)
                .build();

        return CourseAttainmentSnapshot.builder()
                .institutionName("D Y Patil International University, Akurdi Pune")
                .schoolName("School of Engineering and Technology")
                .courseName("Computer Networks and Security")
                .courseCode("CS301")
                .academicYear("2023-24")
                .semester(5)
                .generatedBy("Dr. John Doe")
                .generatedAt(ZonedDateTime.now())
                .overallCoAttainment(new BigDecimal("2.80"))
                .poCodes(poCodes)
                .psoCodes(psoCodes)
                .table1Mapping(t1)
                .table3CoAttainments(t3)
                .examinationData(exam)
                .surveyData(survey)
                .build();
    }

    private static String getCellString(Sheet sheet, int row, int col) {
        Row r = sheet.getRow(row);
        if (r == null) return "";
        Cell c = r.getCell(col);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue();
            case NUMERIC -> String.valueOf(c.getNumericCellValue());
            default -> "";
        };
    }
}
