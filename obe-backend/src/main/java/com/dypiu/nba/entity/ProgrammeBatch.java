package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "programme_batches",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_programme_batch_start_year",
                        columnNames = {"master_programme_id", "start_year"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeBatch {

    @Id
    private String id;

    @Column(name = "master_programme_id", nullable = false)
    private String masterProgrammeId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "start_year", nullable = false)
    private Integer startYear;

    @Column(name = "end_year", nullable = false)
    private Integer endYear;

    @Column(name = "duration_years", nullable = false)
    @Builder.Default
    private Integer durationYears = 4;

    @Column(name = "coordinator_id")
    private Long coordinatorId;

    @Column(name = "coordinator_name", length = 150)
    private String coordinatorName;

    @Column(name = "coordinator_email", length = 150)
    private String coordinatorEmail;

    @Column(name = "year_level", length = 100)
    private String yearLevel;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

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

    // Helper getter/setter for compatibility during phased migration
    public String getProgrammeId() {
        return masterProgrammeId;
    }

    public void setProgrammeId(String programmeId) {
        this.masterProgrammeId = programmeId;
    }

    public String getCoordinator() {
        return coordinatorName;
    }

    public void setCoordinator(String coordinator) {
        this.coordinatorName = coordinator;
    }
}
