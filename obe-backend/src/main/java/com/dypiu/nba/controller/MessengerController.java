package com.dypiu.nba.controller;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.service.MessengerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping({"/messenger", "/api/v1/messenger"})
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class MessengerController {

    private final MessengerService messengerService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchUsers(@RequestParam(required = false) String query) {
        List<Map<String, Object>> users = messengerService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .success(true)
                .message("Users retrieved successfully")
                .data(users)
                .build());
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationDto>>> listConversations() {
        List<ConversationDto> conversations = messengerService.listConversations();
        return ResponseEntity.ok(ApiResponse.<List<ConversationDto>>builder()
                .success(true)
                .message("Conversations retrieved successfully")
                .data(conversations)
                .build());
    }

    @PostMapping("/conversations/direct")
    public ResponseEntity<ApiResponse<ConversationDto>> getOrCreateDirectConversation(
            @Valid @RequestBody CreateDirectConversationRequestDto req) {
        ConversationDto conv = messengerService.getOrCreateDirectConversation(req.getPeerUserId());
        return ResponseEntity.ok(ApiResponse.<ConversationDto>builder()
                .success(true)
                .message("Conversation retrieved successfully")
                .data(conv)
                .build());
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getConversationMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<MessageDto> messages = messengerService.getConversationMessages(id, page, size);
        return ResponseEntity.ok(ApiResponse.<List<MessageDto>>builder()
                .success(true)
                .message("Messages retrieved successfully")
                .data(messages)
                .build());
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @PathVariable String id,
            @Valid @RequestBody SendMessageRequestDto req) {
        MessageDto sent = messengerService.sendMessage(id, req);
        return ResponseEntity.ok(ApiResponse.<MessageDto>builder()
                .success(true)
                .message("Message sent successfully")
                .data(sent)
                .build());
    }

    @PutMapping("/conversations/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markConversationAsRead(@PathVariable String id) {
        messengerService.markConversationAsRead(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Conversation marked as read")
                .build());
    }

    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<BadgeCountDto>> getBadgeCounts() {
        BadgeCountDto counts = messengerService.getBadgeCounts();
        return ResponseEntity.ok(ApiResponse.<BadgeCountDto>builder()
                .success(true)
                .message("Badge counts retrieved successfully")
                .data(counts)
                .build());
    }
}
