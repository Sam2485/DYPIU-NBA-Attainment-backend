package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "hod_setup_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hod_setup_department",
                        columnNames = {"department_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HodSetupProgress {

    @Id
    private String id;

    @Column(name = "department_id", nullable = false)
    private String departmentId;

    @Column(name = "hod_email")
    private String hodEmail;

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
}