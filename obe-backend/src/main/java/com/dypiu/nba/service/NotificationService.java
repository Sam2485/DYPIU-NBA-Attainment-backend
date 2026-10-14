package com.dypiu.nba.service;

import com.dypiu.nba.dto.NotificationDto;
import com.dypiu.nba.entity.Notification;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.repository.NotificationRepository;
import com.dypiu.nba.repository.UserRepository;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserScopeService currentUserScopeService;

    private User getAuthenticatedUser() {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (scope == null || scope.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userRepository.findById(scope.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /**
     * Create and persist a workflow notification with idempotency check.
     */
    @Transactional
    public void createNotification(
            Long recipientId,
            String title,
            String message,
            String eventType,
            String sourceEntityType,
            String sourceEntityId,
            String deepLink,
            String idempotencyKey) {

        if (recipientId == null) {
            log.warn("[NotificationService] Cannot create notification: recipientId is null");
            return;
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Notification> existing = notificationRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                log.debug("[NotificationService] Duplicate event skipped: {}", idempotencyKey);
                return;
            }
        }

        Notification n = Notification.builder()
                .recipientId(recipientId)
                .title(title)
                .message(message)
                .eventType(eventType)
                .sourceEntityType(sourceEntityType)
                .sourceEntityId(sourceEntityId)
                .deepLink(deepLink)
                .idempotencyKey(idempotencyKey)
                .build();

        notificationRepository.save(n);
        log.info("[NotificationService] Notification created for user {}: {}", recipientId, title);
    }

    /**
     * Get paginated notifications for the authenticated user.
     */
    @Transactional(readOnly = true)
    public Page<NotificationDto> getNotifications(int page, int size) {
        User currentUser = getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                .map(this::toDto);
    }

    /**
     * Mark a single notification as read.
     */
    @Transactional
    public void markAsRead(String id) {
        User currentUser = getAuthenticatedUser();
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!n.getRecipientId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to mark this notification");
        }

        if (n.getReadAt() == null) {
            n.setReadAt(ZonedDateTime.now());
            notificationRepository.save(n);
        }
    }

    /**
     * Mark all notifications as read for current user.
     */
    @Transactional
    public void markAllAsRead() {
        User currentUser = getAuthenticatedUser();
        notificationRepository.markAllAsReadForUser(currentUser.getId(), ZonedDateTime.now());
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .recipientId(n.getRecipientId())
                .title(n.getTitle())
                .message(n.getMessage())
                .eventType(n.getEventType())
                .sourceEntityType(n.getSourceEntityType())
                .sourceEntityId(n.getSourceEntityId())
                .deepLink(n.getDeepLink())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
