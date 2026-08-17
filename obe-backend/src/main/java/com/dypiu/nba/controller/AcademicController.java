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
    private final com.dypiu.nba.service.OutcomeService outcomeService;


    // --- Users by Role ---
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsersByRole(
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(ApiResponse.<List<UserDto>>builder()
                .success(true)
                .data(academicService.getUsersByRole(role))
                .build());
    }

    // --- Director School Summary ---
    @GetMapping("/director/school-summary")
    public ResponseEntity<ApiResponse<DirectorSchoolSummaryDto>> getDirectorSchoolSummary(
            @RequestParam(required = false) String directorEmail,
            java.security.Principal principal) {
        String email = (directorEmail != null && !directorEmail.isBlank())
                ? directorEmail
                : (principal != null ? principal.getName() : null);
        return ResponseEntity.ok(ApiResponse.<DirectorSchoolSummaryDto>builder()
                .success(true)
                .data(academicService.getDirectorSchoolSummary(email))
                .build());
    }

    // --- Director Department Summary ---
    @GetMapping("/director/department-summary")
    public ResponseEntity<ApiResponse<List<DepartmentSummaryDto>>> getDepartmentSummary(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String directorEmail,
            java.security.Principal principal) {
        String email = (directorEmail != null && !directorEmail.isBlank())
                ? directorEmail
                : (principal != null ? principal.getName() : null);
        return ResponseEntity.ok(ApiResponse.<List<DepartmentSummaryDto>>builder()
                .success(true)
                .data(academicService.getDepartmentSummary(schoolId, email))
                .build());
    }

    // --- Director Setup Progress ---
    @GetMapping("/director/setup-progress")
    public ResponseEntity<ApiResponse<DirectorSetupProgressDto>> getDirectorSetupProgress(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String directorEmail,
            @RequestParam(required = false) String id,
            java.security.Principal principal) {
        String targetSchool = (schoolId != null && !schoolId.isBlank()) ? schoolId : id;
        String email = (directorEmail != null && !directorEmail.isBlank())
                ? directorEmail
                : (principal != null ? principal.getName() : null);
        return ResponseEntity.ok(ApiResponse.<DirectorSetupProgressDto>builder()
                .success(true)
                .data(academicService.getDirectorSetupProgress(targetSchool, email))
                .build());
    }

    @RequestMapping(value = "/director/setup-progress", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<DirectorSetupProgressDto>> updateDirectorSetupProgress(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) Integer step,
            @RequestParam(required = false) Integer currentStep,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            java.security.Principal principal) {
        String targetSchool = schoolId != null ? schoolId : id;
        Integer targetStep = step != null ? step : currentStep;
        if (body != null) {
            if (targetSchool == null && body.containsKey("identifier")) targetSchool = String.valueOf(body.get("identifier"));
            if (targetSchool == null && body.containsKey("schoolId")) targetSchool = String.valueOf(body.get("schoolId"));
            if (targetStep == null && body.containsKey("step")) {
                try { targetStep = Integer.parseInt(String.valueOf(body.get("step"))); } catch (Exception ignored) {}
            }
        }
        if (targetStep == null) targetStep = 1;
        return ResponseEntity.ok(ApiResponse.<DirectorSetupProgressDto>builder()
                .success(true)
                .message("Director setup progress updated successfully")
                .data(academicService.updateDirectorSetupProgress(targetSchool, targetStep))
                .build());
    }

    // --- HOD Department Summary ---
    @GetMapping("/hod/department-summary")
    public ResponseEntity<ApiResponse<HodDepartmentSummaryDto>> getHodDepartmentSummary(
            @RequestParam(required = false) String hodEmail,
            java.security.Principal principal) {
        String email = (hodEmail != null && !hodEmail.isBlank())
                ? hodEmail
                : (principal != null ? principal.getName() : null);
        System.out.println("\n>>> [CONTROLLER] GET /api/v1/academic/hod/department-summary | hodEmail param: " + hodEmail + " | principal: " + (principal != null ? principal.getName() : "null") + " | resolved email: " + email);
        HodDepartmentSummaryDto data = academicService.getHodDepartmentSummary(email);
        System.out.println("<<< [CONTROLLER] GET /api/v1/academic/hod/department-summary SUCCESS | deptId: " + data.getDeptId() + " | deptName: " + data.getDeptName() + " | hodEmail: " + data.getHodEmail());
        return ResponseEntity.ok(ApiResponse.<HodDepartmentSummaryDto>builder()
                .success(true)
                .data(data)
                .build());
    }

    // --- HOD Setup Progress ---
    @GetMapping("/hod/setup-progress")
    public ResponseEntity<ApiResponse<HodSetupProgressDto>> getHodSetupProgress(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String hodEmail,
            @RequestParam(required = false) String id,
            java.security.Principal principal) {
        String email = (hodEmail != null && !hodEmail.isBlank())
                ? hodEmail
                : (principal != null ? principal.getName() : null);
        String deptId = (departmentId != null && !departmentId.isBlank()) ? departmentId : id;
        System.out.println("\n>>> [CONTROLLER] GET /api/v1/academic/hod/setup-progress | deptId: " + deptId + " | email: " + email);
        HodSetupProgressDto data = academicService.getHodSetupProgress(deptId, email);
        System.out.println("<<< [CONTROLLER] GET /api/v1/academic/hod/setup-progress SUCCESS | currentStep: " + data.getCurrentStep() + " | status: " + data.getOverallStatus());
        return ResponseEntity.ok(ApiResponse.<HodSetupProgressDto>builder()
                .success(true)
                .data(data)
                .build());
    }

    @RequestMapping(value = "/hod/setup-progress", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<HodSetupProgressDto>> updateHodSetupProgress(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String hodEmail,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) Integer step,
            @RequestParam(required = false) Integer currentStep,
            @RequestBody(required = false) java.util.Map<String, Object> body,
            java.security.Principal principal) {
        String deptId = departmentId != null ? departmentId : id;
        String email = (hodEmail != null && !hodEmail.isBlank())
                ? hodEmail
                : (principal != null ? principal.getName() : null);
        Integer targetStep = step != null ? step : currentStep;
        if (body != null) {
            if (deptId == null && body.containsKey("identifier")) deptId = String.valueOf(body.get("identifier"));
            if (deptId == null && body.containsKey("departmentId")) deptId = String.valueOf(body.get("departmentId"));
            if (email == null && body.containsKey("email")) email = String.valueOf(body.get("email"));
            if (targetStep == null && body.containsKey("step")) {
                try { targetStep = Integer.parseInt(String.valueOf(body.get("step"))); } catch (Exception ignored) {}
            }
        }
        if (targetStep == null) targetStep = 1;
        System.out.println("\n>>> [CONTROLLER] POST/PUT /api/v1/academic/hod/setup-progress | deptId: " + deptId + " | step: " + targetStep + " | email: " + email);
        HodSetupProgressDto updated = academicService.updateHodSetupProgress(deptId, targetStep, email);
        System.out.println("<<< [CONTROLLER] POST/PUT /api/v1/academic/hod/setup-progress SUCCESS | newStep: " + updated.getCurrentStep() + " | completedSteps: " + updated.getCompletedSteps());
        return ResponseEntity.ok(ApiResponse.<HodSetupProgressDto>builder()
                .success(true)
                .message("HOD setup progress updated successfully")
                .data(updated)
                .build());
    }

    @PostMapping("/hod/setup-progress/complete")
    public ResponseEntity<ApiResponse<HodSetupProgressDto>> completeHodSetup(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String hodEmail,
            @RequestParam(required = false) String id,
            java.security.Principal principal) {
        String email = (hodEmail != null && !hodEmail.isBlank())
                ? hodEmail
                : (principal != null ? principal.getName() : null);
        String deptId = (departmentId != null && !departmentId.isBlank()) ? departmentId : id;
        System.out.println("\n>>> [CONTROLLER] POST /api/v1/academic/hod/setup-progress/complete | deptId: " + deptId + " | email: " + email);
        HodSetupProgressDto completed = academicService.completeHodSetup(deptId, email);
        System.out.println("<<< [CONTROLLER] POST /api/v1/academic/hod/setup-progress/complete SUCCESS | status: " + completed.getOverallStatus());
        return ResponseEntity.ok(ApiResponse.<HodSetupProgressDto>builder()
                .success(true)
                .message("HOD setup marked as completed successfully")
                .data(completed)
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

    @PutMapping("/departments/{id}")
    public ResponseEntity<ApiResponse<Department>> updateDepartment(
            @PathVariable String id,
            @RequestBody Department department) {
        department.setId(id);
        return ResponseEntity.ok(ApiResponse.<Department>builder()
                .success(true)
                .message("Department updated successfully")
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

    @GetMapping("/programmes/{id}")
    public ResponseEntity<ApiResponse<Programme>> getProgrammeById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<Programme>builder()
                .success(true)
                .data(academicService.getProgrammeById(id))
                .build());
    }

    @PostMapping("/programmes")
    public ResponseEntity<ApiResponse<Programme>> saveProgramme(@RequestBody Programme programme) {
        System.out.println("\n>>> [CONTROLLER] POST /api/v1/academic/programmes | id: " + (programme != null ? programme.getId() : "null") + " | name: " + (programme != null ? programme.getName() : "null") + " | coordinator: " + (programme != null ? programme.getCoordinator() : "null") + " | coordinatorEmail: " + (programme != null ? programme.getCoordinatorEmail() : "null"));
        Programme saved = academicService.saveProgramme(programme);
        System.out.println("<<< [CONTROLLER] POST /api/v1/academic/programmes SUCCESS | savedId: " + saved.getId() + " | coordinator: " + saved.getCoordinator() + " | coordinatorEmail: " + saved.getCoordinatorEmail());
        return ResponseEntity.ok(ApiResponse.<Programme>builder()
                .success(true)
                .message("Programme saved successfully")
                .data(saved)
                .build());
    }

    @PutMapping("/programmes/{id}")
    public ResponseEntity<ApiResponse<Programme>> updateProgramme(
            @PathVariable String id,
            @RequestBody Programme programme) {
        programme.setId(id);
        System.out.println("\n>>> [CONTROLLER] PUT /api/v1/academic/programmes/" + id + " | name: " + programme.getName() + " | coordinator: " + programme.getCoordinator() + " | coordinatorEmail: " + programme.getCoordinatorEmail());
        Programme saved = academicService.saveProgramme(programme);
        System.out.println("<<< [CONTROLLER] PUT /api/v1/academic/programmes/" + id + " SUCCESS | savedId: " + saved.getId() + " | coordinator: " + saved.getCoordinator() + " | coordinatorEmail: " + saved.getCoordinatorEmail());
        return ResponseEntity.ok(ApiResponse.<Programme>builder()
                .success(true)
                .message("Programme updated successfully")
                .data(saved)
                .build());
    }

    @PutMapping("/programmes/{id}/coordinator")
    public ResponseEntity<ApiResponse<Programme>> updateProgrammeCoordinator(
            @PathVariable String id,
            @RequestBody java.util.Map<String, Object> body) {
        String coordinator = body.containsKey("coordinator") ? String.valueOf(body.get("coordinator")) : (body.containsKey("coordinatorId") ? String.valueOf(body.get("coordinatorId")) : null);
        String coordinatorEmail = body.containsKey("coordinatorEmail") ? String.valueOf(body.get("coordinatorEmail")) : null;
        System.out.println("\n>>> [CONTROLLER] PUT /api/v1/academic/programmes/" + id + "/coordinator | coordinator: " + coordinator + " | email: " + coordinatorEmail);
        Programme prog = academicService.getProgrammeById(id);
        if (prog == null) {
            prog = Programme.builder().id(id).name("Programme " + id).code(id).build();
        }
        if (coordinator != null) prog.setCoordinator(coordinator);
        if (coordinatorEmail != null) prog.setCoordinatorEmail(coordinatorEmail);
        Programme saved = academicService.saveProgramme(prog);
        System.out.println("<<< [CONTROLLER] PUT /api/v1/academic/programmes/" + id + "/coordinator SUCCESS | coordinator: " + saved.getCoordinator());
        return ResponseEntity.ok(ApiResponse.<Programme>builder()
                .success(true)
                .message("Programme coordinator updated successfully")
                .data(saved)
                .build());
    }

    @DeleteMapping("/programmes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProgramme(@PathVariable String id) {
        System.out.println("\n>>> [CONTROLLER] DELETE /api/v1/academic/programmes/" + id);
        academicService.deleteProgramme(id);
        System.out.println("<<< [CONTROLLER] DELETE /api/v1/academic/programmes/" + id + " SUCCESS");
        return ResponseEntity.ok(ApiResponse.<Void>builder().success(true).message("Programme deleted").build());
    }

    // --- Batches ---
    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<List<Batch>>> getBatches(
            @RequestParam(required = false) String programmeId,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) String role,
            java.security.Principal principal) {
        String email = (userEmail != null && !userEmail.isBlank()) ? userEmail : (principal != null ? principal.getName() : null);
        List<Batch> batches = academicService.getBatchesScoped(programmeId, email, role);
        return ResponseEntity.ok(ApiResponse.<List<Batch>>builder().success(true).data(batches).build());
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<Batch>> getBatchById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<Batch>builder()
                .success(true)
                .data(academicService.getAllBatches().stream().filter(b -> b.getId().equals(id)).findFirst().orElse(null))
                .build());
    }

    @GetMapping("/batches/{batchId}/context")
    public ResponseEntity<ApiResponse<com.dypiu.nba.dto.BatchContextDto>> getBatchContext(@PathVariable String batchId) {
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.dto.BatchContextDto>builder()
                .success(true)
                .data(academicService.getBatchContext(batchId))
                .build());
    }

    @PostMapping("/batches")
    public ResponseEntity<ApiResponse<Batch>> saveBatch(@RequestBody Batch batch) {
        return ResponseEntity.ok(ApiResponse.<Batch>builder()
                .success(true)
                .message("Batch saved successfully")
                .data(academicService.saveBatch(batch))
                .build());
    }

    @PutMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<Batch>> updateBatch(
            @PathVariable String id,
            @RequestBody Batch batch) {
        batch.setId(id);
        return ResponseEntity.ok(ApiResponse.<Batch>builder()
                .success(true)
                .message("Batch updated successfully")
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

    @GetMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Course>> getCourseById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.<Course>builder()
                .success(true)
                .data(academicService.getCourseById(id))
                .build());
    }

    @PostMapping("/courses")
    public ResponseEntity<ApiResponse<Course>> saveCourse(@RequestBody Course course) {
        return ResponseEntity.ok(ApiResponse.<Course>builder()
                .success(true)
                .message("Course saved successfully")
                .data(academicService.saveCourse(course))
                .build());
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<ApiResponse<Course>> updateCourse(
            @PathVariable String id,
            @RequestBody Course course) {
        course.setId(id);
        return ResponseEntity.ok(ApiResponse.<Course>builder()
                .success(true)
                .message("Course updated successfully")
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

    @GetMapping("/course-offerings/{offeringId}")
    public ResponseEntity<ApiResponse<com.dypiu.nba.entity.CourseOffering>> getCourseOfferingById(@PathVariable String offeringId) {
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.entity.CourseOffering>builder()
                .success(true)
                .data(academicService.getCourseOfferingsByBatch(null).stream().filter(o -> o.getId().equals(offeringId)).findFirst().orElse(null))
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

    @GetMapping("/course-offerings/{offeringId}/outcomes")
    public ResponseEntity<ApiResponse<List<CourseOutcome>>> getOfferingOutcomes(@PathVariable String offeringId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseOutcome>>builder()
                .success(true)
                .data(outcomeService.getOutcomesByOffering(offeringId))
                .build());
    }

    @PostMapping("/course-offerings/{offeringId}/outcomes")
    public ResponseEntity<ApiResponse<List<CourseOutcome>>> saveOfferingOutcomes(
            @PathVariable String offeringId,
            @RequestBody List<CourseOutcome> outcomes) {
        return ResponseEntity.ok(ApiResponse.<List<CourseOutcome>>builder()
                .success(true)
                .message("Course outcomes saved for offering")
                .data(outcomeService.saveOutcomesByOffering(offeringId, outcomes))
                .build());
    }

    @GetMapping("/course-offerings/{offeringId}/mappings")
    public ResponseEntity<ApiResponse<com.dypiu.nba.dto.CourseMappingMatrixDto>> getOfferingMappings(@PathVariable String offeringId) {
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.dto.CourseMappingMatrixDto>builder()
                .success(true)
                .data(outcomeService.getMappingsByOffering(offeringId))
                .build());
    }

    @PutMapping("/course-offerings/{offeringId}/mappings")
    public ResponseEntity<ApiResponse<com.dypiu.nba.dto.CourseMappingMatrixDto>> saveOfferingMappings(
            @PathVariable String offeringId,
            @RequestBody com.dypiu.nba.dto.CourseMappingMatrixDto dto) {
        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.dto.CourseMappingMatrixDto>builder()
                .success(true)
                .message("Course mappings saved for offering")
                .data(outcomeService.saveMappingsByOffering(offeringId, dto))
                .build());
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
