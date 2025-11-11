package com.projects.instagram.controller;

import com.projects.instagram.dto.FollowDto;
import com.projects.instagram.dto.FollowerDto;
import com.projects.instagram.dto.FollowingDto;
import com.projects.instagram.dto.UserDto;
import com.projects.instagram.exception.UserNotFoundException;
import com.projects.instagram.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "http://localhost:5173",
        "http://10.91.2.29:5173",
        "http://10.91.2.29:5173/instagram-clone",
        "https://social-media-frontend-nbdo.vercel.app",
        "*"
})
public class FollowController {


    private final FollowService followService;

    /**
     * Create a follow relation.
     * Production recommendation: prefer deriving current user from authentication.
     * We allow an optional userId param for admin/testing but by default use auth principal.
     */
    @PostMapping("/follow/{targetId}")
    public ResponseEntity<?> followUser(
            @PathVariable("targetId") Long targetId,
            @RequestParam(value = "userId", required = false) Long userId,
            Authentication authentication
    ) {
        Long currentUserId = resolveUserId(userId, authentication);

        FollowDto created = followService.follow(currentUserId, targetId);

        // return 201 Created with Location header pointing to the follow resource (best-effort)
        // If the DTO contains an id we can use that to point to a hypothetical resource URI.
        HttpHeaders headers = new HttpHeaders();
        if (created.getId() != null) {
            headers.setLocation(URI.create(String.format("/api/follows/%d", created.getId())));
        }
        return new ResponseEntity<>(Map.of("success", true, "follow", created), headers, HttpStatus.CREATED);
    }


    @DeleteMapping("/follow/{targetId}")
    public ResponseEntity<?> unfollow(
            @PathVariable("targetId") Long targetId,
            @RequestParam(value = "userId", required = false) Long userId,
            Authentication authentication
    ) {
        Long currentUserId = resolveUserId(userId, authentication);
        followService.unfollow(currentUserId, targetId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/follow/{targetId}/remove")
    public ResponseEntity<?> remove(
            @PathVariable("targetId") Long targetId,
            @RequestParam(value = "userId", required = false) Long userId,
            Authentication authentication
    ) {
        Long currentUserId = resolveUserId(userId, authentication);


        // remove relation where follower = targetId AND following = currentUserId
        followService.removeFollower(targetId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/followers")
    public ResponseEntity<Page<FollowerDto>> getFollowers(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FollowerDto> followerPage = followService.getFollowers(id, pageable);

        return ResponseEntity.ok(followerPage);
    }

    @GetMapping("/users/{id}/followings")
    public ResponseEntity<Page<FollowingDto>> getFollowing(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FollowingDto> followingPage = followService.getFollowings(id, pageable);

        return ResponseEntity.ok(followingPage);
    }


    @GetMapping("/users/{id}/followers/count")
    public ResponseEntity<Map<String, Long>> getFollowersCount(@PathVariable("id") Long id) {
        return ResponseEntity.ok(Map.of("followers", followService.countFollowers(id)));
    }

    @GetMapping("/users/{id}/following/count")
    public ResponseEntity<Map<String, Long>> getFollowingCount(@PathVariable("id") Long id) {
        return ResponseEntity.ok(Map.of("followings", followService.countFollowing(id)));
    }


    // ---------------- Get Followers (only follower IDs) ----------------
    @GetMapping("/followers/{userId}")
    public ResponseEntity<?> getFollowerIds(@PathVariable("userId") Long userId) {
        List<Long> followerIds = followService.getFollowerIds(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "followers", followerIds
        ));
    }

    // ---------------- Get Following (only following IDs) ----------------
    @GetMapping("/following/{userId}")
    public ResponseEntity<?> getFollowingIds(@PathVariable("userId") Long userId) {
        List<Long> followingIds = followService.getFollowingIds(userId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "following", followingIds
        ));
    }

    @GetMapping("/users/{id}/recommendations")
    public ResponseEntity<List<UserDto>> getRecommendations(
            @PathVariable("id") Long id,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(followService.recommendUsers(id, limit));
    }

    // ----------------------- Helpers -----------------------
    private Long resolveUserId(Long overrideUserId, Authentication authentication) {
        if (overrideUserId != null) return overrideUserId;
        if (authentication == null) {
            throw new IllegalArgumentException("Authenticated user not found. Provide userId or authenticate.");
        }
        // Most JWT setups put user id as principal name or inside details; adapt as needed.
        // Here we attempt to parse authentication.getName() as a numeric id.
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to resolve current user's id from authentication principal. Provide userId param or adapt resolveUserId() to your security setup.");
        }
    }
}

