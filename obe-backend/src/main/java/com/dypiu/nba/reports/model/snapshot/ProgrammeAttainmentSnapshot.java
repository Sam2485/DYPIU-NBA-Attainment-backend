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
public class ProgrammeAttainmentSnapshot extends ReportSnapshot {

    private String masterProgrammeId;
    private String masterProgrammeCode;
    private String masterProgrammeName;

    private String programmeBatchId;
    private String programmeBatchName;
    private String academicBatchYears;

    private List<String> poCodes;
    private List<String> psoCodes;

    private AverageMappingSection section1AverageMapping;
    private AverageDirectSection section2AverageDirect;
    private AverageIndirectSection section3AverageIndirect;
    private OverallAttainmentSection section4OverallAttainment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AverageMappingSection {
        private List<CourseMappingRow> courses;
        private Map<String, BigDecimal> averageMappingStrength;
        private BigDecimal overallAverageMappingStrength;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseMappingRow {
        private String programmeBatchCourseId;
        private String courseCode;
        private String courseName;
        private Integer semester;
        private Map<String, BigDecimal> poValues;
        private Map<String, BigDecimal> psoValues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AverageDirectSection {
        private List<CourseDirectRow> courses;
        private Map<String, BigDecimal> averageDirectAttainment;
        private BigDecimal overallDirectAttainment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseDirectRow {
        private String programmeBatchCourseId;
        private String courseCode;
        private String courseName;
        private Integer semester;
        private Map<String, BigDecimal> poValues;
        private Map<String, BigDecimal> psoValues;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AverageIndirectSection {
        private String surveyType;
        private Integer totalStudents;
        private List<StudentSurveyRow> studentResponses;
        private Map<String, BigDecimal> averageIndirectAttainment;
        private BigDecimal overallIndirectAttainment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSurveyRow {
        private Integer srNo;
        private String prn;
        private String studentName;
        private Map<String, BigDecimal> poRatings;
        private Map<String, BigDecimal> psoRatings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverallAttainmentSection {
        private BigDecimal directWeightPercentage;
        private BigDecimal indirectWeightPercentage;
        private Map<String, BigDecimal> averageMappingStrength;
        private Map<String, BigDecimal> averageDirectAttainment;
        private Map<String, BigDecimal> averageIndirectAttainment;
        private Map<String, BigDecimal> finalAttainments;
        private BigDecimal overallProgrammeAttainment;
    }
}
