package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "student_co_marks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_student_co_mark",
                        columnNames = {
                                "programme_batch_course_id",
                                "student_id",
                                "co_code"
                        }
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentCoMark {

    @Id
    private String id;

    @Column(name = "upload_id")
    private String uploadId;

    @Column(name = "programme_batch_course_id", nullable = false)
    private String programmeBatchCourseId;

    @Column(name = "student_id", nullable = false)
    private String studentId;

    @Column(nullable = false, length = 50)
    private String prn;

    private String studentName;

    @Column(name = "co_code", nullable = false, length = 30)
    private String coCode;

    @Column(name = "marks_obtained", nullable = false)
    private BigDecimal marksObtained;

    @Column(name = "max_marks", nullable = false)
    @Builder.Default
    private BigDecimal maxMarks = new BigDecimal("100.00");

    @Column(name = "percentage", insertable = false, updatable = false)
    private BigDecimal percentage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    // Helper compatibility methods
    public String getCourseOfferingId() {
        return programmeBatchCourseId;
    }

    public void setCourseOfferingId(String offeringId) {
        this.programmeBatchCourseId = offeringId;
    }
}
