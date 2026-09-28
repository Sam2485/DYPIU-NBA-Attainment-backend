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
public class CourseImportItemDto {
    private int rowNumber;
    private String sheetName;
    private int semester;
    private String courseCode;
    private String courseName;
    private Integer credits;
    private String courseType;
    private String coordinatorRaw;
    private Long coordinatorId;
    private String coordinatorName;
    private String coordinatorEmail;
    @Builder.Default
    private boolean coordinatorMatched = false;
    @Builder.Default
    private String status = "VALID"; // VALID, WARNING, DUPLICATE, INVALID
    @Builder.Default
    private List<String> issues = new ArrayList<>();
}
