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
    private String masterProgrammeId;
    private String programmeBatchId;
    private String surveyType;
    private int recordsProcessed;
    private List<PoIndirectItem> poIndirectAttainment;
    private List<PsoIndirectItem> psoIndirectAttainment;
    private String status;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PoIndirectItem {
        private String poCode;
        private BigDecimal indirectAttainment;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PsoIndirectItem {
        private String psoCode;
        private BigDecimal indirectAttainment;
    }
}
