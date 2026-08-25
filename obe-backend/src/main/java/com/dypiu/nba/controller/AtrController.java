package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.entity.CourseAtr;
import com.dypiu.nba.entity.ProgrammeAtr;
import com.dypiu.nba.service.AtrService;
import com.dypiu.nba.dto.CourseAtrReportDto;
import com.dypiu.nba.dto.ProgrammeAtrReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/atr")
@RequiredArgsConstructor
public class AtrController {

    private final AtrService atrService;

    @GetMapping({"/course/{courseId}", "/courses/{courseId}"})
    public ResponseEntity<ApiResponse<List<CourseAtr>>> getCourseAtrs(
            @PathVariable String courseId,
            @RequestParam(required = false) String batchId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseAtr>>builder()
                .success(true)
                .data(atrService.getCourseAtrs(courseId))
                .build());
    }

    @RequestMapping(value = {"/course/{courseId}", "/courses/{courseId}"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<List<CourseAtr>>> saveCourseAtrs(
            @PathVariable String courseId,
            @RequestBody List<CourseAtr> atrs,
            @RequestParam(required = false) String batchId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseAtr>>builder()
                .success(true)
                .message("Course ATR saved successfully")
                .data(atrService.saveCourseAtrs(courseId, atrs))
                .build());
    }

    @GetMapping({"/programme/{programmeId}", "/programmes/{programmeId}"})
    public ResponseEntity<ApiResponse<ProgrammeAtr>> getProgrammeAtr(
            @PathVariable String programmeId,
            @RequestParam(value = "batchId", required = false) String batchId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtr>builder()
                .success(true)
                .data(atrService.getProgrammeAtrByBatch(programmeId, batchId).orElse(null))
                .build());
    }

    @GetMapping({"/programme/previous-batch/{batchId}", "/programmes/previous-batch/{batchId}"})
    public ResponseEntity<ApiResponse<ProgrammeAtr>> getPreviousBatchProgrammeAtr(@PathVariable String batchId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtr>builder()
                .success(true)
                .data(atrService.getPreviousBatchProgrammeAtr(batchId).orElse(null))
                .build());
    }

    @GetMapping({"/programme/previous-year/{batchId}", "/programmes/previous-year/{batchId}"})
    public ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> getPreviousYearProgrammeAtrReport(@PathVariable String batchId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtrReportDto>builder()
                .success(true)
                .data(atrService.getPreviousYearProgrammeAtrReport(batchId))
                .build());
    }

    @RequestMapping(value = {"/programme/{programmeId}", "/programmes/{programmeId}"}, method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<ProgrammeAtr>> saveProgrammeAtr(
            @PathVariable String programmeId,
            @RequestBody ProgrammeAtr atr,
            @RequestParam(required = false) String batchId) {
        atr.setProgrammeId(programmeId);
        if (batchId != null && (atr.getBatchId() == null || atr.getBatchId().isBlank())) {
            atr.setBatchId(batchId);
        }
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtr>builder()
                .success(true)
                .message("Programme ATR saved successfully")
                .data(atrService.saveProgrammeAtr(atr))
                .build());
    }
    @GetMapping("/historical/courses/{masterCourseId}")
    public ResponseEntity<ApiResponse<List<CourseAtrReportDto>>> getHistoricalCourseAtrs(
            @PathVariable String masterCourseId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseAtrReportDto>>builder()
                .success(true)
                .data(atrService.getHistoricalCourseAtrs(masterCourseId))
                .build());
    }

    @GetMapping("/historical/programmes/{masterProgrammeId}")
    public ResponseEntity<ApiResponse<List<ProgrammeAtrReportDto>>> getHistoricalProgrammeAtrs(
            @PathVariable String masterProgrammeId) {
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeAtrReportDto>>builder()
                .success(true)
                .data(atrService.getHistoricalProgrammeAtrs(masterProgrammeId))
                .build());
    }

}
