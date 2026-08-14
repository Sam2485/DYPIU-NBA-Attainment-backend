package com.dypiu.nba.dto;

import com.dypiu.nba.entity.Course;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCoordinatorSummaryDto {
    private String coordinatorName;
    private String coordinatorEmail;
    private int assignedCourseCount;
    private int completedWorkflowCount;
    private int pendingAttainmentCount;
    private int pendingAtrCount;
    private List<Course> assignedCourses;
}
