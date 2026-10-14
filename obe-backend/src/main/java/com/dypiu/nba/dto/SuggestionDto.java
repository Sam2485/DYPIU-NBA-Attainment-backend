package com.dypiu.nba.dto;

import lombok.*;
import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestionDto {
    private String id;
    private Long senderId;
    private String senderRole;
    private String senderName;
    private Long recipientId;
    private String recipientRole;
    private String recipientName;
    private String schoolId;
    private String departmentId;
    private String masterProgrammeId;
    private String programmeName;
    private String programmeBatchId;
    private String batchName;
    private String programmeBatchCourseId;
    private String courseName;
    private String contextType;
    private String category;
    private String content;
    private String status;
    private ZonedDateTime readAt;
    private ZonedDateTime createdAt;
}
