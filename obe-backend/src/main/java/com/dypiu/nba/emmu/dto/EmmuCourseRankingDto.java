package com.dypiu.nba.emmu.dto;

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
public class EmmuCourseRankingDto {

    private String masterProgrammeId;
    private String programmeName;
    private String programmeBatchId;
    private String batchName;
    private int totalCourses;
    private int evaluatedCourses;
    private String sortBy;
    private String direction;
    private List<CourseRankItemDto> rankedCourses;
    private String provenance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseRankItemDto {
        private int rank;
        private String programmeBatchCourseId;
        private String masterCourseId;
        private String courseCode;
        private String courseName;
        private Integer semester;
        private String coordinatorName;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal overallAttainment;
        private int totalCos;
        private int cosMeetingTarget;
        private int cosBelowTarget;
        private boolean hasEvaluatedMarks;
    }
}
