package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.CreateSuggestionRequestDto;
import com.dypiu.nba.dto.SuggestionDto;
import com.dypiu.nba.service.SuggestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/suggestions", "/api/v1/suggestions"})
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SuggestionController {

    private final SuggestionService suggestionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SuggestionDto>> createSuggestion(@Valid @RequestBody CreateSuggestionRequestDto req) {
        SuggestionDto created = suggestionService.createSuggestion(req);
        return ResponseEntity.ok(ApiResponse.<SuggestionDto>builder()
                .success(true)
                .message("Suggestion submitted successfully")
                .data(created)
                .build());
    }

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<Page<SuggestionDto>>> getReceivedSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SuggestionDto> suggestions = suggestionService.getReceivedSuggestions(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<SuggestionDto>>builder()
                .success(true)
                .message("Received suggestions retrieved successfully")
                .data(suggestions)
                .build());
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<Page<SuggestionDto>>> getSentSuggestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<SuggestionDto> suggestions = suggestionService.getSentSuggestions(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<SuggestionDto>>builder()
                .success(true)
                .message("Sent suggestions retrieved successfully")
                .data(suggestions)
                .build());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        suggestionService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Suggestion marked as read")
                .build());
    }
}
