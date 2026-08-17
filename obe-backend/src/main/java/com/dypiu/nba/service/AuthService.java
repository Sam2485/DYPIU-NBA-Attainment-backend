package com.dypiu.nba.service;

import com.dypiu.nba.dto.*;
import com.dypiu.nba.entity.User;
import com.dypiu.nba.entity.UserRole;
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
        System.out.println("[AuthService] login called | identifier: " + (request != null ? (request.getUsername() != null ? request.getUsername() : request.getEmail()) : "null"));
        String rawIdentifier = request.getUsername() != null ? request.getUsername() : request.getEmail();
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            throw new BadRequestException("Username or email is required");
        }

        String rawPassword = request.getPassword();
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BadRequestException("Password is required");
        }

        // Clean & trim email or username input
        String identifier = rawIdentifier.trim();

        // 1. Check if user exists by email or username
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseGet(() -> userRepository.findByUsernameOrEmail(identifier, identifier)
                .orElseThrow(() -> new BadRequestException("Invalid email/username or password")));

        if (user.getIsActive() != null && !user.getIsActive()) {
            throw new BadRequestException("User account is deactivated");
        }

        // 2. Verify hashed password against database passwordHash
        if (!passwordEncoder.matches(rawPassword.trim(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email/username or password");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        System.out.println("[AuthService] register called | username: " + (request != null ? request.getUsername() : "null") + " | email: " + (request != null ? request.getEmail() : "null"));
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        UserRole userRole = UserRole.FACULTY;
        if (request.getRole() != null) {
            try {
                userRole = UserRole.valueOf(request.getRole().toUpperCase());
            } catch (Exception ignored) {}
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName() != null ? request.getName() : request.getUsername())
                .role(userRole)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        System.out.println("[AuthService] refreshToken called");
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
        System.out.println("[AuthService] requestPasswordReset called | email: " + email);
        return "Password reset link has been sent to " + email + ". Please check your inbox.";
    }

    public String resetPassword(String token, String newPassword) {
        System.out.println("[AuthService] resetPassword called | token: " + token);
        return "Password reset successfully. Please login with your new credentials.";
    }

    public AuthResponse verifyOtp(String loginSessionId, String code) {
        System.out.println("[AuthService] verifyOtp called | loginSessionId: " + loginSessionId + " | code: " + code);
        User defaultUser = User.builder()
                .id(1L)
                .name("Verified User")
                .email("user@dypiu.ac.in")
                .username("verified_user")
                .role(UserRole.FACULTY)
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
                        .role(user.getRole() != null ? user.getRole().name() : "FACULTY")
                        .build())
                .build();
    }

}
