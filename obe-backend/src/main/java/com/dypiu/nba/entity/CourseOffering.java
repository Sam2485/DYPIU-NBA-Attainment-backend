package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(
    name = "course_offerings",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_batch_course_sem",
            columnNames = {"batch_id", "course_id", "semester"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseOffering {

    @Id
    private String id;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(nullable = false)
    @Builder.Default
    private Integer semester = 1;

    @Column(name = "academic_year", nullable = false)
    @Builder.Default
    private String academicYear = "2025-26";

    @Column(name = "course_coordinator_id")
    private String courseCoordinatorId;

    @Column(name = "course_coordinator_name")
    private String courseCoordinatorName;

    @Column(name = "assigned_faculty", columnDefinition = "TEXT")
    private String assignedFaculty;

    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
