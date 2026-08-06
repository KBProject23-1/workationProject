package com.workit.domain.recommendation.offices.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum OfficeRecommendationErrorCode implements ErrorCode {

    INVALID_RECOMMENDATION_REQUEST(HttpStatus.BAD_REQUEST, "공유오피스 추천 요청값이 올바르지 않습니다."),
    INVALID_RECOMMENDATION_CURSOR(HttpStatus.BAD_REQUEST, "공유오피스 추천 커서가 올바르지 않습니다."),
    WORKATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 워케이션에 접근할 수 없습니다."),
    WORKATION_NOT_FOUND(HttpStatus.NOT_FOUND, "워크케이션 정보를 찾을 수 없습니다."),
    RECOMMENDATION_CONDITION_NOT_READY(HttpStatus.CONFLICT, "추천에 필요한 설문 또는 예산 정보가 부족합니다."),
    OFFICE_RECOMMENDATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "공유오피스 추천 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    OfficeRecommendationErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
