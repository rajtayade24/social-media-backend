package com.projects.instagram.dto;

import lombok.*;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private boolean readByRecipient;
    private Instant createdAt;
}
