package com.dypiu.nba.dto.analytics;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStudentEvidenceThresholdRequest {

    @NotNull(message = "Threshold percentage is required")
    @DecimalMin(value = "0.00", message = "Threshold must be between 0.00% and 100.00%")
    @DecimalMax(value = "100.00", message = "Threshold must be between 0.00% and 100.00%")
    private BigDecimal thresholdPercentage;
}
