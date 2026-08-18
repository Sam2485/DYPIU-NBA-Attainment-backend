package com.dypiu.nba.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyAttainmentResultDto {
    private String courseId;
    private Integer totalStudents;
    private Map<String, Integer> level1Counts; // Count of Slight (1)
    private Map<String, Integer> level2Counts; // Count of Moderate (2)
    private Map<String, Integer> level3Counts; // Count of Substantial (3)
    private Map<String, BigDecimal> level1Percentages; // % of Slight
    private Map<String, BigDecimal> level2Percentages; // % of Moderate
    private Map<String, BigDecimal> level3Percentages; // % of Substantial
    private Map<String, BigDecimal> overallIndirectPercentages; // (pct1 * 1/3) + (pct2 * 2/3) + (pct3 * 3/3)
    private Map<String, BigDecimal> indirectAttainmentScores; // Score out of 3.00
    private Map<String, Integer> coAttainmentLevels; // Dynamic indirect attainment levels (1, 2, 3)
    private BigDecimal overallIndirectCoAttainment; // Average score across all COs
    private List<SurveyResponseRowDto> surveyResponses;
    private Map<String, Object> fileDetails;
}
