package com.dypiu.nba.service;

import com.dypiu.nba.audit.AuditAction;
import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.dto.CourseImportItemDto;
import com.dypiu.nba.dto.CourseImportPreviewDto;
import com.dypiu.nba.dto.CourseImportResultDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import lombok.Getter;
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
public class CourseExcelImportService {

    private final AcademicService academicService;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final ProgrammeBatchCourseRepository programmeBatchCourseRepository;
    private final MasterProgrammeRepository masterProgrammeRepository;
    private final UserRepository userRepository;
    private final AcademicLookupCacheService academicLookupCacheService;
    private final AuditLogService auditLogService;

    private static final Pattern SEMESTER_SHEET_PATTERN =
            Pattern.compile("(?i)(?:sem(?:ester)?|s)\\s*(\\d+)|\\b(\\d+)\\b");

    private static final Set<String> VALID_COURSE_TYPES = Set.of(
            "THEORY", "PRACTICAL", "LAB", "THEORY_PRACTICAL", "PROJECT", "AUDIT", "ELECTIVE"
    );

    /**
     * Dynamically generates an Excel template based on the programme's semester limit.
     */
    public byte[] generateTemplate(String programmeBatchId) {
        if (programmeBatchId == null || programmeBatchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programmeBatchId is required.");
        }

        academicService.enforceBatchScope(programmeBatchId);

        ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme Batch not found: " + programmeBatchId));

        int durationYears = resolveDurationYears(batch);
        int maxSemesters = durationYears * 2;

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Header font and style
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

            CellStyle centerStyle = wb.createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] headers = {
                    "Course Code *",
                    "Course Name *",
                    "Credits *",
                    "Course Type *",
                    "Semester",
                    "Course Coordinator"
            };

            for (int sem = 1; sem <= maxSemesters; sem++) {
                Sheet sheet = wb.createSheet("Sem " + sem);
                sheet.setDisplayGridlines(true);

                // Header row
                Row headerRow = sheet.createRow(0);
                headerRow.setHeightInPoints(24);
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = headerRow.createCell(c);
                    cell.setCellValue(headers[c]);
                    cell.setCellStyle(headerStyle);
                }

                // Sample row for guidance
                Row sampleRow = sheet.createRow(1);
                sampleRow.createCell(0).setCellValue("CS" + sem + "01");
                sampleRow.createCell(1).setCellValue("Sample Course " + sem);
                sampleRow.createCell(2).setCellValue(3);
                sampleRow.createCell(3).setCellValue("THEORY");
                Cell semCell = sampleRow.createCell(4);
                semCell.setCellValue(sem);
                semCell.setCellStyle(centerStyle);
                sampleRow.createCell(5).setCellValue(""); // coordinator optional

                for (int c = 0; c < headers.length; c++) {
                    sheet.setColumnWidth(c, c == 1 ? 8000 : (c == 5 ? 7000 : 4500));
                }
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate course Excel template for batch {}", programmeBatchId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate template: " + e.getMessage());
        }
    }

    /**
     * Parses the uploaded workbook and returns validation & preview without persisting.
     */
    public CourseImportPreviewDto previewImport(String programmeBatchId, MultipartFile file) {
        if (programmeBatchId == null || programmeBatchId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "programmeBatchId is required.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An Excel file (.xlsx) is required.");
        }

        academicService.enforceBatchScope(programmeBatchId);

        ProgrammeBatch batch = programmeBatchRepository.findById(programmeBatchId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programme Batch not found: " + programmeBatchId));

        int durationYears = resolveDurationYears(batch);
        int maxSemesters = durationYears * 2;
        String progName = batch.getName() != null ? batch.getName() : programmeBatchId;

        // Preload active existing courses for this batch
        List<ProgrammeBatchCourse> existingCourses = programmeBatchCourseRepository
                .findByProgrammeBatchIdAndDeletedAtIsNull(batch.getId());
        Set<String> existingCodeSet = existingCourses.stream()
                .map(c -> c.getCode() != null ? c.getCode().trim().toLowerCase() : "")
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        // Preload active users for fast coordinator lookup
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(u -> u.getIsActive() == null || Boolean.TRUE.equals(u.getIsActive()))
                .toList();
        Map<String, User> userEmailMap = new HashMap<>();
        Map<String, User> userUsernameMap = new HashMap<>();
        Map<String, User> userNameMap = new HashMap<>();
        for (User u : activeUsers) {
            if (u.getEmail() != null) userEmailMap.put(u.getEmail().trim().toLowerCase(), u);
            if (u.getUsername() != null) userUsernameMap.put(u.getUsername().trim().toLowerCase(), u);
            if (u.getName() != null) userNameMap.put(u.getName().trim().toLowerCase(), u);
        }

        List<String> globalErrors = new ArrayList<>();
        List<String> globalWarnings = new ArrayList<>();
        List<String> sheetsDetected = new ArrayList<>();
        Map<Integer, List<CourseImportItemDto>> coursesBySemester = new LinkedHashMap<>();

        Map<String, String> workbookSeenCodes = new HashMap<>(); // lowercase code -> location string
        int totalCourses = 0;
        int validCount = 0;
        int warningCount = 0;
        int errorCount = 0;

        DataFormatter formatter = new DataFormatter();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            int numberOfSheets = workbook.getNumberOfSheets();
            if (numberOfSheets == 0) {
                globalErrors.add("The uploaded workbook contains no sheets.");
            }

            for (int s = 0; s < numberOfSheets; s++) {
                Sheet sheet = workbook.getSheetAt(s);
                String sheetName = sheet.getSheetName().trim();
                sheetsDetected.add(sheetName);

                Integer sheetSemester = parseSemesterNumber(sheetName);
                if (sheetSemester == null) {
                    globalErrors.add("Sheet '" + sheetName + "' could not be recognized as a valid semester sheet (expected e.g. 'Sem 1', 'Sem 2', 'Semester 1').");
                    continue;
                }

                if (sheetSemester < 1 || sheetSemester > maxSemesters) {
                    globalErrors.add("Sheet '" + sheetName + "' corresponds to Semester " + sheetSemester +
                            ", which exceeds the maximum allowed semesters (" + maxSemesters + " semesters / " + durationYears + " years) for programme '" + progName + "'.");
                    continue;
                }

                // Check semester allocation editability
                try {
                    academicService.enforceSemesterAllocationEditability(batch.getId(), sheetSemester);
                } catch (ResponseStatusException rse) {
                    globalErrors.add("Semester " + sheetSemester + " (" + sheetName + "): " + rse.getReason());
                    continue;
                }

                // Locate header row and column mappings
                HeaderColumnMapping mapping = resolveHeaderMapping(sheet, formatter);
                if (!mapping.isValid()) {
                    globalErrors.add("Sheet '" + sheetName + "' is missing required column(s): " + String.join(", ", mapping.getMissingColumns()));
                    continue;
                }

                List<CourseImportItemDto> semesterItems = new ArrayList<>();
                int lastRowNum = sheet.getLastRowNum();

                for (int r = mapping.getHeaderRowIndex() + 1; r <= lastRowNum; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null || isRowEmpty(row, formatter)) {
                        continue;
                    }

                    int rowNumber = r + 1; // 1-indexed for user display
                    List<String> issues = new ArrayList<>();
                    boolean isError = false;
                    boolean isWarning = false;

                    String rawCode = getCellText(row, mapping.getCodeCol(), formatter);
                    String rawName = getCellText(row, mapping.getNameCol(), formatter);
                    String rawCredits = getCellText(row, mapping.getCreditsCol(), formatter);
                    String rawType = getCellText(row, mapping.getTypeCol(), formatter);
                    String rawSem = getCellText(row, mapping.getSemesterCol(), formatter);
                    String rawCoord = getCellText(row, mapping.getCoordinatorCol(), formatter);

                    // 1. Course Code
                    if (rawCode.isBlank()) {
                        issues.add("Course Code is required.");
                        isError = true;
                    }
                    String finalCode = rawCode.trim().toUpperCase();

                    // 2. Course Name
                    if (rawName.isBlank()) {
                        issues.add("Course Name is required.");
                        isError = true;
                    }

                    // 3. Credits
                    Integer credits = null;
                    if (rawCredits.isBlank()) {
                        issues.add("Credits is required.");
                        isError = true;
                    } else {
                        try {
                            double credDbl = Double.parseDouble(rawCredits.trim());
                            credits = (int) Math.round(credDbl);
                            if (credits <= 0) {
                                issues.add("Credits must be greater than 0.");
                                isError = true;
                            }
                        } catch (Exception e) {
                            issues.add("Credits must be a valid number (e.g. 3, 4).");
                            isError = true;
                        }
                    }

                    // 4. Course Type
                    String normalizedType = normalizeCourseType(rawType);
                    if (!VALID_COURSE_TYPES.contains(normalizedType)) {
                        issues.add("Invalid course type '" + rawType + "'. Supported types: THEORY, PRACTICAL, LAB, THEORY_PRACTICAL, PROJECT, AUDIT, ELECTIVE.");
                        isError = true;
                    }

                    // 5. Row Semester check against sheet semester
                    if (!rawSem.isBlank()) {
                        try {
                            int parsedRowSem = (int) Math.round(Double.parseDouble(rawSem.trim()));
                            if (parsedRowSem != sheetSemester) {
                                issues.add("Row semester (" + parsedRowSem + ") does not match sheet semester (" + sheetSemester + ").");
                                isError = true;
                            }
                        } catch (Exception e) {
                            // If row semester is not numeric but has text, test if it matches sheet
                            Integer semFromText = parseSemesterNumber(rawSem);
                            if (semFromText == null || semFromText != sheetSemester) {
                                issues.add("Row semester '" + rawSem + "' does not match sheet semester (" + sheetSemester + ").");
                                isError = true;
                            }
                        }
                    }

                    boolean isDuplicate = false;
                    // 6. Duplicate checking
                    if (!finalCode.isBlank()) {
                        String lowerCode = finalCode.toLowerCase();
                        if (workbookSeenCodes.containsKey(lowerCode)) {
                            issues.add("Duplicate course code '" + finalCode + "' in workbook (already present in " + workbookSeenCodes.get(lowerCode) + ").");
                            isError = true;
                            isDuplicate = true;
                        } else {
                            workbookSeenCodes.put(lowerCode, "Sheet '" + sheetName + "', Row " + rowNumber);
                        }

                        if (existingCodeSet.contains(lowerCode)) {
                            issues.add("Course code '" + finalCode + "' already exists in this Programme Batch.");
                            isError = true;
                            isDuplicate = true;
                        }
                    }

                    // 7. Coordinator resolution
                    Long coordId = null;
                    String coordName = null;
                    String coordEmail = null;
                    boolean coordMatched = false;

                    if (!rawCoord.isBlank()) {
                        String cleanCoord = rawCoord.trim();
                        String lowerCoord = cleanCoord.toLowerCase();

                        User matchedUser = userEmailMap.get(lowerCoord);
                        if (matchedUser == null) matchedUser = userUsernameMap.get(lowerCoord);
                        if (matchedUser == null) matchedUser = userNameMap.get(lowerCoord);

                        if (matchedUser != null) {
                            coordId = matchedUser.getId();
                            coordName = matchedUser.getName();
                            coordEmail = matchedUser.getEmail();
                            coordMatched = true;
                        } else {
                            coordName = cleanCoord;
                            coordMatched = false;
                            issues.add("Course Coordinator '" + cleanCoord + "' could not be matched to an active faculty user.");
                            isWarning = true;
                        }
                    } else {
                        coordMatched = true; // Optional coordinator, so empty is considered valid
                    }

                    String status = isDuplicate ? "DUPLICATE" : (isError ? "INVALID" : (isWarning ? "WARNING" : "VALID"));
                    if (isError) {
                        errorCount++;
                    } else if (isWarning) {
                        warningCount++;
                    } else {
                        validCount++;
                    }
                    totalCourses++;

                    CourseImportItemDto item = CourseImportItemDto.builder()
                            .rowNumber(rowNumber)
                            .sheetName(sheetName)
                            .semester(sheetSemester)
                            .courseCode(finalCode)
                            .courseName(rawName.trim())
                            .credits(credits)
                            .courseType(normalizedType)
                            .coordinatorRaw(rawCoord.trim())
                            .coordinatorId(coordId)
                            .coordinatorName(coordName)
                            .coordinatorEmail(coordEmail)
                            .coordinatorMatched(coordMatched)
                            .status(status)
                            .issues(issues)
                            .build();

                    semesterItems.add(item);
                }

                if (!semesterItems.isEmpty()) {
                    coursesBySemester.put(sheetSemester, semesterItems);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse course Excel for batch {}", programmeBatchId, e);
            globalErrors.add("Failed to parse Excel file: " + e.getMessage());
        }

        boolean isValid = globalErrors.isEmpty() && errorCount == 0 && totalCourses > 0;

        return CourseImportPreviewDto.builder()
                .valid(isValid)
                .totalCourses(totalCourses)
                .validCount(validCount)
                .warningCount(warningCount)
                .errorCount(errorCount + globalErrors.size())
                .maxSemesters(maxSemesters)
                .durationYears(durationYears)
                .programmeBatchId(batch.getId())
                .programmeBatchName(batch.getName())
                .masterProgrammeName(progName)
                .sheetsDetected(sheetsDetected)
                .coursesBySemester(coursesBySemester)
                .errors(globalErrors)
                .warnings(globalWarnings)
                .build();
    }

    /**
     * Executes the atomic transactional import of all valid courses.
     * Rolls back completely if any validation error or persistence issue occurs.
     */
    @Transactional
    public CourseImportResultDto commitImport(String programmeBatchId, MultipartFile file) {
        CourseImportPreviewDto preview = previewImport(programmeBatchId, file);

        if (!preview.isValid() || preview.getErrorCount() > 0) {
            String firstError = !preview.getErrors().isEmpty()
                    ? preview.getErrors().get(0)
                    : "Workbook contains invalid courses or semester mismatches.";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Excel import validation failed: " + firstError);
        }

        if (preview.getTotalCourses() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded Excel workbook contains no courses to import.");
        }

        List<ProgrammeBatchCourse> coursesToPersist = new ArrayList<>();
        List<String> importedCodes = new ArrayList<>();
        int matchedCoordinators = 0;
        int unmatchedCoordinators = 0;

        for (Map.Entry<Integer, List<CourseImportItemDto>> entry : preview.getCoursesBySemester().entrySet()) {
            Integer semester = entry.getKey();
            List<CourseImportItemDto> items = entry.getValue();

            // Enforce editability on the semester
            academicService.enforceSemesterAllocationEditability(programmeBatchId, semester);

            for (CourseImportItemDto item : items) {
                if ("INVALID".equals(item.getStatus()) || "DUPLICATE".equals(item.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Cannot commit import: Row " + item.getRowNumber() + " (" + item.getCourseCode() + ") has validation errors.");
                }

                String offeringId = "offering-" + UUID.randomUUID().toString().substring(0, 8);
                ProgrammeBatchCourse course = ProgrammeBatchCourse.builder()
                        .id(offeringId)
                        .programmeBatchId(programmeBatchId)
                        .code(item.getCourseCode())
                        .name(item.getCourseName())
                        .credits(item.getCredits() != null ? item.getCredits() : 3)
                        .courseType(item.getCourseType())
                        .semester(semester)
                        .courseCoordinatorId(item.getCoordinatorId())
                        .courseCoordinatorName(item.getCoordinatorName())
                        .assignedFaculty(item.getCoordinatorName() != null ? (item.getCoordinatorName() + (item.getCoordinatorEmail() != null && !item.getCoordinatorEmail().isBlank() ? " (" + item.getCoordinatorEmail() + ")" : "")) : null)
                        .status("ACTIVE")
                        .build();

                coursesToPersist.add(course);
                importedCodes.add(item.getCourseCode());

                if (item.isCoordinatorMatched() && item.getCoordinatorId() != null) {
                    matchedCoordinators++;
                } else if (!item.getCoordinatorRaw().isBlank()) {
                    unmatchedCoordinators++;
                }
            }
        }

        programmeBatchCourseRepository.saveAll(coursesToPersist);
        programmeBatchCourseRepository.flush();

        if (academicLookupCacheService != null) {
            academicLookupCacheService.evictCourseCache();
        }

        if (auditLogService != null) {
            auditLogService.recordSuccess(
                    AuditAction.CREATE,
                    ResourceType.PROGRAMME_BATCH_COURSE,
                    programmeBatchId,
                    null,
                    "ACTIVE",
                    "Bulk imported " + coursesToPersist.size() + " courses from Excel for batch " + programmeBatchId,
                    Map.of(
                            "courseCount", String.valueOf(coursesToPersist.size()),
                            "semesters", preview.getCoursesBySemester().keySet().toString(),
                            "programmeBatchId", programmeBatchId
                    )
            );
        }

        log.info("Successfully imported {} courses from Excel across {} semesters for batch {}",
                coursesToPersist.size(), preview.getCoursesBySemester().size(), programmeBatchId);

        return CourseImportResultDto.builder()
                .totalCoursesImported(coursesToPersist.size())
                .semestersProcessed(new ArrayList<>(preview.getCoursesBySemester().keySet()))
                .coordinatorsMatched(matchedCoordinators)
                .coordinatorsUnmatched(unmatchedCoordinators)
                .courseCodes(importedCodes)
                .programmeBatchId(programmeBatchId)
                .build();
    }

    // --- Helper Methods ---

    public static Integer parseSemesterNumber(String sheetName) {
        if (sheetName == null || sheetName.isBlank()) return null;
        Matcher m = SEMESTER_SHEET_PATTERN.matcher(sheetName.trim());
        if (m.find()) {
            String val = m.group(1) != null ? m.group(1) : m.group(2);
            try {
                return Integer.parseInt(val);
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static String normalizeCourseType(String raw) {
        if (raw == null || raw.isBlank()) return "THEORY";
        String upper = raw.trim().toUpperCase().replaceAll("[\\s-]+", "_");
        return switch (upper) {
            case "THEORY", "TH" -> "THEORY";
            case "PRACTICAL", "LAB", "PR" -> "PRACTICAL";
            case "THEORY_PRACTICAL", "THEORY_LAB", "TH_PR", "INTEGRATED" -> "THEORY_PRACTICAL";
            case "PROJECT", "PROJ" -> "PROJECT";
            case "AUDIT" -> "AUDIT";
            case "ELECTIVE", "EL" -> "ELECTIVE";
            default -> upper;
        };
    }

    private int resolveDurationYears(ProgrammeBatch batch) {
        if (batch.getDurationYears() != null && batch.getDurationYears() > 0) {
            return batch.getDurationYears();
        }
        if (batch.getMasterProgrammeId() != null) {
            Optional<MasterProgramme> mpOpt = masterProgrammeRepository.findById(batch.getMasterProgrammeId());
            if (mpOpt.isPresent() && mpOpt.get().getDurationYears() != null && mpOpt.get().getDurationYears() > 0) {
                return mpOpt.get().getDurationYears();
            }
        }
        return 4; // Standard fallback
    }

    private HeaderColumnMapping resolveHeaderMapping(Sheet sheet, DataFormatter formatter) {
        int firstRow = sheet.getFirstRowNum();
        int lastRow = Math.min(sheet.getLastRowNum(), firstRow + 5);

        for (int r = firstRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            int codeCol = -1;
            int nameCol = -1;
            int creditsCol = -1;
            int typeCol = -1;
            int semesterCol = -1;
            int coordCol = -1;

            short lastCell = row.getLastCellNum();
            for (int c = 0; c < lastCell; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;
                String text = formatter.formatCellValue(cell).trim().toLowerCase().replaceAll("[*:]", "").trim();

                if (text.contains("code")) {
                    codeCol = c;
                } else if (text.contains("name") || text.contains("title")) {
                    nameCol = c;
                } else if (text.contains("credit")) {
                    creditsCol = c;
                } else if (text.contains("type")) {
                    typeCol = c;
                } else if (text.contains("semester") || text.equals("sem")) {
                    semesterCol = c;
                } else if (text.contains("coordinator") || text.contains("faculty")) {
                    coordCol = c;
                }
            }

            if (codeCol != -1 && nameCol != -1 && creditsCol != -1 && typeCol != -1) {
                return new HeaderColumnMapping(r, codeCol, nameCol, creditsCol, typeCol, semesterCol, coordCol);
            }
        }

        // Return invalid mapping with missing column details
        return new HeaderColumnMapping(-1, -1, -1, -1, -1, -1, -1);
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        short lastCell = row.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !formatter.formatCellValue(cell).trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellText(Row row, int colIndex, DataFormatter formatter) {
        if (colIndex < 0) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    @Getter
    @RequiredArgsConstructor
    private static class HeaderColumnMapping {
        private final int headerRowIndex;
        private final int codeCol;
        private final int nameCol;
        private final int creditsCol;
        private final int typeCol;
        private final int semesterCol;
        private final int coordinatorCol;

        public boolean isValid() {
            return codeCol != -1 && nameCol != -1 && creditsCol != -1 && typeCol != -1;
        }

        public List<String> getMissingColumns() {
            List<String> missing = new ArrayList<>();
            if (codeCol == -1) missing.add("Course Code *");
            if (nameCol == -1) missing.add("Course Name *");
            if (creditsCol == -1) missing.add("Credits *");
            if (typeCol == -1) missing.add("Course Type *");
            return missing;
        }
    }
}
