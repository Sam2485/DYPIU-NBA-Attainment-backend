package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "co_pso_mappings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_co_pso",
                        columnNames = {"course_outcome_id", "pso_code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoPsoMapping {

    @Id
    private String id;

    @Column(name = "course_outcome_id", nullable = false)
    private String courseOutcomeId;

    @Column(name = "pso_code", nullable = false, length = 20)
    private String psoCode;

    @Column(name = "mapping_level", nullable = false)
    @Builder.Default
    private Integer mappingLevel = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}