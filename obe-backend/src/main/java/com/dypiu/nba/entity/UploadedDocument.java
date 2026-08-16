package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
@Entity
@Table(name = "uploaded_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedDocument {

    @Id
    private String id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "course_offering_id")
    private String courseOfferingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "saved_file_name", nullable = false)
    private String savedFileName;

    @Column(name = "saved_path", nullable = false)
    private String savedPath;

    private Long fileSize;

    private Integer recordsProcessed;

    private BigDecimal thresholdPercentage;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @Column(name = "uploaded_at")
    private ZonedDateTime uploadedAt;
}