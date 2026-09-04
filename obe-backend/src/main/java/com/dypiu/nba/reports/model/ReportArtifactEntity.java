package com.dypiu.nba.reports.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "report_artifacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportArtifactEntity {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ReportEntity report;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", length = 30, nullable = false)
    private ArtifactType artifactType;

    @Column(name = "file_reference", length = 500, nullable = false)
    private String fileReference;

    @Column(name = "original_filename", length = 255, nullable = false)
    private String originalFilename;

    @Column(name = "mime_type", length = 100, nullable = false)
    private String mimeType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "sha256_checksum", length = 64, nullable = false)
    private String sha256Checksum;

    @Column(name = "hmac_signature", length = 128, nullable = false)
    private String hmacSignature;

    @Column(name = "generated_at", nullable = false)
    private ZonedDateTime generatedAt;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (generatedAt == null) {
            generatedAt = ZonedDateTime.now();
        }
    }
}
