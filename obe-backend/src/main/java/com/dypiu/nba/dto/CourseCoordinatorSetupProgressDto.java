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

    public String getCourseId() {
        return programmeBatchCourseId;
    }

    public void setCourseId(String courseId) {
        this.programmeBatchCourseId = courseId;
    }

    public String getCourseOfferingId() {
        return programmeBatchCourseId;
    }

    public void setCourseOfferingId(String courseOfferingId) {
        this.programmeBatchCourseId = courseOfferingId;
    }

    public static class CourseCoordinatorSetupProgressDtoBuilder {
        public CourseCoordinatorSetupProgressDtoBuilder courseId(String courseId) {
            this.programmeBatchCourseId = courseId;
            return this;
        }

        public CourseCoordinatorSetupProgressDtoBuilder courseOfferingId(String courseOfferingId) {
            this.programmeBatchCourseId = courseOfferingId;
            return this;
        }
    }
}
