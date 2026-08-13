package com.dypiu.nba.controller;

import com.dypiu.nba.dto.DirectorSetupProgressDto;
import com.dypiu.nba.dto.DirectorSchoolSummaryDto;
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

    // --- Director School Summary ---
    @GetMapping("/director/school-summary")
    public ResponseEntity<ApiResponse<DirectorSchoolSummaryDto>> getDirectorSchoolSummary(
            @RequestParam(required = false) String schoolId) {
        return ResponseEntity.ok(ApiResponse.<DirectorSchoolSummaryDto>builder()
                .success(true)
                .data(academicService.getDirectorSchoolSummary(schoolId))
                .build());
    }

    // --- Director Setup Progress ---
    @GetMapping("/director/setup-progress")
    public ResponseEntity<ApiResponse<DirectorSetupProgressDto>> getDirectorSetupProgress(
            @RequestParam(required = false) String schoolId) {
        return ResponseEntity.ok(ApiResponse.<DirectorSetupProgressDto>builder()
                .success(true)
                .data(academicService.getDirectorSetupProgress(schoolId))
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
    public ResponseEntity<ApiResponse<List<Department>>> getDepartments() {
        return ResponseEntity.ok(ApiResponse.<List<Department>>builder()
                .success(true)
                .data(academicService.getAllDepartments())
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
    public ResponseEntity<ApiResponse<List<Programme>>> getProgrammes() {
        return ResponseEntity.ok(ApiResponse.<List<Programme>>builder()
                .success(true)
                .data(academicService.getAllProgrammes())
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
