package com.dypiu.nba.service;

import com.dypiu.nba.audit.AuditAction;
import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.MasterProgrammeRepository;
import com.dypiu.nba.repository.ProgrammeBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutcomeExcelImportService {

    private final AcademicService academicService;
    private final OutcomeService outcomeService;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final BatchLifecycleService batchLifecycleService;
    private final AcademicLookupCacheService academicLookupCacheService;
    private final AuditLogService auditLogService;

    // Detection Regex Patterns
    private static final Pattern EXPLICIT_PSO_PATTERN =
            Pattern.compile("(?i)^\\s*PSO\\s*(\\d+)[.:\\-\\s]*(.*)$", Pattern.DOTALL);

    private static final Pattern EXPLICIT_PO_PATTERN =
            Pattern.compile("(?i)^\\s*PO\\s*(\\d+)[.:\\-\\s]*(.*)$", Pattern.DOTALL);

    private static final Pattern NUMBERED_PREFIX_PATTERN =
            Pattern.compile("^\\s*(\\d+)[.):\\-\\s]+(.*)$", Pattern.DOTALL);

    private static final Pattern PSO_SECTION_HEADER_PATTERN =
            Pattern.compile("(?i)^\\s*(programme\\s+specific\\s+outcomes?|program\\s+specific\\s+outcomes?|psos?|pso\\s+outcomes?|programme\\s+specific\\s+outcome|specific\\s+outcomes?)\\s*$");

    private static final Pattern PO_SECTION_HEADER_PATTERN =
            Pattern.compile("(?i)^\\s*(programme\\s+outcomes?|program\\s+outcomes?|pos?|po\\s+outcomes?|programme\\s+outcome|general\\s+outcomes?)\\s*$");

    /**
     * Generates a downloadable Excel template for PO / PSO and Competencies.
     */
    public byte[] generateTemplate(String programmeBatchId) {
        if (programmeBatchId == null || programmeBatchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programmeBatchId is required.");
        }

        academicService.enforceBatchScope(programmeBatchId);

        ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme Batch not found: " + programmeBatchId));

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 10);

            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle sectionStyle = wb.createCellStyle();
            Font sectionFont = wb.createFont();
            sectionFont.setBold(true);
            sectionFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            sectionStyle.setFont(sectionFont);
            sectionStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            sectionStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle wrapStyle = wb.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            Sheet sheet = wb.createSheet("PO and Competency");
            sheet.setDisplayGridlines(true);

            // Header Row
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);

            Cell h1 = headerRow.createCell(0);
            h1.setCellValue("Programme Outcomes / PSOs *");
            h1.setCellStyle(headerStyle);

            Cell h2 = headerRow.createCell(1);
            h2.setCellValue("Competency *");
            h2.setCellStyle(headerStyle);

            int r = 1;

            // Sample PO 1 with 3 competencies
            Row row1 = sheet.createRow(r++);
            row1.createCell(0).setCellValue("1. Apply the knowledge of mathematics, science, engineering fundamentals and engineering specialization to the solution of complex engineering problems.");
            row1.getCell(0).setCellStyle(wrapStyle);
            row1.createCell(1).setCellValue("Demonstrate competence in knowledge of basic & applied mathematics.");
            row1.getCell(1).setCellStyle(wrapStyle);

            Row row2 = sheet.createRow(r++);
            row2.createCell(0).setCellValue("");
            row2.createCell(1).setCellValue("Demonstrate competence in basic sciences.");
            row2.getCell(1).setCellStyle(wrapStyle);

            Row row3 = sheet.createRow(r++);
            row3.createCell(0).setCellValue("");
            row3.createCell(1).setCellValue("Demonstrate competence in engineering fundamentals.");
            row3.getCell(1).setCellStyle(wrapStyle);

            // Sample PO 2 with 2 competencies
            Row row4 = sheet.createRow(r++);
            row4.createCell(0).setCellValue("2. Identify, formulate, review research literature, and analyze complex engineering problems.");
            row4.getCell(0).setCellStyle(wrapStyle);
            row4.createCell(1).setCellValue("Demonstrate an ability to identify and formulate complex engineering problems.");
            row4.getCell(1).setCellStyle(wrapStyle);

            Row row5 = sheet.createRow(r++);
            row5.createCell(0).setCellValue("");
            row5.createCell(1).setCellValue("Demonstrate an ability to formulate a solution plan and methodology.");
            row5.getCell(1).setCellStyle(wrapStyle);

            // Section Header for PSOs
            Row sectionRow = sheet.createRow(r++);
            Cell secCell = sectionRow.createCell(0);
            secCell.setCellValue("Programme Specific Outcomes");
            secCell.setCellStyle(sectionStyle);
            sectionRow.createCell(1).setCellValue("");

            // Sample PSO 1 with 2 competencies
            Row row6 = sheet.createRow(r++);
            row6.createCell(0).setCellValue("1. Ability to design and develop software solutions in specialized computing domains.");
            row6.getCell(0).setCellStyle(wrapStyle);
            row6.createCell(1).setCellValue("Apply modern software design paradigms to system development.");
            row6.getCell(1).setCellStyle(wrapStyle);

            Row row7 = sheet.createRow(r++);
            row7.createCell(0).setCellValue("");
            row7.createCell(1).setCellValue("Demonstrate testing and quality assurance practices.");
            row7.getCell(1).setCellStyle(wrapStyle);

            sheet.setColumnWidth(0, 16000);
            sheet.setColumnWidth(1, 16000);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate outcome template for batch {}", programmeBatchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate template: " + e.getMessage());
        }
    }

    /**
     * Parses the uploaded workbook and returns validation & preview without persisting.
     */
    public OutcomeImportPreviewDto previewImport(String programmeBatchId, MultipartFile file) {
        if (programmeBatchId == null || programmeBatchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programmeBatchId is required.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An Excel file (.xlsx) is required.");
        }

        academicService.enforceBatchScope(programmeBatchId);

        ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme Batch not found: " + programmeBatchId));

        MasterProgramme masterProg = null;
        if (batch.getMasterProgrammeId() != null) {
            masterProg = masterProgrammeRepository.findById(batch.getMasterProgrammeId()).orElse(null);
        }

        List<String> globalErrors = new ArrayList<>();
        List<String> globalWarnings = new ArrayList<>();
        List<OutcomeImportItemDto> rawItems = new ArrayList<>();

        DataFormatter formatter = new DataFormatter();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            if (workbook.getNumberOfSheets() == 0) {
                globalErrors.add("The uploaded workbook contains no sheets.");
                return buildEmptyPreview(programmeBatchId, batch, masterProg, globalErrors, globalWarnings);
            }

            // Find target sheet (prefer name with 'po' or 'outcome' or 'competenc')
            Sheet targetSheet = null;
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sh = workbook.getSheetAt(s);
                String name = sh.getSheetName().toLowerCase();
                if (name.contains("po") || name.contains("outcome") || name.contains("competenc")) {
                    targetSheet = sh;
                    break;
                }
            }
            if (targetSheet == null) {
                targetSheet = workbook.getSheetAt(0);
            }

            // Identify header row and column indices
            int outcomeCol = 0;
            int competencyCol = 1;
            int startRow = 0;

            int lastRowNum = targetSheet.getLastRowNum();
            for (int r = 0; r <= Math.min(10, lastRowNum); r++) {
                Row row = targetSheet.getRow(r);
                if (row == null) continue;
                String c0 = getCellText(row, 0, formatter).toLowerCase();
                String c1 = getCellText(row, 1, formatter).toLowerCase();

                if (c0.contains("outcome") || c0.contains("po") || c0.contains("statement")) {
                    outcomeCol = 0;
                    if (c1.contains("competenc") || c1.contains("indicator") || c1.contains("sub")) {
                        competencyCol = 1;
                    }
                    startRow = r + 1;
                    break;
                } else if (c1.contains("outcome") || c1.contains("po")) {
                    outcomeCol = 1;
                    competencyCol = 0;
                    startRow = r + 1;
                    break;
                }
            }

            // Deterministic state-machine parser
            String currentSection = "PO";
            int poCounter = 0;
            int psoCounter = 0;
            OutcomeImportItemDto currentOutcome = null;

            for (int r = startRow; r <= lastRowNum; r++) {
                Row row = targetSheet.getRow(r);
                if (row == null) continue;

                String rawOutcomeText = getCellText(row, outcomeCol, formatter);
                String rawCompetencyText = getCellText(row, competencyCol, formatter);

                if (rawOutcomeText.isBlank() && rawCompetencyText.isBlank()) {
                    continue; // Skip entirely empty row
                }

                // Check for Section Header Row
                if (!rawOutcomeText.isBlank()) {
                    if (PSO_SECTION_HEADER_PATTERN.matcher(rawOutcomeText).matches()) {
                        currentSection = "PSO";
                        currentOutcome = null;
                        continue;
                    }
                    if (PO_SECTION_HEADER_PATTERN.matcher(rawOutcomeText).matches()) {
                        currentSection = "PO";
                        currentOutcome = null;
                        continue;
                    }
                }

                // New Outcome detected
                if (!rawOutcomeText.isBlank()) {
                    String category;
                    String detectionRule;
                    String cleanStatement = rawOutcomeText;

                    Matcher psoPrefixMatcher = EXPLICIT_PSO_PATTERN.matcher(rawOutcomeText);
                    Matcher poPrefixMatcher = EXPLICIT_PO_PATTERN.matcher(rawOutcomeText);
                    Matcher numMatcher = NUMBERED_PREFIX_PATTERN.matcher(rawOutcomeText);

                    if (psoPrefixMatcher.matches()) {
                        category = "PSO";
                        detectionRule = "EXPLICIT_PREFIX";
                        currentSection = "PSO";
                        String rest = psoPrefixMatcher.group(2).trim();
                        cleanStatement = rest.isEmpty() ? rawOutcomeText : rest;
                    } else if (poPrefixMatcher.matches()) {
                        category = "PO";
                        detectionRule = "EXPLICIT_PREFIX";
                        currentSection = "PO";
                        String rest = poPrefixMatcher.group(2).trim();
                        cleanStatement = rest.isEmpty() ? rawOutcomeText : rest;
                    } else if (numMatcher.matches()) {
                        int num = Integer.parseInt(numMatcher.group(1));
                        String rest = numMatcher.group(2).trim();
                        cleanStatement = rest.isEmpty() ? rawOutcomeText : rest;

                        if ("PSO".equalsIgnoreCase(currentSection)) {
                            category = "PSO";
                            detectionRule = "SECTION_HEADER";
                        } else {
                            if (num == 1 && poCounter > 0) {
                                // Numbering reset detected!
                                category = "PSO";
                                detectionRule = "NUMBERING_RESET";
                                currentSection = "PSO";
                            } else if (num > 12) {
                                // NBA POs are 1..12; higher numbers are typically PSOs
                                category = "PSO";
                                detectionRule = "NUMBER_GT_12";
                                currentSection = "PSO";
                            } else {
                                category = "PO";
                                detectionRule = "SEQUENTIAL_NUMBER";
                            }
                        }
                    } else {
                        category = currentSection;
                        detectionRule = "PSO".equalsIgnoreCase(currentSection) ? "SECTION_HEADER" : "DEFAULT_SECTION";
                    }

                    cleanStatement = cleanText(cleanStatement);

                    if ("PO".equalsIgnoreCase(category)) {
                        poCounter++;
                    } else {
                        psoCounter++;
                    }

                    OutcomeImportItemDto newItem = OutcomeImportItemDto.builder()
                            .id("item-" + (rawItems.size() + 1))
                            .rowNumber(r + 1)
                            .category(category)
                            .originalCategory(category)
                            .statement(cleanStatement)
                            .detectionRule(detectionRule)
                            .target(new BigDecimal("2.50"))
                            .competencies(new ArrayList<>())
                            .issues(new ArrayList<>())
                            .status("VALID")
                            .build();

                    if (!rawCompetencyText.isBlank()) {
                        newItem.getCompetencies().add(OutcomeCompetencyImportDto.builder()
                                .id("comp-" + newItem.getId() + "-1")
                                .statement(cleanText(rawCompetencyText))
                                .order(1)
                                .build());
                    }

                    currentOutcome = newItem;
                    rawItems.add(currentOutcome);
                } else {
                    // Col A is blank, Col B has competency text -> continuation row!
                    if (!rawCompetencyText.isBlank()) {
                        if (currentOutcome != null) {
                            int order = currentOutcome.getCompetencies().size() + 1;
                            currentOutcome.getCompetencies().add(OutcomeCompetencyImportDto.builder()
                                    .id("comp-" + currentOutcome.getId() + "-" + order)
                                    .statement(cleanText(rawCompetencyText))
                                    .order(order)
                                    .build());
                        } else {
                            globalWarnings.add("Row " + (r + 1) + ": Competency statement found before any Programme Outcome was defined: '" + rawCompetencyText + "'.");
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse outcome Excel file for batch {}", programmeBatchId, e);
            globalErrors.add("Failed to read Excel file: " + e.getMessage());
            return buildEmptyPreview(programmeBatchId, batch, masterProg, globalErrors, globalWarnings);
        }

        if (rawItems.isEmpty()) {
            globalErrors.add("No outcomes or competencies were found in the uploaded workbook.");
            return buildEmptyPreview(programmeBatchId, batch, masterProg, globalErrors, globalWarnings);
        }

        // Renumber codes and perform validations
        renumberAndValidate(rawItems, globalWarnings);

        int totalPOs = (int) rawItems.stream().filter(i -> "PO".equalsIgnoreCase(i.getCategory())).count();
        int totalPSOs = (int) rawItems.stream().filter(i -> "PSO".equalsIgnoreCase(i.getCategory())).count();
        int totalCompetencies = rawItems.stream().mapToInt(i -> i.getCompetencies() != null ? i.getCompetencies().size() : 0).sum();

        int errorCount = (int) rawItems.stream().filter(i -> "INVALID".equalsIgnoreCase(i.getStatus())).count();
        int warningCount = (int) rawItems.stream().filter(i -> "WARNING".equalsIgnoreCase(i.getStatus())).count();
        int validCount = (int) rawItems.stream().filter(i -> "VALID".equalsIgnoreCase(i.getStatus())).count();

        boolean isValid = globalErrors.isEmpty() && errorCount == 0;

        return OutcomeImportPreviewDto.builder()
                .valid(isValid)
                .programmeBatchId(batch.getId())
                .programmeBatchName(batch.getName())
                .masterProgrammeName(masterProg != null ? masterProg.getName() : null)
                .totalOutcomes(rawItems.size())
                .totalPOs(totalPOs)
                .totalPSOs(totalPSOs)
                .totalCompetencies(totalCompetencies)
                .validCount(validCount)
                .warningCount(warningCount)
                .errorCount(errorCount)
                .items(rawItems)
                .errors(globalErrors)
                .warnings(globalWarnings)
                .build();
    }

    /**
     * Commits the confirmed outcomes and competencies to the database within a single transaction.
     */
    @Transactional
    public OutcomeImportResultDto commitImport(String programmeBatchId, OutcomeImportCommitRequestDto request) {
        if (programmeBatchId == null || programmeBatchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programmeBatchId is required.");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No outcomes provided to import.");
        }

        academicService.enforceBatchScope(programmeBatchId);
        batchLifecycleService.enforceBatchEditability(programmeBatchId);

        List<OutcomeImportItemDto> items = request.getItems();

        // Renumber based on current categories
        List<String> warnings = new ArrayList<>();
        renumberAndValidate(items, warnings);

        List<ProgrammeOutcome> posToSave = new ArrayList<>();
        List<ProgrammeSpecificOutcome> psosToSave = new ArrayList<>();

        int poIdx = 1;
        int psoIdx = 1;
        int totalCompetencies = 0;

        for (OutcomeImportItemDto item : items) {
            String cat = item.getCategory() != null ? item.getCategory().trim().toUpperCase() : "PO";
            String statement = item.getStatement() != null ? item.getStatement().trim() : "";
            if (statement.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Outcome statement cannot be blank (Row " + item.getRowNumber() + ").");
            }

            BigDecimal target = item.getTarget() != null ? item.getTarget() : new BigDecimal("2.50");

            if ("PO".equalsIgnoreCase(cat)) {
                String poCode = "PO" + poIdx;
                String poId = "po-" + UUID.randomUUID().toString().substring(0, 8);

                List<PoCompetency> poComps = new ArrayList<>();
                if (item.getCompetencies() != null) {
                    int cIdx = 1;
                    for (OutcomeCompetencyImportDto cDto : item.getCompetencies()) {
                        if (cDto.getStatement() == null || cDto.getStatement().isBlank()) continue;
                        String cCode = poCode + "." + cIdx;
                        String cId = "pocomp-" + UUID.randomUUID().toString().substring(0, 8);
                        poComps.add(PoCompetency.builder()
                                .id(cId)
                                .poId(poId)
                                .code(cCode)
                                .statement(cDto.getStatement().trim())
                                .build());
                        cIdx++;
                        totalCompetencies++;
                    }
                }

                ProgrammeOutcome po = ProgrammeOutcome.builder()
                        .id(poId)
                        .programmeBatchId(programmeBatchId)
                        .code(poCode)
                        .statement(statement)
                        .target(target)
                        .status(ApprovalStatus.DRAFT)
                        .competencies(poComps)
                        .build();

                posToSave.add(po);
                poIdx++;
            } else {
                String psoCode = "PSO" + psoIdx;
                String psoId = "pso-" + UUID.randomUUID().toString().substring(0, 8);

                List<PsoCompetency> psoComps = new ArrayList<>();
                if (item.getCompetencies() != null) {
                    int cIdx = 1;
                    for (OutcomeCompetencyImportDto cDto : item.getCompetencies()) {
                        if (cDto.getStatement() == null || cDto.getStatement().isBlank()) continue;
                        String cCode = psoCode + "." + cIdx;
                        String cId = "psocomp-" + UUID.randomUUID().toString().substring(0, 8);
                        psoComps.add(PsoCompetency.builder()
                                .id(cId)
                                .psoId(psoId)
                                .code(cCode)
                                .statement(cDto.getStatement().trim())
                                .build());
                        cIdx++;
                        totalCompetencies++;
                    }
                }

                ProgrammeSpecificOutcome pso = ProgrammeSpecificOutcome.builder()
                        .id(psoId)
                        .programmeBatchId(programmeBatchId)
                        .code(psoCode)
                        .statement(statement)
                        .target(target)
                        .status(ApprovalStatus.DRAFT)
                        .competencies(psoComps)
                        .build();

                psosToSave.add(pso);
                psoIdx++;
            }
        }

        // Save through existing authoritative service methods
        outcomeService.savePOs(programmeBatchId, posToSave);
        outcomeService.savePSOs(programmeBatchId, psosToSave);

        // Evict cache
        if (academicLookupCacheService != null) {
            academicLookupCacheService.evictProgrammeBatchCache();
        }

        // Audit log
        if (auditLogService != null) {
            auditLogService.recordSuccess(
                    AuditAction.CREATE,
                    ResourceType.PROGRAMME_OUTCOME,
                    programmeBatchId,
                    null,
                    "ACTIVE",
                    "Imported " + posToSave.size() + " POs and " + psosToSave.size() + " PSOs with " + totalCompetencies + " competencies via Excel bulk import",
                    Map.of(
                            "poCount", String.valueOf(posToSave.size()),
                            "psoCount", String.valueOf(psosToSave.size()),
                            "competencyCount", String.valueOf(totalCompetencies),
                            "programmeBatchId", programmeBatchId
                    )
            );
        }

        return OutcomeImportResultDto.builder()
                .success(true)
                .message("Successfully imported " + posToSave.size() + " POs and " + psosToSave.size() + " PSOs.")
                .programmeBatchId(programmeBatchId)
                .totalOutcomesImported(posToSave.size() + psosToSave.size())
                .totalPOsImported(posToSave.size())
                .totalPSOsImported(psosToSave.size())
                .totalCompetenciesImported(totalCompetencies)
                .poCodes(posToSave.stream().map(ProgrammeOutcome::getCode).collect(Collectors.toList()))
                .psoCodes(psosToSave.stream().map(ProgrammeSpecificOutcome::getCode).collect(Collectors.toList()))
                .build();
    }

    /**
     * Overloaded method to commit directly from MultipartFile without manual preview edits.
     */
    @Transactional
    public OutcomeImportResultDto commitImport(String programmeBatchId, MultipartFile file) {
        OutcomeImportPreviewDto preview = previewImport(programmeBatchId, file);
        if (!preview.isValid()) {
            String errorMsg = !preview.getErrors().isEmpty() ? preview.getErrors().get(0) : "Workbook contains invalid outcome data.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
        }

        OutcomeImportCommitRequestDto request = OutcomeImportCommitRequestDto.builder()
                .items(preview.getItems())
                .build();

        return commitImport(programmeBatchId, request);
    }

    /**
     * Dynamically assigns codes (PO1, PO1.1, PSO1, PSO1.1) and performs validation.
     */
    public void renumberAndValidate(List<OutcomeImportItemDto> items, List<String> warnings) {
        if (items == null) return;

        int poIndex = 1;
        int psoIndex = 1;

        for (OutcomeImportItemDto item : items) {
            item.getIssues().clear();
            boolean isPSO = "PSO".equalsIgnoreCase(item.getCategory());
            String outcomeCode = isPSO ? ("PSO" + psoIndex++) : ("PO" + poIndex++);
            item.setCode(outcomeCode);

            // Validate statement
            if (item.getStatement() == null || item.getStatement().trim().isBlank()) {
                item.getIssues().add("Outcome statement cannot be blank.");
                item.setStatus("INVALID");
            } else {
                item.setStatus("VALID");
            }

            // Renumber competencies
            if (item.getCompetencies() != null && !item.getCompetencies().isEmpty()) {
                int cIdx = 1;
                for (OutcomeCompetencyImportDto comp : item.getCompetencies()) {
                    comp.setCode(outcomeCode + "." + cIdx);
                    comp.setOrder(cIdx);
                    cIdx++;
                }
            } else {
                item.getIssues().add("No competencies defined for " + outcomeCode + ".");
                if (!"INVALID".equals(item.getStatus())) {
                    item.setStatus("WARNING");
                }
            }
        }
    }

    private OutcomeImportPreviewDto buildEmptyPreview(String programmeBatchId, ProgrammeBatch batch,
                                                      MasterProgramme masterProg, List<String> errors, List<String> warnings) {
        return OutcomeImportPreviewDto.builder()
                .valid(false)
                .programmeBatchId(programmeBatchId)
                .programmeBatchName(batch != null ? batch.getName() : programmeBatchId)
                .masterProgrammeName(masterProg != null ? masterProg.getName() : null)
                .totalOutcomes(0)
                .totalPOs(0)
                .totalPSOs(0)
                .totalCompetencies(0)
                .validCount(0)
                .warningCount(0)
                .errorCount(0)
                .items(Collections.emptyList())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private String getCellText(Row row, int colIndex, DataFormatter formatter) {
        if (row == null || colIndex < 0) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        String text = formatter.formatCellValue(cell);
        return text != null ? text.trim() : "";
    }

    private String cleanText(String s) {
        if (s == null) return "";
        return s.replaceAll("\\r\\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }
}
