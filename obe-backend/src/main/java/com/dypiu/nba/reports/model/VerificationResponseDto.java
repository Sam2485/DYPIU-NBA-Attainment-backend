package com.dypiu.nba.reports.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseDto {
    private String reportId;
    private String artifactId;
    private ArtifactType artifactType;
    private String status; // "VALID", "HASH_MISMATCH", "HMAC_MISMATCH", "INVALID_INPUT", "REPORT_NOT_FOUND"
    private boolean isValid;
    private String expectedSha256;
    private String calculatedSha256;
    private String expectedHmac;
    private String calculatedHmac;
    private Long fileSize;
    private ZonedDateTime verifiedAt;
    private String message;
}
