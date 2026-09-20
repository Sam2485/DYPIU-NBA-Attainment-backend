package com.dypiu.nba.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseComparisonAnalyticsResponseDto {

    private CourseMetaDto course1;
    private CourseMetaDto course2;
    private List<String> courseOutcomes;
    private List<CoComparisonItemDto> coComparisons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseMetaDto {
        private String programmeBatchCourseId;
        private String programmeBatchId;
        private String batchName;
        private Integer startYear;
        private Integer endYear;
        private String batchStatus;
        private String masterProgrammeId;
        private String programmeName;
        private String courseCode;
        private String courseName;
        private Integer semester;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal overallCourseAttainment;
        private BigDecimal directWeight;
        private BigDecimal indirectWeight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoMetricsDto {
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal overallAttainment;
        private BigDecimal targetLevel;
        private Boolean targetMet;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoComparisonItemDto {
        private String coCode;
        private String statement;
        private CoMetricsDto course1Metrics;
        private CoMetricsDto course2Metrics;
        private BigDecimal attainmentDelta; // course1.overallAttainment - course2.overallAttainment (pure mathematical delta)
    }
}
