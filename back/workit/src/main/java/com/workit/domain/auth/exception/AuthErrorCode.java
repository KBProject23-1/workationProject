package com.workit.domain.auth.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// Auth 도메인 에러 코드
// ErrorCode 규약(DOMAIN_REASON)에 따라 명칭 정의 (docs API 스펙 기준)
public enum AuthErrorCode implements ErrorCode {

    // 본인인증(PASS) 관련
    // - 인증 ID 누락/빈 값, Redis 세션 없음(만료 포함), status != VERIFIED, 이미 사용 완료(used) 된 세션 → 400
    //   (회원가입/아이디 찾기/비밀번호·PIN 재설정 공용)
    INVALID_VERIFICATION_ID(HttpStatus.BAD_REQUEST, "PASS 인증이 유효하지 않습니다. 다시 시도해주세요."),

    // Mock PASS 본인인증 - 완료 등록 요청 값 오류 (name/phoneNumber 누락·빈 값, 형식 오류) → 400
    // (signup/login 의 INVALID_*_REQUEST 규약과 동일 — 필수 값 누락은 Service Layer 에서 차단)
    INVALID_PASS_REQUEST(HttpStatus.BAD_REQUEST, "본인인증 요청 값이 올바르지 않습니다. 다시 확인해 주세요."),

    // 본인인증 통과 후 CI 기준 중복 가입 감지 → 409 (docs errorCode: DUPLICATE_USER)
    DUPLICATE_USER(HttpStatus.CONFLICT, "이미 가입된 회원입니다. 로그인을 진행해주세요."),

    // 회원가입 이메일 중복 확인 - 이메일 미입력/형식 오류 → 400 (docs errorCode: INVALID_EMAIL_FORMAT)
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요."),

    // 회원가입 완료 - 요청 값 검증 실패 (identityVerificationId/email/password 누락 등) → 400
    INVALID_SIGNUP_REQUEST(HttpStatus.BAD_REQUEST, "회원가입 요청 값이 올바르지 않습니다. 다시 확인해 주세요."),

    // 회원가입 완료 - 이메일 중복 → 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),

    // 회원가입 완료 - 닉네임 중복 → 409

    // 회원가입 완료 - 필수 약관 미동의 / agreedTermsIds 누락·빈 배열 → 400
    // (docs: 최종 회원가입 완료 → MISSING_REQUIRED_TERMS)
    MISSING_REQUIRED_TERMS(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해주세요."),

    // 회원가입 완료 - agreedTermsIds 에 존재하지 않는 약관 ID 포함 → 400
    // (terms 마스터에 없는 ID 는 user_terms_agreements FK 위반으로 500 이 되므로 사전 차단)
    INVALID_TERM_ID(HttpStatus.BAD_REQUEST, "존재하지 않는 약관이 포함되어 있습니다. 다시 확인해 주세요."),

    // 통합 로그인 - 요청 값 검증 실패 (loginType 누락, PASSWORD/PIN 필수 값 누락) → 400
    INVALID_LOGIN_REQUEST(HttpStatus.BAD_REQUEST, "로그인 요청 값이 올바르지 않습니다. 다시 확인해 주세요."),

    // 통합 로그인 - 지원하지 않는 loginType (PASSWORD/PIN 외 값) → 400
    INVALID_LOGIN_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식입니다. PASSWORD 또는 PIN 을 사용해 주세요."),

    // 통합 로그인 - 인증 실패 (회원 없음, password/pin 불일치, 비활성 계정) → 401
    // 인증 실패 원인을 구분해 노출하지 않아 계정 존재 여부를 숨긴다 (계정 열거 방지)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "인증 정보가 올바르지 않습니다. 다시 확인 후 시도해 주세요."),

    // 통합 로그인 - PIN 실패 횟수 초과로 계정 잠금 → 403 (docs: PIN_LOCK_EXCEEDED)
    PIN_LOCK_EXCEEDED(HttpStatus.FORBIDDEN, "핀번호 입력 횟수가 5회 초과하여 계정이 잠겼습니다. PASS 본인인증을 통해 핀번호를 재설정해 주세요."),

    // JWT 토큰 검증 실패 - 서명/형식 오류, sub(userId) 누락 등 → 400
    // (위변조/형식 오류는 400 — 만료는 별도 EXPIRED_TOKEN 401)
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다. 다시 로그인해 주세요."),

    // JWT 토큰 만료 → 401 (knowledge.md: Token 만료는 401)
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 다시 로그인해 주세요."),

    // 지원하지 않는 JWT 토큰 - 알고리즘/형식 불일치, 용도 오류 등 → 400
    UNSUPPORTED_TOKEN(HttpStatus.BAD_REQUEST, "지원하지 않는 토큰 형식입니다. 다시 로그인해 주세요."),

    // 보호 API 접근 - 인증 토큰(JWT) 없이 접근 → 401
    // (knowledge.md: 401 UNAUTHORIZED - JWT 없음)
    AUTH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "인증이 필요합니다. 로그인 후 다시 시도해 주세요."),

    // 인증된 사용자가 권한이 없는 리소스에 접근 → 403
    // (knowledge.md: 403 FORBIDDEN - 권한 부족)
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 아이디 찾기 - CI 기준 가입 회원 없음 → 404
    // (docs: USER_NOT_FOUND — 해당 본인인증 정보로 가입된 계정이 존재하지 않음)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 본인인증 정보로 가입된 계정이 존재하지 않습니다."),

    // 비밀번호 재설정 - 요청 값 검증 실패 (loginId/identityVerificationId 누락·빈 값) → 400
    // (signup/login 의 INVALID_*_REQUEST 규약과 동일 — 필수 값 누락은 Service Layer 에서 차단)
    INVALID_PASSWORD_RESET_REQUEST(HttpStatus.BAD_REQUEST, "비밀번호 재설정 요청 값이 올바르지 않습니다. 다시 확인해 주세요."),

    // 비밀번호 재설정 - 본인확인 실패 (입력한 loginId 의 CI 와 PASS 인증 CI 불일치) → 400
    // (docs: VERIFICATION_FAILED — 입력한 계정 정보와 본인인증(PASS) 정보가 일치하지 않음)
    VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "입력하신 계정 정보와 본인인증(PASS) 정보가 일치하지 않습니다."),

    // 비밀번호 재설정 - passwordResetToken 만료/존재하지 않음/위변조 → 400
    // (docs: RESET_TIMEOUT_OR_INVALID_TOKEN — Redis 저장 토큰이 없으면 5분 만료 또는 잘못된 접근으로 간주)
    RESET_TIMEOUT_OR_INVALID_TOKEN(HttpStatus.BAD_REQUEST, "비밀번호 변경 유효시간(5분)이 만료되었거나 올바르지 않은 접근입니다. 처음부터 다시 진행해 주세요."),

    // 비밀번호 재설정 - 비밀번호 정책 미준수 (영문/숫자/특수문자 포함 8자 이상) → 422
    // (docs: WEAK_PASSWORD — 프론트 1차 검증을 통과하지 못한 약한 비밀번호)
    WEAK_PASSWORD(HttpStatus.UNPROCESSABLE_ENTITY, "비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다."),

    // 비밀번호 변경 - 요청 값 검증 실패 (currentPassword/newPassword 누락·빈 값) → 400
    // (signup/login 의 INVALID_*_REQUEST 규약과 동일 — docs: INVALID_REQUEST "필수 입력값을 확인해 주세요.")
    INVALID_PASSWORD_CHANGE_REQUEST(HttpStatus.BAD_REQUEST, "필수 입력값을 확인해 주세요."),

    // 비밀번호 변경 - 현재 비밀번호 불일치 → 400
    // (docs: AUTH_INVALID_PASSWORD — "현재 비밀번호가 올바르지 않습니다.")
    AUTH_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),

    // 비밀번호 변경 - 신규 비밀번호가 현재 비밀번호와 동일 → 400
    // (docs: AUTH_SAME_PASSWORD — "현재 비밀번호와 다른 비밀번호를 입력해 주세요.")
    AUTH_SAME_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호와 다른 비밀번호를 입력해 주세요."),

    // PIN 번호 최초 설정 - 요청 값 검증 실패 (pinNumber/deviceId/deviceName 누락·빈 값) → 400
    // (signup/login 의 INVALID_*_REQUEST 규약과 동일 — 필수 값 누락은 Service Layer 에서 차단)
    INVALID_PIN_SETUP_REQUEST(HttpStatus.BAD_REQUEST, "PIN 설정 요청 값이 올바르지 않습니다. 다시 확인해 주세요."),

    // PIN 번호 최초 설정 - PIN 형식 오류 (6자리 숫자가 아닌 경우) → 400
    // (docs: INVALID_PIN_FORMAT — 5자리/7자리/문자 포함 등 형식 미준수)
    INVALID_PIN_FORMAT(HttpStatus.BAD_REQUEST, "핀번호는 6자리 숫자여야 합니다. 다시 입력해 주세요."),

    // PIN 번호 최초 설정 - 동일 기기(device_id)에 이미 PIN 등록됨 → 409
    // (knowledge.md: 409 CONFLICT - 중복 데이터 — UNIQUE(user_id, device_id) 위반 사전 차단)
    PIN_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 핀번호가 설정된 기기입니다. 등록된 핀번호로 로그인해 주세요."),

    // 보안 PIN 번호 재설정 - 등록된 PIN(등록 기기)이 없는 회원 → 400
    // (PASS 재인증은 통과했지만 user_device 에 pin_hash 가 등록된 기기가 없는 경우 — PIN 재설정 불가)
    PIN_NOT_REGISTERED(HttpStatus.BAD_REQUEST, "등록된 핀번호가 없습니다. 먼저 핀번호를 설정해 주세요."),

    // 보안 PIN 번호 재설정 - 신규 PIN 이 기존 PIN 과 동일함 → 400
    // (docs: SAME_AS_CURRENT_PIN — "기존 핀번호와 동일한 번호는 사용할 수 없습니다.")
    SAME_AS_CURRENT_PIN(HttpStatus.BAD_REQUEST, "기존 핀번호와 동일한 번호는 사용할 수 없습니다."),

    // 로그인 토큰 재발급 - Refresh Token 검증 실패 (쿠키 누락/만료/위변조/용도 오류/Redis 부재·불일치/비활성 회원) → 401
    // (docs: INVALID_REFRESH_TOKEN — 실패 원인을 구분해 노출하지 않아 세션 정보 유출을 방지)
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "세션이 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요.");

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
