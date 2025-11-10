package com.projects.instagram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "post_comments_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "comment_id", "user_id"})
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostCommentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public PostCommentLike( Long commentId, Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
        this.commentId= commentId;
        this.createdAt = OffsetDateTime.now();
    }

}
