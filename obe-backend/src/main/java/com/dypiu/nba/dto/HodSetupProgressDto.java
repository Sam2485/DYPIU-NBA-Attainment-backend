package com.dypiu.nba.dto;

import com.dypiu.nba.entity.SetupStepStatus;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HodSetupProgressDto {
    private String id;
    private String departmentId;
    private String hodEmail;
    private Integer currentStep;
    private SetupStepStatus overallStatus;
    private List<String> completedSteps;
    private List<String> pendingSteps;
    private ZonedDateTime updatedAt;
}
