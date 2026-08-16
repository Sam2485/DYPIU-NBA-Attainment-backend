package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "attainment_configurations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_attainment_config_offering",
                        columnNames = {"course_offering_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttainmentConfiguration {

    @Id
    private String id;

    @Column(name = "course_offering_id", nullable = false)
    private String courseOfferingId;

    @Column(name = "direct_weight", nullable = false)
    @Builder.Default
    private BigDecimal directWeight = new BigDecimal("80.00");

    @Column(name = "indirect_weight", nullable = false)
    @Builder.Default
    private BigDecimal indirectWeight = new BigDecimal("20.00");

    @Column(name = "direct_threshold", nullable = false)
    @Builder.Default
    private BigDecimal directThreshold = new BigDecimal("60.00");

    @Column(name = "indirect_threshold", nullable = false)
    @Builder.Default
    private BigDecimal indirectThreshold = new BigDecimal("60.00");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AttainmentConfigStatus status = AttainmentConfigStatus.DRAFT;

    private String submittedBy;

    private ZonedDateTime submittedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;
}