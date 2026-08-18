package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dypiu.nba.dto.DirectorSetupProgressDto;
import com.dypiu.nba.dto.DirectorSchoolSummaryDto;
import com.dypiu.nba.dto.DepartmentSummaryDto;
import com.dypiu.nba.dto.HodDepartmentSummaryDto;
import com.dypiu.nba.dto.HodSetupProgressDto;
import com.dypiu.nba.dto.ProgrammeCoordinatorSummaryDto;
import com.dypiu.nba.dto.ProgrammeCoordinatorSetupProgressDto;
import com.dypiu.nba.dto.CourseCoordinatorSummaryDto;
import com.dypiu.nba.dto.CourseCoordinatorSetupProgressDto;
import com.dypiu.nba.dto.UserDto;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicService {

    private final SchoolRepository schoolRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final DirectorSetupProgressRepository directorSetupProgressRepository;
    private final HodSetupProgressRepository hodSetupProgressRepository;
    private final ProgrammeCoordinatorSetupProgressRepository pcSetupProgressRepository;
    private final CourseCoordinatorSetupProgressRepository ccSetupProgressRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final ProgrammeOutcomeRepository programmeOutcomeRepository;
    private final ProgrammeSpecificOutcomeRepository programmeSpecificOutcomeRepository;
    private final PeoOutcomeRepository peoOutcomeRepository;
    private final UserRepository userRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseAtrRepository courseAtrRepository;
    private final ProgrammeAtrRepository programmeAtrRepository;
    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;

    @Transactional(readOnly = true)
    public com.dypiu.nba.dto.BatchContextDto getBatchContext(String batchId) {
        System.out.println("[AcademicService] getBatchContext called | batchId: " + batchId);
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + batchId));

        Programme prog = programmeRepository.findById(batch.getProgrammeId()).orElse(null);
        Department dept = (prog != null && prog.getDepartmentId() != null) ? departmentRepository.findById(prog.getDepartmentId()).orElse(null) : null;
        School school = (dept != null && dept.getSchoolId() != null) ? schoolRepository.findById(dept.getSchoolId()).orElse(null) : null;

        List<Student> students = studentRepository.findByBatchId(batchId);
        List<CourseOffering> offerings = courseOfferingRepository.findByBatchId(batchId);
        Set<String> uniqueCourseIds = offerings.stream().map(CourseOffering::getCourseId).collect(Collectors.toSet());
        List<String> offeringIds = offerings.stream().map(CourseOffering::getId).collect(Collectors.toList());

        long completedAtrs = offeringIds.isEmpty() ? 0 : courseAtrRepository.findByCourseOfferingIdIn(offeringIds).stream()
                                                         .filter(a -> a.getStatus() == CourseAtrStatus.VERIFIED)
                                                         .count();

        String progAtrStatus = "DRAFT";
        if (prog != null) {
            Optional<ProgrammeAtr> patr = programmeAtrRepository.findByProgrammeIdAndBatchId(prog.getId(), batchId);
            if (patr.isPresent() && patr.get().getStatus() != null) {
                progAtrStatus = patr.get().getStatus().name();
            }
        }

        return com.dypiu.nba.dto.BatchContextDto.builder()
                .batch(com.dypiu.nba.dto.BatchContextDto.BatchSummary.builder()
                        .id(batch.getId())
                        .name(batch.getName())
                        .programmeId(batch.getProgrammeId())
                        .programmeName(batch.getProgrammeName())
                        .status(batch.getStatus())
                        .build())
                .programme(prog != null ? com.dypiu.nba.dto.BatchContextDto.ProgrammeSummary.builder()
                                          .id(prog.getId())
                                          .code(prog.getCode())
                                          .name(prog.getName())
                                          .build() : null)
                .department(dept != null ? com.dypiu.nba.dto.BatchContextDto.DepartmentSummary.builder()
                                           .id(dept.getId())
                                           .name(dept.getName())
                                           .build() : null)
                .school(school != null ? com.dypiu.nba.dto.BatchContextDto.SchoolSummary.builder()
                                         .id(school.getId())
                                         .name(school.getName())
                                         .build() : null)
                .statistics(com.dypiu.nba.dto.BatchContextDto.Statistics.builder()
                        .studentCount(students.size())
                        .courseCount(uniqueCourseIds.size())
                        .courseOfferingCount(offerings.size())
                        .completedCourseAtrCount(completedAtrs)
                        .programmeAtrStatus(progAtrStatus)
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CourseOffering> getCourseOfferingsByBatch(String batchId) {
        System.out.println("[AcademicService] getCourseOfferingsByBatch called | batchId: " + batchId);
        return courseOfferingRepository.findByBatchId(batchId);
    }

    @Transactional
    public CourseOffering saveCourseOffering(CourseOffering offering) {
        System.out.println("[AcademicService] saveCourseOffering called | courseId: " + (offering != null ? offering.getCourseId() : "null"));
        if (offering.getId() == null) offering.setId("offering-" + UUID.randomUUID().toString().substring(0, 8));
        return courseOfferingRepository.save(offering);
    }

    @Transactional
    public void deleteCourseOffering(String id) {
        System.out.println("[AcademicService] deleteCourseOffering called | id: " + id);
        courseOfferingRepository.deleteById(id);
    }

    // --- Director School Summary ---
    @Transactional(readOnly = true)
    public DirectorSchoolSummaryDto getDirectorSchoolSummary(String directorEmail) {
        System.out.println("[AcademicService] Starting school summary fetch for directorEmail: " + directorEmail);
        Optional<School> schoolOpt = Optional.empty();

        if (directorEmail != null && !directorEmail.isBlank()) {
            String cleanEmail = directorEmail.trim();
            schoolOpt = schoolRepository.findByDirectorEmailIgnoreCase(cleanEmail);
            if (schoolOpt.isEmpty()) {
                Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (user.getSchoolId() != null && !user.getSchoolId().isBlank()) {
                        schoolOpt = schoolRepository.findById(user.getSchoolId());
                    }
                    if (schoolOpt.isEmpty() && user.getId() != null) {
                        schoolOpt = schoolRepository.findByDirectorId(user.getId());
                    }
                }
            }
        }

        if (schoolOpt.isEmpty()) {
            System.out.println("[AcademicService] No school found under Director email: " + directorEmail);
            return DirectorSchoolSummaryDto.builder()
                    .schoolId(null)
                    .schoolName(null)
                    .schoolCode(null)
                    .directorName(null)
                    .directorEmail(directorEmail)
                    .estYear(null)
                    .totalDepartments(0)
                    .assignedHODsCount(0)
                    .unassignedHODsCount(0)
                    .totalProgrammes(0)
                    .build();
        }

        School school = schoolOpt.get();
        List<Department> departments = departmentRepository.findBySchoolId(school.getId());
        List<String> deptIds = departments.stream().map(Department::getId).toList();
        List<Programme> schoolProgrammes = deptIds.isEmpty() ? Collections.emptyList() : programmeRepository.findByDepartmentIdIn(deptIds);

        int assignedHodCount = 0;
        int unassignedHodCount = 0;

        for (Department dept : departments) {
            boolean isHodAssigned = dept.getHod() != null && !dept.getHod().isBlank() && !dept.getHod().equalsIgnoreCase("Unassigned");
            if (isHodAssigned) {
                assignedHodCount++;
            } else {
                unassignedHodCount++;
            }
        }

        String dName = school.getDirectorName() != null && !school.getDirectorName().isBlank()
                ? school.getDirectorName()
                : school.getDirector();

        DirectorSchoolSummaryDto summary = DirectorSchoolSummaryDto.builder()
                .schoolId(school.getId())
                .schoolName(school.getName())
                .schoolCode(school.getCode())
                .directorName(dName)
                .directorEmail(school.getDirectorEmail())
                .estYear(school.getEstYear())
                .totalDepartments(departments.size())
                .assignedHODsCount(assignedHodCount)
                .unassignedHODsCount(unassignedHodCount)
                .totalProgrammes(schoolProgrammes.size())
                .build();

        System.out.println("[AcademicService] Fetched director school summary for school: " + school.getName() + " (ID: " + school.getId() + ")");
        return summary;
    }

    // --- Director Department Summary ---
    @Transactional(readOnly = true)
    public List<DepartmentSummaryDto> getDepartmentSummary(String schoolId, String directorEmail) {
        System.out.println("[AcademicService] getDepartmentSummary called | schoolId: " + schoolId + " | directorEmail: " + directorEmail);
        String targetSchoolId = schoolId;
        if ((targetSchoolId == null || targetSchoolId.isBlank() || targetSchoolId.equals("sch-1")) && directorEmail != null && !directorEmail.isBlank()) {
            Optional<School> schOpt = schoolRepository.findByDirectorEmailIgnoreCase(directorEmail);
            if (schOpt.isPresent()) {
                targetSchoolId = schOpt.get().getId();
            }
        }
        if (targetSchoolId == null || targetSchoolId.isBlank()) {
            targetSchoolId = "sch-1";
        }

        List<Department> departments = departmentRepository.findBySchoolId(targetSchoolId);

        List<DepartmentSummaryDto> list = new ArrayList<>();
        for (Department dept : departments) {
            boolean isHodAssigned = dept.getHod() != null && !dept.getHod().isBlank() && !dept.getHod().equalsIgnoreCase("Unassigned");
            int progsCount = programmeRepository.findByDepartmentId(dept.getId()).size();
            list.add(DepartmentSummaryDto.builder()
                    .deptId(dept.getId())
                    .deptCode(dept.getCode())
                    .deptName(dept.getName())
                    .deptHodName(dept.getHod())
                    .deptHodEmail(dept.getHodEmail())
                    .hodAssignedStatus(isHodAssigned)
                    .programmesCount(progsCount)
                    .build());
        }

        System.out.println("[AcademicService] Fetched department summary list (" + list.size() + " items) for schoolId: " + targetSchoolId);
        return list;
    }

    // --- Director Setup Progress ---
    @Transactional(readOnly = true)
    public DirectorSetupProgressDto getDirectorSetupProgress(String schoolId, String directorEmail) {
        System.out.println("[AcademicService] getDirectorSetupProgress called | schoolId: " + schoolId + " | directorEmail: " + directorEmail);
        String targetSchoolId = schoolId;
        if ((targetSchoolId == null || targetSchoolId.isBlank() || targetSchoolId.equals("sch-1")) && directorEmail != null && !directorEmail.isBlank()) {
            Optional<School> schOpt = schoolRepository.findByDirectorEmailIgnoreCase(directorEmail);
            if (schOpt.isPresent()) {
                targetSchoolId = schOpt.get().getId();
            }
        }
        if (targetSchoolId == null || targetSchoolId.isBlank()) {
            targetSchoolId = "sch-1";
        }

        final String finalSchoolId = targetSchoolId;
        DirectorSetupProgress progress = directorSetupProgressRepository.findBySchoolId(finalSchoolId)
                .orElseGet(() -> DirectorSetupProgress.builder()
                        .id("progress-" + finalSchoolId)
                        .schoolId(finalSchoolId)
                        .currentStep(1)
                        .currentStepEnum(DirectorSetupStep.SCHOOL)
                        .overallStatus(SetupStepStatus.IN_PROGRESS)
                        .completedSteps("")
                        .pendingSteps("school,department,programme,review")
                        .updatedAt(ZonedDateTime.now())
                        .build());

        DirectorSetupProgressDto dto = buildSetupProgressDto(progress);
        System.out.println("[AcademicService] Fetched director setup progress for schoolId: " + finalSchoolId + " at step: " + progress.getCurrentStep() + ", completedSteps: " + dto.getCompletedSteps());
        return dto;
    }

    @Transactional
    public DirectorSetupProgressDto updateDirectorSetupProgress(
            String schoolId,
            Integer stepNumber) {
        System.out.println("[AcademicService] updateDirectorSetupProgress called | schoolId: " + schoolId + " | stepNumber: " + stepNumber);

        String targetSchoolId =
                (schoolId != null && !schoolId.isBlank())
                        ? schoolId
                        : "sch-1";

        DirectorSetupProgress progress =
                directorSetupProgressRepository
                        .findBySchoolId(targetSchoolId)
                        .orElseGet(() -> DirectorSetupProgress.builder()
                                .id("progress-" + targetSchoolId)
                                .schoolId(targetSchoolId)
                                .build());

        // Valid steps: 1 = School, 2 = Department,
        // 3 = Programme, 4 = Review
        int currentStep =
                (stepNumber != null && stepNumber >= 1 && stepNumber <= 4)
                        ? stepNumber
                        : 1;

        DirectorSetupStep stepEnum;

        SetupStepStatus overallStatus = SetupStepStatus.IN_PROGRESS;

        List<String> completed = new ArrayList<>();
        List<String> pending = new ArrayList<>();

        switch (currentStep) {

            // ─────────────────────────────
            // STEP 1 → SCHOOL
            // ─────────────────────────────
            case 1:

                stepEnum = DirectorSetupStep.SCHOOL;

                pending.addAll(List.of(
                        "school",
                        "department",
                        "programme",
                        "review"
                ));

                break;


            // ─────────────────────────────
            // STEP 2 → DEPARTMENT
            // ─────────────────────────────
            case 2:

                stepEnum = DirectorSetupStep.DEPARTMENT;

                completed.add("school");

                pending.addAll(List.of(
                        "department",
                        "programme",
                        "review"
                ));

                break;


            // ─────────────────────────────
            // STEP 3 → PROGRAMME
            // ─────────────────────────────
            case 3:

                stepEnum = DirectorSetupStep.PROGRAMME;

                completed.addAll(List.of(
                        "school",
                        "department"
                ));

                pending.addAll(List.of(
                        "programme",
                        "review"
                ));

                break;


            // ─────────────────────────────
            // STEP 4 → REVIEW & COMPLETE
            // ─────────────────────────────
            case 4:

                stepEnum = DirectorSetupStep.REVIEW;

                completed.addAll(List.of(
                        "school",
                        "department",
                        "programme",
                        "review"
                ));

                overallStatus = SetupStepStatus.COMPLETED;

                break;


            default:
                throw new IllegalArgumentException(
                        "Invalid Director setup step: " + currentStep
                );
        }

        // ─────────────────────────────
        // UPDATE PROGRESS
        // ─────────────────────────────

        progress.setCurrentStep(currentStep);
        progress.setCurrentStepEnum(stepEnum);
        progress.setOverallStatus(overallStatus);

        progress.setCompletedSteps(
                String.join(",", completed)
        );

        progress.setPendingSteps(
                String.join(",", pending)
        );

        progress.setUpdatedAt(ZonedDateTime.now());

        directorSetupProgressRepository.save(progress);

        DirectorSetupProgressDto dto =
                buildSetupProgressDto(progress);

        System.out.println(
                "[AcademicService] Director setup progress updated | " +
                        "schoolId=" + targetSchoolId +
                        " | currentStep=" + currentStep +
                        " | completed=" + completed +
                        " | pending=" + pending
        );

        return dto;
    }

    private DirectorSetupProgressDto buildSetupProgressDto(DirectorSetupProgress progress) {
        List<String> completedList = (progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank())
                ? Arrays.asList(progress.getCompletedSteps().split(","))
                : Collections.emptyList();

        List<String> pendingList = (progress.getPendingSteps() != null && !progress.getPendingSteps().isBlank())
                ? Arrays.asList(progress.getPendingSteps().split(","))
                : Collections.emptyList();

        Map<DirectorSetupStep, SetupStepStatus> stepStatuses = new EnumMap<>(DirectorSetupStep.class);
        int currentStep = progress.getCurrentStep();

        boolean isSchoolDone = completedList.contains("school");
        boolean isDeptDone = completedList.contains("department");
        boolean isProgDone = completedList.contains("programme");
        boolean isReviewDone = completedList.contains("review");

        stepStatuses.put(DirectorSetupStep.SCHOOL, isSchoolDone ? SetupStepStatus.COMPLETED : (currentStep == 1 ? SetupStepStatus.IN_PROGRESS : SetupStepStatus.NOT_STARTED));
        stepStatuses.put(DirectorSetupStep.DEPARTMENT, isDeptDone ? SetupStepStatus.COMPLETED : (currentStep == 2 ? SetupStepStatus.IN_PROGRESS : SetupStepStatus.NOT_STARTED));
        stepStatuses.put(DirectorSetupStep.PROGRAMME, isProgDone ? SetupStepStatus.COMPLETED : (currentStep == 3 ? SetupStepStatus.IN_PROGRESS : SetupStepStatus.NOT_STARTED));
        stepStatuses.put(DirectorSetupStep.REVIEW, isReviewDone ? SetupStepStatus.COMPLETED : SetupStepStatus.NOT_STARTED);

        return DirectorSetupProgressDto.builder()
                .currentStep(progress.getCurrentStep())
                .currentStepEnum(progress.getCurrentStepEnum())
                .overallStatus(progress.getOverallStatus())
                .completedSteps(completedList)
                .pendingSteps(pendingList)
                .stepStatuses(stepStatuses)
                .schoolId(progress.getSchoolId())
                .build();
    }

    // --- Schools ---
    @Transactional(readOnly = true)
    public List<School> getAllSchools() {
        System.out.println("[AcademicService] getAllSchools called");
        List<School> schools = schoolRepository.findAll();
        System.out.println("[AcademicService] Fetched all schools list (" + schools.size() + " items)");
        return schools;
    }

    @Transactional(readOnly = true)
    public School getSchoolById(String id) {
        System.out.println("[AcademicService] getSchoolById called | id: " + id);
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));
        System.out.println("[AcademicService] Fetched school by id: " + id);
        return school;
    }

    @Transactional
    public School saveSchool(School school) {
        System.out.println("[AcademicService] saveSchool called | school: " + (school != null ? school.getName() : "null"));
        if (school == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "School details cannot be null.");
        }

        // 1. Check if directorId is already mapped to another school
        if (school.getDirectorId() != null) {
            Optional<School> existingByDirectorId = schoolRepository.findByDirectorId(school.getDirectorId());
            if (existingByDirectorId.isPresent()) {
                School existing = existingByDirectorId.get();
                if (school.getId() == null || !existing.getId().equalsIgnoreCase(school.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Director is already assigned to School: " + existing.getName() + " (" + existing.getCode() + "). A Director can only manage one school.");
                }
            }
        }

        // 2. Check if directorEmail is already mapped to another school
        if (school.getDirectorEmail() != null && !school.getDirectorEmail().isBlank()) {
            String cleanEmail = school.getDirectorEmail().trim();
            Optional<School> existingByEmail = schoolRepository.findByDirectorEmailIgnoreCase(cleanEmail);
            if (existingByEmail.isPresent()) {
                School existing = existingByEmail.get();
                if (school.getId() == null || !existing.getId().equalsIgnoreCase(school.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Director with email '" + cleanEmail + "' is already assigned to School: " + existing.getName() + " (" + existing.getCode() + "). A Director can only manage one school.");
                }
            }

            // Sync directorId and directorName from User entity if available
            userRepository.findByEmail(cleanEmail).ifPresent(u -> {
                if (school.getDirectorId() == null) {
                    school.setDirectorId(u.getId());
                }
                if (school.getDirectorName() == null || school.getDirectorName().isBlank()) {
                    school.setDirectorName(u.getName());
                }
            });
        }

        if (school.getId() == null || school.getId().isBlank()) {
            school.setId("sch-" + UUID.randomUUID().toString().substring(0, 8));
        }

        School saved = schoolRepository.save(school);

        // Sync schoolId to the Director user in userRepository
        if (saved.getDirectorEmail() != null && !saved.getDirectorEmail().isBlank()) {
            userRepository.findByEmail(saved.getDirectorEmail().trim()).ifPresent(u -> {
                u.setSchoolId(saved.getId());
                userRepository.save(u);
                System.out.println("[AcademicService] Associated director user (" + u.getEmail() + ") with school: " + saved.getId());
            });
        }

        System.out.println("[AcademicService] Saved school with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public School updateSchool(String id, School schoolDetails) {
        System.out.println("[AcademicService] updateSchool called | id: " + id + " | name: " + (schoolDetails != null ? schoolDetails.getName() : "null"));
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));

        // 1. Validate Director ID Uniqueness
        if (schoolDetails.getDirectorId() != null) {
            Optional<School> existingByDirectorId = schoolRepository.findByDirectorId(schoolDetails.getDirectorId());
            if (existingByDirectorId.isPresent() && !existingByDirectorId.get().getId().equalsIgnoreCase(id)) {
                School existing = existingByDirectorId.get();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Director is already assigned to School: " + existing.getName() + " (" + existing.getCode() + "). A Director can only manage one school.");
            }
            school.setDirectorId(schoolDetails.getDirectorId());
        }

        // 2. Validate Director Email Uniqueness
        if (schoolDetails.getDirectorEmail() != null && !schoolDetails.getDirectorEmail().isBlank()) {
            String cleanEmail = schoolDetails.getDirectorEmail().trim();
            Optional<School> existingByEmail = schoolRepository.findByDirectorEmailIgnoreCase(cleanEmail);
            if (existingByEmail.isPresent() && !existingByEmail.get().getId().equalsIgnoreCase(id)) {
                School existing = existingByEmail.get();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Director with email '" + cleanEmail + "' is already assigned to School: " + existing.getName() + " (" + existing.getCode() + "). A Director can only manage one school.");
            }
            school.setDirectorEmail(cleanEmail);

            // Sync directorId and directorName from User entity if available
            userRepository.findByEmail(cleanEmail).ifPresent(u -> {
                if (school.getDirectorId() == null) {
                    school.setDirectorId(u.getId());
                }
                if (schoolDetails.getDirectorName() == null || schoolDetails.getDirectorName().isBlank()) {
                    school.setDirectorName(u.getName());
                }
            });
        }

        if (schoolDetails.getName() != null && !schoolDetails.getName().isBlank()) {
            school.setName(schoolDetails.getName());
        }
        if (schoolDetails.getCode() != null && !schoolDetails.getCode().isBlank()) {
            school.setCode(schoolDetails.getCode());
        }
        if (schoolDetails.getDirectorName() != null) {
            school.setDirectorName(schoolDetails.getDirectorName());
        } else if (schoolDetails.getDirector() != null) {
            school.setDirector(schoolDetails.getDirector());
        }
        if (schoolDetails.getEstYear() != null) {
            school.setEstYear(schoolDetails.getEstYear());
        }

        School updated = schoolRepository.save(school);

        // Sync schoolId to the Director user in userRepository
        if (updated.getDirectorEmail() != null && !updated.getDirectorEmail().isBlank()) {
            userRepository.findByEmail(updated.getDirectorEmail().trim()).ifPresent(u -> {
                u.setSchoolId(updated.getId());
                userRepository.save(u);
            });
        }

        System.out.println("[AcademicService] Updated school info for id: " + updated.getId());
        return updated;
    }

    // --- Departments ---
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        System.out.println("[AcademicService] getAllDepartments called");
        List<Department> list = departmentRepository.findAll();
        System.out.println("[AcademicService] Fetched all departments (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public List<Department> getDepartmentsBySchool(String schoolId) {
        System.out.println("[AcademicService] getDepartmentsBySchool called | schoolId: " + schoolId);
        List<Department> list = departmentRepository.findBySchoolId(schoolId);
        System.out.println("[AcademicService] Fetched departments (" + list.size() + " items) for schoolId: " + schoolId);
        return list;
    }

    @Transactional
    public Department saveDepartment(Department department) {
        System.out.println("[AcademicService] saveDepartment called | department: " + (department != null ? department.getName() : "null"));
        if (department.getId() == null) department.setId("dept-" + UUID.randomUUID().toString().substring(0, 8));
        Department saved = departmentRepository.save(department);
        System.out.println("[AcademicService] Saved department with id: " + saved.getId());

        // Sync department info to HOD user if hodEmail or hod name matches
        if (saved.getHodEmail() != null && !saved.getHodEmail().isBlank()) {
            userRepository.findByEmail(saved.getHodEmail()).ifPresent(user -> {
                user.setDepartment(saved.getName());
                userRepository.save(user);
                System.out.println("[AcademicService] Updated HOD user (" + user.getEmail() + ") department to: " + saved.getName());
            });
        }
        return saved;
    }

    @Transactional
    public void deleteDepartment(String id) {
        System.out.println("[AcademicService] deleteDepartment called | id: " + id);
        departmentRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted department with id: " + id);
    }

    // --- Users by Role ---
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(String role) {
        System.out.println("[AcademicService] getUsersByRole called | role: " + role);
        List<User> users;

        if (role == null || role.isBlank() || role.equalsIgnoreCase("ALL")) {
            users = userRepository.findAll();
        } else {
            String searchRole = role.trim().toUpperCase().replace("-", "_");
            if (searchRole.equals("PROGRAMME_COORDINATOR")
                    || searchRole.equals("COORDINATOR")
                    || searchRole.equals("PC")
                    || searchRole.equals("PROGRAMME_COORD")) {
                users = userRepository.findByRole(UserRole.PROGRAMME_COORDINATOR);
            } else if (searchRole.equals("COURSE_COORDINATOR")
                    || searchRole.equals("CC")
                    || searchRole.equals("FACULTY")) {
                users = userRepository.findByRole(UserRole.FACULTY);
            } else if (searchRole.equals("HOD")) {
                users = userRepository.findByRole(UserRole.HOD);
            } else if (searchRole.equals("DIRECTOR")) {
                users = userRepository.findByRole(UserRole.DIRECTOR);
            } else if (searchRole.equals("IQAC")) {
                users = userRepository.findByRole(UserRole.IQAC);
            } else {
                try {
                    UserRole userRole = UserRole.valueOf(searchRole);
                    users = userRepository.findByRole(userRole);
                } catch (IllegalArgumentException e) {
                    users = userRepository.findAll();
                }
            }
        }

        List<UserDto> dtos = users.stream()
                .map(u -> {
                    String resolvedEmail = u.getEmail();
                    if (resolvedEmail == null || resolvedEmail.isBlank()) {
                        if (u.getUsername() != null && u.getUsername().contains("@")) {
                            resolvedEmail = u.getUsername();
                        } else if (u.getUsername() != null && !u.getUsername().isBlank()) {
                            resolvedEmail = u.getUsername() + "@dypiu.ac.in";
                        } else {
                            resolvedEmail = "user" + u.getId() + "@dypiu.ac.in";
                        }
                    }
                    return UserDto.builder()
                            .id(u.getId())
                            .name(u.getName() != null && !u.getName().isBlank() ? u.getName() : (u.getUsername() != null ? u.getUsername() : "User " + u.getId()))
                            .email(resolvedEmail)
                            .role(u.getRole() != null
                                    ? u.getRole().name()
                                    : UserRole.FACULTY.name())
                            .department(u.getDepartment())
                            .programme(u.getProgramme())
                            .build();
                })
                .toList();

        System.out.println("[AcademicService] Fetched users by role (" + role + "): count=" + dtos.size());
        return dtos;
    }

    // --- Programmes ---
    @Transactional(readOnly = true)
    private void enrichProgrammeCoordinator(Programme programme) {
        if (programme == null) return;
        String coord = programme.getCoordinator();
        String coordEmail = programme.getCoordinatorEmail();

        // 1. If coordinatorEmail is valid, lookup user by email
        if (coordEmail != null && !coordEmail.isBlank() && coordEmail.contains("@")) {
            Optional<User> uOpt = userRepository.findByEmail(coordEmail.trim());
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                programme.setCoordinator(u.getName());
                programme.setCoordinatorEmail(u.getEmail());
                return;
            }
        }

        // 2. Lookup by coordinator string (ID, email, username, name)
        if (coord != null && !coord.isBlank()) {
            if (coord.matches("\\d+")) {
                try {
                    Long userId = Long.parseLong(coord);
                    userRepository.findById(userId).ifPresent(u -> {
                        programme.setCoordinator(u.getName());
                        if (programme.getCoordinatorEmail() == null || programme.getCoordinatorEmail().isBlank()) {
                            programme.setCoordinatorEmail(u.getEmail());
                        }
                    });
                } catch (NumberFormatException ignored) {}
            } else if (coord.contains("@")) {
                userRepository.findByEmail(coord.trim()).ifPresent(u -> {
                    programme.setCoordinator(u.getName());
                    programme.setCoordinatorEmail(u.getEmail());
                });
            } else {
                Optional<User> uOpt = userRepository.findByUsername(coord.trim());
                if (uOpt.isEmpty()) {
                    uOpt = userRepository.findAll().stream()
                            .filter(u -> u.getName() != null && u.getName().trim().equalsIgnoreCase(coord.trim()))
                            .findFirst();
                }
                if (uOpt.isPresent()) {
                    User u = uOpt.get();
                    programme.setCoordinator(u.getName());
                    if (programme.getCoordinatorEmail() == null || programme.getCoordinatorEmail().isBlank()) {
                        programme.setCoordinatorEmail(u.getEmail());
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public CourseCoordinatorSummaryDto getCourseCoordinatorSummary(String coordinatorEmail) {
        System.out.println("[AcademicService] getCourseCoordinatorSummary called | coordinatorEmail: " + coordinatorEmail);
        String name = "Course Coordinator";
        String email = coordinatorEmail != null ? coordinatorEmail.trim() : "";

        if (!email.isBlank()) {
            Optional<User> uOpt = userRepository.findByEmail(email);
            if (uOpt.isPresent()) {
                name = uOpt.get().getName();
            }
        }

        List<Course> allCourses = courseRepository.findAll();
        final String searchEmail = email.toLowerCase();
        final String searchName = name.toLowerCase();

        List<Course> assigned = allCourses.stream()
                .filter(c -> {
                    if (searchEmail.isBlank()) return true;
                    boolean matchCoord = (c.getCoordinator() != null && (c.getCoordinator().toLowerCase().contains(searchEmail) || (!searchName.isBlank() && c.getCoordinator().toLowerCase().contains(searchName))));
                    boolean matchFaculty = (c.getFaculty() != null && (c.getFaculty().toLowerCase().contains(searchEmail) || (!searchName.isBlank() && c.getFaculty().toLowerCase().contains(searchName))));
                    boolean matchAssigned = (c.getAssignedFaculty() != null && (c.getAssignedFaculty().toLowerCase().contains(searchEmail) || (!searchName.isBlank() && c.getAssignedFaculty().toLowerCase().contains(searchName))));
                    return matchCoord || matchFaculty || matchAssigned;
                })
                .toList();

        List<Course> finalCourses = assigned.isEmpty() ? allCourses : assigned;

        Course primaryCourse = !finalCourses.isEmpty() ? finalCourses.get(0) : null;

        CourseCoordinatorSetupProgressDto setupProgress = null;
        int coCount = 0;
        int poCount = 0;
        int psoCount = 0;

        if (primaryCourse != null) {
            setupProgress = getCourseCoordinatorSetupProgress(email, primaryCourse.getId());

            List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(primaryCourse.getId());
            String offeringId = !offerings.isEmpty() ? offerings.get(0).getId() : primaryCourse.getId();
            coCount = courseOutcomeRepository.findByCourseOfferingId(offeringId).size();

            String programmeId = primaryCourse.getProgrammeId();
            if (programmeId != null && !programmeId.isBlank()) {
                poCount = programmeOutcomeRepository.findByProgrammeId(programmeId).size();
                psoCount = programmeSpecificOutcomeRepository.findByProgrammeId(programmeId).size();
            }
        }

        if (poCount == 0) poCount = 12;
        if (psoCount == 0) psoCount = 2;

        return CourseCoordinatorSummaryDto.builder()
                .coordinatorName(name)
                .coordinatorEmail(email)
                .assignedCourseCount(finalCourses.size())
                .assignedCourses(finalCourses)
                .setupProgress(setupProgress)
                .courseOutcomesCount(coCount)
                .poCount(poCount)
                .psoCount(psoCount)
                .build();
    }

    private String resolveTargetCourseId(String courseId) {
        if (courseId != null && !courseId.isBlank() && courseRepository.existsById(courseId)) {
            return courseId;
        }
        return courseId;
    }

    @Transactional(readOnly = true)
    public CourseCoordinatorSetupProgressDto getCourseCoordinatorSetupProgress(String coordinatorEmail, String courseId) {
        String targetCourseId = resolveTargetCourseId(courseId);
        System.out.println("[AcademicService] getCourseCoordinatorSetupProgress called | courseId: " + courseId + " -> targetCourseId: " + targetCourseId);
        CourseCoordinatorSetupProgress progress = ccSetupProgressRepository.findByCourseOfferingId(targetCourseId)
                .orElseGet(() -> CourseCoordinatorSetupProgress.builder()
                        .id("ccprog-" + UUID.randomUUID().toString().substring(0, 8))
                        .courseOfferingId(targetCourseId)
                        .coordinatorEmail(coordinatorEmail)
                        .currentStep(1)
                        .overallStatus(SetupStepStatus.IN_PROGRESS)
                        .completedSteps("")
                        .pendingSteps("cos,co_mapping,direct,indirect,attainment,course_atr")
                        .updatedAt(ZonedDateTime.now())
                        .build());

        List<String> completed = (progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank())
                ? Arrays.asList(progress.getCompletedSteps().split(","))
                : Collections.emptyList();
        List<String> pending = (progress.getPendingSteps() != null && !progress.getPendingSteps().isBlank())
                ? Arrays.asList(progress.getPendingSteps().split(","))
                : Collections.emptyList();

        return CourseCoordinatorSetupProgressDto.builder()
                .id(progress.getId())
                .courseId(progress.getCourseId())
                .currentStep(progress.getCurrentStep())
                .completedSteps(completed)
                .pendingSteps(pending)
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    @Transactional
    public CourseCoordinatorSetupProgressDto updateCourseCoordinatorSetupProgress(String coordinatorEmail, String courseId, Integer currentStep) {
        String targetCourseId = resolveTargetCourseId(courseId);
        System.out.println("[AcademicService] updateCourseCoordinatorSetupProgress called | courseId: " + courseId + " -> targetCourseId: " + targetCourseId + " | stepNumber: " + currentStep);
        CourseCoordinatorSetupProgress progress = ccSetupProgressRepository.findByCourseOfferingId(targetCourseId)
                .orElseGet(() -> CourseCoordinatorSetupProgress.builder()
                        .id("ccprog-" + UUID.randomUUID().toString().substring(0, 8))
                        .courseOfferingId(targetCourseId)
                        .coordinatorEmail(coordinatorEmail)
                        .currentStep(1)
                        .overallStatus(SetupStepStatus.IN_PROGRESS)
                        .completedSteps("")
                        .pendingSteps("cos,co_mapping,direct,indirect,attainment,course_atr")
                        .build());

        progress.setCurrentStep(currentStep != null ? currentStep : 1);
        if (coordinatorEmail != null && !coordinatorEmail.isBlank()) {
            progress.setCoordinatorEmail(coordinatorEmail);
        }
        progress.setUpdatedAt(ZonedDateTime.now());
        ccSetupProgressRepository.save(progress);
        return getCourseCoordinatorSetupProgress(coordinatorEmail, targetCourseId);
    }

    @Transactional
    public CourseCoordinatorSetupProgressDto completeCourseCoordinatorSetup(String coordinatorEmail, String courseId) {
        String targetCourseId = resolveTargetCourseId(courseId);
        System.out.println("[AcademicService] completeCourseCoordinatorSetup called | courseId: " + courseId + " -> targetCourseId: " + targetCourseId);
        CourseCoordinatorSetupProgress progress = ccSetupProgressRepository.findByCourseOfferingId(targetCourseId)
                .orElseGet(() -> CourseCoordinatorSetupProgress.builder()
                        .id("ccprog-" + UUID.randomUUID().toString().substring(0, 8))
                        .courseOfferingId(targetCourseId)
                        .coordinatorEmail(coordinatorEmail)
                        .build());


        progress.setOverallStatus(SetupStepStatus.COMPLETED);
        progress.setCompletedSteps("cos,co_mapping,direct,indirect,attainment,course_atr");
        progress.setPendingSteps("");
        progress.setUpdatedAt(ZonedDateTime.now());
        ccSetupProgressRepository.save(progress);
        return getCourseCoordinatorSetupProgress(coordinatorEmail, targetCourseId);
    }

    @Transactional(readOnly = true)
    public List<Programme> getAllProgrammes() {
        System.out.println("[AcademicService] getAllProgrammes called");
        List<Programme> list = programmeRepository.findAll();
        list.forEach(this::enrichProgrammeCoordinator);
        System.out.println("[AcademicService] Fetched all programmes (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public Programme getProgrammeById(String id) {
        System.out.println("[AcademicService] getProgrammeById called | id: " + id);
        if (id == null || id.isBlank()) return null;
        Programme p = programmeRepository.findById(id).orElse(null);
        if (p != null) {
            enrichProgrammeCoordinator(p);
        }
        return p;
    }

    @Transactional(readOnly = true)
    public List<Programme> getProgrammesByCoordinatorEmail(String coordinatorEmail) {
        System.out.println("[AcademicService] getProgrammesByCoordinatorEmail called | coordinatorEmail: " + coordinatorEmail);
        if (coordinatorEmail == null || coordinatorEmail.isBlank()) {
            return getAllProgrammes();
        }
        String emailTrim = coordinatorEmail.trim().toLowerCase();
        List<Programme> all = getAllProgrammes();
        List<Programme> filtered = all.stream()
                .filter(p -> (p.getCoordinatorEmail() != null && emailTrim.equalsIgnoreCase(p.getCoordinatorEmail().trim()))
                        || (p.getCoordinator() != null && emailTrim.equalsIgnoreCase(p.getCoordinator().trim())))
                .toList();
        if (filtered.isEmpty()) {
            System.out.println("[AcademicService] No exact match for coordinatorEmail: " + coordinatorEmail + ", returning all " + all.size() + " programmes.");
            return all;
        }
        System.out.println("[AcademicService] Found " + filtered.size() + " programmes for coordinatorEmail: " + coordinatorEmail);
        return filtered;
    }

    @Transactional(readOnly = true)
    public List<Programme> getProgrammesBySchool(String schoolId) {
        System.out.println("[AcademicService] getProgrammesBySchool called | schoolId: " + schoolId);
        List<Department> depts = departmentRepository.findBySchoolId(schoolId);
        if (depts == null || depts.isEmpty()) {
            List<Programme> list = programmeRepository.findAll();
            list.forEach(this::enrichProgrammeCoordinator);
            return list;
        }
        List<String> deptIds = depts.stream().map(Department::getId).toList();
        List<Programme> list = programmeRepository.findByDepartmentIdIn(deptIds);
        list.forEach(this::enrichProgrammeCoordinator);
        System.out.println("[AcademicService] Fetched programmes (" + list.size() + " items) for schoolId: " + schoolId);
        return list;
    }

    @Transactional(readOnly = true)
    public List<Programme> getProgrammesByDepartment(String departmentId) {
        System.out.println("[AcademicService] getProgrammesByDepartment called | departmentId: " + departmentId);
        if (departmentId == null || departmentId.isBlank()) {
            return getAllProgrammes();
        }
        List<Programme> list = programmeRepository.findByDepartmentId(departmentId);
        list.forEach(this::enrichProgrammeCoordinator);
        System.out.println("[AcademicService] Fetched programmes (" + list.size() + " items) for departmentId: " + departmentId);
        return list;
    }

    @Transactional
    public Programme saveProgramme(Programme programme) {
        System.out.println("[AcademicService] saveProgramme called | id: " + (programme != null ? programme.getId() : "null") + " | name: " + (programme != null ? programme.getName() : "null") + " | coordinator: " + (programme != null ? programme.getCoordinator() : "null") + " | coordinatorEmail: " + (programme != null ? programme.getCoordinatorEmail() : "null"));
        if (programme == null) return null;

        Programme targetProg = programme;
        if (programme.getId() != null) {
            Optional<Programme> existingOpt = programmeRepository.findById(programme.getId());
            if (existingOpt.isPresent()) {
                Programme existing = existingOpt.get();
                if (programme.getName() != null) existing.setName(programme.getName());
                if (programme.getCode() != null) existing.setCode(programme.getCode());
                if (programme.getDepartmentId() != null) existing.setDepartmentId(programme.getDepartmentId());
                if (programme.getDurationYears() != null) existing.setDurationYears(programme.getDurationYears());
                if (programme.getStatus() != null) existing.setStatus(programme.getStatus());
                if (programme.getDepartmentName() != null) existing.setDepartmentName(programme.getDepartmentName());
                if (programme.getCoordinator() != null) existing.setCoordinator(programme.getCoordinator());
                if (programme.getCoordinatorEmail() != null) existing.setCoordinatorEmail(programme.getCoordinatorEmail());
                targetProg = existing;
            }
        } else {
            targetProg.setId("prog-" + UUID.randomUUID().toString().substring(0, 8));
        }

        if (programme.getCoordinator() != null && !programme.getCoordinator().isBlank()) {
            targetProg.setCoordinator(programme.getCoordinator());
        }
        if (programme.getCoordinatorEmail() != null && !programme.getCoordinatorEmail().isBlank()) {
            targetProg.setCoordinatorEmail(programme.getCoordinatorEmail());
        }

        final Programme finalProg = targetProg;
        enrichProgrammeCoordinator(finalProg);

        // Populate department name if missing
        if ((finalProg.getDepartmentName() == null || finalProg.getDepartmentName().isBlank()) && finalProg.getDepartmentId() != null) {
            departmentRepository.findById(finalProg.getDepartmentId()).ifPresent(d -> finalProg.setDepartmentName(d.getName()));
        }

        // Bidirectionally synchronize user record if coordinator assigned
        String coordEmail = finalProg.getCoordinatorEmail();
        if (coordEmail != null && !coordEmail.isBlank()) {
            Optional<User> userOpt = userRepository.findByEmail(coordEmail.trim());
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(coordEmail.trim());
            }
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setProgrammeId(finalProg.getId());
                user.setProgramme(finalProg.getName());
                if (finalProg.getDepartmentId() != null) {
                    user.setDepartmentId(finalProg.getDepartmentId());
                }
                if (user.getRole() == UserRole.FACULTY) {
                    user.setRole(UserRole.PROGRAMME_COORDINATOR);
                }
                userRepository.save(user);
                System.out.println("[AcademicService] Synchronized user " + user.getEmail() + " as PC for programme " + finalProg.getName());
            }
        }

        Programme saved = programmeRepository.save(finalProg);
        System.out.println("[AcademicService] Saved programme with id: " + saved.getId() + ", coordinator: " + saved.getCoordinator() + ", coordinatorEmail: " + saved.getCoordinatorEmail());
        return saved;
    }

    @Transactional
    public void deleteProgramme(String id) {
        System.out.println("[AcademicService] deleteProgramme called | id: " + id);
        programmeRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted programme with id: " + id);
    }

    // --- Batches ---
    @Transactional(readOnly = true)
    public List<Batch> getAllBatches() {
        System.out.println("[AcademicService] getAllBatches called");
        List<Batch> list = batchRepository.findAll();
        System.out.println("[AcademicService] Fetched all batches (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public List<Batch> getBatchesByProgramme(String programmeId) {
        System.out.println("[AcademicService] getBatchesByProgramme called | programmeId: " + programmeId);
        List<Batch> list = batchRepository.findByProgrammeId(programmeId);
        System.out.println("[AcademicService] Fetched batches (" + list.size() + " items) for programmeId: " + programmeId);
        return list;
    }

    @Transactional(readOnly = true)
    public List<Batch> getBatchesScoped(String programmeId, String userEmail, String role) {
        System.out.println("================================================================================");
        System.out.println("[AcademicService] >>> getBatchesScoped | programmeId: " + programmeId + " | userEmail: " + userEmail + " | role: " + role);

        // 1. Explicit programme filter
        if (programmeId != null && !programmeId.isBlank()) {
            return getBatchesByProgramme(programmeId);
        }

        // 2. Unrestricted roles or missing parameters
        if (userEmail == null || userEmail.isBlank() || role == null || role.isBlank() ||
                "DIRECTOR".equalsIgnoreCase(role) || "SCHOOL_DIRECTOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            List<Batch> all = getAllBatches();
            System.out.println("  [DIRECTOR / ALL ACCESS]: Returning all " + all.size() + " batches across school.");
            return all;
        }

        String emailTrim = userEmail != null ? userEmail.trim().toLowerCase() : "";
        String userName = "";
        if (userEmail != null && !userEmail.isBlank()) {
            Optional<User> uOpt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(userEmail.trim(), userEmail.trim());
            if (uOpt.isEmpty()) {
                uOpt = userRepository.findByEmail(userEmail.trim());
            }
            if (uOpt.isEmpty()) {
                uOpt = userRepository.findByUsername(userEmail.trim());
            }
            if (uOpt.isEmpty()) {
                uOpt = userRepository.findAll().stream()
                        .filter(u -> u.getName() != null && u.getName().trim().equalsIgnoreCase(userEmail.trim()))
                        .findFirst();
            }
            if (uOpt.isPresent()) {
                User u = uOpt.get();
                if (u.getEmail() != null) emailTrim = u.getEmail().trim().toLowerCase();
                if (u.getName() != null) userName = u.getName().trim().toLowerCase();
                if ((role == null || role.isBlank()) && u.getRole() != null) {
                    role = u.getRole().name();
                }
            }
        }

        final String finalEmailTrim = emailTrim;
        final String searchName = userName;

        // 3. COURSE COORDINATOR (FACULTY): Only batches of the programmes for assigned courses
        if ("FACULTY".equalsIgnoreCase(role) || "COURSE_COORDINATOR".equalsIgnoreCase(role)) {
            List<Course> allCourses = courseRepository.findAll();
            Set<String> assignedProgIds = new HashSet<>();

            for (Course c : allCourses) {
                boolean matchCoord = (c.getCoordinator() != null && (c.getCoordinator().toLowerCase().contains(finalEmailTrim) || (!searchName.isBlank() && c.getCoordinator().toLowerCase().contains(searchName))));
                boolean matchFaculty = (c.getFaculty() != null && (c.getFaculty().toLowerCase().contains(finalEmailTrim) || (!searchName.isBlank() && c.getFaculty().toLowerCase().contains(searchName))));
                boolean matchAssigned = (c.getAssignedFaculty() != null && (c.getAssignedFaculty().toLowerCase().contains(finalEmailTrim) || (!searchName.isBlank() && c.getAssignedFaculty().toLowerCase().contains(searchName))));

                if ((matchCoord || matchFaculty || matchAssigned) && c.getProgrammeId() != null && !c.getProgrammeId().isBlank()) {
                    assignedProgIds.add(c.getProgrammeId());
                }
            }

            if (!assignedProgIds.isEmpty()) {
                List<Batch> filtered = batchRepository.findAll().stream()
                        .filter(b -> assignedProgIds.contains(b.getProgrammeId()))
                        .collect(Collectors.toList());
                System.out.println("  [COURSE COORDINATOR SCOPE]: Found " + filtered.size() + " batches for assigned programme IDs: " + assignedProgIds);
                if (!filtered.isEmpty()) return filtered;
            }
            System.out.println("  [COURSE COORDINATOR SCOPE]: No specific programme batches matched, returning fallback all batches.");
            return getAllBatches();
        }

        // 4. PROGRAMME COORDINATOR: Batches under all assigned programmes
        if ("PROGRAMME_COORDINATOR".equalsIgnoreCase(role)) {
            List<Programme> assignedProgs = getProgrammesByCoordinatorEmail(finalEmailTrim);
            Set<String> progIds = assignedProgs.stream().map(Programme::getId).collect(Collectors.toSet());

            if (!progIds.isEmpty()) {
                List<Batch> filtered = batchRepository.findAll().stream()
                        .filter(b -> progIds.contains(b.getProgrammeId()))
                        .collect(Collectors.toList());
                System.out.println("  [PROGRAMME COORDINATOR SCOPE]: Found " + filtered.size() + " batches for assigned programmes: " + progIds);
                if (!filtered.isEmpty()) return filtered;
            }
            return getAllBatches();
        }

        // 5. HOD: Batches under all programmes belonging to their Department
        if ("HOD".equalsIgnoreCase(role)) {
            List<Department> allDepts = departmentRepository.findAll();
            Department hodDept = allDepts.stream()
                    .filter(d -> (d.getHodEmail() != null && d.getHodEmail().equalsIgnoreCase(finalEmailTrim))
                            || (d.getHod() != null && (d.getHod().toLowerCase().contains(finalEmailTrim) || (!searchName.isBlank() && d.getHod().toLowerCase().contains(searchName)))))
                    .findFirst()
                    .orElse(null);

            if (hodDept != null) {
                List<Programme> deptProgs = programmeRepository.findByDepartmentId(hodDept.getId());
                Set<String> progIds = deptProgs.stream().map(Programme::getId).collect(Collectors.toSet());

                if (!progIds.isEmpty()) {
                    List<Batch> filtered = batchRepository.findAll().stream()
                            .filter(b -> progIds.contains(b.getProgrammeId()))
                            .collect(Collectors.toList());
                    System.out.println("  [HOD SCOPE]: Found " + filtered.size() + " batches under department '" + hodDept.getName() + "' (progIds: " + progIds + ")");
                    if (!filtered.isEmpty()) return filtered;
                }
            }
            return getAllBatches();
        }

        return getAllBatches();
    }

    @Transactional
    public Batch saveBatch(Batch batch) {
        System.out.println("[AcademicService] saveBatch called | name: " + (batch != null ? batch.getName() : "null"));
        if (batch.getId() == null) batch.setId("batch-" + UUID.randomUUID().toString().substring(0, 8));
        Batch saved = batchRepository.save(batch);
        System.out.println("[AcademicService] Saved batch with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteBatch(String id) {
        System.out.println("[AcademicService] deleteBatch called | id: " + id);
        batchRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted batch with id: " + id);
    }

    // --- Courses ---
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        System.out.println("[AcademicService] getAllCourses called");
        List<Course> list = courseRepository.findAll();
        System.out.println("[AcademicService] Fetched all courses (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public Course getCourseById(String id) {
        System.out.println("[AcademicService] getCourseById called | id: " + id);
        return courseRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByProgramme(String programmeId) {

        System.out.println("[AcademicService] getCoursesByProgramme called | programmeId: " + programmeId);
        List<Course> list = courseRepository.findByProgrammeId(programmeId);
        System.out.println("[AcademicService] Fetched courses (" + list.size() + " items) for programmeId: " + programmeId);
        return list;
    }

    @Transactional
    public Course saveCourse(Course course) {
        System.out.println("[AcademicService] saveCourse called | name: " + (course != null ? course.getName() : "null"));
        if (course.getId() == null) course.setId("crs-" + UUID.randomUUID().toString().substring(0, 8));
        Course saved = courseRepository.save(course);
        System.out.println("[AcademicService] Saved course with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteCourse(String id) {
        System.out.println("[AcademicService] deleteCourse called | id: " + id);
        courseRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted course with id: " + id);
    }

    // --- Students ---
    @Transactional(readOnly = true)
    public List<Student> getStudentsByBatch(String batchId) {
        System.out.println("[AcademicService] getStudentsByBatch called | batchId: " + batchId);
        List<Student> list = studentRepository.findByBatchId(batchId);
        System.out.println("[AcademicService] Fetched students (" + list.size() + " items) for batchId: " + batchId);
        return list;
    }

    @Transactional
    public Student saveStudent(Student student) {
        System.out.println("[AcademicService] saveStudent called | name: " + (student != null ? student.getName() : "null"));
        if (student.getId() == null) student.setId("std-" + UUID.randomUUID().toString().substring(0, 8));
        Student saved = studentRepository.save(student);
        System.out.println("[AcademicService] Saved student with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteStudent(String id) {
        System.out.println("[AcademicService] deleteStudent called | id: " + id);
        studentRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted student with id: " + id);
    }

    // --- HOD Department Summary ---
    @Transactional(readOnly = true)
    public HodDepartmentSummaryDto getHodDepartmentSummary(String hodEmail) {
        System.out.println("[AcademicService] Starting HOD department summary fetch for hodEmail: " + hodEmail);

        Department dept = null;

        if (hodEmail != null && !hodEmail.isBlank()) {
            String search = hodEmail.trim();

            // 1. First search department repository by hodEmail
            List<Department> deptList = departmentRepository.findByHodEmailIgnoreCase(search);
            if (!deptList.isEmpty()) {
                dept = deptList.get(0);
            }

            // 2. Search department by HOD display name
            if (dept == null) {
                List<Department> byHod = departmentRepository.findAll().stream()
                        .filter(d -> d.getHod() != null && d.getHod().trim().equalsIgnoreCase(search))
                        .toList();
                if (!byHod.isEmpty()) {
                    dept = byHod.get(0);
                }
            }

            // 3. Look up User by username, email, or name
            if (dept == null) {
                Optional<User> userOpt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(search, search);
                if (userOpt.isEmpty()) {
                    userOpt = userRepository.findByEmail(search);
                }
                if (userOpt.isEmpty()) {
                    userOpt = userRepository.findByUsername(search);
                }
                if (userOpt.isEmpty()) {
                    userOpt = userRepository.findAll().stream()
                            .filter(u -> u.getName() != null && u.getName().trim().equalsIgnoreCase(search))
                            .findFirst();
                }

                if (userOpt.isPresent()) {
                    User u = userOpt.get();
                    if (u.getDepartmentId() != null && !u.getDepartmentId().isBlank()) {
                        dept = departmentRepository.findById(u.getDepartmentId()).orElse(null);
                    }
                    if (dept == null && u.getDepartment() != null && !u.getDepartment().isBlank()) {
                        String userDeptName = u.getDepartment().trim();
                        List<Department> namedDepts = departmentRepository.findByName(userDeptName);
                        if (!namedDepts.isEmpty()) {
                            dept = namedDepts.get(0);
                        } else {
                            List<Department> matches = departmentRepository.findAll().stream()
                                    .filter(d -> d.getName() != null && d.getName().trim().equalsIgnoreCase(userDeptName))
                                    .toList();
                            if (!matches.isEmpty()) {
                                dept = matches.get(0);
                            }
                        }
                    }
                }
            }
        }

        // 4. Default fallback to first department in database
        if (dept == null) {
            List<Department> allDepts = departmentRepository.findAll();
            if (!allDepts.isEmpty()) {
                dept = allDepts.get(0);
            }
        }

        if (dept == null) {
            System.out.println("[AcademicService] No department found in database for hodEmail: " + hodEmail);
            return HodDepartmentSummaryDto.builder()
                    .deptId(null)
                    .deptCode(null)
                    .deptName(null)
                    .hodName(null)
                    .hodEmail(hodEmail)
                    .schoolId(null)
                    .schoolName(null)
                    .programmeCount(0)
                    .assignedCoordinatorsCount(0)
                    .courseCount(0)
                    .setupProgress(getHodSetupProgress(null, hodEmail))
                    .build();
        }

        String deptId = dept.getId();
        String deptName = dept.getName();
        String deptCode = dept.getCode();
        String resolvedHodName = dept.getHod();
        String resolvedHodEmail = (dept.getHodEmail() != null && !dept.getHodEmail().isBlank()) ? dept.getHodEmail() : hodEmail;
        String schoolId = dept.getSchoolId();

        // School info
        String schoolName = "School of Engineering and Technology";
        if (schoolId != null) {
            Optional<School> schOpt = schoolRepository.findById(schoolId);
            if (schOpt.isPresent()) {
                schoolName = schOpt.get().getName();
            }
        }

        // Programmes under department
        List<Programme> programmes = programmeRepository.findByDepartmentId(deptId);
        int programmeCount = programmes.size();

        // Count assigned coordinators
        int assignedCoordinatorsCount = (int) programmes.stream()
                .filter(p -> (p.getCoordinator() != null && !p.getCoordinator().isBlank() && !"Unassigned".equalsIgnoreCase(p.getCoordinator()) && !"No coordinator assigned yet".equalsIgnoreCase(p.getCoordinator()) && !"Pending HOD Assignment".equalsIgnoreCase(p.getCoordinator())) || (p.getCoordinatorEmail() != null && !p.getCoordinatorEmail().isBlank()))
                .count();

        // Courses under department's programmes
        List<String> progIds = programmes.stream().map(Programme::getId).toList();
        int courseCount = 0;
        if (!progIds.isEmpty()) {
            courseCount = courseRepository.findByProgrammeIdIn(progIds).size();
        } else {
            courseCount = courseRepository.findAll().size();
        }

        HodSetupProgressDto progressDto = getHodSetupProgress(deptId, resolvedHodEmail);
        System.out.println("[AcademicService] Fetched HOD department summary for deptId: " + deptId + " (" + deptName + ") | hodEmail: " + resolvedHodEmail);

        return HodDepartmentSummaryDto.builder()
                .deptId(deptId)
                .deptCode(deptCode)
                .deptName(deptName)
                .hodName(resolvedHodName)
                .hodEmail(resolvedHodEmail)
                .schoolId(schoolId)
                .schoolName(schoolName)
                .programmeCount(programmeCount)
                .assignedCoordinatorsCount(assignedCoordinatorsCount)
                .courseCount(courseCount)
                .setupProgress(progressDto)
                .build();
    }

    private String resolveTargetDeptId(String departmentId, String hodEmail) {
        System.out.println("[AcademicService] resolveTargetDeptId called | departmentId: " + departmentId + " | hodEmail: " + hodEmail);
        String targetDeptId = departmentId;
        if (targetDeptId != null && !targetDeptId.isBlank() && !targetDeptId.contains("@") && !targetDeptId.equals("null")) {
            return targetDeptId;
        }

        String search = (hodEmail != null && !hodEmail.isBlank() && !hodEmail.equals("null"))
                ? hodEmail.trim()
                : (targetDeptId != null && !targetDeptId.equals("null") ? targetDeptId.trim() : null);

        if (search != null && !search.isBlank()) {
            // 1. Search by hodEmail
            List<Department> deptList = departmentRepository.findByHodEmailIgnoreCase(search);
            if (!deptList.isEmpty()) {
                return deptList.get(0).getId();
            }

            // 2. Search by HOD display name
            List<Department> byHod = departmentRepository.findAll().stream()
                    .filter(d -> d.getHod() != null && d.getHod().trim().equalsIgnoreCase(search))
                    .toList();
            if (!byHod.isEmpty()) {
                return byHod.get(0).getId();
            }

            // 3. Search user by username, email, or name
            Optional<User> userOpt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(search, search);
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(search);
            }
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByUsername(search);
            }
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findAll().stream()
                        .filter(u -> u.getName() != null && u.getName().trim().equalsIgnoreCase(search))
                        .findFirst();
            }

            if (userOpt.isPresent()) {
                User u = userOpt.get();
                if (u.getDepartmentId() != null && !u.getDepartmentId().isBlank()) {
                    return u.getDepartmentId();
                }
                if (u.getDepartment() != null && !u.getDepartment().isBlank()) {
                    String userDeptName = u.getDepartment().trim();
                    List<Department> namedDepts = departmentRepository.findByName(userDeptName);
                    if (!namedDepts.isEmpty()) {
                        return namedDepts.get(0).getId();
                    }
                    List<Department> matches = departmentRepository.findAll().stream()
                            .filter(d -> d.getName() != null && d.getName().trim().equalsIgnoreCase(userDeptName))
                            .toList();
                    if (!matches.isEmpty()) {
                        return matches.get(0).getId();
                    }
                }
            }
        }

        // 4. Default fallback to first department
        List<Department> allDepts = departmentRepository.findAll();
        if (!allDepts.isEmpty()) {
            targetDeptId = allDepts.get(0).getId();
            System.out.println("[AcademicService] Fallback targetDeptId to first department: " + targetDeptId);
            return targetDeptId;
        }

        return targetDeptId;
    }

    // --- HOD Setup Progress ---
    @Transactional(readOnly = true)
    public HodSetupProgressDto getHodSetupProgress(
            String departmentId,
            String hodEmail) {
        System.out.println("[AcademicService] getHodSetupProgress called | departmentId: " + departmentId + " | hodEmail: " + hodEmail);

        String targetDeptId = resolveTargetDeptId(departmentId, hodEmail);
        validateDepartmentId(targetDeptId);

        HodSetupProgress progress = hodSetupProgressRepository
                .findByDepartmentId(targetDeptId)
                .orElseGet(() -> createDefaultProgress(targetDeptId, hodEmail));

        return buildHodSetupProgressDto(progress);
    }


    @Transactional
    public HodSetupProgressDto updateHodSetupProgress(
            String departmentId,
            Integer stepNumber,
            String hodEmail) {
        System.out.println("[AcademicService] updateHodSetupProgress called | departmentId: " + departmentId + " | stepNumber: " + stepNumber + " | hodEmail: " + hodEmail);

        String targetDeptId = resolveTargetDeptId(departmentId, hodEmail);
        validateDepartmentId(targetDeptId);
        validateStepNumber(stepNumber);

        HodSetupProgress progress = hodSetupProgressRepository
                .findByDepartmentId(targetDeptId)
                .orElseGet(() -> createDefaultProgress(targetDeptId, hodEmail));

        progress.setCurrentStep(stepNumber);

        Set<String> completedSteps = getCompletedSteps(stepNumber);

        Set<String> allSteps = new LinkedHashSet<>(
                Arrays.asList(
                        "coordinators",
                        "batch",
                        "outcomes",
                        "review"
                )
        );

        Set<String> pendingSteps = new LinkedHashSet<>(allSteps);
        pendingSteps.removeAll(completedSteps);

        progress.setCompletedSteps(String.join(",", completedSteps));
        progress.setPendingSteps(String.join(",", pendingSteps));

        if (stepNumber >= 4) {
            progress.setOverallStatus(SetupStepStatus.COMPLETED);
        } else {
            progress.setOverallStatus(SetupStepStatus.IN_PROGRESS);
        }

        if (hodEmail != null && !hodEmail.isBlank()) {
            progress.setHodEmail(hodEmail.trim());
        }

        hodSetupProgressRepository.save(progress);

        System.out.println("[AcademicService] HOD setup progress updated | targetDeptId=" + targetDeptId + " | stepNumber=" + stepNumber + " | completed=" + completedSteps + " | pending=" + pendingSteps);

        return buildHodSetupProgressDto(progress);
    }


    private HodSetupProgress createDefaultProgress(
            String departmentId,
            String hodEmail) {

        return HodSetupProgress.builder()
                .id("progress-dept-" + departmentId)
                .departmentId(departmentId)
                .hodEmail(hodEmail)
                .currentStep(1)
                .overallStatus(SetupStepStatus.IN_PROGRESS)
                .completedSteps("")
                .pendingSteps("coordinators,batch,outcomes,review")
                .build();
    }


    private Set<String> getCompletedSteps(Integer currentStep) {

        Set<String> completed = new LinkedHashSet<>();

        if (currentStep >= 2) {
            completed.add("coordinators");
        }

        if (currentStep >= 3) {
            completed.add("batch");
        }

        if (currentStep >= 4) {
            completed.add("outcomes");
            completed.add("review");
        }

        return completed;
    }


    private void validateDepartmentId(String departmentId) {

        if (departmentId == null || departmentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Department ID is required"
            );
        }
    }


    private void validateStepNumber(Integer stepNumber) {

        if (stepNumber == null || stepNumber < 1 || stepNumber > 4) {
            throw new IllegalArgumentException(
                    "Step number must be between 1 and 4"
            );
        }
    }


    private HodSetupProgressDto buildHodSetupProgressDto(
            HodSetupProgress progress) {

        List<String> completedList =
                progress.getCompletedSteps() != null
                        && !progress.getCompletedSteps().isBlank()
                        ? Arrays.asList(
                        progress.getCompletedSteps().split(",")
                )
                        : Collections.emptyList();

        List<String> pendingList =
                progress.getPendingSteps() != null
                        && !progress.getPendingSteps().isBlank()
                        ? Arrays.asList(
                        progress.getPendingSteps().split(",")
                )
                        : Collections.emptyList();

        return HodSetupProgressDto.builder()
                .id(progress.getId())
                .departmentId(progress.getDepartmentId())
                .hodEmail(progress.getHodEmail())
                .currentStep(progress.getCurrentStep())
                .overallStatus(progress.getOverallStatus())
                .completedSteps(completedList)
                .pendingSteps(pendingList)
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    @Transactional
    public HodSetupProgressDto completeHodSetup(
            String departmentId,
            String hodEmail) {
        System.out.println("[AcademicService] completeHodSetup called | departmentId: " + departmentId + " | hodEmail: " + hodEmail);

        String targetDeptId = resolveTargetDeptId(departmentId, hodEmail);
        validateDepartmentId(targetDeptId);

        HodSetupProgress progress = hodSetupProgressRepository
                .findByDepartmentId(targetDeptId)
                .orElseGet(() -> createDefaultProgress(
                        targetDeptId,
                        hodEmail
                ));

        progress.setCurrentStep(4);

        progress.setCompletedSteps(
                "coordinators,batch,outcomes,review"
        );

        progress.setPendingSteps("");

        progress.setOverallStatus(
                SetupStepStatus.COMPLETED
        );

        if (hodEmail != null && !hodEmail.isBlank()) {
            progress.setHodEmail(hodEmail.trim());
        }

        hodSetupProgressRepository.save(progress);

        System.out.println("[AcademicService] HOD setup marked as COMPLETED for targetDeptId: " + targetDeptId);

        return buildHodSetupProgressDto(progress);
    }

    // --- Programme Coordinator Summary & Setup Progress ---
    @Transactional(readOnly = true)
    public ProgrammeCoordinatorSummaryDto getProgrammeCoordinatorSummary(String coordinatorEmail, String programmeId) {
        System.out.println("[AcademicService] getProgrammeCoordinatorSummary called | coordinatorEmail: " + coordinatorEmail + " | programmeId: " + programmeId);

        List<Programme> allProgrammes = programmeRepository.findAll();
        List<Programme> assignedProgrammes = getProgrammesByCoordinatorEmail(coordinatorEmail);
        if (assignedProgrammes == null || assignedProgrammes.isEmpty()) {
            assignedProgrammes = allProgrammes;
        }

        for (Programme p : assignedProgrammes) {
            enrichProgrammeCoordinator(p);
        }

        Programme prog = null;
        if (programmeId != null && !programmeId.isBlank()) {
            String pId = programmeId.trim();
            prog = assignedProgrammes.stream().filter(p -> pId.equals(p.getId())).findFirst()
                    .orElseGet(() -> allProgrammes.stream().filter(p -> pId.equals(p.getId())).findFirst().orElse(null));
        }
        if (prog == null && !assignedProgrammes.isEmpty()) {
            prog = assignedProgrammes.get(0);
        }
        if (prog == null && !allProgrammes.isEmpty()) {
            prog = allProgrammes.get(0);
        }

        String resolvedName = "Programme Coordinator";
        String resolvedEmail = coordinatorEmail != null ? coordinatorEmail : "";

        if (prog != null) {
            enrichProgrammeCoordinator(prog);
            if (prog.getCoordinator() != null && !prog.getCoordinator().isBlank()) {
                resolvedName = prog.getCoordinator();
            }
            if (resolvedEmail.isBlank() && prog.getCoordinatorEmail() != null && !prog.getCoordinatorEmail().isBlank()) {
                resolvedEmail = prog.getCoordinatorEmail();
            }
        }

        if (prog == null) {
            ProgrammeCoordinatorSetupProgressDto emptyProgress = ProgrammeCoordinatorSetupProgressDto.builder()
                    .currentStep(1)
                    .overallStatus(SetupStepStatus.IN_PROGRESS)
                    .completedSteps(List.of())
                    .pendingSteps(List.of("programme setup", "po/pso target", "programme atr", "verify&finish"))
                    .build();

            return ProgrammeCoordinatorSummaryDto.builder()
                    .coordinatorName(resolvedName)
                    .coordinatorEmail(resolvedEmail)
                    .programmeId("")
                    .programmeCode("—")
                    .programmeName("No Programme Assigned Yet")
                    .departmentId("")
                    .departmentName("")
                    .durationYears(4)
                    .courseCount(0)
                    .activePOsCount(0)
                    .activePSOsCount(0)
                    .activePEOsCount(0)
                    .activeBatchesCount(0)
                    .pendingVerificationsCount(0)
                    .assignedProgrammes(assignedProgrammes)
                    .setupProgress(emptyProgress)
                    .build();
        }

        String targetProgId = prog.getId();
        List<Course> courses = courseRepository.findByProgrammeId(targetProgId);
        List<ProgrammeOutcome> pos = programmeOutcomeRepository.findByProgrammeId(targetProgId);
        List<ProgrammeSpecificOutcome> psos = programmeSpecificOutcomeRepository.findByProgrammeId(targetProgId);
        List<PeoOutcome> peos = peoOutcomeRepository.findByProgrammeId(targetProgId);
        List<Batch> batches = batchRepository.findByProgrammeId(targetProgId);

        ProgrammeCoordinatorSetupProgressDto progressDto = getProgrammeCoordinatorSetupProgress(coordinatorEmail, targetProgId);

        return ProgrammeCoordinatorSummaryDto.builder()
                .programmeId(prog.getId())
                .programmeCode(prog.getCode())
                .programmeName(prog.getName())
                .departmentId(prog.getDepartmentId())
                .departmentName(prog.getDepartmentName())
                .coordinatorName(resolvedName)
                .coordinatorEmail(resolvedEmail)
                .durationYears(prog.getDurationYears())
                .courseCount(courses.size())
                .activePOsCount(pos.size())
                .activePSOsCount(psos.size())
                .activePEOsCount(peos.size())
                .activeBatchesCount(batches.size())
                .pendingVerificationsCount(0)
                .assignedProgrammes(assignedProgrammes)
                .setupProgress(progressDto)
                .build();
    }

    @Transactional(readOnly = true)
    public ProgrammeCoordinatorSetupProgressDto getProgrammeCoordinatorSetupProgress(String coordinatorEmail, String programmeId) {
        System.out.println("[AcademicService] getProgrammeCoordinatorSetupProgress called | coordinatorEmail: " + coordinatorEmail + " | programmeId: " + programmeId);

        String targetProgId = programmeId;
        if ((targetProgId == null || targetProgId.isBlank()) && coordinatorEmail != null && !coordinatorEmail.isBlank()) {
            String emailTrim = coordinatorEmail.trim().toLowerCase();
            List<Programme> list = programmeRepository.findAll();
            Programme p = list.stream().filter(pr -> (pr.getCoordinatorEmail() != null && emailTrim.equalsIgnoreCase(pr.getCoordinatorEmail().trim())) || (pr.getCoordinator() != null && emailTrim.equalsIgnoreCase(pr.getCoordinator().trim()))).findFirst().orElse(null);
            if (p != null) targetProgId = p.getId();
        }
        if (targetProgId == null || targetProgId.isBlank()) {
            List<Programme> all = programmeRepository.findAll();
            if (!all.isEmpty()) targetProgId = all.get(0).getId();
        }

        if (targetProgId == null) {
            return ProgrammeCoordinatorSetupProgressDto.builder()
                    .currentStep(1)
                    .overallStatus(SetupStepStatus.IN_PROGRESS)
                    .completedSteps(List.of())
                    .pendingSteps(List.of("programme setup", "po/pso target", "programme atr", "verify&finish"))
                    .build();
        }

        final String finalProgId = targetProgId;
        ProgrammeCoordinatorSetupProgress progress = pcSetupProgressRepository.findByProgrammeId(finalProgId)
                .orElseGet(() -> pcSetupProgressRepository.save(createDefaultPcProgress(finalProgId, coordinatorEmail)));

        return buildPcSetupProgressDto(progress);
    }

    @Transactional
    public ProgrammeCoordinatorSetupProgressDto updateProgrammeCoordinatorSetupProgress(String coordinatorEmail, String programmeId, Integer stepNumber) {
        System.out.println("[AcademicService] updateProgrammeCoordinatorSetupProgress called | programmeId: " + programmeId + " | stepNumber: " + stepNumber + " | coordinatorEmail: " + coordinatorEmail);

        String targetProgId = programmeId;
        if ((targetProgId == null || targetProgId.isBlank()) && coordinatorEmail != null && !coordinatorEmail.isBlank()) {
            String emailTrim = coordinatorEmail.trim().toLowerCase();
            List<Programme> list = programmeRepository.findAll();
            Programme p = list.stream().filter(pr -> (pr.getCoordinatorEmail() != null && emailTrim.equalsIgnoreCase(pr.getCoordinatorEmail().trim())) || (pr.getCoordinator() != null && emailTrim.equalsIgnoreCase(pr.getCoordinator().trim()))).findFirst().orElse(null);
            if (p != null) targetProgId = p.getId();
        }
        if (targetProgId == null || targetProgId.isBlank()) {
            List<Programme> all = programmeRepository.findAll();
            if (!all.isEmpty()) targetProgId = all.get(0).getId();
        }

        final String finalProgId = targetProgId;
        ProgrammeCoordinatorSetupProgress progress = pcSetupProgressRepository.findByProgrammeId(finalProgId)
                .orElseGet(() -> createDefaultPcProgress(finalProgId, coordinatorEmail));

        int step = (stepNumber != null && stepNumber >= 1 && stepNumber <= 3) ? stepNumber : progress.getCurrentStep();
        progress.setCurrentStep(step);
        if (coordinatorEmail != null && !coordinatorEmail.isBlank()) {
            progress.setCoordinatorEmail(coordinatorEmail);
        }

        List<String> completed = new ArrayList<>();
        List<String> pending = new ArrayList<>();

        if (step > 1) completed.add("programme setup"); else pending.add("programme setup");
        if (step > 2) completed.add("po/pso target"); else pending.add("po/pso target");
        if (step >= 3) completed.add("programme atr"); else pending.add("programme atr");
        if (step >= 3) completed.add("verify&finish"); else pending.add("verify&finish");

        progress.setCompletedSteps(String.join(",", completed));
        progress.setPendingSteps(String.join(",", pending));
        progress.setUpdatedAt(ZonedDateTime.now());

        if (step >= 3) {
            progress.setOverallStatus(SetupStepStatus.COMPLETED);
        } else {
            progress.setOverallStatus(SetupStepStatus.IN_PROGRESS);
        }

        pcSetupProgressRepository.save(progress);
        return buildPcSetupProgressDto(progress);
    }

    @Transactional
    public ProgrammeCoordinatorSetupProgressDto completeProgrammeCoordinatorSetup(String coordinatorEmail, String programmeId) {
        System.out.println("[AcademicService] completeProgrammeCoordinatorSetup called | programmeId: " + programmeId + " | coordinatorEmail: " + coordinatorEmail);
        return updateProgrammeCoordinatorSetupProgress(coordinatorEmail, programmeId, 3);
    }

    private ProgrammeCoordinatorSetupProgress createDefaultPcProgress(String programmeId, String coordinatorEmail) {
        return ProgrammeCoordinatorSetupProgress.builder()
                .id("pcprog-" + UUID.randomUUID().toString().substring(0, 8))
                .programmeId(programmeId)
                .coordinatorEmail(coordinatorEmail)
                .currentStep(1)
                .overallStatus(SetupStepStatus.IN_PROGRESS)
                .completedSteps("")
                .pendingSteps("programme setup,po/pso target,programme atr,verify&finish")
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    private ProgrammeCoordinatorSetupProgressDto buildPcSetupProgressDto(ProgrammeCoordinatorSetupProgress progress) {
        List<String> completed = progress.getCompletedSteps() != null && !progress.getCompletedSteps().isBlank()
                ? Arrays.stream(progress.getCompletedSteps().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();
        List<String> pending = progress.getPendingSteps() != null && !progress.getPendingSteps().isBlank()
                ? Arrays.stream(progress.getPendingSteps().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();

        return ProgrammeCoordinatorSetupProgressDto.builder()
                .id(progress.getId())
                .programmeId(progress.getProgrammeId())
                .coordinatorEmail(progress.getCoordinatorEmail())
                .currentStep(progress.getCurrentStep())
                .overallStatus(progress.getOverallStatus())
                .completedSteps(completed)
                .pendingSteps(pending)
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHodCoordinators(String departmentId) {
        List<Programme> progs = (departmentId != null && !departmentId.isBlank())
                ? programmeRepository.findByDepartmentId(departmentId)
                : programmeRepository.findAll();

        List<Map<String, Object>> list = new ArrayList<>();
        for (Programme p : progs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("programmeId", p.getId());
            item.put("programmeCode", p.getCode());
            item.put("programmeName", p.getName());
            item.put("coordinatorName", p.getCoordinator() != null ? p.getCoordinator() : "Not Assigned");
            item.put("coordinatorEmail", p.getCoordinatorEmail() != null ? p.getCoordinatorEmail() : "");
            item.put("assignedDate", "2025-06-15");
            list.add(item);
        }
        return list;
    }

    @Transactional
    public Map<String, Object> assignHodCoordinator(Map<String, Object> payload) {
        String progId = payload != null && payload.get("programmeId") != null ? payload.get("programmeId").toString() : null;
        String name = payload != null && payload.get("coordinatorName") != null ? payload.get("coordinatorName").toString() : "";
        String email = payload != null && payload.get("coordinatorEmail") != null ? payload.get("coordinatorEmail").toString() : "";

        if (progId != null) {
            Programme p = programmeRepository.findById(progId).orElse(null);
            if (p != null) {
                p.setCoordinator(name);
                p.setCoordinatorEmail(email);
                programmeRepository.save(p);
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Programme coordinator assigned successfully.");
        return res;
    }

    @Transactional
    public Map<String, Object> allocateCourses(String programmeId, List<Map<String, Object>> allocations) {
        if (allocations != null) {
            for (Map<String, Object> item : allocations) {
                String courseId = item.get("courseId") != null ? item.get("courseId").toString() : null;
                String email = item.get("coordinatorEmail") != null ? item.get("coordinatorEmail").toString() : "";
                String name = item.get("courseCoordinatorName") != null ? item.get("courseCoordinatorName").toString() : (item.get("coordinator") != null ? item.get("coordinator").toString() : "");

                if (courseId != null) {
                    Course course = courseRepository.findById(courseId).orElse(null);
                    if (course != null) {
                        course.setCoordinator(name);
                        course.setFaculty(name);
                        course.setAssignedFaculty(name + " (" + email + ")");
                        courseRepository.save(course);
                    }

                    List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseId);
                    for (CourseOffering off : offerings) {
                        off.setCourseCoordinatorName(name);
                        off.setAssignedFaculty(name + " (" + email + ")");
                        courseOfferingRepository.save(off);
                    }
                }
            }
        }

        ApprovalRequest req = ApprovalRequest.builder()
                .id("app-alloc-" + UUID.randomUUID().toString().substring(0, 8))
                .type(ApprovalType.COURSE_ALLOCATION)
                .title("Course Allocation for Programme " + programmeId)
                .programmeId(programmeId)
                .resourceId("allocation-" + programmeId)
                .status(ApprovalStatus.PENDING)
                .submittedBy("Programme Coordinator")
                .submittedAt(ZonedDateTime.now())
                .remarks("Allocations submitted for HOD review.")
                .build();
        approvalRequestRepository.save(req);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Course allocations saved and submitted for verification.");
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConsolidatedOutcomes(String programmeId, String batchId) {
        String pId = programmeId != null && !programmeId.isBlank() ? programmeId : "prog-1";
        List<ProgrammeOutcome> pos = programmeOutcomeRepository.findByProgrammeIdOrderByCodeAsc(pId);
        List<ProgrammeSpecificOutcome> psos = programmeSpecificOutcomeRepository.findByProgrammeIdOrderByCodeAsc(pId);
        List<PeoOutcome> peos = peoOutcomeRepository.findByProgrammeIdOrderByCodeAsc(pId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("programmeId", pId);
        data.put("batchId", batchId);
        data.put("pos", pos);
        data.put("psos", psos);
        data.put("peos", peos);
        return data;
    }

    @Transactional
    public Map<String, Object> saveConsolidatedOutcomes(Map<String, Object> payload) {
        String progId = payload != null && payload.get("programmeId") != null ? payload.get("programmeId").toString() : "prog-1";

        // 1. Process and save POs
        if (payload != null && payload.get("pos") instanceof List<?> poList) {
            List<ProgrammeOutcome> existingPOs = programmeOutcomeRepository.findByProgrammeIdOrderByCodeAsc(progId);
            if (!existingPOs.isEmpty()) {
                programmeOutcomeRepository.deleteAll(existingPOs);
                programmeOutcomeRepository.flush();
            }

            for (Object obj : poList) {
                if (obj instanceof Map<?, ?> poMap) {
                    String code = poMap.get("code") != null ? poMap.get("code").toString() : null;
                    String statement = poMap.get("statement") != null ? poMap.get("statement").toString() : "";
                    if (code != null && !code.isBlank()) {
                        ProgrammeOutcome po = ProgrammeOutcome.builder()
                                .id("po-" + progId + "-" + code.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6))
                                .programmeId(progId)
                                .code(code.trim().toUpperCase())
                                .statement(statement.trim())
                                .build();
                        programmeOutcomeRepository.save(po);
                    }
                }
            }
        }

        // 2. Process and save PSOs
        if (payload != null && payload.get("psos") instanceof List<?> psoList) {
            List<ProgrammeSpecificOutcome> existingPSOs = programmeSpecificOutcomeRepository.findByProgrammeIdOrderByCodeAsc(progId);
            if (!existingPSOs.isEmpty()) {
                programmeSpecificOutcomeRepository.deleteAll(existingPSOs);
                programmeSpecificOutcomeRepository.flush();
            }

            for (Object obj : psoList) {
                if (obj instanceof Map<?, ?> psoMap) {
                    String code = psoMap.get("code") != null ? psoMap.get("code").toString() : null;
                    String statement = psoMap.get("statement") != null ? psoMap.get("statement").toString() : "";
                    if (code != null && !code.isBlank()) {
                        ProgrammeSpecificOutcome pso = ProgrammeSpecificOutcome.builder()
                                .id("pso-" + progId + "-" + code.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6))
                                .programmeId(progId)
                                .code(code.trim().toUpperCase())
                                .statement(statement.trim())
                                .build();
                        programmeSpecificOutcomeRepository.save(pso);
                    }
                }
            }
        }

        // 3. Process and save PEOs
        if (payload != null && payload.get("peos") instanceof List<?> peoList) {
            List<PeoOutcome> existingPEOs = peoOutcomeRepository.findByProgrammeIdOrderByCodeAsc(progId);
            if (!existingPEOs.isEmpty()) {
                peoOutcomeRepository.deleteAll(existingPEOs);
                peoOutcomeRepository.flush();
            }

            for (Object obj : peoList) {
                if (obj instanceof Map<?, ?> peoMap) {
                    String code = peoMap.get("code") != null ? peoMap.get("code").toString() : null;
                    String statement = peoMap.get("statement") != null ? peoMap.get("statement").toString() : "";
                    if (code != null && !code.isBlank()) {
                        PeoOutcome peo = PeoOutcome.builder()
                                .id("peo-" + progId + "-" + code.toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + UUID.randomUUID().toString().substring(0, 6))
                                .programmeId(progId)
                                .code(code.trim().toUpperCase())
                                .statement(statement.trim())
                                .build();
                        peoOutcomeRepository.save(peo);
                    }
                }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Outcomes saved successfully.");
        res.put("data", getConsolidatedOutcomes(progId, null));
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCourseCoTargets(String courseId, String batchId) {
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseId);
        String offeringId = !offerings.isEmpty() ? offerings.get(0).getId() : courseId;
        List<CourseOutcome> cos = courseOutcomeRepository.findByCourseOfferingId(offeringId);

        Map<String, BigDecimal> targets = new LinkedHashMap<>();
        for (CourseOutcome co : cos) {
            targets.put(co.getCode(), co.getTargetLevel() != null ? co.getTargetLevel() : new BigDecimal("2.50"));
        }
        if (targets.isEmpty()) {
            for (int i = 1; i <= 5; i++) {
                targets.put("CO" + i, new BigDecimal("2.50"));
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("courseId", courseId);
        res.put("batchId", batchId);
        res.put("coTargets", targets);
        return res;
    }

    @Transactional
    public Map<String, Object> saveCourseCoTargets(String courseId, Map<String, Object> coTargets) {
        List<CourseOffering> offerings = courseOfferingRepository.findByCourseId(courseId);
        String offeringId = !offerings.isEmpty() ? offerings.get(0).getId() : courseId;
        List<CourseOutcome> cos = courseOutcomeRepository.findByCourseOfferingId(offeringId);

        if (coTargets != null) {
            for (CourseOutcome co : cos) {
                if (coTargets.containsKey(co.getCode())) {
                    Object val = coTargets.get(co.getCode());
                    if (val instanceof Number) {
                        co.setTargetLevel(BigDecimal.valueOf(((Number) val).doubleValue()));
                    } else if (val instanceof String) {
                        try {
                            co.setTargetLevel(new BigDecimal((String) val));
                        } catch (Exception ignored) {}
                    }
                    courseOutcomeRepository.save(co);
                }
            }
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "CO targets saved successfully.");
        res.put("data", getCourseCoTargets(courseId, null));
        return res;
    }
}
