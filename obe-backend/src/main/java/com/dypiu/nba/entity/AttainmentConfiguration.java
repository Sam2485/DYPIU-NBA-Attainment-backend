package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "attainment_configurations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttainmentConfiguration {

    @Id
    private String id;

    @Column(name = "course_id", nullable = false, unique = true)
    private String courseId;

    @Column(name = "course_code", nullable = false)
    private String courseCode;

    @Column(name = "course_name", nullable = false)
    private String courseName;

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

    @Builder.Default
    private String status = "DRAFT"; // DRAFT, SUBMITTED, VERIFIED

    private String submittedBy;
    private ZonedDateTime submittedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
