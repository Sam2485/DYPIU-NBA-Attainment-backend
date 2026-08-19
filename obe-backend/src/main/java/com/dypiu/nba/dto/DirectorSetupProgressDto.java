package com.dypiu.nba.dto;

import com.dypiu.nba.entity.DirectorSetupStep;
import com.dypiu.nba.entity.SetupStepStatus;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorSetupProgressDto {
    private Integer currentStep;
    private DirectorSetupStep currentStepEnum;
    private SetupStepStatus overallStatus;
    private List<String> completedSteps;
    private List<String> pendingSteps;
    private Map<DirectorSetupStep, SetupStepStatus> stepStatuses;
    private String schoolId;
}
