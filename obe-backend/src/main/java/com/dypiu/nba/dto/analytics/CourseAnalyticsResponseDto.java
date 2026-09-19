package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnalyticsResponseDto {

    // 1. Context
    private String programmeBatchId;
    private String programmeBatchCourseId;
    private String batchName;
    private String masterProgrammeId;
    private String programmeName;
    private String schoolId;
    private String schoolName;
    private String departmentId;
    private String departmentName;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private String courseCoordinator;
    private String courseCoordinatorEmail;
    private String masterCourseId;

    // 2. Course Attainment & Weighting
    private BigDecimal overallCourseAttainment;
    private BigDecimal directAttainment;
    private BigDecimal indirectAttainment;
    private BigDecimal directWeight;
    private BigDecimal indirectWeight;
    private BigDecimal directThreshold;
    private BigDecimal indirectThreshold;
    private String attainmentStatus;

    // 3. Selected PO/PSO
    private SelectedOutcomeContributionDto selectedOutcome;

    // 4. Mapped Outcome Collections
    private List<OutcomeContributionItemDto> outcomes;
    private List<OutcomeContributionItemDto> poContributions;
    private List<OutcomeContributionItemDto> psoContributions;

    // 5. Course CO Overview Table
    private List<CourseCoOverviewItemDto> courseOutcomes;

    // 6. Articulation Matrix
    private Map<String, Map<String, Integer>> mappingMatrix;

    // 7. ATR Status Context
    private Boolean courseAtrAvailable;
    private String courseAtrStatus;
}
