package com.projects.instagram.service;

import com.projects.instagram.dto.PostCommentDto;
import com.projects.instagram.dto.PostCommentLikeDto;
import com.projects.instagram.entity.PostComment;
import com.projects.instagram.entity.PostCommentLike;
import com.projects.instagram.entity.User;
import com.projects.instagram.repository.PostCommentLikeRepository;
import com.projects.instagram.repository.PostCommentRepository;
import com.projects.instagram.repository.UserReposotory;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final UserReposotory userReposotory;
    private final PostCommentRepository postCommentRepository;
    private final PostCommentLikeRepository postCommentLikeRepository;

    public PostCommentDto uploadComment(Long postId, Long userId, String commentText) {
        PostComment comment = new PostComment(postId, userId, commentText);
        PostComment saved = postCommentRepository.save(comment); // saved now has the generated id

        PostCommentDto dto = new PostCommentDto();
        dto.setId(saved.getId());
        dto.setPostId(saved.getPostId());
        dto.setUserId(saved.getUserId());
        dto.setComment(saved.getComment());
        dto.setCreatedAt(saved.getCreatedAt());      // use persisted timestamp

        User user = userReposotory.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not found"));

        dto.setUsername(user.getUsername());

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        dto.setProfilePhotoUrl(baseUrl + user.getProfilePhotoUrl());

        return dto;
    }

    public Page<PostCommentDto> getCommentsByPost(Long postId, Pageable pageable) {
        Page<PostComment> comments = postCommentRepository.findByPostId(postId, pageable);

        return comments.map(comment -> {
            PostCommentDto dto = new PostCommentDto();
            dto.setId((comment.getId()));
            dto.setPostId(comment.getPostId());
            dto.setUserId(comment.getUserId());
            dto.setComment(comment.getComment());
            dto.setCreatedAt(comment.getCreatedAt());

            long likesCount = countCommentLikes(comment.getId(), postId);
            dto.setLikes(likesCount);
            dto.setLikedByUser(isLikedByUserToComment(comment.getId(), postId, comment.getUserId()));


            User user = userReposotory.findById(comment.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            dto.setUsername(user.getUsername());

            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            dto.setProfilePhotoUrl(baseUrl + user.getProfilePhotoUrl());

            return dto;
        });
    }

    @Transactional
    public PostCommentLikeDto toggleCommentLike(Long commentId, Long postId, Long userId) {
        if (commentId == null || postId == null || userId == null) {
            throw new IllegalArgumentException("commentId, postId and userId are required");
        }

        postCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with id: " + commentId));

        boolean nowLiked;
        if (postCommentLikeRepository.existsByCommentIdAndPostIdAndUserId(commentId, postId, userId)) {
            postCommentLikeRepository.deleteByCommentIdAndPostIdAndUserId(commentId, postId, userId);
            nowLiked = false;
        } else {

            PostCommentLike like = new PostCommentLike(commentId, postId, userId);
            try {
                postCommentLikeRepository.save(like);
                nowLiked = true;
            } catch (DataIntegrityViolationException ex) {
                nowLiked = true; // concurrent insert -> treat as liked
            }
        }
        long likeCount = postCommentLikeRepository.countByCommentIdAndPostId(commentId, postId);
        return new PostCommentLikeDto(commentId, postId, likeCount, nowLiked);
    }

    public long countCommentLikes(Long commentId, Long postId) {
        if (commentId == null) return 0L;
        if (postId != null) {
            return postCommentLikeRepository.countByCommentIdAndPostId(commentId, postId);
        }
        return postCommentLikeRepository.countByPostId(postId);
    }

    public boolean isLikedByUserToComment(Long commentId, Long postId, Long userId) {
        if (commentId == null || postId == null || userId == null) return false;
        return postCommentLikeRepository.existsByCommentIdAndPostIdAndUserId(commentId, postId, userId);
    }
}