package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "cc_setup_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_cc_setup_batch_course",
                        columnNames = {"programme_batch_course_id"}
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

    @Column(name = "programme_batch_course_id", nullable = false)
    private String programmeBatchCourseId;

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
    private String masterCourseId;

    public String getMasterCourseId() {
        return masterCourseId != null ? masterCourseId : programmeBatchCourseId;
    }

    public void setMasterCourseId(String masterCourseId) {
        this.masterCourseId = masterCourseId;
    }

    public String getProgrammeBatchCourseId() {
        return programmeBatchCourseId;
    }

    public void setProgrammeBatchCourseId(String programmeBatchCourseId) {
        this.programmeBatchCourseId = programmeBatchCourseId;
    }
}
