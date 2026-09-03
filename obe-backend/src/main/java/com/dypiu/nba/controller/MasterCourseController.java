package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.CourseAtrReportDto;
import com.dypiu.nba.dto.CourseAttainmentReportDto;
import com.dypiu.nba.entity.MasterCourse;
import com.dypiu.nba.service.AcademicService;
import com.dypiu.nba.service.AtrService;
import com.dypiu.nba.service.AttainmentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/master-courses", "/api/v1/master-courses"})
@RequiredArgsConstructor
public class MasterCourseController {

    private final AcademicService academicService;
    private final AttainmentReportService attainmentReportService;
    private final AtrService atrService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MasterCourse>>> getAllMasterCourses(
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId) {
        String effectiveProgId = (masterProgrammeId != null && !masterProgrammeId.isBlank()) ? masterProgrammeId : masterProgrammeId;
        String effectiveProgrammeBatchId = (programmeBatchId != null && !programmeBatchId.isBlank()) ? programmeBatchId : programmeBatchId;
        List<MasterCourse> courses = (effectiveProgId != null && !effectiveProgId.isBlank())
                ? academicService.getCoursesByProgramme(effectiveProgId, effectiveProgrammeBatchId)
                : academicService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.<List<MasterCourse>>builder()
                .success(true)
                .data(courses)
                .build());
    }

    @GetMapping("/{masterCourseId}")
    public ResponseEntity<ApiResponse<MasterCourse>> getMasterCourseById(
            @PathVariable String masterCourseId) {
        return ResponseEntity.ok(ApiResponse.<MasterCourse>builder()
                .success(true)
                .data(academicService.getCourseById(masterCourseId))
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MasterCourse>> createMasterCourse(
            @RequestBody MasterCourse course) {
        return ResponseEntity.ok(ApiResponse.<MasterCourse>builder()
                .success(true)
                .message("MasterCourse created successfully")
                .data(academicService.saveCourse(course))
                .build());
    }

    @PutMapping("/{masterCourseId}")
    public ResponseEntity<ApiResponse<MasterCourse>> updateMasterCourse(
            @PathVariable String masterCourseId,
            @RequestBody MasterCourse course) {
        course.setId(masterCourseId);
        return ResponseEntity.ok(ApiResponse.<MasterCourse>builder()
                .success(true)
                .message("MasterCourse updated successfully")
                .data(academicService.saveCourse(course))
                .build());
    }

    @DeleteMapping("/{masterCourseId}")
    public ResponseEntity<ApiResponse<Void>> deleteMasterCourse(
            @PathVariable String masterCourseId) {
        academicService.deleteCourse(masterCourseId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("MasterCourse deleted successfully")
                .build());
    }

    @GetMapping("/{masterCourseId}/historical/attainment-reports")
    public ResponseEntity<ApiResponse<List<CourseAttainmentReportDto>>> getHistoricalAttainmentReports(
            @PathVariable String masterCourseId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseAttainmentReportDto>>builder()
                .success(true)
                .data(attainmentReportService.getHistoricalCourseAttainmentReports(masterCourseId))
                .build());
    }

    @GetMapping("/{masterCourseId}/historical/atrs")
    public ResponseEntity<ApiResponse<List<CourseAtrReportDto>>> getHistoricalCourseAtrs(
            @PathVariable String masterCourseId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseAtrReportDto>>builder()
                .success(true)
                .data(atrService.getHistoricalCourseAtrs(masterCourseId))
                .build());
    }
}
