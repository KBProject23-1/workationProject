package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// 회원가입 본인인증 검증 및 회원 중복 체크 응답
// API 스펙(docs): POST /api/v1/auth/signup/verify-identity → data { identityToken, name }
// - name 은 화면 표시 전용 (docs: "홍길동님 환영합니다" 문구용)
// - identityToken 은 docs 에 정의되어 있으나, 현재 회원가입이 identityVerificationId 를
//   직접 사용해 Redis 세션을 검증·복원하므로 별도 발급하지 않는다 (API 계약 변경 없이 기존 흐름 유지)
// - 개인정보(name 외 phoneNumber/CI 등)는 응답에 포함하지 않는다 (knowledge.md: 노출 최소화)
@Getter
@Builder
public class VerifyIdentityResponseDTO {

    /** 화면 표시용 유저 이름 (PASS 인증 세션에서 복원) */
    private String name;

    public static VerifyIdentityResponseDTO of(String name) {
        return VerifyIdentityResponseDTO.builder()
                .name(name)
                .build();
    }
}
