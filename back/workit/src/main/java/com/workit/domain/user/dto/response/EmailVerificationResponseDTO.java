package com.workit.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

// 로그인 사용자 이메일 인증번호 발송 응답 DTO
// API 스펙(docs): POST /api/v1/users/me/email/verification → data { email }
//
// - email 은 인증번호를 발송한(정규화된) 이메일 — 실제 이메일은 발송하지 않는 Mock 방식이므로
//   프론트가 발송 대상 이메일을 확인할 수 있도록 응답에 포함한다
// - 인증번호(verificationCode)는 응답에 포함하지 않는다 — 운영 환경에서 인증번호 노출 금지 (docs),
//   개발 환경에서는 [MOCK EMAIL] 로그로만 확인한다
@Getter
@Builder
public class EmailVerificationResponseDTO {

    /** 인증번호를 발송한 이메일 (정규화된 값) */
    private String email;

    public static EmailVerificationResponseDTO of(String email) {
        return EmailVerificationResponseDTO.builder()
                .email(email)
                .build();
    }
}
