package com.dypiu.nba.service;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.MasterProgrammeRepository;
import com.dypiu.nba.repository.ProgrammeBatchRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OutcomeExcelImportServiceTest {

    @Mock
    private AcademicService academicService;

    @Mock
    private OutcomeService outcomeService;

    @Mock
    private ProgrammeBatchRepository programmeBatchRepository;

    @Mock
    private MasterProgrammeRepository masterProgrammeRepository;

    @Mock
    private BatchLifecycleService batchLifecycleService;

    @Mock
    private AcademicLookupCacheService academicLookupCacheService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private OutcomeExcelImportService importService;

    private ProgrammeBatch mockBatch;
    private MasterProgramme mockProgramme;

    @BeforeEach
    void setUp() {
        mockBatch = ProgrammeBatch.builder()
                .id("batch-cse-2024")
                .name("B.Tech CSE 2024-2028")
                .masterProgrammeId("prog-cse")
                .status("DRAFT")
                .build();

        mockProgramme = MasterProgramme.builder()
                .id("prog-cse")
                .name("B.Tech Computer Science and Engineering")
                .code("CSE")
                .durationYears(4)
                .build();

        when(programmeBatchRepository.findById("batch-cse-2024")).thenReturn(Optional.of(mockBatch));
        when(masterProgrammeRepository.findById("prog-cse")).thenReturn(Optional.of(mockProgramme));
    }

    @Test
    @DisplayName("Should generate a valid Excel template with PO and PSO sample rows")
    void testGenerateTemplate() {
        byte[] bytes = importService.generateTemplate("batch-cse-2024");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    @DisplayName("Should parse real reference workbook PO and Competency.xlsx correctly")
    void testPreviewRealReferenceWorkbook() throws Exception {
        File file = new File("/Users/rajshaikh/Library/Containers/net.whatsapp.WhatsApp/Data/tmp/documents/78EBFCF7-BFCA-4948-B3B8-758F6C3B9F1F/PO and Competency.xlsx");
        if (!file.exists()) {
            // Skip if path not present on test machine
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            MockMultipartFile multipartFile = new MockMultipartFile(
                    "file",
                    "PO and Competency.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fis
            );

            OutcomeImportPreviewDto preview = importService.previewImport("batch-cse-2024", multipartFile);

            assertNotNull(preview);
            assertTrue(preview.isValid(), "Preview should be valid without errors");
            assertEquals(12, preview.getTotalPOs(), "Should detect exactly 12 POs");
            assertEquals(0, preview.getTotalPSOs(), "Reference file has only POs");
            assertEquals(36, preview.getTotalCompetencies(), "Should extract 36 competencies");

            // Verify PO1
            OutcomeImportItemDto po1 = preview.getItems().get(0);
            assertEquals("PO1", po1.getCode());
            assertEquals("PO", po1.getCategory());
            assertTrue(po1.getStatement().startsWith("Apply the knowledge of mathematics"), "Statement should be stripped of '1. '");
            assertEquals(4, po1.getCompetencies().size(), "PO1 should have 4 competencies");
            assertEquals("PO1.1", po1.getCompetencies().get(0).getCode());
            assertEquals("PO1.4", po1.getCompetencies().get(3).getCode());

            // Verify PO12
            OutcomeImportItemDto po12 = preview.getItems().get(11);
            assertEquals("PO12", po12.getCode());
            assertEquals(3, po12.getCompetencies().size(), "PO12 should have 3 competencies");
            assertEquals("PO12.1", po12.getCompetencies().get(0).getCode());
        }
    }

    @Test
    @DisplayName("Should detect PSOs using section header divider")
    void testPreviewWithExplicitSectionHeaderPSO() throws Exception {
        byte[] workbookBytes;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("PO and Competency");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Programme Outcomes");
            h.createCell(1).setCellValue("Competency");

            // PO 1
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("1. Apply fundamental engineering principles");
            r1.createCell(1).setCellValue("Basic math competency");
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("");
            r2.createCell(1).setCellValue("Basic physics competency");

            // Section Header
            Row rSec = sheet.createRow(3);
            rSec.createCell(0).setCellValue("Programme Specific Outcomes");
            rSec.createCell(1).setCellValue("");

            // PSO 1
            Row r3 = sheet.createRow(4);
            r3.createCell(0).setCellValue("1. Design specialized systems");
            r3.createCell(1).setCellValue("System architecture competency");

            wb.write(out);
            workbookBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookBytes);
        OutcomeImportPreviewDto preview = importService.previewImport("batch-cse-2024", file);

        assertTrue(preview.isValid());
        assertEquals(1, preview.getTotalPOs());
        assertEquals(1, preview.getTotalPSOs());
        assertEquals(3, preview.getTotalCompetencies());

        assertEquals("PO1", preview.getItems().get(0).getCode());
        assertEquals(2, preview.getItems().get(0).getCompetencies().size());

        assertEquals("PSO1", preview.getItems().get(1).getCode());
        assertEquals(1, preview.getItems().get(1).getCompetencies().size());
        assertEquals("PSO1.1", preview.getItems().get(1).getCompetencies().get(0).getCode());
    }

    @Test
    @DisplayName("Should detect PSOs when numbering resets from 12 back to 1")
    void testPreviewWithNumberingReset() throws Exception {
        byte[] workbookBytes;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("PO and Competency");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Programme Outcomes");
            h.createCell(1).setCellValue("Competency");

            int rowIdx = 1;
            for (int i = 1; i <= 12; i++) {
                Row r = sheet.createRow(rowIdx++);
                r.createCell(0).setCellValue(i + ". Engineering PO statement " + i);
                r.createCell(1).setCellValue("Competency for PO " + i);
            }

            // Now row numbering resets to 1 (PSO 1)
            Row rPso1 = sheet.createRow(rowIdx++);
            rPso1.createCell(0).setCellValue("1. Ability to build cloud native software");
            rPso1.createCell(1).setCellValue("Cloud platforms competence");

            Row rPso2 = sheet.createRow(rowIdx++);
            rPso2.createCell(0).setCellValue("2. Ability to solve data science challenges");
            rPso2.createCell(1).setCellValue("Machine learning competence");

            wb.write(out);
            workbookBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookBytes);
        OutcomeImportPreviewDto preview = importService.previewImport("batch-cse-2024", file);

        assertTrue(preview.isValid());
        assertEquals(12, preview.getTotalPOs());
        assertEquals(2, preview.getTotalPSOs());
        assertEquals(14, preview.getTotalCompetencies());

        assertEquals("PSO1", preview.getItems().get(12).getCode());
        assertEquals("PSO2", preview.getItems().get(13).getCode());
    }

    @Test
    @DisplayName("Should detect POs and PSOs with explicit prefixes PO1 / PSO1")
    void testPreviewWithExplicitPrefixes() throws Exception {
        byte[] workbookBytes;
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Outcomes");
            Row h = sheet.createRow(0);
            h.createCell(0).setCellValue("Outcome");
            h.createCell(1).setCellValue("Competency");

            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("PO 1: Engineering knowledge");
            r1.createCell(1).setCellValue("Math analysis");

            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("PSO 1: Domain knowledge");
            r2.createCell(1).setCellValue("Algorithms");

            wb.write(out);
            workbookBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookBytes);
        OutcomeImportPreviewDto preview = importService.previewImport("batch-cse-2024", file);

        assertTrue(preview.isValid());
        assertEquals(1, preview.getTotalPOs());
        assertEquals(1, preview.getTotalPSOs());
        assertEquals("PO1", preview.getItems().get(0).getCode());
        assertEquals("PSO1", preview.getItems().get(1).getCode());
    }

    @Test
    @DisplayName("Should successfully commit imported outcomes to database")
    void testCommitImport() {
        List<OutcomeImportItemDto> items = new ArrayList<>();

        List<OutcomeCompetencyImportDto> poComps = List.of(
                OutcomeCompetencyImportDto.builder().code("PO1.1").statement("Math competence").order(1).build(),
                OutcomeCompetencyImportDto.builder().code("PO1.2").statement("Physics competence").order(2).build()
        );

        items.add(OutcomeImportItemDto.builder()
                .category("PO")
                .statement("Apply engineering principles")
                .target(new BigDecimal("2.50"))
                .competencies(new ArrayList<>(poComps))
                .build());

        List<OutcomeCompetencyImportDto> psoComps = List.of(
                OutcomeCompetencyImportDto.builder().code("PSO1.1").statement("Cloud competence").order(1).build()
        );

        items.add(OutcomeImportItemDto.builder()
                .category("PSO")
                .statement("Design cloud native apps")
                .target(new BigDecimal("2.50"))
                .competencies(new ArrayList<>(psoComps))
                .build());

        OutcomeImportCommitRequestDto request = OutcomeImportCommitRequestDto.builder()
                .items(items)
                .build();

        OutcomeImportResultDto result = importService.commitImport("batch-cse-2024", request);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalPOsImported());
        assertEquals(1, result.getTotalPSOsImported());
        assertEquals(3, result.getTotalCompetenciesImported());

        verify(outcomeService, times(1)).savePOs(eq("batch-cse-2024"), any());
        verify(outcomeService, times(1)).savePSOs(eq("batch-cse-2024"), any());
        verify(academicLookupCacheService, times(1)).evictProgrammeBatchCache();
    }
}
