package com.dypiu.nba.controller;

import com.dypiu.nba.audit.AuditAction;
import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.AuditLogPageResponseDto;
import com.dypiu.nba.dto.AuditLogResponseDto;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import com.dypiu.nba.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.ZonedDateTime;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final CurrentUserScopeService currentUserScopeService;

    private void enforceAdminOrIqac(Principal principal) {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope(principal);
        if (scope == null || (!scope.isAdmin() && !scope.isIqac())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Audit logs can only be viewed by ADMIN and IQAC.");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AuditLogPageResponseDto>> getAuditLogs(
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String actorRole,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) ResourceType resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        enforceAdminOrIqac(principal);

        AuditLogPageResponseDto result = auditLogService.getAuditLogs(
                actorId, actorRole, action, resourceType, resourceId, success, from, to, page, size
        );

        return ResponseEntity.ok(ApiResponse.<AuditLogPageResponseDto>builder()
                .success(true)
                .data(result)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuditLogResponseDto>> getAuditLogById(
            @PathVariable Long id,
            Principal principal) {

        enforceAdminOrIqac(principal);

        AuditLogResponseDto logDto = auditLogService.getAuditLogById(id);
        if (logDto == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Audit log record not found: " + id);
        }

        return ResponseEntity.ok(ApiResponse.<AuditLogResponseDto>builder()
                .success(true)
                .data(logDto)
                .build());
    }
}
