package com.dypiu.nba.emmu.dto;

import com.dypiu.nba.dto.analytics.AnalyticsKpiResponseDto;
import com.dypiu.nba.dto.analytics.AttentionAreaItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmmuContextSummaryDto {

    private EmmuScopeDto scope;
    private EmmuDataCurrency dataCurrency;
    private EmmuCoverageDto coverage;
    private EmmuOutcomeSummaryDto outcomeSummary;
    private List<AttentionAreaItemDto> topDeficits;
    private AnalyticsKpiResponseDto.AtrOperationalSummary atrSummary;
    private EmmuPendingApprovalsSummaryDto pendingApprovals;
    private String provenance;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmmuScopeDto {
        private String role;
        private String userEmail;
        private String schoolId;
        private String schoolName;
        private String departmentId;
        private String departmentName;
        private String masterProgrammeId;
        private String programmeName;
        private String programmeBatchId;
        private String batchName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmmuCoverageDto {
        private int totalCourseOfferings;
        private int evaluatedCourseOfferings;
        private BigDecimal evaluationCoveragePercentage;
        private int totalStudentsEvaluated;
        private int indirectAssessmentCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmmuOutcomeSummaryDto {
        private int poEvaluated;
        private int poMeetingTarget;
        private int poDeficitCount;
        private int psoEvaluated;
        private int psoMeetingTarget;
        private int psoDeficitCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmmuPendingApprovalsSummaryDto {
        private long totalPending;
        private Map<String, Long> breakdown;
    }
}
