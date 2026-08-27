package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(
        name = "course_outcomes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_batch_course_co_code",
                        columnNames = {"programme_batch_course_id", "code"}
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
    @JsonProperty("courseOutcomeId")
    private String id;

    @Column(name = "programme_batch_course_id", nullable = false)
    private String programmeBatchCourseId;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.DRAFT;

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

    // Helper compatibility methods
    public String getProgrammeBatchCourseId() {
        return programmeBatchCourseId;
    }

    public void setProgrammeBatchCourseId(String offeringId) {
        this.programmeBatchCourseId = offeringId;
    }
}
