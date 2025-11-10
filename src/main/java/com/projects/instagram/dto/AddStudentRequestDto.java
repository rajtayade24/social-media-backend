package com.projects.instagram.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddStudentRequestDto {

    @NotBlank(message = "name is required!!")
    private String name;

    @NotBlank
    private String identifier; // email or mobile

    @NotBlank(message = "Username is required!!")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can only contain letters, numbers, and underscores")
    @NotBlank
    private String username;


    @NotBlank
    private String userPassword; // will store only the hash

    private String bio = null;

    private MultipartFile profilePhotoUrl;


}



