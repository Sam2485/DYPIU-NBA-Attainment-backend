package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "batches")
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

    @Column(name = "previous_batch_id")
    private String previousBatchId;

    @Column(name = "programme_code")
    private String programmeCode;

    @Column(name = "programme_name")
    private String programmeName;

    @Column(name = "duration_years", nullable = false)
    @Builder.Default
    private Integer durationYears = 4;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_year", nullable = false)
    private String startYear;

    @Column(name = "end_year", nullable = false)
    private String endYear;

    @Column(name = "year_level")
    private String yearLevel;

    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INITIALIZED, GRADUATED

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
