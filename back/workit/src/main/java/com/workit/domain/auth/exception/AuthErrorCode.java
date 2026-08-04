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
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요."),

    // 회원가입 완료 - 요청 값 검증 실패 (identityToken/email/password/nickname 누락 등) → 400
    INVALID_SIGNUP_REQUEST(HttpStatus.BAD_REQUEST, "회원가입 요청 값이 올바르지 않습니다. 다시 확인해 주세요."),

    // 회원가입 완료 - 회원가입 전용 JWT 서명/형식/용도(sub) 오류 → 400
    INVALID_SIGNUP_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 본인인증 토큰입니다. 본인인증을 다시 진행해 주세요."),

    // 회원가입 완료 - 회원가입 전용 JWT 만료 → 401
    EXPIRED_SIGNUP_TOKEN(HttpStatus.UNAUTHORIZED, "본인인증 유효 시간이 만료되었습니다. 인증을 다시 진행해 주세요."),

    // 회원가입 완료 - Redis 임시 인증 데이터 없음(만료/삭제) → 400
    SIGNUP_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "본인인증 정보가 만료되었습니다. 본인인증을 다시 진행해 주세요."),

    // 회원가입 완료 - 이메일 중복 → 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    // 회원가입 완료 - 닉네임 중복 → 409
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    // 회원가입 완료 - 필수 약관 미동의 / agreedTermsIds 누락·빈 배열 → 400
    // (docs: 최종 회원가입 완료 → MISSING_REQUIRED_TERMS)
    MISSING_REQUIRED_TERMS(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해주세요."),

    // 회원가입 완료 - agreedTermsIds 에 존재하지 않는 약관 ID 포함 → 400
    // (terms 마스터에 없는 ID 는 user_terms_agreements FK 위반으로 500 이 되므로 사전 차단)
    INVALID_TERM_ID(HttpStatus.BAD_REQUEST, "존재하지 않는 약관이 포함되어 있습니다. 다시 확인해 주세요."),

    // JWT 토큰 검증 실패 - 서명/형식 오류, sub(userId) 누락 등 → 400
    // (회원가입 INVALID_SIGNUP_TOKEN 규약과 동일 — 위변조/형식 오류는 400)
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다. 다시 로그인해 주세요."),

    // JWT 토큰 만료 → 401 (knowledge.md: Token 만료는 401)
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 다시 로그인해 주세요."),

    // 지원하지 않는 JWT 토큰 - 알고리즘/형식 불일치, 용도 오류 등 → 400
    UNSUPPORTED_TOKEN(HttpStatus.BAD_REQUEST, "지원하지 않는 토큰 형식입니다. 다시 로그인해 주세요.");

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
