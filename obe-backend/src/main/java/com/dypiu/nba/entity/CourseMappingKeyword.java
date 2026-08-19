package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(
        name = "course_mapping_keywords",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_offering_keyword_type",
                        columnNames = {"course_offering_id", "keyword_type"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseMappingKeyword {

    @Id
    private String id;

    @Column(name = "course_offering_id", nullable = false)
    private String courseOfferingId;

    @Column(name = "keyword_type", nullable = false, length = 20)
    private String keywordType; // PO or PSO

    @Column(name = "keywords_json", nullable = false, columnDefinition = "TEXT")
    private String keywordsJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}