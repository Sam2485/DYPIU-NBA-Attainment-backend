package com.dypiu.nba.service;

import com.dypiu.nba.audit.AuditAction;
import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseMappingExcelImportService {

    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final PoCompetencyRepository poCompetencyRepository;
    private final PsoCompetencyRepository psoCompetencyRepository;
    private final CourseMappingKeywordRepository courseMappingKeywordRepository;
    private final CoPoMappingRepository coPoMappingRepository;
    private final CoPsoMappingRepository coPsoMappingRepository;
    private final OutcomeService outcomeService;
    private final AcademicService academicService;
    private final BatchLifecycleService batchLifecycleService;
    private final ApprovalService approvalService;
    private final CurrentUserScopeService currentUserScopeService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    private static final Pattern CO_PATTERN = Pattern.compile("(?i)^CO\\s*(\\d+)$");
    private static final Pattern PO_CODE_PATTERN = Pattern.compile("(?i)^(PO\\s*\\d+)");
    private static final Pattern PSO_CODE_PATTERN = Pattern.compile("(?i)^(PSO\\s*\\d+)");
    private static final Pattern NUMBERED_PREFIX_PATTERN = Pattern.compile("^\\s*(\\d+)[.):\\-\\s]+");

    private void enforceOfferingAccess(ProgrammeBatchCourse offering, boolean isMutation) {
        if (offering == null) return;
        if (offering.getProgrammeBatchId() != null) {
            academicService.enforceBatchScope(offering.getProgrammeBatchId());
            if (isMutation && batchLifecycleService != null) {
                batchLifecycleService.enforceBatchEditability(offering.getProgrammeBatchId());
            }
        }
        if (currentUserScopeService != null) {
            CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
            if (scope != null && !scope.isIqac() && !scope.isDirector() && !scope.isHod() && !scope.isProgrammeCoordinator()) {
                if (scope.isFaculty()) {
                    boolean isCoordinator = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId()));
                    boolean isAssigned = isCoordinator || (offering.getAssignedFaculty() != null && (offering.getAssignedFaculty().contains(scope.getEmail()) || offering.getAssignedFaculty().contains(scope.getName())));
                    if (!isAssigned) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this course offering.");
                    }
                    if (isMutation && !isCoordinator) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only the assigned Course Coordinator can modify mappings.");
                    }
                }
            }
        }
    }

    /**
     * Generates a downloadable Excel template for CO-PO/PSO Mapping.
     * Supports scope: "ALL", "PO", "PSO".
     * If includeSampleData is true, fills in sample keywords and mapping strengths.
     */
    public byte[] generateTemplate(String programmeBatchCourseId, String scope, boolean includeSampleData) {
        if (programmeBatchCourseId == null || programmeBatchCourseId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programmeBatchCourseId is required.");
        }

        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(programmeBatchCourseId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + programmeBatchCourseId));

        enforceOfferingAccess(offering, false);

        String programmeBatchId = offering.getProgrammeBatchId();
        List<CourseOutcome> cos = courseOutcomeRepository.findByProgrammeBatchCourseId(offering.getId()).stream()
                .sorted(Comparator.comparing(CourseOutcome::getCode, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        if (cos.isEmpty()) {
            // Fallback sample COs if course has not yet defined COs
            for (int i = 1; i <= 5; i++) {
                cos.add(CourseOutcome.builder()
                        .id("co-sample-" + i)
                        .code("CO" + i)
                        .statement("Sample Course Outcome statement " + i + " for " + (offering.getCourseCode() != null ? offering.getCourseCode() : "course"))
                        .build());
            }
        }

        List<ProgrammeOutcome> pos = (programmeBatchId != null)
                ? programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(programmeBatchId)
                : Collections.emptyList();

        List<ProgrammeSpecificOutcome> psos = (programmeBatchId != null)
                ? programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(programmeBatchId)
                : Collections.emptyList();

        String normalizedScope = (scope != null && !scope.isBlank()) ? scope.trim().toUpperCase() : "ALL";

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Font boldFont = wb.createFont();
            boldFont.setBold(true);
            boldFont.setColor(IndexedColors.WHITE.getIndex());
            boldFont.setFontHeightInPoints((short) 10);

            CellStyle darkHeader = wb.createCellStyle();
            darkHeader.setFont(boldFont);
            darkHeader.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            darkHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            darkHeader.setAlignment(HorizontalAlignment.CENTER);
            darkHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            darkHeader.setBorderBottom(BorderStyle.THIN);
            darkHeader.setBorderTop(BorderStyle.THIN);
            darkHeader.setBorderLeft(BorderStyle.THIN);
            darkHeader.setBorderRight(BorderStyle.THIN);

            CellStyle blueSubHeader = wb.createCellStyle();
            Font subFont = wb.createFont();
            subFont.setBold(true);
            subFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            blueSubHeader.setFont(subFont);
            blueSubHeader.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            blueSubHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            blueSubHeader.setAlignment(HorizontalAlignment.CENTER);
            blueSubHeader.setVerticalAlignment(VerticalAlignment.CENTER);
            blueSubHeader.setBorderBottom(BorderStyle.THIN);

            CellStyle wrapStyle = wb.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);
            wrapStyle.setBorderBottom(BorderStyle.THIN);
            wrapStyle.setBorderTop(BorderStyle.THIN);
            wrapStyle.setBorderLeft(BorderStyle.THIN);
            wrapStyle.setBorderRight(BorderStyle.THIN);

            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            centerStyle.setBorderBottom(BorderStyle.THIN);
            centerStyle.setBorderTop(BorderStyle.THIN);
            centerStyle.setBorderLeft(BorderStyle.THIN);
            centerStyle.setBorderRight(BorderStyle.THIN);

            if ("PO".equalsIgnoreCase(normalizedScope)) {
                createMappingSheet(wb, "Mapping of CO to PO", "PO", cos, pos, Collections.emptyList(), darkHeader, blueSubHeader, wrapStyle, centerStyle, includeSampleData);
            } else if ("PSO".equalsIgnoreCase(normalizedScope)) {
                createMappingSheet(wb, "Mapping of CO to PSO", "PSO", cos, Collections.emptyList(), psos, darkHeader, blueSubHeader, wrapStyle, centerStyle, includeSampleData);
            } else {
                createMappingSheet(wb, "Mapping of CO to PO", "PO", cos, pos, Collections.emptyList(), darkHeader, blueSubHeader, wrapStyle, centerStyle, includeSampleData);
                createMappingSheet(wb, "Mapping of CO to PSO", "PSO", cos, Collections.emptyList(), psos, darkHeader, blueSubHeader, wrapStyle, centerStyle, includeSampleData);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate course mapping template for offering {}", programmeBatchCourseId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate mapping template: " + e.getMessage());
        }
    }

    private void createMappingSheet(Workbook wb, String sheetName, String outcomeType,
                                    List<CourseOutcome> cos,
                                    List<ProgrammeOutcome> pos,
                                    List<ProgrammeSpecificOutcome> psos,
                                    CellStyle darkHeader, CellStyle blueSubHeader,
                                    CellStyle wrapStyle, CellStyle centerStyle,
                                    boolean includeSampleData) {
        Sheet sheet = wb.createSheet(sheetName);
        sheet.setDisplayGridlines(true);

        int numCos = cos.size();
        int keywordStartCol = 3; // Column D
        int indicatorStartCol = keywordStartCol + numCos + 1; // Column J (if 5 COs, 3+5+1 = 9 = J)

        // Row 0: Section titles
        Row r0 = sheet.createRow(0);
        r0.setHeightInPoints(24);
        Cell c0_0 = r0.createCell(0);
        c0_0.setCellValue("Justification 1");
        c0_0.setCellStyle(darkHeader);

        Cell c0_kw = r0.createCell(keywordStartCol);
        c0_kw.setCellValue("Keywords mapping to Competency from resepctive CO");
        c0_kw.setCellStyle(darkHeader);

        Cell c0_yn = r0.createCell(indicatorStartCol);
        c0_yn.setCellValue("Y or N");
        c0_yn.setCellStyle(darkHeader);

        // Row 1: Headers
        Row r1 = sheet.createRow(1);
        r1.setHeightInPoints(22);
        Cell c1_a = r1.createCell(0);
        c1_a.setCellValue(outcomeType);
        c1_a.setCellStyle(darkHeader);

        Cell c1_b = r1.createCell(1);
        c1_b.setCellValue("Competency");
        c1_b.setCellStyle(darkHeader);

        for (int i = 0; i < numCos; i++) {
            Cell c = r1.createCell(keywordStartCol + i);
            c.setCellValue(cos.get(i).getCode());
            c.setCellStyle(blueSubHeader);
        }

        for (int i = 0; i < numCos; i++) {
            Cell c = r1.createCell(indicatorStartCol + i);
            c.setCellValue(cos.get(i).getCode());
            c.setCellStyle(blueSubHeader);
        }

        int currentRow = 2;

        if ("PO".equalsIgnoreCase(outcomeType)) {
            for (ProgrammeOutcome po : pos) {
                List<PoCompetency> comps = poCompetencyRepository.findByPoId(po.getId());
                if (comps.isEmpty()) {
                    comps = List.of(PoCompetency.builder().code(po.getCode() + ".1").statement(po.getStatement() != null ? po.getStatement() : "Competency 1").build());
                }

                int outcomeStartRow = currentRow;
                for (int cIdx = 0; cIdx < comps.size(); cIdx++) {
                    PoCompetency comp = comps.get(cIdx);
                    Row row = sheet.createRow(currentRow++);
                    row.setHeightInPoints(22);

                    if (cIdx == 0) {
                        Cell poCell = row.createCell(0);
                        poCell.setCellValue(po.getCode() + ". " + (po.getStatement() != null ? po.getStatement() : ""));
                        poCell.setCellStyle(wrapStyle);
                    }

                    Cell compCell = row.createCell(1);
                    compCell.setCellValue(comp.getStatement() != null ? comp.getStatement() : comp.getCode());
                    compCell.setCellStyle(wrapStyle);

                    if (includeSampleData) {
                        for (int i = 0; i < numCos; i++) {
                            // Sample keywords for odd competencies
                            if ((cIdx + i) % 2 == 0) {
                                Cell kwCell = row.createCell(keywordStartCol + i);
                                kwCell.setCellValue("Keyword for " + cos.get(i).getCode() + " on " + comp.getCode());
                                kwCell.setCellStyle(wrapStyle);

                                Cell ynCell = row.createCell(indicatorStartCol + i);
                                ynCell.setCellValue("Y");
                                ynCell.setCellStyle(centerStyle);
                            }
                        }
                    }
                }

                // Summary Row: No of competencies mapped
                Row numRow = sheet.createRow(currentRow++);
                Cell numLabel = numRow.createCell(keywordStartCol);
                numLabel.setCellValue("No of competencies from given " + po.getCode() + " mapped by COs");
                numLabel.setCellStyle(wrapStyle);

                // Summary Row: % of competencies mapped
                Row pctRow = sheet.createRow(currentRow++);
                Cell pctLabel = pctRow.createCell(keywordStartCol);
                pctLabel.setCellValue("% of competencies from given " + po.getCode() + " mapped by COs");
                pctLabel.setCellStyle(wrapStyle);

                // Summary Row: Mapping strength
                Row strRow = sheet.createRow(currentRow++);
                Cell strLabel = strRow.createCell(keywordStartCol);
                strLabel.setCellValue("Mapping strength of " + po.getCode() + " of CO");
                strLabel.setCellStyle(wrapStyle);

                for (int i = 0; i < numCos; i++) {
                    if (includeSampleData) {
                        int mapped = (comps.size() > 1) ? (comps.size() / 2 + (i % 2)) : 1;
                        if (mapped > comps.size()) mapped = comps.size();
                        int pct = (mapped * 100) / comps.size();
                        String strength = (pct >= 75) ? "3" : (pct >= 50 ? "2" : (pct > 0 ? "1" : "-"));

                        Cell numCell = numRow.createCell(indicatorStartCol + i);
                        numCell.setCellValue(String.valueOf(mapped));
                        numCell.setCellStyle(centerStyle);

                        Cell pctCell = pctRow.createCell(indicatorStartCol + i);
                        pctCell.setCellValue(String.valueOf(pct));
                        pctCell.setCellStyle(centerStyle);

                        Cell strCell = strRow.createCell(indicatorStartCol + i);
                        strCell.setCellValue(strength);
                        strCell.setCellStyle(centerStyle);
                    } else {
                        Cell strCell = strRow.createCell(indicatorStartCol + i);
                        strCell.setCellValue("-");
                        strCell.setCellStyle(centerStyle);
                    }
                }
            }
        } else {
            // PSO
            for (ProgrammeSpecificOutcome pso : psos) {
                List<PsoCompetency> comps = psoCompetencyRepository.findByPsoId(pso.getId());
                if (comps.isEmpty()) {
                    comps = List.of(PsoCompetency.builder().code(pso.getCode() + ".1").statement(pso.getStatement() != null ? pso.getStatement() : "Competency 1").build());
                }

                for (int cIdx = 0; cIdx < comps.size(); cIdx++) {
                    PsoCompetency comp = comps.get(cIdx);
                    Row row = sheet.createRow(currentRow++);
                    row.setHeightInPoints(22);

                    if (cIdx == 0) {
                        Cell psoCell = row.createCell(0);
                        psoCell.setCellValue(pso.getCode() + ". " + (pso.getStatement() != null ? pso.getStatement() : ""));
                        psoCell.setCellStyle(wrapStyle);
                    }

                    Cell compCell = row.createCell(1);
                    compCell.setCellValue(comp.getStatement() != null ? comp.getStatement() : comp.getCode());
                    compCell.setCellStyle(wrapStyle);

                    if (includeSampleData) {
                        for (int i = 0; i < numCos; i++) {
                            if ((cIdx + i) % 2 == 0) {
                                Cell kwCell = row.createCell(keywordStartCol + i);
                                kwCell.setCellValue("Keyword for " + cos.get(i).getCode() + " on " + comp.getCode());
                                kwCell.setCellStyle(wrapStyle);

                                Cell ynCell = row.createCell(indicatorStartCol + i);
                                ynCell.setCellValue("Y");
                                ynCell.setCellStyle(centerStyle);
                            }
                        }
                    }
                }

                Row numRow = sheet.createRow(currentRow++);
                numRow.createCell(keywordStartCol).setCellValue("No of competencies from given " + pso.getCode() + " mapped by COs");
                numRow.getCell(keywordStartCol).setCellStyle(wrapStyle);

                Row pctRow = sheet.createRow(currentRow++);
                pctRow.createCell(keywordStartCol).setCellValue("% of competencies from given " + pso.getCode() + " mapped by COs");
                pctRow.getCell(keywordStartCol).setCellStyle(wrapStyle);

                Row strRow = sheet.createRow(currentRow++);
                strRow.createCell(keywordStartCol).setCellValue("Mapping strength of " + pso.getCode() + " of CO");
                strRow.getCell(keywordStartCol).setCellStyle(wrapStyle);

                for (int i = 0; i < numCos; i++) {
                    if (includeSampleData) {
                        int mapped = (comps.size() > 1) ? (comps.size() / 2 + (i % 2)) : 1;
                        if (mapped > comps.size()) mapped = comps.size();
                        int pct = (mapped * 100) / comps.size();
                        String strength = (pct >= 75) ? "3" : (pct >= 50 ? "2" : (pct > 0 ? "1" : "-"));

                        Cell numCell = numRow.createCell(indicatorStartCol + i);
                        numCell.setCellValue(String.valueOf(mapped));
                        numCell.setCellStyle(centerStyle);

                        Cell pctCell = pctRow.createCell(indicatorStartCol + i);
                        pctCell.setCellValue(String.valueOf(pct));
                        pctCell.setCellStyle(centerStyle);

                        Cell strCell = strRow.createCell(indicatorStartCol + i);
                        strCell.setCellValue(strength);
                        strCell.setCellStyle(centerStyle);
                    } else {
                        Cell strCell = strRow.createCell(indicatorStartCol + i);
                        strCell.setCellValue("-");
                        strCell.setCellStyle(centerStyle);
                    }
                }
            }
        }

        sheet.setColumnWidth(0, 35 * 256);
        sheet.setColumnWidth(1, 35 * 256);
        sheet.setColumnWidth(2, 6 * 256);
        for (int i = 0; i < numCos; i++) {
            sheet.setColumnWidth(keywordStartCol + i, 28 * 256);
            sheet.setColumnWidth(indicatorStartCol + i, 8 * 256);
        }
    }

    /**
     * Inspects and validates the uploaded Excel mapping file without committing changes.
     */
    @Transactional(readOnly = true)
    public CourseMappingImportPreviewDto previewImport(String programmeBatchCourseId, MultipartFile file, String requestedScope) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel file is required.");
        }

        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(programmeBatchCourseId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + programmeBatchCourseId));

        enforceOfferingAccess(offering, false);

        String programmeBatchId = offering.getProgrammeBatchId();
        ProgrammeBatch batch = (programmeBatchId != null) ? programmeBatchRepository.findById(programmeBatchId).orElse(null) : null;

        List<CourseOutcome> expectedCos = courseOutcomeRepository.findByProgrammeBatchCourseId(offering.getId()).stream()
                .sorted(Comparator.comparing(CourseOutcome::getCode, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        List<ProgrammeOutcome> expectedPos = (programmeBatchId != null)
                ? programmeOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(programmeBatchId)
                : Collections.emptyList();

        List<ProgrammeSpecificOutcome> expectedPsos = (programmeBatchId != null)
                ? programmeSpecificOutcomeRepository.findByProgrammeBatchIdOrderByCodeAsc(programmeBatchId)
                : Collections.emptyList();

        int totalExpectedComps = 0;
        for (ProgrammeOutcome po : expectedPos) {
            totalExpectedComps += poCompetencyRepository.findByPoId(po.getId()).size();
        }
        for (ProgrammeSpecificOutcome pso : expectedPsos) {
            totalExpectedComps += psoCompetencyRepository.findByPsoId(pso.getId()).size();
        }

        String scope = (requestedScope != null && !requestedScope.isBlank()) ? requestedScope.trim().toUpperCase() : "ALL";

        CourseMappingImportPreviewDto preview = CourseMappingImportPreviewDto.builder()
                .programmeBatchCourseId(offering.getId())
                .courseCode(offering.getCourseCode())
                .courseName(offering.getCourseName())
                .programmeBatchId(programmeBatchId)
                .programmeBatchName(batch != null ? batch.getName() : "")
                .scope(scope)
                .expectedCoCount(expectedCos.size())
                .expectedCoCodes(expectedCos.stream().map(CourseOutcome::getCode).collect(Collectors.toList()))
                .expectedPoCount(expectedPos.size())
                .expectedPsoCount(expectedPsos.size())
                .expectedCompetencyCount(totalExpectedComps)
                .build();

        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            List<String> sheetNames = new ArrayList<>();
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                sheetNames.add(wb.getSheetName(i));
            }
            preview.setSheetNames(sheetNames);

            boolean parsePo = "ALL".equalsIgnoreCase(scope) || "PO".equalsIgnoreCase(scope);
            boolean parsePso = "ALL".equalsIgnoreCase(scope) || "PSO".equalsIgnoreCase(scope);

            Sheet poSheet = null;
            Sheet psoSheet = null;

            if (parsePo) {
                poSheet = findSheetByKeywords(wb, List.of("co to po", "po mapping", "po"));
                if (poSheet == null && wb.getNumberOfSheets() > 0) {
                    poSheet = wb.getSheetAt(0);
                }
            }

            if (parsePso) {
                psoSheet = findSheetByKeywords(wb, List.of("co to pso", "pso mapping", "pso"));
                if (psoSheet == null && wb.getNumberOfSheets() > 1 && parsePo) {
                    psoSheet = wb.getSheetAt(1);
                } else if (psoSheet == null && !parsePo && wb.getNumberOfSheets() > 0) {
                    psoSheet = wb.getSheetAt(0);
                }
            }

            List<String> detectedCoCodes = new ArrayList<>();
            Map<String, Object> poKwStore = new LinkedHashMap<>();
            Map<String, Object> psoKwStore = new LinkedHashMap<>();
            Map<String, Map<String, Object>> combinedMatrix = new LinkedHashMap<>();
            List<CoPoMapping> poMappings = new ArrayList<>();
            List<CoPsoMapping> psoMappings = new ArrayList<>();

            int totalParsedCompetencies = 0;
            int totalKeywords = 0;
            int totalMappings = 0;

            // 1. Parse PO Sheet
            if (parsePo && poSheet != null) {
                ParsedSheetResult poResult = parseSheet(poSheet, false, expectedCos, expectedPos, Collections.emptyList());
                detectedCoCodes = poResult.detectedCoCodes;
                poKwStore = poResult.keywordsStore;
                totalParsedCompetencies += poResult.competenciesCount;
                totalKeywords += poResult.keywordsCount;
                preview.setSheetPoCount(poResult.outcomesCount);

                for (Map.Entry<String, Map<String, Integer>> coEntry : poResult.matrixLevels.entrySet()) {
                    String coCode = coEntry.getKey();
                    combinedMatrix.computeIfAbsent(coCode, k -> new LinkedHashMap<>());
                    for (Map.Entry<String, Integer> outcomeEntry : coEntry.getValue().entrySet()) {
                        String poCode = outcomeEntry.getKey();
                        Integer level = outcomeEntry.getValue();
                        combinedMatrix.get(coCode).put(poCode, level != null && level > 0 ? level : "-");

                        if (level != null && level > 0) {
                            String coId = findCoIdByCode(expectedCos, coCode);
                            if (coId != null) {
                                poMappings.add(CoPoMapping.builder()
                                        .courseOutcomeId(coId)
                                        .poCode(poCode)
                                        .mappingLevel(level)
                                        .build());
                                totalMappings++;
                            }
                        }
                    }
                }

                preview.getErrors().addAll(poResult.errors);
                preview.getWarnings().addAll(poResult.warnings);
            }

            // 2. Parse PSO Sheet
            if (parsePso && psoSheet != null) {
                ParsedSheetResult psoResult = parseSheet(psoSheet, true, expectedCos, Collections.emptyList(), expectedPsos);
                if (detectedCoCodes.isEmpty()) {
                    detectedCoCodes = psoResult.detectedCoCodes;
                }
                psoKwStore = psoResult.keywordsStore;
                totalParsedCompetencies += psoResult.competenciesCount;
                totalKeywords += psoResult.keywordsCount;
                preview.setSheetPsoCount(psoResult.outcomesCount);

                for (Map.Entry<String, Map<String, Integer>> coEntry : psoResult.matrixLevels.entrySet()) {
                    String coCode = coEntry.getKey();
                    combinedMatrix.computeIfAbsent(coCode, k -> new LinkedHashMap<>());
                    for (Map.Entry<String, Integer> outcomeEntry : coEntry.getValue().entrySet()) {
                        String psoCode = outcomeEntry.getKey();
                        Integer level = outcomeEntry.getValue();
                        combinedMatrix.get(coCode).put(psoCode, level != null && level > 0 ? level : "-");

                        if (level != null && level > 0) {
                            String coId = findCoIdByCode(expectedCos, coCode);
                            if (coId != null) {
                                psoMappings.add(CoPsoMapping.builder()
                                        .courseOutcomeId(coId)
                                        .psoCode(psoCode)
                                        .mappingLevel(level)
                                        .build());
                                totalMappings++;
                            }
                        }
                    }
                }

                preview.getErrors().addAll(psoResult.errors);
                preview.getWarnings().addAll(psoResult.warnings);
            }

            preview.setDetectedCoCodes(detectedCoCodes);
            preview.setSheetCoCount(detectedCoCodes.size());
            preview.setSheetCompetencyCount(totalParsedCompetencies);
            preview.setTotalKeywordsExtracted(totalKeywords);
            preview.setTotalMappingsFound(totalMappings);

            // Validation Verifications
            boolean cosMatch = !expectedCos.isEmpty() && expectedCos.size() == detectedCoCodes.size();
            if (cosMatch) {
                for (int i = 0; i < expectedCos.size(); i++) {
                    if (!expectedCos.get(i).getCode().equalsIgnoreCase(detectedCoCodes.get(i))) {
                        cosMatch = false;
                        break;
                    }
                }
            }
            preview.setCosMatch(cosMatch);
            if (!cosMatch && !expectedCos.isEmpty()) {
                preview.getErrors().add("Course Outcomes mismatch: Course expects " + expectedCos.size() + " COs ("
                        + expectedCos.stream().map(CourseOutcome::getCode).collect(Collectors.joining(", ")) + ") but sheet defines "
                        + detectedCoCodes.size() + " (" + String.join(", ", detectedCoCodes) + ").");
            }

            boolean posMatch = !parsePo || preview.getSheetPoCount() == expectedPos.size() || expectedPos.isEmpty();
            preview.setPosMatch(posMatch);
            if (!posMatch && !expectedPos.isEmpty()) {
                preview.getWarnings().add("Programme Outcomes count mismatch: Batch defines " + expectedPos.size() + " POs, but sheet has " + preview.getSheetPoCount() + ".");
            }

            boolean psosMatch = !parsePso || preview.getSheetPsoCount() == expectedPsos.size() || expectedPsos.isEmpty();
            preview.setPsosMatch(psosMatch);
            if (!psosMatch && !expectedPsos.isEmpty()) {
                preview.getWarnings().add("Programme Specific Outcomes count mismatch: Batch defines " + expectedPsos.size() + " PSOs, but sheet has " + preview.getSheetPsoCount() + ".");
            }

            preview.setCompetenciesMatch(totalExpectedComps == 0 || totalParsedCompetencies > 0);

            preview.setPoKeywordsStore(poKwStore);
            preview.setPsoKeywordsStore(psoKwStore);
            preview.setMatrix(combinedMatrix);
            preview.setPoMappings(poMappings);
            preview.setPsoMappings(psoMappings);

            preview.setValid(preview.getErrors().isEmpty());
            return preview;

        } catch (Exception e) {
            log.error("Failed to preview mapping Excel for offering {}", programmeBatchCourseId, e);
            preview.setValid(false);
            preview.getErrors().add("Unable to read Excel workbook: " + e.getMessage());
            return preview;
        }
    }

    private String findCoIdByCode(List<CourseOutcome> cos, String code) {
        if (code == null) return null;
        for (CourseOutcome co : cos) {
            if (co.getCode() != null && co.getCode().equalsIgnoreCase(code.trim())) {
                return co.getId();
            }
        }
        return null;
    }

    private Sheet findSheetByKeywords(Workbook wb, List<String> keywords) {
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            String name = wb.getSheetName(i).toLowerCase();
            for (String kw : keywords) {
                if (name.contains(kw)) {
                    return wb.getSheetAt(i);
                }
            }
        }
        return null;
    }

    private static class ParsedSheetResult {
        List<String> detectedCoCodes = new ArrayList<>();
        int outcomesCount = 0;
        int competenciesCount = 0;
        int keywordsCount = 0;
        Map<String, Object> keywordsStore = new LinkedHashMap<>(); // CO -> Outcome -> List<List<String>>
        Map<String, Map<String, Integer>> matrixLevels = new LinkedHashMap<>(); // CO -> Outcome -> level
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
    }

    private ParsedSheetResult parseSheet(Sheet sheet, boolean isPso,
                                         List<CourseOutcome> expectedCos,
                                         List<ProgrammeOutcome> expectedPos,
                                         List<ProgrammeSpecificOutcome> expectedPsos) {
        ParsedSheetResult res = new ParsedSheetResult();

        // 1. Locate Row 1 (Header row containing COs)
        Row r1 = sheet.getRow(1);
        if (r1 == null) {
            r1 = sheet.getRow(0);
        }
        if (r1 == null) {
            res.errors.add("Sheet '" + sheet.getSheetName() + "' is empty or missing headers.");
            return res;
        }

        // Detect CO columns in row 1
        List<CoColRef> coCols = new ArrayList<>();
        List<CoColRef> indicatorCols = new ArrayList<>();
        Set<String> seenCoCodes = new HashSet<>();

        DataFormatter df = new DataFormatter();
        for (int c = 2; c < r1.getLastCellNum(); c++) {
            Cell cell = r1.getCell(c);
            String val = df.formatCellValue(cell).trim();
            Matcher m = CO_PATTERN.matcher(val);
            if (m.matches()) {
                String coCode = m.group(1).toUpperCase();
                if (!seenCoCodes.contains(coCode)) {
                    seenCoCodes.add(coCode);
                    coCols.add(new CoColRef(coCode, c));
                    res.detectedCoCodes.add(coCode);
                } else {
                    indicatorCols.add(new CoColRef(coCode, c));
                }
            }
        }

        if (coCols.isEmpty()) {
            res.errors.add("No Course Outcome columns (e.g. CO1, CO2) detected in row 2 of sheet '" + sheet.getSheetName() + "'.");
            return res;
        }

        // Initialize keywordsStore
        for (CoColRef co : coCols) {
            res.keywordsStore.put(co.code, new LinkedHashMap<String, List<List<String>>>());
            res.matrixLevels.put(co.code, new LinkedHashMap<String, Integer>());
        }

        // 2. Parse outcome blocks
        int currentOutcomeIdx = 0;
        String currentOutcomeCode = null;
        List<CompetencyRowRef> currentCompetencies = new ArrayList<>();
        String prefix = isPso ? "PSO" : "PO";

        for (int r = 2; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String colA = df.formatCellValue(row.getCell(0)).trim();
            String colB = df.formatCellValue(row.getCell(1)).trim();
            String colD = df.formatCellValue(row.getCell(coCols.get(0).colIndex)).trim();

            // Summary row check
            if (colD.toLowerCase().contains("mapping strength") || colA.toLowerCase().contains("mapping strength") || colB.toLowerCase().contains("mapping strength")) {
                // Read strength row
                for (CoColRef co : coCols) {
                    Integer level = null;
                    // Try indicator col first
                    CoColRef indRef = findColRef(indicatorCols, co.code);
                    if (indRef != null) {
                        String sVal = df.formatCellValue(row.getCell(indRef.colIndex)).trim();
                        level = parseStrengthLevel(sVal);
                    }
                    if (level == null) {
                        String sVal = df.formatCellValue(row.getCell(co.colIndex)).trim();
                        level = parseStrengthLevel(sVal);
                    }

                    if (level == null) {
                        // Compute dynamically from mapped competencies
                        int mapped = 0;
                        for (CompetencyRowRef comp : currentCompetencies) {
                            List<String> kws = comp.keywordsByCo.get(co.code);
                            if (kws != null && !kws.isEmpty()) mapped++;
                        }
                        if (!currentCompetencies.isEmpty()) {
                            int pct = (mapped * 100) / currentCompetencies.size();
                            level = (pct >= 75) ? 3 : (pct >= 50 ? 2 : (pct > 0 ? 1 : 0));
                        } else {
                            level = 0;
                        }
                    }

                    if (currentOutcomeCode != null) {
                        res.matrixLevels.get(co.code).put(currentOutcomeCode, level);
                    }
                }
                continue;
            }

            if (colD.toLowerCase().contains("no of competencies") || colD.toLowerCase().contains("% of competencies")) {
                continue;
            }

            // Outcome header check in Col A
            if (!colA.isBlank()) {
                // Save previous outcome's competencies into keywordsStore
                commitOutcomeCompetencies(res, currentOutcomeCode, currentCompetencies);

                currentOutcomeIdx++;
                String extractedCode = extractOutcomeCode(colA, prefix, currentOutcomeIdx);
                currentOutcomeCode = extractedCode;
                currentCompetencies.clear();
                res.outcomesCount++;
            }

            // Competency row check
            if (!colB.isBlank() && currentOutcomeCode != null) {
                res.competenciesCount++;
                CompetencyRowRef compRef = new CompetencyRowRef(colB);

                for (CoColRef co : coCols) {
                    String kwText = df.formatCellValue(row.getCell(co.colIndex)).trim();
                    if (!kwText.isBlank()) {
                        List<String> splitKws = Arrays.stream(kwText.split("[,;\\n]"))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());
                        compRef.keywordsByCo.put(co.code, splitKws);
                        res.keywordsCount += splitKws.size();
                    } else {
                        compRef.keywordsByCo.put(co.code, Collections.emptyList());
                    }
                }
                currentCompetencies.add(compRef);
            }
        }

        // Commit trailing outcome
        if (currentOutcomeCode != null && !currentCompetencies.isEmpty()) {
            commitOutcomeCompetencies(res, currentOutcomeCode, currentCompetencies);
        }

        return res;
    }

    private void commitOutcomeCompetencies(ParsedSheetResult res, String outcomeCode, List<CompetencyRowRef> competencies) {
        if (outcomeCode == null || competencies.isEmpty()) return;
        for (String coCode : res.keywordsStore.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, List<List<String>>> coMap = (Map<String, List<List<String>>>) res.keywordsStore.get(coCode);
            List<List<String>> compKwList = new ArrayList<>();
            for (CompetencyRowRef comp : competencies) {
                List<String> kws = comp.keywordsByCo.getOrDefault(coCode, Collections.emptyList());
                compKwList.add(kws);
            }
            coMap.put(outcomeCode, compKwList);
        }
    }

    private String extractOutcomeCode(String text, String prefix, int fallbackIdx) {
        if (text == null || text.isBlank()) return prefix + fallbackIdx;
        Matcher m1 = isPrefixMatch(text, prefix);
        if (m1 != null && m1.find()) {
            return m1.group(1).toUpperCase().replaceAll("\\s+", "");
        }
        Matcher m2 = NUMBERED_PREFIX_PATTERN.matcher(text);
        if (m2.find()) {
            return prefix + m2.group(1);
        }
        return prefix + fallbackIdx;
    }

    private Matcher isPrefixMatch(String text, String prefix) {
        if ("PSO".equalsIgnoreCase(prefix)) {
            return PSO_CODE_PATTERN.matcher(text);
        }
        return PO_CODE_PATTERN.matcher(text);
    }

    private Integer parseStrengthLevel(String s) {
        if (s == null || s.isBlank() || s.equals("-")) return 0;
        try {
            int val = Integer.parseInt(s.trim());
            return (val >= 1 && val <= 3) ? val : 0;
        } catch (NumberFormatException e) {
            try {
                double d = Double.parseDouble(s.trim());
                int val = (int) Math.round(d);
                return (val >= 1 && val <= 3) ? val : 0;
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    private CoColRef findColRef(List<CoColRef> list, String code) {
        for (CoColRef r : list) {
            if (r.code.equalsIgnoreCase(code)) return r;
        }
        return null;
    }

    private record CoColRef(String code, int colIndex) {}

    private static class CompetencyRowRef {
        String statement;
        Map<String, List<String>> keywordsByCo = new LinkedHashMap<>();

        CompetencyRowRef(String statement) {
            this.statement = statement;
        }
    }

    /**
     * Commits the mapping and keywords from the preview/request to database entities.
     */
    @Transactional
    public CourseMappingImportResultDto commitImport(String programmeBatchCourseId, CourseMappingImportCommitRequestDto request) {
        if (programmeBatchCourseId == null || programmeBatchCourseId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programmeBatchCourseId is required.");
        }

        ProgrammeBatchCourse offering = programmeBatchCourseRepository.findById(programmeBatchCourseId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + programmeBatchCourseId));

        enforceOfferingAccess(offering, true);
        if (approvalService != null) {
            approvalService.resetToDraftOnModification(ApprovalType.CO_DEFINITION, offering.getId(), null);
        }

        String scope = (request != null && request.getScope() != null) ? request.getScope().trim().toUpperCase() : "ALL";
        boolean savePo = "ALL".equalsIgnoreCase(scope) || "PO".equalsIgnoreCase(scope);
        boolean savePso = "ALL".equalsIgnoreCase(scope) || "PSO".equalsIgnoreCase(scope);

        int savedPoKwCount = 0;
        int savedPsoKwCount = 0;
        int savedPoMapCount = 0;
        int savedPsoMapCount = 0;

        // 1. Save PO Keywords
        if (savePo && request != null && request.getPoKeywordsStore() != null && !request.getPoKeywordsStore().isEmpty()) {
            try {
                String poJson = objectMapper.writeValueAsString(request.getPoKeywordsStore());
                CourseMappingKeyword entity = courseMappingKeywordRepository
                        .findByProgrammeBatchCourseIdAndKeywordType(offering.getId(), "PO")
                        .orElse(CourseMappingKeyword.builder()
                                .id("kw-po-" + UUID.randomUUID().toString().substring(0, 8))
                                .programmeBatchCourseId(offering.getId())
                                .keywordType("PO")
                                .build());
                entity.setKeywordsJson(poJson);
                courseMappingKeywordRepository.save(entity);
                savedPoKwCount = request.getPoKeywordsStore().size();
            } catch (Exception e) {
                log.error("Failed to serialize PO keywords json for offering {}", offering.getId(), e);
            }
        }

        // 2. Save PSO Keywords
        if (savePso && request != null && request.getPsoKeywordsStore() != null && !request.getPsoKeywordsStore().isEmpty()) {
            try {
                String psoJson = objectMapper.writeValueAsString(request.getPsoKeywordsStore());
                CourseMappingKeyword entity = courseMappingKeywordRepository
                        .findByProgrammeBatchCourseIdAndKeywordType(offering.getId(), "PSO")
                        .orElse(CourseMappingKeyword.builder()
                                .id("kw-pso-" + UUID.randomUUID().toString().substring(0, 8))
                                .programmeBatchCourseId(offering.getId())
                                .keywordType("PSO")
                                .build());
                entity.setKeywordsJson(psoJson);
                courseMappingKeywordRepository.save(entity);
                savedPsoKwCount = request.getPsoKeywordsStore().size();
            } catch (Exception e) {
                log.error("Failed to serialize PSO keywords json for offering {}", offering.getId(), e);
            }
        }

        List<CourseOutcome> cos = courseOutcomeRepository.findByProgrammeBatchCourseId(offering.getId());
        List<String> coIds = cos.stream().map(CourseOutcome::getId).collect(Collectors.toList());

        // 3. Save PO Mappings
        if (savePo && request != null && request.getPoMappings() != null && !coIds.isEmpty()) {
            List<CoPoMapping> existingPo = coPoMappingRepository.findByCourseOutcomeIdIn(coIds);
            Map<String, CoPoMapping> existingMap = existingPo.stream()
                    .collect(Collectors.toMap(m -> m.getCourseOutcomeId() + "::" + m.getPoCode(), m -> m, (a, b) -> a));

            List<CoPoMapping> toSavePo = new ArrayList<>();
            for (CoPoMapping m : request.getPoMappings()) {
                if (m.getCourseOutcomeId() == null || m.getPoCode() == null) continue;
                String key = m.getCourseOutcomeId() + "::" + m.getPoCode();
                CoPoMapping target = existingMap.get(key);
                if (target != null) {
                    target.setMappingLevel(m.getMappingLevel() != null ? m.getMappingLevel() : 0);
                    toSavePo.add(target);
                } else {
                    if (m.getId() == null || m.getId().isBlank()) {
                        m.setId("copomap-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                    toSavePo.add(m);
                }
            }
            coPoMappingRepository.saveAll(toSavePo);
            savedPoMapCount = toSavePo.size();
        }

        // 4. Save PSO Mappings
        if (savePso && request != null && request.getPsoMappings() != null && !coIds.isEmpty()) {
            List<CoPsoMapping> existingPso = coPsoMappingRepository.findByCourseOutcomeIdIn(coIds);
            Map<String, CoPsoMapping> existingMap = existingPso.stream()
                    .collect(Collectors.toMap(m -> m.getCourseOutcomeId() + "::" + m.getPsoCode(), m -> m, (a, b) -> a));

            List<CoPsoMapping> toSavePso = new ArrayList<>();
            for (CoPsoMapping m : request.getPsoMappings()) {
                if (m.getCourseOutcomeId() == null || m.getPsoCode() == null) continue;
                String key = m.getCourseOutcomeId() + "::" + m.getPsoCode();
                CoPsoMapping target = existingMap.get(key);
                if (target != null) {
                    target.setMappingLevel(m.getMappingLevel() != null ? m.getMappingLevel() : 0);
                    toSavePso.add(target);
                } else {
                    if (m.getId() == null || m.getId().isBlank()) {
                        m.setId("copsomap-" + UUID.randomUUID().toString().substring(0, 8));
                    }
                    toSavePso.add(m);
                }
            }
            coPsoMappingRepository.saveAll(toSavePso);
            savedPsoMapCount = toSavePso.size();
        }

        if (auditLogService != null) {
            auditLogService.recordSuccess(AuditAction.UPDATE, ResourceType.CO_PO_MAPPING, offering.getId(),
                    "MAPPING_DRAFT", "MAPPING_IMPORTED", "Imported CO-PO/PSO mappings and keywords from Excel",
                    Map.of("scope", scope, "poMappingsCount", savedPoMapCount, "psoMappingsCount", savedPsoMapCount));
        }

        CourseMappingMatrixDto matrixDto = outcomeService.getMappingsByOffering(offering.getId());

        return CourseMappingImportResultDto.builder()
                .success(true)
                .programmeBatchCourseId(offering.getId())
                .courseCode(offering.getCourseCode())
                .scope(scope)
                .savedPoMappingsCount(savedPoMapCount)
                .savedPsoMappingsCount(savedPsoMapCount)
                .savedPoKeywordsCount(savedPoKwCount)
                .savedPsoKeywordsCount(savedPsoKwCount)
                .message("CO-PO/PSO Mapping Matrix and keywords imported successfully.")
                .mappingMatrix(matrixDto)
                .build();
    }

    /**
     * Multi-part commit entrypoint: parses file and commits atomically.
     */
    @Transactional
    public CourseMappingImportResultDto commitImport(String programmeBatchCourseId, MultipartFile file, String scope) {
        CourseMappingImportPreviewDto preview = previewImport(programmeBatchCourseId, file, scope);
        if (!preview.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Import validation failed: " + String.join("; ", preview.getErrors()));
        }

        CourseMappingImportCommitRequestDto commitReq = CourseMappingImportCommitRequestDto.builder()
                .scope(scope)
                .poKeywordsStore(preview.getPoKeywordsStore())
                .psoKeywordsStore(preview.getPsoKeywordsStore())
                .poMappings(preview.getPoMappings())
                .psoMappings(preview.getPsoMappings())
                .build();

        return commitImport(programmeBatchCourseId, commitReq);
    }
}
