package com.dypiu.nba.reports.model.snapshot;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeAtrSnapshot extends ReportSnapshot {

    private String programmeAtrId;
    private String masterProgrammeId;
    private String masterProgrammeCode;
    private String masterProgrammeName;

    private String programmeBatchId;
    private String batchName;
    private String startYear;
    private String endYear;
    private String status;

    private List<AtrOutcomeRow> poOutcomes;
    private List<AtrOutcomeRow> psoOutcomes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AtrOutcomeRow {
        private String outcomeCode;
        private String outcomeStatement;
        private BigDecimal targetLevel;
        private BigDecimal attainmentLevel;
        private BigDecimal achievementPercentage;
        private List<String> actions;
    }
}
