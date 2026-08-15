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
    private List<Course> assignedCourses;
    private CourseCoordinatorSetupProgressDto setupProgress;
    private int courseOutcomesCount;
    private int poCount;
    private int psoCount;
}
