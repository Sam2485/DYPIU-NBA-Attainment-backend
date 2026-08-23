package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "programme_batch_attainment_reports",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_programme_batch_attainment_report",
                        columnNames = {"programme_batch_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeBatchAttainmentReport {

    @Id
    private String id;

    @Column(name = "programme_batch_id", nullable = false)
    private String programmeBatchId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(name = "overall_programme_attainment")
    private BigDecimal overallProgrammeAttainment;

    @Column(name = "average_mapping_report_json", columnDefinition = "TEXT")
    private String averageMappingReportJson;

    @Column(name = "direct_attainment_report_json", columnDefinition = "TEXT")
    private String directAttainmentReportJson;

    @Column(name = "indirect_attainment_report_json", columnDefinition = "TEXT")
    private String indirectAttainmentReportJson;

    @Column(name = "overall_attainment_report_json", columnDefinition = "TEXT")
    private String overallAttainmentReportJson;

    @Column(name = "submitted_by", length = 150)
    private String submittedBy;

    @Column(name = "submitted_at")
    private ZonedDateTime submittedAt;

    @Column(name = "approved_by", length = 150)
    private String approvedBy;

    @Column(name = "approved_at")
    private ZonedDateTime approvedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}
