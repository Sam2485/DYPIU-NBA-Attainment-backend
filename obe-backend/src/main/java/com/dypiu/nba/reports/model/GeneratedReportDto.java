package com.dypiu.nba.reports.model;

import com.dypiu.nba.reports.model.snapshot.ReportSnapshot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedReportDto {
    private String reportId;
    private ReportType reportType;
    private String institutionId;
    private String masterProgrammeId;
    private String programmeBatchId;
    private String programmeBatchCourseId;
    private String masterCourseId;
    private String templateId;
    private Integer templateVersion;
    private String generatedBy;
    private ZonedDateTime generatedAt;
    private String status;
    private ReportSnapshot snapshot;
    private List<ArtifactSummaryDto> artifacts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtifactSummaryDto {
        private String artifactId;
        private ArtifactType artifactType;
        private String fileReference;
        private String originalFilename;
        private String mimeType;
        private Long fileSize;
        private String sha256Checksum;
        private String hmacSignature;
        private ZonedDateTime generatedAt;
    }
}
