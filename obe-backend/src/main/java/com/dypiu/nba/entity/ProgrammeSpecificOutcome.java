package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "programme_specific_outcomes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeSpecificOutcome {

    @Id
    private String id;

    @Column(name = "programme_id", nullable = false)
    private String programmeId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "academic_year", nullable = false)
    @Builder.Default
    private String academicYear = "2025-26";

    @Transient
    @Builder.Default
    private List<PsoCompetency> competencies = new ArrayList<>();

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;
}
