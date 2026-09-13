package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScopedTrendSeriesDto {

    private String scopeLevel; // "INSTITUTION" | "SCHOOL" | "DEPARTMENT" | "PROGRAMME"
    private String scopeIdentifier;
    private String scopeName;
    private List<CohortOutcomeDataPointDto> cohortDataPoints;
}
