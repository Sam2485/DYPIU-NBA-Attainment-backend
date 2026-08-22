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

    public static class ProgrammeCoordinatorSetupProgressDtoBuilder {
        public ProgrammeCoordinatorSetupProgressDtoBuilder programmeId(String programmeId) {
            this.masterProgrammeId = programmeId;
            return this;
        }

        public ProgrammeCoordinatorSetupProgressDtoBuilder batchId(String batchId) {
            this.programmeBatchId = batchId;
            return this;
        }
    }
}
