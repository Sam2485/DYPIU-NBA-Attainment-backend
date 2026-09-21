package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoIndirectEvidenceItemDto {
    private String coCode;
    private String coStatement;
    private BigDecimal coTargetLevel;
    private Boolean coTargetMet;

    // Authoritative Indirect Attainment Levels & Scores
    private Integer indirectAttainment;
    private BigDecimal indirectScore;
    private BigDecimal overallIndirectPercentage;

    // Response Counts
    private Integer level1Count;
    private Integer level2Count;
    private Integer level3Count;
    private Integer validResponseCount;
    private Integer totalResponses;

    // Percentages (HALF_UP, 2 decimals)
    private BigDecimal level1Percentage;
    private BigDecimal level2Percentage;
    private BigDecimal level3Percentage;

    // Structured Level Distribution
    private Map<String, Integer> levelDistribution;

    // Aggregate-Safe Individual Feedback / Rating Records
    private List<CoIndirectResponseRecordDto> responseRecords;
}
