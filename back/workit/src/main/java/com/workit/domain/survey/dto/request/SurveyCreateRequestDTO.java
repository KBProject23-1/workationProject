package com.workit.domain.survey.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SurveyCreateRequestDTO {

    private List<SurveyAnswerItemRequestDTO> answers;
}
