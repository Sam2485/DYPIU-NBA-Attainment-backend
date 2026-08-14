package com.dypiu.nba.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeCoordinatorSummaryDto {
    private String programmeId;
    private String programmeCode;
    private String programmeName;
    private String departmentId;
    private String departmentName;
    private String coordinatorName;
    private String coordinatorEmail;
    private Integer durationYears;
    private Integer courseCount;
    private Integer activePOsCount;
    private Integer activePSOsCount;
    private Integer activePEOsCount;
    private Integer activeBatchesCount;
    private Integer pendingVerificationsCount;
    private ProgrammeCoordinatorSetupProgressDto setupProgress;
}
