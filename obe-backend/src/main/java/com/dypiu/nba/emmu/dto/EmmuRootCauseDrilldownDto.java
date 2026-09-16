package com.dypiu.nba.emmu.dto;

import com.dypiu.nba.dto.analytics.CohortOutcomeDataPointDto;
import com.dypiu.nba.dto.analytics.CourseAssessmentEvidenceDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmmuRootCauseDrilldownDto {

    private OutcomeDetailDto outcome;
    private EmmuDataCurrency dataCurrency;
    private EmmuContextSummaryDto.EmmuCoverageDto coverage;
    private DirectComponentDto direct;
    private IndirectComponentDto indirect;
    private List<CourseAssessmentEvidenceDto> courseEvidence;
    private List<StudentEvidenceSummaryDto> studentEvidenceSummary;
    private AtrEvidenceDetailDto atrEvidence;
    private List<CohortOutcomeDataPointDto> historicalEvidence;
    private String provenance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutcomeDetailDto {
        private String code;
        private String type; // "PO" or "PSO"
        private String statement;
        private BigDecimal target;
        private BigDecimal attainment;
        private BigDecimal gap;
        private BigDecimal achievementPercentage;
        private boolean targetMet;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectComponentDto {
        private BigDecimal attainment;
        private BigDecimal weight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndirectComponentDto {
        private BigDecimal attainment;
        private BigDecimal weight;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentEvidenceSummaryDto {
        private String courseOfferingId;
        private String courseCode;
        private String courseName;
        private String coCode;
        private int totalStudentsEvaluated;
        private int studentsMeetingThreshold;
        private int studentsBelowThreshold;
        private BigDecimal thresholdPercentage;
        private BigDecimal attainmentRatePercentage;
        private BigDecimal classAveragePercentage;
        private BigDecimal highestPercentage;
        private BigDecimal lowestPercentage;
        private Map<String, Integer> scoreDistribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtrEvidenceDetailDto {
        private boolean hasRecordedAtr;
        private String status;
        private String observations;
        private List<String> recordedActions;
        private String submittedBy;
        private ZonedDateTime submittedAt;
        private String verifiedBy;
        private ZonedDateTime verifiedAt;
        private String approvedBy;
        private ZonedDateTime approvedAt;
        private String verificationComments;
    }
}
