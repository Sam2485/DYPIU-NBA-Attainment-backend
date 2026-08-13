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
    public List<DepartmentSummaryDto> getDepartmentSummary(String schoolId) {
        String targetSchoolId = (schoolId != null && !schoolId.isBlank()) ? schoolId : "sch-1";
        List<Department> departments = departmentRepository.findBySchoolId(targetSchoolId);
        if (departments.isEmpty()) {
            departments = departmentRepository.findAll();
        }

        List<DepartmentSummaryDto> list = new ArrayList<>();
        for (Department dept : departments) {
            boolean isHodAssigned = dept.getHod() != null && !dept.getHod().isBlank() && !dept.getHod().equalsIgnoreCase("Unassigned");
            list.add(DepartmentSummaryDto.builder()
                    .deptId(dept.getId())
                    .deptCode(dept.getCode())
                    .deptName(dept.getName())
                    .deptHodName(dept.getHod())
                    .deptHodEmail(dept.getHodEmail())
                    .hodAssignedStatus(isHodAssigned)
                    .build());
        }

        System.out.println("[AcademicService] Fetched department summary list (" + list.size() + " items) for schoolId: " + targetSchoolId);
        return list;
    }

    // --- Director Setup Progress ---
    @Transactional(readOnly = true)
    public DirectorSetupProgressDto getDirectorSetupProgress(String schoolId) {
        String targetSchoolId = (schoolId != null && !schoolId.isBlank()) ? schoolId : "sch-1";
        DirectorSetupProgress progress = directorSetupProgressRepository.findBySchoolId(targetSchoolId)
                .orElseGet(() -> DirectorSetupProgress.builder()
                        .id("progress-" + targetSchoolId)
                        .schoolId(targetSchoolId)
                        .currentStep(1)
                        .currentStepEnum(DirectorSetupStep.SCHOOL)
                        .overallStatus(SetupStepStatus.IN_PROGRESS)
                        .completedSteps("")
                        .pendingSteps("school,department,programme,review")
                        .updatedAt(ZonedDateTime.now())
                        .build());

        DirectorSetupProgressDto dto = buildSetupProgressDto(progress);
        System.out.println("[AcademicService] Fetched director setup progress for schoolId: " + targetSchoolId + " at step: " + progress.getCurrentStep());
        return dto;
    }

    @Transactional
    public DirectorSetupProgressDto updateDirectorSetupProgress(String schoolId, Integer stepNumber) {
        String targetSchoolId = (schoolId != null && !schoolId.isBlank()) ? schoolId : "sch-1";
        DirectorSetupProgress progress = directorSetupProgressRepository.findBySchoolId(targetSchoolId)
                .orElseGet(() -> DirectorSetupProgress.builder()
                        .id("progress-" + targetSchoolId)
                        .schoolId(targetSchoolId)
                        .build());

        int currentStep = (stepNumber != null && stepNumber >= 1 && stepNumber <= 4) ? stepNumber : 1;
        DirectorSetupStep stepEnum;
        SetupStepStatus overallStatus = SetupStepStatus.IN_PROGRESS;
        List<String> completed = new ArrayList<>();
        List<String> pending = new ArrayList<>();

        switch (currentStep) {
            case 1:
                stepEnum = DirectorSetupStep.SCHOOL;
                pending.addAll(List.of("school", "department", "programme", "review"));
                break;
            case 2:
                stepEnum = DirectorSetupStep.DEPARTMENT;
                completed.add("school");
                pending.addAll(List.of("department", "programme", "review"));
                break;
            case 3:
                stepEnum = DirectorSetupStep.PROGRAMME;
                completed.addAll(List.of("school", "department"));
                pending.addAll(List.of("programme", "review"));
                break;
            case 4:
            default:
                stepEnum = DirectorSetupStep.REVIEW;
                completed.addAll(List.of("school", "department", "programme", "review"));
                overallStatus = SetupStepStatus.COMPLETED;
                break;
        }

        progress.setCurrentStep(currentStep);
        progress.setCurrentStepEnum(stepEnum);
        progress.setOverallStatus(overallStatus);
        progress.setCompletedSteps(String.join(",", completed));
        progress.setPendingSteps(String.join(",", pending));
        progress.setUpdatedAt(ZonedDateTime.now());

        directorSetupProgressRepository.save(progress);
        DirectorSetupProgressDto dto = buildSetupProgressDto(progress);
        System.out.println("[AcademicService] Updated director setup progress for schoolId: " + targetSchoolId + " to step: " + progress.getCurrentStep() + " (" + progress.getOverallStatus() + ")");
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

        stepStatuses.put(DirectorSetupStep.SCHOOL, currentStep > 1 ? SetupStepStatus.COMPLETED : (currentStep == 1 ? SetupStepStatus.IN_PROGRESS : SetupStepStatus.NOT_STARTED));
        stepStatuses.put(DirectorSetupStep.DEPARTMENT, currentStep > 2 ? SetupStepStatus.COMPLETED : (currentStep == 2 ? SetupStepStatus.IN_PROGRESS : SetupStepStatus.NOT_STARTED));
        stepStatuses.put(DirectorSetupStep.PROGRAMME, currentStep > 3 ? SetupStepStatus.COMPLETED : (currentStep == 3 ? SetupStepStatus.IN_PROGRESS : SetupStepStatus.NOT_STARTED));
        stepStatuses.put(DirectorSetupStep.REVIEW, currentStep == 4 ? SetupStepStatus.COMPLETED : SetupStepStatus.NOT_STARTED);

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
        List<School> schools = schoolRepository.findAll();
        System.out.println("[AcademicService] Fetched all schools list (" + schools.size() + " items)");
        return schools;
    }

    @Transactional(readOnly = true)
    public School getSchoolById(String id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));
        System.out.println("[AcademicService] Fetched school by id: " + id);
        return school;
    }

    @Transactional
    public School saveSchool(School school) {
        if (school.getId() == null) school.setId("sch-" + UUID.randomUUID().toString().substring(0, 8));
        School saved = schoolRepository.save(school);
        System.out.println("[AcademicService] Saved new school with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public School updateSchool(String id, School schoolDetails) {
        School school = schoolRepository.findById(id)
                .orElseGet(() -> schoolRepository.findAll().stream().findFirst().orElse(
                        School.builder().id(id).build()
                ));

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

        if (school.getId() == null) school.setId(id);
        School updated = schoolRepository.save(school);
        System.out.println("[AcademicService] Updated school info for id: " + updated.getId());
        return updated;
    }

    // --- Departments ---
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        List<Department> list = departmentRepository.findAll();
        System.out.println("[AcademicService] Fetched all departments (" + list.size() + " items)");
        return list;
    }

    @Transactional
    public Department saveDepartment(Department department) {
        if (department.getId() == null) department.setId("dept-" + UUID.randomUUID().toString().substring(0, 8));
        Department saved = departmentRepository.save(department);
        System.out.println("[AcademicService] Saved department with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteDepartment(String id) {
        departmentRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted department with id: " + id);
    }

    // --- Programmes ---
    @Transactional(readOnly = true)
    public List<Programme> getAllProgrammes() {
        List<Programme> list = programmeRepository.findAll();
        System.out.println("[AcademicService] Fetched all programmes (" + list.size() + " items)");
        return list;
    }

    @Transactional
    public Programme saveProgramme(Programme programme) {
        if (programme.getId() == null) programme.setId("prog-" + UUID.randomUUID().toString().substring(0, 8));
        Programme saved = programmeRepository.save(programme);
        System.out.println("[AcademicService] Saved programme with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteProgramme(String id) {
        programmeRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted programme with id: " + id);
    }

    // --- Batches ---
    @Transactional(readOnly = true)
    public List<Batch> getAllBatches() {
        List<Batch> list = batchRepository.findAll();
        System.out.println("[AcademicService] Fetched all batches (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public List<Batch> getBatchesByProgramme(String programmeId) {
        List<Batch> list = batchRepository.findByProgrammeId(programmeId);
        System.out.println("[AcademicService] Fetched batches (" + list.size() + " items) for programmeId: " + programmeId);
        return list;
    }

    @Transactional
    public Batch saveBatch(Batch batch) {
        if (batch.getId() == null) batch.setId("batch-" + UUID.randomUUID().toString().substring(0, 8));
        Batch saved = batchRepository.save(batch);
        System.out.println("[AcademicService] Saved batch with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteBatch(String id) {
        batchRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted batch with id: " + id);
    }

    // --- Courses ---
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        List<Course> list = courseRepository.findAll();
        System.out.println("[AcademicService] Fetched all courses (" + list.size() + " items)");
        return list;
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByProgramme(String programmeId) {
        List<Course> list = courseRepository.findByProgrammeId(programmeId);
        System.out.println("[AcademicService] Fetched courses (" + list.size() + " items) for programmeId: " + programmeId);
        return list;
    }

    @Transactional
    public Course saveCourse(Course course) {
        if (course.getId() == null) course.setId("crs-" + UUID.randomUUID().toString().substring(0, 8));
        Course saved = courseRepository.save(course);
        System.out.println("[AcademicService] Saved course with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted course with id: " + id);
    }

    // --- Students ---
    @Transactional(readOnly = true)
    public List<Student> getStudentsByBatch(String batchId) {
        List<Student> list = studentRepository.findByBatchId(batchId);
        System.out.println("[AcademicService] Fetched students (" + list.size() + " items) for batchId: " + batchId);
        return list;
    }

    @Transactional
    public Student saveStudent(Student student) {
        if (student.getId() == null) student.setId("std-" + UUID.randomUUID().toString().substring(0, 8));
        Student saved = studentRepository.save(student);
        System.out.println("[AcademicService] Saved student with id: " + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteStudent(String id) {
        studentRepository.deleteById(id);
        System.out.println("[AcademicService] Deleted student with id: " + id);
    }
}
