package com.dypiu.nba.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyResponseRowDto {
    private Integer srNo;
    private String prn;
    private String studentName;
    private Map<String, BigDecimal> coRatings; // Numeric rating 1-3
    private Map<String, String> coFeedbacks; // e.g. {"CO1": "Substantial", ...}
}
