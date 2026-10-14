package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "conversation_id", nullable = false, length = 36)
    private String conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 64)
    private String iv;

    @Column(name = "is_encrypted", nullable = false)
    @Builder.Default
    private Boolean isEncrypted = true;

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
