package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCoOverviewItemDto {

    private String coCode;
    private String statement;
    private BigDecimal target;
    private BigDecimal directPercentage;
    private Integer directLevel;
    private BigDecimal directAttainment;
    private BigDecimal indirectPercentage;
    private BigDecimal indirectScore;
    private Integer indirectLevel;
    private BigDecimal indirectAttainment;
    private BigDecimal overallAttainment;
    private Boolean targetMet;
    private String observation;
    private BigDecimal directWeight;
    private BigDecimal indirectWeight;
    private Map<String, Integer> poMappings;
    private Map<String, Integer> psoMappings;
    private Integer selectedOutcomeMapping;
}
