package com.projects.instagram.controller;


import com.projects.instagram.dto.AddStudentRequestDto;
import com.projects.instagram.dto.AuthRequest;
import com.projects.instagram.dto.UpdateUserDto;
import com.projects.instagram.dto.UserDto;
import com.projects.instagram.entity.User;
import com.projects.instagram.exception.UserNotFoundException;
import com.projects.instagram.repository.UserReposotory;
import com.projects.instagram.security.JwtUtil;
import com.projects.instagram.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = {
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "http://localhost:5173",
        "http://10.91.2.29:5173",
        "http://10.91.2.29:5173/instagram-clone",
        "https://social-media-frontend-nbdo.vercel.app",
        "*"
})
@Slf4j
@RestController
@RequiredArgsConstructor
public class InstaController {

    private final UserProfileService userProfileService;
    private final AuthenticationManager authenticationManager;
    private final UserReposotory userReposotory;
    private final JwtUtil jwtUtil;

    @PostMapping(value = "/users",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE, // It expects a form submission
            produces = MediaType.APPLICATION_JSON_VALUE) // The API responds with JSON.
    public ResponseEntity<?> createNewUser(@Valid @ModelAttribute AddStudentRequestDto addStudentRequestDto,
                                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = bindingResult.getFieldErrors()
                    .stream().collect(Collectors.toMap(f -> f.getField(), f -> f.getDefaultMessage()));
            return ResponseEntity.badRequest().body(errors);
        }

        UserDto created = userProfileService.createNewUser(addStudentRequestDto);

        // build full URL (includes host, port and context-path)
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        if (created.getProfilePhotoUrl() != null && !created.getProfilePhotoUrl().isBlank()) {
            created.setProfilePhotoUrl(baseUrl + created.getProfilePhotoUrl()); // setter
        }
        // return 201 with Location header
        return ResponseEntity.created(URI.create("/instagram/users/" + created.getId())).body(created);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<List<UserDto>> getUsers(@PathVariable Long id) { // ResponseEntity is a Spring class that represents the whole HTTP response.
        return ResponseEntity.ok(userProfileService.getAllUsers(id)); // Status code → (200 OK, 404 NOT FOUND, 500 INTERNAL SERVER ERROR).
    }

    @GetMapping("/users/user/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) { // Spring injects that value into your method parameter
        return ResponseEntity.ok(userProfileService.getUserById(id));
    }

    @PostMapping(value = "/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        try {
            // 1. resolve user first (by email OR username)
            User user = userReposotory.findByEmail(request.identifier())
                    .or(() -> userReposotory.findByUsername(request.identifier()))
//                    .or(() -> userReposotory.findByMobileNumber(request.identifier()))

                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            // Authenticate using either email or username
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.userPassword())
            );

            // Generate JWT token
            String token = jwtUtil.generateToken(user.getUsername()); // canonical username

            // fetch user details by canonical username (not the raw identifier)
            UserDto userDto = userProfileService.authenticate(user.getUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login successful!",
                    "token", token,
                    "type", "Bearer",
                    "user", userDto
            ));
        } catch (
                BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid username/email or password"));
        } catch (Exception ex) {
            // Log and return generic 500 — don't return stack traces to client
            log.error("Login error for identifier={}: {}", request.identifier(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Server error"));
        }
    }

    @PutMapping(value = "/users/email/{email}")
    public ResponseEntity<?> updateByEmail(@PathVariable("email") String email,
                                           @Valid @RequestBody UpdateUserDto dto) {
        try {
            UserDto updated = userProfileService.updateUserByEmail(email, dto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User updated successfully",
                    "user", updated
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


}