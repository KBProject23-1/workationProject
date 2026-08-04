package com.workit.domain.auth.provider;

// 본인인증(PASS) 검증 Provider 인터페이스
//
// knowledge.md Identity Verification Policy 흐름:
//   PortOne identityVerificationId
//     ↓
//   Verify provider result          ← 이 인터페이스가 담당
//     ↓
//   Extract CI
//     ↓
//   Encrypt CI (AES-256)
//     ↓
//   Compare with user identity_ci
//
// - 실제 PortOne 연동은 별도 이슈에서 PortOneIdentityVerificationProvider 로 구현
// - 현재는 MockIdentityVerificationProvider 가 대체 (API 키 없이 테스트 가능)
public interface IdentityVerificationProvider {

    /**
     * PASS 본인인증 결과를 검증하고 인증 정보를 반환한다.
     *
     * @param identityVerificationId PortOne 본인인증 고유 번호 (imp_ver_...)
     * @return 검증된 인증 정보 (CI, 이름)
     * @throws com.workit.exception.BusinessException 인증이 유효하지 않은 경우
     */
    IdentityVerificationResult verify(String identityVerificationId);
}
