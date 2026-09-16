package com.dypiu.nba.emmu.dto;

/**
 * Strategy by which an entity was matched and resolved.
 */
public enum ResolutionMethod {
    EXACT_NAME,
    EXACT_CODE,
    NORMALIZED_NAME,
    ACRONYM_OR_ALIAS,
    FUZZY_MATCH,
    CONTEXTUAL,
    CONVERSATIONAL_REFERENCE,
    FALLBACK
}
