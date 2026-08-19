package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "cc_setup_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cc_setup_offering",
                        columnNames = {"course_offering_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCoordinatorSetupProgress {

    @Id
    private String id;

    @Column(name = "course_offering_id", nullable = false)
    private String courseOfferingId;

    @Column(name = "coordinator_email")
    private String coordinatorEmail;

    @Column(name = "current_step", nullable = false)
    @Builder.Default
    private Integer currentStep = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false)
    @Builder.Default
    private SetupStepStatus overallStatus = SetupStepStatus.NOT_STARTED;

    @Column(name = "completed_steps", length = 500)
    private String completedSteps;

    @Column(name = "pending_steps", length = 500)
    private String pendingSteps;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Transient
    private String courseId;

    public String getCourseId() {
        return courseId != null ? courseId : courseOfferingId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
        if (this.courseOfferingId == null) {
            this.courseOfferingId = courseId;
        }
    }
}