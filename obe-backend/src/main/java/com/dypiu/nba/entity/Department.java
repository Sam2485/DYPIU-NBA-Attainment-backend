package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    private String id;

    @Column(name = "school_id", nullable = false)
    private String schoolId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    private String hod;
    private String hodEmail;

    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
