package com.dypiu.nba.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorSchoolSummaryDto {
    private String schoolId;
    private String schoolName;
    private String schoolCode;
    private String deanName;
    private String estYear;
    private Integer totalDepartments;
    private Integer assignedHODsCount;
    private Integer unassignedHODsCount;
    private Integer totalProgrammes;

}
