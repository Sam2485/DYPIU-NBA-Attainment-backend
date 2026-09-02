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
    private List<StudentSurveyResponseRow> studentSurveyResponses;
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

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class StudentSurveyResponseRow {
        private Integer srNo;
        private String prn;
        private String studentName;
        private java.util.Map<String, String> poRatings;
        private java.util.Map<String, String> psoRatings;
    }
}
