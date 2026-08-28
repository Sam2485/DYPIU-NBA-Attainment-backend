package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApprovalItemDto {
    private String approvalRequestId;
    private String type;
    private String status;
    private ZonedDateTime reviewedAt;
    private Map<String, Object> reviewedBy;
    private String revisionReason;
}
