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
    PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 프로필이 등록되어 있습니다."),

    // 프로필 수정 - 프로필 미등록 사용자의 수정 시도 → 404
    // (최초 등록되지 않은 사용자는 수정 불가 — docs: 내 프로필 정보 수정)
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 프로필 정보가 없습니다. 프로필을 먼저 등록해주세요."),

    // 회원 탈퇴 - 이미 탈퇴(WITHDRAWN) 처리된 회원의 재탈퇴 시도 → 409
    // (knowledge.md: 409 CONFLICT - 상태 충돌 — 이미 탈퇴된 계정은 재탈퇴 불가)
    USER_ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "이미 탈퇴 처리된 회원입니다."),

    // 휴대폰 번호 변경 - PASS 인증된 휴대폰 번호가 현재 휴대폰 번호와 동일함 → 400
    // (docs: 동일한 휴대폰 번호로 변경하는 경우 정책에 따라 실패 처리 —
    //  비밀번호 변경의 AUTH_SAME_PASSWORD(400) / PIN 재설정의 SAME_AS_CURRENT_PIN(400) 과 동일 패턴)
    PHONE_SAME_AS_CURRENT(HttpStatus.BAD_REQUEST, "현재 휴대폰 번호와 동일한 번호로는 변경할 수 없습니다."),

    // 휴대폰 번호 변경 - PASS 인증된 휴대폰 번호가 이미 다른 사용자에게 등록되어 있음 → 409
    // (knowledge.md: 409 CONFLICT - 중복 데이터 — users.phone_number_hash UNIQUE,
    //  회원가입 시 동일 휴대폰 중복을 DUPLICATE_USER(409) 로 처리하는 정책과 동일)
    PHONE_ALREADY_IN_USE(HttpStatus.CONFLICT, "이미 사용 중인 휴대폰 번호입니다.");

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
