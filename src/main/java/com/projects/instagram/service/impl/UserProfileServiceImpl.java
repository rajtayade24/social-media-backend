package com.projects.instagram.service.impl;

import com.projects.instagram.dto.AddStudentRequestDto;
import com.projects.instagram.dto.PostDto;
import com.projects.instagram.dto.UpdateUserDto;
import com.projects.instagram.dto.UserDto;
import com.projects.instagram.entity.Post;
import com.projects.instagram.entity.User;
import com.projects.instagram.exception.UserNotFoundException;
import com.projects.instagram.repository.FollowRepository;
import com.projects.instagram.repository.PostRepository;
import com.projects.instagram.repository.UserReposotory;
import com.projects.instagram.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserReposotory userReposotory;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final PostRepository postRepository;
    private final FollowRepository followRepository;

    @Override
    public List<UserDto> getAllUsers(Long currentUserId) {
        return userReposotory.findAll().stream().map(user -> {
            UserDto dto = new UserDto();

            dto.setId(user.getId());
            dto.setName(user.getName());
            dto.setUsername(user.getUsername());
            dto.setBio(user.getBio());
            dto.setEmail(user.getEmail());
            dto.setBio(user.getBio());
            dto.setMobileNumber(user.getMobileNumber());

            dto.setFollowerCount(followRepository.countByFollowingId(user.getId()));
            dto.setFollowingCount(followRepository.countByFollowerId(user.getId()));

            boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(currentUserId, user.getId());
            dto.setFollowing(isFollowing);

            boolean isFollower = followRepository.existsByFollowerIdAndFollowingId(user.getId(), currentUserId);
            dto.setFollower(isFollower);

            String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
            dto.setProfilePhotoUrl(baseUrl + user.getProfilePhotoUrl()); // because of this file we not use modelmapper // @Override public List<UserDto> getAllUsers() { List<User> users = userReposotory.findAll();return users.stream().map(user -> modelMapper.map(user, UserDto.class)).toList;}


            return dto;
        }).toList();
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userReposotory.findById(id).orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        return modelMapper.map(user, UserDto.class); // instance of the ModelMapper library, used to convert objects from user type to userDto.
    }

    @Value("${upload.dir}")
    private String uploadDir;

    @Override
    public UserDto createNewUser(AddStudentRequestDto dto) {

        // check username uniqueness
        if (dto.getUsername() != null && userReposotory.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("username already taken");
        }

        // if identifier contains @ -> email, else consider it mobile number
        if (dto.getIdentifier().contains("@")) {
            if (userReposotory.existsByEmail(dto.getIdentifier())) {
                throw new IllegalArgumentException("Email already taken");
            }
        }
//        else {
//            if (userReposotory.existsByMobileNumber(dto.getIdentifier())) {
//                throw new IllegalArgumentException("Mobile number already taken");
//            }
//        }
        User user = new User();

        if (dto.getIdentifier().contains("@")) {
            user.setEmail(dto.getIdentifier());
            user.setEmailVerified(true);
            user.setMobileVerified(false);
        } else {
            user.setMobileNumber(dto.getIdentifier());
            user.setMobileVerified(true);
            user.setEmailVerified(false);
        }
        user.setName(dto.getName());
        user.setUsername(dto.getUsername());
        if (dto.getUserPassword() != null && !dto.getUserPassword().isBlank()) {
            user.setUserPassword(passwordEncoder.encode(dto.getUserPassword()));
        }
        user.setBio(dto.getBio());
        user.setRole("ROLE_USER");

        MultipartFile profilePhotoFile = dto.getProfilePhotoUrl(); // obj ( Spring’s type )
        if (profilePhotoFile != null && !profilePhotoFile.isEmpty()) {
            try {

                // Create directory if not exists
                Path uploadPath = Paths.get(uploadDir, "user_profiles");
                Files.createDirectories(uploadPath);

                // Sanitize original filename and build a unique filename
                String original = Paths.get(profilePhotoFile.getOriginalFilename()).getFileName().toString();
                String extension = "";

                int i = original.lastIndexOf('.');
                if (i > 0) extension = original.substring(i);
                //                    Current timestamp                  Random UUID               File extension
                String uniqueName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

                Path filePath = uploadPath.resolve(uniqueName); // produce full path

                // Save file to disk
                try (var in = profilePhotoFile.getInputStream()) {
                    Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                user.setProfilePhotoUrl("/uploads/user_profiles/" + uniqueName);

            } catch (IOException e) {
                throw new RuntimeException("Failed to store file", e);
            }
        }

        //it save the user in DB then access parat
        User savedUser = userReposotory.save(user);

        // Build response DTO explicitly
        UserDto dtoResponse = new UserDto();
        dtoResponse.setId(savedUser.getId());
        dtoResponse.setMobileNumber(savedUser.getMobileNumber());
        dtoResponse.setName(savedUser.getName());
        dtoResponse.setUsername(savedUser.getUsername());
        dtoResponse.setBio(savedUser.getBio());
        dtoResponse.setEmail(savedUser.getEmail());
//        dtoResponse.setUserPassword(savedUser.getUserPassword());
        dtoResponse.setProfilePhotoUrl(savedUser.getProfilePhotoUrl());

        return dtoResponse;
    }

    @Override
    public UserDto authenticate(String identifier) {

        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();


        System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"
                + baseUrl
                + "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n ");


        return userReposotory.findByEmail(identifier)
                .or(() -> userReposotory.findByUsername(identifier))
                .map(user -> {
                    UserDto dto = new UserDto();
                    dto.setEmail(user.getEmail());
                    dto.setId(user.getId());
                    dto.setUsername(user.getUsername());
                    dto.setName(user.getName());
                    dto.setBio(user.getBio());
                    dto.setMobileNumber(user.getMobileNumber());

                    dto.setProfilePhotoUrl(baseUrl + user.getProfilePhotoUrl());

                    dto.setFollowerCount(followRepository.countByFollowingId(user.getId()));
                    dto.setFollowingCount(followRepository.countByFollowerId(user.getId()));

//                    dto.setMyFollowingIds(followRepository.findFollowingIdsByFollowerId(user.getId()));
//                    dto.setMyFollowerIds(followRepository.findFollowerIdsByFollowingId(user.getId()));

                    // Fetch posts
                    List<Post> posts = postRepository.findByUserId(user.getId());
                    List<PostDto> postDtos = posts.stream()
                            .map(p -> {
                                PostDto pd = new PostDto();
                                pd.setId(p.getId());
                                pd.setUserId(p.getUserId());
                                pd.setType(p.getType());
                                pd.setCaption(p.getCaption());
                                pd.setContentType(p.getContentType());
                                pd.setCreatedAt(p.getCreatedAt());

                                String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                                        .path("/api/files/")
                                        .path(p.getFileUrl())
                                        .toUriString();
                                pd.setFileUrl(fileUrl);

                                return pd;
                            })
                            .toList();

                    return dto;
                })
                .orElse(null);
    }

//    @Override
//    public UserDto updateUserByEmail(String email, UpdateUserDto dto) {
//        User user = userReposotory.findByEmail(email)
//                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
//
//        if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
//            if (userReposotory.existsByUsername(dto.getUsername())) {
//                // Instead of returning ResponseEntity, throw a simple runtime exception
//                throw new IllegalArgumentException("Username already taken");
//            }
//            user.setUsername(dto.getUsername());
//        }
//
//        if (dto.getBio() != null) user.setBio(dto.getBio());
//        if (dto.getName() != null) user.setName(dto.getName());
//
//        User saved = userReposotory.save(user);
//        return modelMapper.map(saved, UserDto.class);
//    }

    @Override
    public UserDto updateUserByEmail(String email, UpdateUserDto dto) {
        User user = userReposotory.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // If all fields (username, name, bio) are provided
        if (dto.getUsername() != null && dto.getBio() != null && dto.getName() != null) {

            boolean noChange = dto.getUsername().equals(user.getUsername()) &&
                    dto.getName().equals(user.getName()) &&
                    dto.getBio().equals(user.getBio());

            if (noChange) {
                // Nothing changed — return the same user without saving again
                return modelMapper.map(user, UserDto.class);
            }

            // If username changed, check uniqueness
            if (!dto.getUsername().equals(user.getUsername()) &&
                    userReposotory.existsByUsername(dto.getUsername())) {
                throw new IllegalArgumentException("Username already taken");
            }

            // Apply changes
            user.setUsername(dto.getUsername());
            user.setBio(dto.getBio());
            user.setName(dto.getName());
        }
        // Optional: handle partial updates (if some fields are null)
        else {
            if (dto.getUsername() != null && !dto.getUsername().equals(user.getUsername())) {
                if (userReposotory.existsByUsername(dto.getUsername())) {
                    throw new IllegalArgumentException("Username already taken");
                }
                user.setUsername(dto.getUsername());
            }
            if (dto.getName() != null) user.setName(dto.getName());
            if (dto.getBio() != null) user.setBio(dto.getBio());
        }

        User saved = userReposotory.save(user);
        return modelMapper.map(saved, UserDto.class);
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userReposotory.findByEmail(identifier)
                .or(() -> userReposotory.findByUsername(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identifier: " + identifier));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())              // use canonical username for UserDetails.getUsername()
                .password(user.getUserPassword())          // this must be the encoded password
                .authorities("ROLE_USER")                  // adapt authorities/roles as needed
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
