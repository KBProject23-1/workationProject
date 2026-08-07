package com.workit.domain.user.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// User 도메인 에러 코드
// ErrorCode 규약(DOMAIN_REASON)에 따라 명칭 정의 (docs API 스펙 기준)
public enum UserErrorCode implements ErrorCode {

    // 내 프로필 조회 - 로그인 사용자 정보 없음/비활성 → 404
    // (docs: USER_NOT_FOUND "회원 정보를 찾을 수 없습니다." — 계정 존재 여부 비노출을 위해 상태와 통일 처리)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),

    // 프로필 최초 등록 - 요청 값 검증 실패 (nickname 누락·길이 초과, companyName 길이 초과) → 400
    // (docs: INVALID_PROFILE_REQUEST "프로필 정보를 확인해주세요.")
    INVALID_PROFILE_REQUEST(HttpStatus.BAD_REQUEST, "프로필 정보를 확인해주세요."),

    // 프로필 최초 등록 - nickname 중복 → 409
    // (docs: DUPLICATE_NICKNAME "이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해 주세요." — user_profile.nickname UNIQUE)
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해 주세요."),

    // 프로필 최초 등록 - 이미 프로필이 등록된 사용자의 재등록 시도 → 409
    // (ERD: user_profile.user_id UNIQUE 1:1 — 최초 등록 API는 프로필 미등록 사용자만 호출 가능)
    PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 프로필이 등록되어 있습니다.");

    private final HttpStatus status;
    private final String message;

    UserErrorCode(HttpStatus status, String message) {
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
