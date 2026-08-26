package com.dypiu.nba.dto;

import com.dypiu.nba.entity.SetupStepStatus;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeCoordinatorSetupProgressDto {
    private String id;
    private String masterProgrammeId;
    private String programmeBatchId;
    private String coordinatorEmail;
    private Integer currentStep;
    private SetupStepStatus overallStatus;
    private List<String> completedSteps;
    private List<String> pendingSteps;
    private ZonedDateTime updatedAt;

    public String getMasterProgrammeId() {
        return masterProgrammeId;
    }

    public void setMasterProgrammeId(String masterProgrammeId) {
        this.masterProgrammeId = masterProgrammeId;
    }

    public String getProgrammeBatchId() {
        return programmeBatchId;
    }

    public void setProgrammeBatchId(String programmeBatchId) {
        this.programmeBatchId = programmeBatchId;
    }

    public static class ProgrammeCoordinatorSetupProgressDtoBuilder {
        public ProgrammeCoordinatorSetupProgressDtoBuilder masterProgrammeId(String masterProgrammeId) {
            this.masterProgrammeId = masterProgrammeId;
            return this;
        }

        public ProgrammeCoordinatorSetupProgressDtoBuilder programmeBatchId(String programmeBatchId) {
            this.programmeBatchId = programmeBatchId;
            return this;
        }
    }
}
