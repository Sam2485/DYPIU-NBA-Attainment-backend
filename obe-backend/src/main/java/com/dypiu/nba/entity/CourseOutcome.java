package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "course_outcomes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseOutcome {

    @Id
    private String id;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
