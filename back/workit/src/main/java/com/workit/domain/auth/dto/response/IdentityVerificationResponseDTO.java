package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// 본인인증 검증 응답
// API 스펙(docs): identityToken, name
// - identityToken: CI 가 AES-256 으로 암호화된 임시 토큰
//   (실제 PortOne 연동/회원가입 플로우에서 JWT 발급으로 교체 예정 — jjwt 이미 의존성 존재)
// - name: 화면 표시 전용 (예: "홍길동님 환영합니다")
@Getter
@Builder
public class IdentityVerificationResponseDTO {

    private String identityToken;
    private String name;

    public static IdentityVerificationResponseDTO of(String identityToken, String name) {
        return IdentityVerificationResponseDTO.builder()
                .identityToken(identityToken)
                .name(name)
                .build();
    }
}
