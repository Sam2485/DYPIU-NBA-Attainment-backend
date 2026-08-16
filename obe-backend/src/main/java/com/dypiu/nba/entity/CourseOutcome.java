package com.dypiu.nba.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "course_outcomes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseOutcome {

    @Id
    private String id;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "target_level")
    @Builder.Default
    private BigDecimal targetLevel = new BigDecimal("2.50");

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @JsonProperty("targetLevel")
    public void setTargetLevelFromJson(Object val) {
        if (val == null) return;
        if (val instanceof BigDecimal) {
            this.targetLevel = (BigDecimal) val;
        } else if (val instanceof Number) {
            this.targetLevel = BigDecimal.valueOf(((Number) val).doubleValue());
        } else {
            try {
                this.targetLevel = new BigDecimal(val.toString().trim());
            } catch (Exception e) {
                this.targetLevel = new BigDecimal("2.50");
            }
        }
    }

    @JsonProperty("target")
    public void setTargetFromJson(Object val) {
        if (val != null) {
            setTargetLevelFromJson(val);
        }
    }
}
