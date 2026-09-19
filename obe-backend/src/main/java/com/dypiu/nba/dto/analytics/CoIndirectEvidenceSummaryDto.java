package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoIndirectEvidenceSummaryDto {

    private Integer responseCount;
    private Map<String, Integer> levelDistribution;
    private BigDecimal indirectScore;
    private Integer indirectLevel;
    private BigDecimal indirectPercentage;
}
