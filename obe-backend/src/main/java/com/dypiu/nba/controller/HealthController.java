package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("DYPIU NBA Attainment Backend is running successfully")
                .data(Map.of(
                        "status", "UP",
                        "system", "DYPIU NBA Attainment System",
                        "javaVersion", System.getProperty("java.version"),
                        "springBoot", "3.3.2",
                        "database", "PostgreSQL",
                        "migrationEngine", "Flyway"
                ))
                .build());
    }
}
