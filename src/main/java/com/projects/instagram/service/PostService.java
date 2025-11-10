package com.projects.instagram.service;

import com.projects.instagram.dto.FollowerDto;
import com.projects.instagram.dto.PostDto;
import com.projects.instagram.dto.PostLikeDto;
import com.projects.instagram.entity.*;
import com.projects.instagram.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.apache.catalina.AccessLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;

import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;

@Slf4j
@Service
public class PostService {

    private final UserReposotory userReposotory;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository; // new
    private final PostCommentRepository postCommentRepository;
    private final NotificationService notificationService;
    private final FollowService followService;
    private final Path storageDir;

    @Autowired
    public PostService(PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       PostCommentRepository postCommentRepository,
                       NotificationService notificationService,
                       FollowService followService,
                       @Value("${app.storage.dir:users_posts}") String storageDirPath,
                       UserReposotory userReposotory) {
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.postCommentRepository = postCommentRepository;
        this.notificationService = notificationService;
        this.followService = followService;
        this.userReposotory = userReposotory;

        this.storageDir = Paths.get(storageDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory: " + this.storageDir, e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            return null;

        String original = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        original = Paths.get(original).getFileName().toString();

        String extension = "";
        int i = original.lastIndexOf('.');
        if (i > 0)
            extension = original.substring(i);

        String uniqueName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

        Path target = storageDir.resolve(uniqueName).normalize();
        if (!target.toAbsolutePath().startsWith(storageDir.toAbsolutePath())) {
            throw new RuntimeException("Cannot store file outside storage directory");
        }

        try (java.io.InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            // log.info("Stored file to: {}", target.toAbsolutePath());
            return uniqueName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public Path loadPath(String filename) {
        return storageDir.resolve(filename).normalize();
    }

    @Transactional
    public Post createPost(PostDto dto) {
        if (dto == null)
            throw new IllegalArgumentException("PostDto cannot be null");

        Post post = new Post();
        post.setUserId(dto.getUserId());
        post.setCaption(dto.getCaption());
        post.setType(dto.getType());
        post.setContentType(dto.getContentType());

        MultipartFile file = dto.getFile();
        if (file != null && !file.isEmpty()) {
            String storedFilename = storeFile(file);
            post.setFileUrl(storedFilename);
            if (post.getContentType() == null || post.getContentType().isBlank()) {
                post.setContentType(file.getContentType());
            }
        }

        Post created = postRepository.save(post);

        try {
            List<FollowerDto> follows = followService.getAllFollowers(post.getUserId());

            System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n" + follows + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");

            String msg = "Uploaded a new Post: " + getSubString(post.getCaption());
            follows.forEach(f ->
                    notificationService.createNotificationWithTypeIfNeeded(
                            f.getUserId(),
                            post.getUserId(),
                            NotificationType.UPLOAD,
                            msg,
                            post.getId()
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return created;
    }

    public String getSubString(String caption) {
        String postCaption = caption;
        return (postCaption != null && postCaption.length() > 0
                ? (postCaption.length() > 20 ? postCaption.substring(0, 20) : postCaption)
                : ""
        );

    }

    public Post getPost(Long id) {
        return postRepository.findById(id).orElse(null);
    }

    // public Page<PostDto> getAllPosts(Pageable pageable) {
    // Page<Post> postsPage = postRepository.findAllPosts(pageable);
    // String baseUrl =
    // ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    //
    // return postsPage.map(post -> {
    // User user = userReposotory.findById(post.getUserId())
    // .orElse(null); // or throw an exception if not found
    //
    // String fileUrl = null;
    // if (post.getFileUrl() != null && !post.getFileUrl().isBlank()) {
    // fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
    // .path("/api/files/")
    // .path(post.getFileUrl())
    // .toUriString();
    // }
    // PostDto postDto = new PostDto();
    // postDto.setId(post.getId());
    // postDto.setUserId(post.getUserId());
    // postDto.setType(post.getType());
    // postDto.setCaption(post.getCaption());
    // postDto.setContentType(post.getContentType());
    // postDto.setFileUrl(post.getFileUrl());
    // postDto.setCreatedAt(post.getCreatedAt());
    // postDto.setUsername(user != null ? user.getUsername() : null);
    // postDto.setBio(user != null ? user.getBio(): null);
    // postDto.setEmail(user != null ? user.getEmail() : null);
    // postDto.setProfilePhotoUrl(user != null ? baseUrl + user.getProfilePhotoUrl()
    // : null);
    //
    // long likesCount = countLikes(post.getId());
    // postDto.setLikes(likesCount);
    // postDto.setLikedByUser(isLikedByUser(post.getId(), post.getUserId()));
    //
    // return postDto;
    //
    // });

    // PostService.java - function only (fixed)
    // - uses the computed fileUrl variable (was incorrectly setting raw
    // post.getFileUrl())
    // - sets profilePhotoUrl to the raw value (controller will convert to absolute
    // URL)
    // - keeps likedByUser computed with currentUserId
    public Page<PostDto> getAllPosts(Pageable pageable, Long currentUserId) {
        Page<Post> postsPage = postRepository.findAllPosts(pageable);
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();

        return postsPage.map(post -> {
            User user = userReposotory.findById(post.getUserId()).orElse(null);

            // Build absolute file URL if file exists (service-level convenience)
            String fileUrl = null;
            if (post.getFileUrl() != null && !post.getFileUrl().isBlank()) {
                fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/files/")
                        .path(post.getFileUrl())
                        .toUriString();
            }

            PostDto postDto = new PostDto();
            postDto.setId(post.getId());
            postDto.setUserId(post.getUserId());
            postDto.setType(post.getType());
            postDto.setCaption(post.getCaption());
            postDto.setContentType(post.getContentType());

            postDto.setFileUrl(fileUrl);

            postDto.setCreatedAt(post.getCreatedAt());

            if (user != null) {
                postDto.setUsername(user.getUsername());
                postDto.setBio(user.getBio());
                postDto.setName(user.getName());
                postDto.setProfilePhotoUrl(baseUrl + user.getProfilePhotoUrl());
            }

            long likesCount = countLikes(post.getId());
            postDto.setLikes(likesCount);

            boolean likedByCurrent = currentUserId != null && isLikedByUser(post.getId(), currentUserId);
            postDto.setLikedByUser(likedByCurrent);

            return postDto;
        });
    }

    // public Page<PostDto> getPostsForUser(Long userId, Pageable pageable) {
    // Page<Post> postsPage =
    // postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    // String baseUrl =
    // ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    //
    // return postsPage.map(post -> {
    //
    // String fileUrl = null;
    // if (post.getFileUrl() != null && !post.getFileUrl().isBlank()) {
    // // Always assume DB has only filename
    // fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
    // .path("/api/files/")
    // .path(post.getFileUrl())
    // .toUriString();
    // }
    //
    //
    // PostDto postDto = new PostDto();
    //
    // postDto.setId(post.getId());
    // postDto.setUserId(post.getUserId());
    // postDto.setType(post.getType());
    // postDto.setCaption(post.getCaption());
    // postDto.setContentType(post.getContentType());
    // postDto.setFileUrl(fileUrl); // corrected (was raw earlier)
    // postDto.setCreatedAt(post.getCreatedAt());
    //
    // User user = userReposotory.findById(post.getUserId()).orElse(null);
    // postDto.setUsername(user != null ? user.getUsername() : null);
    // postDto.setProfilePhotoUrl(user != null ? user.getProfilePhotoUrl() : null);
    //
    // long likesCount = countLikes(post.getId());
    // postDto.setLikes(likesCount);
    // postDto.setLikedByUser(isLikedByUser(post.getId(), post.getUserId()));
    //
    // return postDto;
    // });
    // }
    //
    // // in PostService

    public Page<PostDto> getUserPosts(Long userId, Pageable pageable, Long currentUserId) {
        Page<Post> postsPage = postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return postsPage.map(post -> {
            PostDto postDto = new PostDto();
            postDto.setId(post.getId());
            postDto.setUserId(post.getUserId());
            postDto.setType(post.getType());
            postDto.setCaption(post.getCaption());
            postDto.setContentType(post.getContentType());
            postDto.setFileUrl(post.getFileUrl()); // raw filename for now
            postDto.setCreatedAt(post.getCreatedAt());

            User user = userReposotory.findById(post.getUserId()).orElse(null);
            if (user != null) {
                postDto.setUsername(user.getUsername());
                postDto.setName(user.getName());
                postDto.setBio(user.getBio());
                postDto.setProfilePhotoUrl(user.getProfilePhotoUrl());
            }

            long likesCount = countLikes(post.getId()); // consider batch loading
            postDto.setLikes(likesCount);

            boolean liked = currentUserId != null && isLikedByUser(post.getId(), currentUserId);
            postDto.setLikedByUser(liked);

            return postDto;
        });
    }

    // PostService.java
    // public Page<PostDto> getPostsForUser(Long userId, Pageable pageable, Long
    // currentUserId) {
    // Page<Post> postsPage =
    // postRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    // List<Post> posts = postsPage.getContent();
    // if (posts.isEmpty()) {
    // return postsPage.map(p -> null); // empty page -> map -> empty DTO page
    // }
    //
    // // 1) Batch load users
    // Set<Long> userIds =
    // posts.stream().map(Post::getUserId).collect(Collectors.toSet());
    // Map<Long, User> usersById = userReposotory.findAllById(userIds)
    // .stream().collect(Collectors.toMap(User::getId, Function.identity()));
    //
    // // 2) Get post ids
    // List<Long> postIds =
    // posts.stream().map(Post::getId).collect(Collectors.toList());
    //
    // // 3) Batch load likes count (custom repo method)
    // Map<Long, Long> likesCountMap =
    // postLikeRepository.countLikesByPostIds(postIds); // implement below
    //
    // // 4) Batch load which posts are liked by currentUser
    // Set<Long> likedByUserPostIds = (currentUserId != null)
    // ? new HashSet<>(postLikeRepository.findPostIdsLikedByUser(currentUserId,
    // postIds))
    // : Collections.emptySet();
    //
    // // 5) Map posts -> PostDto using the maps (no per-post DB calls)
    // List<PostDto> dtos = posts.stream().map(post -> {
    // PostDto dto = new PostDto();
    // dto.setId(post.getId());
    // dto.setUserId(post.getUserId());
    // dto.setType(post.getType());
    // dto.setCaption(post.getCaption());
    // dto.setContentType(post.getContentType());
    // dto.setFileUrl(post.getFileUrl());
    // dto.setCreatedAt(post.getCreatedAt());
    //
    // User user = usersById.get(post.getUserId());
    // if (user != null) {
    // dto.setUsername(user.getUsername());
    // dto.setName(user.getName());
    // dto.setBio(user.getBio());
    // dto.setProfilePhotoUrl(user.getProfilePhotoUrl());
    // }
    //
    // dto.setLikes(likesCountMap.getOrDefault(post.getId(), 0L));
    // dto.setLikedByUser(likedByUserPostIds.contains(post.getId()));
    // return dto;
    // }).collect(Collectors.toList());
    //
    // // Return Page<PostDto> preserving original pagination meta
    // return new PageImpl<>(dtos, postsPage.getPageable(),
    // postsPage.getTotalElements());
    // }

    // --- new toggle like method ---
    @Transactional
    public PostLikeDto toggleLike(Long postId, Long userId) {
        if (postId == null || userId == null) {
            throw new IllegalArgumentException("postId and userId are required");
        }

        // (optional) verify post exists
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));

        boolean nowLiked;
        // check if user already liked
        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            // remove like
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            nowLiked = false;

            notificationService.deleteNotificationWithTypeIfNeeded(
                    post.getUserId(), userId, NotificationType.LIKE, post.getId()
            );

        } else {
            // add like
            PostLike like = new PostLike(postId, userId);
            try {
                postLikeRepository.save(like);
                nowLiked = true;

                String msg = "Liked your Post: " + getSubString(post.getCaption());
                notificationService.createNotificationWithTypeIfNeeded(
                        post.getUserId(), userId, NotificationType.LIKE, msg, postId
                );

            } catch (DataIntegrityViolationException ex) {
                // liked
                nowLiked = true;
            }
        }

        long likesCount = postLikeRepository.countByPostId(postId);
        return new PostLikeDto(postId, likesCount, nowLiked);
    }

    // optional helper if you want just counts / status:
    public long countLikes(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }

    public boolean isLikedByUser(Long postId, Long userId) {
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }

    @org.springframework.transaction.annotation.Transactional
    public boolean deletePost(Long postId, Long userId) {
        if (postId == null || userId == null) {
            throw new IllegalArgumentException("postId and userId are required");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to delete this post");
        }

        postLikeRepository.deleteByPostId(postId);
        postCommentRepository.deleteByPostId(postId);
        postRepository.delete(post); // deletes post + likes + comments automatically

        List<Long> ids = notificationService.getAllNotificationsByPostId(postId);

        ids.forEach(recipientId -> {
            notificationService.deleteNotificationWithTypeIfNeeded(recipientId, userId, NotificationType.UPLOAD, postId);
        });

        return true;
    }

    public Long countPosts(Long currentUserId) {
        return postRepository.countAllByUserId(currentUserId);
    }
}
