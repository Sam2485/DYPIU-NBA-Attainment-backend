package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutcomeCompetencyImportDto {
    private String id;
    private String code;       // e.g. "PO1.1", "PSO1.1"
    private String statement;  // Competency statement
    private int order;         // 1-indexed order within outcome
}
