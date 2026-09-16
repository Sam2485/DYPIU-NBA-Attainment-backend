package com.dypiu.nba.emmu.router;

import com.dypiu.nba.emmu.dto.EmmuConversationContext;
import com.dypiu.nba.emmu.dto.EmmuIntent;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class EmmuIntentRouter {

    private static final Pattern OUTCOME_PATTERN = Pattern.compile(
            "\\b(PO|PSO|CO)\\s*([0-9]{1,2})\\b", Pattern.CASE_INSENSITIVE);

    private static final Set<String> MUTATION_KEYWORDS = Set.of(
            "approve", "reject", "delete", "remove", "submit", "modify", "update", "change",
            "update mark", "save mark", "change target", "revise", "mutate", "insert", "edit"
    );

    private static final Set<String> OBE_TERMS = Set.of(
            "attainment", "outcome", "target", "deficit", "gap", "po", "pso", "co",
            "atr", "action taken", "course", "batch", "programme", "program", "department",
            "school", "indirect", "direct", "survey", "nba", "obe", "rubric"
    );

    @Data
    @Builder
    public static class RoutedIntent {
        private EmmuIntent primaryIntent;
        private List<EmmuIntent> allIntents;
        private String outcomeCode;
        private String outcomeType; // PO, PSO, CO
        private boolean isMutationAttempt;
        private boolean requiresObeEvidence;
        private String entityQuery;
    }

    public RoutedIntent route(String message) {
        return route(message, null);
    }

    public RoutedIntent route(String message, EmmuConversationContext context) {
        if (message == null || message.isBlank()) {
            return RoutedIntent.builder()
                    .primaryIntent(EmmuIntent.GENERIC_CHAT)
                    .allIntents(List.of(EmmuIntent.GENERIC_CHAT))
                    .requiresObeEvidence(false)
                    .build();
        }

        String lower = message.toLowerCase().trim();

        // 1. Check for mutation attempts (Read-Only Guardrail)
        if (isMutationAttempt(lower)) {
            return RoutedIntent.builder()
                    .primaryIntent(EmmuIntent.MUTATION_ATTEMPT)
                    .allIntents(List.of(EmmuIntent.MUTATION_ATTEMPT))
                    .isMutationAttempt(true)
                    .requiresObeEvidence(false)
                    .build();
        }

        // 2. Extract Outcome Code (e.g. PO3, PSO1, CO4)
        String outcomeCode = null;
        String outcomeType = null;
        Matcher outcomeMatcher = OUTCOME_PATTERN.matcher(message);
        if (outcomeMatcher.find()) {
            outcomeType = outcomeMatcher.group(1).toUpperCase();
            outcomeCode = outcomeType + outcomeMatcher.group(2);
        } else if (context != null && context.getOutcomeCode() != null && !context.getOutcomeCode().isBlank()) {
            outcomeCode = context.getOutcomeCode().toUpperCase();
            outcomeType = outcomeCode.startsWith("PSO") ? "PSO" : outcomeCode.startsWith("CO") ? "CO" : "PO";
        }

        // 3. Check for Generic Chat (non-OBE queries)
        boolean hasObeTerms = OBE_TERMS.stream().anyMatch(lower::contains) || outcomeCode != null;
        if (!hasObeTerms) {
            return RoutedIntent.builder()
                    .primaryIntent(EmmuIntent.GENERIC_CHAT)
                    .allIntents(List.of(EmmuIntent.GENERIC_CHAT))
                    .requiresObeEvidence(false)
                    .build();
        }

        // 4. Classify OBE Intents
        List<EmmuIntent> detectedIntents = new ArrayList<>();

        if (lower.contains("why") || lower.contains("reason") || lower.contains("cause")
                || lower.contains("weak") || lower.contains("deficit") || lower.contains("gap")) {
            detectedIntents.add(EmmuIntent.ROOT_CAUSE);
        }

        if (lower.contains("action") || lower.contains("atr") || lower.contains("corrective")
                || lower.contains("remedial") || lower.contains("improvement plan")) {
            detectedIntents.add(EmmuIntent.ATR_ACTION);
        }

        if (lower.contains("trend") || lower.contains("over the years") || lower.contains("historical")
                || lower.contains("decline") || lower.contains("improved") || lower.contains("prior batch")) {
            detectedIntents.add(EmmuIntent.HISTORICAL_TREND);
        }

        if (lower.contains("rank") || lower.contains("top course") || lower.contains("weak course")
                || lower.contains("contributing course") || lower.contains("which course")
                || lower.contains("lowest") || lower.contains("highest") || lower.contains("best course")
                || lower.contains("worst course")) {
            detectedIntents.add(EmmuIntent.COURSE_RANKING);
        }

        if (lower.contains("student") || lower.contains("prn") || lower.contains("failed")
                || lower.contains("below threshold") || lower.contains("score")) {
            detectedIntents.add(EmmuIntent.STUDENT_EVIDENCE);
        }

        if (lower.contains("summary") || lower.contains("overview") || lower.contains("landscape")
                || lower.contains("status of")) {
            detectedIntents.add(EmmuIntent.CONTEXT_SUMMARY);
        }

        if (outcomeCode != null && detectedIntents.isEmpty()) {
            detectedIntents.add(EmmuIntent.OUTCOME_ATTAINMENT);
        }

        if (detectedIntents.isEmpty()) {
            if (lower.contains("how") || lower.contains("what is") || lower.contains("formula")
                    || lower.contains("criterion") || lower.contains("rubric")) {
                detectedIntents.add(EmmuIntent.GENERIC_OBE);
            } else {
                detectedIntents.add(EmmuIntent.CONTEXT_SUMMARY);
            }
        }

        EmmuIntent primary = detectedIntents.get(0);

        return RoutedIntent.builder()
                .primaryIntent(primary)
                .allIntents(detectedIntents)
                .outcomeCode(outcomeCode)
                .outcomeType(outcomeType)
                .isMutationAttempt(false)
                .requiresObeEvidence(primary != EmmuIntent.GENERIC_OBE && primary != EmmuIntent.GENERIC_CHAT)
                .build();
    }

    private boolean isMutationAttempt(String lower) {
        boolean hasMutationVerb = MUTATION_KEYWORDS.stream().anyMatch(kw ->
                lower.startsWith(kw) || lower.contains(" " + kw + " ") || lower.contains(" " + kw + " this"));
        return hasMutationVerb && (lower.contains("atr") || lower.contains("course")
                || lower.contains("report") || lower.contains("approval") || lower.contains("mark")
                || lower.contains("target") || lower.contains("batch") || lower.contains("record"));
    }
}
