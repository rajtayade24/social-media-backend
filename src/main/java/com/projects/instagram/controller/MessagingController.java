package com.projects.instagram.controller;

import com.projects.instagram.dto.*;
import com.projects.instagram.service.MessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "http://localhost:5173",
        "http://10.91.2.29:5173",
        "http://10.91.2.29:5173/instagram-clone",
        "*"
})
public class MessagingController {

    private final MessagingService messageService;

    @PostMapping("/conversations/{userId}")
    public ResponseEntity<ConversationDto> createConversation(@PathVariable Long userId,
                                                              @RequestBody ConversationCreateRequest req,
                                                              Authentication authentication) {
        Long currentUserId = resolveUserId(userId, authentication);
        ConversationDto dto = messageService.createOrGetDirectConversation(currentUserId, req.getParticipantIds());
        return ResponseEntity.created(URI.create("/api/conversations/" + dto.getId())).body(dto);
    }

    // list conversations for user
    @GetMapping("/users/{userId}/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(@PathVariable Long userId,
                                                                  Authentication authentication) {
        // you can allow admin fetching via param; for now we just return requested id
        List<ConversationDto> list = messageService.getConversationsForUser(userId);
        return ResponseEntity.ok(list);
    }

    // send message (either to existing conversation or supply participantIds to create conversation first)
    @PostMapping("/conversations/{conversationId}/messages/{userId}")
    public ResponseEntity<MessageDto> sendMessageToConversation(@PathVariable("conversationId") Long conversationId,
                                                                @PathVariable Long userId,
                                                                @RequestBody SendMessageRequestDto req,
                                                                Authentication authentication) {
        Long currentUserId = resolveUserId(userId, authentication);
        // ensure conversationId present in request
        req.setConversationId(conversationId);
        MessageDto sent = messageService.sendMessage(currentUserId, req);
        return ResponseEntity.created(URI.create("/api/conversations/" + conversationId + "/messages/" + sent.getId())).body(sent);
    }

    // get messages with pagination (client can request size/page)
    @GetMapping("/conversations/{conversationId}/messages/{userId}")
    public ResponseEntity<Page<MessageDto>> getMessages(@PathVariable Long conversationId,
                                                        @PathVariable Long userId,
                                                        @RequestParam(name = "page", defaultValue = "0") int page,
                                                        @RequestParam(name = "size", defaultValue = "50") int size,
                                                        Authentication authentication) {
        Long currentUserId = resolveUserId(userId, authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<MessageDto> msgs = messageService.getMessages(conversationId, pageable, currentUserId);
        return ResponseEntity.ok(msgs);
    }

    @PostMapping("/conversations/{conversationId}/read/{userId}")
    public ResponseEntity<Map<String, Object>> markRead(
            @PathVariable Long conversationId,
            @PathVariable Long userId,
            Authentication authentication) {

        Long currentUserId = resolveUserId(userId, authentication); // derive from authentication
        int updatedCount = messageService.markConversationRead(conversationId, currentUserId);

        return ResponseEntity.ok(Map.of("readCount", updatedCount));
    }

    @GetMapping("/users/{userId}/conversations/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadConversationsCount(
            @PathVariable Long userId,
            Authentication authentication) {

        Long currentUserId = resolveUserId(userId, authentication);
        if (!currentUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        long unreadConversations = messageService.countUnreadConversations(userId);
        return ResponseEntity.ok(Map.of("unreadConversations", unreadConversations));
    }

    @GetMapping("/users/{userId}/conversations/unread-messages")
    public ResponseEntity<Map<String, Long>> getUnreadMessagesCount(
            @PathVariable Long userId,
            Authentication authentication) {

        Long currentUserId = resolveUserId(userId, authentication);
        if (!currentUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        long unreadMessages = messageService.countUnreadMessages(userId);
        return ResponseEntity.ok(Map.of("unreadMessages", unreadMessages));
    }

    // ---------------- Helper ----------------
    private Long resolveUserId(Long overrideUserId, Authentication authentication) {
        if (overrideUserId != null) return overrideUserId;
        if (authentication == null) {
            throw new IllegalArgumentException("Authenticated user not found. Provide userId or authenticate.");
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            // if your Security stores username rather than id, adapt here (e.g., look up user by username)
            throw new IllegalArgumentException("Unable to resolve current user's id from authentication principal. Provide userId param or adapt resolveUserId() to your security setup.");
        }
    }

    @GetMapping("/users/{id}/messaged-users")
    public ResponseEntity<List<MessagedUserDto>> getMessagedUsers(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = resolveUserId(id, authentication);
        if (!currentUserId.equals(id)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(messageService.getMessagedUsers(id));
    }
}
