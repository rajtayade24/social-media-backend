//package com.projects.instagram.service;
//
//import com.projects.instagram.dto.FollowDto;
//import com.projects.instagram.dto.UserDto;
//import org.springframework.data.domain.Pageable;
//
//import java.util.List;
//
//public interface FollowService {
//
//    FollowDto follow(Long followerId, Long followingId);
//
//    void unfollow(Long followerId, Long followingId);
//
//    List<FollowDto> getFollowers(Long userId);
//
//    List<FollowDto> getFollowing(Long userId);
//
//    long countFollowers(Long userId);
//
//    long countFollowing(Long userId);
//
//    List<UserDto> recommendUsers(Long userId, int limit);
//}


// -----------------------------------------------------------------------------
// File: FollowService.java
// -----------------------------------------------------------------------------
package com.projects.instagram.service;


import com.projects.instagram.dto.FollowDto;
import com.projects.instagram.dto.FollowerDto;
import com.projects.instagram.dto.FollowingDto;
import com.projects.instagram.dto.UserDto;
import com.projects.instagram.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.parameters.P;


import java.util.List;


public interface FollowService {
    FollowDto follow(Long followerId, Long followingId);

    void unfollow(Long followerId, Long followingId);

    Page<FollowerDto> getFollowers(Long userId, Pageable pageable);
    List<FollowerDto> getAllFollowers(Long userId);

    Page<FollowingDto> getFollowings(Long userId, Pageable pageable);

    long countFollowers(Long userId);

    long countFollowing(Long userId);

    List<UserDto> recommendUsers(Long userId, int limit);

    void removeFollower(Long targetId, Long currentUserId);


    public List<Long> getFollowerIds(Long userId);

    public List<Long> getFollowingIds(Long userId);


}