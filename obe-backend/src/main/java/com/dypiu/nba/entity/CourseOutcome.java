package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "course_outcomes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_offering_co_code",
                        columnNames = {"course_offering_id", "code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseOutcome {

    @Id
    private String id;

    @Column(name = "course_offering_id", nullable = false)
    private String courseOfferingId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "target_level", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal targetLevel = new BigDecimal("2.50");

    @Column(name = "blooms_level", length = 50)
    @Builder.Default
    private String bloomsLevel = "L3 - Apply";

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Transient
    public BigDecimal getTarget() {
        return targetLevel;
    }

    public void setTarget(BigDecimal target) {
        if (target != null) {
            this.targetLevel = target;
        }
    }
}