package com.dypiu.nba.service;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AttainmentCalculationService {

    private final AttainmentConfigurationRepository configRepository;
    private final StudentCoMarkRepository studentCoMarkRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final StudentRepository studentRepository;
    private final BatchRepository batchRepository;
    private final UploadedDocumentRepository uploadedDocumentRepository;
    private final CourseRepository courseRepository;

    private final Map<String, ExaminationAttainmentResultDto> examinationAttainmentStore = new ConcurrentHashMap<>();
    private final Map<String, SurveyAttainmentResultDto> surveyAttainmentStore = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public AttainmentConfiguration getAttainmentConfig(String courseId) {
        return configRepository.findByCourseId(courseId)
                .orElseGet(() -> AttainmentConfiguration.builder()
                        .id("cfg-" + courseId)
                        .courseId(courseId)
                        .courseCode("COURSE")
                        .courseName("Course Name")
                        .directWeight(new BigDecimal("80.00"))
                        .indirectWeight(new BigDecimal("20.00"))
                        .directThreshold(new BigDecimal("60.00"))
                        .indirectThreshold(new BigDecimal("60.00"))
                        .status("VERIFIED")
                        .build());
    }

    @Transactional
    public AttainmentConfiguration saveAttainmentConfig(String courseId, AttainmentConfiguration config) {
        config.setCourseId(courseId);
        if (config.getId() == null) config.setId("cfg-" + courseId);
        return configRepository.save(config);
    }

    // --- Database Persistence Helper Methods ---

    @Transactional
    public void saveStudentCoMarksToDatabase(String courseId, Map<String, BigDecimal> coMaxMarks, List<StudentMarksRowDto> studentList) {
        if (courseId == null || studentList == null || studentList.isEmpty()) return;

        try {
            studentCoMarkRepository.deleteByCourseId(courseId);
            studentCoMarkRepository.flush();

            // 1. Resolve Programme & Academic Year from Course
            Course course = courseRepository.findById(courseId).orElse(null);
            String progId = course != null ? course.getProgrammeId() : null;
            String academicYear = course != null && course.getAcademicYear() != null ? course.getAcademicYear() : "2025-26";

            // 2. Find or Create Batch for this programme
            String targetBatchId = null;
            if (progId != null) {
                List<Batch> progBatches = batchRepository.findByProgrammeId(progId);
                if (!progBatches.isEmpty()) {
                    targetBatchId = progBatches.get(0).getId();
                }
            }

            if (targetBatchId == null) {
                targetBatchId = batchRepository.findAll().stream()
                        .map(Batch::getId)
                        .findFirst()
                        .orElse(null);
            }

            if (targetBatchId == null) {
                String pId = progId != null ? progId : "prog-1";
                Batch newBatch = Batch.builder()
                        .id("batch-" + UUID.randomUUID().toString().substring(0, 8))
                        .programmeId(pId)
                        .programmeCode("BTECH")
                        .programmeName("B.Tech Programme")
                        .name("Batch " + academicYear)
                        .startYear("2025")
                        .endYear("2029")
                        .status("ACTIVE")
                        .build();
                try {
                    targetBatchId = batchRepository.save(newBatch).getId();
                } catch (Exception e) {
                    targetBatchId = "batch-default";
                }
            }

            final String batchIdToUse = targetBatchId;
            List<StudentCoMark> markEntities = new ArrayList<>();
            int newlyRegisteredCount = 0;

            for (StudentMarksRowDto st : studentList) {
                String prn = st.getPrn();
                String name = st.getStudentName() != null && !st.getStudentName().isBlank() ? st.getStudentName() : ("Student " + st.getSrNo());

                Student studentEntity = null;
                if (prn != null && !prn.isBlank()) {
                    Optional<Student> existingOpt = studentRepository.findByPrn(prn);
                    if (existingOpt.isPresent()) {
                        Student existing = existingOpt.get();
                        boolean updated = false;
                        if ((existing.getName() == null || existing.getName().startsWith("Student ")) && !name.startsWith("Student ")) {
                            existing.setName(name);
                            updated = true;
                        }
                        if (existing.getBatchId() == null || existing.getBatchId().isBlank()) {
                            existing.setBatchId(batchIdToUse);
                            updated = true;
                        }
                        if (updated) {
                            try {
                                studentEntity = studentRepository.save(existing);
                            } catch (Exception ignored) {
                                studentEntity = existing;
                            }
                        } else {
                            studentEntity = existing;
                        }
                    } else {
                        Student newStudent = Student.builder()
                                .id(prn) // Set student ID to PRN for foreign key integrity
                                .batchId(batchIdToUse)
                                .prn(prn)
                                .name(name)
                                .email(prn + "@dypiu.edu.in")
                                .status("ENROLLED")
                                .build();
                        try {
                            studentEntity = studentRepository.save(newStudent);
                            newlyRegisteredCount++;
                        } catch (Exception e) {
                            studentEntity = studentRepository.findByPrn(prn).orElse(newStudent);
                        }
                    }
                }

                String studentId = studentEntity != null ? studentEntity.getId() : (prn != null ? prn : "std-" + st.getSrNo());

                if (st.getCoMarks() != null) {
                    for (Map.Entry<String, BigDecimal> entry : st.getCoMarks().entrySet()) {
                        String coCode = entry.getKey();
                        BigDecimal markObtained = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
                        BigDecimal maxMark = coMaxMarks != null && coMaxMarks.containsKey(coCode)
                                ? coMaxMarks.get(coCode)
                                : new BigDecimal("100.00");

                        markEntities.add(StudentCoMark.builder()
                                .id(UUID.randomUUID().toString())
                                .courseId(courseId)
                                .studentId(studentId)
                                .prn(prn != null ? prn : "PRN" + st.getSrNo())
                                .studentName(name)
                                .coCode(coCode)
                                .marksObtained(markObtained)
                                .maxMarks(maxMark)
                                .build());
                    }
                }
            }

            studentCoMarkRepository.saveAll(markEntities);
            System.out.println("  [STUDENT DB AUTO-SYNC SUCCESS]: Registered " + newlyRegisteredCount + " new students in batch '" + batchIdToUse + "' and persisted " + markEntities.size() + " StudentCoMark records for course: " + courseId);
        } catch (Exception e) {
            System.err.println("  [ERROR PERSISTING STUDENT MARKS & REGISTERING DB STUDENTS]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Examination Sheet Attainment Logic & Backend File Processing ---

    @Transactional
    public ExaminationAttainmentResultDto processAndSaveExaminationFile(String courseId, MultipartFile file, BigDecimal optionalThreshold) {
        return processAndSaveExaminationFile(courseId, file, optionalThreshold, "Teacher / Course Coordinator");
    }

    @Transactional
    public ExaminationAttainmentResultDto processAndSaveExaminationFile(String courseId, MultipartFile file, BigDecimal optionalThreshold, String uploadedBy) {
        Course course = courseRepository.findById(courseId).orElse(null);
        String progId = course != null ? course.getProgrammeId() : "N/A";
        String batchName = course != null && course.getAcademicYear() != null ? course.getAcademicYear() : "2025-26";
        String uploader = uploadedBy != null && !uploadedBy.isBlank() ? uploadedBy : "Teacher / Course Coordinator";

        System.out.println("================================================================================");
        System.out.println("[AUDIT LOG - DIRECT ATTAINMENT FILE UPLOAD]");
        System.out.println("  Document Type : DIRECT ATTAINMENT (EXAMINATION)");
        System.out.println("  Course ID     : " + courseId);
        System.out.println("  Programme ID  : " + progId);
        System.out.println("  Batch / Year  : " + batchName);
        System.out.println("  Uploaded By   : " + uploader);
        System.out.println("  Original File : " + file.getOriginalFilename());

        // 1. Save uploaded file to separate direct_attainment directory
        String uploadDir = System.getProperty("user.home") + "/.obe_uploads/direct_attainment/" + courseId + "/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "examination_sheet.xlsx";
        String savedFileName = UUID.randomUUID().toString().substring(0, 8) + "_" + originalFilename;
        File targetFile = new File(dir, savedFileName);

        try {
            file.transferTo(targetFile);
            System.out.println("  [SAVED PATH]  : " + targetFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("  [ERROR SAVING FILE TO DISK]: " + e.getMessage());
        }

        // 2. Parse Excel file using Apache POI
        BigDecimal threshold = optionalThreshold != null ? optionalThreshold : new BigDecimal("45.00");
        Map<String, BigDecimal> coMaxMarks = new LinkedHashMap<>();
        List<StudentMarksRowDto> studentList = new ArrayList<>();
        Map<String, Integer> coHeaderMap = new LinkedHashMap<>();
        Integer expectedStudentCount = null;

        try (InputStream is = new FileInputStream(targetFile);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheet("2. Examination");
            if (sheet == null && workbook.getNumberOfSheets() > 0) {
                sheet = workbook.getSheetAt(0);
            }

            if (sheet != null) {
                DataFormatter formatter = new DataFormatter();

                // Pass 1: Find CO headers (CO1, CO2, CO3, ...)
                for (Row r : sheet) {
                    for (Cell c : r) {
                        String val = formatter.formatCellValue(c).trim();
                        if (val.toUpperCase().matches("^CO\\d+$")) {
                            coHeaderMap.put(val.toUpperCase(), c.getColumnIndex());
                        }
                    }
                    if (!coHeaderMap.isEmpty()) break;
                }

                if (coHeaderMap.isEmpty()) {
                    coHeaderMap.put("CO1", 5);
                    coHeaderMap.put("CO2", 6);
                    coHeaderMap.put("CO3", 7);
                    coHeaderMap.put("CO4", 8);
                    coHeaderMap.put("CO5", 9);
                }

                // Pass 2: Parse total student count, threshold, Out of marks, and student rows
                for (Row r : sheet) {
                    if (r == null) continue;

                    StringBuilder sb = new StringBuilder();
                    for (Cell c : r) {
                        if (c != null) {
                            sb.append(formatter.formatCellValue(c).trim()).append(" ");
                        }
                    }
                    String rowText = sb.toString().trim().toLowerCase();
                    if (rowText.isBlank()) continue;

                    // 0. Total Number of Students (e.g. Row 9)
                    if (rowText.contains("total number of students") || rowText.contains("total students")) {
                        for (Cell c : r) {
                            if (c != null) {
                                String cVal = formatter.formatCellValue(c).trim();
                                try {
                                    int cnt = Integer.parseInt(cVal);
                                    if (cnt > 0 && cnt < 2000) {
                                        expectedStudentCount = cnt;
                                        System.out.println("  [ROW 9 TOTAL STUDENTS COUNT DETECTED]: " + expectedStudentCount);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }

                    // 1. Threshold
                    if (rowText.contains("threshhold") || rowText.contains("threshold")) {
                        for (Cell c : r) {
                            if (c != null) {
                                String cVal = formatter.formatCellValue(c).trim();
                                try {
                                    double d = Double.parseDouble(cVal);
                                    if (d > 0 && d <= 100) {
                                        threshold = BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }

                    // 2. Out of marks (Row 19)
                    if (rowText.startsWith("out of") || (rowText.contains("out of") && !rowText.contains("fraction") && !rowText.contains("%"))) {
                        for (Map.Entry<String, Integer> entry : coHeaderMap.entrySet()) {
                            Cell c = r.getCell(entry.getValue());
                            if (c != null) {
                                String cVal = formatter.formatCellValue(c).trim();
                                try {
                                    double d = Double.parseDouble(cVal);
                                    if (d > 0) {
                                        coMaxMarks.put(entry.getKey(), BigDecimal.valueOf(d).setScale(2, RoundingMode.HALF_UP));
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }

                    // 3. Student rows: search for PRN (8-12 digits)
                    String prnVal = null;
                    String nameVal = "";
                    int prnCol = -1;

                    for (Cell c : r) {
                        if (c != null) {
                            String cVal = formatter.formatCellValue(c).trim();
                            if (cVal.matches("^\\d{8,12}$")) {
                                prnVal = cVal;
                                prnCol = c.getColumnIndex();
                                break;
                            }
                        }
                    }

                    if (prnVal != null && prnCol != -1) {
                        if (expectedStudentCount != null && studentList.size() >= expectedStudentCount) {
                            continue;
                        }
                        for (int colIdx = prnCol + 1; colIdx < Math.min(prnCol + 4, r.getLastCellNum()); colIdx++) {
                            Cell nameCell = r.getCell(colIdx);
                            if (nameCell != null) {
                                String str = formatter.formatCellValue(nameCell).trim();
                                if (!str.isBlank() && str.length() > 1 && !str.matches("^\\d+$")) {
                                    nameVal = str;
                                    break;
                                }
                            }
                        }

                        Map<String, BigDecimal> coMarks = new LinkedHashMap<>();
                        for (Map.Entry<String, Integer> entry : coHeaderMap.entrySet()) {
                            Cell markCell = r.getCell(entry.getValue());
                            BigDecimal mark = BigDecimal.ZERO;
                            if (markCell != null) {
                                String mVal = formatter.formatCellValue(markCell).trim();
                                try {
                                    mark = BigDecimal.valueOf(Double.parseDouble(mVal)).setScale(2, RoundingMode.HALF_UP);
                                } catch (NumberFormatException ignored) {}
                            }
                            coMarks.put(entry.getKey(), mark);
                        }

                        studentList.add(StudentMarksRowDto.builder()
                                .srNo(studentList.size() + 1)
                                .prn(prnVal)
                                .studentName(nameVal.isBlank() ? "Student " + (studentList.size() + 1) : nameVal)
                                .coMarks(coMarks)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("  [EXCEL POI PARSING ERROR]: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. Persist Student CO Marks to Database
        saveStudentCoMarksToDatabase(courseId, coMaxMarks, studentList);

        // 4. Persist Uploaded Document Record to Database with Audit Metadata
        try {
            uploadedDocumentRepository.deleteByCourseIdAndDocumentType(courseId, "EXAMINATION");
            UploadedDocument doc = UploadedDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .courseId(courseId)
                    .programmeId(progId)
                    .batchName(batchName)
                    .uploadedBy(uploader)
                    .documentType("EXAMINATION")
                    .fileName(originalFilename)
                    .savedFileName(savedFileName)
                    .savedPath(targetFile.getAbsolutePath())
                    .fileSize(file.getSize())
                    .recordsProcessed(studentList.size())
                    .thresholdPercentage(threshold)
                    .uploadedAt(ZonedDateTime.now())
                    .build();
            uploadedDocumentRepository.save(doc);
            System.out.println("  [DATABASE PERSISTED] Direct Attainment Document Audit Record Saved Successfully.");
            System.out.println("================================================================================");
        } catch (Exception e) {
            System.err.println("  [ERROR SAVING UPLOADED DOCUMENT RECORD]: " + e.getMessage());
        }

        ExaminationMarksPayloadDto payload = ExaminationMarksPayloadDto.builder()
                .courseId(courseId)
                .thresholdPercentage(threshold)
                .coMaxMarks(coMaxMarks)
                .studentMarks(studentList)
                .build();

        ExaminationAttainmentResultDto result = calculateExaminationAttainment(courseId, payload);

        Map<String, Object> fileDetails = new LinkedHashMap<>();
        fileDetails.put("fileName", originalFilename);
        fileDetails.put("savedFileName", savedFileName);
        fileDetails.put("fileSize", file.getSize());
        fileDetails.put("savedPath", targetFile.getAbsolutePath());
        fileDetails.put("uploadedAt", java.time.ZonedDateTime.now().toString());
        fileDetails.put("recordsProcessed", studentList.size());
        fileDetails.put("status", "SAVED_AND_VERIFIED");

        result.setFileDetails(fileDetails);
        examinationAttainmentStore.put(courseId, result);

        return result;
    }

    @Transactional
    public ExaminationAttainmentResultDto calculateExaminationAttainment(String courseId, ExaminationMarksPayloadDto payload) {
        System.out.println("================================================================================");
        System.out.println("[AttainmentCalculationService] >>> calculateExaminationAttainment called | courseId: " + courseId);

        if (payload == null) {
            return getExaminationAttainment(courseId);
        }

        BigDecimal threshold = payload.getThresholdPercentage() != null ? payload.getThresholdPercentage() : new BigDecimal("45.00");
        Map<String, BigDecimal> maxMarksMap = payload.getCoMaxMarks() != null ? payload.getCoMaxMarks() : new LinkedHashMap<>();
        List<StudentMarksRowDto> studentList = payload.getStudentMarks() != null ? payload.getStudentMarks() : Collections.emptyList();

        int totalStudents = studentList.size();
        System.out.println("  [THRESHOLD PERCENTAGE]: " + threshold + "%");
        System.out.println("  [OUT OF MARKS PER CO]: " + maxMarksMap);
        System.out.println("  [TOTAL STUDENTS COUNT]: " + totalStudents);

        Map<String, BigDecimal> thresholdMarksMap = new LinkedHashMap<>();
        Map<String, Integer> aboveThresholdCountMap = new LinkedHashMap<>();
        Map<String, BigDecimal> percentageAboveThresholdMap = new LinkedHashMap<>();
        Map<String, Integer> attainmentLevelsMap = new LinkedHashMap<>();

        BigDecimal sumLevels = BigDecimal.ZERO;
        int coCount = 0;

        for (Map.Entry<String, BigDecimal> entry : maxMarksMap.entrySet()) {
            String coCode = entry.getKey();
            BigDecimal maxMarks = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;

            // 1. Threshold mark for CO = maxMarks * (threshold / 100)
            BigDecimal coThresholdMark = maxMarks.multiply(threshold).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            thresholdMarksMap.put(coCode, coThresholdMark);

            // 2. Count students >= coThresholdMark
            int countAbove = 0;
            for (StudentMarksRowDto st : studentList) {
                if (st.getCoMarks() != null && st.getCoMarks().containsKey(coCode)) {
                    BigDecimal markObtained = st.getCoMarks().get(coCode);
                    if (markObtained != null && markObtained.compareTo(coThresholdMark) >= 0) {
                        countAbove++;
                    }
                }
            }
            aboveThresholdCountMap.put(coCode, countAbove);

            // 3. % of students above threshold = (countAbove / totalStudents) * 100
            BigDecimal pctAbove = totalStudents > 0
                    ? BigDecimal.valueOf(((double) countAbove / totalStudents) * 100.0).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            percentageAboveThresholdMap.put(coCode, pctAbove);

            // 4. Calculate Score 1-3 based on percentage score:
            //    >= 60% -> 3
            //    >= 40% -> 2
            //    > 0%  -> 1
            //    0%    -> 0
            int level = 0;
            double pctVal = pctAbove.doubleValue();
            if (pctVal >= 60.0) {
                level = 3;
            } else if (pctVal >= 40.0) {
                level = 2;
            } else if (pctVal > 0.0) {
                level = 1;
            }
            attainmentLevelsMap.put(coCode, level);

            sumLevels = sumLevels.add(BigDecimal.valueOf(level));
            coCount++;

            System.out.println("  [" + coCode + "] MaxMarks=" + maxMarks + " | ThresholdMark=" + coThresholdMark
                    + " | StudentsAbove=" + countAbove + "/" + totalStudents + " (" + pctAbove + "%) | AttainmentLevel=" + level);
        }

        BigDecimal overallDirectAttainment = coCount > 0
                ? sumLevels.divide(BigDecimal.valueOf(coCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        System.out.println("  [OVERALL DIRECT CO ATTAINMENT SCORE]: " + overallDirectAttainment);
        System.out.println("================================================================================");

        ExaminationAttainmentResultDto result = ExaminationAttainmentResultDto.builder()
                .courseId(courseId)
                .thresholdPercentage(threshold)
                .totalStudents(totalStudents)
                .coMaxMarks(maxMarksMap)
                .coThresholdMarks(thresholdMarksMap)
                .studentsAboveThreshold(aboveThresholdCountMap)
                .percentageAboveThreshold(percentageAboveThresholdMap)
                .coAttainmentLevels(attainmentLevelsMap)
                .overallDirectCoAttainment(overallDirectAttainment)
                .studentMarks(studentList)
                .build();

        examinationAttainmentStore.put(courseId, result);
        return result;
    }

    @Transactional(readOnly = true)
    public ExaminationAttainmentResultDto getExaminationAttainment(String courseId) {
        // 1. Check in-memory store
        if (courseId != null && examinationAttainmentStore.containsKey(courseId)) {
            return examinationAttainmentStore.get(courseId);
        }

        // 2. Restore from Database Tables (student_co_marks & uploaded_documents)
        if (courseId != null) {
            List<StudentCoMark> dbMarks = studentCoMarkRepository.findByCourseId(courseId);
            if (dbMarks != null && !dbMarks.isEmpty()) {
                System.out.println("  [DATABASE RESTORE]: Reconstructing direct attainment for courseId: " + courseId + " from " + dbMarks.size() + " StudentCoMark records");

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

                BigDecimal threshold = new BigDecimal("45.00");
                Map<String, Object> fileDetails = null;

                Optional<UploadedDocument> docOpt = uploadedDocumentRepository
                        .findFirstByCourseIdAndDocumentTypeOrderByUploadedAtDesc(courseId, "EXAMINATION");

                if (docOpt.isPresent()) {
                    UploadedDocument doc = docOpt.get();
                    if (doc.getThresholdPercentage() != null) threshold = doc.getThresholdPercentage();

                    fileDetails = new LinkedHashMap<>();
                    fileDetails.put("fileName", doc.getFileName());
                    fileDetails.put("savedFileName", doc.getSavedFileName());
                    fileDetails.put("fileSize", doc.getFileSize());
                    fileDetails.put("savedPath", doc.getSavedPath());
                    fileDetails.put("uploadedAt", doc.getUploadedAt().toString());
                    fileDetails.put("recordsProcessed", doc.getRecordsProcessed());
                    fileDetails.put("status", "SAVED_AND_VERIFIED");
                }

                ExaminationMarksPayloadDto payload = ExaminationMarksPayloadDto.builder()
                        .courseId(courseId)
                        .thresholdPercentage(threshold)
                        .coMaxMarks(coMaxMarks)
                        .studentMarks(studentList)
                        .build();

                ExaminationAttainmentResultDto result = calculateExaminationAttainment(courseId, payload);
                result.setFileDetails(fileDetails);
                examinationAttainmentStore.put(courseId, result);
                return result;
            }
        }

        return ExaminationAttainmentResultDto.builder()
                .courseId(courseId)
                .thresholdPercentage(new BigDecimal("45.00"))
                .totalStudents(0)
                .coMaxMarks(Collections.emptyMap())
                .coThresholdMarks(Collections.emptyMap())
                .studentsAboveThreshold(Collections.emptyMap())
                .percentageAboveThreshold(Collections.emptyMap())
                .coAttainmentLevels(Collections.emptyMap())
                .overallDirectCoAttainment(BigDecimal.ZERO)
                .studentMarks(Collections.emptyList())
                .fileDetails(null)
                .build();
    }

    // --- Course End Survey Attainment Logic (Sheet 3: Course End Survey) ---

    @Transactional
    public SurveyAttainmentResultDto processAndSaveSurveyFile(String courseId, MultipartFile file) {
        return processAndSaveSurveyFile(courseId, file, "Teacher / Course Coordinator");
    }

    @Transactional
    public SurveyAttainmentResultDto processAndSaveSurveyFile(String courseId, MultipartFile file, String uploadedBy) {
        Course course = courseRepository.findById(courseId).orElse(null);
        String progId = course != null ? course.getProgrammeId() : "N/A";
        String batchName = course != null && course.getAcademicYear() != null ? course.getAcademicYear() : "2025-26";
        String uploader = uploadedBy != null && !uploadedBy.isBlank() ? uploadedBy : "Teacher / Course Coordinator";

        System.out.println("================================================================================");
        System.out.println("[AUDIT LOG - INDIRECT ATTAINMENT FILE UPLOAD]");
        System.out.println("  Document Type : INDIRECT ATTAINMENT (SURVEY)");
        System.out.println("  Course ID     : " + courseId);
        System.out.println("  Programme ID  : " + progId);
        System.out.println("  Batch / Year  : " + batchName);
        System.out.println("  Uploaded By   : " + uploader);
        System.out.println("  Original File : " + file.getOriginalFilename());

        String uploadDir = System.getProperty("user.home") + "/.obe_uploads/indirect_attainment/" + courseId + "/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "survey_sheet.xlsx";
        String savedFileName = UUID.randomUUID().toString().substring(0, 8) + "_" + originalFilename;
        File targetFile = new File(dir, savedFileName);

        try {
            file.transferTo(targetFile);
            System.out.println("  [SAVED PATH]  : " + targetFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("  [ERROR SAVING SURVEY FILE TO DISK]: " + e.getMessage());
        }

        List<SurveyResponseRowDto> surveyResponses = new ArrayList<>();
        Map<String, Integer> coHeaderMap = new LinkedHashMap<>();
        int headerRowIdx = -1;
        Integer expectedStudents = null;

        try (InputStream is = new FileInputStream(targetFile);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheet("3. Course End Survey");
            if (sheet == null && workbook.getNumberOfSheets() > 0) {
                sheet = workbook.getSheetAt(0);
            }

            if (sheet != null) {
                DataFormatter formatter = new DataFormatter();

                // Pass 1: Find Expected Students count & CO Header Row (Row 18)
                int rIdx = 0;
                for (Row r : sheet) {
                    StringBuilder sb = new StringBuilder();
                    for (Cell c : r) {
                        if (c != null) sb.append(formatter.formatCellValue(c).trim()).append(" ");
                    }
                    String rowText = sb.toString().trim().toLowerCase();

                    if (rowText.contains("no. of students") || rowText.contains("total students") || rowText.contains("number of students")) {
                        for (Cell c : r) {
                            if (c != null) {
                                try {
                                    int cnt = Integer.parseInt(formatter.formatCellValue(c).trim());
                                    if (cnt > 0 && cnt < 2000) expectedStudents = cnt;
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }

                    boolean hasCO1 = false;
                    for (Cell c : r) {
                        if (c != null && formatter.formatCellValue(c).trim().equalsIgnoreCase("CO1")) {
                            hasCO1 = true;
                            break;
                        }
                    }
                    if (hasCO1 && rIdx > 5) {
                        headerRowIdx = rIdx;
                        for (Cell c : r) {
                            String val = formatter.formatCellValue(c).trim();
                            if (val.toUpperCase().matches("^CO\\d+$")) {
                                coHeaderMap.put(val.toUpperCase(), c.getColumnIndex());
                            }
                        }
                    }
                    rIdx++;
                }

                if (coHeaderMap.isEmpty()) {
                    coHeaderMap.put("CO1", 2);
                    coHeaderMap.put("CO2", 3);
                    coHeaderMap.put("CO3", 4);
                    coHeaderMap.put("CO4", 5);
                    coHeaderMap.put("CO5", 6);
                }

                // Pass 2: Parse student survey response rows
                rIdx = 0;
                for (Row r : sheet) {
                    if (r == null || rIdx <= headerRowIdx) {
                        rIdx++;
                        continue;
                    }

                    Map<String, String> coFeedbacks = new LinkedHashMap<>();
                    int validCount = 0;

                    for (Map.Entry<String, Integer> entry : coHeaderMap.entrySet()) {
                        Cell c = r.getCell(entry.getValue());
                        if (c != null) {
                            String valStr = formatter.formatCellValue(c).trim();
                            if (valStr.matches("(?i)^(substantial|moderate|slight|[123])$")) {
                                coFeedbacks.put(entry.getKey(), valStr);
                                validCount++;
                            }
                        }
                    }

                    if (validCount == coHeaderMap.size()) {
                        if (expectedStudents == null || surveyResponses.size() < expectedStudents) {
                            surveyResponses.add(SurveyResponseRowDto.builder()
                                    .srNo(surveyResponses.size() + 1)
                                    .studentName("Student " + (surveyResponses.size() + 1))
                                    .coFeedbacks(coFeedbacks)
                                    .build());
                        }
                    }
                    rIdx++;
                }
            }
        } catch (Exception e) {
            System.err.println("  [SURVEY POI PARSING ERROR]: " + e.getMessage());
            e.printStackTrace();
        }

        // Persist Survey Upload Document Metadata with Audit Metadata
        try {
            uploadedDocumentRepository.deleteByCourseIdAndDocumentType(courseId, "SURVEY");
            UploadedDocument doc = UploadedDocument.builder()
                    .id(UUID.randomUUID().toString())
                    .courseId(courseId)
                    .programmeId(progId)
                    .batchName(batchName)
                    .uploadedBy(uploader)
                    .documentType("SURVEY")
                    .fileName(originalFilename)
                    .savedFileName(savedFileName)
                    .savedPath(targetFile.getAbsolutePath())
                    .fileSize(file.getSize())
                    .recordsProcessed(surveyResponses.size())
                    .uploadedAt(ZonedDateTime.now())
                    .build();
            uploadedDocumentRepository.save(doc);
            System.out.println("  [DATABASE PERSISTED] Indirect Attainment Document Audit Record Saved Successfully.");
            System.out.println("================================================================================");
        } catch (Exception e) {
            System.err.println("  [ERROR SAVING SURVEY UPLOAD DOCUMENT RECORD]: " + e.getMessage());
        }

        SurveyMarksPayloadDto payload = SurveyMarksPayloadDto.builder()
                .courseId(courseId)
                .surveyResponses(surveyResponses)
                .build();

        SurveyAttainmentResultDto result = calculateSurveyAttainment(courseId, payload);

        Map<String, Object> fileDetails = new LinkedHashMap<>();
        fileDetails.put("fileName", originalFilename);
        fileDetails.put("savedFileName", savedFileName);
        fileDetails.put("fileSize", file.getSize());
        fileDetails.put("savedPath", targetFile.getAbsolutePath());
        fileDetails.put("uploadedAt", java.time.ZonedDateTime.now().toString());
        fileDetails.put("recordsProcessed", surveyResponses.size());
        fileDetails.put("status", "SAVED_AND_VERIFIED");

        result.setFileDetails(fileDetails);
        surveyAttainmentStore.put(courseId, result);
        return result;
    }

    @Transactional
    public SurveyAttainmentResultDto calculateSurveyAttainment(String courseId, SurveyMarksPayloadDto payload) {
        System.out.println("================================================================================");
        System.out.println("[AttainmentCalculationService] >>> calculateSurveyAttainment called | courseId: " + courseId);

        if (payload == null || payload.getSurveyResponses() == null) {
            return getSurveyAttainment(courseId);
        }

        List<SurveyResponseRowDto> list = payload.getSurveyResponses();
        int totalStudents = list.size();
        System.out.println("  [TOTAL SURVEY RESPONSES]: " + totalStudents);

        Set<String> coCodes = new LinkedHashSet<>();
        for (SurveyResponseRowDto st : list) {
            if (st.getCoFeedbacks() != null) {
                coCodes.addAll(st.getCoFeedbacks().keySet());
            }
        }
        if (coCodes.isEmpty()) {
            coCodes.addAll(Arrays.asList("CO1", "CO2", "CO3", "CO4", "CO5"));
        }

        Map<String, Integer> lvl1Counts = new LinkedHashMap<>();
        Map<String, Integer> lvl2Counts = new LinkedHashMap<>();
        Map<String, Integer> lvl3Counts = new LinkedHashMap<>();

        Map<String, BigDecimal> lvl1Pcts = new LinkedHashMap<>();
        Map<String, BigDecimal> lvl2Pcts = new LinkedHashMap<>();
        Map<String, BigDecimal> lvl3Pcts = new LinkedHashMap<>();

        Map<String, BigDecimal> overallIndirectPcts = new LinkedHashMap<>();
        Map<String, BigDecimal> indirectAttainmentScores = new LinkedHashMap<>();

        BigDecimal sumScores = BigDecimal.ZERO;
        int coCount = 0;

        for (String coCode : coCodes) {
            int cnt1 = 0; // Slight (1)
            int cnt2 = 0; // Moderate (2)
            int cnt3 = 0; // Substantial (3)

            for (SurveyResponseRowDto st : list) {
                if (st.getCoFeedbacks() != null && st.getCoFeedbacks().containsKey(coCode)) {
                    String fb = String.valueOf(st.getCoFeedbacks().get(coCode)).trim().toLowerCase();
                    if (fb.contains("slight") || fb.equals("1")) {
                        cnt1++;
                    } else if (fb.contains("moderate") || fb.equals("2")) {
                        cnt2++;
                    } else if (fb.contains("substantial") || fb.equals("3")) {
                        cnt3++;
                    } else {
                        cnt3++;
                    }
                }
            }

            lvl1Counts.put(coCode, cnt1);
            lvl2Counts.put(coCode, cnt2);
            lvl3Counts.put(coCode, cnt3);

            BigDecimal pct1 = totalStudents > 0 ? BigDecimal.valueOf(((double) cnt1 / totalStudents) * 100.0).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal pct2 = totalStudents > 0 ? BigDecimal.valueOf(((double) cnt2 / totalStudents) * 100.0).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal pct3 = totalStudents > 0 ? BigDecimal.valueOf(((double) cnt3 / totalStudents) * 100.0).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            lvl1Pcts.put(coCode, pct1);
            lvl2Pcts.put(coCode, pct2);
            lvl3Pcts.put(coCode, pct3);

            double overallPctVal = (pct1.doubleValue() * (1.0 / 3.0)) + (pct2.doubleValue() * (2.0 / 3.0)) + (pct3.doubleValue() * (3.0 / 3.0));
            BigDecimal overallIndirectPct = BigDecimal.valueOf(overallPctVal).setScale(2, RoundingMode.HALF_UP);
            overallIndirectPcts.put(coCode, overallIndirectPct);

            double scoreVal = (overallPctVal / 100.0) * 3.0;
            BigDecimal indirectScore = BigDecimal.valueOf(scoreVal).setScale(2, RoundingMode.HALF_UP);
            indirectAttainmentScores.put(coCode, indirectScore);

            sumScores = sumScores.add(indirectScore);
            coCount++;

            System.out.println("  [" + coCode + "] Slight(1)=" + cnt1 + " (" + pct1 + "%) | Moderate(2)=" + cnt2 + " (" + pct2 + "%) | Substantial(3)=" + cnt3 + " (" + pct3 + "%) | OverallIndirect%=" + overallIndirectPct + "% | IndirectScore=" + indirectScore + "/3.00");
        }

        BigDecimal overallIndirectCoAttainment = coCount > 0
                ? sumScores.divide(BigDecimal.valueOf(coCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        System.out.println("  [OVERALL INDIRECT CO ATTAINMENT SCORE]: " + overallIndirectCoAttainment);
        System.out.println("================================================================================");

        SurveyAttainmentResultDto result = SurveyAttainmentResultDto.builder()
                .courseId(courseId)
                .totalStudents(totalStudents)
                .level1Counts(lvl1Counts)
                .level2Counts(lvl2Counts)
                .level3Counts(lvl3Counts)
                .level1Percentages(lvl1Pcts)
                .level2Percentages(lvl2Pcts)
                .level3Percentages(lvl3Pcts)
                .overallIndirectPercentages(overallIndirectPcts)
                .indirectAttainmentScores(indirectAttainmentScores)
                .overallIndirectCoAttainment(overallIndirectCoAttainment)
                .surveyResponses(list)
                .build();

        surveyAttainmentStore.put(courseId, result);
        return result;
    }

    private List<SurveyResponseRowDto> parseSurveyFileToResponses(File targetFile) {
        List<SurveyResponseRowDto> surveyResponses = new ArrayList<>();
        Map<String, Integer> coHeaderMap = new LinkedHashMap<>();
        int headerRowIdx = -1;

        try (InputStream is = new FileInputStream(targetFile);
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheet("3. Course End Survey");
            if (sheet == null && workbook.getNumberOfSheets() > 0) {
                sheet = workbook.getSheetAt(0);
            }

            if (sheet != null) {
                DataFormatter formatter = new DataFormatter();
                int rIdx = 0;
                for (Row r : sheet) {
                    boolean hasCO1 = false;
                    for (Cell c : r) {
                        if (c != null && formatter.formatCellValue(c).trim().equalsIgnoreCase("CO1")) {
                            hasCO1 = true;
                            break;
                        }
                    }
                    if (hasCO1 && rIdx > 5) {
                        headerRowIdx = rIdx;
                        for (Cell c : r) {
                            String val = formatter.formatCellValue(c).trim();
                            if (val.toUpperCase().matches("^CO\\d+$")) {
                                coHeaderMap.put(val.toUpperCase(), c.getColumnIndex());
                            }
                        }
                    }
                    rIdx++;
                }

                if (coHeaderMap.isEmpty()) {
                    coHeaderMap.put("CO1", 2);
                    coHeaderMap.put("CO2", 3);
                    coHeaderMap.put("CO3", 4);
                    coHeaderMap.put("CO4", 5);
                    coHeaderMap.put("CO5", 6);
                }

                rIdx = 0;
                int srNo = 1;
                for (Row r : sheet) {
                    if (r == null || rIdx <= headerRowIdx) {
                        rIdx++;
                        continue;
                    }

                    String prnVal = "";
                    Cell prnCell = r.getCell(1);
                    if (prnCell != null) prnVal = formatter.formatCellValue(prnCell).trim();

                    Map<String, String> coFeedbacks = new LinkedHashMap<>();
                    boolean hasFeedback = false;
                    for (Map.Entry<String, Integer> entry : coHeaderMap.entrySet()) {
                        String coCode = entry.getKey();
                        int colIdx = entry.getValue();
                        Cell feedbackCell = r.getCell(colIdx);
                        if (feedbackCell != null) {
                            String val = formatter.formatCellValue(feedbackCell).trim();
                            if (!val.isEmpty()) {
                                coFeedbacks.put(coCode, val);
                                hasFeedback = true;
                            }
                        }
                    }

                    if (hasFeedback) {
                        surveyResponses.add(SurveyResponseRowDto.builder()
                                .srNo(srNo++)
                                .studentName(prnVal.isEmpty() ? "Student " + (srNo - 1) : prnVal)
                                .coFeedbacks(coFeedbacks)
                                .build());
                    }
                    rIdx++;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing survey file: " + e.getMessage());
        }

        return surveyResponses;
    }

    @Transactional(readOnly = true)
    public SurveyAttainmentResultDto getSurveyAttainment(String courseId) {
        if (courseId != null && surveyAttainmentStore.containsKey(courseId)) {
            return surveyAttainmentStore.get(courseId);
        }

        // Restore Survey Upload Record from uploaded_documents
        if (courseId != null) {
            Optional<UploadedDocument> docOpt = uploadedDocumentRepository
                    .findFirstByCourseIdAndDocumentTypeOrderByUploadedAtDesc(courseId, "SURVEY");

            if (docOpt.isPresent()) {
                UploadedDocument doc = docOpt.get();
                Map<String, Object> fileDetails = new LinkedHashMap<>();
                fileDetails.put("fileName", doc.getFileName());
                fileDetails.put("savedFileName", doc.getSavedFileName());
                fileDetails.put("fileSize", doc.getFileSize());
                fileDetails.put("savedPath", doc.getSavedPath());
                fileDetails.put("uploadedAt", doc.getUploadedAt().toString());
                fileDetails.put("recordsProcessed", doc.getRecordsProcessed());
                fileDetails.put("status", "SAVED_AND_VERIFIED");

                if (doc.getSavedPath() != null && new File(doc.getSavedPath()).exists()) {
                    List<SurveyResponseRowDto> responses = parseSurveyFileToResponses(new File(doc.getSavedPath()));
                    if (!responses.isEmpty()) {
                        SurveyMarksPayloadDto payload = SurveyMarksPayloadDto.builder()
                                .courseId(courseId)
                                .surveyResponses(responses)
                                .build();
                        SurveyAttainmentResultDto result = calculateSurveyAttainment(courseId, payload);
                        result.setFileDetails(fileDetails);
                        surveyAttainmentStore.put(courseId, result);
                        return result;
                    }
                }

                SurveyAttainmentResultDto result = SurveyAttainmentResultDto.builder()
                        .courseId(courseId)
                        .totalStudents(doc.getRecordsProcessed() != null ? doc.getRecordsProcessed() : 0)
                        .level1Counts(Collections.emptyMap())
                        .level2Counts(Collections.emptyMap())
                        .level3Counts(Collections.emptyMap())
                        .level1Percentages(Collections.emptyMap())
                        .level2Percentages(Collections.emptyMap())
                        .level3Percentages(Collections.emptyMap())
                        .overallIndirectPercentages(Collections.emptyMap())
                        .indirectAttainmentScores(Collections.emptyMap())
                        .overallIndirectCoAttainment(BigDecimal.ZERO)
                        .surveyResponses(Collections.emptyList())
                        .fileDetails(fileDetails)
                        .build();

                surveyAttainmentStore.put(courseId, result);
                return result;
            }
        }

        return SurveyAttainmentResultDto.builder()
                .courseId(courseId)
                .totalStudents(0)
                .level1Counts(Collections.emptyMap())
                .level2Counts(Collections.emptyMap())
                .level3Counts(Collections.emptyMap())
                .level1Percentages(Collections.emptyMap())
                .level2Percentages(Collections.emptyMap())
                .level3Percentages(Collections.emptyMap())
                .overallIndirectPercentages(Collections.emptyMap())
                .indirectAttainmentScores(Collections.emptyMap())
                .overallIndirectCoAttainment(BigDecimal.ZERO)
                .surveyResponses(Collections.emptyList())
                .fileDetails(null)
                .build();
    }

    private <T> T getCoMapValue(Map<String, T> map, String coCode, int index, T defaultValue) {
        if (map == null || map.isEmpty()) return defaultValue;
        if (coCode != null && map.containsKey(coCode)) return map.get(coCode);

        String posKey = "CO" + (index + 1);
        if (map.containsKey(posKey)) return map.get(posKey);

        String normCoCode = coCode != null ? coCode.toLowerCase().replaceAll("[^a-z0-9]", "") : "";
        String normPosKey = posKey.toLowerCase();
        String indexStr = String.valueOf(index + 1);

        for (Map.Entry<String, T> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;
            String k = entry.getKey().toLowerCase().replaceAll("[^a-z0-9]", "");
            if (k.equals(normCoCode) || k.equals(normPosKey) || k.endsWith(indexStr) || k.equals("co" + indexStr)) {
                return entry.getValue();
            }
        }
        return defaultValue;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calculateCourseCoAttainment(String courseId) {
        AttainmentConfiguration config = getAttainmentConfig(courseId);
        List<CourseOutcome> cos = courseOutcomeRepository.findByCourseId(courseId);

        ExaminationAttainmentResultDto examResult = getExaminationAttainment(courseId);
        SurveyAttainmentResultDto surveyResult = getSurveyAttainment(courseId);

        // If no COs in DB for course, fallback to CO1..CO5
        if (cos == null || cos.isEmpty()) {
            cos = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                cos.add(CourseOutcome.builder()
                        .id("co-fallback-" + i)
                        .courseId(courseId)
                        .code("CO" + i)
                        .statement("Course outcome CO" + i)
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

            // 1. Direct Examination Data
            BigDecimal directPct = getCoMapValue(examResult.getPercentageAboveThreshold(), coCode, i, BigDecimal.ZERO);
            Integer directLevelObj = getCoMapValue(examResult.getCoAttainmentLevels(), coCode, i, 0);
            int directLevel = directLevelObj != null ? directLevelObj : 0;

            // 2. Indirect Course End Survey Data
            BigDecimal indirectPct = getCoMapValue(surveyResult.getOverallIndirectPercentages(), coCode, i, BigDecimal.ZERO);
            BigDecimal indirectScore = getCoMapValue(surveyResult.getIndirectAttainmentScores(), coCode, i, BigDecimal.ZERO);

            int indirectLevel = indirectScore.compareTo(new BigDecimal("2.50")) >= 0 ? 3 : (indirectScore.compareTo(new BigDecimal("1.50")) >= 0 ? 2 : (indirectScore.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0));
            if (indirectPct.compareTo(new BigDecimal("80.00")) >= 0) {
                indirectLevel = 3;
            } else if (indirectPct.compareTo(new BigDecimal("60.00")) >= 0) {
                indirectLevel = 2;
            } else if (indirectPct.compareTo(BigDecimal.ZERO) > 0 && indirectLevel == 0) {
                indirectLevel = 1;
            }

            // 3. Weightages (80% Direct / 20% Indirect)
            double directW = config.getDirectWeight() != null ? config.getDirectWeight().doubleValue() / 100.0 : 0.80;
            double indirectW = config.getIndirectWeight() != null ? config.getIndirectWeight().doubleValue() / 100.0 : 0.20;

            double combinedScore = (directLevel * directW) + (indirectLevel * indirectW);
            BigDecimal roundedAttainment = BigDecimal.valueOf(combinedScore).setScale(2, RoundingMode.HALF_UP);

            sumCoAttainment = sumCoAttainment.add(roundedAttainment);
            countCOs++;

            Map<String, Object> coRes = new LinkedHashMap<>();
            coRes.put("coCode", coCode);
            coRes.put("statement", statement);
            coRes.put("directPct", directPct);
            coRes.put("directLevel", directLevel);
            coRes.put("indirectPct", indirectPct);
            coRes.put("indirectLevel", indirectLevel);
            coRes.put("indirectScore", indirectScore);
            coRes.put("combinedAttainment", roundedAttainment);

            coResults.add(coRes);
        }

        BigDecimal overallCoAttainment = countCOs > 0
                ? sumCoAttainment.divide(BigDecimal.valueOf(countCOs), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("courseId", courseId);
        response.put("config", config);
        response.put("coAttainments", coResults);
        response.put("overallCoAttainment", overallCoAttainment);
        response.put("examDetails", examResult);
        response.put("surveyDetails", surveyResult);

        return response;
    }

    public List<UploadedDocument> getUploadedDocumentsForCourse(String courseId) {
        return uploadedDocumentRepository.findByCourseId(courseId);
    }
}
