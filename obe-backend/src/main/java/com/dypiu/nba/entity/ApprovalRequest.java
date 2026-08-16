package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;
@Entity
@Table(name = "approval_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequest {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ApprovalType type;

    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Entity being approved.
     *
     * Examples:
     * PROGRAMME -> programmeId
     * BATCH -> batchId
     * COURSE_OFFERING -> courseOfferingId
     * PROGRAMME_ATR -> programmeAtrId
     * COURSE_ATR -> courseAtrId
     */
    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "programme_id")
    private String programmeId;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "course_id")
    private String courseId;

    @Column(name = "course_offering_id")
    private String courseOfferingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(name = "submitted_by", nullable = false)
    private String submittedBy;

    @Column(name = "submitted_at")
    private ZonedDateTime submittedAt;

    private String approvedBy;

    private ZonedDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}