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
    PHONE_ALREADY_IN_USE(HttpStatus.CONFLICT, "이미 사용 중인 휴대폰 번호입니다."),

    // 이메일 인증번호 발송 - email 누락/형식 오류 → 400
    // (docs: 이메일 인증번호 발송 — 이메일 누락, 이메일 형식이 올바르지 않음 → 400,
    //  INVALID_PROFILE_REQUEST(400) 와 동일한 INVALID_*_REQUEST 규약 — javax.validation 미사용 환경,
    //  검증은 Service Layer 에서 EmailValidator 공통 정책으로 수행)
    INVALID_EMAIL_REQUEST(HttpStatus.BAD_REQUEST, "이메일을 확인해주세요."),

    // 이메일 인증번호 발송 - 현재 사용자의 이메일과 동일한 이메일 → 400
    // (docs: 현재 이메일과 동일한 이메일 → 400 — 휴대폰 번호 변경의
    //  PHONE_SAME_AS_CURRENT(400) 와 동일 패턴)
    EMAIL_SAME_AS_CURRENT(HttpStatus.BAD_REQUEST, "현재 이메일과 동일한 이메일로는 변경할 수 없습니다."),

    // 이메일 인증번호 발송 - 다른 사용자가 이미 사용 중인 이메일 → 409
    // (knowledge.md: 409 CONFLICT - 중복 데이터 — users.email_hash UNIQUE,
    //  휴대폰 번호 변경의 PHONE_ALREADY_IN_USE(409) 와 동일 패턴)
    EMAIL_ALREADY_IN_USE(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    // 이메일 인증번호 발송 - 인증번호 생성/임시 저장 실패 → 500
    // (docs: 인증번호 발급 실패 — Mock 저장소(Redis) 오류 등 발급이 완료되지 못한 경우)
    EMAIL_VERIFICATION_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "이메일 인증번호 발급에 실패했습니다. 잠시 후 다시 시도해주세요."),

    // 이메일 인증번호 확인 - 해당 이메일에 발송된 인증정보가 존재하지 않음 → 400
    // (docs: 이메일 인증번호 확인 — 인증번호 발송 기록이 없음)
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST,
            "발송된 인증번호가 없습니다. 인증번호를 다시 발송해주세요."),

    // 이메일 인증번호 확인 - 인증번호 유효시간(5분) 만료 → 400
    // (docs: 이메일 인증번호 확인 — 인증번호가 만료됨)
    EMAIL_VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST,
            "인증번호가 만료되었습니다. 인증번호를 다시 발송해주세요."),

    // 이메일 인증번호 확인 - 입력 인증번호 누락/불일치 → 400
    // (docs: 이메일 인증번호 확인 — 인증번호 누락, 인증번호가 일치하지 않음)
    EMAIL_VERIFICATION_CODE_INVALID(HttpStatus.BAD_REQUEST,
            "인증번호가 올바르지 않습니다."),

    // 이메일 인증번호 확인 - 이미 인증 완료된 인증번호 재사용 → 400
    // (docs: 이메일 인증번호 확인 — 이미 사용된 인증번호, 재사용 방지)
    EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST,
            "이미 인증이 완료된 인증번호입니다. 인증번호를 다시 발송해주세요."),

    // 이메일 변경 - 이메일 인증 완료 정보가 없거나(인증정보 없음) 만료되었거나 인증 완료 상태(VERIFIED)가 아님 → 400
    // (docs: 이메일 변경 — 이메일 인증 미완료/인증정보 없음/인증정보 만료 모두 "이메일 인증 필요" 로 처리)
    // - 이메일 변경 API 는 Request Body 를 받지 않으므로, 인증 완료 정보는 EmailVerificationStore 에서
    //   현재 사용자(userId) 기준으로만 조회한다 — 인증 완료 상태가 아니면 변경을 거부한다
    // - 세부 원인(미발급/만료/미인증)을 구분해 노출하지 않는다 (인증 완료 정보 재시도 유도)
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.BAD_REQUEST,
            "이메일 인증이 필요합니다. 인증번호를 발송하고 인증을 완료해주세요.");

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
