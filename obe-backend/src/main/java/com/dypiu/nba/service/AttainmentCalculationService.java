package com.dypiu.nba.service;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttainmentCalculationService {

    private final AttainmentConfigurationRepository configRepository;
    private final StudentCoMarkRepository studentCoMarkRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final BatchRepository batchRepository;
    private final ProgrammeRepository programmeRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final ProgrammeTargetRepository programmeTargetRepository;
    private final CoPoMappingRepository coPoMappingRepository;
    private final CoPsoMappingRepository coPsoMappingRepository;
    private final com.dypiu.nba.security.CurrentUserScopeService currentUserScopeService;

    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:uploads}")
    private String baseUploadDir;

    private final Map<String, ExaminationAttainmentResultDto> examinationAttainmentStore = new ConcurrentHashMap<>();
    private final Map<String, SurveyAttainmentResultDto> surveyAttainmentStore = new ConcurrentHashMap<>();
    private final Map<String, ProgrammeSurveyResultDto> programmeSurveyStore = new ConcurrentHashMap<>();

    public String resolveOfferingId(String offeringOrCourseId) {
        if (offeringOrCourseId == null || offeringOrCourseId.isBlank()) return null;
        if (courseOfferingRepository.existsById(offeringOrCourseId)) {
            return offeringOrCourseId;
        }
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(offeringOrCourseId);
        if (!offerings.isEmpty()) {
            return offerings.get(0).getId();
        }
        return offeringOrCourseId;
    }

    private CurrentUserScope getScope() {
        try {
            return currentUserScopeService != null ? currentUserScopeService.getCurrentUserScope() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void enforceOfferingOrCourseScope(String courseOfferingOrCourseId) {
        if (courseOfferingOrCourseId == null || courseOfferingOrCourseId.isBlank()) return;
        com.dypiu.nba.security.CurrentUserScope scope = getScope();
        if (scope == null || scope.isAdmin() || scope.isIqac()) return;

        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        if (offeringId != null && courseOfferingRepository.existsById(offeringId)) {
            CourseOffering offering = courseOfferingRepository.findById(offeringId).orElse(null);
            if (offering != null) {
                if (scope.isFaculty()) {
                    boolean isCoord = (offering.getCourseCoordinatorId() != null && Objects.equals(offering.getCourseCoordinatorId(), scope.getUserId()))
                            ;
                    boolean isAssigned = isCoord || (offering.getAssignedFaculty() != null && (offering.getAssignedFaculty().contains(scope.getEmail()) || offering.getAssignedFaculty().contains(scope.getName())));
                    if (!isAssigned) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course Offering.");
                    }
                    return;
                }
                if (scope.isProgrammeCoordinator() && offering.getCourseId() != null) {
                    Course course = courseRepository.findById(offering.getCourseId()).orElse(null);
                    if (course != null && !scope.getRequiredProgrammeId().equals(course.getProgrammeId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned programme scope.");
                    }
                }
                return;
            }
        }

        if (courseRepository.existsById(courseOfferingOrCourseId)) {
            Course course = courseRepository.findById(courseOfferingOrCourseId).orElse(null);
            if (course != null) {
                if (scope.isFaculty()) {
                    List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(course.getId());
                    boolean hasAssigned = offerings.stream().anyMatch(o -> {
                        boolean isCoord = (o.getCourseCoordinatorId() != null && Objects.equals(o.getCourseCoordinatorId(), scope.getUserId()))
                                ;
                        return isCoord || (o.getAssignedFaculty() != null && (o.getAssignedFaculty().contains(scope.getEmail()) || o.getAssignedFaculty().contains(scope.getName())));
                    });
                    if (!hasAssigned) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You are not assigned to this Course.");
                    }
                    return;
                }
                if (scope.isProgrammeCoordinator() && !course.getProgrammeId().equals(scope.getRequiredProgrammeId())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Resource is outside your assigned programme scope.");
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public AttainmentConfiguration getAttainmentConfig(String courseOfferingOrCourseId) {
        System.out.println("[AttainmentCalculationService] getAttainmentConfig called | courseOfferingOrCourseId: " + courseOfferingOrCourseId);
        enforceOfferingOrCourseScope(courseOfferingOrCourseId);
        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        return configRepository.findByCourseOfferingId(offeringId)
                .orElseGet(() -> AttainmentConfiguration.builder()
                        .id("cfg-" + offeringId)
                        .courseOfferingId(offeringId)
                        .directWeight(new BigDecimal("80.00"))
                        .indirectWeight(new BigDecimal("20.00"))
                        .directThreshold(new BigDecimal("60.00"))
                        .indirectThreshold(new BigDecimal("60.00"))
                        .status(AttainmentConfigStatus.DRAFT)
                        .build());
    }

    @Transactional
    public AttainmentConfiguration saveAttainmentConfig(String courseOfferingOrCourseId, AttainmentConfiguration config) {
        System.out.println("[AttainmentCalculationService] saveAttainmentConfig called | courseOfferingOrCourseId: " + courseOfferingOrCourseId);
        enforceOfferingOrCourseScope(courseOfferingOrCourseId);
        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        config.setCourseOfferingId(offeringId);
        if (config.getId() == null) config.setId("cfg-" + offeringId);
        return configRepository.save(config);
    }

    // --- Database Persistence Helper Methods ---

    @Transactional
    public void saveStudentCoMarksToDatabase(String courseOfferingOrCourseId, Map<String, BigDecimal> coMaxMarks, List<StudentMarksRowDto> studentList) {
        System.out.println("[AttainmentCalculationService] saveStudentCoMarksToDatabase called | courseOfferingOrCourseId: " + courseOfferingOrCourseId + " | students: " + (studentList != null ? studentList.size() : 0));
        if (courseOfferingOrCourseId == null || studentList == null || studentList.isEmpty()) return;

        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + offeringId));

        String batchId = offering.getBatchId();

        // 1. Strict Cohort/Batch Validation: Validate that all students belong to the Course Offering's batch
        for (StudentMarksRowDto st : studentList) {
            String prn = st.getPrn();
            if (prn == null || prn.isBlank()) continue;

            Optional<Student> studentOpt = studentRepository.findByPrn(prn);
            if (studentOpt.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Validation Error: Student with PRN '" + prn + "' is not registered in the system. Mark upload rejected.");
            }

            Student student = studentOpt.get();
            if (student.getBatchId() == null || !student.getBatchId().equals(batchId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Validation Error: Student PRN '" + prn + "' belongs to batch '" + student.getBatchId()
                                + "', but this Course Offering belongs to batch '" + batchId + "'. Cross-batch mark upload rejected.");
            }
        }

        // 2. Delete existing marks for this offering
        studentCoMarkRepository.deleteByCourseOfferingId(offeringId);
        studentCoMarkRepository.flush();

        // 3. Save validated Student CO marks
        List<StudentCoMark> markEntities = new ArrayList<>();
        for (StudentMarksRowDto st : studentList) {
            String prn = st.getPrn();
            Student student = studentRepository.findByPrn(prn).orElse(null);
            if (student == null) continue;

            if (st.getCoMarks() != null) {
                for (Map.Entry<String, BigDecimal> entry : st.getCoMarks().entrySet()) {
                    String coCode = entry.getKey();
                    BigDecimal marksObtained = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
                    BigDecimal maxMarks = coMaxMarks.getOrDefault(coCode, new BigDecimal("100.00"));

                    StudentCoMark markEntity = StudentCoMark.builder()
                            .id("mrk-" + UUID.randomUUID().toString().substring(0, 8))
                            .courseOfferingId(offeringId)
                            .studentId(student.getId())
                            .prn(student.getPrn())
                            .studentName(student.getName())
                            .coCode(coCode)
                            .marksObtained(marksObtained)
                            .maxMarks(maxMarks)
                            .build();
                    markEntities.add(markEntity);
                }
            }
        }

        if (!markEntities.isEmpty()) {
            studentCoMarkRepository.saveAll(markEntities);
        }
    }

    // =========================================================================
    //  LEVEL BAND EVALUATOR & PARSER HELPERS
    // =========================================================================

    public record LevelBand(int level, BigDecimal minPercentage, BigDecimal maxPercentage) {}

    public List<LevelBand> parseLevelBands(String levelsJson, boolean isDirect) {
        if (levelsJson != null && !levelsJson.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(levelsJson);
                if (root.isArray() && root.size() > 0) {
                    List<LevelBand> bands = new ArrayList<>();
                    for (JsonNode node : root) {
                        int level = node.has("level") ? node.get("level").asInt() : bands.size() + 1;
                        BigDecimal min = node.has("minPercentage") ? new BigDecimal(node.get("minPercentage").asText()) :
                                (node.has("min") ? new BigDecimal(node.get("min").asText()) : BigDecimal.ZERO);
                        BigDecimal max = node.has("maxPercentage") ? new BigDecimal(node.get("maxPercentage").asText()) :
                                (node.has("max") ? new BigDecimal(node.get("max").asText()) : new BigDecimal("100.00"));

                        if (min.compareTo(max) > 0) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Invalid level band configuration: minPercentage (" + min + ") cannot be greater than maxPercentage (" + max + ").");
                        }
                        if (min.compareTo(BigDecimal.ZERO) < 0 || max.compareTo(new BigDecimal("100.00")) > 0) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Invalid level band configuration: percentages must be within 0 to 100. Found [" + min + ", " + max + "].");
                        }
                        bands.add(new LevelBand(level, min, max));
                    }
                    bands.sort(Comparator.comparing(LevelBand::minPercentage));
                    return bands;
                }
            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (Exception e) {
                log.warn("Failed to parse custom {} levels JSON: {}. Using default level bands.", isDirect ? "direct" : "indirect", levelsJson);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid level band JSON format: " + e.getMessage());
            }
        }

        // Default standard 3-tier bands
        return List.of(
                new LevelBand(1, new BigDecimal("0.00"), new BigDecimal("40.00")),
                new LevelBand(2, new BigDecimal("40.00"), new BigDecimal("60.00")),
                new LevelBand(3, new BigDecimal("60.00"), new BigDecimal("100.00"))
        );
    }

    public int evaluateLevelBand(BigDecimal percentage, List<LevelBand> bands) {
        if (bands == null || bands.isEmpty()) {
            bands = List.of(
                    new LevelBand(1, new BigDecimal("0.00"), new BigDecimal("40.00")),
                    new LevelBand(2, new BigDecimal("40.00"), new BigDecimal("60.00")),
                    new LevelBand(3, new BigDecimal("60.00"), new BigDecimal("100.00"))
            );
        }

        if (percentage == null) return 0;

        for (int i = 0; i < bands.size(); i++) {
            LevelBand band = bands.get(i);
            boolean isTopBand = (i == bands.size() - 1) || band.maxPercentage().compareTo(new BigDecimal("100.00")) >= 0;

            if (isTopBand) {
                if (percentage.compareTo(band.minPercentage()) >= 0 && percentage.compareTo(band.maxPercentage()) <= 0) {
                    return band.level();
                }
            } else {
                if (percentage.compareTo(band.minPercentage()) >= 0 && percentage.compareTo(band.maxPercentage()) < 0) {
                    return band.level();
                }
            }
        }

        if (percentage.compareTo(new BigDecimal("100.00")) > 0) {
            return bands.get(bands.size() - 1).level();
        } else if (percentage.compareTo(BigDecimal.ZERO) <= 0) {
            return bands.get(0).level();
        }

        return bands.get(0).level();
    }

    private Sheet findExaminationSheet(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet s = workbook.getSheetAt(i);
            String name = s.getSheetName();
            if (name != null && name.toLowerCase().matches(".*(examination|exam|direct).*")) {
                return s;
            }
        }
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet s = workbook.getSheetAt(i);
            for (Row r : s) {
                for (Cell c : r) {
                    try {
                        String text = c.getStringCellValue();
                        if (text != null) {
                            String lower = text.toLowerCase();
                            if (lower.contains("direct method") || lower.contains("co assessment") || lower.contains("fraction of out of")) {
                                return s;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return workbook.getSheetAt(0);
    }

    private Sheet findSurveySheet(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet s = workbook.getSheetAt(i);
            String name = s.getSheetName();
            if (name != null && name.toLowerCase().matches(".*(course\\s*end\\s*survey|survey|indirect).*")) {
                return s;
            }
        }
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet s = workbook.getSheetAt(i);
            for (Row r : s) {
                for (Cell c : r) {
                    try {
                        String text = c.getStringCellValue();
                        if (text != null) {
                            String lower = text.toLowerCase();
                            if (lower.contains("indirect method") || lower.contains("substantial") || lower.contains("slight")) {
                                return s;
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return workbook.getSheetAt(workbook.getNumberOfSheets() > 1 ? 1 : 0);
    }

    private BigDecimal extractThresholdFromSheet(Sheet sheet, FormulaEvaluator evaluator) {
        for (Row row : sheet) {
            for (Cell cell : row) {
                String str = null;
                try {
                    str = cell.getStringCellValue();
                } catch (Exception ignored) {}
                if (str != null) {
                    String lower = str.toLowerCase().trim();
                    if ((lower.contains("threshold") || lower.contains("threshhold"))
                            && !lower.contains("above") && !lower.contains("% of") && !lower.contains("fraction") && !lower.contains("reference")) {
                        int rIdx = row.getRowNum();
                        for (int rSearch = rIdx; rSearch <= Math.min(sheet.getLastRowNum(), rIdx + 1); rSearch++) {
                            Row r = sheet.getRow(rSearch);
                            if (r == null) continue;
                            for (Cell valCell : r) {
                                if (valCell == cell) continue;
                                Double numVal = getNumericCellValue(valCell, evaluator);
                                if (numVal != null && numVal > 0) {
                                    if (numVal > 0 && numVal <= 1.0) {
                                        numVal = numVal * 100.0;
                                    }
                                    BigDecimal thresh = BigDecimal.valueOf(numVal).setScale(2, RoundingMode.HALF_UP);
                                    if (thresh.compareTo(BigDecimal.ZERO) >= 0 && thresh.compareTo(new BigDecimal("100.00")) <= 0) {
                                        return thresh;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private Double getNumericCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                String str = cell.getStringCellValue().trim();
                if (str.isEmpty() || "-".equals(str) || "--".equals(str)) return null;
                try {
                    return Double.parseDouble(str);
                } catch (NumberFormatException e) {
                    return null;
                }
            case FORMULA:
                if (evaluator != null) {
                    try {
                        CellValue cellValue = evaluator.evaluate(cell);
                        if (cellValue != null) {
                            if (cellValue.getCellType() == CellType.NUMERIC) {
                                return cellValue.getNumberValue();
                            } else if (cellValue.getCellType() == CellType.STRING) {
                                String s = cellValue.getStringValue().trim();
                                if (!s.isEmpty() && !"-".equals(s)) {
                                    try { return Double.parseDouble(s); } catch (Exception ignored) {}
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
                try {
                    return cell.getNumericCellValue();
                } catch (Exception ignored) {}
                return null;
            case BOOLEAN:
                return cell.getBooleanCellValue() ? 1.0 : 0.0;
            default:
                return null;
        }
    }

    private String getStringCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                long longVal = (long) cell.getNumericCellValue();
                if (cell.getNumericCellValue() == (double) longVal) {
                    return String.valueOf(longVal);
                }
                return String.valueOf(cell.getNumericCellValue());
            case FORMULA:
                if (evaluator != null) {
                    try {
                        CellValue cellValue = evaluator.evaluate(cell);
                        if (cellValue != null) {
                            if (cellValue.getCellType() == CellType.STRING) {
                                return cellValue.getStringValue().trim();
                            } else if (cellValue.getCellType() == CellType.NUMERIC) {
                                long lv = (long) cellValue.getNumberValue();
                                if (cellValue.getNumberValue() == (double) lv) {
                                    return String.valueOf(lv);
                                }
                                return String.valueOf(cellValue.getNumberValue());
                            }
                        }
                    } catch (Exception ignored) {}
                }
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception ignored) {}
                return "";
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    // =========================================================================
    //  EXAMINATION DIRECT ATTAINMENT ENGINE
    // =========================================================================

    public ExaminationAttainmentResultDto calculateExaminationAttainment(String courseOfferingOrCourseId, ExaminationMarksPayloadDto payload) {
        System.out.println("[AttainmentCalculationService] calculateExaminationAttainment called | courseOfferingOrCourseId: " + courseOfferingOrCourseId);
        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        AttainmentConfiguration config = getAttainmentConfig(offeringId);

        if (payload == null || payload.getStudentMarks() == null || payload.getStudentMarks().isEmpty()) {
            return getExaminationAttainment(offeringId);
        }

        BigDecimal thresholdPct = payload.getThresholdPercentage() != null ? payload.getThresholdPercentage() :
                (config.getDirectThreshold() != null ? config.getDirectThreshold() : new BigDecimal("60.00"));
        Map<String, BigDecimal> coMaxMarks = payload.getCoMaxMarks() != null ? payload.getCoMaxMarks() : Collections.emptyMap();
        List<StudentMarksRowDto> studentMarks = payload.getStudentMarks();

        Map<String, BigDecimal> thresholdMarks = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : coMaxMarks.entrySet()) {
            BigDecimal max = e.getValue() != null ? e.getValue() : new BigDecimal("100.00");
            BigDecimal thresh = max.multiply(thresholdPct).divide(new BigDecimal("100.00"), 2, RoundingMode.HALF_UP);
            thresholdMarks.put(e.getKey(), thresh);
        }

        Map<String, Integer> countAbove = new LinkedHashMap<>();
        Map<String, Integer> countTotal = new LinkedHashMap<>();

        for (String coCode : coMaxMarks.keySet()) {
            countAbove.put(coCode, 0);
            countTotal.put(coCode, 0);
        }

        for (StudentMarksRowDto student : studentMarks) {
            if (student.getCoMarks() == null) continue;
            for (Map.Entry<String, BigDecimal> e : student.getCoMarks().entrySet()) {
                String coCode = e.getKey();
                BigDecimal marks = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;
                BigDecimal thresh = thresholdMarks.getOrDefault(coCode, BigDecimal.ZERO);

                countTotal.put(coCode, countTotal.getOrDefault(coCode, 0) + 1);
                if (marks.compareTo(thresh) >= 0) {
                    countAbove.put(coCode, countAbove.getOrDefault(coCode, 0) + 1);
                }
            }
        }

        Map<String, BigDecimal> percentageAbove = new LinkedHashMap<>();
        Map<String, Integer> attainmentLevels = new LinkedHashMap<>();

        List<LevelBand> directBands = parseLevelBands(config.getDirectLevelsJson(), true);

        double sumLevels = 0;
        int coCount = 0;

        for (String coCode : coMaxMarks.keySet()) {
            int total = countTotal.getOrDefault(coCode, 0);
            int above = countAbove.getOrDefault(coCode, 0);

            BigDecimal pct = total > 0
                    ? BigDecimal.valueOf(above).multiply(new BigDecimal("100.00")).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            percentageAbove.put(coCode, pct);

            int level = evaluateLevelBand(pct, directBands);
            attainmentLevels.put(coCode, level);
            sumLevels += level;
            coCount++;
        }

        BigDecimal overallDirectCoAttainment = coCount > 0
                ? BigDecimal.valueOf(sumLevels / coCount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ExaminationAttainmentResultDto result = ExaminationAttainmentResultDto.builder()
                .courseId(offeringId)
                .thresholdPercentage(thresholdPct)
                .totalStudents(studentMarks.size())
                .coMaxMarks(coMaxMarks)
                .coThresholdMarks(thresholdMarks)
                .studentMarks(studentMarks)
                .studentsAboveThreshold(countAbove)
                .percentageAboveThreshold(percentageAbove)
                .coAttainmentLevels(attainmentLevels)
                .overallDirectCoAttainment(overallDirectCoAttainment)
                .build();

        examinationAttainmentStore.put(offeringId, result);
        return result;
    }

    @Transactional
    public ExaminationAttainmentResultDto processAndSaveExaminationFile(String courseOfferingOrCourseId, MultipartFile file, BigDecimal thresholdPercentage, String uploadedBy) {
        System.out.println("[AttainmentCalculationService] processAndSaveExaminationFile called | courseOfferingOrCourseId: " + courseOfferingOrCourseId);
        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + offeringId));
        String batchId = offering.getBatchId();

        AttainmentConfiguration config = getAttainmentConfig(offeringId);

        BigDecimal threshold = thresholdPercentage;
        Map<String, BigDecimal> coMaxMarks = new LinkedHashMap<>();
        List<StudentMarksRowDto> studentList = new ArrayList<>();

        if (file != null && !file.isEmpty()) {
            try {
                String uploadDir = (baseUploadDir != null ? baseUploadDir : "uploads") + "/examination/" + offeringId;
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "exam_marks.xlsx";
                String savedFileName = System.currentTimeMillis() + "_" + originalFilename;
                File targetFile = new File(dir, savedFileName);
                file.transferTo(targetFile);

                try (InputStream is = new FileInputStream(targetFile);
                     Workbook workbook = WorkbookFactory.create(is)) {
                    FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    Sheet sheet = findExaminationSheet(workbook);

                    // 1. Threshold extraction
                    BigDecimal extractedThreshold = extractThresholdFromSheet(sheet, evaluator);
                    if (extractedThreshold != null) {
                        threshold = extractedThreshold;
                    } else if (threshold == null) {
                        threshold = config.getDirectThreshold() != null ? config.getDirectThreshold() : new BigDecimal("60.00");
                    }

                    // 2. Locate CO header row & map CO columns
                    int coHeaderRowNum = -1;
                    Map<Integer, String> coColMap = new LinkedHashMap<>();
                    for (Row row : sheet) {
                        Map<Integer, String> candidateCols = new LinkedHashMap<>();
                        for (Cell cell : row) {
                            String text = getStringCellValue(cell, evaluator);
                            if (text != null && text.matches("(?i)^CO\\s*\\d+$")) {
                                candidateCols.put(cell.getColumnIndex(), text.toUpperCase().replaceAll("\\s+", ""));
                            }
                        }
                        if (candidateCols.size() > coColMap.size()) {
                            coColMap = candidateCols;
                            coHeaderRowNum = row.getRowNum();
                        }
                    }

                    if (coColMap.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Course Outcome (CO) headers found in Examination sheet.");
                    }

                    // 3. Locate Out Of row
                    int outOfRowNum = -1;
                    for (Row row : sheet) {
                        for (Cell cell : row) {
                            String text = getStringCellValue(cell, evaluator);
                            if (text != null && text.matches("(?i)^out\\s*of.*|^max(\\.?\\s*marks)?.*|^maximum.*")) {
                                outOfRowNum = row.getRowNum();
                                break;
                            }
                        }
                        if (outOfRowNum != -1) break;
                    }

                    if (outOfRowNum != -1) {
                        Row outOfRow = sheet.getRow(outOfRowNum);
                        for (Map.Entry<Integer, String> e : coColMap.entrySet()) {
                            int colIdx = e.getKey();
                            String coCode = e.getValue();
                            Cell c = outOfRow.getCell(colIdx);
                            Double val = getNumericCellValue(c, evaluator);
                            if (val == null || val <= 0) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Invalid or missing 'Out Of' marks for " + coCode + ". Out Of marks must be greater than zero.");
                            }
                            coMaxMarks.put(coCode, BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP));
                        }
                    } else {
                        // Fallback: check row directly above CO header or default to 20.00
                        for (String coCode : coColMap.values()) {
                            coMaxMarks.put(coCode, new BigDecimal("20.00"));
                        }
                    }

                    // 4. Locate PRN and Name columns
                    int prnCol = -1;
                    int nameCol = -1;
                    for (int r = Math.max(0, coHeaderRowNum - 3); r <= coHeaderRowNum; r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;
                        for (Cell cell : row) {
                            String text = getStringCellValue(cell, evaluator);
                            if (text != null) {
                                String lower = text.toLowerCase();
                                if (lower.contains("prn") || lower.contains("roll") || lower.contains("student id") || lower.contains("reg")) {
                                    prnCol = cell.getColumnIndex();
                                } else if (lower.contains("name") || lower.contains("student name")) {
                                    nameCol = cell.getColumnIndex();
                                }
                            }
                        }
                    }
                    if (prnCol == -1) prnCol = 1;
                    if (nameCol == -1) nameCol = 2;

                    // 5. Parse student mark records
                    for (int r = coHeaderRowNum + 1; r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;

                        Cell prnCell = row.getCell(prnCol);
                        String prn = getStringCellValue(prnCell, evaluator);

                        Cell nameCell = nameCol >= 0 ? row.getCell(nameCol) : null;
                        String name = getStringCellValue(nameCell, evaluator);

                        if (prn == null || prn.isBlank()) {
                            // If PRN is in col 0
                            if (prnCol != 0) {
                                String prn0 = getStringCellValue(row.getCell(0), evaluator);
                                if (prn0 != null && !prn0.isBlank() && !prn0.matches("\\d+")) {
                                    prn = prn0;
                                }
                            }
                        }

                        if (prn == null || prn.isBlank()) continue;

                        String prnLower = prn.toLowerCase();
                        if (r == outOfRowNum || prnLower.contains("average") || prnLower.contains("total") || prnLower.contains("threshold") || prnLower.contains("count") || prnLower.contains("target") || prnLower.contains("max") || prnLower.contains("out of")) {
                            continue;
                        }

                        Map<String, BigDecimal> coMarks = new LinkedHashMap<>();
                        for (Map.Entry<Integer, String> e : coColMap.entrySet()) {
                            int colIdx = e.getKey();
                            String coCode = e.getValue();
                            BigDecimal outOf = coMaxMarks.getOrDefault(coCode, new BigDecimal("100.00"));
                            Cell markCell = row.getCell(colIdx);

                            Double numMark = getNumericCellValue(markCell, evaluator);
                            if (numMark == null) {
                                String strMark = getStringCellValue(markCell, evaluator);
                                if (strMark.isEmpty() || "-".equals(strMark) || "--".equals(strMark) || "ab".equalsIgnoreCase(strMark) || "a".equalsIgnoreCase(strMark)) {
                                    numMark = 0.0;
                                } else {
                                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "Invalid non-numeric mark '" + strMark + "' for student PRN '" + prn + "' in " + coCode);
                                }
                            }

                            BigDecimal mark = BigDecimal.valueOf(numMark).setScale(2, RoundingMode.HALF_UP);
                            if (mark.compareTo(BigDecimal.ZERO) < 0) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Invalid mark " + mark + " for student PRN '" + prn + "' in " + coCode + ". Mark must be non-negative.");
                            }
                            if (mark.compareTo(outOf) > 0) {
                                log.warn("Student PRN '{}' mark {} for {} exceeds Out Of {}. Clamping mark to max.", prn, mark, coCode, outOf);
                                mark = outOf;
                            }
                            coMarks.put(coCode, mark);
                        }

                        studentList.add(StudentMarksRowDto.builder()
                                .srNo(studentList.size() + 1)
                                .prn(prn)
                                .studentName(name != null && !name.isBlank() ? name : "Student " + (studentList.size() + 1))
                                .coMarks(coMarks)
                                .build());
                    }
                }

                if (studentList.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid student mark records found in the Examination sheet.");
                }

                // Update Course Offering's AttainmentConfiguration with authoritative uploaded threshold
                config.setDirectThreshold(threshold);
                config.setUpdatedAt(ZonedDateTime.now());
                configRepository.save(config);

                // Persist Student marks and audit document
                saveStudentCoMarksToDatabase(offeringId, coMaxMarks, studentList);

                uploadedDocumentRepository.deleteByCourseOfferingIdAndDocumentType(offeringId, DocumentType.EXAMINATION);
                UploadedDocument doc = UploadedDocument.builder()
                        .id("doc-" + UUID.randomUUID().toString().substring(0, 8))
                        .courseOfferingId(offeringId)
                        .batchId(batchId)
                        .documentType(DocumentType.EXAMINATION)
                        .fileName(originalFilename)
                        .savedFileName(savedFileName)
                        .savedPath(targetFile.getAbsolutePath())
                        .fileSize(file.getSize())
                        .recordsProcessed(studentList.size())
                        .thresholdPercentage(threshold)
                        .uploadedBy(uploadedBy != null ? uploadedBy : "Course Coordinator")
                        .uploadedAt(ZonedDateTime.now())
                        .build();
                uploadedDocumentRepository.save(doc);

            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (Exception e) {
                log.error("  [EXAM FILE PROCESS ERROR]: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to process examination file: " + e.getMessage());
            }
        }

        ExaminationMarksPayloadDto payload = ExaminationMarksPayloadDto.builder()
                .courseId(offeringId)
                .thresholdPercentage(threshold)
                .coMaxMarks(coMaxMarks)
                .studentMarks(studentList)
                .build();

        return calculateExaminationAttainment(offeringId, payload);
    }

    @Transactional(readOnly = true)
    public ExaminationAttainmentResultDto getExaminationAttainment(String courseOfferingOrCourseId) {
        System.out.println("[AttainmentCalculationService] getExaminationAttainment called | courseOfferingOrCourseId: " + courseOfferingOrCourseId);
        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        if (examinationAttainmentStore.containsKey(offeringId)) {
            return examinationAttainmentStore.get(offeringId);
        }

        List<StudentCoMark> dbMarks = studentCoMarkRepository.findByCourseOfferingId(offeringId);
        if (!dbMarks.isEmpty()) {
            Map<String, BigDecimal> coMaxMarks = new LinkedHashMap<>();
            Map<String, Map<String, BigDecimal>> studentCoMarksMap = new LinkedHashMap<>();
            Map<String, String> studentNamesMap = new LinkedHashMap<>();

            for (StudentCoMark mark : dbMarks) {
                String prn = mark.getPrn();
                String coCode = mark.getCoCode();

                if (!coMaxMarks.containsKey(coCode) && mark.getMaxMarks() != null) {
                    coMaxMarks.put(coCode, mark.getMaxMarks());
                }

                studentCoMarksMap.putIfAbsent(prn, new LinkedHashMap<>());
                studentCoMarksMap.get(prn).put(coCode, mark.getMarksObtained());
                studentNamesMap.put(prn, mark.getStudentName());
            }

            List<StudentMarksRowDto> studentList = new ArrayList<>();
            int srNo = 1;
            for (Map.Entry<String, Map<String, BigDecimal>> entry : studentCoMarksMap.entrySet()) {
                String prn = entry.getKey();
                studentList.add(StudentMarksRowDto.builder()
                        .srNo(srNo++)
                        .prn(prn)
                        .studentName(studentNamesMap.getOrDefault(prn, "Student " + srNo))
                        .coMarks(entry.getValue())
                        .build());
            }

            BigDecimal threshold = getAttainmentConfig(offeringId).getDirectThreshold();
            if (threshold == null) threshold = new BigDecimal("60.00");
            Optional<UploadedDocument> docOpt = uploadedDocumentRepository
                    .findFirstByCourseOfferingIdAndDocumentTypeOrderByUploadedAtDesc(offeringId, DocumentType.EXAMINATION);
            if (docOpt.isPresent() && docOpt.get().getThresholdPercentage() != null) {
                threshold = docOpt.get().getThresholdPercentage();
            }

            ExaminationMarksPayloadDto payload = ExaminationMarksPayloadDto.builder()
                    .courseId(offeringId)
                    .thresholdPercentage(threshold)
                    .coMaxMarks(coMaxMarks)
                    .studentMarks(studentList)
                    .build();

            return calculateExaminationAttainment(offeringId, payload);
        }

        return ExaminationAttainmentResultDto.builder()
                .courseId(offeringId)
                .thresholdPercentage(getAttainmentConfig(offeringId).getDirectThreshold() != null ? getAttainmentConfig(offeringId).getDirectThreshold() : new BigDecimal("60.00"))
                .totalStudents(0)
                .coMaxMarks(Collections.emptyMap())
                .coThresholdMarks(Collections.emptyMap())
                .studentMarks(Collections.emptyList())
                .studentsAboveThreshold(Collections.emptyMap())
                .percentageAboveThreshold(Collections.emptyMap())
                .coAttainmentLevels(Collections.emptyMap())
                .overallDirectCoAttainment(BigDecimal.ZERO)
                .build();
    }

    // =========================================================================
    //  COURSE END SURVEY INDIRECT ATTAINMENT ENGINE
    // =========================================================================

    public SurveyAttainmentResultDto calculateSurveyAttainment(String courseOfferingOrCourseId, SurveyMarksPayloadDto payload) {
        System.out.println("[AttainmentCalculationService] calculateSurveyAttainment called | courseOfferingOrCourseId: " + courseOfferingOrCourseId);
        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        AttainmentConfiguration config = getAttainmentConfig(offeringId);

        if (payload == null || payload.getSurveyResponses() == null || payload.getSurveyResponses().isEmpty()) {
            return getSurveyAttainment(offeringId);
        }

        List<SurveyResponseRowDto> responses = payload.getSurveyResponses();
        Set<String> coCodes = new LinkedHashSet<>();
        for (SurveyResponseRowDto r : responses) {
            if (r.getCoRatings() != null) coCodes.addAll(r.getCoRatings().keySet());
        }
        if (coCodes.isEmpty()) {
            coCodes = Set.of("CO1", "CO2", "CO3", "CO4", "CO5");
        }

        int totalStudents = responses.size();
        Map<String, Integer> level1Counts = new LinkedHashMap<>();
        Map<String, Integer> level2Counts = new LinkedHashMap<>();
        Map<String, Integer> level3Counts = new LinkedHashMap<>();
        Map<String, BigDecimal> level1Percentages = new LinkedHashMap<>();
        Map<String, BigDecimal> level2Percentages = new LinkedHashMap<>();
        Map<String, BigDecimal> level3Percentages = new LinkedHashMap<>();
        Map<String, BigDecimal> overallIndirectPercentages = new LinkedHashMap<>();
        Map<String, BigDecimal> indirectAttainmentScores = new LinkedHashMap<>();
        Map<String, Integer> coAttainmentLevels = new LinkedHashMap<>();

        List<LevelBand> indirectBands = parseLevelBands(config.getIndirectLevelsJson(), false);

        double sumScores = 0;
        int coCount = 0;

        for (String co : coCodes) {
            int count1 = 0;
            int count2 = 0;
            int count3 = 0;

            for (SurveyResponseRowDto r : responses) {
                if (r.getCoRatings() != null && r.getCoRatings().containsKey(co)) {
                    BigDecimal rating = r.getCoRatings().get(co);
                    if (rating != null) {
                        int rInt = (int) Math.round(rating.doubleValue());
                        if (rInt == 1) count1++;
                        else if (rInt == 2) count2++;
                        else if (rInt == 3) count3++;
                    }
                }
            }

            int validResponses = count1 + count2 + count3;
            int divisor = validResponses > 0 ? validResponses : (totalStudents > 0 ? totalStudents : 1);

            double pct1Raw = (double) count1 * 100.0 / divisor;
            double pct2Raw = (double) count2 * 100.0 / divisor;
            double pct3Raw = (double) count3 * 100.0 / divisor;

            BigDecimal pct1 = BigDecimal.valueOf(pct1Raw).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pct2 = BigDecimal.valueOf(pct2Raw).setScale(2, RoundingMode.HALF_UP);
            BigDecimal pct3 = BigDecimal.valueOf(pct3Raw).setScale(2, RoundingMode.HALF_UP);

            // Official workbook formula: overallIndirectPercentage = (pct1 * 0.33 + pct2 * 0.67 + pct3 * 1.0)
            double overallIndirectPct = (pct1Raw * 0.33) + (pct2Raw * 0.67) + (pct3Raw * 1.0);
            BigDecimal overallPct = BigDecimal.valueOf(overallIndirectPct).setScale(2, RoundingMode.HALF_UP);

            // Indirect score out of 3.00
            double scoreVal = (count1 * 1.0 + count2 * 2.0 + count3 * 3.0) / divisor;
            BigDecimal indirectScore = BigDecimal.valueOf(scoreVal).setScale(2, RoundingMode.HALF_UP);

            // Dynamic Level Band evaluation:
            int level = evaluateLevelBand(overallPct, indirectBands);

            level1Counts.put(co, count1);
            level2Counts.put(co, count2);
            level3Counts.put(co, count3);
            level1Percentages.put(co, pct1);
            level2Percentages.put(co, pct2);
            level3Percentages.put(co, pct3);
            overallIndirectPercentages.put(co, overallPct);
            indirectAttainmentScores.put(co, indirectScore);
            coAttainmentLevels.put(co, level);

            sumScores += indirectScore.doubleValue();
            coCount++;
        }

        BigDecimal overallIndirectCoAttainment = coCount > 0
                ? BigDecimal.valueOf(sumScores / coCount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        SurveyAttainmentResultDto result = SurveyAttainmentResultDto.builder()
                .courseId(offeringId)
                .totalStudents(totalStudents)
                .level1Counts(level1Counts)
                .level2Counts(level2Counts)
                .level3Counts(level3Counts)
                .level1Percentages(level1Percentages)
                .level2Percentages(level2Percentages)
                .level3Percentages(level3Percentages)
                .overallIndirectPercentages(overallIndirectPercentages)
                .indirectAttainmentScores(indirectAttainmentScores)
                .coAttainmentLevels(coAttainmentLevels)
                .overallIndirectCoAttainment(overallIndirectCoAttainment)
                .surveyResponses(responses)
                .build();

        surveyAttainmentStore.put(offeringId, result);
        return result;
    }

    @Transactional
    public SurveyAttainmentResultDto processAndSaveSurveyFile(String courseOfferingOrCourseId, MultipartFile file, BigDecimal thresholdPercentage, String uploadedBy) {
        System.out.println("[AttainmentCalculationService] processAndSaveSurveyFile called | courseOfferingOrCourseId: " + courseOfferingOrCourseId);
        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course Offering not found: " + offeringId));
        String batchId = offering.getBatchId();

        List<SurveyResponseRowDto> surveyResponses = new ArrayList<>();

        if (file != null && !file.isEmpty()) {
            try {
                String uploadDir = (baseUploadDir != null ? baseUploadDir : "uploads") + "/survey/" + offeringId;
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "survey_responses.xlsx";
                String savedFileName = System.currentTimeMillis() + "_" + originalFilename;
                File targetFile = new File(dir, savedFileName);
                file.transferTo(targetFile);

                try (InputStream is = new FileInputStream(targetFile);
                     Workbook workbook = WorkbookFactory.create(is)) {
                    FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    Sheet sheet = findSurveySheet(workbook);

                    // Find CO header row (closest to student response rows)
                    int coHeaderRowNum = -1;
                    Map<Integer, String> coColMap = new LinkedHashMap<>();
                    for (Row row : sheet) {
                        Map<Integer, String> candidateCols = new LinkedHashMap<>();
                        for (Cell cell : row) {
                            String text = getStringCellValue(cell, evaluator);
                            if (text != null && text.matches("(?i)^CO\\s*\\d+$")) {
                                candidateCols.put(cell.getColumnIndex(), text.toUpperCase().replaceAll("\\s+", ""));
                            }
                        }
                        if (candidateCols.size() >= coColMap.size() && !candidateCols.isEmpty()) {
                            coColMap = candidateCols;
                            coHeaderRowNum = row.getRowNum();
                        }
                    }

                    if (coColMap.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Course Outcome (CO) headers found in Survey sheet.");
                    }

                    // Check if any column header explicitly mentioned PRN or Name
                    int prnColIdx = -1;
                    int nameColIdx = -1;
                    if (coHeaderRowNum >= 0) {
                        Row hRow = sheet.getRow(coHeaderRowNum);
                        if (hRow != null) {
                            for (Cell cell : hRow) {
                                String hText = getStringCellValue(cell, evaluator);
                                if (hText != null) {
                                    String clean = hText.toLowerCase().replaceAll("[^a-z]", "");
                                    if (clean.contains("prn") || clean.contains("rollno") || clean.contains("studentid")) {
                                        prnColIdx = cell.getColumnIndex();
                                    } else if (clean.contains("name") || clean.contains("studentname")) {
                                        nameColIdx = cell.getColumnIndex();
                                    }
                                }
                            }
                        }
                    }

                    // Parse student response rows below CO header
                    for (int r = coHeaderRowNum + 1; r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;

                        Map<String, BigDecimal> coRatings = new LinkedHashMap<>();
                        Map<String, String> coFeedbacks = new LinkedHashMap<>();
                        boolean hasAnyRating = false;

                        for (Map.Entry<Integer, String> e : coColMap.entrySet()) {
                            int colIdx = e.getKey();
                            String coCode = e.getValue();
                            Cell cell = row.getCell(colIdx);

                            Double numVal = getNumericCellValue(cell, evaluator);
                            String strVal = getStringCellValue(cell, evaluator);

                            if (numVal != null && numVal >= 1 && numVal <= 3) {
                                int valInt = (int) Math.round(numVal);
                                coRatings.put(coCode, BigDecimal.valueOf(valInt).setScale(2, RoundingMode.HALF_UP));
                                String label = valInt == 3 ? "Substantial" : (valInt == 2 ? "Moderate" : "Slight");
                                coFeedbacks.put(coCode, label);
                                hasAnyRating = true;
                            } else if (strVal != null && !strVal.isBlank()) {
                                String trimmed = strVal.trim();
                                if ("substantial".equalsIgnoreCase(trimmed) || "3".equals(trimmed) || "3.0".equals(trimmed) || "high".equalsIgnoreCase(trimmed) || "3 - substantial".equalsIgnoreCase(trimmed)) {
                                    coRatings.put(coCode, new BigDecimal("3.00"));
                                    coFeedbacks.put(coCode, "Substantial");
                                    hasAnyRating = true;
                                } else if ("moderate".equalsIgnoreCase(trimmed) || "2".equals(trimmed) || "2.0".equals(trimmed) || "medium".equalsIgnoreCase(trimmed) || "2 - moderate".equalsIgnoreCase(trimmed)) {
                                    coRatings.put(coCode, new BigDecimal("2.00"));
                                    coFeedbacks.put(coCode, "Moderate");
                                    hasAnyRating = true;
                                } else if ("slight".equalsIgnoreCase(trimmed) || "1".equals(trimmed) || "1.0".equals(trimmed) || "low".equalsIgnoreCase(trimmed) || "1 - slight".equalsIgnoreCase(trimmed)) {
                                    coRatings.put(coCode, new BigDecimal("1.00"));
                                    coFeedbacks.put(coCode, "Slight");
                                    hasAnyRating = true;
                                } else if ("-".equals(trimmed) || "--".equals(trimmed)) {
                                    // Empty rating
                                } else {
                                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                            "Invalid survey response '" + strVal + "' for " + coCode + ". Valid values are 'Slight', 'Moderate', 'Substantial' or 1, 2, 3.");
                                }
                            }
                        }

                        if (!hasAnyRating) continue; // Skip empty rows

                        String prn = null;
                        if (prnColIdx != -1) {
                            Cell pCell = row.getCell(prnColIdx);
                            String rawPrn = getStringCellValue(pCell, evaluator);
                            if (rawPrn != null && !rawPrn.isBlank()) {
                                prn = rawPrn.trim();
                            }
                        }

                        String sName = null;
                        if (nameColIdx != -1) {
                            Cell nCell = row.getCell(nameColIdx);
                            String rawName = getStringCellValue(nCell, evaluator);
                            if (rawName != null && !rawName.isBlank()) {
                                sName = rawName.trim();
                            }
                        }

                        int srNo = surveyResponses.size() + 1;
                        surveyResponses.add(SurveyResponseRowDto.builder()
                                .srNo(srNo)
                                .prn(prn != null && !prn.isBlank() ? prn : "SRV-" + srNo)
                                .studentName(sName != null && !sName.isBlank() ? sName : "Student " + srNo)
                                .coRatings(coRatings)
                                .coFeedbacks(coFeedbacks)
                                .build());
                    }
                }

                if (surveyResponses.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid survey response rows found in the Survey sheet.");
                }

                // Validate student PRNs against Course Offering's batch if PRNs are provided
                for (SurveyResponseRowDto sr : surveyResponses) {
                    String prn = sr.getPrn();
                    if (prn != null && !prn.isBlank() && !prn.startsWith("SRV-")) {
                        Optional<Student> studentOpt = studentRepository.findByPrn(prn);
                        if (studentOpt.isEmpty()) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Validation Error: Student with PRN '" + prn + "' is not registered in the system. Survey upload rejected.");
                        }
                        Student student = studentOpt.get();
                        if (student.getBatchId() == null || !student.getBatchId().equals(batchId)) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Validation Error: Student PRN '" + prn + "' belongs to batch '" + student.getBatchId()
                                            + "', but this Course Offering belongs to batch '" + batchId + "'. Cross-batch survey upload rejected.");
                        }
                    }
                }

                uploadedDocumentRepository.deleteByCourseOfferingIdAndDocumentType(offeringId, DocumentType.SURVEY);
                UploadedDocument doc = UploadedDocument.builder()
                        .id("doc-" + UUID.randomUUID().toString().substring(0, 8))
                        .courseOfferingId(offeringId)
                        .batchId(batchId)
                        .documentType(DocumentType.SURVEY)
                        .fileName(originalFilename)
                        .savedFileName(savedFileName)
                        .savedPath(targetFile.getAbsolutePath())
                        .fileSize(file.getSize())
                        .recordsProcessed(surveyResponses.size())
                        .thresholdPercentage(thresholdPercentage)
                        .uploadedBy(uploadedBy != null ? uploadedBy : "Course Coordinator")
                        .uploadedAt(ZonedDateTime.now())
                        .build();
                uploadedDocumentRepository.save(doc);

            } catch (ResponseStatusException rse) {
                throw rse;
            } catch (Exception e) {
                log.error("  [SURVEY FILE PROCESS ERROR]: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to process survey file: " + e.getMessage());
            }
        }

        SurveyMarksPayloadDto payload = SurveyMarksPayloadDto.builder()
                .courseId(offeringId)
                .surveyResponses(surveyResponses)
                .build();

        return calculateSurveyAttainment(offeringId, payload);
    }

    @Transactional(readOnly = true)
    public SurveyAttainmentResultDto getSurveyAttainment(String courseOfferingOrCourseId) {
        System.out.println("[AttainmentCalculationService] getSurveyAttainment called | courseOfferingOrCourseId: " + courseOfferingOrCourseId);
        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        if (surveyAttainmentStore.containsKey(offeringId)) {
            return surveyAttainmentStore.get(offeringId);
        }

        return SurveyAttainmentResultDto.builder()
                .courseId(offeringId)
                .surveyResponses(Collections.emptyList())
                .indirectAttainmentScores(Collections.emptyMap())
                .overallIndirectPercentages(Collections.emptyMap())
                .coAttainmentLevels(Collections.emptyMap())
                .overallIndirectCoAttainment(BigDecimal.ZERO)
                .build();
    }

    // =========================================================================
    //  COMBINED CO ATTAINMENT CALCULATION
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> calculateCourseCoAttainment(String courseOfferingOrCourseId) {
        System.out.println("[AttainmentCalculationService] calculateCourseCoAttainment called | courseOfferingOrCourseId: " + courseOfferingOrCourseId);
        enforceOfferingOrCourseScope(courseOfferingOrCourseId);
        String offeringId = resolveOfferingId(courseOfferingOrCourseId);
        CourseOffering offering = courseOfferingRepository.findById(offeringId).orElse(null);
        String courseId = offering != null ? offering.getCourseId() : offeringId;

        AttainmentConfiguration config = getAttainmentConfig(offeringId);
        List<CourseOutcome> cos = courseOutcomeRepository.findByCourseOfferingId(offeringId);

        ExaminationAttainmentResultDto examResult = getExaminationAttainment(offeringId);
        SurveyAttainmentResultDto surveyResult = getSurveyAttainment(offeringId);

        if (cos.isEmpty()) {
            cos = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                cos.add(CourseOutcome.builder()
                        .id("co-default-" + i)
                        .courseOfferingId(offeringId)
                        .code("CO" + i)
                        .statement("Course outcome CO" + i)
                        .targetLevel(new BigDecimal("2.50"))
                        .build());
            }
        }

        List<Map<String, Object>> coResults = new ArrayList<>();
        BigDecimal sumCoAttainment = BigDecimal.ZERO;
        int countCOs = 0;

        for (int i = 0; i < cos.size(); i++) {
            CourseOutcome co = cos.get(i);
            String coCode = co.getCode();
            String statement = co.getStatement() != null ? co.getStatement() : "Course outcome " + coCode;

            BigDecimal directPct = getCoMapValue(examResult.getPercentageAboveThreshold(), coCode, i, BigDecimal.ZERO);
            Integer directLevelObj = getCoMapValue(examResult.getCoAttainmentLevels(), coCode, i, 0);
            int directLevel = directLevelObj != null ? directLevelObj : evaluateLevelBand(directPct, parseLevelBands(config.getDirectLevelsJson(), true));

            BigDecimal indirectPct = getCoMapValue(surveyResult.getOverallIndirectPercentages(), coCode, i, BigDecimal.ZERO);
            BigDecimal indirectScore = getCoMapValue(surveyResult.getIndirectAttainmentScores(), coCode, i, BigDecimal.ZERO);
            Integer indirectLevelObj = getCoMapValue(surveyResult.getCoAttainmentLevels(), coCode, i, 0);
            int indirectLevel = indirectLevelObj != null ? indirectLevelObj : evaluateLevelBand(indirectPct, parseLevelBands(config.getIndirectLevelsJson(), false));

            double directW = config.getDirectWeight() != null ? config.getDirectWeight().doubleValue() / 100.0 : 0.80;
            double indirectW = config.getIndirectWeight() != null ? config.getIndirectWeight().doubleValue() / 100.0 : 0.20;

            double combinedScore = (directLevel * directW) + (indirectLevel * indirectW);
            BigDecimal roundedAttainment = BigDecimal.valueOf(combinedScore).setScale(2, RoundingMode.HALF_UP);

            sumCoAttainment = sumCoAttainment.add(roundedAttainment);
            countCOs++;

            BigDecimal target = co.getTargetLevel() != null ? co.getTargetLevel() : new BigDecimal("2.50");
            boolean targetMet = roundedAttainment.compareTo(target) >= 0;

            Map<String, Object> coRes = new LinkedHashMap<>();
            coRes.put("coCode", coCode);
            coRes.put("statement", statement);
            coRes.put("directPct", directPct);
            coRes.put("directLevel", directLevel);
            coRes.put("directScore", BigDecimal.valueOf(directLevel).setScale(2, RoundingMode.HALF_UP));
            coRes.put("indirectPct", indirectPct);
            coRes.put("indirectLevel", indirectLevel);
            coRes.put("indirectScore", indirectScore);
            coRes.put("combinedAttainment", roundedAttainment);
            coRes.put("finalAttainment", roundedAttainment);
            coRes.put("target", target);
            coRes.put("targetMet", targetMet);

            coResults.add(coRes);
        }

        BigDecimal overallCoAttainment = countCOs > 0
                ? sumCoAttainment.divide(BigDecimal.valueOf(countCOs), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        double avgDirect = coResults.stream().mapToDouble(c -> ((Number) c.get("directLevel")).doubleValue()).average().orElse(0.0);
        double avgIndirect = coResults.stream().mapToDouble(c -> ((BigDecimal) c.get("indirectScore")).doubleValue()).average().orElse(0.0);

        BigDecimal directAttainment = BigDecimal.valueOf(avgDirect).setScale(2, RoundingMode.HALF_UP);
        BigDecimal indirectAttainment = BigDecimal.valueOf(avgIndirect).setScale(2, RoundingMode.HALF_UP);

        // Calculate Authoritative Course PO and PSO Direct Attainments
        List<String> coIds = cos.stream().map(CourseOutcome::getId).filter(Objects::nonNull).collect(Collectors.toList());
        List<CoPoMapping> poMappings = (coPoMappingRepository != null && !coIds.isEmpty())
                ? coPoMappingRepository.findByCourseOutcomeIdIn(coIds)
                : Collections.emptyList();
        Map<String, List<Integer>> poVals = new LinkedHashMap<>();
        for (CoPoMapping m : poMappings) {
            if (m.getMappingLevel() != null && m.getMappingLevel() > 0 && m.getPoCode() != null) {
                poVals.computeIfAbsent(m.getPoCode().toUpperCase(), k -> new ArrayList<>()).add(m.getMappingLevel());
            }
        }
        Map<String, BigDecimal> poAttainment = new LinkedHashMap<>();
        Map<String, BigDecimal> poAverages = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> e : poVals.entrySet()) {
            double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            poAverages.put(e.getKey(), BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            BigDecimal att = BigDecimal.valueOf((avg * overallCoAttainment.doubleValue()) / 3.0).setScale(2, RoundingMode.HALF_UP);
            poAttainment.put(e.getKey(), att);
        }

        List<CoPsoMapping> psoMappings = (coPsoMappingRepository != null && !coIds.isEmpty())
                ? coPsoMappingRepository.findByCourseOutcomeIdIn(coIds)
                : Collections.emptyList();
        Map<String, List<Integer>> psoVals = new LinkedHashMap<>();
        for (CoPsoMapping m : psoMappings) {
            if (m.getMappingLevel() != null && m.getMappingLevel() > 0 && m.getPsoCode() != null) {
                psoVals.computeIfAbsent(m.getPsoCode().toUpperCase(), k -> new ArrayList<>()).add(m.getMappingLevel());
            }
        }
        Map<String, BigDecimal> psoAttainment = new LinkedHashMap<>();
        Map<String, BigDecimal> psoAverages = new LinkedHashMap<>();
        for (Map.Entry<String, List<Integer>> e : psoVals.entrySet()) {
            double avg = e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0);
            psoAverages.put(e.getKey(), BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            BigDecimal att = BigDecimal.valueOf((avg * overallCoAttainment.doubleValue()) / 3.0).setScale(2, RoundingMode.HALF_UP);
            psoAttainment.put(e.getKey(), att);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("courseOfferingId", offeringId);
        response.put("courseId", courseId);
        response.put("config", config);
        response.put("directAttainment", directAttainment);
        response.put("indirectAttainment", indirectAttainment);
        response.put("overallCoAttainment", overallCoAttainment);
        response.put("overallCOAttainment", overallCoAttainment);
        response.put("coAttainments", coResults);
        response.put("coDetails", coResults);
        response.put("poAttainment", poAttainment);
        response.put("psoAttainment", psoAttainment);
        response.put("poDirectAttainment", poAttainment);
        response.put("psoDirectAttainment", psoAttainment);
        response.put("poAverages", poAverages);
        response.put("psoAverages", psoAverages);
        response.put("examDetails", examResult);
        response.put("surveyDetails", surveyResult);

        return response;
    }

    public List<UploadedDocument> getUploadedDocumentsForOffering(String courseOfferingId) {
        System.out.println("[AttainmentCalculationService] getUploadedDocumentsForOffering called | courseOfferingId: " + courseOfferingId);
        return uploadedDocumentRepository.findByCourseOfferingId(courseOfferingId);
    }

    public List<UploadedDocument> getUploadedDocumentsForCourse(String courseId) {
        System.out.println("[AttainmentCalculationService] getUploadedDocumentsForCourse called | courseId: " + courseId);
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseId);
        List<UploadedDocument> docs = new ArrayList<>();
        for (CourseOffering o : offerings) {
            docs.addAll(uploadedDocumentRepository.findByCourseOfferingId(o.getId()));
        }
        return docs;
    }

    // =========================================================================
    //  PROGRAMME LEVEL INDIRECT SURVEY PROCESSING
    // =========================================================================

    private Sheet findProgrammeSurveySheet(Workbook workbook) {
        int numberOfSheets = workbook.getNumberOfSheets();
        for (int i = 0; i < numberOfSheets; i++) {
            String sheetName = workbook.getSheetName(i).trim();
            if (sheetName.matches("(?i).*average\\s*attainment.*id.*|.*indirect.*|.*programme.*survey.*|.*exit.*survey.*|.*po.*pso.*survey.*")) {
                return workbook.getSheetAt(i);
            }
        }
        // Content inspection fallback
        for (int i = 0; i < numberOfSheets; i++) {
            Sheet s = workbook.getSheetAt(i);
            for (Row r : s) {
                for (Cell c : r) {
                    String text = getStringCellValue(c, null);
                    if (text != null && text.matches("(?i)^PO\\s*1$|^PSO\\s*1$")) {
                        return s;
                    }
                }
            }
        }
        return workbook.getSheetAt(0);
    }

    @Transactional
    public ProgrammeSurveyResultDto processAndSaveProgrammeSurveyFile(String programmeId, String batchId, MultipartFile file, String uploadedBy) {
        System.out.println("[AttainmentCalculationService] processAndSaveProgrammeSurveyFile called | programmeId: " + programmeId + " | batchId: " + batchId);
        String key = programmeId + "::" + batchId;

        // Load authoritative configured POs and PSOs for the programme
        List<ProgrammeOutcome> pos = programmeOutcomeRepository.findByProgrammeIdOrderByCodeAsc(programmeId);
        if (pos == null || pos.isEmpty()) {
            List<ProgrammeOutcome> defaultPos = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                defaultPos.add(ProgrammeOutcome.builder()
                        .id("po-def-" + i)
                        .programmeId(programmeId)
                        .code("PO" + i)
                        .statement("Program Outcome " + i)
                        .build());
            }
            pos = defaultPos;
        }
        List<ProgrammeSpecificOutcome> psos = programmeSpecificOutcomeRepository.findByProgrammeIdOrderByCodeAsc(programmeId);
        if (psos == null || psos.isEmpty()) {
            List<ProgrammeSpecificOutcome> defaultPsos = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                defaultPsos.add(ProgrammeSpecificOutcome.builder()
                        .id("pso-def-" + i)
                        .programmeId(programmeId)
                        .code("PSO" + i)
                        .statement("Program Specific Outcome " + i)
                        .build());
            }
            psos = defaultPsos;
        }

        Set<String> configuredPOCodes = pos.stream()
                .map(p -> p.getCode().toUpperCase().replaceAll("\\s+", ""))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> configuredPSOCodes = psos.stream()
                .map(p -> p.getCode().toUpperCase().replaceAll("\\s+", ""))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<ProgrammeSurveyResultDto.OutcomeIndirectItem> poItems = new ArrayList<>();
        List<ProgrammeSurveyResultDto.OutcomeIndirectItem> psoItems = new ArrayList<>();
        int rowsProcessed = 0;

        if (file != null && !file.isEmpty()) {
            File targetFile = null;
            try {
                String uploadDir = (baseUploadDir != null ? baseUploadDir : "uploads") + "/programme_survey/" + programmeId + "/" + batchId;
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "programme_survey.xlsx";
                String savedFileName = System.currentTimeMillis() + "_" + originalFileName;
                targetFile = new File(dir, savedFileName);
                file.transferTo(targetFile);

                try (InputStream is = new FileInputStream(targetFile);
                     Workbook workbook = WorkbookFactory.create(is)) {
                    FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                    Sheet sheet = findProgrammeSurveySheet(workbook);

                    // 1. Locate PO/PSO header row
                    int headerRowNum = -1;
                    Map<Integer, String> poColMap = new LinkedHashMap<>();
                    Map<Integer, String> psoColMap = new LinkedHashMap<>();
                    Set<String> duplicateCodes = new LinkedHashSet<>();
                    Set<String> unknownCodes = new LinkedHashSet<>();

                    for (Row row : sheet) {
                        Map<Integer, String> rowPoCols = new LinkedHashMap<>();
                        Map<Integer, String> rowPsoCols = new LinkedHashMap<>();
                        Set<String> rowSeen = new HashSet<>();
                        Set<String> rowDups = new LinkedHashSet<>();
                        Set<String> rowUnk = new LinkedHashSet<>();

                        for (Cell cell : row) {
                            String text = getStringCellValue(cell, evaluator);
                            if (text == null || text.isBlank()) continue;
                            String clean = text.trim().toUpperCase().replaceAll("\\s+", "");

                            if (clean.matches("^PO\\d+$")) {
                                if (!rowSeen.add(clean)) rowDups.add(clean);
                                if (!configuredPOCodes.contains(clean)) rowUnk.add(clean);
                                rowPoCols.put(cell.getColumnIndex(), clean);
                            } else if (clean.matches("^PSO\\d+$")) {
                                if (!rowSeen.add(clean)) rowDups.add(clean);
                                if (!configuredPSOCodes.contains(clean)) rowUnk.add(clean);
                                rowPsoCols.put(cell.getColumnIndex(), clean);
                            } else if (clean.matches("^(PO|PSO).*") && !clean.contains("AVERAGE") && !clean.contains("ATTAINMENT") && !clean.contains("DIRECT") && !clean.contains("INDIRECT") && !clean.contains("TARGET") && !clean.contains("MAPPING") && !clean.contains("FRAMEWORK")) {
                                rowUnk.add(clean);
                            }
                        }

                        if ((rowPoCols.size() + rowPsoCols.size()) > (poColMap.size() + psoColMap.size())) {
                            poColMap = rowPoCols;
                            psoColMap = rowPsoCols;
                            duplicateCodes = rowDups;
                            unknownCodes = rowUnk;
                            headerRowNum = row.getRowNum();
                        }
                    }

                    if (headerRowNum == -1 || (poColMap.isEmpty() && psoColMap.isEmpty())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Program Outcome (PO/PSO) headers found in Programme End Survey sheet.");
                    }

                    // 2. Strict Exact Outcome Validation Rules
                    // Duplicate Check (A6)
                    if (!duplicateCodes.isEmpty()) {
                        String dup = duplicateCodes.iterator().next();
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programme End Survey contains duplicate outcome " + dup + ".");
                    }

                    // Extra / Unknown Outcome Check (A4 & A5)
                    if (!unknownCodes.isEmpty()) {
                        String unk = unknownCodes.iterator().next();
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programme End Survey contains unconfigured outcome " + unk + ".");
                    }

                    // Missing PO Check (A3 & A7)
                    for (String cfgPo : configuredPOCodes) {
                        if (!poColMap.containsValue(cfgPo)) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programme End Survey is missing configured outcome " + cfgPo + ".");
                        }
                    }

                    // Missing PSO Check (A3 & A7)
                    for (String cfgPso : configuredPSOCodes) {
                        if (!psoColMap.containsValue(cfgPso)) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Programme End Survey is missing configured outcome " + cfgPso + ".");
                        }
                    }

                    // 3. Dynamic Student Response Rows Processing (A9)
                    Map<String, List<Double>> poRatingsMap = new LinkedHashMap<>();
                    Map<String, List<Double>> psoRatingsMap = new LinkedHashMap<>();
                    for (String po : configuredPOCodes) poRatingsMap.put(po, new ArrayList<>());
                    for (String pso : configuredPSOCodes) psoRatingsMap.put(pso, new ArrayList<>());

                    for (int r = headerRowNum + 1; r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;

                        boolean hasData = false;
                        Map<String, Double> rowPoVals = new HashMap<>();
                        Map<String, Double> rowPsoVals = new HashMap<>();

                        for (Map.Entry<Integer, String> entry : poColMap.entrySet()) {
                            Cell c = row.getCell(entry.getKey());
                            Double num = getNumericCellValue(c, evaluator);
                            if (num == null) {
                                String s = getStringCellValue(c, evaluator);
                                if (s != null && !s.isBlank()) {
                                    String tr = s.trim().toLowerCase();
                                    if (tr.contains("substantial") || tr.equals("3") || tr.equals("3.0") || tr.equals("high")) num = 3.0;
                                    else if (tr.contains("moderate") || tr.equals("2") || tr.equals("2.0") || tr.equals("medium")) num = 2.0;
                                    else if (tr.contains("slight") || tr.equals("1") || tr.equals("1.0") || tr.equals("low")) num = 1.0;
                                }
                            }
                            if (num != null && num >= 0) {
                                if (num > 3.0 && num <= 100.0) {
                                    num = (num / 100.0) * 3.0;
                                }
                                rowPoVals.put(entry.getValue(), Math.min(3.0, num));
                                hasData = true;
                            }
                        }

                        for (Map.Entry<Integer, String> entry : psoColMap.entrySet()) {
                            Cell c = row.getCell(entry.getKey());
                            Double num = getNumericCellValue(c, evaluator);
                            if (num == null) {
                                String s = getStringCellValue(c, evaluator);
                                if (s != null && !s.isBlank()) {
                                    String tr = s.trim().toLowerCase();
                                    if (tr.contains("substantial") || tr.equals("3") || tr.equals("3.0") || tr.equals("high")) num = 3.0;
                                    else if (tr.contains("moderate") || tr.equals("2") || tr.equals("2.0") || tr.equals("medium")) num = 2.0;
                                    else if (tr.contains("slight") || tr.equals("1") || tr.equals("1.0") || tr.equals("low")) num = 1.0;
                                }
                            }
                            if (num != null && num >= 0) {
                                if (num > 3.0 && num <= 100.0) {
                                    num = (num / 100.0) * 3.0;
                                }
                                rowPsoVals.put(entry.getValue(), Math.min(3.0, num));
                                hasData = true;
                            }
                        }

                        if (hasData) {
                            rowsProcessed++;
                            for (Map.Entry<String, Double> e : rowPoVals.entrySet()) {
                                poRatingsMap.get(e.getKey()).add(e.getValue());
                            }
                            for (Map.Entry<String, Double> e : rowPsoVals.entrySet()) {
                                psoRatingsMap.get(e.getKey()).add(e.getValue());
                            }
                        }
                    }

                    if (rowsProcessed == 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No valid survey response rows found in the Programme End Survey sheet.");
                    }

                    // Compute average indirect attainment for each PO & PSO
                    for (String poCode : configuredPOCodes) {
                        List<Double> ratings = poRatingsMap.get(poCode);
                        BigDecimal avgVal;
                        if (ratings != null && !ratings.isEmpty()) {
                            double avg = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                            avgVal = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
                        } else {
                            avgVal = BigDecimal.ZERO;
                        }
                        poItems.add(ProgrammeSurveyResultDto.OutcomeIndirectItem.builder()
                                .outcomeCode(poCode)
                                .indirectAttainment(avgVal)
                                .build());
                    }

                    for (String psoCode : configuredPSOCodes) {
                        List<Double> ratings = psoRatingsMap.get(psoCode);
                        BigDecimal avgVal;
                        if (ratings != null && !ratings.isEmpty()) {
                            double avg = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                            avgVal = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
                        } else {
                            avgVal = BigDecimal.ZERO;
                        }
                        psoItems.add(ProgrammeSurveyResultDto.OutcomeIndirectItem.builder()
                                .outcomeCode(psoCode)
                                .indirectAttainment(avgVal)
                                .build());
                    }

                    // 4. Persist uploaded document record only after successful validation
                    UploadedDocument doc = UploadedDocument.builder()
                            .id("doc-" + UUID.randomUUID().toString().substring(0, 8))
                            .batchId(batchId)
                            .documentType(DocumentType.SURVEY)
                            .fileName(originalFileName)
                            .savedFileName(savedFileName)
                            .savedPath(targetFile.getAbsolutePath())
                            .fileSize(file.getSize())
                            .recordsProcessed(rowsProcessed)
                            .uploadedBy(uploadedBy != null ? uploadedBy : "Programme Coordinator")
                            .uploadedAt(ZonedDateTime.now())
                            .build();
                    uploadedDocumentRepository.save(doc);
                }
            } catch (ResponseStatusException rse) {
                if (targetFile != null && targetFile.exists()) targetFile.delete();
                throw rse;
            } catch (Exception e) {
                if (targetFile != null && targetFile.exists()) targetFile.delete();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse Programme End Survey file: " + e.getMessage());
            }
        } else {
            // When no survey file is uploaded yet, initialize zero attainment for configured outcomes
            for (String poCode : configuredPOCodes) {
                poItems.add(ProgrammeSurveyResultDto.OutcomeIndirectItem.builder().outcomeCode(poCode).indirectAttainment(BigDecimal.ZERO).build());
            }
            for (String psoCode : configuredPSOCodes) {
                psoItems.add(ProgrammeSurveyResultDto.OutcomeIndirectItem.builder().outcomeCode(psoCode).indirectAttainment(BigDecimal.ZERO).build());
            }
        }

        ProgrammeSurveyResultDto result = ProgrammeSurveyResultDto.builder()
                .uploadId("psurvey-" + UUID.randomUUID().toString().substring(0, 8))
                .programmeId(programmeId)
                .batchId(batchId)
                .surveyType("PROGRAMME_INDIRECT")
                .recordsProcessed(rowsProcessed)
                .poIndirectAttainment(poItems)
                .psoIndirectAttainment(psoItems)
                .status("PROCESSED")
                .build();

        programmeSurveyStore.put(key, result);
        return result;
    }

    // =========================================================================
    //  PROGRAMME ATTAINMENT AGGREGATION ENGINE (BATCH-CENTRIC)
    // =========================================================================

    @Transactional(readOnly = true)
    public ProgrammeAttainmentResultDto calculateProgrammeAttainment(String programmeId, String batchId) {
        System.out.println("[AttainmentCalculationService] calculateProgrammeAttainment called | programmeId: " + programmeId + " | batchId: " + batchId);
        Programme prog = programmeRepository.findById(programmeId).orElse(null);
        Batch batch = batchRepository.findById(batchId).orElse(null);

        List<CourseOffering> offerings = courseOfferingRepository.findByBatchId(batchId);
        List<ProgrammeOutcome> pos = programmeOutcomeRepository.findByProgrammeIdOrderByCodeAsc(programmeId);
        if (pos == null || pos.isEmpty()) {
            List<ProgrammeOutcome> defaultPos = new ArrayList<>();
            for (int i = 1; i <= 12; i++) {
                defaultPos.add(ProgrammeOutcome.builder()
                        .id("po-def-" + i)
                        .programmeId(programmeId)
                        .code("PO" + i)
                        .statement("Program Outcome " + i)
                        .build());
            }
            pos = defaultPos;
        }
        List<ProgrammeSpecificOutcome> psos = programmeSpecificOutcomeRepository.findByProgrammeIdOrderByCodeAsc(programmeId);
        if (psos == null || psos.isEmpty()) {
            List<ProgrammeSpecificOutcome> defaultPsos = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                defaultPsos.add(ProgrammeSpecificOutcome.builder()
                        .id("pso-def-" + i)
                        .programmeId(programmeId)
                        .code("PSO" + i)
                        .statement("Program Specific Outcome " + i)
                        .build());
            }
            psos = defaultPsos;
        }

        List<ProgrammeTarget> targets = programmeTargetRepository.findByBatchId(batchId);
        if (targets.isEmpty()) {
            List<Batch> progBatches = batchRepository.findByProgrammeId(programmeId);
            List<String> bIds = progBatches.stream().map(Batch::getId).collect(Collectors.toList());
            if (!bIds.isEmpty()) {
                targets = programmeTargetRepository.findByBatchIdIn(bIds);
            }
        }

        Map<String, BigDecimal> targetMap = new HashMap<>();
        for (ProgrammeTarget pt : targets) {
            if (pt.getOutcomeCode() != null && pt.getTargetValue() != null) {
                targetMap.put(pt.getOutcomeCode().toUpperCase(), pt.getTargetValue());
            }
        }

        Map<Integer, List<CourseOffering>> semOfferings = new TreeMap<>();
        for (CourseOffering o : offerings) {
            int sem = o.getSemester() != null ? o.getSemester() : 1;
            semOfferings.computeIfAbsent(sem, k -> new ArrayList<>()).add(o);
        }

        List<ProgrammeAttainmentResultDto.OutcomeMappingItem> poMappingBreakdown = new ArrayList<>();
        List<ProgrammeAttainmentResultDto.OutcomeDirectItem> poDirectBreakdown = new ArrayList<>();

        for (int i = 0; i < pos.size(); i++) {
            String poCode = pos.get(i).getCode();
            List<ProgrammeAttainmentResultDto.SemesterValue> semMapValues = new ArrayList<>();
            List<ProgrammeAttainmentResultDto.SemesterValue> semDirectValues = new ArrayList<>();
            double totalMap = 0;
            double totalDirect = 0;
            int semCountWithData = 0;

            for (int s = 1; s <= 8; s++) {
                List<CourseOffering> sOfferings = semOfferings.getOrDefault(s, Collections.emptyList());
                double semMapSum = 0;
                int semMapCount = 0;
                double semDirectSum = 0;
                int semDirectCount = 0;

                for (CourseOffering off : sOfferings) {
                    List<CourseOutcome> cos = courseOutcomeRepository.findByCourseOfferingId(off.getId());
                    for (CourseOutcome co : cos) {
                        if (coPoMappingRepository != null) {
                            List<CoPoMapping> mappings = coPoMappingRepository.findByCourseOutcomeId(co.getId());
                            for (CoPoMapping m : mappings) {
                                if (poCode.equalsIgnoreCase(m.getPoCode()) && m.getMappingLevel() != null && m.getMappingLevel() > 0) {
                                    semMapSum += m.getMappingLevel();
                                    semMapCount++;
                                }
                            }
                        }
                    }
                    try {
                        Map<String, Object> cAtt = calculateCourseCoAttainment(off.getId());
                        if (cAtt != null && cAtt.get("directAttainment") instanceof BigDecimal bDirect && bDirect.compareTo(BigDecimal.ZERO) > 0) {
                            semDirectSum += bDirect.doubleValue();
                            semDirectCount++;
                        }
                    } catch (Exception ignored) {}
                }

                BigDecimal mapVal = semMapCount > 0 ? BigDecimal.valueOf(semMapSum / semMapCount).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                BigDecimal directVal = semDirectCount > 0 ? BigDecimal.valueOf(semDirectSum / semDirectCount).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                semMapValues.add(ProgrammeAttainmentResultDto.SemesterValue.builder().semester(s).averageMapping(mapVal).averageAttainment(directVal).build());
                semDirectValues.add(ProgrammeAttainmentResultDto.SemesterValue.builder().semester(s).averageMapping(mapVal).averageAttainment(directVal).build());

                if (mapVal.compareTo(BigDecimal.ZERO) > 0 || directVal.compareTo(BigDecimal.ZERO) > 0) {
                    totalMap += mapVal.doubleValue();
                    totalDirect += directVal.doubleValue();
                    semCountWithData++;
                }
            }

            BigDecimal avgMap = semCountWithData > 0 ? BigDecimal.valueOf(totalMap / semCountWithData).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal avgDirect = semCountWithData > 0 ? BigDecimal.valueOf(totalDirect / semCountWithData).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            poMappingBreakdown.add(ProgrammeAttainmentResultDto.OutcomeMappingItem.builder()
                    .poCode(poCode)
                    .semesterValues(semMapValues)
                    .overallAverage(avgMap)
                    .build());

            poDirectBreakdown.add(ProgrammeAttainmentResultDto.OutcomeDirectItem.builder()
                    .poCode(poCode)
                    .semesterValues(semDirectValues)
                    .overallAverage(avgDirect)
                    .build());
        }

        List<ProgrammeAttainmentResultDto.OutcomeMappingItem> psoMappingBreakdown = new ArrayList<>();
        List<ProgrammeAttainmentResultDto.OutcomeDirectItem> psoDirectBreakdown = new ArrayList<>();

        for (int i = 0; i < psos.size(); i++) {
            String psoCode = psos.get(i).getCode();
            List<ProgrammeAttainmentResultDto.SemesterValue> semMapValues = new ArrayList<>();
            List<ProgrammeAttainmentResultDto.SemesterValue> semDirectValues = new ArrayList<>();
            double totalMap = 0;
            double totalDirect = 0;
            int semCountWithData = 0;

            for (int s = 1; s <= 8; s++) {
                List<CourseOffering> sOfferings = semOfferings.getOrDefault(s, Collections.emptyList());
                double semMapSum = 0;
                int semMapCount = 0;
                double semDirectSum = 0;
                int semDirectCount = 0;

                for (CourseOffering off : sOfferings) {
                    List<CourseOutcome> cos = courseOutcomeRepository.findByCourseOfferingId(off.getId());
                    for (CourseOutcome co : cos) {
                        if (coPsoMappingRepository != null) {
                            List<CoPsoMapping> mappings = coPsoMappingRepository.findByCourseOutcomeId(co.getId());
                            for (CoPsoMapping m : mappings) {
                                if (psoCode.equalsIgnoreCase(m.getPsoCode()) && m.getMappingLevel() != null && m.getMappingLevel() > 0) {
                                    semMapSum += m.getMappingLevel();
                                    semMapCount++;
                                }
                            }
                        }
                    }
                    try {
                        Map<String, Object> cAtt = calculateCourseCoAttainment(off.getId());
                        if (cAtt != null && cAtt.get("directAttainment") instanceof BigDecimal bDirect && bDirect.compareTo(BigDecimal.ZERO) > 0) {
                            semDirectSum += bDirect.doubleValue();
                            semDirectCount++;
                        }
                    } catch (Exception ignored) {}
                }

                BigDecimal mapVal = semMapCount > 0 ? BigDecimal.valueOf(semMapSum / semMapCount).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                BigDecimal directVal = semDirectCount > 0 ? BigDecimal.valueOf(semDirectSum / semDirectCount).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                semMapValues.add(ProgrammeAttainmentResultDto.SemesterValue.builder().semester(s).averageMapping(mapVal).averageAttainment(directVal).build());
                semDirectValues.add(ProgrammeAttainmentResultDto.SemesterValue.builder().semester(s).averageMapping(mapVal).averageAttainment(directVal).build());

                if (mapVal.compareTo(BigDecimal.ZERO) > 0 || directVal.compareTo(BigDecimal.ZERO) > 0) {
                    totalMap += mapVal.doubleValue();
                    totalDirect += directVal.doubleValue();
                    semCountWithData++;
                }
            }

            BigDecimal avgMap = semCountWithData > 0 ? BigDecimal.valueOf(totalMap / semCountWithData).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal avgDirect = semCountWithData > 0 ? BigDecimal.valueOf(totalDirect / semCountWithData).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            psoMappingBreakdown.add(ProgrammeAttainmentResultDto.OutcomeMappingItem.builder()
                    .psoCode(psoCode)
                    .semesterValues(semMapValues)
                    .overallAverage(avgMap)
                    .build());

            psoDirectBreakdown.add(ProgrammeAttainmentResultDto.OutcomeDirectItem.builder()
                    .psoCode(psoCode)
                    .semesterValues(semDirectValues)
                    .overallAverage(avgDirect)
                    .build());
        }

        String key = programmeId + "::" + batchId;
        ProgrammeSurveyResultDto exitSurvey = programmeSurveyStore.containsKey(key)
                ? programmeSurveyStore.get(key)
                : processAndSaveProgrammeSurveyFile(programmeId, batchId, null, null);

        Map<String, BigDecimal> exitSurveyPoMap = new HashMap<>();
        if (exitSurvey.getPoIndirectAttainment() != null) {
            for (ProgrammeSurveyResultDto.OutcomeIndirectItem it : exitSurvey.getPoIndirectAttainment()) {
                exitSurveyPoMap.put(it.getOutcomeCode().toUpperCase(), it.getIndirectAttainment());
            }
        }

        Map<String, BigDecimal> exitSurveyPsoMap = new HashMap<>();
        if (exitSurvey.getPsoIndirectAttainment() != null) {
            for (ProgrammeSurveyResultDto.OutcomeIndirectItem it : exitSurvey.getPsoIndirectAttainment()) {
                exitSurveyPsoMap.put(it.getOutcomeCode().toUpperCase(), it.getIndirectAttainment());
            }
        }

        List<ProgrammeAttainmentResultDto.OutcomeAttainmentItem> poOverallList = new ArrayList<>();
        for (ProgrammeAttainmentResultDto.OutcomeDirectItem d : poDirectBreakdown) {
            String code = d.getPoCode();
            BigDecimal direct = d.getOverallAverage();
            BigDecimal indirect = exitSurveyPoMap.getOrDefault(code.toUpperCase(), BigDecimal.ZERO);

            double overallScore = (direct.doubleValue() * 0.80) + (indirect.doubleValue() * 0.20);
            BigDecimal overall = BigDecimal.valueOf(overallScore).setScale(2, RoundingMode.HALF_UP);

            BigDecimal target = targetMap.getOrDefault(code.toUpperCase(), new BigDecimal("2.50"));
            BigDecimal pct = target.compareTo(BigDecimal.ZERO) > 0
                    ? overall.divide(target, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            boolean achieved = overall.compareTo(target) >= 0;
            String obs = String.format("%s%% Target %s", pct, achieved ? "Achieved" : "Not Achieved");

            List<String> actions = Collections.emptyList();

            poOverallList.add(ProgrammeAttainmentResultDto.OutcomeAttainmentItem.builder()
                    .poCode(code)
                    .outcomeCode(code)
                    .outcomeStatement(code + " - Professional Engineering Competency")
                    .directAttainment(direct)
                    .indirectAttainment(indirect)
                    .overallAttainment(overall)
                    .target(target)
                    .achievementPercentage(pct)
                    .observation(obs)
                    .actions(actions)
                    .build());
        }

        List<ProgrammeAttainmentResultDto.OutcomeAttainmentItem> psoOverallList = new ArrayList<>();
        for (ProgrammeAttainmentResultDto.OutcomeDirectItem d : psoDirectBreakdown) {
            String code = d.getPsoCode();
            BigDecimal direct = d.getOverallAverage();
            BigDecimal indirect = exitSurveyPsoMap.getOrDefault(code.toUpperCase(), BigDecimal.ZERO);

            double overallScore = (direct.doubleValue() * 0.80) + (indirect.doubleValue() * 0.20);
            BigDecimal overall = BigDecimal.valueOf(overallScore).setScale(2, RoundingMode.HALF_UP);

            BigDecimal target = targetMap.getOrDefault(code.toUpperCase(), new BigDecimal("2.50"));
            BigDecimal pct = target.compareTo(BigDecimal.ZERO) > 0
                    ? overall.divide(target, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            boolean achieved = overall.compareTo(target) >= 0;
            String obs = String.format("%s%% Target %s", pct, achieved ? "Achieved" : "Not Achieved");

            List<String> actions = Collections.emptyList();

            psoOverallList.add(ProgrammeAttainmentResultDto.OutcomeAttainmentItem.builder()
                    .psoCode(code)
                    .outcomeCode(code)
                    .outcomeStatement(code + " - Programme Specific Domain Competency")
                    .directAttainment(direct)
                    .indirectAttainment(indirect)
                    .overallAttainment(overall)
                    .target(target)
                    .achievementPercentage(pct)
                    .observation(obs)
                    .actions(actions)
                    .build());
        }

        Map<String, BigDecimal> indirectMap = new LinkedHashMap<>();
        exitSurveyPoMap.forEach(indirectMap::put);
        exitSurveyPsoMap.forEach(indirectMap::put);

        return ProgrammeAttainmentResultDto.builder()
                .programme(prog != null ? ProgrammeAttainmentResultDto.ProgrammeSummary.builder().id(prog.getId()).code(prog.getCode()).name(prog.getName()).build() : null)
                .batch(batch != null ? ProgrammeAttainmentResultDto.BatchSummary.builder().id(batch.getId()).name(batch.getName()).startYear(batch.getStartYear() != null ? String.valueOf(batch.getStartYear()) : "").endYear(batch.getEndYear() != null ? String.valueOf(batch.getEndYear()) : "").build() : null)
                .summary(ProgrammeAttainmentResultDto.Summary.builder().courseOfferingCount(offerings.size()).semesterCount(semOfferings.size()).build())
                .averageMapping(ProgrammeAttainmentResultDto.MappingBreakdown.builder().pos(poMappingBreakdown).psos(psoMappingBreakdown).build())
                .averageDirectAttainment(ProgrammeAttainmentResultDto.DirectAttainmentBreakdown.builder().pos(poDirectBreakdown).psos(psoDirectBreakdown).build())
                .averageIndirectAttainment(indirectMap)
                .overallAttainment(ProgrammeAttainmentResultDto.OverallAttainmentBreakdown.builder().pos(poOverallList).psos(psoOverallList).build())
                .build();
    }

    public ProgrammeAttainmentDatasetDto getProgrammeAttainmentDataset(String programmeId, String batchId) {
        System.out.println("[AttainmentCalculationService] getProgrammeAttainmentDataset called | programmeId: " + programmeId + " | batchId: " + batchId);
        ProgrammeAttainmentResultDto res = calculateProgrammeAttainment(programmeId, batchId);

        List<String> columns = List.of("Outcome", "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6", "Sem 7", "Sem 8", "Average");

        List<Map<String, Object>> mapRows = new ArrayList<>();
        if (res.getAverageMapping() != null && res.getAverageMapping().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeMappingItem it : res.getAverageMapping().getPos()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("outcome", it.getPoCode());
                if (it.getSemesterValues() != null) {
                    for (ProgrammeAttainmentResultDto.SemesterValue sv : it.getSemesterValues()) {
                        r.put("sem" + sv.getSemester(), sv.getAverageMapping());
                    }
                }
                r.put("average", it.getOverallAverage());
                mapRows.add(r);
            }
        }
        if (res.getAverageMapping() != null && res.getAverageMapping().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeMappingItem it : res.getAverageMapping().getPsos()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("outcome", it.getPsoCode());
                if (it.getSemesterValues() != null) {
                    for (ProgrammeAttainmentResultDto.SemesterValue sv : it.getSemesterValues()) {
                        r.put("sem" + sv.getSemester(), sv.getAverageMapping());
                    }
                }
                r.put("average", it.getOverallAverage());
                mapRows.add(r);
            }
        }

        List<Map<String, Object>> dirRows = new ArrayList<>();
        if (res.getAverageDirectAttainment() != null && res.getAverageDirectAttainment().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeDirectItem it : res.getAverageDirectAttainment().getPos()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("outcome", it.getPoCode());
                if (it.getSemesterValues() != null) {
                    for (ProgrammeAttainmentResultDto.SemesterValue sv : it.getSemesterValues()) {
                        r.put("sem" + sv.getSemester(), sv.getAverageAttainment());
                    }
                }
                r.put("average", it.getOverallAverage());
                dirRows.add(r);
            }
        }
        if (res.getAverageDirectAttainment() != null && res.getAverageDirectAttainment().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeDirectItem it : res.getAverageDirectAttainment().getPsos()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("outcome", it.getPsoCode());
                if (it.getSemesterValues() != null) {
                    for (ProgrammeAttainmentResultDto.SemesterValue sv : it.getSemesterValues()) {
                        r.put("sem" + sv.getSemester(), sv.getAverageAttainment());
                    }
                }
                r.put("average", it.getOverallAverage());
                dirRows.add(r);
            }
        }

        Map<String, BigDecimal> overallMap = new LinkedHashMap<>();
        if (res.getOverallAttainment() != null && res.getOverallAttainment().getPos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : res.getOverallAttainment().getPos()) {
                overallMap.put(it.getOutcomeCode(), it.getOverallAttainment());
            }
        }
        if (res.getOverallAttainment() != null && res.getOverallAttainment().getPsos() != null) {
            for (ProgrammeAttainmentResultDto.OutcomeAttainmentItem it : res.getOverallAttainment().getPsos()) {
                overallMap.put(it.getOutcomeCode(), it.getOverallAttainment());
            }
        }

        return ProgrammeAttainmentDatasetDto.builder()
                .programmeId(programmeId)
                .batchId(batchId)
                .averageMapping(ProgrammeAttainmentDatasetDto.TableData.builder().columns(columns).rows(mapRows).build())
                .averageDirectAttainment(ProgrammeAttainmentDatasetDto.TableData.builder().columns(columns).rows(dirRows).build())
                .averageIndirectAttainment(res.getAverageIndirectAttainment())
                .overallAttainment(overallMap)
                .build();
    }


    private <T> T getCoMapValue(Map<String, T> map, String key, int index, T fallback) {
        if (map == null || map.isEmpty()) return fallback;
        if (map.containsKey(key)) return map.get(key);
        for (Map.Entry<String, T> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue();
        }
        List<T> values = new ArrayList<>(map.values());
        if (index < values.size()) return values.get(index);
        return fallback;
    }
}
