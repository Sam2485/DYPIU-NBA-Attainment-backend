package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "cc_setup_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCoordinatorSetupProgress {

    @Id
    private String id;

    @Column(name = "course_id", nullable = false, unique = true)
    private String courseId;

    @Column(name = "coordinator_email")
    private String coordinatorEmail;

    @Column(name = "current_step", nullable = false)
    private Integer currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false)
    private SetupStepStatus overallStatus;

    @Column(name = "completed_steps", length = 500)
    private String completedSteps;

    @Column(name = "pending_steps", length = 500)
    private String pendingSteps;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
