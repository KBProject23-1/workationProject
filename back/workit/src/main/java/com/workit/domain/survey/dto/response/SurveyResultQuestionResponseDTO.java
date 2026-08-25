package com.workit.domain.survey.dto.response;

import com.workit.domain.survey.vo.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SurveyResultQuestionResponseDTO {

    private final Long questionId;
    private final String questionCode;
    private final String question;
    private final String category;
    private final QuestionType questionType;
    private final List<SurveyResultOptionResponseDTO> options;
    private final List<Long> selectedOptionIds;
}
