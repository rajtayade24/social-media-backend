package com.projects.instagram.controller;

import com.projects.instagram.dto.PostDto;
import com.projects.instagram.dto.PostLikeDto;
import com.projects.instagram.entity.Post;
import com.projects.instagram.repository.UserReposotory;
import com.projects.instagram.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@RestController
@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "http://localhost:5173",
        "http://10.91.2.29:5173",
        "http://10.91.2.29:5173/instagram-clone",
        "https://social-media-frontend-nbdo.vercel.app",
        "*"
})
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserReposotory userReposotory;

    // ----------------------- Helpers -----------------------
    private Long resolveUserId(Long overrideUserId, Authentication authentication) {
        if (overrideUserId != null) return overrideUserId;
        if (authentication == null) {
            return null;
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------- Helper: build URL only if needed ----------
    private String buildFileUrlIfNeeded(String rawFileUrl) {
        if (rawFileUrl == null || rawFileUrl.isBlank()) return null;
        String lower = rawFileUrl.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return rawFileUrl; // already absolute
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/")
                .path(rawFileUrl)
                .toUriString();
    }

    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadPost(@ModelAttribute @Valid PostDto postDto,
                                        BindingResult bindingResult,
                                        HttpServletRequest request) {

        // log.info("Upload hit. Content-Type: {}", request.getContentType());

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getFieldErrors());
        }

        MultipartFile file = postDto.getFile();
        // log.info("Bound file: {}", (file == null ? "null" : file.getOriginalFilename()));
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("file is required");
        }
        String type = postDto.getType();
        if (type == null || !(type.equalsIgnoreCase("POST") || type.equalsIgnoreCase("REEL"))) {
            return ResponseEntity.badRequest().body("type must be POST or REEL");
        }

        Post created = postService.createPost(postDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping(value = "/api/files/{filename:.+}")
    public ResponseEntity<?> serveFile(@PathVariable String filename, HttpServletRequest request) {
        // Basic filename validation to avoid path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Path path = postService.loadPath(filename); // resolves against storage dir
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String contentType = Files.probeContentType(path);
            MediaType mediaType = (contentType != null)
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Malformed URL while serving file {}: {}", filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            log.error("IO error while serving file {}: {}", filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/posts/{overrideCurrentUserId}")
    public ResponseEntity<?> getAllPosts(
            @PathVariable(name = "overrideCurrentUserId", required = false) Long overrideCurrentUserId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication authentication) {

        // prefer explicit override from query param, otherwise resolve from Authentication
        Long currentUserId = resolveUserId(overrideCurrentUserId, authentication);

        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + currentUserId + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostDto> postsPage = postService.getAllPosts(pageable, currentUserId);

        // ensure DTOs expose absolute URLs (idempotent)
        postsPage.forEach(p -> {
            p.setFileUrl(buildFileUrlIfNeeded(p.getFileUrl()));
        });

        return ResponseEntity.ok(postsPage);
    }


    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<Page<PostDto>> getUserPasts(
            @PathVariable Long userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            Authentication authentication) {

        // IMPORTANT: pass null here to resolve the ID from Authentication
        Long currentUserId = resolveUserId(null, authentication);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostDto> postsPage = postService.getUserPosts(userId, pageable, currentUserId);

        // Build full URLs for files and profile photos
        postsPage.forEach(p -> {
            p.setFileUrl(buildFileUrlIfNeeded(p.getFileUrl()));
        });

        return ResponseEntity.ok(postsPage);
    }

    @GetMapping("/users/{userId}/posts/count")
    public ResponseEntity<Long> countPosts(@PathVariable Long userId,
                                           Authentication authentication) {
        Long currentUserId = resolveUserId(userId, authentication);

        return ResponseEntity.ok(postService.countPosts(currentUserId));
    }

    // PostController.java - function only (improved toggleLike)
// - if client doesn't send userId, try to resolve it from Authentication
    @PostMapping("/api/posts/{postId}/like")
    public ResponseEntity<PostLikeDto> toggleLike(@PathVariable Long postId,
                                                  @RequestBody(required = false) Map<String, Long> body,
                                                  Authentication authentication) {
        Long userId = (body != null) ? body.get("userId") : null;

        // If client didn't provide userId, try resolving from Authentication
        if (userId == null) {
            userId = resolveUserId(null, authentication); // reuse your helper
            if (userId == null) {
                // 400 or 401: choose 401 if you want to require auth; 400 if you allow anonymous but need id
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        PostLikeDto dto = postService.toggleLike(postId, userId);

        return ResponseEntity.ok(dto);
    }

    // optional: get likes info
    @GetMapping("/api/posts/{postId}/like")
    public ResponseEntity<PostLikeDto> getLikes(@PathVariable Long postId,
                                                @RequestParam(required = false) Long userId) {
        long count = postService.countLikes(postId);
        boolean liked = (userId != null) && postService.isLikedByUser(postId, userId);
        PostLikeDto dto = new PostLikeDto(postId, count, liked);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/api/posts/{postId}/delete/{userId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @PathVariable(required = false) Long userId,
                                           Authentication authentication) {
        Long currentUserId = resolveUserId(userId, authentication);

        boolean deleted = postService.deletePost(postId, currentUserId);

        if (deleted) {
            return ResponseEntity.noContent().build(); // 204 No Content
        } else {
            return ResponseEntity.notFound().build(); // or 403 Forbidden depending on logic
        }
    }

}


