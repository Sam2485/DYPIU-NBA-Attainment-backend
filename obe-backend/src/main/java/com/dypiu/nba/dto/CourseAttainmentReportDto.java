package com.dypiu.nba.dto;

import com.dypiu.nba.entity.ReportStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAttainmentReportDto {

    @com.fasterxml.jackson.annotation.JsonProperty("courseAttainmentReportId")
    private String id;
    private String programmeBatchCourseId;
    private String masterCourseId;
    private String courseCode;
    private String courseName;
    private String programmeBatchId;
    private String batchName;
    private Integer semester;
    private ReportStatus status;

    private BigDecimal overallCoAttainment;
    private BigDecimal directAttainment;
    private BigDecimal indirectAttainment;

    // Table 1: CO -> PO/PSO Articulation Matrix
    private List<Table1Row> table1Mapping;

    // Table 2: Course PO/PSO Direct Attainment Contribution (Average Mapping * Overall CO Attainment / 3)
    private List<Table2PoRow> table2DirectPO;
    private List<Table2PsoRow> table2DirectPSO;

    // Table 3: CO Direct + Indirect + Final CO Attainment (80% Direct + 20% Indirect)
    private List<Table3Row> table3CoAttainments;

    private String submittedBy;
    private ZonedDateTime submittedAt;
    private String approvedBy;
    private ZonedDateTime approvedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Table1Row {
        private String coCode;
        private Map<String, Integer> poMappings; // e.g. "PO1" -> 3, "PO2" -> 2
        private Map<String, Integer> psoMappings; // e.g. "PSO1" -> 2
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Table2PoRow {
        private String poCode;
        private BigDecimal averageMapping;
        private BigDecimal directContribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Table2PsoRow {
        private String psoCode;
        private BigDecimal averageMapping;
        private BigDecimal directContribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Table3Row {
        private String coCode;
        private String statement;
        private BigDecimal targetLevel;
        private BigDecimal directPercentage;
        private Integer directLevel;
        private BigDecimal indirectPercentage;
        private BigDecimal indirectScore;
        private Integer indirectLevel;
        private BigDecimal finalAttainment;
        private Boolean targetMet;
        private String observation;
    }
}
