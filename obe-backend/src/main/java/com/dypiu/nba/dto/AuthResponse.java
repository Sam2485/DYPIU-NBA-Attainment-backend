package com.dypiu.nba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserDto user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        @com.fasterxml.jackson.annotation.JsonProperty("userId")
        private Long id;
        private String name;
        private String email;
        private String username;
        private String role;
        private String schoolId;
        private String departmentId;
        private String masterProgrammeId;
        private String department;
        private String programme;
    }
}
