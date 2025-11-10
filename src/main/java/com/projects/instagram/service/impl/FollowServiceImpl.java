//package com.projects.instagram.service.impl;
//
//import com.projects.instagram.dto.FollowDto;
//import com.projects.instagram.dto.UserDto;
//import com.projects.instagram.entity.Follow;
//import com.projects.instagram.entity.User;
//import com.projects.instagram.exception.UserNotFoundException;
//import com.projects.instagram.repository.FollowRepository;
//import com.projects.instagram.repository.UserReposotory;
//import com.projects.instagram.service.FollowService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.modelmapper.ModelMapper;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
//
//import java.time.OffsetDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class FollowServiceImpl implements FollowService {
//
//    private final FollowRepository followRepository;
//    private final UserReposotory userReposotory;
//    private final ModelMapper modelMapper;
//
//    // ---------- Helper: build URL only if needed ----------
//    private String buildFileUrlIfNeeded(String rawFileUrl) {
//        if (rawFileUrl == null || rawFileUrl.isBlank()) return null;
//        String lower = rawFileUrl.toLowerCase();
//        if (lower.startsWith("http://") || lower.startsWith("https://")) {
//            return rawFileUrl; // already absolute
//        }
//        return ServletUriComponentsBuilder.fromCurrentContextPath()
//                .path("/api/files/")
//                .path(rawFileUrl)
//                .toUriString();
//    }
//
//    private FollowDto mapFollowToDto(Follow follow) {
//        FollowDto dto = new FollowDto();
//        dto.setId(follow.getId());
//        dto.setCreatedAt(follow.getCreatedAt());
//
//        User follower = follow.getFollower();
//        if (follower != null) {
//            dto.setFollowerId(follower.getId());
//            dto.setFollowerUsername(follower.getUsername());
//            dto.setFollowerProfilePhotoUrl(buildFileUrlIfNeeded(follower.getProfilePhotoUrl()));
//        }
//
//        User following = follow.getFollowing();
//        if (following != null) {
//            dto.setFollowingId(following.getId());
//            dto.setFollowingUsername(following.getUsername());
//            dto.setFollowingProfilePhotoUrl(buildFileUrlIfNeeded(following.getProfilePhotoUrl()));
//        }
//        return dto;
//    }
//
//    private UserDto mapUserToDto(User user) {
//        UserDto dto = modelMapper.map(user, UserDto.class);
//        if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isBlank()) {
//            dto.setProfilePhotoUrl(buildFileUrlIfNeeded(user.getProfilePhotoUrl()));
//        }
//        return dto;
//    }
//
//    @Override
//    @Transactional
//    public FollowDto follow(Long followerId, Long followingId) {
//        if (Objects.equals(followerId, followingId)) {
//            throw new IllegalArgumentException("User cannot follow themself");
//        }
//
//        User follower = userReposotory.findById(followerId)
//                .orElseThrow(() -> new UserNotFoundException("Follower user not found: " + followerId));
//        User following = userReposotory.findById(followingId)
//                .orElseThrow(() -> new UserNotFoundException("Following user not found: " + followingId));
//
//        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
//            // already following - return existing record as DTO
//            return followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
//                    .map(this::mapFollowToDto)
//                    .orElseThrow(() -> new IllegalStateException("Follow relation exists but not found"));
//        }
//
//        Follow f = Follow.builder()
//                .follower(follower)
//                .following(following)
//                .createdAt(OffsetDateTime.now())
//                .build();
//        Follow saved = followRepository.save(f);
//        return mapFollowToDto(saved);
//    }
//
//    @Override
//    @Transactional
//    public void unfollow(Long followerId, Long followingId) {
//        followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
//                .ifPresent(followRepository::delete);
//    }
//
//    @Override
//    public List<FollowDto> getFollowers(Long userId) {
//        List<Follow> records = followRepository.findByFollowingId(userId);
//        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"+records+"\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
//
//        return records.stream().map(this::mapFollowToDto).collect(Collectors.toList());
//    }
//
//    @Override
//    public List<FollowDto> getFollowing(Long userId) {
//        List<Follow> records = followRepository.findByFollowerId(userId);
//        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"+records+"\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
//        return records.stream().map(this::mapFollowToDto).collect(Collectors.toList());
//    }
//
//    @Override
//    public long countFollowers(Long userId) {
//        return followRepository.countByFollowingId(userId);
//    }
//
//    @Override
//    public long countFollowing(Long userId) {
//        return followRepository.countByFollowerId(userId);
//    }
//
//    /**
//     * Recommend users using friends-of-friends and fallback to popularity.
//     */
//    @Override
//    public List<UserDto> recommendUsers(Long userId, int limit) {
//        // who I follow
//        List<Follow> myFollowings = followRepository.findByFollowerId(userId);
//        Set<Long> excluded = myFollowings.stream().map(f -> f.getFollowing().getId()).collect(Collectors.toSet());
//        excluded.add(userId);
//
//        if (myFollowings.isEmpty()) {
//            // fallback to popular users
//            List<Follow> allFollows = followRepository.findAll();
//            Map<Long, Long> popularity = allFollows.stream().collect(Collectors.groupingBy(f -> f.getFollowing().getId(), Collectors.counting()));
//            return popularity.entrySet().stream()
//                    .filter(e -> !excluded.contains(e.getKey()))
//                    .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
//                    .limit(limit)
//                    .map(e -> userReposotory.findById(e.getKey()).map(this::mapUserToDto).orElse(null))
//                    .filter(Objects::nonNull)
//                    .collect(Collectors.toList());
//        }
//
//        // gather followings-of-my-followings
//        List<Long> myFollowingIds = myFollowings.stream().map(f -> f.getFollowing().getId()).collect(Collectors.toList());
//        List<Follow> foaf = followRepository.findByFollowerIdIn(myFollowingIds); // who my followings follow
//
//        Map<Long, Long> counts = new HashMap<>();
//        for (Follow f : foaf) {
//            Long candidate = f.getFollowing().getId();
//            if (excluded.contains(candidate)) continue;
//            counts.put(candidate, counts.getOrDefault(candidate, 0L) + 1L);
//        }
//
//        List<Long> recommendedIds = counts.entrySet().stream()
//                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
//                .limit(limit)
//                .map(Map.Entry::getKey)
//                .collect(Collectors.toList());
//
//        List<UserDto> result = recommendedIds.stream()
//                .map(id -> userReposotory.findById(id).map(this::mapUserToDto).orElse(null))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//
//        if (result.size() < limit) {
//            // fill with popular users not already in result
//            Set<Long> already = result.stream().map(UserDto::getId).collect(Collectors.toSet());
//            List<Follow> allFollows = followRepository.findAll();
//            Map<Long, Long> popularity = allFollows.stream().collect(Collectors.groupingBy(f -> f.getFollowing().getId(), Collectors.counting()));
//            List<UserDto> fallback = popularity.entrySet().stream()
//                    .filter(e -> !excluded.contains(e.getKey()))
//                    .filter(e -> !already.contains(e.getKey()))
//                    .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
//                    .map(e -> userReposotory.findById(e.getKey()).map(this::mapUserToDto).orElse(null))
//                    .filter(Objects::nonNull)
//                    .limit(limit - result.size())
//                    .collect(Collectors.toList());
//            result.addAll(fallback);
//        }
//
//        return result;
//    }
//}


// -----------------------------------------------------------------------------
// File: FollowServiceImpl.java
// -----------------------------------------------------------------------------
package com.projects.instagram.service.impl;


import com.projects.instagram.dto.FollowDto;
import com.projects.instagram.dto.FollowerDto;
import com.projects.instagram.dto.FollowingDto;
import com.projects.instagram.dto.UserDto;
import com.projects.instagram.entity.Follow;
import com.projects.instagram.entity.NotificationType;
import com.projects.instagram.entity.User;
import com.projects.instagram.exception.UserNotFoundException;
import com.projects.instagram.repository.FollowRepository;
import com.projects.instagram.repository.UserReposotory;
import com.projects.instagram.service.FollowService;
import com.projects.instagram.service.FileUrlService;
import com.projects.instagram.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Data
@Slf4j
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserReposotory userReposotory;
    private final ModelMapper modelMapper;
    private final FileUrlService fileUrlService;
    private final NotificationService notificationService;


    // ---------------- Helper mappers ----------------
    private FollowDto mapFollowToDto(Follow follow) {
        FollowDto dto = new FollowDto();
        dto.setId(follow.getId());
        dto.setCreatedAt(follow.getCreatedAt());


        User follower = follow.getFollower();
        if (follower != null) {
            dto.setFollowerId(follower.getId());
            dto.setFollowerUsername(follower.getUsername());
            dto.setFollowerName(follower.getName());
            dto.setFollowerProfilePhotoUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + follower.getProfilePhotoUrl());
        }


        User following = follow.getFollowing();
        if (following != null) {
            dto.setFollowingId(following.getId());
            dto.setFollowingUsername(following.getUsername());
            dto.setFollowingName(following.getName());
            dto.setFollowingProfilePhotoUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + following.getProfilePhotoUrl());
        }
        return dto;
    }

    private UserDto mapUserToDto(User user) {
        UserDto dto = modelMapper.map(user, UserDto.class);
        dto.setProfilePhotoUrl(ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString() + user.getProfilePhotoUrl());
        return dto;
    }


    // ---------------- Business methods ----------------
    @Override
    @Transactional
    public FollowDto follow(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new IllegalArgumentException("followerId and followingId are required");
        }
        if (Objects.equals(followerId, followingId)) {
            throw new IllegalArgumentException("User cannot follow themself");
        }

        User follower = userReposotory.findById(followerId)
                .orElseThrow(() -> new UserNotFoundException("Follower user not found: " + followerId));
        User following = userReposotory.findById(followingId)
                .orElseThrow(() -> new UserNotFoundException("Following user not found: " + followingId));

// if exists, return existing DTO (avoid unique constraint failure)
        followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .ifPresent(f -> {
                    throw new IllegalArgumentException("Already following");
                });

        Follow f = Follow.builder()
                .follower(follower)
                .following(following)
                .createdAt(OffsetDateTime.now())
                .build();

        try {
            Follow saved = followRepository.save(f);

            String msg = "Started following you";
            notificationService.createNotificationWithTypeIfNeeded(followingId, followerId, NotificationType.FOLLOW, msg);

            return mapFollowToDto(saved);
        } catch (DataIntegrityViolationException ex) {
// Race condition: another insert happened concurrently; return existing relation
            log.warn("Data integrity violation while saving follow, checking existing relation", ex);
            return followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                    .map(this::mapFollowToDto)
                    .orElseThrow(() -> new IllegalStateException("Could not create or find follow relation"));
        }
    }


    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new IllegalArgumentException("followerId and followingId are required");
        }
        try {
            followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                    .ifPresent(followRepository::delete);

            notificationService.deleteNotificationWithTypeIfNeeded(followingId, followerId, NotificationType.FOLLOW);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    @Transactional
    public void removeFollower(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new IllegalArgumentException("followerId and followingId are required");
        }

        // delete where follower = followerIdToRemove AND following = myId
        followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .ifPresent(followRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowingDto> getFollowings(Long userId, Pageable pageable) {
        Page<Follow> followPage = followRepository.findByFollowerId(userId, pageable);
        // Use Page.map to preserve paging metadata
        return followPage.map(f -> {
            // map the followed user (the "following" field) into FollowingDto
            FollowingDto dto = new FollowingDto();
            dto.setId(f.getId());
            dto.setUserId(f.getFollowing().getId());
            dto.setUsername(f.getFollowing().getUsername());
            dto.setName(f.getFollowing().getName());
            dto.setProfilePhotoUrl(f.getFollowing().getProfilePhotoUrl());
            dto.setCreatedAt(f.getCreatedAt());
            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowerDto> getFollowers(Long userId, Pageable pageable) {

        Page<Follow> followPage = followRepository.findByFollowingId(userId, pageable);

        return followPage.map(f -> {
            FollowerDto dto = new FollowerDto();
            dto.setId(f.getId());
            dto.setUserId(f.getFollower().getId());
            dto.setUsername(f.getFollower().getUsername());
            dto.setName(f.getFollower().getName());
            dto.setProfilePhotoUrl(f.getFollower().getProfilePhotoUrl());
            dto.setCreatedAt(f.getCreatedAt());

            return dto;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<FollowerDto> getAllFollowers(Long userId) {

        List<Follow> followPage = followRepository.findAllByFollowingId(userId);

        return followPage.stream().map(f -> {
            FollowerDto dto = new FollowerDto();
            dto.setId(f.getId());
            dto.setUserId(f.getFollower().getId());
            dto.setUsername(f.getFollower().getUsername());
            dto.setName(f.getFollower().getName());
            dto.setProfilePhotoUrl(f.getFollower().getProfilePhotoUrl());
            dto.setCreatedAt(f.getCreatedAt());

            return dto;
        }).toList();
    }


//    private FollowerDto mapFollowToFollowerDto(Follow follow) {
//        User followerUser = follow.getFollower(); // assuming you have getFollower() relation
//        return new FollowerDto(
//                follow.getId(),
//                followerUser.getId(),
//                followerUser.getUsername(),
//                followerUser.getName(),
//                followerUser.getProfilePhotoUrl(),
//                follow.getCreatedAt()
//        );
//    }

//    private FollowingDto mapFollowToFollowingDto(Follow follow) {
//        User followingUser = follow.getFollowing(); // assuming you have a getFollowing() relation
//        return new FollowingDto(
//                follow.getId(),
//                followingUser.getId(),
//                followingUser.getUsername(),
//                followingUser.getName(),
//                followingUser.getProfilePhotoUrl(),
//                follow.getCreatedAt()
//        );
//    }


    @Override
    @Transactional(readOnly = true)
    public long countFollowers(Long userId) {
        return followRepository.countByFollowingId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFollowing(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFollowerIds(Long userId) {
        return followRepository.findFollowerIdsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFollowingIds(Long userId) {
        return followRepository.findFollowingIdsByUserId(userId);
    }


    @Override
    @Transactional(readOnly = true)
    public List<UserDto> recommendUsers(Long userId, int limit) {
// friends-of-friends strategy with DB-friendly fallbacks
        List<Follow> myFollowings = followRepository.findByFollowerId(userId);
        Set<Long> excluded = myFollowings.stream()
                .map(f -> f.getFollowing().getId())
                .collect(Collectors.toSet());
        excluded.add(userId);


        if (myFollowings.isEmpty()) {
// fallback to popularity using DB query
            return followRepository.findPopularFollowingIds(org.springframework.data.domain.PageRequest.of(0, limit))
                    .stream()
                    .map(pair -> (Long) pair[0])
                    .filter(id -> !excluded.contains(id))
                    .map(id -> userReposotory.findById(id).map(this::mapUserToDto).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        List<Long> myFollowingIds = myFollowings.stream().map(f -> f.getFollowing().getId()).collect(Collectors.toList());
        List<Follow> foaf = followRepository.findByFollowerIdIn(myFollowingIds);


        Map<Long, Long> counts = new HashMap<>();
        for (Follow f : foaf) {
            Long candidate = f.getFollowing().getId();
            if (excluded.contains(candidate)) continue;
            counts.put(candidate, counts.getOrDefault(candidate, 0L) + 1L);
        }


        List<Long> recommendedIds = counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();


        List<UserDto> result = recommendedIds.stream()
                .map(id -> userReposotory.findById(id).map(this::mapUserToDto).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());


        if (result.size() < limit) {
            Set<Long> already = result.stream().map(UserDto::getId).collect(Collectors.toSet());
            followRepository.findPopularFollowingIds(org.springframework.data.domain.PageRequest.of(0, limit))
                    .stream()
                    .map(pair -> (Long) pair[0])
                    .filter(id -> !excluded.contains(id) && !already.contains(id))
                    .map(id -> userReposotory.findById(id).map(this::mapUserToDto).orElse(null))
                    .filter(Objects::nonNull)
                    .limit(limit - result.size())
                    .forEach(result::add);
        }

        return result;
    }
}