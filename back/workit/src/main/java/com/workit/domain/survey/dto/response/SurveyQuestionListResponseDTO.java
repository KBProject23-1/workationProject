package com.workit.domain.survey.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SurveyQuestionListResponseDTO {

    private final List<SurveyQuestionItemResponseDTO> questions;

    public static SurveyQuestionListResponseDTO of(List<SurveyQuestionItemResponseDTO> questions) {
        return new SurveyQuestionListResponseDTO(questions);
    }
}

