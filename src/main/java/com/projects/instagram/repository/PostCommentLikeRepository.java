package com.projects.instagram.repository;

import com.projects.instagram.entity.PostCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentLikeRepository extends JpaRepository<PostCommentLike, Long> {
    boolean existsByCommentIdAndPostIdAndUserId(Long commentId, Long postId, Long userId);
    void deleteByCommentIdAndPostIdAndUserId(Long commentId, Long postId, Long userId);

    long countByPostId(Long postId);
    long countByCommentIdAndPostId(Long commentId, Long postId);

    void deleteByPostId(Long postId);

}
