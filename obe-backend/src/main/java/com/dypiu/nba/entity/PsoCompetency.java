package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(
        name = "pso_competencies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pso_competency_code",
                        columnNames = {"pso_id", "code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PsoCompetency {

    @Id
    private String id;

    @Column(name = "pso_id", nullable = false)
    private String psoId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;
}