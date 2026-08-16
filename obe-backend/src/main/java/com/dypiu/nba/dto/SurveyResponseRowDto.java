package com.dypiu.nba.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyResponseRowDto {
    private Integer srNo;
    private String studentName;
    private Map<String, String> coFeedbacks; // e.g. {"CO1": "Substantial", "CO2": "Moderate", "CO3": "Slight", ...}
}
