package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "course_atrs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAtr {

    @Id
    private String id;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "course_offering_id")
    private String courseOfferingId;

    @Column(name = "co_code", nullable = false, length = 30)
    private String coCode;

    private String title;

    @Column(name = "target_score", nullable = false)
    private BigDecimal targetScore;

    @Column(name = "actual_score", nullable = false)
    private BigDecimal actualScore;

    @Column(name = "pct_achieved", nullable = false)
    private BigDecimal pctAchieved;

    @Column(nullable = false, length = 50)
    private String status; // "Target Achieved", "Target Not Achieved"

    @Column(columnDefinition = "TEXT")
    private String statement;

    @Column(name = "actions_json", columnDefinition = "TEXT")
    private String actionsJson;

    private String submittedBy;

    @Column(name = "submitted_at", insertable = false, updatable = false)
    private ZonedDateTime submittedAt;
}
