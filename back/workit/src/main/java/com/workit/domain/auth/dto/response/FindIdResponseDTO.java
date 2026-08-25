package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// 아이디 찾기 응답
// API 스펙(docs): POST /api/v1/auth/find-id → data.email, data.createdAt
//
// 보안 규칙 (knowledge.md: 개인정보 노출 최소화):
// - 아이디(이메일) 원문은 응답에 포함하지 않고 마스킹본만 반환한다
//   예: user1234@example.com → user****@example.com (EmailMasker 규칙)
@Getter
@Builder
public class FindIdResponseDTO {

    /** 마스킹된 아이디(이메일) — 로컬파트 앞 4자리 + '****' + 도메인 */
    private String email;

    /** 가입일 (yyyy-MM-dd) — docs 응답 예시: "2026-07-24" */
    private String createdAt;

    public static FindIdResponseDTO of(String maskedEmail, String createdAt) {
        return FindIdResponseDTO.builder()
                .email(maskedEmail)
                .createdAt(createdAt)
                .build();
    }
}
