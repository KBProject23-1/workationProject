package com.workit.domain.survey.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SurveyQuestionOptionResponseDTO {

    private final Long optionId;
    private final String optionCode;
    private final String optionText;
    private final Integer displayOrder;
}

