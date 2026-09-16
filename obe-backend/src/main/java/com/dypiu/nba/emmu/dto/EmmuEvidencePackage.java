package com.dypiu.nba.emmu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Structured, authoritative evidence package passed to LLM or returned to client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmmuEvidencePackage {
    private String currency; // "LIVE" or "FINALIZED"
    private EmmuIntent primaryIntent;
    private List<EmmuIntent> intents;
    private Map<String, Object> scope;
    private Map<String, Object> resolvedEntities;
    private Map<String, Object> evidence;
    private boolean hasEvidence;
    private boolean isReadOnly;
    private boolean requiresClarification;
    private String clarificationPrompt;
    private List<String> clarificationOptions;
}
