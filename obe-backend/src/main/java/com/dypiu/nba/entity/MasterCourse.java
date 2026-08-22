package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(
        name = "master_courses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_programme_code",
                        columnNames = {"master_programme_id", "code"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterCourse {

    @Id
    private String id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "master_programme_id", nullable = false)
    private String masterProgrammeId;

    @Column(nullable = false)
    private Integer credits;

    @Column(name = "course_type", length = 50)
    private String courseType;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;

    @Transient
    private String coordinator;

    @Transient
    private String coordinatorEmail;

    @Transient
    private String faculty;

    @Transient
    private String programmeBatchCourseId;

    @Transient
    private String assignedFaculty;

    @Transient
    private String semester;

    @Transient
    private String academicYear;

    @Transient
    private List<CourseOutcome> courseOutcomes;

    @Transient
    public String getType() {
        return courseType;
    }

    public void setType(String type) {
        if (type != null) {
            this.courseType = type;
        }
    }

    // Helper compatibility methods
    public String getProgrammeId() {
        return masterProgrammeId;
    }

    public void setProgrammeId(String programmeId) {
        this.masterProgrammeId = programmeId;
    }

    public String getCourseOfferingId() {
        return programmeBatchCourseId;
    }

    public void setCourseOfferingId(String offeringId) {
        this.programmeBatchCourseId = offeringId;
    }
}
