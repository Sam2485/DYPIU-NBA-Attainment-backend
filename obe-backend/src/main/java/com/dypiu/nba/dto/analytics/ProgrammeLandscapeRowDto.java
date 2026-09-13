package com.dypiu.nba.dto.analytics;

import com.dypiu.nba.entity.ProgrammeAtrStatus;
import com.dypiu.nba.entity.ReportStatus;
import lombok.*;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgrammeLandscapeRowDto {

    private String masterProgrammeId;
    private String programmeName;
    private String programmeCode;
    private String degreeAwarded;
    private String departmentId;
    private String departmentName;
    private String schoolId;
    private String schoolName;
    private String programmeBatchId;
    private String batchName;
    private Integer startYear;
    private Integer endYear;

    private int posEvaluated;
    private int posMet;
    private int posTotal;

    private int psosEvaluated;
    private int psosMet;
    private int psosTotal;

    private int gapCount;
    private boolean hasGaps;

    private String reportAvailabilityStatus; // "FINALIZED_REPORT_AVAILABLE" | "NO_FINALIZED_REPORT"
    private ReportStatus underlyingReportStatus; // DRAFT | FINALIZED | APPROVED | null
    private String atrStatus; // ProgrammeAtrStatus string or "NOT_RECORDED"
    private ZonedDateTime finalizedAt;
}
