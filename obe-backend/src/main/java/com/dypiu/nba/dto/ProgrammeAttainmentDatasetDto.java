package com.dypiu.nba.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeAttainmentDatasetDto {
    private String programmeId;
    private String batchId;
    private TableData averageMapping;
    private TableData averageDirectAttainment;
    private Map<String, BigDecimal> averageIndirectAttainment;
    private Map<String, BigDecimal> overallAttainment;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TableData {
        private List<String> columns;
        private List<Map<String, Object>> rows;
    }
}
