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
public class HistoricalCourseAttainmentResponseDto {

    private String courseCode;
    private String courseName;
    private String masterProgrammeId;
    private String programmeName;

    private String currentProgrammeBatchCourseId;
    private String currentBatchId;
    private String currentBatchName;
    private String currentBatchStatus;
    private Integer currentSemester;

    private BigDecimal directWeight;
    private BigDecimal indirectWeight;

    private List<HistoricalCourseBatchDto> batches;
    private List<String> courseOutcomes;
    private List<HistoricalCoDataPointDto> coDataPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalCourseBatchDto {
        private String programmeBatchCourseId;
        private String programmeBatchId;
        private String batchName;
        private Integer startYear;
        private Integer endYear;
        private String status;
        private Integer semester;
        private String courseCode;
        private String courseName;
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
    public static class HistoricalCoDataPointDto {
        private String programmeBatchCourseId;
        private String programmeBatchId;
        private String batchName;
        private Integer startYear;
        private Integer endYear;
        private String status;
        private String coCode;
        private String statement;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal overallAttainment;
        private BigDecimal targetLevel;
        private Boolean targetMet;
    }
}
