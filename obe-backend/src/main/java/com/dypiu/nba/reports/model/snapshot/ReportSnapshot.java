package com.dypiu.nba.reports.model.snapshot;

import com.dypiu.nba.reports.model.ReportType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.ZonedDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "snapshotType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProgrammeAttainmentSnapshot.class, name = "PROGRAMME_ATTAINMENT"),
    @JsonSubTypes.Type(value = CourseAttainmentSnapshot.class, name = "COURSE_ATTAINMENT"),
    @JsonSubTypes.Type(value = ProgrammeAtrSnapshot.class, name = "PROGRAMME_ATR"),
    @JsonSubTypes.Type(value = CourseAtrSnapshot.class, name = "COURSE_ATR")
})
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class ReportSnapshot {
    private String reportId;
    private ReportType reportType;
    private String institutionId;
    private String institutionName;
    private String schoolName;
    private String academicYear;
    private String generatedBy;
    private ZonedDateTime generatedAt;
}
