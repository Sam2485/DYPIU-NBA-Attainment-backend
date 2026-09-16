package com.dypiu.nba.emmu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a plausible authorized candidate entity during resolution or clarification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityCandidateDto {
    private String canonicalId;
    private String name;
    private String code;
    private String additionalInfo;
    private Double confidenceScore;
    private ResolutionMethod matchType;
}
