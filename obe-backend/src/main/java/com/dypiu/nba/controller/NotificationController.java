package com.dypiu.nba.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.dto.NotificationDto;
import com.dypiu.nba.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping({"/notifications", "/api/v1/notifications"})
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<NotificationDto> notifications = notificationService.getNotifications(page, size);
        return ResponseEntity.ok(ApiResponse.<Page<NotificationDto>>builder()
                .success(true)
                .message("Notifications retrieved successfully")
                .data(notifications)
                .build());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Notification marked as read")
                .build());
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("All notifications marked as read")
                .build());
    }
}
