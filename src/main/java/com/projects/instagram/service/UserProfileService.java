package com.projects.instagram.service;

import com.projects.instagram.dto.AddStudentRequestDto;
import com.projects.instagram.dto.UpdateUserDto;
import com.projects.instagram.dto.UserDto;
import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

public interface UserProfileService extends UserDetailsService {

    List<UserDto> getAllUsers(Long currentUserId);

    UserDto getUserById(Long id);

    UserDto authenticate(String identifier);

    UserDto createNewUser(AddStudentRequestDto addStudentRequestDto);

    UserDto updateUserByEmail(String email, @Valid UpdateUserDto dto);

   UserDetails loadUserByUsername(String username);
}
