package com.projects.instagram.service;



import com.projects.instagram.repository.UserReposotory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthService {


    private final UserReposotory userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;


    public AuthService(UserReposotory userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

//    public User register(RegisterRequest request) {
//        if (userRepository.existsByUsername(request.username())) {
//            throw new IllegalArgumentException("Username already taken");
//        }
//        if (userRepository.existsByEmail(request.email())) {
//            throw new IllegalArgumentException("Email already taken");
//        }
//
//        User user = User.builder()
//                .username(request.username())
//                .email(request.email())
//                .password(passwordEncoder.encode(request.password()))
//                .role("ROLE_USER")
//                .build();
//
//        return userRepository.save(user);
//    }


//    public void authenticate(AuthRequest request) {
//        authenticationManager.authenticate(
//                new UsernamePasswordAuthenticationToken(request.username(), request.password())
//        );
//    }
}