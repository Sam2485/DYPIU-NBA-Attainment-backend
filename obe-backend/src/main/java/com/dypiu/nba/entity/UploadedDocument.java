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

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType; // "EXAMINATION" or "SURVEY"

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "saved_file_name", nullable = false)
    private String savedFileName;

    @Column(name = "saved_path", nullable = false)
    private String savedPath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "threshold_percentage")
    private BigDecimal thresholdPercentage;

    @Column(name = "uploaded_at")
    private ZonedDateTime uploadedAt;
}
