package com.workit.domain.survey.service;

import com.workit.domain.survey.dto.request.SurveyCreateRequestDTO;
import com.workit.domain.survey.dto.response.SurveyCreateResponseDTO;
import com.workit.domain.survey.dto.response.SurveyModifyResponseDTO;
import com.workit.domain.survey.dto.response.SurveyQuestionListResponseDTO;
import com.workit.domain.survey.dto.response.SurveyResultResponseDTO;

public interface SurveyService {

    SurveyQuestionListResponseDTO findQuestionList(Long userId);

    SurveyCreateResponseDTO createSurvey(Long userId, SurveyCreateRequestDTO request);

    SurveyResultResponseDTO getMySurveyResult(Long userId);

    SurveyModifyResponseDTO modifySurvey(Long userId, Long surveyId, SurveyCreateRequestDTO request);
}

