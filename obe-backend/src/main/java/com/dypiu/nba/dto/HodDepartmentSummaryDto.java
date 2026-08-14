package com.dypiu.nba.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HodDepartmentSummaryDto {
    private String deptId;
    private String deptCode;
    private String deptName;
    private String hodName;
    private String hodEmail;
    private String schoolId;
    private String schoolName;
    private Integer programmeCount;
    private Integer assignedCoordinatorsCount;
    private Integer courseCount;
    private HodSetupProgressDto setupProgress;
}
