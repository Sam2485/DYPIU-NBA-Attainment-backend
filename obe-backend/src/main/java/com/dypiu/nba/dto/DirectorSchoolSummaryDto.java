package com.dypiu.nba.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorSchoolSummaryDto {
    private String schoolId;
    private String schoolName;
    private String schoolCode;
    private String directorName;
    private String directorEmail;
    private String estYear;
    private Integer totalDepartments;
    private Integer assignedHODsCount;
    private Integer unassignedHODsCount;
    private Integer totalProgrammes;
}
