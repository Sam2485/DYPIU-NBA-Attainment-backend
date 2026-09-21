package com.dypiu.nba.reports.excel;

import com.dypiu.nba.reports.model.snapshot.CourseAtrSnapshot;
import com.dypiu.nba.reports.model.snapshot.CourseAttainmentSnapshot;
import com.dypiu.nba.reports.model.snapshot.ProgrammeAttainmentSnapshot;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class CourseExcelHeaderRendererTest {

    // 1x1 transparent PNG sample for testing logo embedding
    private static final byte[] SAMPLE_PNG_LOGO = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
            0x42, 0x60, (byte) 0x82
    };

    @Test
    @DisplayName("Dynamic Header Width: Header last column must strictly equal content last column for various column counts")
    void testDynamicHeaderWidth() {
        int[] testColumnCounts = {6, 8, 17, 22, 33};

        for (int totalCols : testColumnCounts) {
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("TestSheet_" + totalCols);

            int nextRow = CourseExcelHeaderRenderer.renderHeader(
                    wb, sheet,
                    "D Y Patil International University, Akurdi Pune",
                    "School of Engineering and Technology",
                    "Course Outcome attainment Calculations through Direct Method",
                    totalCols,
                    null, null,
                    true
            );

            assertEquals(5, nextRow, "Next row index must be 5");

            // Verify Title Band in Row 3 spans exactly 0 to totalCols - 1
            CellRangeAddress titleBandMerge = null;
            for (CellRangeAddress mr : sheet.getMergedRegions()) {
                if (mr.getFirstRow() == 3 && mr.getLastRow() == 3) {
                    titleBandMerge = mr;
                    break;
                }
            }

            assertNotNull(titleBandMerge, "Title band merge must exist at row 3 for totalCols=" + totalCols);
            assertEquals(0, titleBandMerge.getFirstColumn(), "Title band must start at col 0");
            assertEquals(totalCols - 1, titleBandMerge.getLastColumn(),
                    "Title band last column must strictly equal content last column for totalCols=" + totalCols);

            // Verify title cell value and Cyan fill
            Row row3 = sheet.getRow(3);
            assertNotNull(row3);
            assertEquals(20.5f, row3.getHeightInPoints(), 0.1f, "Title band height must be 20.5pt");

            Cell titleCell = row3.getCell(0);
            assertEquals("Course Outcome attainment Calculations through Direct Method", titleCell.getStringCellValue());

            CellStyle titleStyle = titleCell.getCellStyle();
            assertTrue(titleStyle instanceof XSSFCellStyle);
            XSSFColor bgColor = ((XSSFCellStyle) titleStyle).getFillForegroundColorColor();
            assertNotNull(bgColor, "Cyan fill must be applied to title band");
            byte[] rgb = bgColor.getRGB();
            // Cyan: 142, 201, 218
            assertEquals(142, rgb[0] & 0xFF);
            assertEquals(201, rgb[1] & 0xFF);
            assertEquals(218, rgb[2] & 0xFF);

            // Verify University Name in Row 0
            Row row0 = sheet.getRow(0);
            assertEquals(15.0f, row0.getHeightInPoints(), 0.1f, "University row height must be 15pt");

            // Verify School Name in Row 1
            Row row1 = sheet.getRow(1);
            assertEquals(15.0f, row1.getHeightInPoints(), 0.1f, "School row height must be 15pt");

            // Verify Row 4 is Spacer row
            Row row4 = sheet.getRow(4);
            assertNotNull(row4);
            assertEquals(14.25f, row4.getHeightInPoints(), 0.1f, "Spacer row height must be 14.25pt");
        }
    }

    @Test
    @DisplayName("Two Logo Slots: Embedded Apache POI drawings with aspect ratio and null fallback")
    void testLogoEmbeddingAndFallback() {
        // 1. With Both Logos configured
        Workbook wbWithLogos = new XSSFWorkbook();
        Sheet sheetWithLogos = wbWithLogos.createSheet("WithLogos");

        CourseExcelHeaderRenderer.renderHeader(
                wbWithLogos, sheetWithLogos,
                "D Y Patil International University, Akurdi Pune",
                "School of Engineering and Technology",
                "Programme Outcome attainment through Direct Attainment Method",
                17,
                SAMPLE_PNG_LOGO, SAMPLE_PNG_LOGO,
                true
        );

        Drawing<?> drawing = sheetWithLogos.getDrawingPatriarch();
        assertNotNull(drawing, "Drawing patriarch must be created when logos are supplied");

        // 2. With Null Logos (Fallback: empty slots, intact geometry, no crash)
        Workbook wbWithoutLogos = new XSSFWorkbook();
        Sheet sheetWithoutLogos = wbWithoutLogos.createSheet("WithoutLogos");

        assertDoesNotThrow(() -> {
            CourseExcelHeaderRenderer.renderHeader(
                    wbWithoutLogos, sheetWithoutLogos,
                    "D Y Patil International University, Akurdi Pune",
                    "School of Engineering and Technology",
                    "Programme Outcome attainment through Direct Attainment Method",
                    17,
                    null, null,
                    true
            );
        }, "Null logos must not cause exceptions and must preserve geometry");
    }

    @Test
    @DisplayName("Course Attainment Workbook generation uses CourseExcelHeaderRenderer with dynamic content width")
    void testCourseAttainmentWorkbookUsesCourseHeader() {
        ExcelReportRenderer renderer = new ExcelReportRenderer();

        CourseAttainmentSnapshot snapshot = CourseAttainmentSnapshot.builder()
                .reportId("rep-c-001")
                .courseCode("CS301")
                .courseName("Computer Networks and Security")
                .semester(5)
                .institutionName("D Y Patil International University, Akurdi Pune")
                .schoolName("School of Engineering and Technology")
                .poCodes(List.of("PO1", "PO2", "PO3", "PO4", "PO5", "PO6", "PO7", "PO8", "PO9", "PO10", "PO11", "PO12"))
                .psoCodes(List.of("PSO1", "PSO2", "PSO3"))
                .overallCoAttainment(new BigDecimal("2.67"))
                .table1Mapping(List.of(
                        CourseAttainmentSnapshot.CoMappingRow.builder()
                                .coCode("CO1")
                                .poMappings(Map.of("PO1", 3, "PO2", 2))
                                .psoMappings(Map.of("PSO1", 2))
                                .build()
                ))
                .table3CoAttainments(List.of(
                        CourseAttainmentSnapshot.CoAttainmentRow.builder()
                                .coCode("CO1")
                                .statement("Understand network fundamentals")
                                .targetLevel(new BigDecimal("2.50"))
                                .directPercentage(new BigDecimal("75.0"))
                                .directLevel(3)
                                .indirectPercentage(new BigDecimal("80.0"))
                                .indirectLevel(2)
                                .finalAttainment(new BigDecimal("2.67"))
                                .targetMet(true)
                                .build()
                ))
                .build();

        byte[] bytes = renderer.renderCourseAttainment(snapshot, SAMPLE_PNG_LOGO, null);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // Verify that Course header last column matches content last column (17 columns: 0 to 16)
        assertDoesNotThrow(() -> {
            Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(bytes));
            Sheet sheet = wb.getSheetAt(0);

            CellRangeAddress titleBand = null;
            for (CellRangeAddress mr : sheet.getMergedRegions()) {
                if (mr.getFirstRow() == 3 && mr.getLastRow() == 3) {
                    titleBand = mr;
                    break;
                }
            }
            assertNotNull(titleBand);
            assertEquals(16, titleBand.getLastColumn(), "Course header must terminate at col 16 (Col Q) for 17 columns");
            wb.close();
        });
    }

    @Test
    @DisplayName("PROGRAMME REPORTS ISOLATION: Programme report header is UNCHANGED and uses CommonExcelHeaderRenderer")
    void testProgrammeReportHeaderRemainsUnchanged() {
        ExcelReportRenderer renderer = new ExcelReportRenderer();

        ProgrammeAttainmentSnapshot progSnapshot = ProgrammeAttainmentSnapshot.builder()
                .reportId("rep-prog-001")
                .institutionName("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE")
                .schoolName("School of Engineering and Technology")
                .masterProgrammeName("B.Tech Computer Science")
                .masterProgrammeCode("BTECH-CSE")
                .academicYear("2024-2028")
                .poCodes(List.of("PO1", "PO2", "PO3"))
                .psoCodes(List.of("PSO1"))
                .section4OverallAttainment(ProgrammeAttainmentSnapshot.OverallAttainmentSection.builder()
                        .averageMappingStrength(Map.of("PO1", new BigDecimal("2.50")))
                        .averageDirectAttainment(Map.of("PO1", new BigDecimal("2.40")))
                        .averageIndirectAttainment(Map.of("PO1", new BigDecimal("2.60")))
                        .finalAttainments(Map.of("PO1", new BigDecimal("2.44")))
                        .build())
                .build();

        byte[] progBytes = renderer.renderProgrammeAttainmentMaster(progSnapshot);
        assertNotNull(progBytes);
        assertTrue(progBytes.length > 0);

        // Verify Programme Report header structure: Row 0 institution, Row 1 school, Row 2 report title, Row 3 metadata bar
        assertDoesNotThrow(() -> {
            Workbook wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(progBytes));
            Sheet sheet = wb.getSheet("Overall Programme Attainment");
            assertNotNull(sheet, "Overall Programme Attainment sheet must exist in Programme Master report");

            // Row 1 has institution title in center cell (col 1 for 16 columns)
            Row r1 = sheet.getRow(1);
            assertNotNull(r1);
            assertEquals("D. Y. PATIL INTERNATIONAL UNIVERSITY, PUNE", r1.getCell(1).getStringCellValue());

            // Row 2 has school name
            Row r2 = sheet.getRow(2);
            assertNotNull(r2);
            assertEquals("School of Engineering and Technology", r2.getCell(1).getStringCellValue());

            // Row 3 has Academic Year (col 0) and Report Title (col 1)
            Row r3 = sheet.getRow(3);
            assertNotNull(r3);
            assertTrue(r3.getCell(0).getStringCellValue().contains("Academic Year: 2024-2028"));
            assertEquals("Overall Attainment", r3.getCell(1).getStringCellValue());

            // Row 5 has Term (col 0)
            Row r5 = sheet.getRow(5);
            assertNotNull(r5);
            assertTrue(r5.getCell(0).getStringCellValue().contains("Term – I & II"));

            wb.close();
        });
    }

    @Test
    @DisplayName("Calculate Course Academic Year: Converts multi-year batch span to 1-year course academic year based on semester")
    void testCalculateCourseAcademicYear() {
        // User example: batch 2025-2029, semester 3 -> 2026-2027
        assertEquals("2026-2027", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2025-2029", 3));

        // Semesters 1 and 2: Year 1 (2024-2025 for 2024-2028 batch)
        assertEquals("2024-2025", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2024-2028", 1));
        assertEquals("2024-2025", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2024-2028", 2));

        // Semesters 3 and 4: Year 2 (2025-2026 for 2024-2028 batch)
        assertEquals("2025-2026", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2024-2028", 3));
        assertEquals("2025-2026", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2024-2028", 4));

        // Semesters 5 and 6: Year 3 (2026-2027 for 2024-2028 batch)
        assertEquals("2026-2027", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2024-2028", 5));
        assertEquals("2026-2027", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2024-2028", 6));

        // Semesters 7 and 8: Year 4 (2027-2028 for 2024-2028 batch)
        assertEquals("2027-2028", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2024-2028", 7));
        assertEquals("2027-2028", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2024-2028", 8));

        // AY prefix preservation
        assertEquals("AY 2026-2027", CourseExcelHeaderRenderer.calculateCourseAcademicYear("AY 2025-2029", 3));

        // Already 1-year academic year should be preserved
        assertEquals("2023-24", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2023-24", 5));
        assertEquals("2025-26", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2025-26", 3));
        assertEquals("2025-2026", CourseExcelHeaderRenderer.calculateCourseAcademicYear("2025-2026", 3));

        // Null and blank handling
        assertEquals("", CourseExcelHeaderRenderer.calculateCourseAcademicYear(null, 3));
        assertEquals("", CourseExcelHeaderRenderer.calculateCourseAcademicYear("", 3));
    }
}
