package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(
        name = "programme_specific_outcomes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_batch_pso_code",
                        columnNames = {"programme_batch_id", "code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeSpecificOutcome {

    @Id
    private String id;

    @Column(name = "programme_batch_id", nullable = false)
    private String programmeBatchId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "target", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal target = new BigDecimal("2.50");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Transient
    private String academicYear;

    @Transient
    private List<PsoCompetency> competencies;

    // Helper compatibility methods
    public String getMasterProgrammeId() {
        return programmeBatchId;
    }

    public void setMasterProgrammeId(String masterProgrammeId) {
        this.programmeBatchId = masterProgrammeId;
    }
}
