package com.dypiu.nba.dto;

import com.dypiu.nba.entity.MasterProgramme;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeCoordinatorSummaryDto {
    private String coordinatorEmail;
    private String coordinatorName;
    private String masterProgrammeId;

    private String programmeName;
    private String programmeCode;
    private String departmentId;
    private String departmentName;
    private Integer durationYears;
    private Integer courseCount;
    private Integer totalCoursesCount;
    private Integer activePOsCount;
    private Integer activePSOsCount;
    private Integer activePEOsCount;
    private Integer activeBatchesCount;
    private Integer pendingVerificationsCount;
    private List<MasterProgramme> assignedProgrammes;
    private ProgrammeCoordinatorSetupProgressDto setupProgress;
}
