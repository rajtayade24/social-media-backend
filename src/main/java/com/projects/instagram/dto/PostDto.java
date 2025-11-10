package com.projects.instagram.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {

    private Long id;

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "type is required") // "POST" or "REEL"
    private String type;

    private String caption;

    // optional: client can declare contentType, otherwise server will detect it
    private String contentType;
    private OffsetDateTime createdAt;
    // actual uploaded file
    private MultipartFile file;

    private String fileUrl;


    private String username;
    private String name;
    private  String bio;
    private String profilePhotoUrl;


    private Long likes;
    private boolean likedByUser;

    // 👇 Add this constructor for convenience
    public PostDto(Long id, Long userId, String type, String caption, String contentType,
                   OffsetDateTime createdAt, String fileUrl, String username, String name, String bio, String profilePhotoUrl,
                   Long likes, Boolean likedByUser
    ) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.caption = caption;
        this.contentType = contentType;
        this.createdAt = createdAt;
        this.fileUrl = fileUrl;
        this.username = username;
        this.name=name;
        this.bio = bio;
        this.profilePhotoUrl = profilePhotoUrl;
        this.likes = likes;
        this.likedByUser = likedByUser;
    }


    public String getBio() {
        return bio;
    }
}
