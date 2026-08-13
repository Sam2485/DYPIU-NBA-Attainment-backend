package com.dypiu.nba.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentSummaryDto {
    private String deptId;
    private String deptCode;
    private String deptName;
    private String deptHodName;
    private String deptHodEmail;
    private Boolean hodAssignedStatus;
}
