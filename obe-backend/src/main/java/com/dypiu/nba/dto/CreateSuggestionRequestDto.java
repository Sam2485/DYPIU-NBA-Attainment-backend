package com.dypiu.nba.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSuggestionRequestDto {

    private String schoolId;
    private String departmentId;
    private String masterProgrammeId;
    private String programmeBatchId;
    private String programmeBatchCourseId;

    @Builder.Default
    private String contextType = "PROGRAMME_BATCH"; // 'PROGRAMME_BATCH', 'COURSE', 'SCHOOL', 'DEPARTMENT'

    @Builder.Default
    private String category = "GENERAL";

    @NotBlank(message = "Suggestion content cannot be blank")
    private String content;
}
