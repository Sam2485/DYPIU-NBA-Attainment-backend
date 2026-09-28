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
public class CourseImportPreviewDto {
    private boolean valid;
    private int totalCourses;
    private int validCount;
    private int warningCount;
    private int errorCount;
    private int maxSemesters;
    private int durationYears;
    private String programmeBatchId;
    private String programmeBatchName;
    private String masterProgrammeName;

    @Builder.Default
    private List<String> sheetsDetected = new ArrayList<>();

    @Builder.Default
    private Map<Integer, List<CourseImportItemDto>> coursesBySemester = new LinkedHashMap<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
