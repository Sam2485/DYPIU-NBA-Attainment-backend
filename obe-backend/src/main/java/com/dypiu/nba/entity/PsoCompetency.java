package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pso_competencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PsoCompetency {

    @Id
    private String id;

    @Column(name = "pso_id")
    private String psoId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;
}
