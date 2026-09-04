package com.dypiu.nba.reports.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "report_assets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAssetEntity {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Column(name = "institution_id", length = 50)
    private String institutionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", length = 50, nullable = false)
    private ReportAssetType assetType;

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Column(name = "mime_type", length = 100, nullable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }
}
