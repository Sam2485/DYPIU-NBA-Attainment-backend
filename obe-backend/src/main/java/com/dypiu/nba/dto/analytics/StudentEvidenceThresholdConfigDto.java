package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEvidenceThresholdConfigDto {

    private BigDecimal thresholdPercentage;
    private String updatedBy;
    private ZonedDateTime updatedAt;
}
