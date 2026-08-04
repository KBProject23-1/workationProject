package com.workit.domain.auth.provider;

import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// MockIdentityVerificationProvider 단위 테스트
// - 실제 PortOne 연동 없이 provider 계약(성공/실패)을 검증한다
class MockIdentityVerificationProviderTest {

    private final IdentityVerificationProvider provider = new MockIdentityVerificationProvider();

    @Test
    @DisplayName("정상 인증 ID → CI 와 이름을 포함한 결과 반환")
    void verify_success() {
        IdentityVerificationResult result = provider.verify("imp_ver_1234567890");

        assertNotNull(result);
        assertNotNull(result.getCi());
        assertFalse(result.getCi().trim().isEmpty());
        assertNotNull(result.getName());
        assertFalse(result.getName().trim().isEmpty());
        assertNotNull(result.getPhoneNumber());
        assertFalse(result.getPhoneNumber().trim().isEmpty());
        // Mock CI 는 입력 ID 기반으로 생성된다
        assertEquals("MOCK-CI-imp_ver_1234567890", result.getCi());
        // Mock 휴대폰 번호도 제공된다 (users.phone_number_* 저장 대상)
        // imp_ver_1234567890 → 숫자 1234567890 → 뒤 8자리 34567890 → 01034567890
        assertEquals("01034567890", result.getPhoneNumber());
    }

    @Test
    @DisplayName("null/빈 인증 ID → INVALID_VERIFICATION_ID 예외")
    void verify_blankIdentifier_throws() {
        assertThrows(BusinessException.class, () -> provider.verify(null));
        assertThrows(BusinessException.class, () -> provider.verify(""));
        assertThrows(BusinessException.class, () -> provider.verify("   "));
    }

    @Test
    @DisplayName("실패 마커(INVALID_IDENTIFIER) → INVALID_VERIFICATION_ID 예외")
    void verify_invalidMarker_throws() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> provider.verify(MockIdentityVerificationProvider.INVALID_IDENTIFIER));

        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, e.getErrorCode());
    }
}
