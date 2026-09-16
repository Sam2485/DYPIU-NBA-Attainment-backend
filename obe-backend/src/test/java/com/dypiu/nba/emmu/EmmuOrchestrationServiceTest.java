package com.dypiu.nba.emmu;

import com.dypiu.nba.emmu.ai.EmmuAiClient;
import com.dypiu.nba.emmu.dto.*;
import com.dypiu.nba.emmu.resolver.AcademicEntityResolver;
import com.dypiu.nba.emmu.router.EmmuIntentRouter;
import com.dypiu.nba.emmu.service.EmmuEvidenceService;
import com.dypiu.nba.emmu.service.EmmuOrchestrationService;
import com.dypiu.nba.repository.ProgrammeBatchRepository;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmmuOrchestrationServiceTest {

    @Mock
    private EmmuAiClient aiClient;
    @Mock
    private EmmuEvidenceService evidenceService;
    @Mock
    private AcademicEntityResolver entityResolver;
    @Mock
    private CurrentUserScopeService scopeService;
    @Mock
    private ProgrammeBatchRepository batchRepository;

    private EmmuIntentRouter intentRouter;
    private EmmuOrchestrationService orchestrationService;

    @BeforeEach
    void setUp() {
        intentRouter = new EmmuIntentRouter();
        orchestrationService = new EmmuOrchestrationService(
                intentRouter,
                entityResolver,
                evidenceService,
                scopeService,
                batchRepository,
                aiClient
        );
    }

    @Test
    @DisplayName("Mutation request should be blocked immediately with read-only invariant message")
    void testMutationAttemptBlocked() {
        Principal principal = () -> "iqac_user";
        AtomicReference<EmmuEvidencePackage> metadataRef = new AtomicReference<>();
        StringBuilder tokenBuffer = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);

        EmmuChatRequest request = EmmuChatRequest.builder()
                .message("Please approve the ATR report for batch 2024")
                .history(List.of())
                .context(Collections.emptyMap())
                .build();

        orchestrationService.streamChat(
                request,
                principal,
                metadataRef::set,
                tokenBuffer::append,
                () -> completed.set(true),
                err -> {},
                new AtomicBoolean(false)
        );

        assertThat(completed.get()).isTrue();
        assertThat(metadataRef.get()).isNotNull();
        assertThat(metadataRef.get().getPrimaryIntent()).isEqualTo(EmmuIntent.MUTATION_ATTEMPT);
        assertThat(metadataRef.get().isHasEvidence()).isFalse();
        assertThat(tokenBuffer.toString()).contains("read-only access");
        // Verify AI client and evidence service were NEVER called
        verifyNoInteractions(aiClient);
        verifyNoInteractions(evidenceService);
    }

    @Test
    @DisplayName("Generic greeting should bypass OBE evidence lookup and stream from AI client")
    void testGenericChatBypassesEvidence() {
        Principal principal = () -> "iqac_user";
        AtomicReference<EmmuEvidencePackage> metadataRef = new AtomicReference<>();
        StringBuilder tokenBuffer = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);

        doAnswer(invocation -> {
            java.util.function.Consumer<String> consumer = invocation.getArgument(3);
            Runnable onDone = invocation.getArgument(4);
            consumer.accept("Hello! ");
            consumer.accept("I am Emmu, your OBE assistant.");
            onDone.run();
            return null;
        }).when(aiClient).stream(anyString(), anyString(), anyList(), any(), any(), any(), any());

        EmmuChatRequest request = EmmuChatRequest.builder()
                .message("Hello, who are you?")
                .history(List.of())
                .context(Collections.emptyMap())
                .build();

        orchestrationService.streamChat(
                request,
                principal,
                metadataRef::set,
                tokenBuffer::append,
                () -> completed.set(true),
                err -> {},
                new AtomicBoolean(false)
        );

        assertThat(completed.get()).isTrue();
        assertThat(metadataRef.get()).isNotNull();
        assertThat(metadataRef.get().getPrimaryIntent()).isEqualTo(EmmuIntent.GENERIC_CHAT);
        assertThat(metadataRef.get().isHasEvidence()).isFalse();
        assertThat(tokenBuffer.toString()).isEqualTo("Hello! I am Emmu, your OBE assistant.");
        verifyNoInteractions(evidenceService);
    }

    @Test
    @DisplayName("Ambiguous academic entity should trigger clarification prompt and candidate options")
    void testAmbiguousEntityClarification() {
        Principal principal = () -> "iqac_user";
        AtomicReference<EmmuEvidencePackage> metadataRef = new AtomicReference<>();
        StringBuilder tokenBuffer = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);

        EntityResolutionResult ambiguousResult = EntityResolutionResult.builder()
                .status(ResolutionStatus.AMBIGUOUS)
                .isAmbiguous(true)
                .requiresClarification(true)
                .clarificationPrompt("Did you mean CS101 or CS102?")
                .candidates(List.of(
                        EntityCandidateDto.builder().name("Data Structures").code("CS101").build(),
                        EntityCandidateDto.builder().name("Algorithms").code("CS102").build()
                ))
                .build();

        when(entityResolver.resolve(any(), eq(principal))).thenReturn(ambiguousResult);

        EmmuChatRequest request = EmmuChatRequest.builder()
                .message("Tell me about course Data Structures")
                .history(List.of())
                .context(Collections.emptyMap())
                .build();

        orchestrationService.streamChat(
                request,
                principal,
                metadataRef::set,
                tokenBuffer::append,
                () -> completed.set(true),
                err -> {},
                new AtomicBoolean(false)
        );

        assertThat(completed.get()).isTrue();
        assertThat(metadataRef.get()).isNotNull();
        assertThat(metadataRef.get().isRequiresClarification()).isTrue();
        assertThat(metadataRef.get().getClarificationOptions()).hasSize(2);
        assertThat(metadataRef.get().getClarificationOptions().get(0)).contains("Data Structures (CS101)");
        assertThat(tokenBuffer.toString()).isEqualTo("Did you mean CS101 or CS102?");
        verifyNoInteractions(aiClient);
    }

    @Test
    @DisplayName("Outcome query should fetch authorized evidence and inject into AI streaming")
    void testOutcomeQueryFetchesAuthorizedEvidence() {
        Principal principal = () -> "iqac_user";
        AtomicReference<EmmuEvidencePackage> metadataRef = new AtomicReference<>();
        StringBuilder tokenBuffer = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);

        EmmuRootCauseDrilldownDto drilldownDto = EmmuRootCauseDrilldownDto.builder()
                .outcome(EmmuRootCauseDrilldownDto.OutcomeDetailDto.builder()
                        .code("PO3")
                        .type("PO")
                        .build())
                .dataCurrency(EmmuDataCurrency.FINALIZED_EVALUATED_DATA)
                .build();

        when(evidenceService.getRootCauseDrilldown(eq("batch-1"), eq("PO3"), eq("PO"), eq(principal)))
                .thenReturn(drilldownDto);

        doAnswer(invocation -> {
            java.util.function.Consumer<String> consumer = invocation.getArgument(3);
            Runnable onDone = invocation.getArgument(4);
            consumer.accept("Based on official records, PO3 attainment has a deficit in CS201.");
            onDone.run();
            return null;
        }).when(aiClient).stream(anyString(), anyString(), anyList(), any(), any(), any(), any());

        EmmuChatRequest request = EmmuChatRequest.builder()
                .message("Why is PO3 attainment so low?")
                .history(List.of())
                .context(Map.of("batchId", "batch-1"))
                .build();

        orchestrationService.streamChat(
                request,
                principal,
                metadataRef::set,
                tokenBuffer::append,
                () -> completed.set(true),
                err -> {},
                new AtomicBoolean(false)
        );

        assertThat(completed.get()).isTrue();
        assertThat(metadataRef.get()).isNotNull();
        assertThat(metadataRef.get().getCurrency()).isEqualTo("FINALIZED");
        assertThat(metadataRef.get().isHasEvidence()).isTrue();
        assertThat(tokenBuffer.toString()).contains("PO3 attainment has a deficit");
        verify(aiClient).stream(contains("FINALIZED"), anyString(), anyList(), any(), any(), any(), any());
    }
}
