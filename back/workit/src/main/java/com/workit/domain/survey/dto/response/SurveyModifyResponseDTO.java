package com.workit.domain.survey.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SurveyModifyResponseDTO {

    private final Long surveyId;
    private final LocalDateTime updatedAt;
}
