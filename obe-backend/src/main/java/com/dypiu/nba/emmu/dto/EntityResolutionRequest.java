package com.dypiu.nba.emmu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for resolving natural-language academic references.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityResolutionRequest {
    private AcademicEntityType entityType;
    private String query;
    private EmmuConversationContext context;
}
