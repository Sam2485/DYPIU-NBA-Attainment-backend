package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutcomeDirectCourseDto {

    private String programmeBatchCourseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private String courseCoordinator;
    private BigDecimal overallCourseAttainment;
    private BigDecimal mappingStrength;
    private BigDecimal contribution;
}
