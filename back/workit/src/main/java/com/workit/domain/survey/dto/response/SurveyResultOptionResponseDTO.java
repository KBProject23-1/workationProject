package com.workit.domain.survey.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SurveyResultOptionResponseDTO {

    private final Long optionId;
    private final String optionName;
}
