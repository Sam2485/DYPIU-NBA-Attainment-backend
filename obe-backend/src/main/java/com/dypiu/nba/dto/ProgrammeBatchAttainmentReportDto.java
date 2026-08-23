package com.dypiu.nba.dto;

import com.dypiu.nba.entity.ReportStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeBatchAttainmentReportDto {

    private String id;
    private String batchId;
    private String batchName;
    private String masterProgrammeId;
    private String programmeName;
    private String programmeCode;
    private ReportStatus status;
    private BigDecimal overallProgrammeAttainment;

    // Report 1: Average Mapping Report (Course/Semester level articulation & final average)
    private List<Report1Row> report1AverageMapping;

    // Report 2: Direct Attainment Report (Course/Semester level Table 2 contributions & final direct attainment)
    private List<Report2Row> report2DirectAttainment;

    // Report 3: Indirect Attainment Report (Programme End Survey PO/PSO results)
    private List<Report3Row> report3IndirectAttainment;

    // Report 4: Overall Programme Attainment Report (80% Direct + 20% Indirect per PO/PSO, targets & observations)
    private List<Report4Row> report4OverallAttainment;

    private String submittedBy;
    private ZonedDateTime submittedAt;
    private String approvedBy;
    private ZonedDateTime approvedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SemesterContribution {
        private Integer semester;
        private BigDecimal value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report1Row {
        private String outcomeCode; // e.g. "PO1", "PSO1"
        private List<SemesterContribution> semesterAverages;
        private BigDecimal programmeAverageMapping;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report2Row {
        private String outcomeCode;
        private List<SemesterContribution> semesterDirectAttainments;
        private BigDecimal programmeDirectAttainment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report3Row {
        private String outcomeCode;
        private BigDecimal percentageSubstantial; // % giving 3
        private BigDecimal percentageModerate;    // % giving 2
        private BigDecimal percentageSlight;      // % giving 1
        private BigDecimal weightedScore;
        private BigDecimal indirectPercentage;
        private BigDecimal indirectAttainmentLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report4Row {
        private String outcomeCode;
        private String statement;
        private BigDecimal targetLevel;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal finalAttainment; // 80% Direct + 20% Indirect
        private Boolean targetMet;
        private String observation;
    }
}
