package com.projects.instagram.repository;

import com.projects.instagram.entity.PostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    Page<PostComment> findByPostId(Long postId, Pageable pageable);
    Optional<PostComment>  findByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
    Page<PostComment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    void deleteByPostId(Long postId);
}
