package com.dypiu.nba.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchComparisonDto {
    private String masterProgrammeId;
    private List<BatchAttainmentItem> batches;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BatchAttainmentItem {
        private String programmeBatchId;
        private String batchName;
        private String programmeAtrStatus;
        private Map<String, BigDecimal> poAttainment;
        private Map<String, BigDecimal> psoAttainment;
    }
}
