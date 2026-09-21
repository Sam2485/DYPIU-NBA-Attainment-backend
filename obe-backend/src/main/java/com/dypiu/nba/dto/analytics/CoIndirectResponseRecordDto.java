package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoIndirectResponseRecordDto {
    private Integer responseNumber;
    private String responseIdentifier;
    private String maskedPrn;
    private BigDecimal rating;
    private Integer ratingLevel;
    private String feedback;
}
