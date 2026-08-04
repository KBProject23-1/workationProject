package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// 본인인증 검증 응답
// API 스펙(docs): identityToken, name
// - identityToken: 회원가입 전용 임시 JWT (Payload: sub, temporaryUserKey, iat, exp)
//   - 개인정보(name, phoneNumber, CI 원문, AES 암호화 CI)는 Payload 에 포함하지 않는다
//   - 회원가입 완료 API(#66)에서 서명 검증 후
//     Redis(signup:verification:{temporaryUserKey})에서 인증 데이터를 복원해 DB 에 저장한다
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
