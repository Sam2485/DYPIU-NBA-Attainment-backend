package com.dypiu.nba.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutcomeImportResultDto {
    private boolean success;
    private String message;
    private String programmeBatchId;
    private int totalOutcomesImported;
    private int totalPOsImported;
    private int totalPSOsImported;
    private int totalCompetenciesImported;
    @Builder.Default
    private List<String> poCodes = new ArrayList<>();
    @Builder.Default
    private List<String> psoCodes = new ArrayList<>();
}
