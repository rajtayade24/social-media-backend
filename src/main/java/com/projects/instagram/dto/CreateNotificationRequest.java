package com.projects.instagram.dto;


import com.projects.instagram.entity.NotificationType;
import lombok.Data;

@Data
public class CreateNotificationRequest {
    public Long recipientId;
    public Long actorId;
    public NotificationType type;
    public String title;
    public String message;
    public String link;
    public String metadata; // JSON string (optional)
}