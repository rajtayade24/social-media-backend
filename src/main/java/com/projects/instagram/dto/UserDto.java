package com.projects.instagram.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class UserDto {

    private Long id;
    private String mobileNumber;
    private String name;
    private String username;
    private String email;
    private String userPassword; // will store only the hash
    private String bio;
    private String profilePhotoUrl;

    // NEW: follower / following counts
    private Long followerCount = 0L;
    private Long followingCount = 0L;

    private boolean isFollowing = false;
    private boolean isFollower = false;

    private String role = "ROLE_USER"; // simple single-role example

//    private List<Long> myFollowerIds;
//    private List<Long> myFollowingIds;

    private Set<Long> myFollowerIds = new HashSet<>();
    private Set<Long> myFollowingIds = new HashSet<>();
}
