package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.ProgrammeTargetDto;
import com.dypiu.nba.entity.*;
import com.dypiu.nba.service.OutcomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/outcomes")
@RequiredArgsConstructor
public class OutcomeController {

    private final OutcomeService outcomeService;

    @GetMapping("/programmes/{programmeId}/pos")
    public ResponseEntity<ApiResponse<List<ProgrammeOutcome>>> getPOs(@PathVariable String programmeId) {
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeOutcome>>builder()
                .success(true)
                .data(outcomeService.getPOsByProgramme(programmeId))
                .build());
    }

    @PostMapping("/programmes/{programmeId}/pos")
    public ResponseEntity<ApiResponse<List<ProgrammeOutcome>>> savePOs(@PathVariable String programmeId, @RequestBody List<ProgrammeOutcome> pos) {
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeOutcome>>builder()
                .success(true)
                .message("POs saved successfully")
                .data(outcomeService.savePOs(programmeId, pos))
                .build());
    }

    @GetMapping("/programmes/{programmeId}/psos")
    public ResponseEntity<ApiResponse<List<ProgrammeSpecificOutcome>>> getPSOs(@PathVariable String programmeId) {
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeSpecificOutcome>>builder()
                .success(true)
                .data(outcomeService.getPSOsByProgramme(programmeId))
                .build());
    }

    @PostMapping("/programmes/{programmeId}/psos")
    public ResponseEntity<ApiResponse<List<ProgrammeSpecificOutcome>>> savePSOs(@PathVariable String programmeId, @RequestBody List<ProgrammeSpecificOutcome> psos) {
        return ResponseEntity.ok(ApiResponse.<List<ProgrammeSpecificOutcome>>builder()
                .success(true)
                .message("PSOs saved successfully")
                .data(outcomeService.savePSOs(programmeId, psos))
                .build());
    }

    @GetMapping("/programmes/{programmeId}/peos")
    public ResponseEntity<ApiResponse<List<PeoOutcome>>> getPEOs(@PathVariable String programmeId) {
        return ResponseEntity.ok(ApiResponse.<List<PeoOutcome>>builder()
                .success(true)
                .data(outcomeService.getPEOsByProgramme(programmeId))
                .build());
    }

    @PostMapping("/programmes/{programmeId}/peos")
    public ResponseEntity<ApiResponse<List<PeoOutcome>>> savePEOs(@PathVariable String programmeId, @RequestBody List<PeoOutcome> peos) {
        return ResponseEntity.ok(ApiResponse.<List<PeoOutcome>>builder()
                .success(true)
                .message("PEOs saved successfully")
                .data(outcomeService.savePEOs(programmeId, peos))
                .build());
    }

    @GetMapping("/courses/{courseId}/cos")
    public ResponseEntity<ApiResponse<List<CourseOutcome>>> getCOs(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<List<CourseOutcome>>builder()
                .success(true)
                .data(outcomeService.getCOsByCourse(courseId))
                .build());
    }

    @PostMapping("/courses/{courseId}/cos")
    public ResponseEntity<ApiResponse<List<CourseOutcome>>> saveCOs(@PathVariable String courseId, @RequestBody List<CourseOutcome> cos) {
        return ResponseEntity.ok(ApiResponse.<List<CourseOutcome>>builder()
                .success(true)
                .message("COs saved successfully")
                .data(outcomeService.saveCOs(courseId, cos))
                .build());
    }

    // --- Target Benchmark Levels ---
    @GetMapping("/programmes/{programmeId}/targets")
    public ResponseEntity<ApiResponse<ProgrammeTargetDto>> getProgrammeTargets(@PathVariable String programmeId) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeTargetDto>builder()
                .success(true)
                .data(outcomeService.getProgrammeTargets(programmeId))
                .build());
    }

    @PostMapping("/programmes/{programmeId}/targets")
    public ResponseEntity<ApiResponse<ProgrammeTargetDto>> saveProgrammeTargets(@PathVariable String programmeId, @RequestBody ProgrammeTargetDto targets) {
        return ResponseEntity.ok(ApiResponse.<ProgrammeTargetDto>builder()
                .success(true)
                .message("Programme targets saved successfully")
                .data(outcomeService.saveProgrammeTargets(programmeId, targets))
                .build());
    }
}
