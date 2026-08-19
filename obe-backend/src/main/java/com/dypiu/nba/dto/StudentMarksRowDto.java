package com.dypiu.nba.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMarksRowDto {
    private Integer srNo;
    private String prn;
    private String studentName;
    private Map<String, BigDecimal> coMarks; // e.g. {"CO1": 18, "CO2": 12, "CO3": 22, ...}
}
