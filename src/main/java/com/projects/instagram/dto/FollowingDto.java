package com.projects.instagram.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowingDto {
    private Long id;

    private Long userId;
    private String username;
    private String name;
    private String profilePhotoUrl;

    private OffsetDateTime createdAt;
}