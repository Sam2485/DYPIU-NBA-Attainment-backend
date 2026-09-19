package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoDirectEvidenceSummaryDto {

    private Integer totalStudents;
    private Integer evaluatedStudents;
    private BigDecimal threshold;
    private BigDecimal directPercentage;
    private Integer directLevel;
    private BigDecimal directAttainment;
    private Integer studentsMeetingThreshold;
}
