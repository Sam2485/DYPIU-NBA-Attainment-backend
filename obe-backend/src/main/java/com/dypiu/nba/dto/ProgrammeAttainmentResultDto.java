package com.dypiu.nba.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeAttainmentResultDto {
    private ProgrammeSummary programme;
    private BatchSummary batch;
    private Summary summary;
    private MappingBreakdown averageMapping;
    private DirectAttainmentBreakdown averageDirectAttainment;
    private Map<String, BigDecimal> averageIndirectAttainment;
    private OverallAttainmentBreakdown overallAttainment;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProgrammeSummary {
        private String id;
        private String code;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BatchSummary {
        private String id;
        private String name;
        private String startYear;
        private String endYear;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Summary {
        private int courseOfferingCount;
        private int semesterCount;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class MappingBreakdown {
        private List<OutcomeMappingItem> pos;
        private List<OutcomeMappingItem> psos;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public static class OutcomeMappingItem {
        private String poCode;
        private String psoCode;
        private List<SemesterValue> semesterValues;
        private BigDecimal overallAverage;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SemesterValue {
        private Integer semester;
        private BigDecimal averageMapping;
        private BigDecimal averageAttainment;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DirectAttainmentBreakdown {
        private List<OutcomeDirectItem> pos;
        private List<OutcomeDirectItem> psos;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public static class OutcomeDirectItem {
        private String poCode;
        private String psoCode;
        private List<SemesterValue> semesterValues;
        private BigDecimal overallAverage;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OverallAttainmentBreakdown {
        private List<OutcomeAttainmentItem> pos;
        private List<OutcomeAttainmentItem> psos;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    public static class OutcomeAttainmentItem {
        private String poCode;
        private String psoCode;
        private String outcomeCode;
        private String outcomeStatement;
        private BigDecimal target;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal directWeight;
        private BigDecimal indirectWeight;
        private BigDecimal overallAttainment;
        private BigDecimal achievementPercentage;
        private String observation;
        private List<String> actions;
    }

    public Map<String, BigDecimal> getPoAttainments() {
        Map<String, BigDecimal> map = new java.util.LinkedHashMap<>();
        if (overallAttainment != null && overallAttainment.getPos() != null) {
            for (OutcomeAttainmentItem item : overallAttainment.getPos()) {
                String code = item.getPoCode() != null ? item.getPoCode() : item.getOutcomeCode();
                if (code != null) map.put(code, item.getOverallAttainment());
            }
        }
        return map;
    }

    public Map<String, BigDecimal> getPsoAttainments() {
        Map<String, BigDecimal> map = new java.util.LinkedHashMap<>();
        if (overallAttainment != null && overallAttainment.getPsos() != null) {
            for (OutcomeAttainmentItem item : overallAttainment.getPsos()) {
                String code = item.getPsoCode() != null ? item.getPsoCode() : item.getOutcomeCode();
                if (code != null) map.put(code, item.getOverallAttainment());
            }
        }
        return map;
    }
}
