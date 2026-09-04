package com.dypiu.nba.reports.model.snapshot;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAttainmentSnapshot extends ReportSnapshot {

    private String programmeBatchCourseId;
    private String masterCourseId;
    private String courseCode;
    private String courseName;
    private Integer semester;

    private String programmeBatchId;
    private String batchName;

    private BigDecimal overallCoAttainment;
    private BigDecimal directAttainment;
    private BigDecimal indirectAttainment;

    private List<String> poCodes;
    private List<String> psoCodes;

    private List<CoMappingRow> table1Mapping;
    private List<OutcomeContributionRow> table2DirectPO;
    private List<OutcomeContributionRow> table2DirectPSO;
    private List<CoAttainmentRow> table3CoAttainments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoMappingRow {
        private String coCode;
        private Map<String, Integer> poMappings;
        private Map<String, Integer> psoMappings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutcomeContributionRow {
        private String outcomeCode;
        private BigDecimal averageMapping;
        private BigDecimal directContribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoAttainmentRow {
        private String coCode;
        private String statement;
        private BigDecimal targetLevel;
        private BigDecimal directPercentage;
        private Integer directLevel;
        private BigDecimal indirectPercentage;
        private BigDecimal indirectScore;
        private Integer indirectLevel;
        private BigDecimal finalAttainment;
        private Boolean targetMet;
    }
}
