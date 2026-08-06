package com.workit.domain.survey.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SurveyAnswerItemRequestDTO {

    private Long questionId;
    private List<Long> optionIds;
}
