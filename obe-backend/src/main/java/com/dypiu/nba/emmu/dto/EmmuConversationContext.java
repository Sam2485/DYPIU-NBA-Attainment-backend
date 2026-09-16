package com.dypiu.nba.emmu.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsulates the multi-turn conversational context for contextual entity resolution.
 * This is conversation state and NEVER acts as authorization proof.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmmuConversationContext {
    private String schoolId;
    private String schoolName;
    private String departmentId;
    private String departmentName;
    private String masterProgrammeId;
    private String programmeName;
    private String programmeBatchId;
    private String batchName;
    private Integer semester;
    private String programmeBatchCourseId;
    private String courseCode;
    private String courseName;
    private String outcomeCode; // e.g., 'PO3', 'CO2', 'PSO1'
    private String outcomeType; // 'PO', 'PSO', 'CO'
    private AcademicEntityType lastResolvedEntityType;
    private String lastResolvedEntityId;
    private String lastResolvedEntityName;
}
