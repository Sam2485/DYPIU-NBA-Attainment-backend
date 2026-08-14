package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "programme_targets", uniqueConstraints = {
    @UniqueConstraint(name = "uq_prog_target", columnNames = {"programme_id", "outcome_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeTarget {

    @Id
    private String id;

    @Column(name = "programme_id", nullable = false)
    private String programmeId;

    @Column(name = "outcome_code", nullable = false, length = 20)
    private String outcomeCode;

    @Column(name = "target_value", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal targetValue = new BigDecimal("2.00");

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
