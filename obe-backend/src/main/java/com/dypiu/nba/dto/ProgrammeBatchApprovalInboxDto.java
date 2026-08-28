package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProgrammeBatchApprovalInboxDto {
    private String programmeBatchId;
    private String programmeBatchName;
    private Integer totalPendingItems;
    private Integer totalReviewedItems;
    private Integer totalProgrammeBatchCourses;
    private List<CourseApprovalCardDto> courses;
}
