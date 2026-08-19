package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "batches",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_batch_programme_start_year",
                        columnNames = {"programme_id", "start_year"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

    @Id
    private String id;

    @Column(name = "programme_id", nullable = false)
    private String programmeId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    @Column(name = "end_year", nullable = false)
    private Integer endYear;

    @Column(name = "duration_years", nullable = false)
    @Builder.Default
    private Integer durationYears = 4;

    @Column(name = "previous_batch_id")
    private String previousBatchId;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    // ACTIVE, COMPLETED, ARCHIVED

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Transient
    private String programmeName;

    @Transient
    private String programmeCode;

    @Transient
    @Builder.Default
    private Integer currentSemester = 1;
}