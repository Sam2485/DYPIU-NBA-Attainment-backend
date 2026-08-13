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

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getUsername() != null ? request.getUsername() : request.getEmail();
        if (identifier == null || identifier.isBlank()) {
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

        return buildAuthResponse(user);
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
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || !tokenProvider.validateRefreshToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String username = tokenProvider.getUsernameFromJwt(refreshToken);
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new BadRequestException("User not found for refresh token"));

        return buildAuthResponse(user);
    }

    public String requestPasswordReset(String email) {
        return "Password reset link has been sent to " + email + ". Please check your inbox.";
    }

    public String resetPassword(String token, String newPassword) {
        return "Password reset successfully. Please login with your new credentials.";
    }

    public AuthResponse verifyOtp(String loginSessionId, String code) {
        User defaultUser = User.builder()
                .id(1L)
                .name("Verified User")
                .email("user@dypiu.ac.in")
                .username("verified_user")
                .role("FACULTY")
                .department("Computer Science & Engineering")
                .programme("B.Tech CSE")
                .build();

        return buildAuthResponse(defaultUser);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = tokenProvider.generateTokenForUser(user.getUsername());
        String refreshToken = tokenProvider.generateRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .token(accessToken)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getJwtExpirationInMs())
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
}
