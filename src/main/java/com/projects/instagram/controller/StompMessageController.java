package com.projects.instagram.controller;

import com.projects.instagram.dto.MessageDto;
import com.projects.instagram.dto.SendMessageRequestDto;
import com.projects.instagram.service.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StompMessageController {

    private final MessagingService messageService;

    @MessageMapping("/conversations/send")
    @SendToUser("/queue/messages") // optional personal queue response
    public MessageDto handleStompMessage(@Payload SendMessageRequestDto req,
                                         StompHeaderAccessor sha) {
        // NOTE: retrieving authenticated user id from sha may require custom handshake/auth
        // For now assume that message payload contains senderId or client sent Authorization token and you've set principal earlier
        Long senderId = null;
        try {
            senderId = Long.parseLong(sha.getUser().getName());
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot resolve sender id from stomp principal");
        }
        return messageService.sendMessage(senderId, req);
    }
}
