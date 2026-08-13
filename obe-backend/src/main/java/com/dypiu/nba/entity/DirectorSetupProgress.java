package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "director_setup_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectorSetupProgress {

    @Id
    private String id;

    @Column(name = "school_id", nullable = false, unique = true)
    private String schoolId;

    @Column(name = "current_step", nullable = false)
    private Integer currentStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step_enum", nullable = false)
    private DirectorSetupStep currentStepEnum;

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
