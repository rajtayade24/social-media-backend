package com.projects.instagram.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    // senderId kept as long (denormalized for cheaper reads)
    private Long senderId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private boolean readByRecipient = false;

    private Instant createdAt = Instant.now();
}
