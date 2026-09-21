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
    private String courseCoordinatorName;

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
    private List<PoDetailRow> poDetails;
    private List<PsoDetailRow> psoDetails;
    private ExaminationSection examinationData;
    private SurveySection surveyData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoDetailRow {
        private String poCode;
        private String statement;
        private List<CompetencyDetailRow> competencies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PsoDetailRow {
        private String psoCode;
        private String statement;
        private List<CompetencyDetailRow> competencies;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompetencyDetailRow {
        private String competencyCode;
        private String statement;
        private Map<String, String> coKeywords;
        private Map<String, String> coMappings;
    }

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExaminationSection {
        private String courseName;
        private String className;
        private String academicYear;
        private Integer totalStudents;
        private BigDecimal thresholdPercentage;
        private List<String> coCodes;
        private Map<String, BigDecimal> coMaxMarks;
        private Map<String, BigDecimal> coThresholdMarks;
        private Map<String, Integer> studentsAboveThreshold;
        private Map<String, BigDecimal> percentageAboveThreshold;
        private List<StudentMarksRow> students;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentMarksRow {
        private Integer srNo;
        private String prn;
        private String studentName;
        private Map<String, BigDecimal> coMarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SurveySection {
        private Integer totalStudents;
        private List<String> coCodes;
        private Map<String, Integer> level1Counts; // Slight (1)
        private Map<String, Integer> level2Counts; // Moderate (2)
        private Map<String, Integer> level3Counts; // Substantial (3)
        private Map<String, BigDecimal> level1Percentages;
        private Map<String, BigDecimal> level2Percentages;
        private Map<String, BigDecimal> level3Percentages;
        private Map<String, BigDecimal> overallIndirectPercentages;
        private List<SurveyResponseRow> responses;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SurveyResponseRow {
        private Integer srNo;
        private Map<String, String> coFeedbacks; // "Slight", "Moderate", "Substantial"
    }
}
