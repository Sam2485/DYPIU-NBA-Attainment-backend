package com.dypiu.nba.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAtrReportDto {
    private String reportType;
    private String courseAtrId;
    private CourseOfferingSummary courseOffering;
    private CourseSummary course;
    private BatchSummary batch;
    private List<OutcomeRow> outcomes;
    private String status;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CourseOfferingSummary {
        private String id;
        private String masterCourseId;
        private String programmeBatchId;
        private Integer semester;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CourseSummary {
        private String id;
        private String code;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BatchSummary {
        private String id;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OutcomeRow {
        private String outcomeCode;
        private String outcomeStatement;
        private BigDecimal targetLevel;
        private BigDecimal attainmentLevel;
        private BigDecimal achievementPercentage;
        private String observation;
        private List<String> actions;
    }
}
