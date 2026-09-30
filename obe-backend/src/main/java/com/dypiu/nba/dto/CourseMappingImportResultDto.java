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
public class CourseMappingImportResultDto {
    private boolean success;
    private String programmeBatchCourseId;
    private String courseCode;
    private String scope;
    private int savedPoMappingsCount;
    private int savedPsoMappingsCount;
    private int savedPoKeywordsCount;
    private int savedPsoKeywordsCount;
    private String message;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    private CourseMappingMatrixDto mappingMatrix;
}
