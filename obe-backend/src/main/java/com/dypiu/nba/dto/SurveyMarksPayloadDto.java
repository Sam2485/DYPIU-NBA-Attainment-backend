package com.dypiu.nba.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyMarksPayloadDto {
    private String courseId;
    private List<SurveyResponseRowDto> surveyResponses;
}
