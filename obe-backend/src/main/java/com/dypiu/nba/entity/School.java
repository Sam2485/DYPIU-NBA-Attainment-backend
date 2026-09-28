package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "schools")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School {

    @Id
    @JsonProperty("schoolId")
    private String id;

    @Column(nullable = false, length = 20)
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

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Transient
    private Integer totalDepartments;

    @Transient
    private Integer totalProgrammes;

    @Transient
    public String getDirector() {
        return directorName;
    }

    public void setDirector(String director) {
        this.directorName = director;
    }

    @Transient
    public String getDean() {
        return directorName;
    }

    public void setDean(String dean) {
        this.directorName = dean;
    }

    @Transient
    public String getDeanEmail() {
        return directorEmail;
    }

    public void setDeanEmail(String deanEmail) {
        this.directorEmail = deanEmail;
    }
}