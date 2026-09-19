package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectedOutcomeContributionDto {

    private String outcomeCode;
    private String outcomeType;
    private String outcomeStatement;
    private BigDecimal mappingStrength;
    private BigDecimal overallCourseAttainment;
    private BigDecimal contribution;
    private BigDecimal target;
    private Boolean targetMet;
    private Boolean mapped;
}
