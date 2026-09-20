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
public class HistoricalProgrammeAttainmentResponseDto {

    private String masterProgrammeId;
    private String programmeName;
    private String currentBatchId;
    private List<HistoricalBatchSummaryDto> batches;
    private List<String> outcomes;
    private List<HistoricalOutcomeDataPointDto> dataPoints;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalBatchSummaryDto {
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
    public static class HistoricalOutcomeDataPointDto {
        private String batchId;
        private String batchName;
        private Integer startYear;
        private Integer endYear;
        private String status;
        private String outcomeCode;
        private String outcomeType; // "PO" or "PSO"
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal finalAttainment;
        private BigDecimal targetLevel;
        private BigDecimal gap;
        private Boolean targetMet;
    }
}
