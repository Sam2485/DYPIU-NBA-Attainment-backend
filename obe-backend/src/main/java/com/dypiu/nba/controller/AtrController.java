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

    @GetMapping("/master-courses/{masterCourseId}")
    public ResponseEntity<ApiResponse<List<CourseAtr>>> getCourseAtrs(
            @PathVariable String masterCourseId,
            @RequestParam(required = false) String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseAtr>>builder()
                .success(true)
                .data(atrService.getCourseAtrs(masterCourseId))
                .build());
    }

    @RequestMapping(value = "/master-courses/{masterCourseId}", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<List<CourseAtr>>> saveCourseAtrs(
            @PathVariable String masterCourseId,
            @RequestBody List<CourseAtr> atrs,
            @RequestParam(required = false) String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseAtr>>builder()
                .success(true)
                .message("Course ATR saved successfully")
                .data(atrService.saveCourseAtrs(masterCourseId, atrs))
                .build());
    }

    @GetMapping("/master-programmes/{masterProgrammeId}")
    public ResponseEntity<ApiResponse<ProgrammeAtr>> getProgrammeAtr(
            @PathVariable String masterProgrammeId,
            @RequestParam(value = "programmeBatchId", required = false) String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtr>builder()
                .success(true)
                .data(atrService.getProgrammeAtrByBatch(masterProgrammeId, programmeBatchId).orElse(null))
                .build());
    }

    @GetMapping("/master-programmes/previous-batch/{programmeBatchId}")
    public ResponseEntity<ApiResponse<ProgrammeAtr>> getPreviousBatchProgrammeAtr(@PathVariable String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtr>builder()
                .success(true)
                .data(atrService.getPreviousBatchProgrammeAtr(programmeBatchId).orElse(null))
                .build());
    }

    @GetMapping("/master-programmes/previous-year/{programmeBatchId}")
    public ResponseEntity<ApiResponse<ProgrammeAtrReportDto>> getPreviousYearProgrammeAtrReport(@PathVariable String programmeBatchId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtrReportDto>builder()
                .success(true)
                .data(atrService.getPreviousYearProgrammeAtrReport(programmeBatchId))
                .build());
    }

    @RequestMapping(value = "/master-programmes/{masterProgrammeId}", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<ProgrammeAtr>> saveProgrammeAtr(
            @PathVariable String masterProgrammeId,
            @RequestBody ProgrammeAtr atr,
            @RequestParam(required = false) String programmeBatchId) {
        atr.setMasterProgrammeId(masterProgrammeId);
        if (programmeBatchId != null && (atr.getProgrammeBatchId() == null || atr.getProgrammeBatchId().isBlank())) {
            atr.setProgrammeBatchId(programmeBatchId);
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
