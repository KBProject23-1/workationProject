package com.workit.domain.survey.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SurveyCreateResponseDTO {

    private final Long surveyId;
    private final LocalDateTime createdAt;
}
