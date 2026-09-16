package com.dypiu.nba.emmu;

import com.dypiu.nba.emmu.ai.EmmuAiClient;
import com.dypiu.nba.emmu.dto.ChatMessageDto;
import com.dypiu.nba.emmu.dto.EmmuChatRequest;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.entity.UserRole;
import com.dypiu.nba.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class EmmuChatApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private EmmuAiClient aiClient;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByUsername("iqac_test_user")) {
            userRepository.save(User.builder()
                    .username("iqac_test_user")
                    .email("iqac_test@dypiu.ac.in")
                    .name("IQAC Test User")
                    .passwordHash("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF")
                    .role(UserRole.IQAC)
                    .isActive(true)
                    .build());
        }
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("POST /api/v1/emmu/chat with mutation request triggers read-only invariant message")
    void testMutationRequestRestEndpoint() throws Exception {
        EmmuChatRequest chatReq = EmmuChatRequest.builder()
                .message("Please approve the ATR report for the CSE department")
                .history(Collections.emptyList())
                .context(Collections.emptyMap())
                .build();

        mockMvc.perform(post("/api/v1/emmu/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intent").value("MUTATION_ATTEMPT"))
                .andExpect(jsonPath("$.data.response").value(org.hamcrest.Matchers.containsString("strictly read-only access")));
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("POST /api/v1/emmu/chat with greeting streams AI response without errors")
    void testGreetingChatRestEndpoint() throws Exception {
        doAnswer(invocation -> {
            Consumer<String> onToken = invocation.getArgument(3);
            Runnable onComplete = invocation.getArgument(4);
            onToken.accept("Hello! I am Emmu, your NBA attainment AI assistant.");
            onComplete.run();
            return null;
        }).when(aiClient).stream(anyString(), anyString(), anyList(), any(), any(), any(), any());

        EmmuChatRequest chatReq = EmmuChatRequest.builder()
                .message("Hello Emmu, what can you do?")
                .history(Collections.emptyList())
                .context(Collections.emptyMap())
                .build();

        mockMvc.perform(post("/api/v1/emmu/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.intent").value("GENERIC_CHAT"))
                .andExpect(jsonPath("$.data.response").value(org.hamcrest.Matchers.containsString("NBA attainment AI assistant")));
    }

    @Test
    @DisplayName("Unauthenticated request to /api/v1/emmu/chat should be rejected with 401 or 403")
    void testUnauthenticatedChatAccess() throws Exception {
        EmmuChatRequest chatReq = EmmuChatRequest.builder()
                .message("Hello")
                .build();

        mockMvc.perform(post("/api/v1/emmu/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "iqac_test_user", roles = {"IQAC"})
    @DisplayName("GET /api/v1/emmu/health returns service status")
    void testEmmuHealth() throws Exception {
        mockMvc.perform(get("/api/v1/emmu/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.service").value("DYPIU OBE Emmu Intelligence Assistant"))
                .andExpect(jsonPath("$.data.status").exists());
    }
}
