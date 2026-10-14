package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Suggestion {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "sender_role", nullable = false, length = 50)
    private String senderRole;

    @Column(name = "sender_name", length = 150)
    private String senderName;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "recipient_role", nullable = false, length = 50)
    private String recipientRole;

    @Column(name = "recipient_name", length = 150)
    private String recipientName;

    @Column(name = "school_id", length = 50)
    private String schoolId;

    @Column(name = "department_id", length = 50)
    private String departmentId;

    @Column(name = "master_programme_id", length = 50)
    private String masterProgrammeId;

    @Column(name = "programme_name", length = 255)
    private String programmeName;

    @Column(name = "programme_batch_id", length = 50)
    private String programmeBatchId;

    @Column(name = "batch_name", length = 255)
    private String batchName;

    @Column(name = "programme_batch_course_id", length = 50)
    private String programmeBatchCourseId;

    @Column(name = "course_name", length = 255)
    private String courseName;

    @Column(name = "context_type", nullable = false, length = 50)
    private String contextType; // 'PROGRAMME_BATCH', 'COURSE', 'SCHOOL', 'DEPARTMENT'

    @Column(name = "category", length = 50)
    @Builder.Default
    private String category = "GENERAL";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "DELIVERED"; // 'DELIVERED', 'ACKNOWLEDGED', 'RESOLVED'

    @Column(name = "read_at")
    private ZonedDateTime readAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = ZonedDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now();
    }
}
