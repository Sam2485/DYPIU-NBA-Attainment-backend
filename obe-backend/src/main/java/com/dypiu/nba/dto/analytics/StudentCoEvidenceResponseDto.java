package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCoEvidenceResponseDto {

    private String programmeBatchCourseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private String courseCoordinatorName;
    private String coCode;
    private String coStatement;
    private BigDecimal coTargetLevel;
    private BigDecimal coDirectAttainment;
    private BigDecimal coIndirectAttainment;
    private BigDecimal coOverallAttainment;
    private Boolean coTargetMet;

    // Configured Performance Evidence Benchmark
    private BigDecimal configuredThresholdPercentage;

    // Aggregated Student Evidence Metrics
    private int totalStudentsEvaluated;
    private int studentsMeetingThreshold;
    private int studentsBelowThreshold;
    private BigDecimal attainmentRatePercentage;
    private BigDecimal classAveragePercentage;
    private BigDecimal highestPercentage;
    private BigDecimal lowestPercentage;

    // Neutral Numerical Performance Distribution Ranges (e.g. "90-100%", "80-89%", etc.)
    private Map<String, Integer> scoreDistribution;

    // Privacy-Preserved Student-Level Records
    private List<StudentEvidenceRowDto> studentRecords;
}
