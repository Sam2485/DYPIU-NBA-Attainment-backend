package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "course_atrs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_batch_course_co_atr",
                        columnNames = {"programme_batch_course_id", "co_code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAtr {

    @Id
    private String id;

    @Column(name = "programme_batch_course_id", nullable = false)
    private String programmeBatchCourseId;

    @Column(name = "co_code", nullable = false, length = 30)
    private String coCode;

    private String title;

    @Column(name = "target_score", nullable = false)
    private BigDecimal targetScore;

    @Column(name = "actual_score", nullable = false)
    private BigDecimal actualScore;

    @Column(name = "pct_achieved", nullable = false)
    @Builder.Default
    private BigDecimal pctAchieved = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CourseAtrStatus status = CourseAtrStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String statement;

    @Column(name = "actions_json", columnDefinition = "TEXT")
    private String actionsJson;

    // Created by Course Coordinator
    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "submitted_at")
    private ZonedDateTime submittedAt;

    // Verified by Programme Coordinator
    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(name = "verified_at")
    private ZonedDateTime verifiedAt;

    @Column(name = "verification_comments", columnDefinition = "TEXT")
    private String verificationComments;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    // Helper compatibility methods
    public String getProgrammeBatchCourseId() {
        return programmeBatchCourseId;
    }

    public void setProgrammeBatchCourseId(String programmeBatchCourseId) {
        this.programmeBatchCourseId = programmeBatchCourseId;
    }
}
