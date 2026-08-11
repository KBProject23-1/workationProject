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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// MockPassServiceImpl 단위 테스트
// - 프론트가 생성한 identityVerificationId 가 VERIFIED 세션으로 등록되는지 검증한다
// - Redis 없이 동작하도록 인메모리 Fake MockPassStore 사용
class MockPassServiceImplTest {

    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef"; // 32바이트
    private static final String TEST_ID = "mock-12345678";
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
    @DisplayName("정상 완료 등록 - VERIFIED 세션 생성 + name 반환 + 개인정보 암호화 저장")
    void complete_success() {
        MockPassStatusResponseDTO result = service.complete(completeRequest(TEST_ID, TEST_NAME, TEST_PHONE));

        assertEquals(TEST_ID, result.getIdentityVerificationId());
        assertEquals(MockPassStatus.VERIFIED.name(), result.getStatus());
        assertEquals(TEST_NAME, result.getName());

        // 세션은 VERIFIED + 개인정보는 AES 암호화본으로만 저장 (원문 저장 금지)
        MockPassSession session = store.find(TEST_ID);
        assertNotNull(session);
        assertEquals(MockPassStatus.VERIFIED.name(), session.getStatus());
        assertTrue(!TEST_NAME.equals(session.getEncryptedName()));
        assertTrue(!TEST_PHONE.equals(session.getEncryptedPhone()));
        assertEquals(TEST_NAME, PersonalDataCipher.decrypt(session.getEncryptedName()));
        assertEquals(TEST_PHONE, PersonalDataCipher.decrypt(session.getEncryptedPhone()));
    }

    @Test
    @DisplayName("동일 ID 재등록 - 기존 세션 덮어쓰기 (멱등)")
    void complete_idempotentOverwrite() {
        service.complete(completeRequest(TEST_ID, "홍길동", TEST_PHONE));
        MockPassStatusResponseDTO result = service.complete(completeRequest(TEST_ID, "김철수", "01098765432"));

        assertEquals(TEST_ID, result.getIdentityVerificationId());
        assertEquals(MockPassStatus.VERIFIED.name(), result.getStatus());
        assertEquals("김철수", result.getName());
        assertEquals("김철수", PersonalDataCipher.decrypt(store.find(TEST_ID).getEncryptedName()));
    }

    @Test
    @DisplayName("identityVerificationId 누락/빈 값 - INVALID_VERIFICATION_ID")
    void complete_blankId_throws() {
        assertThrows(BusinessException.class,
                () -> service.complete(completeRequest(null, TEST_NAME, TEST_PHONE)));
        assertThrows(BusinessException.class,
                () -> service.complete(completeRequest("   ", TEST_NAME, TEST_PHONE)));
    }

    @Test
    @DisplayName("name/phoneNumber 누락 - INVALID_PASS_REQUEST")
    void complete_missingRequiredFields_throws() {
        MockPassCompleteRequestDTO noName = completeRequest(TEST_ID, "", TEST_PHONE);
        BusinessException e1 = assertThrows(BusinessException.class, () -> service.complete(noName));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e1.getErrorCode());

        MockPassCompleteRequestDTO noPhone = completeRequest(TEST_ID, TEST_NAME, "  ");
        BusinessException e2 = assertThrows(BusinessException.class, () -> service.complete(noPhone));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e2.getErrorCode());
    }

    @Test
    @DisplayName("Mock ID 형식 오류 - mock- 접두어 없음/길이 초과 → INVALID_PASS_REQUEST")
    void complete_invalidIdFormat_throws() {
        // mock- 접두어가 없으면 거부 (실제 PortOne 형식 ID 로 우회 불가)
        BusinessException e1 = assertThrows(BusinessException.class,
                () -> service.complete(completeRequest("imp_ver_1234567890", TEST_NAME, TEST_PHONE)));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e1.getErrorCode());

        // 길이 초과 (Java 8 호환 — 반복문 사용)
        StringBuilder sb = new StringBuilder("mock-");
        for (int i = 0; i < 200; i++) {
            sb.append('a');
        }
        BusinessException e2 = assertThrows(BusinessException.class,
                () -> service.complete(completeRequest(sb.toString(), TEST_NAME, TEST_PHONE)));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e2.getErrorCode());
    }

    @Test
    @DisplayName("휴대폰 형식 오류 - 하이픈/9자리 → INVALID_PASS_REQUEST")
    void complete_invalidPhoneFormat_throws() {
        MockPassCompleteRequestDTO hyphen = completeRequest(TEST_ID, TEST_NAME, "010-1234-5678");
        BusinessException e1 = assertThrows(BusinessException.class, () -> service.complete(hyphen));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e1.getErrorCode());

        MockPassCompleteRequestDTO shortPhone = completeRequest(TEST_ID, TEST_NAME, "010123456");
        BusinessException e2 = assertThrows(BusinessException.class, () -> service.complete(shortPhone));
        assertEquals(AuthErrorCode.INVALID_PASS_REQUEST, e2.getErrorCode());
    }

    // ---------- 헬퍼 ----------

    private MockPassCompleteRequestDTO completeRequest(String id, String name, String phone) {
        MockPassCompleteRequestDTO request = new MockPassCompleteRequestDTO();
        request.setIdentityVerificationId(id);
        request.setName(name);
        request.setPhoneNumber(phone);
        return request;
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
