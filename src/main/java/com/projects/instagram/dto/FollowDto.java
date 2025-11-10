//package com.projects.instagram.dto;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.time.OffsetDateTime;
//
///**
// * DTO used to transfer follow relation information between server and client.
// */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class FollowDto {
//    private Long id;
//
//    // follower info
//    private Long followerId;
//    private String followerUsername;
//    private String followerProfilePhotoUrl;
//
//    // following info
//    private Long followingId;
//    private String followingUsername;
//    private String followingProfilePhotoUrl;
//
//    private OffsetDateTime createdAt;
//}

// -----------------------------------------------------------------------------
// File: FollowDto.java
// -----------------------------------------------------------------------------
package com.projects.instagram.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.OffsetDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowDto {
    private Long id;


    // follower info
    private Long followerId;
    private String followerUsername;
    private String followerName;
    private String followerProfilePhotoUrl;


    // following info
    private Long followingId;
    private String followingUsername;
    private String followingName;
    private String followingProfilePhotoUrl;


    private OffsetDateTime createdAt;
}