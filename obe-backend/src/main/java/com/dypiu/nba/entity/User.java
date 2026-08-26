package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /*
     * Organizational scope.
     *
     * IQAC      → schoolId may be null because IQAC is institution-wide
     * DIRECTOR  → schoolId
     * HOD       → departmentId
     * PC        → masterProgrammeId
     * FACULTY   → depends on assignment
     */
    @Column(name = "school_id")
    private String schoolId;

    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "master_programme_id")
    private String masterProgrammeId;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Transient
    private String department;

    @Transient
    private String programme;
}