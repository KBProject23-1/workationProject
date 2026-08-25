package com.workit.domain.auth.service;

import com.workit.domain.auth.MockPassStatus;
import com.workit.domain.auth.dto.request.MockPassCompleteRequestDTO;
import com.workit.domain.auth.dto.response.MockPassStatusResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

// MockPassServiceImpl 단위 테스트
// - identityVerificationId 를 프론트가 보내지 않고 백엔드가 직접 생성/발급하는지 검증한다
// - CI 도 서비스가 생성해 세션에 암호화 저장하고, used=false 로 시작하는지 검증한다
// - Redis 없이 동작하도록 인메모리 Fake MockPassStore 사용
class MockPassServiceImplTest {

    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef"; // 32바이트
    private static final String TEST_NAME = "홍길동";
    private static final String TEST_PHONE = "01012345678";

    private FakeMockPassStore store;
    private MockPassService service;

    @BeforeEach
    void setUp() {
        System.setProperty("personal.data.aes.key", TEST_AES_KEY);
        PersonalDataCipher.reloadKey();
        store = new FakeMockPassStore();
        service = new MockPassServiceImpl(store);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("personal.data.aes.key");
        PersonalDataCipher.reloadKey();
    }

    @Test
    @DisplayName("정상 인증 - 백엔드가 identityVerificationId 생성 + VERIFIED(used=false) 세션 저장 + name 미반환")
    void complete_success() {
        // When — 프론트는 이름/휴대폰 번호만 보낸다 (identityVerificationId 없음)
        MockPassStatusResponseDTO result = service.complete(completeRequest(TEST_NAME, TEST_PHONE));

        // Then — 백엔드가 발급한 ID/status 만 반환한다 (개인정보 name 은 응답에 포함하지 않는다)
        assertNotNull(result.getIdentityVerificationId());
        assertEquals(MockPassStatus.VERIFIED.name(), result.getStatus());

        // identityVerificationId 는 백엔드가 생성한 값 (프론트 입력/전달 없음)
        assertFalse(result.getIdentityVerificationId().trim().isEmpty());
        assertTrue(result.getIdentityVerificationId().length() >= 32, "UUID 형식 길이");

        // 세션은 VERIFIED + used=false + 개인정보는 AES 암호화본으로만 저장 (원문 저장 금지)
        MockPassSession session = store.find(result.getIdentityVerificationId());
        assertNotNull(session);
        assertEquals(MockPassStatus.VERIFIED.name(), session.getStatus());
        assertFalse(session.isUsed());
        assertEquals(result.getIdentityVerificationId(), session.getIdentityVerificationId());

        // name/phoneNumber/CI 모두 암호화본만 저장된다
        assertTrue(!TEST_NAME.equals(session.getEncryptedName()));
        assertTrue(!TEST_PHONE.equals(session.getEncryptedPhone()));
        assertEquals(TEST_NAME, PersonalDataCipher.decrypt(session.getEncryptedName()));
        assertEquals(TEST_PHONE, PersonalDataCipher.decrypt(session.getEncryptedPhone()));

        // CI 는 동일 휴대폰 → 동일 결정값 (개인정보가 아닌 Mock 값)
        assertEquals("MOCK-CI-" + sha256Hex(TEST_PHONE),
                PersonalDataCipher.decrypt(session.getEncryptedCi()));
    }

    @Test
    @DisplayName("호출마다 새 identityVerificationId 발급 - 두 인증의 ID 는 서로 다르다")
    void complete_generatesDistinctIds() {
        MockPassStatusResponseDTO first = service.complete(completeRequest(TEST_NAME, TEST_PHONE));
        MockPassStatusResponseDTO second = service.complete(completeRequest("김철수", "01098765432"));

        assertNotEquals(first.getIdentityVerificationId(), second.getIdentityVerificationId());
        // 두 세션 모두 Redis(인메모리 저장소)에 보관된다
        assertEquals(2, store.sessions.size());
        assertNotNull(store.find(first.getIdentityVerificationId()));
        assertNotNull(store.find(second.getIdentityVerificationId()));
    }

    @Test
    @DisplayName("동일 휴대폰 재인증 - 세션 ID 는 달라도 CI 는 동일 (아이디 찾기 재인증 일치 보장)")
    void complete_samePhone_sameCi() {
        service.complete(completeRequest(TEST_NAME, TEST_PHONE));
        service.complete(completeRequest("홍길동", TEST_PHONE));

        // 서로 다른 두 세션이 존재한다
        assertEquals(2, store.sessions.size());

        // CI 는 휴대폰 기반 결정값이므로 동일하다
        String ci1 = PersonalDataCipher.decrypt(store.sessions.values().iterator().next().getEncryptedCi());
        for (MockPassSession session : store.sessions.values()) {
            assertEquals(ci1, PersonalDataCipher.decrypt(session.getEncryptedCi()));
        }
    }

    @Test
    @DisplayName("name/phoneNumber 누락 - INVALID_PASS_REQUEST")
    void complete_missingRequiredFields_throws() {
        MockPassCompleteRequestDTO noName = completeRequest("", TEST_PHONE);
        BusinessException e1 = assertThrows(BusinessException.class, () -> service.complete(noName));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e1.getErrorCode());

        MockPassCompleteRequestDTO noPhone = completeRequest(TEST_NAME, "  ");
        BusinessException e2 = assertThrows(BusinessException.class, () -> service.complete(noPhone));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e2.getErrorCode());

        // 실패 시 세션이 저장되지 않는다
        assertTrue(store.sessions.isEmpty());
    }

    @Test
    @DisplayName("휴대폰 형식 오류 - 하이픈/9자리 → INVALID_PASS_REQUEST")
    void complete_invalidPhoneFormat_throws() {
        MockPassCompleteRequestDTO hyphen = completeRequest(TEST_NAME, "010-1234-5678");
        BusinessException e1 = assertThrows(BusinessException.class, () -> service.complete(hyphen));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e1.getErrorCode());

        MockPassCompleteRequestDTO shortPhone = completeRequest(TEST_NAME, "010123456");
        BusinessException e2 = assertThrows(BusinessException.class, () -> service.complete(shortPhone));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e2.getErrorCode());

        assertTrue(store.sessions.isEmpty());
    }

    // ---------- 헬퍼 ----------

    private MockPassCompleteRequestDTO completeRequest(String name, String phone) {
        MockPassCompleteRequestDTO request = new MockPassCompleteRequestDTO();
        request.setName(name);
        request.setPhoneNumber(phone);
        return request;
    }

    /** 테스트용 SHA-256 hex — Service 의 CI 생성 규칙과 독립적으로 기대값을 계산한다 */
    private static String sha256Hex(String value) {
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
