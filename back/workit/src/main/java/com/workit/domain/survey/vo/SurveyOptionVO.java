package com.workit.domain.survey.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyOptionVO {

    private Long optionId;
    private Long questionId;
    private String optionCode;
    private String optionName;
}

