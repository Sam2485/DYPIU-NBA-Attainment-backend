package com.dypiu.nba.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeSurveyResultDto {
    private String uploadId;
    private String programmeId;
    private String batchId;
    private String surveyType;
    private int recordsProcessed;
    private List<OutcomeIndirectItem> poIndirectAttainment;
    private List<OutcomeIndirectItem> psoIndirectAttainment;
    private String status;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OutcomeIndirectItem {
        private String outcomeCode;
        private BigDecimal indirectAttainment;
    }
}
