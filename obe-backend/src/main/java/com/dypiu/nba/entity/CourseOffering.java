package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "course_offerings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_batch_course_semester",
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
    private Integer semester;

    @Column(name = "course_coordinator_id")
    private Long courseCoordinatorId;

    @Column(name = "course_coordinator_name")
    private String courseCoordinatorName;

    @Column(name = "assigned_faculty", columnDefinition = "TEXT")
    private String assignedFaculty;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}