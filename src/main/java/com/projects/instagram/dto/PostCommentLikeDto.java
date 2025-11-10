package com.projects.instagram.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCommentLikeDto {
    private Long commentId;

    private Long postId;

    private Long likes;
    private boolean likedByUser;
}
