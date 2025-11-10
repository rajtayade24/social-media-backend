package com.projects.instagram.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {
    private Long id;
    private List<UserSummaryDto> participants;
    private Instant lastMessageAt;
    private String lastMessagePreview; // optional
    private Long unreadCount; // relative to requesting user
}
