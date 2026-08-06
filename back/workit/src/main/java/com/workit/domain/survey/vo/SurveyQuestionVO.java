package com.workit.domain.survey.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SurveyQuestionVO {

    private Long questionId;
    private String questionCode;
    private String questionText;
    private String category;
    private QuestionType questionType;
    private Integer minSelection;
    private Integer maxSelection;
    private List<SurveyOptionVO> options;
}

