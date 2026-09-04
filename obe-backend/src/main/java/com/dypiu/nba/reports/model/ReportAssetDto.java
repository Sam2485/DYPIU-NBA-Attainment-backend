package com.dypiu.nba.reports.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAssetDto {
    private String assetId;
    private String institutionId;
    private ReportAssetType assetType;
    private String originalFilename;
    private String storagePath;
    private String mimeType;
    private Long fileSize;
    private String createdBy;
    private ZonedDateTime createdAt;
}
