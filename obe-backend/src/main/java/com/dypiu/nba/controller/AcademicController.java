package com.dypiu.nba.controller;

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
import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.service.AcademicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/academic")
@RequiredArgsConstructor
public class AcademicController {

    private final AcademicService academicService;

    // --- Users by Role ---
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByRole(
            @RequestParam(required = false, defaultValue = "HOD") String role) {
        return ResponseEntity.ok(ApiResponse.<List<UserDto>>builder()
                .success(true)
                .data(academicService.getUsersByRole(role))
                .build());
    }

    // --- Director School Summary ---
    @GetMapping("/director/school-summary")
    public ResponseEntity<ApiResponse<DirectorSchoolSummaryDto>> getDirectorSchoolSummary(
            @RequestParam(required = false) String directorEmail) {
        return ResponseEntity.ok(ApiResponse.<DirectorSchoolSummaryDto>builder()
                .success(true)
                .data(academicService.getDirectorSchoolSummary(directorEmail))
                .build());
    }

    // --- Director Department Summary ---
    @GetMapping("/director/department-summary")
    public ResponseEntity<ApiResponse<List<DepartmentSummaryDto>>> getDepartmentSummary(
            @RequestParam(required = true) String schoolId,
            @RequestParam(required = true) String directorEmail) {
        return ResponseEntity.ok(ApiResponse.<List<DepartmentSummaryDto>>builder()
                .success(true)
                .data(academicService.getDepartmentSummary(schoolId, directorEmail))
                .build());
    }

    // --- Director Setup Progress ---
    @GetMapping("/director/setup-progress")
    public ResponseEntity<ApiResponse<DirectorSetupProgressDto>> getDirectorSetupProgress(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String directorEmail) {
        return ResponseEntity.ok(ApiResponse.<DirectorSetupProgressDto>builder()
                .success(true)
                .data(academicService.getDirectorSetupProgress(schoolId, directorEmail))
                .build());
    }

    @PostMapping("/director/setup-progress")
    public ResponseEntity<ApiResponse<DirectorSetupProgressDto>> updateDirectorSetupProgress(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false, defaultValue = "1") Integer currentStep) {
        return ResponseEntity.ok(ApiResponse.<DirectorSetupProgressDto>builder()
                .success(true)
                .message("Director setup progress updated successfully")
                .data(academicService.updateDirectorSetupProgress(schoolId, currentStep))
                .build());
    }

    // --- HOD Department Summary ---
    @GetMapping("/hod/department-summary")
    public ResponseEntity<ApiResponse<HodDepartmentSummaryDto>> getHodDepartmentSummary(
            @RequestParam(required = false) String hodEmail) {
        return ResponseEntity.ok(ApiResponse.<HodDepartmentSummaryDto>builder()
                .success(true)
                .data(academicService.getHodDepartmentSummary(hodEmail))
                .build());
    }

    // --- HOD Setup Progress ---
    @GetMapping("/hod/setup-progress")
    public ResponseEntity<ApiResponse<HodSetupProgressDto>> getHodSetupProgress(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String hodEmail) {
        return ResponseEntity.ok(ApiResponse.<HodSetupProgressDto>builder()
                .success(true)
                .data(academicService.getHodSetupProgress(departmentId, hodEmail))
                .build());
    }

    @PutMapping("/hod/setup-progress")
    public ResponseEntity<ApiResponse<HodSetupProgressDto>> updateHodSetupProgress(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false, defaultValue = "1") Integer currentStep,
            @RequestParam(required = false) String hodEmail) {
        return ResponseEntity.ok(ApiResponse.<HodSetupProgressDto>builder()
                .success(true)
                .message("HOD setup progress updated successfully")
                .data(academicService.updateHodSetupProgress(departmentId, currentStep, hodEmail))
                .build());
    }

    @PostMapping("/hod/setup-progress/complete")
    public ResponseEntity<ApiResponse<HodSetupProgressDto>> completeHodSetup(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String hodEmail) {
        return ResponseEntity.ok(ApiResponse.<HodSetupProgressDto>builder()
                .success(true)
                .message("HOD setup marked as completed successfully")
                .data(academicService.completeHodSetup(departmentId, hodEmail))
                .build());
    }

    // --- Programme Coordinator Summary & Setup Progress ---
    @GetMapping("/coordinator/programme-summary")
    public ResponseEntity<ApiResponse<ProgrammeCoordinatorSummaryDto>> getProgrammeCoordinatorSummary(
            @RequestParam(required = false) String coordinatorEmail,
            @RequestParam(required = false) String programmeId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeCoordinatorSummaryDto>builder()
                .success(true)
                .data(academicService.getProgrammeCoordinatorSummary(coordinatorEmail, programmeId))
                .build());
    }

    @GetMapping("/coordinator/setup-progress")
    public ResponseEntity<ApiResponse<ProgrammeCoordinatorSetupProgressDto>> getProgrammeCoordinatorSetupProgress(
            @RequestParam(required = false) String coordinatorEmail,
            @RequestParam(required = false) String programmeId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeCoordinatorSetupProgressDto>builder()
                .success(true)
                .data(academicService.getProgrammeCoordinatorSetupProgress(coordinatorEmail, programmeId))
                .build());
    }

    @PutMapping("/coordinator/setup-progress")
    public ResponseEntity<ApiResponse<ProgrammeCoordinatorSetupProgressDto>> updateProgrammeCoordinatorSetupProgress(
            @RequestParam(required = false) String coordinatorEmail,
            @RequestParam(required = false) String programmeId,
            @RequestParam(required = false, defaultValue = "1") Integer currentStep) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeCoordinatorSetupProgressDto>builder()
                .success(true)
                .message("Programme Coordinator setup progress updated successfully")
                .data(academicService.updateProgrammeCoordinatorSetupProgress(coordinatorEmail, programmeId, currentStep))
                .build());
    }

    @PostMapping("/coordinator/setup-progress/complete")
    public ResponseEntity<ApiResponse<ProgrammeCoordinatorSetupProgressDto>> completeProgrammeCoordinatorSetup(
            @RequestParam(required = false) String coordinatorEmail,
            @RequestParam(required = false) String programmeId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeCoordinatorSetupProgressDto>builder()
                .success(true)
                .message("Programme Coordinator setup marked as completed successfully")
                .data(academicService.completeProgrammeCoordinatorSetup(coordinatorEmail, programmeId))
                .build());
    }

    @GetMapping("/course-coordinator/summary")
    public ResponseEntity<ApiResponse<CourseCoordinatorSummaryDto>> getCourseCoordinatorSummary(
            @RequestParam(required = false) String coordinatorEmail) {
        return ResponseEntity.ok(ApiResponse.<CourseCoordinatorSummaryDto>builder()
                .success(true)
                .data(academicService.getCourseCoordinatorSummary(coordinatorEmail))
                .build());
    }

    @GetMapping("/course-coordinator/setup-progress")
    public ResponseEntity<ApiResponse<CourseCoordinatorSetupProgressDto>> getCourseCoordinatorSetupProgress(
            @RequestParam(required = false) String coordinatorEmail,
            @RequestParam(required = false) String courseId) {
        return ResponseEntity.ok(ApiResponse.<CourseCoordinatorSetupProgressDto>builder()
                .success(true)
                .data(academicService.getCourseCoordinatorSetupProgress(coordinatorEmail, courseId))
                .build());
    }

    @PostMapping("/course-coordinator/setup-progress")
    public ResponseEntity<ApiResponse<CourseCoordinatorSetupProgressDto>> updateCourseCoordinatorSetupProgress(
            @RequestParam(required = false) String coordinatorEmail,
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false, defaultValue = "1") Integer currentStep) {
        return ResponseEntity.ok(ApiResponse.<CourseCoordinatorSetupProgressDto>builder()
                .success(true)
                .message("Course Coordinator setup progress updated successfully")
                .data(academicService.updateCourseCoordinatorSetupProgress(coordinatorEmail, courseId, currentStep))
                .build());
    }

    @PostMapping("/course-coordinator/setup-progress/complete")
    public ResponseEntity<ApiResponse<CourseCoordinatorSetupProgressDto>> completeCourseCoordinatorSetup(
            @RequestParam(required = false) String coordinatorEmail,
            @RequestParam(required = false) String courseId) {
        return ResponseEntity.ok(ApiResponse.<CourseCoordinatorSetupProgressDto>builder()
                .success(true)
                .message("Course Coordinator setup marked as completed successfully")
                .data(academicService.completeCourseCoordinatorSetup(coordinatorEmail, courseId))
                .build());
    }

    // --- Schools ---
    @GetMapping("/schools")
    public ResponseEntity<ApiResponse<List<School>>> getSchools() {
        return ResponseEntity.ok(ApiResponse.<List<School>>builder()
                .success(true)
                .data(academicService.getAllSchools())
                .build());
    }

    @GetMapping("/schools/{id}")
    public ResponseEntity<ApiResponse<School>> getSchoolById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<School>builder()
                .success(true)
                .data(academicService.getSchoolById(id))
                .build());
    }

    @PostMapping("/schools")
    public ResponseEntity<ApiResponse<School>> saveSchool(@RequestBody School school) {
        return ResponseEntity.ok(ApiResponse.<School>builder()
                .success(true)
                .message("School saved successfully")
                .data(academicService.saveSchool(school))
                .build());
    }

    @PutMapping("/schools/{id}")
    public ResponseEntity<ApiResponse<School>> updateSchool(@PathVariable String id, @RequestBody School school) {
        return ResponseEntity.ok(ApiResponse.<School>builder()
                .success(true)
                .message("School updated successfully")
                .data(academicService.updateSchool(id, school))
                .build());
    }

    // --- Departments ---
    @GetMapping("/departments")
    public ResponseEntity<ApiResponse<List<Department>>> getDepartments(@RequestParam(required = false) String schoolId) {
        List<Department> list = (schoolId != null && !schoolId.isBlank())
                ? academicService.getDepartmentsBySchool(schoolId)
                : academicService.getAllDepartments();
        return ResponseEntity.ok(ApiResponse.<List<Department>>builder()
                .success(true)
                .data(list)
                .build());
    }

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<Department>> saveDepartment(@RequestBody Department department) {
        return ResponseEntity.ok(ApiResponse.<Department>builder()
                .success(true)
                .message("Department saved successfully")
                .data(academicService.saveDepartment(department))
                .build());
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable String id) {
        academicService.deleteDepartment(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Department deleted").build());
    }

    // --- Programmes ---
    @GetMapping("/programmes")
    public ResponseEntity<ApiResponse<List<Programme>>> getProgrammes(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String coordinatorEmail) {
        List<Programme> list = (coordinatorEmail != null && !coordinatorEmail.isBlank())
                ? academicService.getProgrammesByCoordinatorEmail(coordinatorEmail)
                : (departmentId != null && !departmentId.isBlank())
                ? academicService.getProgrammesByDepartment(departmentId)
                : (schoolId != null && !schoolId.isBlank())
                ? academicService.getProgrammesBySchool(schoolId)
                : academicService.getAllProgrammes();
        return ResponseEntity.ok(ApiResponse.<List<Programme>>builder()
                .success(true)
                .data(list)
                .build());
    }

    @PostMapping("/programmes")
    public ResponseEntity<ApiResponse<Programme>> saveProgramme(@RequestBody Programme programme) {
        return ResponseEntity.ok(ApiResponse.<Programme>builder()
                .success(true)
                .message("Programme saved successfully")
                .data(academicService.saveProgramme(programme))
                .build());
    }

    @DeleteMapping("/programmes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProgramme(@PathVariable String id) {
        academicService.deleteProgramme(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Programme deleted").build());
    }

    // --- Batches ---
    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<Batch>>> getBatches(@RequestParam(required = false) String programmeId) {
        List<Batch> batches = programmeId != null ? academicService.getBatchesByProgramme(programmeId) : academicService.getAllBatches();
        return ResponseEntity.ok(ApiResponse.<List<Batch>>builder().success(true).data(batches).build());
    }

    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<Batch>> saveBatch(@RequestBody Batch batch) {
        return ResponseEntity.ok(ApiResponse.<Batch>builder()
                .success(true)
                .message("Batch saved successfully")
                .data(academicService.saveBatch(batch))
                .build());
    }

    @DeleteMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(@PathVariable String id) {
        academicService.deleteBatch(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Batch deleted").build());
    }

    // --- Courses ---
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<List<Course>>> getCourses(@RequestParam(required = false) String programmeId) {
        List<Course> courses = programmeId != null ? academicService.getCoursesByProgramme(programmeId) : academicService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.<List<Course>>builder().success(true).data(courses).build());
    }

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<Course>> saveCourse(@RequestBody Course course) {
        return ResponseEntity.ok(ApiResponse.<Course>builder()
                .success(true)
                .message("Course saved successfully")
                .data(academicService.saveCourse(course))
                .build());
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable String id) {
        academicService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Course deleted").build());
    }

    // --- Course Offerings (Batch Specific) ---
    @GetMapping("/course-offerings")
    public ResponseEntity<ApiResponse<List<com.dypiu.nba.entity.CourseOffering>>> getCourseOfferings(@RequestParam String batchId) {
        return ResponseEntity.ok(ApiResponse.<List<com.dypiu.nba.entity.CourseOffering>>builder()
                .success(true)
                .data(academicService.getCourseOfferingsByBatch(batchId))
                .build());
    }

    @PostMapping("/course-offerings")
    public ResponseEntity<ApiResponse<com.dypiu.nba.entity.CourseOffering>> saveCourseOffering(@RequestBody com.dypiu.nba.entity.CourseOffering offering) {
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.entity.CourseOffering>builder()
                .success(true)
                .message("Course Offering saved successfully")
                .data(academicService.saveCourseOffering(offering))
                .build());
    }

    @DeleteMapping("/course-offerings/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourseOffering(@PathVariable String id) {
        academicService.deleteCourseOffering(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Course Offering deleted").build());
    }

    // --- Students ---
    @GetMapping("/batches/{batchId}/students")
    public ResponseEntity<ApiResponse<List<Student>>> getStudentsByBatch(@PathVariable String batchId) {
        return ResponseEntity.ok(ApiResponse.<List<Student>>builder()
                .success(true)
                .data(academicService.getStudentsByBatch(batchId))
                .build());
    }

    @PostMapping("/batches/{batchId}/students")
    public ResponseEntity<ApiResponse<Student>> saveStudent(@PathVariable String batchId, @RequestBody Student student) {
        student.setBatchId(batchId);
        return ResponseEntity.ok(ApiResponse.<Student>builder()
                .success(true)
                .message("Student saved successfully")
                .data(academicService.saveStudent(student))
                .build());
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable String id) {
        academicService.deleteStudent(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Student deleted").build());
    }
}
