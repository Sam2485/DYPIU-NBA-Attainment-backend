package com.dypiu.nba.service;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ProgrammeEndSurveyValidationIntegrationTest {

    @Autowired
    private AttainmentCalculationService attainmentService;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProgrammeRepository programmeRepository;

    @Autowired
    private BatchRepository batchRepository;

    @Autowired
    private ProgrammeOutcomeRepository programmeOutcomeRepository;

    @Autowired
    private ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;

    @Autowired
    private UploadedDocumentRepository uploadedDocumentRepository;

    private Programme programme;
    private Batch batch;

    private static final String OFFICIAL_WORKBOOK_PATH = "/Users/rajshaikh/Desktop/testing/Final-Mapping-Attainment Values Sheet (2) (1) (1).xlsx";

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 6);

        School school = schoolRepository.save(School.builder()
                .id("sch-psurv-" + suffix)
                .name("School of Engineering " + suffix)
                .code("SOE-" + suffix)
                .build());

        Department department = departmentRepository.save(Department.builder()
                .id("dept-psurv-" + suffix)
                .schoolId(school.getId())
                .name("Computer Science " + suffix)
                .code("CSE-" + suffix)
                .build());

        programme = programmeRepository.save(Programme.builder()
                .id("prog-psurv-" + suffix)
                .departmentId(department.getId())
                .name("B.Tech Computer Science " + suffix)
                .code("BTECH-CSE-" + suffix)
                .durationYears(4)
                .build());

        batch = batchRepository.save(Batch.builder()
                .id("batch-psurv-" + suffix)
                .programmeId(programme.getId())
                .name("2024-2028")
                .startYear(2024)
                .endYear(2028)
                .durationYears(4)
                .build());

        // Configure PO1 - PO12
        for (int i = 1; i <= 12; i++) {
            programmeOutcomeRepository.save(ProgrammeOutcome.builder()
                    .id("po-psurv-" + i + "-" + suffix)
                    .programmeId(programme.getId())
                    .code("PO" + i)
                    .statement("Program Outcome Statement " + i)
                    .build());
        }

        // Configure PSO1 - PSO3
        for (int i = 1; i <= 3; i++) {
            programmeSpecificOutcomeRepository.save(ProgrammeSpecificOutcome.builder()
                    .id("pso-psurv-" + i + "-" + suffix)
                    .programmeId(programme.getId())
                    .code("PSO" + i)
                    .statement("Program Specific Outcome Statement " + i)
                    .build());
        }
    }

    private MockMultipartFile createSurveyWorkbook(List<String> headerCodes, int numStudentRows) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Average Attainment(ID)");

            // Title rows
            Row r0 = sheet.createRow(0);
            r0.createCell(0).setCellValue("D Y Patil International University, Akurdi Pune");
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("PO & PSO Attainment (Indirect)");

            // Header row at row index 8 (Row 9)
            Row hRow = sheet.createRow(8);
            hRow.createCell(0).setCellValue("Sr No");
            hRow.createCell(1).setCellValue("PRN");
            hRow.createCell(2).setCellValue("Name of the Student");
            for (int i = 0; i < headerCodes.size(); i++) {
                hRow.createCell(3 + i).setCellValue(headerCodes.get(i));
            }

            // Student rows starting at row index 9
            for (int r = 0; r < numStudentRows; r++) {
                Row sRow = sheet.createRow(9 + r);
                sRow.createCell(0).setCellValue(r + 1);
                sRow.createCell(1).setCellValue("PRN" + (1000 + r));
                sRow.createCell(2).setCellValue("Student " + (r + 1));
                for (int i = 0; i < headerCodes.size(); i++) {
                    sRow.createCell(3 + i).setCellValue((r % 3 == 0) ? "Substantial" : ((r % 3 == 1) ? "Moderate" : "Slight"));
                }
            }

            wb.write(bos);
            return new MockMultipartFile(
                    "file",
                    "programme_survey.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bos.toByteArray()
            );
        }
    }

    @Test
    @DisplayName("1. Exact Outcome Match (PO1-PO12 + PSO1-PSO3): Valid Upload Accepted")
    void testExactOutcomeMatchAccepted() throws IOException {
        List<String> headers = List.of(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6",
                "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2", "PSO3"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 25);
        ProgrammeSurveyResultDto result = attainmentService.processAndSaveProgrammeSurveyFile(
                programme.getId(),
                batch.getId(),
                file,
                "Coordinator Test"
        );

        assertNotNull(result);
        assertEquals("PROCESSED", result.getStatus());
        assertEquals(25, result.getRecordsProcessed());
        assertEquals(12, result.getPoIndirectAttainment().size());
        assertEquals(3, result.getPsoIndirectAttainment().size());
    }

    @Test
    @DisplayName("2. Missing Configured PO: Rejected with Controlled Message")
    void testMissingPORejected() throws IOException {
        // Missing PO5
        List<String> headers = List.of(
                "PO1", "PO2", "PO3", "PO4", "PO6",
                "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2", "PSO3"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 20);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attainmentService.processAndSaveProgrammeSurveyFile(programme.getId(), batch.getId(), file, "Coordinator")
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Programme End Survey is missing configured outcome PO5."));
    }

    @Test
    @DisplayName("3. Extra Unconfigured PO: Rejected with Controlled Message")
    void testExtraPORejected() throws IOException {
        // Contains extra PO13
        List<String> headers = List.of(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6",
                "PO7", "PO8", "PO9", "PO10", "PO11", "PO12", "PO13",
                "PSO1", "PSO2", "PSO3"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 20);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attainmentService.processAndSaveProgrammeSurveyFile(programme.getId(), batch.getId(), file, "Coordinator")
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Programme End Survey contains unconfigured outcome PO13."));
    }

    @Test
    @DisplayName("4. Missing Configured PSO: Rejected with Controlled Message")
    void testMissingPSORejected() throws IOException {
        // Missing PSO2
        List<String> headers = List.of(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6",
                "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO3"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 20);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attainmentService.processAndSaveProgrammeSurveyFile(programme.getId(), batch.getId(), file, "Coordinator")
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Programme End Survey is missing configured outcome PSO2."));
    }

    @Test
    @DisplayName("5. Extra Unconfigured PSO: Rejected with Controlled Message")
    void testExtraPSORejected() throws IOException {
        // Contains extra PSO4
        List<String> headers = List.of(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6",
                "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2", "PSO3", "PSO4"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 20);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attainmentService.processAndSaveProgrammeSurveyFile(programme.getId(), batch.getId(), file, "Coordinator")
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Programme End Survey contains unconfigured outcome PSO4."));
    }

    @Test
    @DisplayName("6. Duplicate Outcome in Header: Rejected with Controlled Message")
    void testDuplicateOutcomeRejected() throws IOException {
        // Duplicate PO2
        List<String> headers = List.of(
                "PO1", "PO2", "PO2", "PO3", "PO4", "PO5", "PO6",
                "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2", "PSO3"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 20);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attainmentService.processAndSaveProgrammeSurveyFile(programme.getId(), batch.getId(), file, "Coordinator")
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Programme End Survey contains duplicate outcome PO2."));
    }

    @Test
    @DisplayName("7. Unknown Outcome Code: Rejected with Controlled Message")
    void testUnknownOutcomeRejected() throws IOException {
        // PO7 replaced with unconfigured code
        List<String> headers = List.of(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6",
                "PO99", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2", "PSO3"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 20);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attainmentService.processAndSaveProgrammeSurveyFile(programme.getId(), batch.getId(), file, "Coordinator")
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("Programme End Survey contains unconfigured outcome PO99."));
    }

    @Test
    @DisplayName("8. Reordered Outcome Columns: Accepted if Complete Set Matches")
    void testReorderedOutcomesAccepted() throws IOException {
        // Scrambled column order
        List<String> headers = List.of(
                "PSO3", "PO12", "PO1", "PO11", "PSO1", "PO2", "PO10", "PO3",
                "PSO2", "PO9", "PO4", "PO8", "PO5", "PO7", "PO6"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 30);
        ProgrammeSurveyResultDto result = attainmentService.processAndSaveProgrammeSurveyFile(
                programme.getId(),
                batch.getId(),
                file,
                "Coordinator"
        );

        assertNotNull(result);
        assertEquals(30, result.getRecordsProcessed());
        assertEquals(12, result.getPoIndirectAttainment().size());
        assertEquals(3, result.getPsoIndirectAttainment().size());
    }

    @Test
    @DisplayName("9. Dynamic Dynamic Respondent Count: Works with Any Student Count (87 rows)")
    void testDynamicRespondentCount() throws IOException {
        List<String> headers = List.of(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6",
                "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2", "PSO3"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 87);
        ProgrammeSurveyResultDto result = attainmentService.processAndSaveProgrammeSurveyFile(
                programme.getId(),
                batch.getId(),
                file,
                "Coordinator"
        );

        assertNotNull(result);
        assertEquals(87, result.getRecordsProcessed());
    }

    @Test
    @DisplayName("10. Zero Student Responses: Controlled Error, Never HTTP 500")
    void testZeroResponsesControlledError() throws IOException {
        List<String> headers = List.of(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6",
                "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2", "PSO3"
        );

        MockMultipartFile file = createSurveyWorkbook(headers, 0); // 0 student rows
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attainmentService.processAndSaveProgrammeSurveyFile(programme.getId(), batch.getId(), file, "Coordinator")
        );

        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("No valid survey response rows found"));
    }

    @Test
    @DisplayName("11. Official Workbook PO & PSO Attainment (Indirect) Sheet Upload Test")
    void testOfficialWorkbookProgrammeSurveyUpload() throws Exception {
        File officialFile = new File(OFFICIAL_WORKBOOK_PATH);
        if (!officialFile.exists()) {
            System.out.println("Official workbook not found at " + OFFICIAL_WORKBOOK_PATH + ", skipping test.");
            return;
        }

        MockMultipartFile multipartFile;
        try (FileInputStream fis = new FileInputStream(officialFile)) {
            multipartFile = new MockMultipartFile(
                    "file",
                    "Final-Mapping-Attainment Values Sheet (2) (1) (1).xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fis
            );
        }

        // The raw template has no student entries, so it should be safely rejected with HTTP 400
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                attainmentService.processAndSaveProgrammeSurveyFile(
                        programme.getId(),
                        batch.getId(),
                        multipartFile,
                        "Programme Coordinator"
                )
        );
        assertEquals(400, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("No valid survey response rows found"));
    }
}
