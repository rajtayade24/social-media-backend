package com.projects.instagram.service.impl;

import com.projects.instagram.dto.*;
import com.projects.instagram.entity.Conversation;
import com.projects.instagram.entity.Message;
import com.projects.instagram.entity.User;
import com.projects.instagram.repository.ConversationRepository;
import com.projects.instagram.repository.MessageRepository;
import com.projects.instagram.repository.UserReposotory;
import com.projects.instagram.service.MessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingServiceImpl implements MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserReposotory userReposotory;
    private final ModelMapper modelMapper;
    private final SimpMessagingTemplate messagingTemplate; // to broadcast over websocket

    /**
     * For direct chats we look for an existing conversation that contains exactly the provided participants
     * (simple approach: load conversations of one participant and filter in memory).
     */
    @Override
    @Transactional
    public ConversationDto createOrGetDirectConversation(Long requesterId, List<Long> participantIds) {
        // ensure requester included
        Set<Long> participantsSet = new HashSet<>(participantIds);
        participantsSet.add(requesterId);

        // pick one participant (first) and scan their conversations
        Long probe = participantsSet.iterator().next();
        List<Conversation> candidates = conversationRepository.findByParticipant(probe);

        for (Conversation c : candidates) {
            Set<Long> cIds = c.getParticipants().stream().map(User::getId).collect(Collectors.toSet());
            if (cIds.equals(participantsSet)) {
                return toConversationDto(c, requesterId, null);
            }
        }

        // not found -> create
        Conversation conv = new Conversation();
        conv.setCreatedAt(Instant.now());
        conv.setLastMessageAt(null);

        for (Long uid : participantsSet) {
            User u = userReposotory.findById(uid).orElseThrow(() -> new IllegalArgumentException("User not found: " + uid));
            conv.getParticipants().add(u);
        }

        Conversation saved = conversationRepository.save(conv);
        return toConversationDto(saved, requesterId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDto getConversationDto(Long conversationId, Long requesterId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        return toConversationDto(conv, requesterId, null);
    }

    @Override
    @Transactional
    public MessageDto sendMessage(Long requesterId, SendMessageRequestDto req) {
        Conversation conversation;

        if (req.getConversationId() != null) {
            conversation = conversationRepository.findById(req.getConversationId())
                    .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + req.getConversationId()));
        } else {
            // create or get conversation from participantIds
            ConversationDto dto = createOrGetDirectConversation(requesterId, req.getParticipantIds());
            conversation = conversationRepository.findById(dto.getId()).orElseThrow();
        }

        Message m = new Message();
        m.setConversation(conversation);
        m.setSenderId(requesterId);
        m.setContent(req.getContent());
        m.setCreatedAt(Instant.now());
        m.setReadByRecipient(false);

        Message saved = messageRepository.save(m);

        // update lastMessageAt on conversation
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversationRepository.save(conversation);

        MessageDto out = modelMapper.map(saved, MessageDto.class);
        out.setConversationId(conversation.getId());

        // broadcast to topic for that conversation (subscribed clients will receive)
        try {
            messagingTemplate.convertAndSend("/topic/conversations/" + conversation.getId(), out);
        } catch (Exception e) {
            log.warn("Failed to broadcast websocket message: {}", e.getMessage());
        }

        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageDto> getMessages(Long conversationId, Pageable pageable, Long requesterId) {
        Page<Message> page = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
        List<MessageDto> dtos = page.stream()
                .map(m -> {
                    MessageDto md = new MessageDto();
                    md.setId(m.getId());
                    md.setConversationId(m.getConversation().getId());
                    md.setSenderId(m.getSenderId());
                    md.setContent(m.getContent());
                    md.setReadByRecipient(m.isReadByRecipient());
                    md.setCreatedAt(m.getCreatedAt());
                    return md;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationDto> getConversationsForUser(Long userId) {
        List<Conversation> conversations = conversationRepository.findByParticipant(userId);
        return conversations.stream()
                .map(c -> toConversationDto(c, userId, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int markConversationRead(Long conversationId, Long requesterId) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        boolean isParticipant = conv.getParticipants().stream()
                .anyMatch(u -> Objects.equals(u.getId(), requesterId));
        if (!isParticipant) {
            throw new IllegalArgumentException("User is not a participant of this conversation.");
        }

        // Bulk DB update - returns number of rows affected
        int updated = messageRepository.markReadByConversationIdAndSenderIdNot(conversationId, requesterId);

        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadConversations(Long requesterId) {
        // Optional: validate requester exists (skipped for brevity)
        return messageRepository.countDistinctConversationIdsWithUnreadForUser(requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadMessages(Long requesterId) {
        return messageRepository.countUnreadMessagesForUser(requesterId);
    }

    // Helper to map Conversation -> ConversationDto
    private ConversationDto toConversationDto(Conversation conv, Long requesterId, String lastPreview) {
        ConversationDto dto = new ConversationDto();
        dto.setId(conv.getId());
        dto.setLastMessageAt(conv.getLastMessageAt());

        List<UserSummaryDto> participants = conv.getParticipants().stream()
                .map(u -> new UserSummaryDto(u.getId(), u.getUsername(), u.getName(), u.getProfilePhotoUrl(), u.getBio()))
                .collect(Collectors.toList());
        dto.setParticipants(participants);

        // last message preview (fetch last message, naive)
        Page<Message> last = messageRepository.findByConversationIdOrderByCreatedAtAsc(conv.getId(), PageRequest.of(0, 1, Sort.by("createdAt").descending()));
        if (!last.isEmpty()) {
            dto.setLastMessagePreview(last.getContent().get(0).getContent());
        }

        long unread = messageRepository.countByConversationIdAndReadByRecipientFalseAndSenderIdNot(conv.getId(), requesterId);
        dto.setUnreadCount(unread);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessagedUserDto> getMessagedUsers(Long requesterId) {
        // Fetch conversations that include requester
        List<Conversation> convs = conversationRepository.findByParticipant(requesterId);

        // For each conversation, pick other participants (for 1:1 chats there will be single other)
        List<MessagedUserDto> result = new ArrayList<>();

        for (Conversation conv : convs) {
            // find the "other user" (skip requester). For group chats you can create one record per other participant or treat group differently.
            for (User participant : conv.getParticipants()) {
                if (participant.getId().equals(requesterId)) continue;

                // last message for this conversation (get most recent)
                Page<Message> lastMsgPage = messageRepository.findByConversationIdOrderByCreatedAtAsc(
                        conv.getId(), PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"))
                );
                String preview = null;
                Instant lastAt = conv.getLastMessageAt();
                if (!lastMsgPage.isEmpty()) {
                    Message last = lastMsgPage.getContent().get(0);
                    preview = last.getContent();
                    lastAt = last.getCreatedAt();
                }

                long unread = messageRepository.countByConversationIdAndReadByRecipientFalseAndSenderIdNot(conv.getId(), requesterId);

                // Build DTO
                String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
                String photo = participant.getProfilePhotoUrl();
                if (photo != null && !photo.isBlank() && !photo.toLowerCase().startsWith("http")) {
                    photo = baseUrl + photo;
                }
                MessagedUserDto dto = new MessagedUserDto();
                dto.setUserId(participant.getId());
                dto.setUsername(participant.getUsername());
                dto.setName(participant.getName());
                dto.setProfilePhotoUrl(participant.getProfilePhotoUrl());
                dto.setLastMessageAt(lastAt);
                dto.setLastMessagePreview(preview);
                dto.setUnreadCount(unread);

                result.add(dto);
            }
        }

        // Optional: sort by lastMessageAt desc so most recent chats come first
        result.sort(Comparator.comparing((MessagedUserDto m) -> m.getLastMessageAt(), Comparator.nullsLast(Comparator.reverseOrder())));

        // If you want to dedupe when group/duplicate conversation entries occur for a pair, you can dedupe by userId here.
        Map<Long, MessagedUserDto> deduped = new LinkedHashMap<>();
        for (MessagedUserDto m : result) {
            // keep the first (most recent after the sort)
            deduped.putIfAbsent(m.getUserId(), m);
        }

        return new ArrayList<>(deduped.values());
    }
}
