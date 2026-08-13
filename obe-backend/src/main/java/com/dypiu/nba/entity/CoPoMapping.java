package com.dypiu.nba.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "co_po_mappings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoPoMapping {

    @Id
    private String id;

    @Column(name = "course_outcome_id", nullable = false)
    private String courseOutcomeId;

    @Column(name = "po_code", nullable = false, length = 20)
    private String poCode;

    @Column(name = "mapping_level", nullable = false)
    @Builder.Default
    private Integer mappingLevel = 0; // 0, 1, 2, 3

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private ZonedDateTime updatedAt;
}
