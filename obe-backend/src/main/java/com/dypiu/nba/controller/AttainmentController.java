package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.entity.AttainmentConfiguration;
import com.dypiu.nba.service.AttainmentCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
