package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "pc_setup_progress",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pc_setup_batch",
                        columnNames = {"programme_batch_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeCoordinatorSetupProgress {

    @Id
    private String id;

    @Column(name = "programme_batch_id", nullable = false)
    private String programmeBatchId;

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
    private String masterProgrammeId;

    public String getProgrammeBatchId() {
        return programmeBatchId;
    }

    public void setProgrammeBatchId(String programmeBatchId) {
        this.programmeBatchId = programmeBatchId;
    }

    public String getMasterProgrammeId() {
        return masterProgrammeId != null ? masterProgrammeId : programmeBatchId;
    }

    public void setMasterProgrammeId(String masterProgrammeId) {
        if (this.programmeBatchId == null) {
            this.programmeBatchId = masterProgrammeId;
        }
    }
}
