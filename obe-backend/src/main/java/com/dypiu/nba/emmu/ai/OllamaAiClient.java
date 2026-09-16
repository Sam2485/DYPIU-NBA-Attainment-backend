package com.dypiu.nba.emmu.ai;

import com.dypiu.nba.emmu.dto.ChatMessageDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaAiClient implements EmmuAiClient {

    private final EmmuAiProperties properties;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + "/api/tags"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("[OllamaAiClient] Health check failed at {}: {}", properties.getBaseUrl(), e.getMessage());
            return false;
        }
    }

    @Override
    public String generate(String systemPrompt, String userMessage, List<ChatMessageDto> history) {
        StringBuilder fullResponse = new StringBuilder();
        AtomicBoolean cancelFlag = new AtomicBoolean(false);

        stream(systemPrompt, userMessage, history,
                fullResponse::append,
                () -> {},
                err -> {
                    throw new RuntimeException("Ollama generation failed: " + err.getMessage(), err);
                },
                cancelFlag
        );

        return fullResponse.toString();
    }

    @Override
    public void stream(String systemPrompt,
                       String userMessage,
                       List<ChatMessageDto> history,
                       Consumer<String> onToken,
                       Runnable onComplete,
                       Consumer<Throwable> onError,
                       AtomicBoolean cancelFlag) {

        try {
            List<Map<String, String>> messages = new ArrayList<>();

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                messages.add(Map.of("role", "system", "content", systemPrompt));
            }

            if (history != null && !history.isEmpty()) {
                int start = Math.max(0, history.size() - 8);
                for (int i = start; i < history.size(); i++) {
                    ChatMessageDto item = history.get(i);
                    if (item.getText() != null && !item.getText().isBlank()) {
                        String role = "user".equalsIgnoreCase(item.getSender()) ? "user" : "assistant";
                        messages.add(Map.of("role", role, "content", item.getText()));
                    }
                }
            }

            if (userMessage != null && !userMessage.isBlank()) {
                messages.add(Map.of("role", "user", "content", userMessage));
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", properties.getChat().getOptions().getModel());
            payload.put("messages", messages);
            payload.put("stream", true);

            Map<String, Object> options = new HashMap<>();
            options.put("temperature", properties.getChat().getOptions().getTemperature());
            options.put("top_p", properties.getChat().getOptions().getTopP());
            payload.put("options", options);

            String requestJson = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getBaseUrl() + "/api/chat"))
                    .timeout(Duration.ofMinutes(3))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/x-ndjson, application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                onError.accept(new RuntimeException("Ollama returned HTTP status: " + response.statusCode()));
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cancelFlag != null && cancelFlag.get()) {
                        log.info("[OllamaAiClient] Stream cancelled by client request");
                        break;
                    }

                    line = line.trim();
                    if (line.isEmpty()) continue;

                    try {
                        JsonNode node = objectMapper.readTree(line);
                        JsonNode msgNode = node.path("message");
                        String content = msgNode.path("content").asText("");

                        if (!content.isEmpty()) {
                            onToken.accept(content);
                        }

                        if (node.path("done").asBoolean(false)) {
                            break;
                        }
                    } catch (Exception parseEx) {
                        log.warn("[OllamaAiClient] Error parsing chunk: {}", parseEx.getMessage());
                    }
                }
            }

            onComplete.run();

        } catch (Exception e) {
            log.warn("[OllamaAiClient] Streaming error: {}", e.getMessage());
            onError.accept(e);
        }
    }
}
