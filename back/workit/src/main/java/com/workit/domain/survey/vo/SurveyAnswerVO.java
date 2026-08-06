package com.workit.domain.survey.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyAnswerVO {

    private Long questionId;
    private String questionCode;
    private String questionText;
    private String category;
    private QuestionType questionType;
    private Long optionId;
    private String optionName;
}

