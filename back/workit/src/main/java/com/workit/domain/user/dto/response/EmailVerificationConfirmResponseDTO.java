package com.workit.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

// 로그인 사용자 이메일 인증번호 확인 응답 DTO
// API 스펙(docs): POST /api/v1/users/me/email/verification/confirm → data { verified }
//
// - verified 는 이메일 인증 완료 여부 — 확인 성공 시 항상 true
//   (인증 성공 시 서버가 해당 이메일에 대한 인증 완료 상태(VERIFIED)를 저장한다)
// - 이 API 는 인증번호 검증 → 인증 완료 상태 저장까지만 담당하며,
//   실제 이메일 변경은 별도의 이메일 변경 API(PATCH /api/v1/users/me/email) 에서 처리한다
@Getter
@Builder
public class EmailVerificationConfirmResponseDTO {

    /** 이메일 인증 완료 여부 (확인 성공 시 true) */
    private boolean verified;

    public static EmailVerificationConfirmResponseDTO of(boolean verified) {
        return EmailVerificationConfirmResponseDTO.builder()
                .verified(verified)
                .build();
    }
}
