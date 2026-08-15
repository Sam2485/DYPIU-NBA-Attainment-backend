package com.dypiu.nba.dto;

import com.dypiu.nba.entity.Programme;
import lombok.*;
import java.util.List;

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
    private List<Programme> assignedProgrammes;
    private ProgrammeCoordinatorSetupProgressDto setupProgress;
}
