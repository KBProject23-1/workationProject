package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// 비밀번호 재설정 1단계 응답
// API 스펙(docs): POST /api/v1/auth/password/verify → data.passwordResetToken, data.expiresAt
//
// 보안 규칙 (knowledge.md):
// - passwordResetToken 은 Redis(password:reset:{token})에 저장된 1회성 UUID 로,
//   5분 TTL 만료 또는 사용 완료 시 폐기된다
// - expiresAt 은 토큰의 만료 시각(epoch millis) — 프론트가 5분 유효시간 카운트다운을
//   표시할 때 서버 시각 기준으로 정확히 맞추기 위해 함께 내려준다
//   (프론트가 TTL 설정값을 하드코딩하지 않도록 한다)
@Getter
@Builder
public class PasswordVerifyResponseDTO {

    /** Redis 에 저장된 비밀번호 변경용 임시 UUID (5분 TTL) */
    private String passwordResetToken;

    /** passwordResetToken 만료 시각 (epoch millis — 5분 TTL 기준) */
    private long expiresAt;

    public static PasswordVerifyResponseDTO of(String passwordResetToken, long expiresAt) {
        return PasswordVerifyResponseDTO.builder()
                .passwordResetToken(passwordResetToken)
                .expiresAt(expiresAt)
                .build();
    }
}
