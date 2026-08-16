package com.dypiu.nba.controller;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.AttainmentConfiguration;
import com.dypiu.nba.service.AttainmentCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/attainment")
@RequiredArgsConstructor
public class AttainmentController {

    private final AttainmentCalculationService calculationService;

    @GetMapping("/config/{courseId}")
    public ResponseEntity<ApiResponse<AttainmentConfiguration>> getConfig(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<AttainmentConfiguration>builder()
                .success(true)
                .data(calculationService.getAttainmentConfig(courseId))
                .build());
    }

    @PostMapping("/config/{courseId}")
    public ResponseEntity<ApiResponse<AttainmentConfiguration>> saveConfig(@PathVariable String courseId, @RequestBody AttainmentConfiguration config) {
        return ResponseEntity.ok(ApiResponse.<AttainmentConfiguration>builder()
                .success(true)
                .message("Attainment configuration saved")
                .data(calculationService.saveAttainmentConfig(courseId, config))
                .build());
    }

    @GetMapping("/calculate/course/{courseId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateCourseCoAttainment(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("CO Attainment calculated successfully")
                .data(calculationService.calculateCourseCoAttainment(courseId))
                .build());
    }

    // --- Examination Attainment Endpoints (Sheet 2: Examination) ---

    @GetMapping("/examination/{courseId}")
    public ResponseEntity<ApiResponse<ExaminationAttainmentResultDto>> getExaminationAttainment(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<ExaminationAttainmentResultDto>builder()
                .success(true)
                .message("Examination attainment fetched successfully")
                .data(calculationService.getExaminationAttainment(courseId))
                .build());
    }

    @PostMapping("/examination/{courseId}")
    public ResponseEntity<ApiResponse<ExaminationAttainmentResultDto>> saveAndCalculateExaminationAttainment(
            @PathVariable String courseId,
            @RequestBody ExaminationMarksPayloadDto payload) {
        return ResponseEntity.ok(ApiResponse.<ExaminationAttainmentResultDto>builder()
                .success(true)
                .message("Examination threshold, out-of marks, student marks saved and attainment calculated successfully")
                .data(calculationService.calculateExaminationAttainment(courseId, payload))
                .build());
    }

    @PostMapping(value = "/examination/{courseId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExaminationAttainmentResultDto>> uploadAndProcessExaminationSheet(
            @PathVariable String courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "thresholdPercentage", required = false) BigDecimal thresholdPercentage) {
        return ResponseEntity.ok(ApiResponse.<ExaminationAttainmentResultDto>builder()
                .success(true)
                .message("Examination sheet document saved on backend server disk, parsed via Apache POI, and attainment calculated successfully")
                .data(calculationService.processAndSaveExaminationFile(courseId, file, thresholdPercentage))
                .build());
    }

    // --- Course End Survey Attainment Endpoints (Sheet 3: Course End Survey) ---

    @GetMapping("/survey/{courseId}")
    public ResponseEntity<ApiResponse<SurveyAttainmentResultDto>> getSurveyAttainment(@PathVariable String courseId) {
        return ResponseEntity.ok(ApiResponse.<SurveyAttainmentResultDto>builder()
                .success(true)
                .message("Course End Survey attainment fetched successfully")
                .data(calculationService.getSurveyAttainment(courseId))
                .build());
    }

    @PostMapping("/survey/{courseId}")
    public ResponseEntity<ApiResponse<SurveyAttainmentResultDto>> saveAndCalculateSurveyAttainment(
            @PathVariable String courseId,
            @RequestBody SurveyMarksPayloadDto payload) {
        return ResponseEntity.ok(ApiResponse.<SurveyAttainmentResultDto>builder()
                .success(true)
                .message("Course End Survey responses saved and indirect attainment calculated successfully")
                .data(calculationService.calculateSurveyAttainment(courseId, payload))
                .build());
    }

    @PostMapping(value = "/survey/{courseId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SurveyAttainmentResultDto>> uploadAndProcessSurveySheet(
            @PathVariable String courseId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.<SurveyAttainmentResultDto>builder()
                .success(true)
                .message("Course End Survey sheet document saved on backend server disk, parsed via Apache POI, and indirect attainment calculated successfully")
                .data(calculationService.processAndSaveSurveyFile(courseId, file))
                .build());
    }
}
