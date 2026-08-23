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

    @com.fasterxml.jackson.annotation.JsonProperty("programmeBatchAttainmentReportId")
    private String id;
    private String programmeBatchId;
    private String batchName;
    private String masterProgrammeId;
    private String programmeName;
    private String programmeCode;
    private ReportStatus status;
    private BigDecimal overallProgrammeAttainment;

    // Report 1: Average Mapping Report (Course/Semester level articulation & final average)
    private List<Report1PoRow> report1AverageMappingPO;
    private List<Report1PsoRow> report1AverageMappingPSO;

    // Report 2: Direct Attainment Report (Course/Semester level Table 2 contributions & final direct attainment)
    private List<Report2PoRow> report2DirectAttainmentPO;
    private List<Report2PsoRow> report2DirectAttainmentPSO;

    // Report 3: Indirect Attainment Report (Programme End Survey PO/PSO results)
    private List<Report3PoRow> report3IndirectAttainmentPO;
    private List<Report3PsoRow> report3IndirectAttainmentPSO;

    // Report 4: Overall Programme Attainment Report (80% Direct + 20% Indirect per PO/PSO, targets & observations)
    private List<Report4PoRow> report4OverallAttainmentPO;
    private List<Report4PsoRow> report4OverallAttainmentPSO;

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
    public static class Report1PoRow {
        private String poCode;
        private List<SemesterContribution> semesterAverages;
        private BigDecimal programmeAverageMapping;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report1PsoRow {
        private String psoCode;
        private List<SemesterContribution> semesterAverages;
        private BigDecimal programmeAverageMapping;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report2PoRow {
        private String poCode;
        private List<SemesterContribution> semesterDirectAttainments;
        private BigDecimal programmeDirectAttainment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report2PsoRow {
        private String psoCode;
        private List<SemesterContribution> semesterDirectAttainments;
        private BigDecimal programmeDirectAttainment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report3PoRow {
        private String poCode;
        private BigDecimal percentageSubstantial;
        private BigDecimal percentageModerate;
        private BigDecimal percentageSlight;
        private BigDecimal weightedScore;
        private BigDecimal indirectPercentage;
        private BigDecimal indirectAttainmentLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report3PsoRow {
        private String psoCode;
        private BigDecimal percentageSubstantial;
        private BigDecimal percentageModerate;
        private BigDecimal percentageSlight;
        private BigDecimal weightedScore;
        private BigDecimal indirectPercentage;
        private BigDecimal indirectAttainmentLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report4PoRow {
        private String poCode;
        private String statement;
        private BigDecimal targetLevel;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal finalAttainment;
        private Boolean targetMet;
        private String observation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Report4PsoRow {
        private String psoCode;
        private String statement;
        private BigDecimal targetLevel;
        private BigDecimal directAttainment;
        private BigDecimal indirectAttainment;
        private BigDecimal finalAttainment;
        private Boolean targetMet;
        private String observation;
    }
}
