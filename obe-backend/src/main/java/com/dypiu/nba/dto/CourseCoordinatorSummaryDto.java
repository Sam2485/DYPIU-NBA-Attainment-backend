package com.dypiu.nba.dto;

import com.dypiu.nba.entity.MasterCourse;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCoordinatorSummaryDto {
    private String schoolName;
    private String departmentName;
    private String programmeName;
    private String batchName;
    private String courseOfferingId;
    private String programmeBatchCourseId;
    private String coordinatorEmail;
    private String coordinatorName;
    private String courseCode;
    private String courseName;
    private int assignedCourseCount;
    private int courseOutcomesCount;
    private int poCount;
    private int psoCount;
    private int peoCount;
    private List<MasterCourse> assignedCourses;
    private CourseCoordinatorSetupProgressDto setupProgress;
}
