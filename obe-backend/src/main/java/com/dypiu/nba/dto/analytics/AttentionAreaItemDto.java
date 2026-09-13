package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttentionAreaItemDto {

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
    private String recordedAtrObservations;
    private List<CourseAssessmentEvidenceDto> contributingCourseEvidence;
}
