package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchOverviewResponseDto {

    private BatchContextDto batch;
    private List<PoHealthDto> poHealth;
    private List<PsoHealthDto> psoHealth;
    private SummaryCountsDto summary;
    private List<AttentionAreaItemDto> attentionAreas;
    private DirectIndirectDto directIndirect;
    private List<CourseContributionItemDto> courseContributions;
    private ProgrammeIndirectSummaryDto programmeIndirect;
    private ProgrammeAtrSummaryDto programmeAtr;
    private CourseAtrSummaryDto courseAtr;
    private HistoricalNavigationDto historical;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchContextDto {
        private String programmeBatchId;
        private String batchName;
        private String status;
        private Integer startYear;
        private Integer endYear;
        private String academicYear;
        private Integer currentSemester;
        private SchoolSummary school;
        private DepartmentSummary department;
        private ProgrammeSummary programme;
        private String coordinatorName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchoolSummary {
        private String id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentSummary {
        private String id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgrammeSummary {
        private String id;
        private String code;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PoHealthDto {
        private String poCode;
        private String poStatement;
        private BigDecimal attainment;
        private BigDecimal target;
        private BigDecimal gap;
        private boolean targetMet;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PsoHealthDto {
        private String psoCode;
        private String psoStatement;
        private BigDecimal attainment;
        private BigDecimal target;
        private BigDecimal gap;
        private boolean targetMet;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryCountsDto {
        private int poEvaluated;
        private int poMet;
        private int poBelowTarget;
        private int psoEvaluated;
        private int psoMet;
        private int psoBelowTarget;
        private int totalDeficits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectIndirectDto {
        @Builder.Default
        private BigDecimal programmeDirectWeight = new BigDecimal("0.80");
        @Builder.Default
        private BigDecimal programmeIndirectWeight = new BigDecimal("0.20");
        private BigDecimal programmeDirect;
        private BigDecimal programmeIndirect;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseContributionItemDto {
        private String programmeBatchCourseId;
        private String courseCode;
        private String courseName;
        private Integer semester;
        private String courseCoordinator;
        private BigDecimal overallCourseAttainment;
        private Map<String, BigDecimal> poContributions;
        private Map<String, BigDecimal> psoContributions;
        private Map<String, BigDecimal> poMappingStrength;
        private Map<String, BigDecimal> psoMappingStrength;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgrammeIndirectSummaryDto {
        private int assessmentCount;
        private boolean hasExitSurvey;
        private Map<String, BigDecimal> exitSurveyScores;
        private Map<String, BigDecimal> consolidatedIndirectAttainment;
        private Map<String, Integer> evaluationCounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgrammeAtrSummaryDto {
        private boolean exists;
        private String status;
        private boolean revisionRequired;
        private String verificationComments;
        private String observations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseAtrSummaryDto {
        private int totalCourses;
        private int coursesWithAtr;
        private int totalRecords;
        private int draftCount;
        private int submittedCount;
        private int submittedForVerificationCount;
        private int pendingApprovalCount;
        private int verifiedCount;
        private int approvedCount;
        private int needsRevisionCount;
        private int revisionRequestedCount;
        private int rejectedCount;
        private int revisionRequiredCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalNavigationDto {
        private String masterProgrammeId;
        private boolean available;
        private int batchCount;
    }
}
