package com.dypiu.nba.entity;

import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.deletion.DeletionRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;

@Entity
@Table(name = "deletion_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 100)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false, length = 255)
    private String resourceId;

    @Column(name = "programme_batch_id", length = 100)
    private String programmeBatchId;

    @Column(name = "programme_batch_course_id", length = 100)
    private String programmeBatchCourseId;

    @Column(name = "master_programme_id", length = 100)
    private String masterProgrammeId;

    @Column(name = "department_id", length = 100)
    private String departmentId;

    @Column(name = "school_id", length = 100)
    private String schoolId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private DeletionRequestStatus status = DeletionRequestStatus.PENDING;

    @Column(name = "requested_by", length = 255)
    private String requestedBy;

    @Column(name = "requested_by_id", length = 100)
    private String requestedById;

    @Column(name = "requested_by_role", length = 50)
    private String requestedByRole;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private ZonedDateTime requestedAt = ZonedDateTime.now();

    @Column(name = "reviewed_by", length = 255)
    private String reviewedBy;

    @Column(name = "reviewed_by_id", length = 100)
    private String reviewedById;

    @Column(name = "reviewed_by_role", length = 50)
    private String reviewedByRole;

    @Column(name = "reviewed_at")
    private ZonedDateTime reviewedAt;

    @Column(name = "executed_at")
    private ZonedDateTime executedAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
