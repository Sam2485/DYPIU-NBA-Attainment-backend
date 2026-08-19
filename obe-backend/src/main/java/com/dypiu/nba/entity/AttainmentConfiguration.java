package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

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

    @Column(name = "direct_levels_json", columnDefinition = "TEXT")
    private String directLevelsJson;

    @Column(name = "indirect_levels_json", columnDefinition = "TEXT")
    private String indirectLevelsJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @Transient
    public List<Map<String, Object>> getDirectLevels() {
        return List.of(
                Map.of("level", 1, "minPercentage", 0, "maxPercentage", 50),
                Map.of("level", 2, "minPercentage", 50, "maxPercentage", 70),
                Map.of("level", 3, "minPercentage", 70, "maxPercentage", 100)
        );
    }

    @Transient
    public List<Map<String, Object>> getIndirectLevels() {
        return List.of(
                Map.of("level", 1, "minPercentage", 0, "maxPercentage", 50),
                Map.of("level", 2, "minPercentage", 50, "maxPercentage", 70),
                Map.of("level", 3, "minPercentage", 70, "maxPercentage", 100)
        );
    }
}