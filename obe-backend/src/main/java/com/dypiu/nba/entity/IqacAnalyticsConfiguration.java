package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "iqac_analytics_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IqacAnalyticsConfiguration {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "student_evidence_threshold", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal studentEvidenceThreshold = new BigDecimal("50.00");

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = ZonedDateTime.now();
        }
        if (studentEvidenceThreshold == null) {
            studentEvidenceThreshold = new BigDecimal("50.00");
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now();
    }
}
