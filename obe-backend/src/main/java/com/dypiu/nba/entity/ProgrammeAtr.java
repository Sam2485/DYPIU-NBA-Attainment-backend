package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "programme_atrs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_programme_batch_atr",
                        columnNames = {"programme_id", "batch_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeAtr {

    @Id
    private String id;

    @Column(name = "programme_id", nullable = false)
    private String programmeId;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProgrammeAtrStatus status = ProgrammeAtrStatus.DRAFT;

    // Created/submitted by Programme Coordinator
    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "submitted_at")
    private ZonedDateTime submittedAt;

    // Verified by HOD
    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private ZonedDateTime verifiedAt;

    @Column(name = "verification_comments", columnDefinition = "TEXT")
    private String verificationComments;

    @Column(name = "observations_json", columnDefinition = "TEXT")
    private String observationsJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}