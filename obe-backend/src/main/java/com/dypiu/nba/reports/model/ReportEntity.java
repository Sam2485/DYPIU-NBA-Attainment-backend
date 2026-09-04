package com.dypiu.nba.reports.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportEntity {

    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", length = 50, nullable = false)
    private ReportType reportType;

    @Column(name = "institution_id", length = 50)
    private String institutionId;

    @Column(name = "master_programme_id", length = 50)
    private String masterProgrammeId;

    @Column(name = "programme_batch_id", length = 50)
    private String programmeBatchId;

    @Column(name = "programme_batch_course_id", length = 50)
    private String programmeBatchCourseId;

    @Column(name = "master_course_id", length = 50)
    private String masterCourseId;

    @Column(name = "template_id", length = 50)
    private String templateId;

    @Column(name = "template_version")
    private Integer templateVersion;

    @Column(name = "generated_by", length = 150)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false)
    private ZonedDateTime generatedAt;

    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = "GENERATED";

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    @Builder.Default
    private List<ReportArtifactEntity> artifacts = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = ZonedDateTime.now();
        }
        if (generatedAt == null) {
            generatedAt = ZonedDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = ZonedDateTime.now();
    }
}
