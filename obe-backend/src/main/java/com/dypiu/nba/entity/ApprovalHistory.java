package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "approval_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalHistory {

    @Id
    private String id;

    @Column(name = "approval_request_id", nullable = false)
    private String approvalRequestId;

    @Column(name = "actor_name", nullable = false)
    private String actorName;

    @Column(name = "actor_role", nullable = false)
    private String actorRole;

    @Column(nullable = false)
    private String action; // SUBMITTED, APPROVED, REJECTED, REVISION_REQUESTED

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "timestamp", insertable = false, updatable = false)
    private ZonedDateTime timestamp;
}
