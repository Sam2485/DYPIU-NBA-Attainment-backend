package com.dypiu.nba.dto;

import com.dypiu.nba.entity.CoPoMapping;
import com.dypiu.nba.entity.CoPsoMapping;
import com.dypiu.nba.entity.CourseOutcome;
import com.dypiu.nba.entity.ProgrammeOutcome;
import com.dypiu.nba.entity.ProgrammeSpecificOutcome;
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
    private List<CourseOutcome> cos;
    private List<ProgrammeOutcome> pos;
    private List<ProgrammeSpecificOutcome> psos;
    private List<CoPoMapping> poMappings;
    private List<CoPsoMapping> psoMappings;
    private Map<String, Object> poKeywordsStore;
    private Map<String, Object> psoKeywordsStore;
}
