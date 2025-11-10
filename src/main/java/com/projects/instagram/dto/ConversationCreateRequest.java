package com.projects.instagram.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor

@AllArgsConstructor
public class ConversationCreateRequest {
    // participant ids excluding the requester (requester will be added server-side)
    private List<Long> participantIds;
}
