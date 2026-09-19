package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutcomeIndirectDrilldownResponseDto {

    private String programmeBatchId;
    private String batchName;
    private String outcomeCode;
    private String outcomeType;
    private String outcomeStatement;

    private BigDecimal indirectAttainment;
    private BigDecimal target;
    private BigDecimal indirectGap;
    private boolean targetMet;

    private int totalEvidenceCount;
    private int participatingEvidenceCount;

    private List<OutcomeIndirectEvidenceItemDto> evidence;
}
