package com.workit.domain.auth.provider;

import com.workit.domain.auth.MockPassStatus;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.service.MockPassSession;
import com.workit.domain.auth.service.MockPassStore;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// MockIdentityVerificationProvider 단위 테스트
//
// - 백엔드가 발급한 Mock PASS 세션(Redis)이 VERIFIED 상태일 때만 인증 성공으로 수용하는지 검증한다.
// - PENDING(인증 미완료)/세션 없음 → INVALID_VERIFICATION_ID (프론트 우회 불가 확인)
class MockIdentityVerificationProviderTest {

    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef"; // 32바이트

    private FakeMockPassStore store;
    private IdentityVerificationProvider provider;

    @BeforeEach
    void setUp() {
        System.setProperty("personal.data.aes.key", TEST_AES_KEY);
        PersonalDataCipher.reloadKey();
        store = new FakeMockPassStore();
        provider = new MockIdentityVerificationProvider(store);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("personal.data.aes.key");
        PersonalDataCipher.reloadKey();
    }

    @Test
    @DisplayName("VERIFIED 세션 → CI/이름/휴대폰 포함 결과 반환")
    void verify_verifiedSession_success() {
        // Given — Mock PASS 검증(OTP)을 통과한 VERIFIED 세션이 Redis 에 존재
        store.save("mock-12345678", verifiedSession("mock-12345678"));

        // When
        IdentityVerificationResult result = provider.verify("mock-12345678");

        // Then — 세션에 저장된 개인정보(복호화)가 그대로 반환된다
        assertNotNull(result);
        assertFalse(result.getCi().trim().isEmpty());
        assertEquals("MOCK-CI-mock-12345678", result.getCi());
        assertEquals("홍길동", result.getName());
        assertEquals("01012345678", result.getPhoneNumber());
    }

    @Test
    @DisplayName("PENDING 세션 → INVALID_VERIFICATION_ID (인증 미완료 — 흐름 우회 불가)")
    void verify_pendingSession_throws() {
        // Given — 인증 시작만 한 상태 (OTP 검증 전)
        MockPassSession session = new MockPassSession();
        session.setIdentityVerificationId("mock-12345678");
        session.setStatus(MockPassStatus.PENDING.name());
        store.save("mock-12345678", session);

        // When / Then
        BusinessException e = assertThrows(BusinessException.class,
                () -> provider.verify("mock-12345678"));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, e.getErrorCode());
    }

    @Test
    @DisplayName("세션 없음(임의 ID 입력) → INVALID_VERIFICATION_ID")
    void verify_noSession_throws() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> provider.verify("mock-00000000"));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, e.getErrorCode());
    }

    @Test
    @DisplayName("null/빈 인증 ID → INVALID_VERIFICATION_ID 예외")
    void verify_blankIdentifier_throws() {
        assertThrows(BusinessException.class, () -> provider.verify(null));
        assertThrows(BusinessException.class, () -> provider.verify(""));
        assertThrows(BusinessException.class, () -> provider.verify("   "));
    }

    /** VERIFIED 상태의 Mock 세션 생성 — 개인정보는 AES-256 암호화본 저장 (지식 규칙 준수) */
    private MockPassSession verifiedSession(String id) {
        return MockPassSession.builder()
                .identityVerificationId(id)
                .status(MockPassStatus.VERIFIED.name())
                .encryptedName(PersonalDataCipher.encrypt("홍길동"))
                .encryptedPhone(PersonalDataCipher.encrypt("01012345678"))
                .build();
    }

    /** 인메모리 MockPassStore — Redis 구현과 동일 계약 (테스트 전용) */
    private static class FakeMockPassStore implements MockPassStore {

        private final Map<String, MockPassSession> sessions = new HashMap<>();

        @Override
        public void save(String identityVerificationId, MockPassSession session) {
            sessions.put(identityVerificationId, session);
        }

        @Override
        public MockPassSession find(String identityVerificationId) {
            return sessions.get(identityVerificationId);
        }

        @Override
        public void delete(String identityVerificationId) {
            sessions.remove(identityVerificationId);
        }
    }
}
