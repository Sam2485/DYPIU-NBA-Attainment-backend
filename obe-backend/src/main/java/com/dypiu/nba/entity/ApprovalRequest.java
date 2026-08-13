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

    @Column(nullable = false, length = 50)
    private String type; // PO_PSO_FRAMEWORK, PROGRAMME_ATR, COURSE_CO_WEIGHTAGES, PROGRAMME_TARGETS, COURSE_ALLOCATION, COURSE_ATR

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "programme_id")
    private String programmeId;

    @Column(name = "course_id")
    private String courseId;

    @Column(name = "submitted_by", nullable = false)
    private String submittedBy;

    @Column(name = "submitted_at")
    private ZonedDateTime submittedAt;

    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, NEEDS_REVISION

    private String approvedBy;
    private ZonedDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
