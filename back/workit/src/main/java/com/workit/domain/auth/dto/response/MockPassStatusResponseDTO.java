package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// Mock PASS 인증 상태 응답 (verify / status / cancel 공용)
// POST /api/v1/auth/pass/verify
// GET  /api/v1/auth/pass/{identityVerificationId}
// POST /api/v1/auth/pass/{identityVerificationId}/cancel
//
// - status: PENDING | VERIFIED | FAILED | CANCELLED (MockPassStatus)
// - name: VERIFIED 상태에서만 사용자 이름을 함께 반환 (화면 표시용 — 본인이 입력한 값)
@Getter
@Builder
public class MockPassStatusResponseDTO {

    private String identityVerificationId;
    private String status;
    private String name;

    public static MockPassStatusResponseDTO of(String identityVerificationId, String status, String name) {
        return MockPassStatusResponseDTO.builder()
                .identityVerificationId(identityVerificationId)
                .status(status)
                .name(name)
                .build();
    }
}
