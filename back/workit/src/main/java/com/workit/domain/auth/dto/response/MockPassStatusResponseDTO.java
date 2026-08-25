package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// Mock PASS 인증 응답
// POST /api/v1/auth/pass
//
// - status: PENDING | VERIFIED | FAILED | CANCELLED (MockPassStatus)
// - 개인정보(name/phoneNumber/CI)는 응답에 포함하지 않는다.
//   프론트는 발급받은 identityVerificationId 만 보관하고,
//   회원가입/아이디 찾기 등 후속 API 에 identityVerificationId 만 전달하면
//   백엔드가 Redis 세션에서 인증 정보를 복원한다.
@Getter
@Builder
public class MockPassStatusResponseDTO {

    /** Mock PASS 인증 고유 번호 — MockPassService 가 생성해 프론트에 발급한다 */
    private String identityVerificationId;

    /** 인증 상태 (VERIFIED) */
    private String status;

    public static MockPassStatusResponseDTO of(String identityVerificationId, String status) {
        return MockPassStatusResponseDTO.builder()
                .identityVerificationId(identityVerificationId)
                .status(status)
                .build();
    }
}
