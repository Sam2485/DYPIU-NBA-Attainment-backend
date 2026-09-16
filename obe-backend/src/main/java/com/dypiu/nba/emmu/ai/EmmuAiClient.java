package com.dypiu.nba.emmu.ai;

import com.dypiu.nba.emmu.dto.ChatMessageDto;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Interface contract for AI / LLM inference used by Emmu.
 * Enables interchangeable backends (Ollama, vLLM, mock) without altering business logic.
 */
public interface EmmuAiClient {

    String generate(String systemPrompt, String userMessage, List<ChatMessageDto> history);

    void stream(String systemPrompt,
                String userMessage,
                List<ChatMessageDto> history,
                Consumer<String> onToken,
                Runnable onComplete,
                Consumer<Throwable> onError,
                AtomicBoolean cancelFlag);

    boolean isAvailable();
}
