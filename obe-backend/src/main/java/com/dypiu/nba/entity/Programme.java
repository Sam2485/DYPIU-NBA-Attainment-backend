package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "programmes")
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

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "duration_years", nullable = false)
    @Builder.Default
    private Integer durationYears = 4;

    private String departmentName;
    private String coordinator;
    private String coordinatorEmail;

    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
