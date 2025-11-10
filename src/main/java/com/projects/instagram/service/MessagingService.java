package com.projects.instagram.service;

import com.projects.instagram.dto.ConversationDto;
import com.projects.instagram.dto.MessageDto;
import com.projects.instagram.dto.MessagedUserDto;
import com.projects.instagram.dto.SendMessageRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MessagingService {

    ConversationDto createOrGetDirectConversation(Long requesterId, List<Long> participantIds);

    ConversationDto getConversationDto(Long conversationId, Long requesterId);

    MessageDto sendMessage(Long requesterId, SendMessageRequestDto req);

    Page<MessageDto> getMessages(Long conversationId, Pageable pageable, Long requesterId);

    List<ConversationDto> getConversationsForUser(Long userId);

    List<MessagedUserDto> getMessagedUsers(Long requesterId);

    int markConversationRead(Long conversationId, Long requesterId);

    long countUnreadConversations(Long requesterId);

    long countUnreadMessages(Long requesterId);
}
