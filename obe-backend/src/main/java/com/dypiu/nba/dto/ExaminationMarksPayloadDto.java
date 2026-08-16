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
public class ExaminationMarksPayloadDto {
    private String courseId;
    private BigDecimal thresholdPercentage; // e.g. 45
    private Map<String, BigDecimal> coMaxMarks; // e.g. {"CO1": 20, "CO2": 18, "CO3": 22, ...}
    private List<StudentMarksRowDto> studentMarks;
}
