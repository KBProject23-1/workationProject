package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.MockIdentityVerificationProvider;
import com.workit.domain.auth.util.SignupTokenProvider;
import com.workit.domain.auth.vo.TermsVO;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// AuthServiceImpl 단위 테스트
// - Mockito 의존성이 없으므로 AuthMapper/저장소를 수동 Fake 로, Provider 는 Mock 구현체로 주입한다
// - API 응답 구조(CommonResponse, data.termsList)는 Controller 계층에서 확인하도록 유지
class AuthServiceImplTest {

    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef"; // 32바이트
    private static final String TEST_JWT_SECRET = "fedcba9876543210fedcba9876543210"; // 32바이트

    // Mock Provider 는 CI = "MOCK-CI-" + identityVerificationId 를 반환한다.
    // CI SHA-256 해시는 프로덕션 해시 로직을 테스트가 복제하지 않도록 미리 계산한 값을 사용한다.
    private static final String CI_HASH_1234567890 =
            "71e9474f56472fabcc6fc9daf7008e17d92f11dffc705e7c542147ac5eb1e87a";
    private static final String CI_HASH_5555555555 =
            "2952fad00678028cbcceedb02f32745939a9c38e35330babb76cc607818f5a3f";
    private static final String CI_HASH_9999999999 =
            "00f5bd77a441d1d2b1a15cc14402e1cb6888ead3c017443fb20875b28f341497";

    private AuthService authService;
    private InMemorySignupVerificationStore signupVerificationStore;

    // 수동 Fake Mapper - 테스트에서 원하는 약관 목록 / CI 해시 중복 상태를 그대로 돌려준다
    private static class FakeAuthMapper implements AuthMapper {

        private final List<TermsVO> terms;

        /** 이미 가입된 회원으로 간주할 CI SHA-256 해시 집합 */
        private final Set<String> existingCiHashes;

        FakeAuthMapper(List<TermsVO> terms) {
            this(terms, Collections.emptySet());
        }

        FakeAuthMapper(List<TermsVO> terms, Set<String> existingCiHashes) {
            this.terms = terms;
            this.existingCiHashes = existingCiHashes;
        }

        @Override
        public List<TermsVO> selectTermsList() {
            return terms;
        }

        @Override
        public int countByCiHash(String ciHash) {
            return existingCiHashes.contains(ciHash) ? 1 : 0;
        }
    }

    // 인메모리 Fake 저장소 - Redis 없이 Service 플로우(저장/조회)를 검증한다
    // data 필드는 테스트가 저장 여부를 직접 확인할 수 있도록 패키지 접근으로 연다
    static class InMemorySignupVerificationStore implements SignupVerificationStore {

        final Map<String, SignupVerificationData> data = new HashMap<>();

        @Override
        public void save(String temporaryUserKey, SignupVerificationData value) {
            data.put(temporaryUserKey, value);
        }

        @Override
        public SignupVerificationData find(String temporaryUserKey) {
            return data.get(temporaryUserKey);
        }

        @Override
        public void delete(String temporaryUserKey) {
            data.remove(temporaryUserKey);
        }
    }

    @BeforeAll
    static void setUpAesKey() {
        // verifyIdentity 가 내부에서 PersonalDataCipher.encrypt() 를 호출하므로
        // 실행 환경(AES 환경변수)과 무관하게 동작하도록 키를 주입한다
        System.setProperty("personal.data.aes.key", TEST_AES_KEY);
        PersonalDataCipher.reloadKey();
    }

    @AfterAll
    static void tearDownAesKey() {
        // 다른 테스트 클래스에 영향이 없도록 원상 복구
        System.clearProperty("personal.data.aes.key");
        PersonalDataCipher.reloadKey();
    }

    private TermsVO term(Long id, String title, boolean required) {
        TermsVO vo = new TermsVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setContent("제1조: " + title + " 본문 내용");
        vo.setRequired(required);
        vo.setCreatedAt(LocalDateTime.of(2026, 7, 30, 0, 0));
        return vo;
    }

    @BeforeEach
    void setUp() {
        List<TermsVO> terms = Arrays.asList(
                term(1L, "서비스 이용약관", true),
                term(2L, "개인정보 수집 및 이용 동의", true),
                term(3L, "마케팅 정보 수신 동의", false)
        );
        signupVerificationStore = new InMemorySignupVerificationStore();
        authService = new AuthServiceImpl(
                new FakeAuthMapper(terms),
                new MockIdentityVerificationProvider(),
                new SignupTokenProvider(TEST_JWT_SECRET, 10),
                signupVerificationStore
        );
    }

    // ---------- 약관 목록 조회 ----------

    @Test
    @DisplayName("정상 약관 목록 조회 - 전체 약관을 DTO로 변환해 반환")
    void getTermsList_success() {
        TermsListResponseDTO result = authService.getTermsList();

        assertNotNull(result);
        assertNotNull(result.getTermsList());
        assertEquals(3, result.getTermsList().size());
    }

    @Test
    @DisplayName("Mapper 조회 결과가 Service 반환값에 그대로 반영되는지 검증")
    void getTermsList_mapsMapperResult() {
        TermsListResponseDTO result = authService.getTermsList();

        TermsResponseDTO first = result.getTermsList().get(0);
        assertEquals(1L, first.getTermId());
        assertEquals("서비스 이용약관", first.getTitle());
        assertEquals("제1조: 서비스 이용약관 본문 내용", first.getContent());
        assertTrue(first.getRequired());

        TermsResponseDTO optional = result.getTermsList().get(2);
        assertEquals(3L, optional.getTermId());
        assertEquals("마케팅 정보 수신 동의", optional.getTitle());
        assertFalse(optional.getRequired());
    }

    @Test
    @DisplayName("데이터 없음 처리 - 빈 목록을 반환")
    void getTermsList_emptyList() {
        AuthService emptyService = new AuthServiceImpl(
                new FakeAuthMapper(Collections.emptyList()),
                new MockIdentityVerificationProvider(),
                new SignupTokenProvider(TEST_JWT_SECRET, 10),
                new InMemorySignupVerificationStore()
        );

        TermsListResponseDTO result = emptyService.getTermsList();

        assertNotNull(result);
        assertNotNull(result.getTermsList());
        assertTrue(result.getTermsList().isEmpty());
    }

    // ---------- 본인인증 검증 ----------

    @Test
    @DisplayName("본인인증 성공 - 회원가입 전용 JWT(identityToken)와 name 반환 + 임시 데이터 Redis 저장")
    void verifyIdentity_success() {
        IdentityVerificationResponseDTO result =
                authService.verifyIdentity("imp_ver_1234567890");

        assertNotNull(result);
        assertEquals("홍길동", result.getName());

        // identityToken 은 JWT 형식 (헤더.페이로드.서명 3부분)
        String token = result.getIdentityToken();
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        // Payload 검증 — 회원가입용 claims 만 포함 (개인정보 없음)
        Claims claims = new SignupTokenProvider(TEST_JWT_SECRET, 10).verifySignupToken(token);
        assertEquals(SignupTokenProvider.SUBJECT_SIGNUP_VERIFICATION, claims.getSubject());
        assertFalse(claims.containsKey("name"));
        assertFalse(claims.containsKey("phoneNumber"));
        assertFalse(claims.containsKey("ci"));
        assertFalse(claims.containsKey("encryptedCi"));

        // Redis(인메모리 Fake)에 저장된 임시 데이터 검증
        String temporaryUserKey = claims.get("temporaryUserKey", String.class);
        SignupVerificationData saved = signupVerificationStore.find(temporaryUserKey);
        assertNotNull(saved);
        assertEquals("imp_ver_1234567890", saved.getVerificationId());
        assertEquals(CI_HASH_1234567890, saved.getCiHash());

        // CI 는 원문이 아닌 AES 암호화본만 저장된다
        assertFalse(saved.getEncryptedCi().contains("MOCK-CI-"));
        assertEquals("MOCK-CI-imp_ver_1234567890",
                PersonalDataCipher.decrypt(saved.getEncryptedCi()));

        // name 도 암호화본만 저장된다 (개인정보 원문 Redis 저장 금지)
        assertNotEquals("홍길동", saved.getEncryptedName());
        assertEquals("홍길동", PersonalDataCipher.decrypt(saved.getEncryptedName()));
    }

    @Test
    @DisplayName("본인인증 실패 - 유효하지 않은 인증 ID는 예외 발생")
    void verifyIdentity_invalidId_throws() {
        assertThrows(BusinessException.class,
                () -> authService.verifyIdentity(MockIdentityVerificationProvider.INVALID_IDENTIFIER));
        assertThrows(BusinessException.class,
                () -> authService.verifyIdentity(""));
        assertThrows(BusinessException.class,
                () -> authService.verifyIdentity(null));
    }

    @Test
    @DisplayName("중복 가입 - 동일 CI로 이미 가입한 회원이 있으면 DUPLICATE_USER 예외 발생")
    void verifyIdentity_duplicateUser_throws() {
        // Mock Provider 는 CI = "MOCK-CI-" + identityVerificationId 를 반환한다.
        // SHA-256("MOCK-CI-imp_ver_9999999999") 값을 하드코딩해
        // Service 의 해시 로직과 무관하게 독립적으로 중복 감지 경로를 검증한다.
        AuthService service = new AuthServiceImpl(
                new FakeAuthMapper(Collections.emptyList(),
                        Collections.singleton(CI_HASH_9999999999)),
                new MockIdentityVerificationProvider(),
                new SignupTokenProvider(TEST_JWT_SECRET, 10),
                signupVerificationStore
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.verifyIdentity("imp_ver_9999999999"));

        assertEquals(AuthErrorCode.DUPLICATE_USER, ex.getErrorCode());

        // 중복 감지 시 임시 데이터가 저장되지 않아야 한다
        assertTrue(signupVerificationStore.data.isEmpty());
    }

    @Test
    @DisplayName("중복 미해당 - CI가 없으면 정상 진행, JWT와 임시 데이터 저장 확인")
    void verifyIdentity_noDuplicate_success() {
        IdentityVerificationResponseDTO result =
                authService.verifyIdentity("imp_ver_5555555555");

        assertNotNull(result);
        assertEquals(3, result.getIdentityToken().split("\\.").length);

        Claims claims = new SignupTokenProvider(TEST_JWT_SECRET, 10)
                .verifySignupToken(result.getIdentityToken());

        SignupVerificationData saved =
                signupVerificationStore.find(claims.get("temporaryUserKey", String.class));
        assertNotNull(saved);
        assertEquals(CI_HASH_5555555555, saved.getCiHash());
    }
}
