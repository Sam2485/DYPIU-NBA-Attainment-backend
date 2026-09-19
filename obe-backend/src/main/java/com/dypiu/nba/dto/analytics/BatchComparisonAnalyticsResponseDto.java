package com.dypiu.nba.dto.analytics;

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
public class BatchComparisonAnalyticsResponseDto {

    private BatchMetaDto batch1;
    private BatchMetaDto batch2;
    private List<OutcomeComparisonItemDto> outcomes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchMetaDto {
        private String programmeId;
        private String programmeName;
        private String batchId;
        private String batchName;
        private Integer startYear;
        private Integer endYear;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutcomeMetricsDto {
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal finalAttainment;
        private BigDecimal targetLevel;
        private BigDecimal gap;
        private Boolean targetMet;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutcomeComparisonItemDto {
        private String outcomeCode;
        private String outcomeType; // "PO" or "PSO"
        private OutcomeMetricsDto batch1;
        private OutcomeMetricsDto batch2;
    }
}
