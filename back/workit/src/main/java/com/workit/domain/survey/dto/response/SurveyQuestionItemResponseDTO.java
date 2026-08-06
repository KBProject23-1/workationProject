package com.workit.domain.survey.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import com.workit.domain.survey.vo.QuestionType;

import java.util.List;

@Getter
@AllArgsConstructor
public class SurveyQuestionItemResponseDTO {

    private final Long questionId;
    private final String questionCode;
    private final String questionText;
    private final QuestionType questionType;
    private final Boolean required;
    private final Integer minSelections;
    private final Integer maxSelections;
    private final Integer displayOrder;
    private final List<SurveyQuestionOptionResponseDTO> options;
}
