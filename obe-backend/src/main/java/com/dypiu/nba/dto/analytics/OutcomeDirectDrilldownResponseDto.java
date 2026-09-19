package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutcomeDirectDrilldownResponseDto {

    private String programmeBatchId;
    private String batchName;
    private String outcomeCode;
    private String outcomeType;
    private String outcomeStatement;
    private BigDecimal directAttainment;
    private BigDecimal target;
    private BigDecimal directGap;
    private boolean targetMet;
    private int contributingCourseCount;
    private List<OutcomeDirectCourseDto> courses;
}
