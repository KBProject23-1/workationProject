package com.workit.domain.user.dto.response;

import lombok.Builder;
import lombok.Getter;

// 로그인 사용자 이메일 변경 응답 DTO
// API 스펙(docs): PATCH /api/v1/users/me/email → data { updatedEmail }
//
// 개인정보 규칙 (knowledge.md):
// - updatedEmail 은 EmailVerificationStore 에 인증 완료(VERIFIED) 상태로 저장된 이메일을
//   Service 가 복호화한 값 — 프론트가 전달한 이메일이 아닌 백엔드 인증 결과 값
//   (이메일 변경 API 는 Request Body 를 받지 않는다 — docs 보안 조건)
@Getter
@Builder
public class EmailChangeResponseDTO {

    /** 이메일 인증을 완료해 변경된 이메일 주소 */
    private String updatedEmail;

    public static EmailChangeResponseDTO of(String updatedEmail) {
        return EmailChangeResponseDTO.builder()
                .updatedEmail(updatedEmail)
                .build();
    }
}
