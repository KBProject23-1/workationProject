package com.workit.domain.survey.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class SurveyResultResponseDTO {

    private final Long surveyId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<SurveyResultQuestionResponseDTO> questions;

    public static SurveyResultResponseDTO of(Long surveyId,
                                            LocalDateTime createdAt,
                                            LocalDateTime updatedAt,
                                            List<SurveyResultQuestionResponseDTO> questions) {
        return new SurveyResultResponseDTO(surveyId, createdAt, updatedAt, questions);
    }
}
