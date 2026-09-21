package com.dypiu.nba.dto.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoIndirectEvidenceResponseDto {
    // Course Context
    private String programmeBatchCourseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private String batchName;
    private String programmeName;
    private String departmentName;
    private String schoolName;
    private String courseCoordinatorName;

    // Survey & Assessment Metadata
    private String assessmentMethod;
    private Integer totalSurveyResponses;
    private BigDecimal indirectWeight;
    private BigDecimal overallIndirectAttainment;
    private String selectedCoCode;

    // Outcome Evidence
    private List<CoIndirectEvidenceItemDto> coEvidence;
}
