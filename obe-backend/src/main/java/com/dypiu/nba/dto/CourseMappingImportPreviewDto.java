package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseMappingImportPreviewDto {
    private boolean valid;
    private String programmeBatchCourseId;
    private String courseCode;
    private String courseName;
    private String programmeBatchId;
    private String programmeBatchName;
    private String scope; // "ALL", "PO", "PSO"

    // Verification stats
    private int expectedCoCount;
    private int sheetCoCount;
    private boolean cosMatch;

    private int expectedPoCount;
    private int sheetPoCount;
    private boolean posMatch;

    private int expectedPsoCount;
    private int sheetPsoCount;
    private boolean psosMatch;

    private int expectedCompetencyCount;
    private int sheetCompetencyCount;
    private boolean competenciesMatch;

    private int totalKeywordsExtracted;
    private int totalMappingsFound;

    @Builder.Default
    private List<String> sheetNames = new ArrayList<>();

    @Builder.Default
    private List<String> detectedCoCodes = new ArrayList<>();

    @Builder.Default
    private List<String> expectedCoCodes = new ArrayList<>();

    @Builder.Default
    private Map<String, String> coCodeMapping = new LinkedHashMap<>(); // e.g. "CO1" -> "EM321.1"

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    // Extracted payload ready for preview or commit
    @Builder.Default
    private Map<String, Object> poKeywordsStore = new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Object> psoKeywordsStore = new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Map<String, Object>> matrix = new LinkedHashMap<>(); // CO -> PO/PSO -> level (1, 2, 3, or "-")

    @Builder.Default
    private List<com.dypiu.nba.entity.CoPoMapping> poMappings = new ArrayList<>();

    @Builder.Default
    private List<com.dypiu.nba.entity.CoPsoMapping> psoMappings = new ArrayList<>();

    // Rich outcome & competency preview items for UI inspection (like HOD PO/PSO inspect)
    @Builder.Default
    private List<OutcomeMappingPreviewItem> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OutcomeMappingPreviewItem {
        private String id;
        private String category; // "PO" or "PSO"
        private String code; // "PO1", "PO2", "PSO1"
        private String statement;
        private int rowNumber;
        @Builder.Default
        private List<CompetencyMappingPreviewItem> competencies = new ArrayList<>();
        @Builder.Default
        private Map<String, Integer> mappingStrengths = new LinkedHashMap<>(); // CO1 -> 3, CO2 -> 2
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CompetencyMappingPreviewItem {
        private String id;
        private String code; // "PO1.1", "PSO1.1"
        private String statement;
        @Builder.Default
        private Map<String, List<String>> keywordsByCo = new LinkedHashMap<>(); // CO1 -> ["Modes of heat transfer"], CO2 -> [...]
    }
}
