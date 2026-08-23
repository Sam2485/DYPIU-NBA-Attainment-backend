package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(
        name = "programme_batch_courses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_batch_course_sem",
                        columnNames = {"programme_batch_id", "master_course_id", "semester"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammeBatchCourse {

    @Id
    private String id;

    @Column(name = "master_course_id", nullable = false)
    private String masterCourseId;

    @Column(name = "programme_batch_id", nullable = false)
    private String programmeBatchId;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "course_coordinator_id")
    private Long courseCoordinatorId;

    @Column(name = "course_coordinator_name")
    private String courseCoordinatorName;

    @Column(name = "assigned_faculty", columnDefinition = "TEXT")
    private String assignedFaculty;

    @Column(name = "course_code_override", length = 50)
    private String courseCodeOverride;

    @Column(name = "course_name_override", length = 255)
    private String courseNameOverride;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Transient
    private String coordinatorEmail;

    @Transient
    private String courseCode;

    @Transient
    private String courseName;

    @Transient
    private String academicYear;

    @Transient
    public String getCoordinator() {
        return courseCoordinatorName;
    }

    public void setCoordinator(String coordinator) {
        this.courseCoordinatorName = coordinator;
    }

    // Helper compatibility methods
    public String getCourseId() {
        return masterCourseId;
    }

    public void setCourseId(String courseId) {
        this.masterCourseId = courseId;
    }

    public String getBatchId() {
        return programmeBatchId;
    }

    public void setBatchId(String batchId) {
        this.programmeBatchId = batchId;
    }

    public String getEffectiveCourseCode(MasterCourse masterCourse) {
        if (courseCodeOverride != null && !courseCodeOverride.isBlank()) {
            return courseCodeOverride;
        }
        return masterCourse != null ? masterCourse.getCode() : "";
    }

    public String getEffectiveCourseName(MasterCourse masterCourse) {
        if (courseNameOverride != null && !courseNameOverride.isBlank()) {
            return courseNameOverride;
        }
        return masterCourse != null ? masterCourse.getName() : "";
    }
}
