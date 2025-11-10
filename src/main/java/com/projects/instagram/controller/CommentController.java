package com.projects.instagram.controller;

import com.projects.instagram.dto.PostCommentDto;
import com.projects.instagram.dto.PostCommentLikeDto;
import com.projects.instagram.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "http://localhost:5173",
        "http://10.91.2.29:5173",
        "http://10.91.2.29:5173/instagram-clone",
        "*"
})
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/api/posts/{postId}/comment")
    public ResponseEntity<PostCommentDto> getposts(@PathVariable Long postId,
                                                   @RequestBody Map<String, Object> body) {
        Object userIdObj = body.get("userId");
        Object commentObj = body.get("comment");

        if (userIdObj == null || commentObj == null) {
            return ResponseEntity.badRequest().build();
        }

        Long userId = Long.valueOf(userIdObj.toString());
        String comment = commentObj.toString();

        PostCommentDto dto = commentService.uploadComment(postId, userId, comment);
        // log.info("New Comment: {}", dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<Page<PostCommentDto>> getCommentsByPost(
            @PathVariable Long postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostCommentDto> commentsPage = commentService.getCommentsByPost(postId, pageable);

        return ResponseEntity.ok(commentsPage);
    }


    // toggle like: client sends { "userId": 123 } OR get userId from auth principal
    @PostMapping("/api/posts/comments/{commentId}/like")
    public ResponseEntity<PostCommentLikeDto> toggleLike(@PathVariable Long commentId,
                                                         @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        Long postId = body.get("postId");

        if (postId == null || userId == null) {
            return ResponseEntity.badRequest().build();
        }
        PostCommentLikeDto dto = commentService.toggleCommentLike(commentId, postId, userId);
        System.out.println(dto);
        return ResponseEntity.ok(dto);
    }

    // optional: get likes info
    @GetMapping("/api/posts/comments/{commentId}/like")
    public ResponseEntity<PostCommentLikeDto> getCommentLikes(@PathVariable Long commentId,
                                                              @RequestParam(required = false) Long postId,
                                                              @RequestParam(required = false) Long userId) {

        if (commentId == null) return ResponseEntity.badRequest().build();

        long count = commentService.countCommentLikes(commentId, postId);
        boolean liked = (postId != null && userId != null) && commentService.isLikedByUserToComment(commentId, postId, userId);

        PostCommentLikeDto dto = new PostCommentLikeDto(commentId, postId, count, liked);
        return ResponseEntity.ok(dto);
    }

}
