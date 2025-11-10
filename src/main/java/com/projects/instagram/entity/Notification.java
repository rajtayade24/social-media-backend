package com.projects.instagram.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;


@Entity
//@Table(name = "notifications", indexes = {
//        @Index(name = "idx_notifications_recipient", columnList = "recipient_id"),
//        @Index(name = "idx_notifications_createdat", columnList = "created_at")
//})

@Table(name = "notifications",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_unique", columnNames = {
                "recipient_id", "actor_id", "type", "post_id"
        }))
@NoArgsConstructor
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "recipient_id", nullable = false)
    private Long recipientId; // user who receives


    @Column(name = "actor_id")
    private Long actorId; // user who triggered (nullable for SYSTEM)


    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;


    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();


    @Column(name = "title")
    private String title;


    @Column(name = "message", length = 1000)
    private String message;


    @Column(name = "link")
    private String link; // optional deep-link to client (post url, profile, etc.)


    @Column(name = "metadata", length = 2000)
    private String metadata; // JSON string for structured payload
}