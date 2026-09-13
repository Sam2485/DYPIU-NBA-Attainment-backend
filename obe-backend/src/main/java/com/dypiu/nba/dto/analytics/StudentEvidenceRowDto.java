package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEvidenceRowDto {

    private String studentIdentifier;
    private String maskedPrn;
    private BigDecimal marksObtained;
    private BigDecimal maxMarks;
    private BigDecimal percentage;
    private boolean thresholdMet;
}
