package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtrGapDetailDto {

    private String id;
    private String masterProgrammeId;
    private String programmeName;
    private String programmeCode;
    private String programmeBatchId;
    private String batchName;
    private String departmentName;
    private String outcomeCode;
    private String outcomeType; // "PO" | "PSO"
    private String outcomeStatement;
    private BigDecimal configuredTarget;
    private BigDecimal attainedValue;
    private BigDecimal gap;
    private BigDecimal achievementPercentage;
    private boolean hasRecordedAtr;
    private String atrStatus;
    private String recordedObservations;
    private List<String> recordedActions;
    private String submittedBy;
    private ZonedDateTime submittedAt;
    private String verifiedBy;
    private ZonedDateTime verifiedAt;
    private String approvedBy;
    private ZonedDateTime approvedAt;
    private String verificationComments;
}
