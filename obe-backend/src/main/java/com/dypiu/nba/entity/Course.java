package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    private String id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "programme_id", nullable = false)
    private String programmeId;

    @Column(nullable = false, length = 50)
    private String semester;

    private String coordinator;
    private String faculty;

    @Column(name = "assigned_faculty", columnDefinition = "TEXT")
    private String assignedFaculty;

    @Column(name = "academic_year", nullable = false)
    @Builder.Default
    private String academicYear = "2025-26";

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
