package com.projects.instagram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "post_Comments")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "comment", nullable = false)
    private String comment;

    public PostComment(Long postId, Long userId, String comment) {
        this.postId = postId;
        this.userId = userId;
        this.createdAt = OffsetDateTime.now();
        this.comment = comment;
    }

}
