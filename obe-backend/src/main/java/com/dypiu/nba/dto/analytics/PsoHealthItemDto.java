package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PsoHealthItemDto {

    private String psoCode;
    private String psoStatement;
    private int applicableCohortCount;
    private int evaluatedInstanceCount;
    private int targetMetCount;
    private int targetDeficitCount;
    private BigDecimal achievementRatePercentage;
    private BigDecimal averageAttainment;
    private BigDecimal averageTarget;
    private BigDecimal averageGap;
    private BigDecimal directAttainmentAverage;
    private BigDecimal indirectAttainmentAverage;
    private BigDecimal meanDivergence;
}
