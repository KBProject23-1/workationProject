package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// 비밀번호 재설정 1단계 응답
// API 스펙(docs): POST /api/v1/auth/password/verify → data.passwordResetToken
//
// 보안 규칙 (knowledge.md):
// - passwordResetToken 은 Redis(password:reset:{token})에 저장된 1회성 UUID 로,
//   5분 TTL 만료 또는 사용 완료 시 폐기된다
@Getter
@Builder
public class PasswordVerifyResponseDTO {

    /** Redis 에 저장된 비밀번호 변경용 임시 UUID (5분 TTL) */
    private String passwordResetToken;

    public static PasswordVerifyResponseDTO of(String passwordResetToken) {
        return PasswordVerifyResponseDTO.builder()
                .passwordResetToken(passwordResetToken)
                .build();
    }
}
