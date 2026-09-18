package com.dypiu.nba.dto.analytics;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsKpiResponseDto {

    private ScopeSummary scopeSummary;
    private OutcomeTargetAchievement poTargetAchievement;
    private OutcomeTargetAchievement psoTargetAchievement;
    private ProgrammeCohortHealth programmeCohortHealth;
    private AtrOperationalSummary atrOperationalSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeSummary {
        private long totalSchools;
        private long totalDepartments;
        private long totalMasterProgrammes;
        private long totalBatches;
        private long totalEvaluatedBatches;
        private long totalEvaluatedCourseOfferings;
        private String dataSourceCurrency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutcomeTargetAchievement {
        private int totalEvaluatedInstances;
        private int targetMetInstances;
        private int targetDeficitInstances;
        private BigDecimal achievementRatePercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgrammeCohortHealth {
        private int totalEvaluatedCohorts;
        private int cohortsFullyMeetingTargets;
        private int cohortsWithGaps;
        private BigDecimal fullyMeetingTargetRatePercentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtrOperationalSummary {
        private long totalRecordedProgrammeAtrs;
        private long approvedProgrammeAtrs;
        private long submittedProgrammeAtrs;
        private long draftProgrammeAtrs;
        private long revisionRequestedProgrammeAtrs;
        private long rejectedProgrammeAtrs;
    }
}
