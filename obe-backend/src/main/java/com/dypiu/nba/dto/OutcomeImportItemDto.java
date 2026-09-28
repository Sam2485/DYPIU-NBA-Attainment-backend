package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutcomeImportItemDto {
    private String id;
    private int rowNumber;
    private String category;         // "PO" or "PSO"
    private String originalCategory; // Original detection category: "PO" or "PSO"
    private String code;             // e.g. "PO1", "PSO1"
    private String statement;
    @Builder.Default
    private BigDecimal target = new BigDecimal("2.50");
    private String detectionRule;    // "EXPLICIT_PREFIX", "SECTION_HEADER", "NUMBERING_RESET", "NUMBER_GT_12", "SEQUENTIAL_NUMBER", "DEFAULT_SECTION"

    @Builder.Default
    private List<OutcomeCompetencyImportDto> competencies = new ArrayList<>();

    @Builder.Default
    private List<String> issues = new ArrayList<>();

    @Builder.Default
    private String status = "VALID"; // VALID, WARNING, INVALID
}
