package com.dypiu.nba.emmu.websocket;

import com.dypiu.nba.emmu.dto.ChatMessageDto;
import com.dypiu.nba.emmu.dto.EmmuChatRequest;
import com.dypiu.nba.emmu.dto.EmmuEvidencePackage;
import com.dypiu.nba.emmu.service.EmmuOrchestrationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmmuChatWebSocketHandler extends TextWebSocketHandler {

    private final EmmuOrchestrationService orchestrationService;
    private final ObjectMapper objectMapper;

    // Track active cancellation flags per WebSocket session
    private final Map<String, AtomicBoolean> activeStreams = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[EmmuWebSocket] Client connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode root = objectMapper.readTree(payload);

        String actionType = root.path("type").asText("chat");

        // Client requested to stop/cancel generation
        if ("stop".equalsIgnoreCase(actionType)) {
            cancelActiveStream(session.getId());
            sendJson(session, Map.of("type", "stopped"));
            return;
        }

        String userPrompt = root.path("message").asText("");
        if (userPrompt.isBlank()) {
            return;
        }

        List<ChatMessageDto> history = new ArrayList<>();
        JsonNode historyNode = root.path("history");
        if (historyNode.isArray()) {
            for (JsonNode item : historyNode) {
                history.add(new ChatMessageDto(
                        item.path("sender").asText("user"),
                        item.path("text").asText("")
                ));
            }
        }

        Map<String, Object> context = null;
        JsonNode contextNode = root.path("context");
        if (contextNode.isObject()) {
            context = objectMapper.convertValue(contextNode, new TypeReference<>() {});
        }

        EmmuChatRequest chatRequest = EmmuChatRequest.builder()
                .message(userPrompt)
                .history(history)
                .context(context)
                .build();

        // Resolve authenticated Principal
        Principal principal = (Principal) session.getAttributes().get(JwtWebSocketHandshakeInterceptor.PRINCIPAL_ATTR);
        if (principal == null) {
            principal = session.getPrincipal();
        }

        // Cancel previous stream if already active for this session
        cancelActiveStream(session.getId());

        AtomicBoolean cancelFlag = new AtomicBoolean(false);
        activeStreams.put(session.getId(), cancelFlag);

        // Notify client stream has started
        sendJson(session, Map.of("type", "start"));

        final Principal finalPrincipal = principal;

        // Run streaming pipeline asynchronously in virtual thread / worker thread
        Thread.ofVirtual().start(() -> {
            try {
                orchestrationService.streamChat(
                        chatRequest,
                        finalPrincipal,
                        (EmmuEvidencePackage pkg) -> {
                            try {
                                Map<String, Object> meta = new HashMap<>();
                                meta.put("type", "metadata");
                                meta.put("currency", pkg.getCurrency() != null ? pkg.getCurrency() : "LIVE");
                                if (pkg.getPrimaryIntent() != null) {
                                    meta.put("intent", pkg.getPrimaryIntent().name());
                                }
                                if (pkg.getClarificationOptions() != null && !pkg.getClarificationOptions().isEmpty()) {
                                    meta.put("clarificationOptions", pkg.getClarificationOptions());
                                }
                                meta.put("hasEvidence", pkg.isHasEvidence());
                                sendJson(session, meta);
                            } catch (Exception e) {
                                log.warn("[EmmuWebSocket] Error sending metadata: {}", e.getMessage());
                            }
                        },
                        (String tokenChunk) -> {
                            try {
                                if (session.isOpen()) {
                                    sendJson(session, Map.of(
                                            "type", "token",
                                            "token", tokenChunk
                                    ));
                                }
                            } catch (Exception e) {
                                log.warn("[EmmuWebSocket] Error sending token: {}", e.getMessage());
                            }
                        },
                        () -> {
                            try {
                                if (session.isOpen()) {
                                    sendJson(session, Map.of("type", "done"));
                                }
                            } catch (Exception e) {
                                log.warn("[EmmuWebSocket] Error sending done: {}", e.getMessage());
                            } finally {
                                activeStreams.remove(session.getId());
                            }
                        },
                        (Throwable error) -> {
                            try {
                                log.warn("[EmmuWebSocket] Stream error for session {}: {}", session.getId(), error.getMessage());
                                if (session.isOpen()) {
                                    sendJson(session, Map.of(
                                            "type", "error",
                                            "message", "I couldn't retrieve the OBE evidence right now. Please try again."
                                    ));
                                }
                            } catch (Exception ignored) {
                            } finally {
                                activeStreams.remove(session.getId());
                            }
                        },
                        cancelFlag
                );
            } catch (Exception e) {
                log.error("[EmmuWebSocket] Unhandled error in chat thread: {}", e.getMessage(), e);
                try {
                    if (session.isOpen()) {
                        sendJson(session, Map.of(
                                "type", "error",
                                "message", "The OBE assistant encountered an issue. Please try again."
                        ));
                    }
                } catch (Exception ignored) {
                } finally {
                    activeStreams.remove(session.getId());
                }
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("[EmmuWebSocket] Client disconnected: {}", session.getId());
        cancelActiveStream(session.getId());
    }

    private void cancelActiveStream(String sessionId) {
        AtomicBoolean flag = activeStreams.remove(sessionId);
        if (flag != null) {
            flag.set(true);
        }
    }

    private synchronized void sendJson(WebSocketSession session, Object data) throws IOException {
        if (session != null && session.isOpen()) {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        }
    }
}
