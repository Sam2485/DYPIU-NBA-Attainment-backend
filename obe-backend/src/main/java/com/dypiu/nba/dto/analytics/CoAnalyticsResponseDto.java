package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoAnalyticsResponseDto {

    // 1. Course context
    private String programmeBatchId;
    private String programmeBatchCourseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private String courseCoordinator;
    private String batchName;
    private String masterProgrammeId;
    private String programmeName;

    // 2. CO context
    private String coCode;
    private String coStatement;
    private BigDecimal target;
    private Boolean targetMet;
    private BigDecimal directAttainment;
    private BigDecimal indirectAttainment;
    private BigDecimal overallAttainment;
    private BigDecimal directWeight;
    private BigDecimal indirectWeight;
    private String observation;

    // 3. PO/PSO mappings for this CO
    private Map<String, Integer> poMappings;
    private Map<String, Integer> psoMappings;

    // 4. Evidence summaries
    private CoDirectEvidenceSummaryDto directEvidenceSummary;
    private CoIndirectEvidenceSummaryDto indirectEvidenceSummary;
}
