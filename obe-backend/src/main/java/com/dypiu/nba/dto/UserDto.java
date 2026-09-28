package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    @JsonProperty("userId")
    private Long id;

    @JsonProperty("id")
    public Long getIdValue() {
        return id;
    }

    private String username;
    private String name;
    private String email;
    private String role;
    private String schoolId;
    private String departmentId;
    private String masterProgrammeId;
    private String department;
    private String programme;
    private List<String> roles;

    @Builder.Default
    private List<UserOrganizationalAssignmentDto> assignments = java.util.Collections.emptyList();

    @Builder.Default
    private List<Map<String, String>> schools = java.util.Collections.emptyList();

    @Builder.Default
    private List<String> schoolIds = java.util.Collections.emptyList();

    @Builder.Default
    private List<String> schoolNames = java.util.Collections.emptyList();

    @JsonProperty("isActive")
    private Boolean isActive;
}
