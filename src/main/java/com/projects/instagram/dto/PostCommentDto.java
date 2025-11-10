package com.projects.instagram.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PostCommentDto {
    private Long id;
    private Long postId;
    private Long userId;
    private String comment;
    public OffsetDateTime createdAt;

    private String username;
    private String profilePhotoUrl;

    private Long likes;
    private boolean likedByUser;
}
