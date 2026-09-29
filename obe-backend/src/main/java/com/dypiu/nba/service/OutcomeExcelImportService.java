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
import java.util.concurrent.atomic.AtomicInteger;
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
     * Supports scope: "ALL" (2 sheets: Sheet 1 = PO, Sheet 2 = PSO), "PO" (1 sheet: PO), "PSO" (1 sheet: PSO).
     */
    public byte[] generateTemplate(String programmeBatchId, String scope) {
        if (programmeBatchId == null || programmeBatchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programmeBatchId is required.");
        }

        academicService.enforceBatchScope(programmeBatchId);

        ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme Batch not found: " + programmeBatchId));

        String normalizedScope = (scope != null && !scope.isBlank()) ? scope.trim().toUpperCase() : "ALL";

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

            CellStyle wrapStyle = wb.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            if ("PO".equalsIgnoreCase(normalizedScope)) {
                // Single sheet for PO only
                createOutcomeSheet(wb, "PO and Competency", "Programme Outcomes *", "PO", headerStyle, wrapStyle);
            } else if ("PSO".equalsIgnoreCase(normalizedScope)) {
                // Single sheet for PSO only
                createOutcomeSheet(wb, "PSO and Competency", "Programme Specific Outcomes *", "PSO", headerStyle, wrapStyle);
            } else {
                // Combined: Sheet 1 for PO, Sheet 2 for PSO
                createOutcomeSheet(wb, "PO and Competency", "Programme Outcomes *", "PO", headerStyle, wrapStyle);
                createOutcomeSheet(wb, "PSO and Competency", "Programme Specific Outcomes *", "PSO", headerStyle, wrapStyle);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate outcome template for batch {}", programmeBatchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate template: " + e.getMessage());
        }
    }

    public byte[] generateTemplate(String programmeBatchId) {
        return generateTemplate(programmeBatchId, "ALL");
    }

    private void createOutcomeSheet(Workbook wb, String sheetName, String outcomeHeader, String category,
                                    CellStyle headerStyle, CellStyle wrapStyle) {
        Sheet sheet = wb.createSheet(sheetName);
        sheet.setDisplayGridlines(true);

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);

        Cell h1 = headerRow.createCell(0);
        h1.setCellValue(outcomeHeader);
        h1.setCellStyle(headerStyle);

        Cell h2 = headerRow.createCell(1);
        h2.setCellValue("Competency *");
        h2.setCellStyle(headerStyle);

        int r = 1;
        if ("PO".equalsIgnoreCase(category)) {
            // Sample PO 1 with 3 competencies
            Row r1 = sheet.createRow(r++);
            r1.createCell(0).setCellValue("1. Apply the knowledge of mathematics, science, engineering fundamentals and engineering specialization to the solution of complex engineering problems.");
            r1.getCell(0).setCellStyle(wrapStyle);
            r1.createCell(1).setCellValue("Demonstrate competence in knowledge of basic & applied mathematics.");
            r1.getCell(1).setCellStyle(wrapStyle);

            Row r2 = sheet.createRow(r++);
            r2.createCell(0).setCellValue("");
            r2.createCell(1).setCellValue("Demonstrate competence in basic sciences.");
            r2.getCell(1).setCellStyle(wrapStyle);

            Row r3 = sheet.createRow(r++);
            r3.createCell(0).setCellValue("");
            r3.createCell(1).setCellValue("Demonstrate competence in engineering fundamentals.");
            r3.getCell(1).setCellStyle(wrapStyle);

            // Sample PO 2 with 2 competencies
            Row r4 = sheet.createRow(r++);
            r4.createCell(0).setCellValue("2. Identify, formulate, review research literature, and analyze complex engineering problems.");
            r4.getCell(0).setCellStyle(wrapStyle);
            r4.createCell(1).setCellValue("Demonstrate an ability to identify and formulate complex engineering problems.");
            r4.getCell(1).setCellStyle(wrapStyle);

            Row r5 = sheet.createRow(r++);
            r5.createCell(0).setCellValue("");
            r5.createCell(1).setCellValue("Demonstrate an ability to formulate a solution plan and methodology.");
            r5.getCell(1).setCellStyle(wrapStyle);
        } else {
            // Sample PSO 1 with 2 competencies
            Row r1 = sheet.createRow(r++);
            r1.createCell(0).setCellValue("1. Ability to design and develop software solutions in specialized computing domains.");
            r1.getCell(0).setCellStyle(wrapStyle);
            r1.createCell(1).setCellValue("Apply modern software design paradigms to system development.");
            r1.getCell(1).setCellStyle(wrapStyle);

            Row r2 = sheet.createRow(r++);
            r2.createCell(0).setCellValue("");
            r2.createCell(1).setCellValue("Demonstrate testing and quality assurance practices.");
            r2.getCell(1).setCellStyle(wrapStyle);

            // Sample PSO 2 with 2 competencies
            Row r3 = sheet.createRow(r++);
            r3.createCell(0).setCellValue("2. Demonstrate skills to adapt to emerging software technologies, frameworks, and cloud paradigms.");
            r3.getCell(0).setCellStyle(wrapStyle);
            r3.createCell(1).setCellValue("Evaluate emerging cloud architectural patterns and technology stacks.");
            r3.getCell(1).setCellStyle(wrapStyle);

            Row r4 = sheet.createRow(r++);
            r4.createCell(0).setCellValue("");
            r4.createCell(1).setCellValue("Implement prototype solutions utilizing modern industry frameworks.");
            r4.getCell(1).setCellStyle(wrapStyle);
        }

        sheet.setColumnWidth(0, 16000);
        sheet.setColumnWidth(1, 16000);
    }

    /**
     * Parses the uploaded workbook and returns validation & preview without persisting.
     * Supports scope: "ALL" (Sheet 1 = PO, Sheet 2 = PSO), "PO" (Sheet = PO), "PSO" (Sheet = PSO).
     */
    public OutcomeImportPreviewDto previewImport(String programmeBatchId, MultipartFile file, String scope) {
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
        List<String> sheetNames = new ArrayList<>();

        DataFormatter formatter = new DataFormatter();
        String normalizedScope = (scope != null && !scope.isBlank()) ? scope.trim().toUpperCase() : "ALL";

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            if (workbook.getNumberOfSheets() == 0) {
                globalErrors.add("The uploaded workbook contains no sheets.");
                return buildEmptyPreview(programmeBatchId, batch, masterProg, globalErrors, globalWarnings, normalizedScope, Collections.emptyList());
            }

            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                sheetNames.add(workbook.getSheetAt(s).getSheetName());
            }

            AtomicInteger poCounter = new AtomicInteger(0);
            AtomicInteger psoCounter = new AtomicInteger(0);

            if ("PO".equalsIgnoreCase(normalizedScope)) {
                // Parse PO sheet only
                Sheet poSheet = findSheet(workbook, "po", true);
                if (poSheet == null) poSheet = workbook.getSheetAt(0);
                parseSheetRows(poSheet, "PO", rawItems, globalWarnings, formatter, poCounter, psoCounter);
            } else if ("PSO".equalsIgnoreCase(normalizedScope)) {
                // Parse PSO sheet only
                Sheet psoSheet = findSheet(workbook, "pso", false);
                if (psoSheet == null) psoSheet = workbook.getSheetAt(0);
                parseSheetRows(psoSheet, "PSO", rawItems, globalWarnings, formatter, poCounter, psoCounter);
            } else {
                // ALL mode:
                if (workbook.getNumberOfSheets() >= 2) {
                    // First sheet for PO, second sheet for PSO (per specification)
                    Sheet poSheet = findSheet(workbook, "po", true);
                    Sheet psoSheet = findSheet(workbook, "pso", false);
                    if (poSheet == null) poSheet = workbook.getSheetAt(0);
                    if (psoSheet == null) psoSheet = workbook.getSheetAt(1);

                    if (poSheet == psoSheet) {
                        poSheet = workbook.getSheetAt(0);
                        psoSheet = workbook.getSheetAt(1);
                    }

                    parseSheetRows(poSheet, "PO", rawItems, globalWarnings, formatter, poCounter, psoCounter);
                    parseSheetRows(psoSheet, "PSO", rawItems, globalWarnings, formatter, poCounter, psoCounter);
                } else {
                    // Single continuous sheet with legacy detection
                    parseSheetRows(workbook.getSheetAt(0), null, rawItems, globalWarnings, formatter, poCounter, psoCounter);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse outcome Excel file for batch {}", programmeBatchId, e);
            globalErrors.add("Failed to read Excel file: " + e.getMessage());
            return buildEmptyPreview(programmeBatchId, batch, masterProg, globalErrors, globalWarnings, normalizedScope, sheetNames);
        }

        if (rawItems.isEmpty()) {
            globalErrors.add("No outcomes or competencies were found in the uploaded workbook.");
            return buildEmptyPreview(programmeBatchId, batch, masterProg, globalErrors, globalWarnings, normalizedScope, sheetNames);
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
                .scope(normalizedScope)
                .sheetNames(sheetNames)
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

    public OutcomeImportPreviewDto previewImport(String programmeBatchId, MultipartFile file) {
        return previewImport(programmeBatchId, file, "ALL");
    }

    private Sheet findSheet(Workbook wb, String keyword, boolean excludePsoIfPo) {
        for (int s = 0; s < wb.getNumberOfSheets(); s++) {
            Sheet sh = wb.getSheetAt(s);
            String name = sh.getSheetName().toLowerCase();
            if (excludePsoIfPo && name.contains("pso")) {
                continue;
            }
            if (name.contains(keyword.toLowerCase())) {
                return sh;
            }
        }
        return null;
    }

    private void parseSheetRows(
            Sheet sheet,
            String forcedCategory,
            List<OutcomeImportItemDto> rawItems,
            List<String> globalWarnings,
            DataFormatter formatter,
            AtomicInteger poCounter,
            AtomicInteger psoCounter) {

        int outcomeCol = 0;
        int competencyCol = 1;
        int startRow = 0;

        int lastRowNum = sheet.getLastRowNum();
        for (int r = 0; r <= Math.min(10, lastRowNum); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String c0 = getCellText(row, 0, formatter).toLowerCase();
            String c1 = getCellText(row, 1, formatter).toLowerCase();

            if (c0.contains("outcome") || c0.contains("po") || c0.contains("pso") || c0.contains("statement")) {
                outcomeCol = 0;
                if (c1.contains("competenc") || c1.contains("indicator") || c1.contains("sub")) {
                    competencyCol = 1;
                }
                startRow = r + 1;
                break;
            } else if (c1.contains("outcome") || c1.contains("po") || c1.contains("pso")) {
                outcomeCol = 1;
                competencyCol = 0;
                startRow = r + 1;
                break;
            }
        }

        String currentSection = forcedCategory != null ? forcedCategory : "PO";
        OutcomeImportItemDto currentOutcome = null;

        for (int r = startRow; r <= lastRowNum; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String rawOutcomeText = getCellText(row, outcomeCol, formatter);
            String rawCompetencyText = getCellText(row, competencyCol, formatter);

            if (rawOutcomeText.isBlank() && rawCompetencyText.isBlank()) {
                continue;
            }

            // Check for Section Header Row (only when forcedCategory is null)
            if (forcedCategory == null && !rawOutcomeText.isBlank()) {
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

                if (forcedCategory != null) {
                    category = forcedCategory;
                    detectionRule = "DEDICATED_SHEET_" + forcedCategory;
                    Matcher psoPrefixMatcher = EXPLICIT_PSO_PATTERN.matcher(rawOutcomeText);
                    Matcher poPrefixMatcher = EXPLICIT_PO_PATTERN.matcher(rawOutcomeText);
                    Matcher numMatcher = NUMBERED_PREFIX_PATTERN.matcher(rawOutcomeText);
                    if (psoPrefixMatcher.matches()) {
                        String rest = psoPrefixMatcher.group(2).trim();
                        cleanStatement = rest.isEmpty() ? rawOutcomeText : rest;
                    } else if (poPrefixMatcher.matches()) {
                        String rest = poPrefixMatcher.group(2).trim();
                        cleanStatement = rest.isEmpty() ? rawOutcomeText : rest;
                    } else if (numMatcher.matches()) {
                        String rest = numMatcher.group(2).trim();
                        cleanStatement = rest.isEmpty() ? rawOutcomeText : rest;
                    }
                } else {
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
                            if (num == 1 && poCounter.get() > 0) {
                                category = "PSO";
                                detectionRule = "NUMBERING_RESET";
                                currentSection = "PSO";
                            } else if (num > 12) {
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
                }

                cleanStatement = cleanText(cleanStatement);

                if ("PO".equalsIgnoreCase(category)) {
                    poCounter.incrementAndGet();
                } else {
                    psoCounter.incrementAndGet();
                }

                OutcomeImportItemDto newItem = OutcomeImportItemDto.builder()
                        .id("item-" + (rawItems.size() + 1))
                        .rowNumber(r + 1)
                        .sheetName(sheet.getSheetName())
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
                // Continuation row: competency statement
                if (!rawCompetencyText.isBlank()) {
                    if (currentOutcome != null) {
                        int order = currentOutcome.getCompetencies().size() + 1;
                        currentOutcome.getCompetencies().add(OutcomeCompetencyImportDto.builder()
                                .id("comp-" + currentOutcome.getId() + "-" + order)
                                .statement(cleanText(rawCompetencyText))
                                .order(order)
                                .build());
                    } else {
                        globalWarnings.add(sheet.getSheetName() + " Row " + (r + 1) + ": Competency statement found before any Outcome was defined: '" + rawCompetencyText + "'.");
                    }
                }
            }
        }
    }

    /**
     * Commits the confirmed outcomes and competencies to the database within a single transaction.
     * Supports scope: "ALL", "PO", "PSO".
     */
    @Transactional
    public OutcomeImportResultDto commitImport(String programmeBatchId, OutcomeImportCommitRequestDto request, String scope) {
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

        String effectiveScope = (scope != null && !scope.isBlank())
                ? scope.trim().toUpperCase()
                : (request.getScope() != null && !request.getScope().isBlank() ? request.getScope().trim().toUpperCase() : "ALL");

        // Save through existing authoritative service methods with scope isolation
        if ("PO".equalsIgnoreCase(effectiveScope)) {
            outcomeService.savePOs(programmeBatchId, posToSave);
            // PSOs are completely untouched!
        } else if ("PSO".equalsIgnoreCase(effectiveScope)) {
            outcomeService.savePSOs(programmeBatchId, psosToSave);
            // POs are completely untouched!
        } else {
            // ALL / COMBINED:
            if (!posToSave.isEmpty()) {
                outcomeService.savePOs(programmeBatchId, posToSave);
            }
            if (!psosToSave.isEmpty()) {
                outcomeService.savePSOs(programmeBatchId, psosToSave);
            }
        }

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
                    "Imported " + posToSave.size() + " POs and " + psosToSave.size() + " PSOs with " + totalCompetencies + " competencies via Excel bulk import (" + effectiveScope + ")",
                    Map.of(
                            "poCount", String.valueOf(posToSave.size()),
                            "psoCount", String.valueOf(psosToSave.size()),
                            "competencyCount", String.valueOf(totalCompetencies),
                            "programmeBatchId", programmeBatchId,
                            "scope", effectiveScope
                    )
            );
        }

        String successMessage;
        if ("PO".equalsIgnoreCase(effectiveScope)) {
            successMessage = "Successfully imported " + posToSave.size() + " POs with " + totalCompetencies + " competencies. Existing PSOs were preserved.";
        } else if ("PSO".equalsIgnoreCase(effectiveScope)) {
            successMessage = "Successfully imported " + psosToSave.size() + " PSOs with " + totalCompetencies + " competencies. Existing POs were preserved.";
        } else {
            successMessage = "Successfully imported " + posToSave.size() + " POs and " + psosToSave.size() + " PSOs with " + totalCompetencies + " competencies.";
        }

        return OutcomeImportResultDto.builder()
                .success(true)
                .message(successMessage)
                .programmeBatchId(programmeBatchId)
                .totalOutcomesImported(posToSave.size() + psosToSave.size())
                .totalPOsImported(posToSave.size())
                .totalPSOsImported(psosToSave.size())
                .totalCompetenciesImported(totalCompetencies)
                .poCodes(posToSave.stream().map(ProgrammeOutcome::getCode).collect(Collectors.toList()))
                .psoCodes(psosToSave.stream().map(ProgrammeSpecificOutcome::getCode).collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public OutcomeImportResultDto commitImport(String programmeBatchId, OutcomeImportCommitRequestDto request) {
        return commitImport(programmeBatchId, request, request != null ? request.getScope() : "ALL");
    }

    /**
     * Overloaded method to commit directly from MultipartFile without manual preview edits.
     */
    @Transactional
    public OutcomeImportResultDto commitImport(String programmeBatchId, MultipartFile file, String scope) {
        OutcomeImportPreviewDto preview = previewImport(programmeBatchId, file, scope);
        if (!preview.isValid()) {
            String errorMsg = !preview.getErrors().isEmpty() ? preview.getErrors().get(0) : "Workbook contains invalid outcome data.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
        }

        OutcomeImportCommitRequestDto request = OutcomeImportCommitRequestDto.builder()
                .scope(scope)
                .items(preview.getItems())
                .build();

        return commitImport(programmeBatchId, request, scope);
    }

    @Transactional
    public OutcomeImportResultDto commitImport(String programmeBatchId, MultipartFile file) {
        return commitImport(programmeBatchId, file, "ALL");
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
                                                      MasterProgramme masterProg, List<String> errors, List<String> warnings,
                                                      String scope, List<String> sheetNames) {
        return OutcomeImportPreviewDto.builder()
                .valid(false)
                .scope(scope)
                .sheetNames(sheetNames != null ? sheetNames : Collections.emptyList())
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
