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
public class ApprovalActionResultDto {
    private String approvalRequestId;
    private String type;
    private String status;
    private Map<String, Object> reviewedBy;
    private ZonedDateTime reviewedAt;
    private String revisionReason;
}
