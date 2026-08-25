package com.workit.domain.survey.controller;

import com.workit.domain.survey.dto.request.SurveyCreateRequestDTO;
import com.workit.domain.survey.dto.response.SurveyCreateResponseDTO;
import com.workit.domain.survey.dto.response.SurveyModifyResponseDTO;
import com.workit.domain.survey.dto.response.SurveyQuestionListResponseDTO;
import com.workit.domain.survey.dto.response.SurveyResultResponseDTO;
import com.workit.domain.survey.service.SurveyService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    // 설문 질문과 선택지를 표시 순서대로 조회한다.
    @GetMapping("/questions")
    public ResponseEntity<CommonResponse<SurveyQuestionListResponseDTO>> surveyQuestionList(
            @CurrentUser Long userId) {
        return GlobalResponseFactory.success(surveyService.findQuestionList(userId));
    }

    // 로그인 사용자의 현재 진행 중 워케이션 기준 설문 응답을 최초 저장한다.
    @PostMapping
    public ResponseEntity<CommonResponse<SurveyCreateResponseDTO>> surveyAdd(
            @CurrentUser Long userId,
            @RequestBody SurveyCreateRequestDTO request) {
        return GlobalResponseFactory.created(surveyService.createSurvey(userId, request));
    }

    // 로그인 사용자의 저장된 설문과 문항별 선택 결과를 조회한다.
    @GetMapping("/users")
    public ResponseEntity<CommonResponse<SurveyResultResponseDTO>> surveyDetails(
            @CurrentUser Long userId) {
        return GlobalResponseFactory.success(surveyService.getMySurveyResult(userId));
    }

    // 로그인 사용자의 설문 결과를 수정한다.
    @PatchMapping("/{surveyId}")
    public ResponseEntity<CommonResponse<SurveyModifyResponseDTO>> surveyModify(
            @CurrentUser Long userId,
            @PathVariable("surveyId") Long surveyId,
            @RequestBody SurveyCreateRequestDTO request) {
        return GlobalResponseFactory.success(surveyService.modifySurvey(userId, surveyId, request));
    }
}
