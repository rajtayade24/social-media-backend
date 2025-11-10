package com.projects.instagram.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {
    private Long id;
    private String username;
    private String name;
    private String profilePhotoUrl;
    private  String bio;
}
