package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "source_entity_type", nullable = false, length = 50)
    private String sourceEntityType;

    @Column(name = "source_entity_id", nullable = false, length = 100)
    private String sourceEntityId;

    @Column(name = "deep_link", length = 255)
    private String deepLink;

    @Column(name = "idempotency_key", unique = true, length = 190)
    private String idempotencyKey;

    @Column(name = "read_at")
    private ZonedDateTime readAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }
}
