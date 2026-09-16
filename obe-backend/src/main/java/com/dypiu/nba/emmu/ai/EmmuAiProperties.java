package com.dypiu.nba.emmu.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.ai.ollama")
public class EmmuAiProperties {

    private String baseUrl = "http://127.0.0.1:11434";
    private Chat chat = new Chat();

    @Data
    public static class Chat {
        private Options options = new Options();
    }

    @Data
    public static class Options {
        private String model = "llama3.1:8b";
        private Double temperature = 0.3;
        private Double topP = 0.9;
    }
}
