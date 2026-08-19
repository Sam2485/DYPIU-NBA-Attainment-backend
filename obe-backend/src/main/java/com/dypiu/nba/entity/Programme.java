package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "programmes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_department_programme_code",
                        columnNames = {"department_id", "code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Programme {

    @Id
    private String id;

    @Column(name = "department_id", nullable = false)
    private String departmentId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "duration_years", nullable = false)
    @Builder.Default
    private Integer durationYears = 4;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "coordinator", length = 150)
    private String coordinator;

    @Column(name = "coordinator_email", length = 150)
    private String coordinatorEmail;

    @Column(name = "department_name", length = 255)
    private String departmentName;
}