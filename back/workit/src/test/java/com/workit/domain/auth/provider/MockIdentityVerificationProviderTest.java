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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// MockIdentityVerificationProvider 단위 테스트
//
// - 백엔드가 발급한 Mock PASS 세션(Redis)이 VERIFIED && 미사용(used=false) 상태일 때만
//   인증 성공으로 수용하는지 검증한다.
// - 세션에 저장된 CI 를 복호화해 그대로 반환한다 (CI 는 MockPassService 가 발급 시점에 생성)
// - PENDING / 사용 완료(used=true) / 세션 없음 → INVALID_VERIFICATION_ID (프론트 우회 불가 확인)
class MockIdentityVerificationProviderTest {

    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef"; // 32바이트

    /** 테스트 휴대폰 — CI 기대값 계산에 사용 */
    private static final String TEST_PHONE = "01012345678";

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
    @DisplayName("VERIFIED 미사용 세션 → CI/이름/휴대폰 포함 결과 반환")
    void verify_verifiedSession_success() {
        // Given — Mock PASS 가 생성한 VERIFIED(used=false) 세션이 Redis 에 존재
        store.save("mock-12345678", verifiedSession("mock-12345678", TEST_PHONE));

        // When
        IdentityVerificationResult result = provider.verify("mock-12345678");

        // Then — 세션에 저장된 개인정보(복호화)가 그대로 반환된다
        assertNotNull(result);
        assertFalse(result.getCi().trim().isEmpty());
        // CI 는 MockPassService 가 발급 시점에 생성해 세션에 보관한 값 (휴대폰 SHA-256 결정값)
        assertEquals("MOCK-CI-" + sha256(TEST_PHONE), result.getCi());
        assertEquals("홍길동", result.getName());
        assertEquals(TEST_PHONE, result.getPhoneNumber());
    }

    @Test
    @DisplayName("동일 휴대폰 재인증 - 서로 다른 인증 ID(세션)여도 동일 CI 반환 (아이디 찾기 일치 보장)")
    void verify_samePhoneDifferentSession_sameCi() {
        // Given — 같은 사람이 회원가입과 아이디 찾기에서 각각 다른 인증 ID 로 인증한 상황
        store.save("mock-signup-abc", verifiedSession("mock-signup-abc", TEST_PHONE));
        store.save("mock-findid-xyz", verifiedSession("mock-findid-xyz", TEST_PHONE));

        // When — 서로 다른 인증 ID 로 각각 verify
        IdentityVerificationResult signupResult = provider.verify("mock-signup-abc");
        IdentityVerificationResult findIdResult = provider.verify("mock-findid-xyz");

        // Then — 동일 휴대폰이면 CI 가 동일해야 한다
        //   (회원가입 시 identity_ci_hash = SHA-256(CI) 저장 → 아이디 찾기 CI 와 일치해야 조회 가능)
        assertEquals(signupResult.getCi(), findIdResult.getCi());
    }

    @Test
    @DisplayName("서로 다른 휴대폰 - CI 가 달라야 한다 (1인 1계정 식별)")
    void verify_differentPhone_differentCi() {
        // Given — 휴대폰이 다른 두 사람의 VERIFIED 세션
        store.save("mock-user-1111", verifiedSession("mock-user-1111", "01011111111"));
        store.save("mock-user-2222", verifiedSession("mock-user-2222", "01022222222"));

        // When / Then — 휴대폰이 다르면 CI 도 달라야 한다
        assertNotEquals(provider.verify("mock-user-1111").getCi(),
                provider.verify("mock-user-2222").getCi());
    }

    @Test
    @DisplayName("사용 완료(used=true) 세션 → INVALID_VERIFICATION_ID (재사용 방지)")
    void verify_usedSession_throws() {
        // Given — 회원가입 등으로 이미 사용 완료 처리된 세션
        MockPassSession session = verifiedSession("mock-12345678", TEST_PHONE);
        session.setUsed(true);
        store.save("mock-12345678", session);

        // When / Then — 같은 인증 ID 로 재사용할 수 없다 (1회성 인증)
        BusinessException e = assertThrows(BusinessException.class,
                () -> provider.verify("mock-12345678"));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, e.getErrorCode());
    }

    @Test
    @DisplayName("PENDING 세션 → INVALID_VERIFICATION_ID (인증 미완료 — 흐름 우회 불가)")
    void verify_pendingSession_throws() {
        // Given — 인증 시작만 한 상태
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

    /** VERIFIED 미사용 상태의 Mock 세션 생성 — 개인정보는 AES-256 암호화본 저장 (지식 규칙 준수) */
    private MockPassSession verifiedSession(String id, String phone) {
        return MockPassSession.builder()
                .identityVerificationId(id)
                .status(MockPassStatus.VERIFIED.name())
                .encryptedName(PersonalDataCipher.encrypt("홍길동"))
                .encryptedPhone(PersonalDataCipher.encrypt(phone))
                .encryptedCi(PersonalDataCipher.encrypt("MOCK-CI-" + sha256(phone)))
                .used(false)
                .build();
    }

    /** 테스트용 SHA-256 hex — MockPassService 의 CI 생성 규칙과 독립적으로 기대값을 계산한다 */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
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
        public void markUsed(String identityVerificationId) {
            MockPassSession session = sessions.get(identityVerificationId);
            if (session != null) {
                session.setUsed(true);
            }
        }

        @Override
        public void delete(String identityVerificationId) {
            sessions.remove(identityVerificationId);
        }
    }
}
