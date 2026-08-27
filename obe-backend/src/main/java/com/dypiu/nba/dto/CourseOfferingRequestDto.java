package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseOfferingRequestDto {

    private String programmeBatchId;
    private String masterCourseId;
    private String courseCodeOverride;
    private String courseNameOverride;
    private Integer semester;
    private String academicYear;
    private Long courseCoordinatorId;
    private String courseCoordinatorName;
    private String courseCoordinatorEmail;
    private String coordinatorEmail;
    private String coordinator;
    private Object assignedFaculty;
}
