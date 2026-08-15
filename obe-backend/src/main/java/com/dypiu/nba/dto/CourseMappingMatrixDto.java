package com.dypiu.nba.dto;

import com.dypiu.nba.entity.CoPoMapping;
import com.dypiu.nba.entity.CoPsoMapping;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseMappingMatrixDto {
    private String courseId;
    private String programmeId;
    private List<CoPoMapping> poMappings;
    private List<CoPsoMapping> psoMappings;
    private Map<String, Object> poKeywordsStore;
    private Map<String, Object> psoKeywordsStore;
}
