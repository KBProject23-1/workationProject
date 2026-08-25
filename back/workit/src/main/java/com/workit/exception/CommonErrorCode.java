package com.workit.exception;

import org.springframework.http.HttpStatus;

// 도메인에 귀속되지 않는 공통 에러 코드
// ErrorCode 규약(DOMAIN_REASON)에 따라 COMMON_ 접두사를 사용한다
public enum CommonErrorCode implements ErrorCode {

    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    COMMON_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    COMMON_MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
    COMMON_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "요청 값의 형식이 올바르지 않습니다."),
    COMMON_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력 값 검증에 실패했습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 ID의 요소가 없습니다."),
    COMMON_NO_HANDLER(HttpStatus.NOT_FOUND, "존재하지 않는 API 경로입니다."),
    COMMON_METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
    COMMON_DATA_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 처리 중 오류가 발생했습니다."),
    COMMON_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    CommonErrorCode(HttpStatus status, String message) {
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
