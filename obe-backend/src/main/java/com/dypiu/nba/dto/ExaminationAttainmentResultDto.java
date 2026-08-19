package com.dypiu.nba.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExaminationAttainmentResultDto {
    private String courseId;
    private BigDecimal thresholdPercentage; // e.g. 45.00
    private Integer totalStudents; // e.g. 24
    private Map<String, BigDecimal> coMaxMarks; // e.g. {"CO1": 20, "CO2": 18, ...}
    private Map<String, BigDecimal> coThresholdMarks; // Fraction of out-of marks: maxMarks * threshold / 100
    private Map<String, Integer> studentsAboveThreshold; // Count of students with marks >= thresholdMarks
    private Map<String, BigDecimal> percentageAboveThreshold; // (studentsAboveThreshold / totalStudents) * 100
    private Map<String, Integer> coAttainmentLevels; // Score 1-3 based on percentage: >=60% -> 3, >=40% -> 2, >0% -> 1, 0% -> 0
    private BigDecimal overallDirectCoAttainment; // Average of CO attainment levels
    private List<StudentMarksRowDto> studentMarks;
    private Map<String, Object> fileDetails; // Saved file metadata: fileName, fileSize, savedPath, uploadedAt, status
}
