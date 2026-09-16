package com.dypiu.nba.emmu.dto;

/**
 * Outcome status of an academic entity resolution attempt.
 */
public enum ResolutionStatus {
    RESOLVED,
    AMBIGUOUS,
    NOT_FOUND,
    UNAUTHORIZED,
    INVALID_CONTEXT
}
