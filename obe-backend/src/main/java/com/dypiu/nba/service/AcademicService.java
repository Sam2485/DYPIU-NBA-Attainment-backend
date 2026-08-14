package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dypiu.nba.dto.DirectorSetupProgressDto;
import com.dypiu.nba.dto.DirectorSchoolSummaryDto;
import com.dypiu.nba.dto.DepartmentSummaryDto;
import com.dypiu.nba.dto.HodDepartmentSummaryDto;
import com.dypiu.nba.dto.HodSetupProgressDto;
import com.dypiu.nba.dto.UserDto;
import java.time.ZonedDateTime;
import java.util.*;

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
    private final UserRepository userRepository;

    // --- Director School Summary ---
    @Transactional(readOnly = true)
    public DirectorSchoolSummaryDto getDirectorSchoolSummary(String directorEmail) {
        System.out.println("[AcademicService] Starting school summary fetch for directorEmail: " + directorEmail );
        Optional<School> schoolOpt = Optional.empty();

        // 1. Fetch school having director email as our director email
        if (directorEmail != null && !directorEmail.isBlank()) {
            schoolOpt = schoolRepository.findByDirectorEmailIgnoreCase(directorEmail);
        }


        if (schoolOpt.isEmpty()) {
            System.out.println("[AcademicService] No school found under Director email: " + directorEmail);
            return DirectorSchoolSummaryDto.builder()
                    .schoolId(null)
                    .schoolName(null)
                    .schoolCode(null)
                    .directorName(null)
                    .directorEmail(null)
                    .estYear(null)
                    .totalDepartments(0)
                    .assignedHODsCount(0)
                    .unassignedHODsCount(0)
                    .totalProgrammes(0)
                    .build();
        }

        School school = schoolOpt.get();
        List<Department> departments = departmentRepository.findBySchoolId(school.getId());
        List<Programme> allProgrammes = programmeRepository.findAll();

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

        DirectorSchoolSummaryDto summary = DirectorSchoolSummaryDto.builder()
                .schoolId(school.getId())
                .schoolName(school.getName())
                .schoolCode(school.getCode())
                .directorName(school.getDirector())
                .directorEmail(school.getDirectorEmail())
                .estYear(school.getEstYear())
                .totalDepartments(departments.size())
                .assignedHODsCount(assignedHodCount)
                .unassignedHODsCount(unassignedHodCount)
                .totalProgrammes(allProgrammes.size())

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
            int progsCount = programmeRepository.findByDepartmentIdOrDepartmentName(dept.getId(), dept.getName()).size();
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
            // STEP 4 → REVIEW
            // ─────────────────────────────
            case 4:

                stepEnum = DirectorSetupStep.REVIEW;

                completed.addAll(List.of(
                        "school",
                        "department",
                        "programme"
                ));

                // Review is currently being worked on.
                pending.add("review");

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
        if (school.getId() == null || school.getId().isBlank()) {
            school.setId("sch-" + UUID.randomUUID().toString().substring(0, 8));
        }
        School saved = schoolRepository.save(school);
        System.out.println("[AcademicService] Saved school with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public School updateSchool(String id, School schoolDetails) {
        System.out.println("[AcademicService] updateSchool called | id: " + id + " | name: " + (schoolDetails != null ? schoolDetails.getName() : "null"));
        School school = schoolRepository.findById(id)
                .orElseGet(() -> {
                    if (schoolDetails.getDirectorEmail() != null && !schoolDetails.getDirectorEmail().isBlank()) {
                        return schoolRepository.findByDirectorEmailIgnoreCase(schoolDetails.getDirectorEmail())
                                .orElse(School.builder().id(id).build());
                    }
                    return School.builder().id(id).build();
                });

        if (schoolDetails.getName() != null && !schoolDetails.getName().isBlank()) {
            school.setName(schoolDetails.getName());
        }
        if (schoolDetails.getCode() != null && !schoolDetails.getCode().isBlank()) {
            school.setCode(schoolDetails.getCode());
        }
        if (schoolDetails.getDirector() != null) {
            school.setDirector(schoolDetails.getDirector());
        }
        if (schoolDetails.getDirectorEmail() != null) {
            school.setDirectorEmail(schoolDetails.getDirectorEmail());
        }
        if (schoolDetails.getEstYear() != null) {
            school.setEstYear(schoolDetails.getEstYear());
        }

        if (school.getId() == null || school.getId().isBlank()) {
            school.setId(id);
        }
        School updated = schoolRepository.save(school);
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
        String searchRole = role != null ? role.trim() : "HOD";
        List<User> users = userRepository.findByRoleIgnoreCase(searchRole);
        if (users.isEmpty() && (searchRole.equalsIgnoreCase("programme-coordinator") || searchRole.equalsIgnoreCase("programme_coordinator") || searchRole.equalsIgnoreCase("PROGRAMME_COORDINATOR"))) {
            users = userRepository.findByRoleIgnoreCase("PROGRAMME_COORDINATOR");
            if (users.isEmpty()) {
                users = userRepository.findByRoleIgnoreCase("programme-coordinator");
            }
        }
        List<UserDto> dtos = users.stream().map(u -> UserDto.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .role(u.getRole())
                .department(u.getDepartment())
                .programme(u.getProgramme())
                .build()
        ).toList();
        System.out.println("[AcademicService] Fetched users by role (" + searchRole + "): count=" + dtos.size());
        return dtos;
    }

    // --- Programmes ---
    @Transactional(readOnly = true)
    public List<Programme> getAllProgrammes() {
        System.out.println("[AcademicService] getAllProgrammes called");
        List<Programme> list = programmeRepository.findAll();
        System.out.println("[AcademicService] Fetched all programmes (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public List<Programme> getProgrammesBySchool(String schoolId) {
        System.out.println("[AcademicService] getProgrammesBySchool called | schoolId: " + schoolId);
        List<Department> depts = departmentRepository.findBySchoolId(schoolId);
        if (depts == null || depts.isEmpty()) {
            return programmeRepository.findAll();
        }
        List<String> deptIds = depts.stream().map(Department::getId).toList();
        List<Programme> list = programmeRepository.findByDepartmentIdIn(deptIds);
        System.out.println("[AcademicService] Fetched programmes (" + list.size() + " items) for schoolId: " + schoolId);
        return list;
    }

    @Transactional(readOnly = true)
    public List<Programme> getProgrammesByDepartment(String departmentId) {
        System.out.println("[AcademicService] getProgrammesByDepartment called | departmentId: " + departmentId);
        if (departmentId == null || departmentId.isBlank()) {
            return getAllProgrammes();
        }
        Optional<Department> deptOpt = departmentRepository.findById(departmentId);
        String deptName = deptOpt.isPresent() ? deptOpt.get().getName() : "";
        List<Programme> list = programmeRepository.findByDepartmentIdOrDepartmentName(departmentId, deptName);
        if (list.isEmpty()) {
            list = programmeRepository.findByDepartmentId(departmentId);
        }
        System.out.println("[AcademicService] Fetched programmes (" + list.size() + " items) for departmentId: " + departmentId);
        return list;
    }

    @Transactional
    public Programme saveProgramme(Programme programme) {
        System.out.println("[AcademicService] saveProgramme called | id: " + (programme != null ? programme.getId() : "null") + " | name: " + (programme != null ? programme.getName() : "null") + " | coordinator: " + (programme != null ? programme.getCoordinator() : "null") + " | coordinatorEmail: " + (programme != null ? programme.getCoordinatorEmail() : "null"));
        if (programme.getId() == null) programme.setId("prog-" + UUID.randomUUID().toString().substring(0, 8));
        Programme saved = programmeRepository.save(programme);
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

        // 1. First search department repository by hodEmail
        if (hodEmail != null && !hodEmail.isBlank()) {
            String trimmedEmail = hodEmail.trim();
            Optional<Department> deptOpt = departmentRepository.findByHodEmailIgnoreCase(trimmedEmail);
            if (deptOpt.isPresent()) {
                dept = deptOpt.get();
            }
        }

        // 2. If not found by hodEmail, look up user by email to get user.getDepartment()
        if (dept == null && hodEmail != null && !hodEmail.isBlank()) {
            String trimmedEmail = hodEmail.trim();
            Optional<User> userOpt = userRepository.findByEmail(trimmedEmail);
            if (userOpt.isPresent() && userOpt.get().getDepartment() != null && !userOpt.get().getDepartment().isBlank()) {
                String userDeptName = userOpt.get().getDepartment().trim();
                Optional<Department> deptOpt = departmentRepository.findByName(userDeptName);
                if (deptOpt.isPresent()) {
                    dept = deptOpt.get();
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
        List<Programme> programmes = programmeRepository.findByDepartmentIdOrDepartmentName(deptId, deptName);
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
        if ((targetDeptId == null || targetDeptId.isBlank()) && hodEmail != null && !hodEmail.isBlank()) {
            String trimmedEmail = hodEmail.trim();
            Optional<Department> deptOpt = departmentRepository.findByHodEmailIgnoreCase(trimmedEmail);
            if (deptOpt.isPresent()) {
                targetDeptId = deptOpt.get().getId();
            } else {
                Optional<User> userOpt = userRepository.findByEmail(trimmedEmail);
                if (userOpt.isPresent() && userOpt.get().getDepartment() != null && !userOpt.get().getDepartment().isBlank()) {
                    String userDeptName = userOpt.get().getDepartment().trim();
                    Optional<Department> dOpt = departmentRepository.findByName(userDeptName);
                    if (dOpt.isPresent()) {
                        targetDeptId = dOpt.get().getId();
                    } else {
                        List<Department> matches = departmentRepository.findAll().stream()
                                .filter(d -> d.getName() != null && d.getName().trim().equalsIgnoreCase(userDeptName))
                                .toList();
                        if (!matches.isEmpty()) {
                            targetDeptId = matches.get(0).getId();
                        }
                    }
                }
            }
        }
        System.out.println("[AcademicService] Resolved targetDeptId: " + targetDeptId);
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

        progress.setOverallStatus(SetupStepStatus.IN_PROGRESS);

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
}
