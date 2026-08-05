package com.workit.domain.auth.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// Auth 도메인 에러 코드
// ErrorCode 규약(DOMAIN_REASON)에 따라 명칭 정의 (docs API 스펙 기준)
public enum AuthErrorCode implements ErrorCode {

    // 본인인증(PASS) 관련
    // docs: 본인인증 검증 및 회원 중복 체크 → 400
    INVALID_VERIFICATION_ID(HttpStatus.BAD_REQUEST, "PASS 인증이 유효하지 않습니다. 다시 시도해주세요."),

    // 본인인증 통과 후 CI 기준 중복 가입 감지 → 409 (docs errorCode: DUPLICATE_USER)
    DUPLICATE_USER(HttpStatus.CONFLICT, "이미 가입된 회원입니다. 로그인을 진행해주세요."),

    // 회원가입 이메일 중복 확인 - 이메일 미입력/형식 오류 → 400 (docs errorCode: INVALID_EMAIL_FORMAT)
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요.");

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
