package com.dypiu.nba.controller;

import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.deletion.DeletionRequestStatus;
import com.dypiu.nba.dto.*;
import com.dypiu.nba.service.DeletionRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/deletion-requests")
@RequiredArgsConstructor
public class DeletionRequestController {

    private final DeletionRequestService deletionRequestService;

    @PostMapping
    public ResponseEntity<ApiResponse> requestDeletion(@Valid @RequestBody DeletionRequestCreateDto dto) {
        DeletionRequestResponseDto res = deletionRequestService.requestDeletion(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.builder()
                .success(true)
                .message("Deletion request created successfully.")
                .data(res)
                .build());
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse> rejectDeletion(@PathVariable Long id, @RequestBody(required = false) DeletionRejectDto dto) {
        DeletionRequestResponseDto res = deletionRequestService.rejectDeletion(id, dto);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Deletion request rejected successfully.")
                .data(res)
                .build());
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse> executeDeletion(@PathVariable Long id, @Valid @RequestBody DeletionExecuteDto dto) {
        DeletionRequestResponseDto res = deletionRequestService.executeDeletion(id, dto);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Deletion request executed successfully with soft-delete.")
                .data(res)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getDeletionRequests(
            @RequestParam(required = false) DeletionRequestStatus status,
            @RequestParam(required = false) ResourceType resourceType
    ) {
        List<DeletionRequestResponseDto> res = deletionRequestService.getDeletionRequests(status, resourceType);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(res)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getDeletionRequestById(@PathVariable Long id) {
        DeletionRequestResponseDto res = deletionRequestService.getDeletionRequestById(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .data(res)
                .build());
    }
}
