package com.dypiu.nba.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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
        private List<String> roles;
        private String schoolId;
        private String schoolName;
        private String departmentId;
        private String departmentName;
        private String masterProgrammeId;
        private String masterProgrammeName;
        private String programmeBatchId;
        private String department;
        private String programme;

        @Builder.Default
        private List<Map<String, String>> schools = java.util.Collections.emptyList();

        @Builder.Default
        private List<String> schoolIds = java.util.Collections.emptyList();

        @Builder.Default
        private List<String> schoolNames = java.util.Collections.emptyList();

        @Builder.Default
        private List<UserOrganizationalAssignmentDto> assignments = java.util.Collections.emptyList();
    }
}
