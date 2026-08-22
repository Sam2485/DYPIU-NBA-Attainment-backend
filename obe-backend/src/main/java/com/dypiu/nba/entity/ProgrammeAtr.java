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
                        columnNames = {"programme_batch_id"}
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

    @Column(name = "programme_batch_id", nullable = false)
    private String programmeBatchId;

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

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private ZonedDateTime approvedAt;

    @Column(name = "verification_comments", columnDefinition = "TEXT")
    private String verificationComments;

    @Column(name = "observations_json", columnDefinition = "TEXT")
    private String observationsJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    // Helper compatibility methods
    public String getBatchId() {
        return programmeBatchId;
    }

    public void setBatchId(String batchId) {
        this.programmeBatchId = batchId;
    }

    public String getProgrammeId() {
        return programmeBatchId;
    }

    public void setProgrammeId(String programmeId) {
        // preserved for compatibility
    }
}
