package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "programme_atrs")
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

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "academic_year", nullable = false)
    @Builder.Default
    private String academicYear = "2025-26";

    @Builder.Default
    private String status = "DRAFT"; // DRAFT, SUBMITTED_FOR_APPROVAL, APPROVED, NEEDS_REVISION

    private String submittedBy;
    private ZonedDateTime submittedAt;
    private String approvedBy;
    private ZonedDateTime approvedAt;

    @Column(name = "observations_json", columnDefinition = "TEXT")
    private String observationsJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
