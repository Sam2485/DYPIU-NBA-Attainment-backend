package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "director_id")
    private Long directorId;

    @Column(name = "director_name")
    private String directorName;

    @Column(name = "director_email")
    private String directorEmail;

    @Column(name = "est_year")
    private String estYear;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Transient
    public String getDirector() {
        return directorName;
    }

    public void setDirector(String director) {
        this.directorName = director;
    }
}