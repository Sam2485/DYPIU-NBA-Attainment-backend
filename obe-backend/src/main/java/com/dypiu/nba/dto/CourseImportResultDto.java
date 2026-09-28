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
public class CourseImportResultDto {
    private int totalCoursesImported;
    @Builder.Default
    private List<Integer> semestersProcessed = new ArrayList<>();
    private int coordinatorsMatched;
    private int coordinatorsUnmatched;
    @Builder.Default
    private List<String> courseCodes = new ArrayList<>();
    private String programmeBatchId;
}
