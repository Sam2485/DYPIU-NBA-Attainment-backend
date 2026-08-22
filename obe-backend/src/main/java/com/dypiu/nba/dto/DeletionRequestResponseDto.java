package com.dypiu.nba.dto;

import com.dypiu.nba.audit.ResourceType;
import com.dypiu.nba.deletion.DeletionRequestStatus;
import lombok.*;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletionRequestResponseDto {
    private Long id;
    private ResourceType resourceType;
    private String resourceId;
    private String programmeBatchId;
    private String programmeBatchCourseId;
    private String masterProgrammeId;
    private String departmentId;
    private String schoolId;
    private DeletionRequestStatus status;
    private String requestedBy;
    private String requestedById;
    private String requestedByRole;
    private ZonedDateTime requestedAt;
    private String reviewedBy;
    private String reviewedById;
    private String reviewedByRole;
    private ZonedDateTime reviewedAt;
    private ZonedDateTime executedAt;
    private String remarks;
    private String rejectionReason;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
