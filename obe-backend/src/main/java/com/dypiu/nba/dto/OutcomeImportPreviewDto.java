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
public class OutcomeImportPreviewDto {
    private boolean valid;
    private String programmeBatchId;
    private String programmeBatchName;
    private String masterProgrammeName;
    private int totalOutcomes;
    private int totalPOs;
    private int totalPSOs;
    private int totalCompetencies;
    private int validCount;
    private int warningCount;
    private int errorCount;

    @Builder.Default
    private List<OutcomeImportItemDto> items = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
