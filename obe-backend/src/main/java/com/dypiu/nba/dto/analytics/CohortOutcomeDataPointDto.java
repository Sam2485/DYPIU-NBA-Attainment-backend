package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortOutcomeDataPointDto {

    private String programmeBatchId;
    private String batchName;
    private Integer startYear;
    private Integer endYear;
    private String outcomeCode;
    private BigDecimal configuredTarget;
    private BigDecimal overallAttainment;
    private BigDecimal directAttainment;
    private BigDecimal indirectAttainment;
    private BigDecimal gap;
    private Boolean targetMet;
}
