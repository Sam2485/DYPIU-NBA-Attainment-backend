package com.dypiu.nba.emmu.controller;

import com.dypiu.nba.dto.ApiResponse;
import com.dypiu.nba.emmu.dto.EmmuContextSummaryDto;
import com.dypiu.nba.emmu.dto.EmmuCourseIntelligenceDto;
import com.dypiu.nba.emmu.dto.EmmuCourseRankingDto;
import com.dypiu.nba.emmu.dto.EmmuRootCauseDrilldownDto;
import com.dypiu.nba.emmu.service.EmmuEvidenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping({"/emmu", "/api/v1/emmu", "/api/v1/analytics/emmu"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('IQAC', 'SUPER_ADMIN', 'DIRECTOR', 'HOD', 'PROGRAMME_COORDINATOR', 'FACULTY', 'COURSE_COORDINATOR', 'ADMIN')")
public class EmmuController {

    private final EmmuEvidenceService emmuEvidenceService;
    private final com.dypiu.nba.emmu.resolver.AcademicEntityResolver academicEntityResolver;

    @PostMapping("/resolve")
    public ResponseEntity<ApiResponse<com.dypiu.nba.emmu.dto.EntityResolutionResult>> resolveEntity(
            @RequestBody com.dypiu.nba.emmu.dto.EntityResolutionRequest request,
            Principal principal) {

        log.debug("[EmmuController] resolveEntity (POST): query={}, entityType={}",
                request != null ? request.getQuery() : null, request != null ? request.getEntityType() : null);

        com.dypiu.nba.emmu.dto.EntityResolutionResult result = academicEntityResolver.resolve(request, principal);

        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.emmu.dto.EntityResolutionResult>builder()
                .success(true)
                .message("Entity resolved successfully")
                .data(result)
                .build());
    }

    @GetMapping("/resolve")
    public ResponseEntity<ApiResponse<com.dypiu.nba.emmu.dto.EntityResolutionResult>> resolveEntityGet(
            @RequestParam String query,
            @RequestParam(required = false) com.dypiu.nba.emmu.dto.AcademicEntityType entityType,
            @RequestParam(required = false) String programmeBatchId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String programmeBatchCourseId,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String outcomeCode,
            Principal principal) {

        log.debug("[EmmuController] resolveEntity (GET): query={}, entityType={}", query, entityType);

        com.dypiu.nba.emmu.dto.EmmuConversationContext context = com.dypiu.nba.emmu.dto.EmmuConversationContext.builder()
                .programmeBatchId(programmeBatchId)
                .masterProgrammeId(masterProgrammeId)
                .departmentId(departmentId)
                .schoolId(schoolId)
                .programmeBatchCourseId(programmeBatchCourseId)
                .semester(semester)
                .outcomeCode(outcomeCode)
                .build();

        com.dypiu.nba.emmu.dto.EntityResolutionRequest request = com.dypiu.nba.emmu.dto.EntityResolutionRequest.builder()
                .query(query)
                .entityType(entityType)
                .context(context)
                .build();

        com.dypiu.nba.emmu.dto.EntityResolutionResult result = academicEntityResolver.resolve(request, principal);

        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.emmu.dto.EntityResolutionResult>builder()
                .success(true)
                .message("Entity resolved successfully")
                .data(result)
                .build());
    }

    @GetMapping("/context-summary")
    public ResponseEntity<ApiResponse<EmmuContextSummaryDto>> getContextSummary(
            @RequestParam(required = false) String schoolId,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String masterProgrammeId,
            @RequestParam(required = false) String programmeBatchId,
            Principal principal) {

        log.debug("[EmmuController] getContextSummary: schoolId={}, departmentId={}, masterProgrammeId={}, programmeBatchId={}",
                schoolId, departmentId, masterProgrammeId, programmeBatchId);

        EmmuContextSummaryDto data = emmuEvidenceService.getContextSummary(
                schoolId, departmentId, masterProgrammeId, programmeBatchId, principal);

        return ResponseEntity.ok(ApiResponse.<EmmuContextSummaryDto>builder()
                .success(true)
                .message("Emmu context summary retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/root-cause-drilldown")
    public ResponseEntity<ApiResponse<EmmuRootCauseDrilldownDto>> getRootCauseDrilldown(
            @RequestParam(required = false) String programmeBatchId,
            @RequestParam String outcomeCode,
            @RequestParam(defaultValue = "PO") String outcomeType,
            Principal principal) {

        log.debug("[EmmuController] getRootCauseDrilldown: programmeBatchId={}, outcomeCode={}, outcomeType={}",
                programmeBatchId, outcomeCode, outcomeType);

        EmmuRootCauseDrilldownDto data = emmuEvidenceService.getRootCauseDrilldown(
                programmeBatchId, outcomeCode, outcomeType, principal);

        return ResponseEntity.ok(ApiResponse.<EmmuRootCauseDrilldownDto>builder()
                .success(true)
                .message("Emmu root-cause drilldown retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/course-rankings")
    public ResponseEntity<ApiResponse<EmmuCourseRankingDto>> getCourseRankings(
            @RequestParam(required = false) String programmeBatchId,
            @RequestParam(required = false) Integer semester,
            @RequestParam(defaultValue = "attainment") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            Principal principal) {

        log.debug("[EmmuController] getCourseRankings: programmeBatchId={}, semester={}, sortBy={}, direction={}",
                programmeBatchId, semester, sortBy, direction);

        EmmuCourseRankingDto data = emmuEvidenceService.getCourseRankings(
                programmeBatchId, semester, sortBy, direction, principal);

        return ResponseEntity.ok(ApiResponse.<EmmuCourseRankingDto>builder()
                .success(true)
                .message("Emmu course rankings retrieved successfully")
                .data(data)
                .build());
    }

    @GetMapping("/course-intelligence")
    public ResponseEntity<ApiResponse<EmmuCourseIntelligenceDto>> getCourseIntelligence(
            @RequestParam(required = false) String programmeBatchCourseId,
            @RequestParam(required = false) String masterCourseId,
            Principal principal) {

        log.debug("[EmmuController] getCourseIntelligence: programmeBatchCourseId={}, masterCourseId={}",
                programmeBatchCourseId, masterCourseId);

        EmmuCourseIntelligenceDto data = emmuEvidenceService.getCourseIntelligence(
                programmeBatchCourseId, masterCourseId, principal);

        return ResponseEntity.ok(ApiResponse.<EmmuCourseIntelligenceDto>builder()
                .success(true)
                .message("Emmu course intelligence retrieved successfully")
                .data(data)
                .build());
    }

    private final com.dypiu.nba.emmu.service.EmmuOrchestrationService orchestrationService;
    private final com.dypiu.nba.emmu.ai.EmmuAiClient aiClient;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getHealth() {
        boolean ollamaOnline = aiClient.isAvailable();
        return ResponseEntity.ok(ApiResponse.<java.util.Map<String, Object>>builder()
                .success(true)
                .message("Emmu status retrieved")
                .data(java.util.Map.of(
                        "service", "DYPIU OBE Emmu Intelligence Assistant",
                        "ollamaOnline", ollamaOnline,
                        "status", ollamaOnline ? "UP" : "DEGRADED"
                ))
                .build());
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<com.dypiu.nba.emmu.dto.EmmuChatResponse>> chat(
            @RequestBody com.dypiu.nba.emmu.dto.EmmuChatRequest request,
            Principal principal) {

        StringBuilder responseBuilder = new StringBuilder();
        java.util.concurrent.atomic.AtomicReference<com.dypiu.nba.emmu.dto.EmmuEvidencePackage> metaRef = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean cancelFlag = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicReference<Throwable> errorRef = new java.util.concurrent.atomic.AtomicReference<>();

        orchestrationService.streamChat(
                request,
                principal,
                metaRef::set,
                responseBuilder::append,
                latch::countDown,
                err -> {
                    errorRef.set(err);
                    latch.countDown();
                },
                cancelFlag
        );

        try {
            latch.await(60, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }

        if (errorRef.get() != null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.<com.dypiu.nba.emmu.dto.EmmuChatResponse>builder()
                            .success(false)
                            .message("I couldn't retrieve the OBE evidence right now: " + errorRef.get().getMessage())
                            .build());
        }

        com.dypiu.nba.emmu.dto.EmmuEvidencePackage pkg = metaRef.get();

        com.dypiu.nba.emmu.dto.EmmuChatResponse chatResp = com.dypiu.nba.emmu.dto.EmmuChatResponse.builder()
                .response(responseBuilder.toString())
                .currency(pkg != null ? pkg.getCurrency() : "LIVE")
                .intent(pkg != null ? pkg.getPrimaryIntent() : null)
                .requiresClarification(pkg != null && pkg.isRequiresClarification())
                .clarificationOptions(pkg != null ? pkg.getClarificationOptions() : null)
                .build();

        return ResponseEntity.ok(ApiResponse.<com.dypiu.nba.emmu.dto.EmmuChatResponse>builder()
                .success(true)
                .message("Emmu response generated")
                .data(chatResp)
                .build());
    }
}
