package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutcomeIndirectEvidenceItemDto {

    private String assessmentId;
    private String type; // "EVENT", "SURVEY", "CO_CURRICULAR", "EXIT_SURVEY"
    private String name;
    private String description;
    private ZonedDateTime date;
    private boolean outcomeEvaluated;
    private BigDecimal outcomeValue;
    private Integer responseCount; // for Exit Survey, recordsProcessed
    private String createdBy;
}
