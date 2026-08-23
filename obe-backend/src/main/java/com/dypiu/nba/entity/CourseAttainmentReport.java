package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "course_attainment_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_attainment_report",
                        columnNames = {"programme_batch_course_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAttainmentReport {

    @Id
    private String id;

    @Column(name = "programme_batch_course_id", nullable = false)
    private String programmeBatchCourseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(name = "overall_co_attainment")
    private BigDecimal overallCoAttainment;

    @Column(name = "direct_attainment")
    private BigDecimal directAttainment;

    @Column(name = "indirect_attainment")
    private BigDecimal indirectAttainment;

    @Column(name = "table1_mapping_json", columnDefinition = "TEXT")
    private String table1MappingJson;

    @Column(name = "table2_direct_json", columnDefinition = "TEXT")
    private String table2DirectJson;

    @Column(name = "table3_co_attainment_json", columnDefinition = "TEXT")
    private String table3CoAttainmentJson;

    @Column(name = "submitted_by", length = 150)
    private String submittedBy;

    @Column(name = "submitted_at")
    private ZonedDateTime submittedAt;

    @Column(name = "verified_by", length = 150)
    private String verifiedBy;

    @Column(name = "verified_at")
    private ZonedDateTime verifiedAt;

    @Column(name = "approved_by", length = 150)
    private String approvedBy;

    @Column(name = "approved_at")
    private ZonedDateTime approvedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
