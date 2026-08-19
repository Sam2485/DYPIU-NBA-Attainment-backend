package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.entity.CoPoMapping;
import com.dypiu.nba.entity.CoPsoMapping;
import com.dypiu.nba.service.MappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/mappings")
@RequiredArgsConstructor
public class MappingController {

    private final MappingService mappingService;

    @GetMapping("/cos/{coId}/po")
    public ResponseEntity<ApiResponse<List<CoPoMapping>>> getCoPoMappings(@PathVariable String coId) {
        return ResponseEntity.ok(ApiResponse.<List<CoPoMapping>>builder()
                .success(true)
                .data(mappingService.getCoPoMappings(coId))
                .build());
    }

    @PostMapping("/cos/{coId}/po")
    public ResponseEntity<ApiResponse<List<CoPoMapping>>> saveCoPoMappings(@PathVariable String coId, @RequestBody List<CoPoMapping> mappings) {
        return ResponseEntity.ok(ApiResponse.<List<CoPoMapping>>builder()
                .success(true)
                .message("CO-PO mappings saved successfully")
                .data(mappingService.saveCoPoMappings(coId, mappings))
                .build());
    }

    @GetMapping("/cos/{coId}/pso")
    public ResponseEntity<ApiResponse<List<CoPsoMapping>>> getCoPsoMappings(@PathVariable String coId) {
        return ResponseEntity.ok(ApiResponse.<List<CoPsoMapping>>builder()
                .success(true)
                .data(mappingService.getCoPsoMappings(coId))
                .build());
    }

    @PostMapping("/cos/{coId}/pso")
    public ResponseEntity<ApiResponse<List<CoPsoMapping>>> saveCoPsoMappings(@PathVariable String coId, @RequestBody List<CoPsoMapping> mappings) {
        return ResponseEntity.ok(ApiResponse.<List<CoPsoMapping>>builder()
                .success(true)
                .message("CO-PSO mappings saved successfully")
                .data(mappingService.saveCoPsoMappings(coId, mappings))
                .build());
    }
}
