package com.dypiu.nba.emmu.dto;

import com.dypiu.nba.dto.CourseAtrReportDto;
import com.dypiu.nba.dto.analytics.StudentCoEvidenceResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmmuCourseIntelligenceDto {

    private String programmeBatchCourseId;
    private String masterCourseId;
    private String courseCode;
    private String courseName;
    private Integer semester;
    private String courseCoordinatorName;
    private String masterProgrammeId;
    private String programmeName;
    private String programmeBatchId;
    private String batchName;
    private BigDecimal directAttainment;
    private BigDecimal indirectAttainment;
    private BigDecimal overallAttainment;
    private List<CourseOutcomeIntelligenceDto> cos;
    private List<StudentCoEvidenceResponseDto> studentEvidenceList;
    private CourseAtrReportDto courseAtr;
    private String provenance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseOutcomeIntelligenceDto {
        private String coCode;
        private String statement;
        private BigDecimal targetLevel;
        private Integer directLevel;
        private Integer indirectLevel;
        private BigDecimal finalAttainment;
        private Boolean targetMet;
        private Map<String, Integer> poMappings;
        private Map<String, Integer> psoMappings;
    }
}
