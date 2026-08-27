package com.dypiu.nba.dto;

import com.dypiu.nba.entity.SetupStepStatus;
import lombok.*;
import java.time.ZonedDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCoordinatorSetupProgressDto {
    private String id;
    private String programmeBatchCourseId;
    private Integer currentStep;
    private List<String> completedSteps;
    private List<String> pendingSteps;
    private ZonedDateTime updatedAt;

    public String getMasterCourseId() {
        return programmeBatchCourseId;
    }

    public void setMasterCourseId(String masterCourseId) {
        this.programmeBatchCourseId = masterCourseId;
    }

    public String getProgrammeBatchCourseId() {
        return programmeBatchCourseId;
    }

    public void setProgrammeBatchCourseId(String programmeBatchCourseId) {
        this.programmeBatchCourseId = programmeBatchCourseId;
    }

    public static class CourseCoordinatorSetupProgressDtoBuilder {
        public CourseCoordinatorSetupProgressDtoBuilder masterCourseId(String masterCourseId) {
            this.programmeBatchCourseId = masterCourseId;
            return this;
        }

        public CourseCoordinatorSetupProgressDtoBuilder programmeBatchCourseId(String programmeBatchCourseId) {
            this.programmeBatchCourseId = programmeBatchCourseId;
            return this;
        }
    }
}
