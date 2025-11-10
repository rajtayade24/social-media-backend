package com.projects.instagram.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequestDto {
    // either conversationId or participantIds (for new direct conversation)
    private Long conversationId;

    // If conversationId is null, we will create/get a conversation with these participants
    private List<Long> participantIds;

    @NotBlank(message = "Message content required")
    private String content;
}
