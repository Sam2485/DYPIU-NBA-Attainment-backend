package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.entity.CourseAtr;
import com.dypiu.nba.entity.ProgrammeAtr;
import com.dypiu.nba.service.AtrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/atr")
@RequiredArgsConstructor
public class AtrController {

    private final AtrService atrService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<CourseAtr>>> getCourseAtrs(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseAtr>>builder()
                .success(true)
                .data(atrService.getCourseAtrs(courseId))
                .build());
    }

    @PostMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<CourseAtr>>> saveCourseAtrs(@PathVariable String courseId, @RequestBody List<CourseAtr> atrs) {
        return ResponseEntity.ok(ApiResponse.<List<CourseAtr>>builder()
                .success(true)
                .message("Course ATR saved successfully")
                .data(atrService.saveCourseAtrs(courseId, atrs))
                .build());
    }

    @GetMapping("/programme/{programmeId}")
    public ResponseEntity<ApiResponse<ProgrammeAtr>> getProgrammeAtr(@PathVariable String programmeId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtr>builder()
                .success(true)
                .data(atrService.getProgrammeAtr(programmeId).orElse(null))
                .build());
    }

    @PostMapping("/programme/{programmeId}")
    public ResponseEntity<ApiResponse<ProgrammeAtr>> saveProgrammeAtr(@PathVariable String programmeId, @RequestBody ProgrammeAtr atr) {
        atr.setProgrammeId(programmeId);
        return ResponseEntity.ok(ApiResponse.<ProgrammeAtr>builder()
                .success(true)
                .message("Programme ATR saved successfully")
                .data(atrService.saveProgrammeAtr(atr))
                .build());
    }
}
