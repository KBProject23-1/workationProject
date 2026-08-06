package com.workit.domain.survey.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

import lombok.RequiredArgsConstructor;

// 설문 도메인 에러 코드
// 문서 기준 코드명을 유지해서 클라이언트 처리와의 정합성을 맞춘다
@RequiredArgsConstructor
public enum SurveyErrorCode implements ErrorCode {

    // 400
    INVALID_SURVEY_REQUEST(HttpStatus.BAD_REQUEST, "설문 조회 요청이 올바르지 않습니다."),
    INVALID_SURVEY_QUESTION_REQUEST(HttpStatus.BAD_REQUEST, "설문 문항 조회 요청이 올바르지 않습니다."),
    INVALID_SURVEY_ANSWER(HttpStatus.BAD_REQUEST, "설문 답변이 올바르지 않습니다."),

    // 403
    SURVEY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 설문에 접근할 수 없습니다."),
    SURVEY_QUESTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "설문 문항을 조회할 수 없습니다."),

    // 404
    SURVEY_NOT_FOUND(HttpStatus.NOT_FOUND, "저장된 설문을 찾을 수 없습니다."),

    // 409
    SURVEY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 설문이 있습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

