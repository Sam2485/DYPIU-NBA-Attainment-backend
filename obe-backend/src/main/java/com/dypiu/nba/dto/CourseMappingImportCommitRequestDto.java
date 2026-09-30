package com.dypiu.nba.dto;

import com.dypiu.nba.entity.CoPoMapping;
import com.dypiu.nba.entity.CoPsoMapping;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseMappingImportCommitRequestDto {
    private String scope; // "ALL", "PO", "PSO"
    private Map<String, Object> poKeywordsStore;
    private Map<String, Object> psoKeywordsStore;
    private List<CoPoMapping> poMappings;
    private List<CoPsoMapping> psoMappings;
}
