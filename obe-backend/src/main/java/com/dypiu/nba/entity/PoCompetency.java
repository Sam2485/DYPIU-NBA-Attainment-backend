package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "po_competencies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoCompetency {

    @Id
    private String id;

    @Column(name = "po_id")
    private String poId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;
}
