package com.dypiu.nba.service;

import com.dypiu.nba.entity.*;
import com.dypiu.nba.exception.ResourceNotFoundException;
import com.dypiu.nba.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dypiu.nba.dto.DirectorSetupProgressDto;
import com.dypiu.nba.dto.DirectorSchoolSummaryDto;
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
    public DirectorSchoolSummaryDto getDirectorSchoolSummary(String schoolId) {
        String targetSchoolId = (schoolId != null && !schoolId.isBlank()) ? schoolId : "sch-1";
        School school = schoolRepository.findById(targetSchoolId)
                .orElseGet(() -> schoolRepository.findAll().stream().findFirst().orElse(
                        School.builder().id("sch-1").name("School of Engineering & Technology").code("SET").dean("Dr. R. K. Deshmukh").estYear("2019").build()
                ));

        List<Department> departments = departmentRepository.findBySchoolId(targetSchoolId);
        if (departments.isEmpty()) {
            departments = departmentRepository.findAll();
        }

        List<Programme> allProgrammes = programmeRepository.findAll();

        int assignedHodCount = 0;
        int unassignedHodCount = 0;
        List<DirectorSchoolSummaryDto.DepartmentSummaryDto> deptSummaries = new ArrayList<>();

        for (Department dept : departments) {
            boolean isHodAssigned = dept.getHod() != null && !dept.getHod().isBlank() && !dept.getHod().equalsIgnoreCase("Unassigned");
            if (isHodAssigned) {
                assignedHodCount++;
            } else {
                unassignedHodCount++;
            }

            long progCount = allProgrammes.stream()
                    .filter(p -> dept.getId().equals(p.getDepartmentId()))
                    .count();

        }

        return DirectorSchoolSummaryDto.builder()
                .schoolId(school.getId())
                .schoolName(school.getName())
                .schoolCode(school.getCode())
                .deanName(school.getDean())
                .estYear(school.getEstYear())
                .totalDepartments(departments.size())
                .assignedHODsCount(assignedHodCount)
                .unassignedHODsCount(unassignedHodCount)
                .totalProgrammes(allProgrammes.size())
                .build();
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

        return buildSetupProgressDto(progress);
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
        return buildSetupProgressDto(progress);
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
        return schoolRepository.findAll();
    }

    @Transactional(readOnly = true)
    public School getSchoolById(String id) {
        System.out.println("loaded school data");
        return schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with id: " + id));
    }

    @Transactional
    public School saveSchool(School school) {
        if (school.getId() == null) school.setId("sch-" + UUID.randomUUID().toString().substring(0, 8));
        return schoolRepository.save(school);
    }

    @Transactional
    public School updateSchool(String id, School schoolDetails) {
        System.out.println("Updated school info");
        School school = schoolRepository.findById(id)
                .orElseGet(() -> schoolRepository.findAll().stream().findFirst().orElse(
                        School.builder().id(id).code("SET").name("School of Engineering & Technology").build()
                ));

        if (schoolDetails.getName() != null && !schoolDetails.getName().isBlank()) {
            school.setName(schoolDetails.getName());
        }
        if (schoolDetails.getCode() != null && !schoolDetails.getCode().isBlank()) {
            school.setCode(schoolDetails.getCode());
        }
        if (schoolDetails.getDean() != null) {
            school.setDean(schoolDetails.getDean());
        }
        if (schoolDetails.getEstYear() != null) {
            school.setEstYear(schoolDetails.getEstYear());
        }
        if (schoolDetails.getEmail() != null) {
            school.setEmail(schoolDetails.getEmail());
        }

        if (school.getId() == null) school.setId(id);
        return schoolRepository.save(school);
    }

    // --- Departments ---
    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Transactional
    public Department saveDepartment(Department department) {
        if (department.getId() == null) department.setId("dept-" + UUID.randomUUID().toString().substring(0, 8));
        return departmentRepository.save(department);
    }

    @Transactional
    public void deleteDepartment(String id) {
        departmentRepository.deleteById(id);
    }

    // --- Programmes ---
    @Transactional(readOnly = true)
    public List<Programme> getAllProgrammes() {
        return programmeRepository.findAll();
    }

    @Transactional
    public Programme saveProgramme(Programme programme) {
        if (programme.getId() == null) programme.setId("prog-" + UUID.randomUUID().toString().substring(0, 8));
        return programmeRepository.save(programme);
    }

    @Transactional
    public void deleteProgramme(String id) {
        programmeRepository.deleteById(id);
    }

    // --- Batches ---
    @Transactional(readOnly = true)
    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Batch> getBatchesByProgramme(String programmeId) {
        return batchRepository.findByProgrammeId(programmeId);
    }

    @Transactional
    public Batch saveBatch(Batch batch) {
        if (batch.getId() == null) batch.setId("batch-" + UUID.randomUUID().toString().substring(0, 8));
        return batchRepository.save(batch);
    }

    @Transactional
    public void deleteBatch(String id) {
        batchRepository.deleteById(id);
    }

    // --- Courses ---
    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Course> getCoursesByProgramme(String programmeId) {
        return courseRepository.findByProgrammeId(programmeId);
    }

    @Transactional
    public Course saveCourse(Course course) {
        if (course.getId() == null) course.setId("crs-" + UUID.randomUUID().toString().substring(0, 8));
        return courseRepository.save(course);
    }

    @Transactional
    public void deleteCourse(String id) {
        courseRepository.deleteById(id);
    }

    // --- Students ---
    @Transactional(readOnly = true)
    public List<Student> getStudentsByBatch(String batchId) {
        return studentRepository.findByBatchId(batchId);
    }

    @Transactional
    public Student saveStudent(Student student) {
        if (student.getId() == null) student.setId("std-" + UUID.randomUUID().toString().substring(0, 8));
        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(String id) {
        studentRepository.deleteById(id);
    }
}
