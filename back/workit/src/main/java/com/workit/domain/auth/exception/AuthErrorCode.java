package com.workit.domain.auth.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// Auth 도메인 에러 코드
// ErrorCode 규약(DOMAIN_REASON)에 따라 명칭 정의 (docs API 스펙 기준)
public enum AuthErrorCode implements ErrorCode {

    // 본인인증(PASS) 관련
    // docs: 본인인증 검증 및 회원 중복 체크 → 400
    INVALID_VERIFICATION_ID(HttpStatus.BAD_REQUEST, "PASS 인증이 유효하지 않습니다. 다시 시도해주세요.");

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
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
