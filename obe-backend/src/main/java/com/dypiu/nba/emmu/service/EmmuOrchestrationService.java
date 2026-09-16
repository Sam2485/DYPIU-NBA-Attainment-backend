package com.dypiu.nba.emmu.service;

import com.dypiu.nba.emmu.ai.EmmuAiClient;
import com.dypiu.nba.emmu.ai.EmmuSystemPrompt;
import com.dypiu.nba.emmu.dto.*;
import com.dypiu.nba.emmu.resolver.AcademicEntityResolver;
import com.dypiu.nba.emmu.router.EmmuIntentRouter;
import com.dypiu.nba.entity.ProgrammeBatch;
import com.dypiu.nba.repository.ProgrammeBatchRepository;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Authoritative Emmu Orchestration Pipeline.
 * Enforces:
 * 1. Read-Only Invariant: strictly observational, no database mutations.
 * 2. Scope & RBAC verification before evidence retrieval.
 * 3. Phase 11C Academic Entity Resolution with clarification on ambiguity.
 * 4. Multi-tool evidence combination from Phase 11B EmmuEvidenceService.
 * 5. Data currency tagging (LIVE vs FINALIZED).
 * 6. Streaming through Ollama with authoritative evidence injection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmmuOrchestrationService {

    private final EmmuIntentRouter intentRouter;
    private final AcademicEntityResolver academicEntityResolver;
    private final EmmuEvidenceService emmuEvidenceService;
    private final CurrentUserScopeService currentUserScopeService;
    private final ProgrammeBatchRepository programmeBatchRepository;
    private final EmmuAiClient aiClient;

    /**
     * Executes the streaming pipeline for an incoming chat query.
     */
    public void streamChat(EmmuChatRequest request,
                           Principal principal,
                           Consumer<EmmuEvidencePackage> onMetadata,
                           Consumer<String> onToken,
                           Runnable onComplete,
                           Consumer<Throwable> onError,
                           AtomicBoolean cancelFlag) {

        try {
            String message = request != null ? request.getMessage() : "";
            List<ChatMessageDto> history = request != null && request.getHistory() != null
                    ? request.getHistory()
                    : Collections.emptyList();

            EmmuConversationContext context = parseContext(request != null ? request.getContext() : null);

            // =================================================================
            // 1. INTENT ROUTING & READ-ONLY GUARDRAIL
            // =================================================================
            EmmuIntentRouter.RoutedIntent routed = intentRouter.route(message, context);
            log.info("[EmmuOrchestration] Routed query: intent={}, outcomeCode={}, isMutation={}",
                    routed.getPrimaryIntent(), routed.getOutcomeCode(), routed.isMutationAttempt());

            // Check Read-Only Guardrail
            if (routed.isMutationAttempt()) {
                String readOnlyMsg = "Emmu is an observational intelligence assistant with strictly read-only access. "
                        + "To approve ATRs, edit courses, or submit marks, please use the official action buttons directly in the OBE workflow screens.";

                EmmuEvidencePackage pkg = EmmuEvidencePackage.builder()
                        .currency("FINALIZED")
                        .primaryIntent(EmmuIntent.MUTATION_ATTEMPT)
                        .isReadOnly(true)
                        .hasEvidence(false)
                        .build();

                onMetadata.accept(pkg);
                onToken.accept(readOnlyMsg);
                onComplete.run();
                return;
            }

            // Check Generic Chat (no OBE database evidence required)
            if (routed.getPrimaryIntent() == EmmuIntent.GENERIC_CHAT) {
                EmmuEvidencePackage pkg = EmmuEvidencePackage.builder()
                        .currency("LIVE")
                        .primaryIntent(EmmuIntent.GENERIC_CHAT)
                        .isReadOnly(true)
                        .hasEvidence(false)
                        .build();

                onMetadata.accept(pkg);
                aiClient.stream(EmmuSystemPrompt.BASE_SYSTEM_PROMPT, message, history,
                        onToken, onComplete, onError, cancelFlag);
                return;
            }

            // =================================================================
            // 2. ACADEMIC ENTITY RESOLUTION (PHASE 11C)
            // =================================================================
            String effectiveBatchId = context != null ? context.getProgrammeBatchId() : null;
            String effectiveSchoolId = context != null ? context.getSchoolId() : null;
            String effectiveDeptId = context != null ? context.getDepartmentId() : null;
            String effectiveProgId = context != null ? context.getMasterProgrammeId() : null;
            String effectiveCourseId = context != null ? context.getProgrammeBatchCourseId() : null;

            // Check if entity is mentioned in query that needs resolution
            EntityResolutionResult entityResult = null;
            if (containsEntityKeywords(message)) {
                EntityResolutionRequest resReq = EntityResolutionRequest.builder()
                        .query(message)
                        .context(context)
                        .build();
                entityResult = academicEntityResolver.resolve(resReq, principal);

                // Handle Ambiguity / Clarification
                if (entityResult != null && (entityResult.isAmbiguous() || entityResult.isRequiresClarification())) {
                    List<String> options = new ArrayList<>();
                    if (entityResult.getCandidates() != null) {
                        for (EntityCandidateDto cand : entityResult.getCandidates()) {
                            String label = cand.getName() != null ? cand.getName() : cand.getCode();
                            if (cand.getCode() != null && !cand.getCode().equals(label)) {
                                label += " (" + cand.getCode() + ")";
                            }
                            options.add(label != null ? label : cand.getCanonicalId());
                        }
                    }

                    String clarificationMsg = entityResult.getClarificationPrompt() != null
                            ? entityResult.getClarificationPrompt()
                            : "I found multiple matching academic entities for your query. Which one do you mean?";

                    EmmuEvidencePackage pkg = EmmuEvidencePackage.builder()
                            .currency("LIVE")
                            .primaryIntent(routed.getPrimaryIntent())
                            .requiresClarification(true)
                            .clarificationPrompt(clarificationMsg)
                            .clarificationOptions(options)
                            .hasEvidence(false)
                            .build();

                    onMetadata.accept(pkg);
                    onToken.accept(clarificationMsg);
                    onComplete.run();
                    return;
                }

                if (entityResult != null && entityResult.getStatus() == ResolutionStatus.RESOLVED) {
                    if (entityResult.getEntityType() == AcademicEntityType.PROGRAMME_BATCH) {
                        effectiveBatchId = entityResult.getCanonicalId();
                    } else if (entityResult.getEntityType() == AcademicEntityType.COURSE_OFFERING) {
                        effectiveCourseId = entityResult.getCanonicalId();
                    } else if (entityResult.getEntityType() == AcademicEntityType.DEPARTMENT) {
                        effectiveDeptId = entityResult.getCanonicalId();
                    } else if (entityResult.getEntityType() == AcademicEntityType.PROGRAMME) {
                        effectiveProgId = entityResult.getCanonicalId();
                    } else if (entityResult.getEntityType() == AcademicEntityType.SCHOOL) {
                        effectiveSchoolId = entityResult.getCanonicalId();
                    }
                }
            }

            // Fallback: If no batch ID is specified, resolve the active batch for the user's scope
            if (effectiveBatchId == null) {
                effectiveBatchId = resolveDefaultBatchForUser(principal, effectiveProgId, effectiveDeptId);
            }

            // =================================================================
            // 3. AUTHORIZATION & SCOPE VALIDATION (RBAC)
            // =================================================================
            CurrentUserScope userScope = currentUserScopeService.getCurrentUserScope(principal);
            validateScopeAuthorization(userScope, effectiveSchoolId, effectiveDeptId, effectiveProgId, effectiveBatchId);

            // =================================================================
            // 4. AUTHORIZED EVIDENCE RETRIEVAL (PHASE 11B)
            // =================================================================
            Map<String, Object> evidenceData = new LinkedHashMap<>();
            String currency = "LIVE";

            String outcomeCode = routed.getOutcomeCode();
            String outcomeType = routed.getOutcomeType() != null ? routed.getOutcomeType() : "PO";

            // Multi-Tool execution according to routed intents
            for (EmmuIntent intent : routed.getAllIntents()) {
                switch (intent) {
                    case ROOT_CAUSE, OUTCOME_ATTAINMENT -> {
                        if (outcomeCode != null && effectiveBatchId != null) {
                            try {
                                EmmuRootCauseDrilldownDto drilldown = emmuEvidenceService.getRootCauseDrilldown(
                                        effectiveBatchId, outcomeCode, outcomeType, principal);
                                evidenceData.put("rootCauseDrilldown_" + outcomeCode, drilldown);
                                if (drilldown != null && drilldown.getDataCurrency() != null) {
                                    currency = drilldown.getDataCurrency().name().contains("FINALIZED")
                                            ? "FINALIZED" : "LIVE";
                                }
                            } catch (Exception ex) {
                                log.debug("[EmmuOrchestration] Root cause drilldown non-fatal: {}", ex.getMessage());
                            }
                        }
                    }
                    case COURSE_RANKING -> {
                        if (effectiveBatchId != null) {
                            try {
                                EmmuCourseRankingDto rankings = emmuEvidenceService.getCourseRankings(
                                        effectiveBatchId, null, "attainment", "ASC", principal);
                                evidenceData.put("courseRankings", rankings);
                            } catch (Exception ex) {
                                log.debug("[EmmuOrchestration] Course rankings non-fatal: {}", ex.getMessage());
                            }
                        }
                    }
                    case COURSE_INTELLIGENCE -> {
                        if (effectiveCourseId != null) {
                            try {
                                EmmuCourseIntelligenceDto intelligence = emmuEvidenceService.getCourseIntelligence(
                                        effectiveCourseId, null, principal);
                                evidenceData.put("courseIntelligence", intelligence);
                            } catch (Exception ex) {
                                log.debug("[EmmuOrchestration] Course intelligence non-fatal: {}", ex.getMessage());
                            }
                        }
                    }
                    case CONTEXT_SUMMARY -> {
                        try {
                            EmmuContextSummaryDto summary = emmuEvidenceService.getContextSummary(
                                    effectiveSchoolId, effectiveDeptId, effectiveProgId, effectiveBatchId, principal);
                            evidenceData.put("contextSummary", summary);
                            if (summary != null && summary.getDataCurrency() != null) {
                                currency = summary.getDataCurrency().name().contains("FINALIZED")
                                        ? "FINALIZED" : "LIVE";
                            }
                        } catch (Exception ex) {
                            log.debug("[EmmuOrchestration] Context summary non-fatal: {}", ex.getMessage());
                        }
                    }
                    default -> {}
                }
            }

            // Always add context summary if evidenceData is empty but a batch exists
            if (evidenceData.isEmpty() && effectiveBatchId != null) {
                try {
                    EmmuContextSummaryDto summary = emmuEvidenceService.getContextSummary(
                            effectiveSchoolId, effectiveDeptId, effectiveProgId, effectiveBatchId, principal);
                    evidenceData.put("contextSummary", summary);
                } catch (Exception ignored) {
                }
            }

            // =================================================================
            // 5. STRUCTURED EVIDENCE PACKAGE CONSTRUCTION
            // =================================================================
            Map<String, Object> scopeMeta = new HashMap<>();
            if (effectiveSchoolId != null) scopeMeta.put("schoolId", effectiveSchoolId);
            if (effectiveDeptId != null) scopeMeta.put("departmentId", effectiveDeptId);
            if (effectiveProgId != null) scopeMeta.put("programmeId", effectiveProgId);
            if (effectiveBatchId != null) scopeMeta.put("batchId", effectiveBatchId);

            Map<String, Object> resolvedEntities = new HashMap<>();
            if (outcomeCode != null) resolvedEntities.put("outcome", outcomeCode);

            EmmuEvidencePackage evidencePackage = EmmuEvidencePackage.builder()
                    .currency(currency)
                    .primaryIntent(routed.getPrimaryIntent())
                    .intents(routed.getAllIntents())
                    .scope(scopeMeta)
                    .resolvedEntities(resolvedEntities)
                    .evidence(evidenceData)
                    .hasEvidence(!evidenceData.isEmpty())
                    .isReadOnly(true)
                    .build();

            // Send metadata packet to client
            onMetadata.accept(evidencePackage);

            // =================================================================
            // 6. SPRING AI / OLLAMA STREAMING INFERENCE
            // =================================================================
            String enrichedSystemPrompt = EmmuSystemPrompt.buildPromptWithEvidence(evidencePackage);
            aiClient.stream(enrichedSystemPrompt, message, history, onToken, onComplete, onError, cancelFlag);

        } catch (org.springframework.security.access.AccessDeniedException ade) {
            log.warn("[EmmuOrchestration] Access denied: {}", ade.getMessage());
            onToken.accept("🔒 Access Denied: You do not have authorization to view OBE evidence for this academic entity.");
            onComplete.run();
        } catch (Exception e) {
            log.error("[EmmuOrchestration] Unexpected pipeline error: {}", e.getMessage(), e);
            onError.accept(e);
        }
    }

    private EmmuConversationContext parseContext(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return null;
        return EmmuConversationContext.builder()
                .schoolId(asString(map.get("schoolId")))
                .departmentId(asString(map.get("departmentId")))
                .masterProgrammeId(asString(map.get("programmeId")))
                .programmeBatchId(asString(map.get("batchId")))
                .programmeBatchCourseId(asString(map.get("courseId")))
                .outcomeCode(asString(map.get("outcomeCode")))
                .build();
    }

    private String asString(Object obj) {
        return obj != null ? String.valueOf(obj) : null;
    }

    private boolean containsEntityKeywords(String msg) {
        String lower = msg.toLowerCase();
        return lower.contains("cse") || lower.contains("computer") || lower.contains("department")
                || lower.contains("school") || lower.contains("batch") || lower.contains("b.tech")
                || lower.contains("m.tech") || lower.contains("course") || lower.contains("engineering");
    }

    private void validateScopeAuthorization(CurrentUserScope userScope,
                                           String schoolId,
                                           String departmentId,
                                           String programmeId,
                                           String batchId) {
        if (userScope == null) return;
        if (userScope.isIqac()) return;

        if (userScope.hasSchoolScope() && schoolId != null && !schoolId.equals(userScope.getSchoolId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized school access: " + schoolId);
        }
        if (userScope.hasDepartmentScope() && departmentId != null && !departmentId.equals(userScope.getDepartmentId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized department access: " + departmentId);
        }
        if (userScope.hasProgrammeScope() && programmeId != null && !programmeId.equals(userScope.getMasterProgrammeId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized programme access: " + programmeId);
        }
    }

    private String resolveDefaultBatchForUser(Principal principal, String programmeId, String departmentId) {
        try {
            CurrentUserScope scope = currentUserScopeService.getCurrentUserScope(principal);
            if (programmeId != null) {
                List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(programmeId);
                if (!batches.isEmpty()) {
                    return batches.get(0).getId();
                }
            }
            if (scope != null && scope.getMasterProgrammeId() != null) {
                List<ProgrammeBatch> batches = programmeBatchRepository.findByMasterProgrammeIdAndDeletedAtIsNull(scope.getMasterProgrammeId());
                if (!batches.isEmpty()) {
                    return batches.get(0).getId();
                }
            }
            List<ProgrammeBatch> allBatches = programmeBatchRepository.findAll();
            return allBatches.isEmpty() ? null : allBatches.get(0).getId();
        } catch (Exception e) {
            return null;
        }
    }
}
