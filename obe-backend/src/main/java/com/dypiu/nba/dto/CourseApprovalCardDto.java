package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseApprovalCardDto {
    private String programmeBatchCourseId;
    private String masterCourseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private Integer pendingApprovalCount;
    private Integer reviewedApprovalCount;
    private List<ApprovalItemDto> approvalItems;
    private Map<String, Object> submittedBy;
    private ZonedDateTime latestSubmittedAt;
}
