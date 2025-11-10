package com.projects.instagram.dto;

import com.projects.instagram.entity.NotificationType;
import lombok.Data;

import java.time.Instant;

@Data
public class NotificationDto {
    public Long id;
    public Long recipientId;
    public Long actorId;
    public NotificationType type;
    public Long postId;
    public boolean read;
    public Instant createdAt;
    public String title;
    public String message;
    public String link;
    public String metadata;

    private String username;
    private String name;
    private  String bio;
    private String profilePhotoUrl;

}
