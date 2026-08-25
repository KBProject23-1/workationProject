package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 비밀번호 재설정 2단계 - 비밀번호 변경 요청
// API 스펙(docs): PATCH /api/v1/auth/password/reset
// 필드: passwordResetToken, newPassword
//
// - 1단계(verify)에서 발급받은 5분 유효 임시 토큰과 신규 비밀번호를 받아 비밀번호를 변경한다
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값 검증은 Service Layer 에서 수행한다
// - newPassword 는 민감정보 — 로그/toString 노출 금지 (LoginRequestDTO.password 와 동일 정책)
@Getter
@Setter
@ToString
public class PasswordResetRequestDTO {

    /** 비밀번호 변경용 임시 UUID (Redis 에 저장된 passwordResetToken) — 필수 */
    private String passwordResetToken;

    /** 새 비밀번호 원문 — 로그/toString 노출 금지 */
    @ToString.Exclude
    private String newPassword;
}
