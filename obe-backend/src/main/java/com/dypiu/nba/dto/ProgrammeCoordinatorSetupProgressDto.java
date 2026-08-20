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
    private String programmeId;
    private String batchId;
    private String coordinatorEmail;
    private Integer currentStep;
    private SetupStepStatus overallStatus;
    private List<String> completedSteps;
    private List<String> pendingSteps;
    private ZonedDateTime updatedAt;
}
