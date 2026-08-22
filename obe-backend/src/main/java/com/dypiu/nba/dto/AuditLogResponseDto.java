package com.dypiu.nba.dto;

import com.dypiu.nba.audit.AuditAction;
import com.dypiu.nba.audit.ResourceType;
import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponseDto {
    private Long id;
    private String actorId;
    private String actorRole;
    private String actorName;
    private String actorEmail;
    private AuditAction action;
    private ResourceType resourceType;
    private String resourceId;
    private String oldStatus;
    private String newStatus;
    private String remarks;
    private String metadata;
    private boolean success;
    private String ipAddress;
    private String userAgent;
    private ZonedDateTime createdAt;
}
