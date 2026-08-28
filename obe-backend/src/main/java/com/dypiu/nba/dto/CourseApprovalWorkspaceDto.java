package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourseApprovalWorkspaceDto {
    private Map<String, Object> programmeBatchCourse;
    private List<ApprovalItemDto> approvalItems;
}
