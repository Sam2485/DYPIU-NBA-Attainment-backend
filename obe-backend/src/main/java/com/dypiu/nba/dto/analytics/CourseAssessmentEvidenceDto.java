package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAssessmentEvidenceDto {

    private String courseOfferingId;
    private String masterCourseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private String courseCoordinatorName;
    private String coCode;
    private BigDecimal coOverallAttainment;
    private BigDecimal coDirectAttainment;
    private BigDecimal coIndirectAttainment;
    private BigDecimal coTarget;
    private Integer mappingStrength;
    private Boolean coTargetMet;
}
