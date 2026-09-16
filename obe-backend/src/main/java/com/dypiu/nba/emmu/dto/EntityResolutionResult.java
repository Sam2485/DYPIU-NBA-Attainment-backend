package com.dypiu.nba.emmu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Structured result returned by AcademicEntityResolver for consumption by AI/LLM orchestrators.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityResolutionResult {
    private AcademicEntityType entityType;
    private ResolutionStatus status;
    private String canonicalId;
    private String canonicalName;
    private String canonicalCode;
    private String matchedText;
    private ResolutionMethod resolutionMethod;
    private Double confidence;
    private boolean isAmbiguous;
    private boolean requiresClarification;
    private String clarificationPrompt;
    private int candidateCount;
    private List<EntityCandidateDto> candidates;
    private boolean contextUsed;
    private Map<String, Object> metadata;
}
