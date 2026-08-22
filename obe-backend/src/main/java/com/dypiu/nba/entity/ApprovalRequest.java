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

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "master_programme_id")
    private String masterProgrammeId;

    @Column(name = "programme_batch_id")
    private String programmeBatchId;

    @Column(name = "master_course_id")
    private String masterCourseId;

    @Column(name = "programme_batch_course_id")
    private String programmeBatchCourseId;

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

    // Helper compatibility methods
    public String getProgrammeId() {
        return masterProgrammeId;
    }

    public void setProgrammeId(String programmeId) {
        this.masterProgrammeId = programmeId;
    }

    public String getBatchId() {
        return programmeBatchId;
    }

    public void setBatchId(String batchId) {
        this.programmeBatchId = batchId;
    }

    public String getCourseId() {
        return masterCourseId;
    }

    public void setCourseId(String courseId) {
        this.masterCourseId = courseId;
    }

    public String getCourseOfferingId() {
        return programmeBatchCourseId;
    }

    public void setCourseOfferingId(String offeringId) {
        this.programmeBatchCourseId = offeringId;
    }
}
