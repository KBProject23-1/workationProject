package com.workit.domain.user.service;

import com.workit.domain.user.exception.UserErrorCode;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// MockEmailVerificationServiceImpl 단위 테스트
// - 6자리 숫자 인증번호 생성/이메일 AES 암호화 저장/재발급 시 기존 인증번호 무효화/유효시간 설정을 검증한다
// - Redis 없이 동작하도록 인메모리 Fake EmailVerificationStore 사용
//   (MockPassServiceImplTest 의 FakeMockPassStore 와 동일 패턴)
class MockEmailVerificationServiceImplTest {

    /** 테스트용 AES 키 (32바이트) — PersonalDataCipher 키 로드 규칙 1순위(시스템 프로퍼티) 사용 */
    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef";

    /** 테스트용 인증 대상 이메일 (정규화된 값) */
    private static final String EMAIL = "new@example.com";

    /** 인증번호 형식 — 6자리 숫자 (docs) */
    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");

    /** 테스트용 저장소 TTL (docs: 인증번호 유효시간 5분) */
    private static final Duration TTL = Duration.ofMinutes(5);

    private FakeEmailVerificationStore store;
    private MockEmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        System.setProperty("personal.data.aes.key", TEST_AES_KEY);
        PersonalDataCipher.reloadKey();
        store = new FakeEmailVerificationStore(TTL);
        service = new MockEmailVerificationServiceImpl(store);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("personal.data.aes.key");
        PersonalDataCipher.reloadKey();
    }

    @Test
    @DisplayName("정상 발급 - 6자리 숫자 인증번호 생성 + 이메일 AES 암호화본 저장 + verified=false + 만료 시각 설정")
    void issue_success() {
        // When
        service.issueVerificationCode(EMAIL);

        // Then — 이메일별 Mock 인증 정보가 저장된다 (email/verificationCode/expiresAt/verified)
        EmailVerificationSession session = store.find(EMAIL);
        assertNotNull(session);
        // 6자리 숫자 인증번호
        assertTrue(CODE_PATTERN.matcher(session.getVerificationCode()).matches(),
                "인증번호는 6자리 숫자여야 한다");
        // 이메일은 AES-256 암호화본으로만 저장 (원문 저장 금지 — knowledge.md)
        assertFalse(EMAIL.equals(session.getEncryptedEmail()));
        assertEquals(EMAIL, PersonalDataCipher.decrypt(session.getEncryptedEmail()));
        // 인증 완료 전 상태
        assertFalse(session.isVerified());
        // 만료 시각이 설정되어 있다 (유효시간 5분)
        assertTrue(session.getExpiresAt() > 0);
    }

    @Test
    @DisplayName("유효시간 설정 - expiresAt 이 발급 시각 + 저장소 TTL(5분) 기준으로 설정된다")
    void issue_setsExpiry() {
        // When
        long before = System.currentTimeMillis();
        service.issueVerificationCode(EMAIL);
        long after = System.currentTimeMillis();

        // Then — expiresAt ≈ now + TTL(5분)
        EmailVerificationSession session = store.find(EMAIL);
        long ttlMillis = TTL.toMillis();
        assertTrue(session.getExpiresAt() >= before + ttlMillis - 1000,
                "expiresAt 은 발급 시각 + TTL(5분) 이상이어야 한다");
        assertTrue(session.getExpiresAt() <= after + ttlMillis,
                "expiresAt 은 발급 시각 + TTL(5분) 이내여야 한다");
    }

    @Test
    @DisplayName("재발급 - 같은 이메일 재발송 시 기존 인증번호가 폐기되고 새 인증번호가 발급된다")
    void issue_reissueReplacesExisting() {
        // Given — 첫 번째 발급
        service.issueVerificationCode(EMAIL);
        EmailVerificationSession first = store.find(EMAIL);

        // When — 같은 이메일로 재발급
        service.issueVerificationCode(EMAIL);

        // Then — 저장소에는 최신 세션 하나만 유지된다 (같은 key 덮어쓰기 — 기존 인증번호 무효화)
        assertEquals(1, store.sessions.size());
        EmailVerificationSession second = store.find(EMAIL);
        assertNotNull(second);
        // 새 인증번호가 발급되어 기존 인증번호가 무효화된다
        assertNotEquals(first.getVerificationCode(), second.getVerificationCode());
        assertTrue(CODE_PATTERN.matcher(second.getVerificationCode()).matches());
    }

    @Test
    @DisplayName("발급 실패 - 임시 저장소 오류 시 EMAIL_VERIFICATION_SEND_FAILED (docs: 인증번호 발급 실패)")
    void issue_storeFailure() {
        // Given — 저장소가 오류를 던진다 (직렬화/Redis 장애 등)
        store.failSave = true;

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.issueVerificationCode(EMAIL));
        assertEquals(UserErrorCode.EMAIL_VERIFICATION_SEND_FAILED, ex.getErrorCode());
    }

    // ---------- 인증번호 확인 (confirmVerificationCode) ----------

    @Test
    @DisplayName("인증번호 확인 성공 - 올바른 인증번호 입력 시 인증 완료(verified=true) 상태로 저장")
    void confirm_success() {
        // Given — 인증번호 발급 완료
        service.issueVerificationCode(EMAIL);
        String code = store.find(EMAIL).getVerificationCode();

        // When — 발급된 인증번호와 동일한 값을 입력
        service.confirmVerificationCode(EMAIL, code);

        // Then — 예외 없이 인증 완료되며, 인증정보가 verified=true 로 저장된다
        //   (docs: 인증 성공 시 인증 완료 상태 저장 — 이후 이메일 변경 API 에서 사용)
        EmailVerificationSession session = store.find(EMAIL);
        assertNotNull(session);
        assertTrue(session.isVerified(), "인증 성공 후 인증정보는 verified=true 여야 한다");
    }

    @Test
    @DisplayName("인증번호 확인 - 잘못된 인증번호 → EMAIL_VERIFICATION_CODE_INVALID + verified 유지(false)")
    void confirm_wrongCode() {
        // Given — 인증번호 발급 완료
        service.issueVerificationCode(EMAIL);

        // When & Then — 저장된 인증번호와 다른 값을 입력하면 인증 실패
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmVerificationCode(EMAIL, "000000"));
        assertEquals(UserErrorCode.EMAIL_VERIFICATION_CODE_INVALID, ex.getErrorCode());

        // 인증 실패 시 인증정보가 인증 완료 상태로 변경되지 않아야 한다
        assertFalse(store.find(EMAIL).isVerified());
    }

    @Test
    @DisplayName("인증번호 확인 - 발송된 인증정보가 존재하지 않음 → EMAIL_VERIFICATION_NOT_FOUND")
    void confirm_notFound() {
        // Given — 인증번호 미발급 (저장소에 인증정보 없음)

        // When & Then — 인증번호를 입력해도 발송 기록이 없어 인증 실패
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmVerificationCode(EMAIL, "123456"));
        assertEquals(UserErrorCode.EMAIL_VERIFICATION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("인증번호 확인 - 유효시간(5분)이 지난 인증번호 → EMAIL_VERIFICATION_CODE_EXPIRED")
    void confirm_expired() {
        // Given — 인증번호 발급 후 만료 시각을 과거로 변경 (유효시간 경과)
        service.issueVerificationCode(EMAIL);
        store.find(EMAIL).setExpiresAt(System.currentTimeMillis() - 1000);
        String code = store.find(EMAIL).getVerificationCode();

        // When & Then — 만료된 인증번호는 올바른 값이어도 사용할 수 없다 (docs)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmVerificationCode(EMAIL, code));
        assertEquals(UserErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("인증번호 확인 - 이미 인증 완료된 인증번호 재사용 → EMAIL_ALREADY_VERIFIED (재사용 불가)")
    void confirm_alreadyVerified() {
        // Given — 1회 인증 성공 (verified=true 상태)
        service.issueVerificationCode(EMAIL);
        String code = store.find(EMAIL).getVerificationCode();
        service.confirmVerificationCode(EMAIL, code);

        // When & Then — 동일 인증번호로 재인증 시도 시 실패 (docs: 인증 성공 후 동일 인증번호 재사용 불가)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.confirmVerificationCode(EMAIL, code));
        assertEquals(UserErrorCode.EMAIL_ALREADY_VERIFIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("인증번호 확인 - 인증 성공 후 인증 완료 상태가 Store 에 정상적으로 저장된다")
    void confirm_storesVerifiedStateInStore() {
        // Given — 인증번호 발급 완료
        service.issueVerificationCode(EMAIL);
        String code = store.find(EMAIL).getVerificationCode();
        assertFalse(store.find(EMAIL).isVerified());

        // When — 인증 확인 성공
        service.confirmVerificationCode(EMAIL, code);

        // Then — Store 에 저장된 인증정보가 verified=true 상태로 유지된다
        //   (이후 이메일 변경 API 가 인증 완료된 이메일을 조회/사용 — docs)
        assertTrue(store.find(EMAIL).isVerified());
    }

    /** 인메모리 EmailVerificationStore — Redis 구현과 동일 계약 (테스트 전용) */
    private static class FakeEmailVerificationStore implements EmailVerificationStore {

        private final Map<String, EmailVerificationSession> sessions = new HashMap<>();
        private final Duration ttl;
        private boolean failSave;

        FakeEmailVerificationStore(Duration ttl) {
            this.ttl = ttl;
        }

        @Override
        public void save(String email, EmailVerificationSession session) {
            if (failSave) {
                throw new IllegalStateException("Redis connection error");
            }
            sessions.put(email, session);
        }

        @Override
        public EmailVerificationSession find(String email) {
            return sessions.get(email);
        }

        @Override
        public void delete(String email) {
            sessions.remove(email);
        }

        @Override
        public Duration getTtl() {
            return ttl;
        }
    }
}
