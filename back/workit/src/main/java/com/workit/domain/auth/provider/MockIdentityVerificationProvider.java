package com.workit.domain.auth.provider;

import com.workit.domain.auth.MockPassStatus;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.service.MockPassSession;
import com.workit.domain.auth.service.MockPassStore;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import org.springframework.stereotype.Component;

// PASS 본인인증 검증 Mock 구현체
//
// - 실제 PortOne 연동 이전까지 auth 플로우 개발/테스트를 가능하게 하는 임시 구현
// - 실연동 구현(PortOneIdentityVerificationProvider)으로 교체 시 Service 코드는 수정하지 않는다
//
// Mock 세션 연동 (백엔드가 인증 상태 관리):
//   MockPassService( POST /api/v1/auth/pass → OTP 검증 → VERIFIED )가 발급한
//   Redis(mock:pass:{identityVerificationId}) 세션 중 status == VERIFIED 인 경우에만 인증 성공으로 간주한다.
//   - 프론트가 인증 ID 를 임의 생성/입력하거나, 흐름을 건너뛰어 VERIFIED 를 우회할 수 없다.
//   - name / phoneNumber 는 Mock 세션에 저장된 사용자 입력값(Service 에서 AES 암호화)을 복호화해 반환한다.
@Component
public class MockIdentityVerificationProvider implements IdentityVerificationProvider {

    /** Mock CI 접두사 — 실제 CI 가 아니며 개인정보가 아님 */
    private static final String MOCK_CI_PREFIX = "MOCK-CI-";

    private final MockPassStore mockPassStore;

    public MockIdentityVerificationProvider(MockPassStore mockPassStore) {
        this.mockPassStore = mockPassStore;
    }

    @Override
    public IdentityVerificationResult verify(String identityVerificationId) {
        String id = identityVerificationId == null ? "" : identityVerificationId.trim();

        if (id.isEmpty()) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
        }

        // Mock PASS 세션 조회 — VERIFIED 상태여야만 인증 성공으로 인정한다
        // (세션 없음 / PENDING / FAILED / CANCELLED → 모두 유효하지 않은 인증)
        MockPassSession session = mockPassStore.find(id);
        if (session == null || !MockPassStatus.VERIFIED.name().equals(session.getStatus())) {
            throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
        }

        // 개인정보 복호화는 Service/Provider Layer 에서만 수행한다 (Controller/Mapper 금지)
        return IdentityVerificationResult.builder()
                .ci(MOCK_CI_PREFIX + id)
                .name(PersonalDataCipher.decrypt(session.getEncryptedName()))
                .phoneNumber(PersonalDataCipher.decrypt(session.getEncryptedPhone()))
                .build();
    }
}
