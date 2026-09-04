package com.dypiu.nba.reports.model.snapshot;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAtrSnapshot extends ReportSnapshot {

    private String courseAtrId;
    private String programmeBatchCourseId;
    private String masterCourseId;
    private String courseCode;
    private String courseName;
    private Integer semester;

    private String programmeBatchId;
    private String batchName;
    private String status;

    private List<AtrOutcomeRow> outcomes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtrOutcomeRow {
        private String outcomeCode;
        private String outcomeStatement;
        private BigDecimal targetLevel;
        private BigDecimal attainmentLevel;
        private BigDecimal achievementPercentage;
        private List<String> actions;
    }
}
