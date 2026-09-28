package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganizationalAssignmentDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String role;

    private String schoolId;
    private String schoolName;

    private String departmentId;
    private String departmentName;

    private String masterProgrammeId;
    private String masterProgrammeName;

    @JsonProperty("isActive")
    @Builder.Default
    private Boolean isActive = true;

    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
