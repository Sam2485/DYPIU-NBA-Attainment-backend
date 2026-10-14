package com.dypiu.nba.service;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.Conversation;
import com.dypiu.nba.entity.Message;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.repository.ConversationRepository;
import com.dypiu.nba.repository.MessageRepository;
import com.dypiu.nba.repository.NotificationRepository;
import com.dypiu.nba.repository.SuggestionRepository;
import com.dypiu.nba.repository.UserRepository;
import com.dypiu.nba.security.CurrentUserScope;
import com.dypiu.nba.security.CurrentUserScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessengerService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final SuggestionRepository suggestionRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserScopeService currentUserScopeService;

    private User getAuthenticatedUser() {
        CurrentUserScope scope = currentUserScopeService.getCurrentUserScope();
        if (scope == null || scope.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return userRepository.findById(scope.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    /**
     * Get or create a direct conversation with a peer user.
     * Enforces canonical order (userOneId < userTwoId) to avoid duplicates.
     */
    @Transactional
    public ConversationDto getOrCreateDirectConversation(Long peerUserId) {
        User currentUser = getAuthenticatedUser();
        if (currentUser.getId().equals(peerUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot start a conversation with yourself");
        }

        User peer = userRepository.findById(peerUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Peer user not found"));

        Long userOne = Math.min(currentUser.getId(), peer.getId());
        Long userTwo = Math.max(currentUser.getId(), peer.getId());

        Conversation conversation = conversationRepository.findByUserPair(userOne, userTwo)
                .orElseGet(() -> {
                    Conversation c = Conversation.builder()
                            .userOneId(userOne)
                            .userTwoId(userTwo)
                            .lastMessageAt(ZonedDateTime.now())
                            .build();
                    return conversationRepository.save(c);
                });

        return toConversationDto(conversation, currentUser.getId(), peer);
    }

    /**
     * List conversations for the authenticated user ordered by last message time.
     */
    @Transactional(readOnly = true)
    public List<ConversationDto> listConversations() {
        User currentUser = getAuthenticatedUser();
        List<Conversation> convs = conversationRepository.findUserConversations(currentUser.getId());

        if (convs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> peerIds = new HashSet<>();
        for (Conversation c : convs) {
            peerIds.add(c.getUserOneId().equals(currentUser.getId()) ? c.getUserTwoId() : c.getUserOneId());
        }

        Map<Long, User> peers = userRepository.findAllById(peerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return convs.stream().map(c -> {
            Long peerId = c.getUserOneId().equals(currentUser.getId()) ? c.getUserTwoId() : c.getUserOneId();
            User peer = peers.get(peerId);
            return toConversationDto(c, currentUser.getId(), peer);
        }).collect(Collectors.toList());
    }

    /**
     * Get paginated messages for a conversation.
     */
    @Transactional(readOnly = true)
    public List<MessageDto> getConversationMessages(String conversationId, int page, int size) {
        User currentUser = getAuthenticatedUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.getUserOneId().equals(currentUser.getId()) && !conversation.getUserTwoId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this conversation");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Message> messagePage = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);

        List<Message> list = new ArrayList<>(messagePage.getContent());
        Collections.reverse(list); // Chronological order for chat display

        return list.stream().map(this::toMessageDto).collect(Collectors.toList());
    }

    /**
     * Send a message in a conversation.
     */
    @Transactional
    public MessageDto sendMessage(String conversationId, SendMessageRequestDto req) {
        User currentUser = getAuthenticatedUser();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        if (!conversation.getUserOneId().equals(currentUser.getId()) && !conversation.getUserTwoId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to this conversation");
        }

        Long recipientId = conversation.getUserOneId().equals(currentUser.getId()) ? conversation.getUserTwoId() : conversation.getUserOneId();

        Message msg = Message.builder()
                .conversationId(conversationId)
                .senderId(currentUser.getId())
                .recipientId(recipientId)
                .content(req.getContent())
                .iv(req.getIv())
                .isEncrypted(req.getIsEncrypted() != null ? req.getIsEncrypted() : true)
                .build();

        Message saved = messageRepository.save(msg);

        // Update conversation last message timestamp & preview
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversation.setLastMessageSenderId(currentUser.getId());
        String preview = Boolean.TRUE.equals(saved.getIsEncrypted()) ? "[Encrypted message]" : saved.getContent();
        if (preview.length() > 100) preview = preview.substring(0, 97) + "...";
        conversation.setLastMessagePreview(preview);
        conversationRepository.save(conversation);

        return toMessageDto(saved);
    }

    /**
     * Mark a conversation as read by the authenticated recipient.
     */
    @Transactional
    public void markConversationAsRead(String conversationId) {
        User currentUser = getAuthenticatedUser();
        messageRepository.markConversationAsRead(conversationId, currentUser.getId(), ZonedDateTime.now());
    }

    /**
     * Search eligible users for chatting.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchUsers(String query) {
        User currentUser = getAuthenticatedUser();
        List<User> users;
        if (query != null && !query.trim().isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            users = userRepository.searchUsers(q);
        } else {
            users = userRepository.findAll().stream().limit(50).collect(Collectors.toList());
        }

        return users.stream()
                .filter(u -> !u.getId().equals(currentUser.getId()) && Boolean.TRUE.equals(u.getIsActive()))
                .map(u -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", u.getId());
                    map.put("name", u.getName());
                    map.put("email", u.getEmail());
                    map.put("role", u.getRole() != null ? u.getRole().name() : "USER");
                    map.put("schoolId", u.getSchoolId());
                    map.put("departmentId", u.getDepartmentId());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Unified unread badge count across (1) Chats, (2) Suggestions, (3) Notifications.
     */
    @Transactional(readOnly = true)
    public BadgeCountDto getBadgeCounts() {
        User currentUser = getAuthenticatedUser();
        long unreadChats = messageRepository.countTotalUnreadMessagesForUser(currentUser.getId());
        long unreadSuggestions = suggestionRepository.countUnreadSuggestionsForUser(currentUser.getId());
        long unreadNotifications = notificationRepository.countUnreadNotificationsForUser(currentUser.getId());

        return BadgeCountDto.builder()
                .unreadChats(unreadChats)
                .unreadSuggestions(unreadSuggestions)
                .unreadNotifications(unreadNotifications)
                .totalUnread(unreadChats + unreadSuggestions + unreadNotifications)
                .build();
    }

    private ConversationDto toConversationDto(Conversation c, Long currentUserId, User peer) {
        Long peerId = c.getUserOneId().equals(currentUserId) ? c.getUserTwoId() : c.getUserOneId();
        long unread = messageRepository.countUnreadMessages(c.getId(), currentUserId);

        return ConversationDto.builder()
                .id(c.getId())
                .userOneId(c.getUserOneId())
                .userTwoId(c.getUserTwoId())
                .peerId(peerId)
                .peerName(peer != null ? peer.getName() : "User " + peerId)
                .peerEmail(peer != null ? peer.getEmail() : null)
                .peerRole(peer != null && peer.getRole() != null ? peer.getRole().name() : null)
                .lastMessagePreview(c.getLastMessagePreview())
                .lastMessageSenderId(c.getLastMessageSenderId())
                .lastMessageAt(c.getLastMessageAt())
                .unreadCount(unread)
                .build();
    }

    private MessageDto toMessageDto(Message m) {
        return MessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .recipientId(m.getRecipientId())
                .content(m.getContent())
                .iv(m.getIv())
                .isEncrypted(m.getIsEncrypted())
                .readAt(m.getReadAt())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
