package com.projects.instagram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessagedUserDto {
    private Long id;
    private Long userId;                 // id of the other user (keeps it explicit)
    private String username;
    private String name;
    private String profilePhotoUrl;

    // Messaging-specific fields:
    private Instant lastMessageAt;
    private String lastMessagePreview;
    private Long unreadCount = 0L;
}
