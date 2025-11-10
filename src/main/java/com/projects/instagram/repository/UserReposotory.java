package com.projects.instagram.repository;

import com.projects.instagram.entity.User;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserReposotory extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMobileNumber(String mobile);
    Optional<User> findByUsername(String username);


    boolean existsByUsername(@NotBlank String identifier);
    boolean existsByEmail(@NotBlank String  identifier);
}
