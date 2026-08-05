package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.MockIdentityVerificationProvider;
import com.workit.domain.auth.util.JwtTokenProvider;
import com.workit.domain.auth.util.SignupTokenProvider;
import com.workit.domain.auth.vo.LoginUserVO;
import com.workit.domain.auth.vo.TermsVO;
import com.workit.domain.auth.vo.UserAuthVO;
import com.workit.domain.auth.vo.UserProfileVO;
import com.workit.domain.auth.vo.UserVO;
import com.workit.domain.wallet.dto.request.ChargeRequest;
import com.workit.domain.wallet.dto.request.RefundRequest;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;
import com.workit.domain.wallet.dto.response.WalletResponse;
import com.workit.domain.wallet.service.WalletService;
import com.workit.exception.BusinessException;
import com.workit.global.util.PasswordEncryptor;
import com.workit.global.util.PersonalDataCipher;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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

    // 이메일 중복 확인 테스트용 사전 계산 hash
    // SHA-256("used@example.com") — 이미 가입된 이메일로 간주
    private static final String EMAIL_HASH_USED =
            "aefc1b2b97574c8419760aa407a5ee332933d95b0b905b5c80f3473588d526e3";
    // SHA-256("new@example.com") — 가입 가능한 이메일
    private static final String EMAIL_HASH_NEW =
            "f0030501023327437b06e5c6f87df7871b8e704ae608d1d0b7b24fdd2a06c716";
    // SHA-256("test@example.com") — 회원가입 완료 테스트용 이메일
    private static final String EMAIL_HASH_TEST =
            "973dfe463ec85785f5f95af5ba3906eedb2d931c24e69824a89ea65dba4e813b";

    // Mock Provider 의 휴대폰 번호는 입력 id 로부터 유도된다.
    // imp_ver_1234567890 → 숫자 1234567890 → 뒤 8자리 34567890 → "01034567890"
    // SHA-256("01034567890") — users.phone_number_hash 저장값 검증용
    private static final String PHONE_HASH =
            "b741eec0f41484b79603ed742c3b036a996bfc22ed56672e995cc3f9cf9adfc9";
    private static final String MOCK_PHONE_NUMBER = "01034567890";

    private AuthService authService;
    private InMemorySignupVerificationStore signupVerificationStore;
    private FakeAuthMapper authMapper;
    private FakeWalletService walletService;
    private JwtTokenProvider jwtTokenProvider;
    private FakeRefreshTokenStore refreshTokenStore;
    private FakeLoginFailCounter loginFailCounter;

    // 수동 Fake Mapper - 테스트에서 원하는 약관 목록 / CI·이메일·닉네임 중복 상태를 그대로 돌려주고,
    // 회원가입 완료 시 insert 되는 데이터를 캡처해 테스트가 검증할 수 있게 한다
    private static class FakeAuthMapper implements AuthMapper {

        private final List<TermsVO> terms;

        /** 이미 가입된 회원으로 간주할 CI SHA-256 해시 집합 */
        private final Set<String> existingCiHashes;

        /** 이미 가입된 회원으로 간주할 이메일 SHA-256 해시 집합 */
        private final Set<String> existingEmailHashes;

        /** 이미 가입된 회원으로 간주할 닉네임 집합 */
        private final Set<String> existingNicknames;

        /** 회원가입 완료 시 insert 된 users 목록 (검증용) */
        final List<UserVO> insertedUsers = new ArrayList<>();

        /** 회원가입 완료 시 insert 된 user_auth 목록 (검증용) */
        final List<UserAuthVO> insertedUserAuths = new ArrayList<>();

        /** 회원가입 완료 시 insert 된 user_profile 목록 (검증용) */
        final List<UserProfileVO> insertedUserProfiles = new ArrayList<>();

        /** 회원가입 완료 시 insert 된 약관 동의(user_terms_agreements) 목록 (검증용) */
        final List<UserTermsInsert> insertedUserTerms = new ArrayList<>();

        /** users PK 자동 증가 흉내 — insert 마다 1씩 증가 */
        private long nextUserId = 1L;

        /** countByEmailHash 호출 횟수 — 길이 검증 실패 시 DB 조회가 발생하지 않는지 검증용 */
        int emailHashLookupCount = 0;

        /** 로그인 조회용 — email_hash 로 등록된 회원 (테스트에서 직접 등록) */
        final Map<String, LoginUserVO> usersByEmailHash = new HashMap<>();

        /** 로그인 조회용 — phone_number_hash 로 등록된 회원 (테스트에서 직접 등록) */
        final Map<String, LoginUserVO> usersByPhoneHash = new HashMap<>();

        /** 로그인 조회용 — device_id 로 등록된 회원 (테스트에서 직접 등록) */
        final Map<String, LoginUserVO> usersByDeviceId = new HashMap<>();

        /** 재발급 조회용 — userId 로 등록된 회원 (테스트에서 직접 등록) */
        final Map<Long, LoginUserVO> usersById = new HashMap<>();

        /** findUserByEmailHash 호출 시 사용된 hash 기록 — email_hash 조회 확인용 */
        final List<String> emailHashLoginLookups = new ArrayList<>();

        /** findUserByPhoneHash 호출 시 사용된 hash 기록 — phone_hash 조회 확인용 */
        final List<String> phoneHashLoginLookups = new ArrayList<>();

        FakeAuthMapper(List<TermsVO> terms) {
            this(terms, Collections.emptySet());
        }

        FakeAuthMapper(List<TermsVO> terms, Set<String> existingCiHashes) {
            this(terms, existingCiHashes, Collections.emptySet());
        }

        FakeAuthMapper(List<TermsVO> terms, Set<String> existingCiHashes, Set<String> existingEmailHashes) {
            this(terms, existingCiHashes, existingEmailHashes, Collections.emptySet());
        }

        FakeAuthMapper(List<TermsVO> terms, Set<String> existingCiHashes,
                       Set<String> existingEmailHashes, Set<String> existingNicknames) {
            this.terms = terms;
            // 테스트에서 중복 상태를 동적으로 추가할 수 있도록 가변 집합으로 복사한다
            this.existingCiHashes = new java.util.HashSet<>(existingCiHashes);
            this.existingEmailHashes = new java.util.HashSet<>(existingEmailHashes);
            this.existingNicknames = new java.util.HashSet<>(existingNicknames);
        }

        @Override
        public List<TermsVO> selectTermsList() {
            return terms;
        }

        @Override
        public List<Long> selectRequiredTermsIds() {
            List<Long> ids = new ArrayList<>();
            for (TermsVO term : terms) {
                if (Boolean.TRUE.equals(term.getRequired())) {
                    ids.add(term.getId());
                }
            }
            return ids;
        }

        @Override
        public List<Long> selectExistingTermIds(List<Long> termIds) {
            List<Long> existing = new ArrayList<>();
            for (Long termId : termIds) {
                for (TermsVO term : terms) {
                    if (termId != null && termId.equals(term.getId())) {
                        existing.add(termId);
                        break;
                    }
                }
            }
            return existing;
        }

        @Override
        public int insertUserTerms(Long userId, List<Long> termIds) {
            insertedUserTerms.add(new UserTermsInsert(userId, termIds));
            return 1;
        }

        /** user_terms_agreements insert 캡처용 — (userId, termIds) 쌍을 보관해 테스트가 검증한다 */
        static class UserTermsInsert {
            final long userId;
            final List<Long> termIds;

            UserTermsInsert(long userId, List<Long> termIds) {
                this.userId = userId;
                this.termIds = termIds;
            }
        }

        @Override
        public int countByCiHash(String ciHash) {
            return existingCiHashes.contains(ciHash) ? 1 : 0;
        }

        @Override
        public int countByEmailHash(String emailHash) {
            emailHashLookupCount++;
            return existingEmailHashes.contains(emailHash) ? 1 : 0;
        }

        @Override
        public int countByNickname(String nickname) {
            return existingNicknames.contains(nickname) ? 1 : 0;
        }

        @Override
        public int insertUser(UserVO user) {
            user.setId(nextUserId++);
            insertedUsers.add(user);
            return 1;
        }

        @Override
        public int insertUserAuth(UserAuthVO userAuth) {
            insertedUserAuths.add(userAuth);
            return 1;
        }

        @Override
        public int insertUserProfile(UserProfileVO userProfile) {
            insertedUserProfiles.add(userProfile);
            return 1;
        }

        @Override
        public LoginUserVO findUserByEmailHash(String emailHash) {
            emailHashLoginLookups.add(emailHash);
            return usersByEmailHash.get(emailHash);
        }

        @Override
        public LoginUserVO findUserByPhoneHash(String phoneHash) {
            phoneHashLoginLookups.add(phoneHash);
            return usersByPhoneHash.get(phoneHash);
        }

        @Override
        public LoginUserVO findUserByDeviceId(String deviceId) {
            return usersByDeviceId.get(deviceId);
        }

        @Override
        public LoginUserVO findUserById(Long userId) {
            return usersById.get(userId);
        }
    }

    // 수동 Fake WalletService - createWallet 이 호출되었는지(생성된 userId)를 기록한다
    static class FakeWalletService implements WalletService {

        final List<Long> createdWalletUserIds = new ArrayList<>();

        @Override
        public void createWallet(Long userId) {
            createdWalletUserIds.add(userId);
        }

        @Override
        public WalletResponse getMyWallet(Long userId) {
            throw new UnsupportedOperationException("테스트에서 사용하지 않음");
        }

        @Override
        public ChargeResponse charge(Long userId, ChargeRequest request) {
            throw new UnsupportedOperationException("테스트에서 사용하지 않음");
        }

        @Override
        public RefundResponse refund(Long userId, RefundRequest request) {
            throw new UnsupportedOperationException("테스트에서 사용하지 않음");
        }
    }

    // 인메모리 Fake 저장소 - Redis 없이 Refresh Token 저장(로그인) 플로우를 검증한다
    static class FakeRefreshTokenStore implements RefreshTokenStore {

        /** userId → 저장된 refreshTokenHash (SHA-256) */
        final Map<Long, String> saved = new HashMap<>();

        /** userId → 저장 시 사용된 TTL(초) */
        final Map<Long, Long> savedTtls = new HashMap<>();

        /** delete 호출된 userId 목록 */
        final List<Long> deletedUserIds = new ArrayList<>();

        @Override
        public void save(Long userId, String refreshTokenHash, long ttlSeconds) {
            saved.put(userId, refreshTokenHash);
            savedTtls.put(userId, ttlSeconds);
        }

        @Override
        public String find(Long userId) {
            return saved.get(userId);
        }

        @Override
        public void delete(Long userId) {
            saved.remove(userId);
            savedTtls.remove(userId);
            deletedUserIds.add(userId);
        }
    }

    // 인메모리 Fake 카운터 - Redis 없이 PIN 실패 횟수/잠금 플로우를 검증한다
    static class FakeLoginFailCounter implements LoginFailCounter {

        final Map<Long, Integer> counts = new HashMap<>();

        @Override
        public int getCount(Long userId) {
            return counts.getOrDefault(userId, 0);
        }

        @Override
        public void increment(Long userId) {
            counts.put(userId, getCount(userId) + 1);
        }

        @Override
        public void reset(Long userId) {
            counts.remove(userId);
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
        authMapper = new FakeAuthMapper(terms);
        walletService = new FakeWalletService();
        jwtTokenProvider = new JwtTokenProvider(TEST_JWT_SECRET, 15, 20160);
        refreshTokenStore = new FakeRefreshTokenStore();
        loginFailCounter = new FakeLoginFailCounter();
        authService = new AuthServiceImpl(
                authMapper,
                new MockIdentityVerificationProvider(),
                new SignupTokenProvider(TEST_JWT_SECRET, 10),
                signupVerificationStore,
                walletService,
                jwtTokenProvider,
                refreshTokenStore,
                loginFailCounter
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
        new InMemorySignupVerificationStore(),
        new FakeWalletService(),
        new JwtTokenProvider(TEST_JWT_SECRET, 15, 20160),
        new FakeRefreshTokenStore(),
        new FakeLoginFailCounter()
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

        // phone 도 암호화본만 저장된다 (원문 Redis 저장 금지)
        assertNotEquals(MOCK_PHONE_NUMBER, saved.getEncryptedPhone());
        assertEquals(MOCK_PHONE_NUMBER, PersonalDataCipher.decrypt(saved.getEncryptedPhone()));
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
        signupVerificationStore,
        new FakeWalletService(),
        new JwtTokenProvider(TEST_JWT_SECRET, 15, 20160),
        new FakeRefreshTokenStore(),
        new FakeLoginFailCounter()
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

    // ---------- 회원가입 이메일 중복 확인 ----------

    private AuthService emailServiceWithExisting(Set<String> existingEmailHashes) {
        return new AuthServiceImpl(
                new FakeAuthMapper(Collections.emptyList(),
                        Collections.emptySet(), existingEmailHashes),
                new MockIdentityVerificationProvider(),
        new SignupTokenProvider(TEST_JWT_SECRET, 10),
        new InMemorySignupVerificationStore(),
        new FakeWalletService(),
        new JwtTokenProvider(TEST_JWT_SECRET, 15, 20160),
        new FakeRefreshTokenStore(),
        new FakeLoginFailCounter()
        );
    }

    @Test
    @DisplayName("이메일 중복 확인 - 존재하지 않는 email_hash → available=true")
    void checkEmailAvailability_notExists_available() {
        AuthService service = emailServiceWithExisting(Collections.emptySet());

        EmailAvailabilityResponseDTO result = service.checkEmailAvailability("new@example.com");

        assertNotNull(result);
        assertTrue(result.isAvailable());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 존재하는 email_hash → available=false")
    void checkEmailAvailability_exists_duplicate() {
        AuthService service =
                emailServiceWithExisting(Collections.singleton(EMAIL_HASH_USED));

        EmailAvailabilityResponseDTO result = service.checkEmailAvailability("used@example.com");

        assertNotNull(result);
        assertFalse(result.isAvailable());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 대소문자 무관 (소문자 정규화 후 hash) → 동일 결과")
    void checkEmailAvailability_caseInsensitive() {
        // SHA-256("used@example.com") 이 이미 가입된 상태에서
        // 대문자 입력("USED@EXAMPLE.COM")도 소문자 정규화 후 hash 되므로 중복으로 판단해야 한다
        AuthService service =
                emailServiceWithExisting(Collections.singleton(EMAIL_HASH_USED));

        EmailAvailabilityResponseDTO upper = service.checkEmailAvailability("USED@EXAMPLE.COM");
        assertFalse(upper.isAvailable());

        EmailAvailabilityResponseDTO spaced = service.checkEmailAvailability("  used@example.com  ");
        assertFalse(spaced.isAvailable());
    }

    @Test
    @DisplayName("이메일 중복 확인 - null/blank → INVALID_EMAIL_FORMAT 예외")
    void checkEmailAvailability_blank_throws() {
        AuthService service = emailServiceWithExisting(Collections.emptySet());

        assertThrows(BusinessException.class, () -> service.checkEmailAvailability(null));
        assertThrows(BusinessException.class, () -> service.checkEmailAvailability(""));
        assertThrows(BusinessException.class, () -> service.checkEmailAvailability("   "));
    }

    @Test
    @DisplayName("이메일 중복 확인 - 잘못된 형식 → INVALID_EMAIL_FORMAT 예외")
    void checkEmailAvailability_invalidFormat_throws() {
        AuthService service = emailServiceWithExisting(Collections.emptySet());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkEmailAvailability("not-an-email"));
        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 최대 길이(254자) 초과 → INVALID_EMAIL_FORMAT + DB 조회 미발생")
    void checkEmailAvailability_tooLong_throws() {
        FakeAuthMapper mapper = new FakeAuthMapper(Collections.emptyList(),
                Collections.emptySet(), Collections.emptySet());
        AuthService service = new AuthServiceImpl(
                mapper,
                new MockIdentityVerificationProvider(),
        new SignupTokenProvider(TEST_JWT_SECRET, 10),
        new InMemorySignupVerificationStore(),
        new FakeWalletService(),
        new JwtTokenProvider(TEST_JWT_SECRET, 15, 20160),
        new FakeRefreshTokenStore(),
        new FakeLoginFailCounter()
        );

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkEmailAvailability(buildLongEmail(255)));

        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());
        // 길이 검증 실패 시 DB(hash) 조회가 발생하지 않아야 한다
        assertEquals(0, mapper.emailHashLookupCount);
    }

    @Test
    @DisplayName("이메일 중복 확인 - 최대 길이(254자) 경계 → 정상 처리 + hash 조회 1회")
    void checkEmailAvailability_maxLengthBoundary_success() {
        FakeAuthMapper mapper = new FakeAuthMapper(Collections.emptyList(),
                Collections.emptySet(), Collections.emptySet());
        AuthService service = new AuthServiceImpl(
                mapper,
                new MockIdentityVerificationProvider(),
        new SignupTokenProvider(TEST_JWT_SECRET, 10),
        new InMemorySignupVerificationStore(),
        new FakeWalletService(),
        new JwtTokenProvider(TEST_JWT_SECRET, 15, 20160),
        new FakeRefreshTokenStore(),
        new FakeLoginFailCounter()
        );

        EmailAvailabilityResponseDTO result = service.checkEmailAvailability(buildLongEmail(254));

        assertNotNull(result);
        assertTrue(result.isAvailable());
        assertEquals(1, mapper.emailHashLookupCount);
    }

    /**
     * 지정한 전체 길이의 이메일 생성 — 로컬파트 64자 + @ + 도메인 + .com (RFC 5321 형식 유지)
     * - Java 8 호환을 위해 String.repeat 대신 반복문 사용
     */
    private String buildLongEmail(int totalLength) {
        int domainLength = totalLength - 65; // 로컬파트 64자 + @ 1자 제외
        StringBuilder sb = new StringBuilder(totalLength);
        for (int i = 0; i < 64; i++) {
            sb.append('a');
        }
        sb.append('@');
        for (int i = 0; i < domainLength - 4; i++) {
            sb.append('b');
        }
        sb.append(".com");
        return sb.toString();
    }

    // ---------- 최종 회원가입 완료 ----------

    /** 회원가입 요청 DTO 생성 헬퍼 — 기본값으로 필수 약관(1, 2) 전체 동의 상태를 만든다 */
    private SignupRequestDTO signupRequest(String identityToken, String email, String nickname) {
        return signupRequest(identityToken, email, nickname, Arrays.asList(1L, 2L));
    }

    /** 회원가입 요청 DTO 생성 헬퍼 — 동의 약관 ID 목록 지정 */
    private SignupRequestDTO signupRequest(String identityToken, String email, String nickname,
                                           List<Long> agreedTermsIds) {
        SignupRequestDTO request = new SignupRequestDTO();
        request.setIdentityToken(identityToken);
        request.setEmail(email);
        request.setPassword("password123!");
        request.setNickname(nickname);
        request.setAgreedTermsIds(agreedTermsIds);
        return request;
    }

    /** verifyIdentity 를 거쳐 발급된 유효한 identityToken 과 저장소 상태를 재사용한다 */
    private String issueValidIdentityToken() {
        IdentityVerificationResponseDTO result =
                authService.verifyIdentity("imp_ver_1234567890");
        return result.getIdentityToken();
    }

    @Test
    @DisplayName("정상 회원가입 - users/user_auth/user_profile insert + 지갑 생성 + Redis 삭제 + 암호화 저장")
    void signup_success() {
        String token = issueValidIdentityToken();

        authService.signup(signupRequest(token, "test@example.com", "tester"));

        // users insert 검증
        assertEquals(1, authMapper.insertedUsers.size());
        UserVO user = authMapper.insertedUsers.get(0);
        assertNotNull(user.getId());
        assertEquals("ACTIVE", user.getStatus());
        // email_hash / email_encrypt
        assertEquals(EMAIL_HASH_TEST, user.getEmailHash());
        assertEquals("test@example.com", PersonalDataCipher.decrypt(user.getEmailEncrypt()));
        // name_encrypt
        assertEquals("홍길동", PersonalDataCipher.decrypt(user.getNameEncrypt()));
        // phone_number_hash / phone_number_encrypt
        assertEquals(PHONE_HASH, user.getPhoneNumberHash());
        assertEquals(MOCK_PHONE_NUMBER, PersonalDataCipher.decrypt(user.getPhoneNumberEncrypt()));

        // user_auth insert 검증
        assertEquals(1, authMapper.insertedUserAuths.size());
        UserAuthVO userAuth = authMapper.insertedUserAuths.get(0);
        assertEquals(user.getId(), userAuth.getUserId());
        // password 는 BCrypt 해시 (원문과 다르고 matches 검증 통과)
        assertNotEquals("password123!", userAuth.getPasswordHash());
        assertTrue(PasswordEncryptor.matches("password123!", userAuth.getPasswordHash()));
        // CI hash / encrypt
        assertEquals(CI_HASH_1234567890, userAuth.getIdentityCiHash());
        assertEquals("MOCK-CI-imp_ver_1234567890",
                PersonalDataCipher.decrypt(userAuth.getIdentityCiEncrypt()));

        // user_profile insert 검증
        assertEquals(1, authMapper.insertedUserProfiles.size());
        UserProfileVO profile = authMapper.insertedUserProfiles.get(0);
        assertEquals(user.getId(), profile.getUserId());
        assertEquals("tester", profile.getNickname());

        // user_terms_agreements insert 검증 (약관 동의 저장 — 필수 약관 1, 2)
        assertEquals(1, authMapper.insertedUserTerms.size());
        assertEquals(user.getId(), authMapper.insertedUserTerms.get(0).userId);
        assertEquals(Arrays.asList(1L, 2L), authMapper.insertedUserTerms.get(0).termIds);

        // 전자지갑 생성 검증
        assertEquals(Collections.singletonList(user.getId()), walletService.createdWalletUserIds);

        // 회원가입 완료 후 Redis 임시 데이터 삭제 검증
        assertTrue(signupVerificationStore.data.isEmpty());
    }

    @Test
    @DisplayName("CI 중복 실패 - 최종 가입 시점에 동일 CI 가입자가 있으면 DUPLICATE_USER")
    void signup_duplicateCi_throws() {
        // verifyIdentity 단계와 동일한 CI 해시가 이미 가입된 상태로 구성
        String token = issueValidIdentityToken();

        authMapper.existingCiHashes.add(CI_HASH_1234567890);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester")));

        assertEquals(AuthErrorCode.DUPLICATE_USER, ex.getErrorCode());

        // 중복 감지 시 어떤 insert 도 발생하지 않아야 한다
        assertTrue(authMapper.insertedUsers.isEmpty());
        assertTrue(authMapper.insertedUserAuths.isEmpty());
        assertTrue(authMapper.insertedUserProfiles.isEmpty());
        // Redis 데이터는 삭제되지 않고 남아 있어야 재시도 가능
        assertFalse(signupVerificationStore.data.isEmpty());
    }

    @Test
    @DisplayName("이메일 중복 실패 - 동일 email_hash 가 있으면 DUPLICATE_EMAIL")
    void signup_duplicateEmail_throws() {
        String token = issueValidIdentityToken();

        authMapper.existingEmailHashes.add(EMAIL_HASH_TEST);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester")));

        assertEquals(AuthErrorCode.DUPLICATE_EMAIL, ex.getErrorCode());
        assertTrue(authMapper.insertedUsers.isEmpty());
    }

    @Test
    @DisplayName("닉네임 중복 실패 - 동일 nickname 이 있으면 DUPLICATE_NICKNAME")
    void signup_duplicateNickname_throws() {
        String token = issueValidIdentityToken();

        authMapper.existingNicknames.add("tester");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester")));

        assertEquals(AuthErrorCode.DUPLICATE_NICKNAME, ex.getErrorCode());
        assertTrue(authMapper.insertedUsers.isEmpty());
    }

    @Test
    @DisplayName("JWT 만료 - 만료된 identityToken 은 EXPIRED_SIGNUP_TOKEN")
    void signup_expiredToken_throws() {
        // verifyIdentity 는 유효한 임시 데이터를 저장하지만, 토큰은 과거 만료 시각으로 발급한다
        SignupVerificationData data = SignupVerificationData.builder()
                .verificationId("imp_ver_1234567890")
                .ciHash(CI_HASH_1234567890)
                .encryptedCi(PersonalDataCipher.encrypt("MOCK-CI-imp_ver_1234567890"))
                .encryptedName(PersonalDataCipher.encrypt("홍길동"))
                .encryptedPhone(PersonalDataCipher.encrypt(MOCK_PHONE_NUMBER))
                .build();
        signupVerificationStore.save("expired-key", data);

        String expiredToken = new SignupTokenProvider(TEST_JWT_SECRET, 10)
                .issue("expired-key", new Date(System.currentTimeMillis() - 60_000L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(expiredToken, "test@example.com", "tester")));

        assertEquals(AuthErrorCode.EXPIRED_SIGNUP_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("JWT 위변조 - 서명이 틀린 identityToken 은 INVALID_SIGNUP_TOKEN")
    void signup_tamperedToken_throws() {
        String token = issueValidIdentityToken();
        // 끝에서 두 번째 base64 글자를 바꾼다.
        // 마지막 글자는 256비트 서명의 패딩 비트만 담고 있어 'a'→'b' 교체 시
        // 복호화된 서명이 동일해질 수 있어(플레이크) 반드시 유효 비트를 바꾸는 위치를 사용한다
        String tampered = token.substring(0, token.length() - 2)
                + (token.charAt(token.length() - 2) == 'a' ? "b" : "a")
                + token.charAt(token.length() - 1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(tampered, "test@example.com", "tester")));

        assertEquals(AuthErrorCode.INVALID_SIGNUP_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("용도 오류 - 회원가입 전용 토큰이 아닌 JWT(sub 불일치)는 INVALID_SIGNUP_TOKEN")
    void signup_wrongSubjectToken_throws() {
        // 같은 시크릿으로 서명했지만 sub 만 다른 토큰 (회원가입 토큰 오용 방지 검증)
        String otherToken = Jwts.builder()
                .setSubject("access-token")
                .claim("temporaryUserKey", "temp-key-1")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(otherToken, "test@example.com", "tester")));

        assertEquals(AuthErrorCode.INVALID_SIGNUP_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("Redis 데이터 없음 - 토큰은 유효하지만 임시 데이터가 없으면 SIGNUP_VERIFICATION_NOT_FOUND")
    void signup_verificationNotFound_throws() {
        // 임시 데이터 저장 없이 유효한 토큰만 발급한다
        String token = new SignupTokenProvider(TEST_JWT_SECRET, 10).issue("no-data-key");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester")));

        assertEquals(AuthErrorCode.SIGNUP_VERIFICATION_NOT_FOUND, ex.getErrorCode());
        assertTrue(authMapper.insertedUsers.isEmpty());
    }

    @Test
    @DisplayName("필수 값 누락 - identityToken/email/password/nickname 누락은 INVALID_SIGNUP_REQUEST")
    void signup_missingRequired_throws() {
        // identityToken 누락
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest("", "test@example.com", "tester")));
        assertEquals(AuthErrorCode.INVALID_SIGNUP_REQUEST, ex.getErrorCode());

        // email 누락
        assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest("some-token", "", "tester")));

        // nickname 누락
        assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest("some-token", "test@example.com", "")));

        // null 요청
        assertThrows(BusinessException.class, () -> authService.signup(null));
    }

    @Test
    @DisplayName("이메일 형식 오류 - 잘못된 이메일은 INVALID_EMAIL_FORMAT")
    void signup_invalidEmailFormat_throws() {
        String token = issueValidIdentityToken();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "not-an-email", "tester")));

        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());
    }

    @Test
    @DisplayName("약관 동의 - 모든 필수 약관 동의 시 가입 성공 + 약관 동의 저장 (필수 + 선택)")
    void signup_allRequiredTermsAgreed_success() {
        String token = issueValidIdentityToken();

        // 필수(1, 2) + 선택(3) 전부 동의
        authService.signup(signupRequest(token, "test@example.com", "tester",
                Arrays.asList(1L, 2L, 3L)));

        assertEquals(1, authMapper.insertedUsers.size());
        // 동의한 약관이 그대로 저장된다
        assertEquals(1, authMapper.insertedUserTerms.size());
        assertEquals(authMapper.insertedUsers.get(0).getId(),
                authMapper.insertedUserTerms.get(0).userId);
        assertEquals(Arrays.asList(1L, 2L, 3L), authMapper.insertedUserTerms.get(0).termIds);
    }

    @Test
    @DisplayName("약관 동의 - 필수 약관 일부 누락 시 MISSING_REQUIRED_TERMS")
    void signup_missingRequiredTerms_throws() {
        String token = issueValidIdentityToken();

        // 필수 약관 2 번을 누락하고 1 번만 동의
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester",
                        Collections.singletonList(1L))));

        assertEquals(AuthErrorCode.MISSING_REQUIRED_TERMS, ex.getErrorCode());

        // 실패 시 어떤 insert 도 발생하지 않아야 한다
        assertTrue(authMapper.insertedUsers.isEmpty());
        assertTrue(authMapper.insertedUserAuths.isEmpty());
        assertTrue(authMapper.insertedUserProfiles.isEmpty());
        assertTrue(authMapper.insertedUserTerms.isEmpty());
    }

    @Test
    @DisplayName("약관 동의 - 존재하지 않는 약관 ID 포함 시 INVALID_TERM_ID")
    void signup_invalidTermId_throws() {
        String token = issueValidIdentityToken();

        // 필수(1, 2)는 동의했지만 terms 에 존재하지 않는 99 번이 섞여 있음
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester",
                        Arrays.asList(1L, 2L, 99L))));

        assertEquals(AuthErrorCode.INVALID_TERM_ID, ex.getErrorCode());

        // 실패 시 어떤 insert 도 발생하지 않아야 한다 (FK 위반 500 대신 400)
        assertTrue(authMapper.insertedUsers.isEmpty());
        assertTrue(authMapper.insertedUserTerms.isEmpty());
    }

    @Test
    @DisplayName("약관 동의 - null 요소 포함 시에도 INVALID_TERM_ID")
    void signup_nullTermId_throws() {
        String token = issueValidIdentityToken();

        // [1, 2, null] — null 요소는 terms 에 존재할 수 없으므로 검증 실패
        List<Long> agreedWithNull = new ArrayList<>(Arrays.asList(1L, 2L, null));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester",
                        agreedWithNull)));

        assertEquals(AuthErrorCode.INVALID_TERM_ID, ex.getErrorCode());
        assertTrue(authMapper.insertedUsers.isEmpty());
    }

    @Test
    @DisplayName("약관 동의 - 선택 약관 미동의 시에도 가입 성공")
    void signup_optionalTermsNotAgreed_success() {
        String token = issueValidIdentityToken();

        // 필수(1, 2)만 동의하고 선택(3)은 미동의
        authService.signup(signupRequest(token, "test@example.com", "tester",
                Arrays.asList(1L, 2L)));

        assertEquals(1, authMapper.insertedUsers.size());
        assertEquals(Arrays.asList(1L, 2L), authMapper.insertedUserTerms.get(0).termIds);
    }

    @Test
    @DisplayName("약관 동의 - agreedTermsIds 누락/빈 배열 시 MISSING_REQUIRED_TERMS")
    void signup_agreedTermsMissing_throws() {
        String token = issueValidIdentityToken();

        // null
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester", null)));
        assertEquals(AuthErrorCode.MISSING_REQUIRED_TERMS, nullEx.getErrorCode());

        // 빈 배열
        BusinessException emptyEx = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester",
                        Collections.emptyList())));
        assertEquals(AuthErrorCode.MISSING_REQUIRED_TERMS, emptyEx.getErrorCode());

        assertTrue(authMapper.insertedUsers.isEmpty());
        assertTrue(authMapper.insertedUserTerms.isEmpty());
    }

    @Test
    @DisplayName("약관 동의 - 중복 ID 전달 시 중복 제거 후 저장")
    void signup_duplicateTermIds_deduplicated() {
        String token = issueValidIdentityToken();

        authService.signup(signupRequest(token, "test@example.com", "tester",
                Arrays.asList(1L, 1L, 2L, 2L)));

        assertEquals(1, authMapper.insertedUserTerms.size());
        assertEquals(Arrays.asList(1L, 2L), authMapper.insertedUserTerms.get(0).termIds);
    }

    @Test
    @DisplayName("이메일 소문자 정규화 - 대문자 이메일도 소문자 hash 로 저장된다")
    void signup_emailLowercased() {
        String token = issueValidIdentityToken();

        authService.signup(signupRequest(token, "TEST@EXAMPLE.COM", "tester"));

        // SHA-256("test@example.com") 과 동일해야 한다 (소문자 정규화)
        assertEquals(EMAIL_HASH_TEST, authMapper.insertedUsers.get(0).getEmailHash());
        assertEquals("test@example.com",
                PersonalDataCipher.decrypt(authMapper.insertedUsers.get(0).getEmailEncrypt()));
    }

    @Test
    @DisplayName("회원가입 완료 - 최대 길이(254자) 이메일 정상 가입 (trim/lowercase 후 암호화·hash 저장)")
    void signup_emailMaxLength_success() {
        String token = issueValidIdentityToken();
        String maxLengthEmail = buildLongEmail(254);

        authService.signup(signupRequest(token, maxLengthEmail, "tester"));

        assertEquals(1, authMapper.insertedUsers.size());
        UserVO user = authMapper.insertedUsers.get(0);
        // 원문은 AES 암호화 저장 — 복호화 시 입력값과 동일
        assertEquals(maxLengthEmail, PersonalDataCipher.decrypt(user.getEmailEncrypt()));
        // email_hash 는 SHA-256 hex (64자)
        assertEquals(64, user.getEmailHash().length());
        // 전자지갑 생성까지 정상
        assertEquals(Collections.singletonList(user.getId()), walletService.createdWalletUserIds);
    }

    @Test
    @DisplayName("회원가입 완료 - 255자 이상 이메일 → INVALID_EMAIL_FORMAT + hash 조회/insert 미발생")
    void signup_emailTooLong_throws() {
        String token = issueValidIdentityToken();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, buildLongEmail(255), "tester")));

        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());
        // 길이 검증은 hash 생성 전에 수행되므로 DB(email_hash) 조회가 없어야 한다
        assertEquals(0, authMapper.emailHashLookupCount);
        // 어떤 insert 도 발생하지 않아야 한다 (DB insert 이전 차단)
        assertTrue(authMapper.insertedUsers.isEmpty());
        assertTrue(authMapper.insertedUserAuths.isEmpty());
        assertTrue(authMapper.insertedUserProfiles.isEmpty());
        assertTrue(authMapper.insertedUserTerms.isEmpty());
    }

    // ---------- 통합 로그인 ----------

    /**
     * 로그인 조회용 회원 등록 (Fake Mapper 에 email_hash/phone_hash/deviceId 별로 등록)
     * - password/pin 은 BCrypt 해시로 저장 (Service 검증 대상)
     * - name 은 AES 암호화본으로 저장 (응답 시 Service 에서 복호화)
     */
    private void registerLoginUser(Long userId, String email, String phoneNumber,
                                   String password, String pin, String deviceId, String status) {
        LoginUserVO user = new LoginUserVO();
        user.setId(userId);
        user.setStatus(status);
        user.setNameEncrypt(PersonalDataCipher.encrypt("홍길동"));
        user.setPasswordHash(PasswordEncryptor.encode(password));
        user.setPinHash(PasswordEncryptor.encode(pin));
        authMapper.usersByEmailHash.put(sha256(email), user);
        authMapper.usersByPhoneHash.put(sha256(phoneNumber), user);
        authMapper.usersByDeviceId.put(deviceId, user);
        authMapper.usersById.put(userId, user);
    }

    /** 로그인 요청 DTO 생성 헬퍼 */
    private LoginRequestDTO loginRequest(String loginType, String loginId, String password,
                                         String pinNumber, String deviceId) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setLoginType(loginType);
        request.setLoginId(loginId);
        request.setPassword(password);
        request.setPinNumber(pinNumber);
        request.setDeviceId(deviceId);
        return request;
    }

    /** 테스트용 SHA-256 hex — Service 의 hash 로직과 독립적으로 기대값을 계산한다 */
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

    @Test
    @DisplayName("PASSWORD 이메일 로그인 성공 - userId/name/token_info 반환 + 토큰 발급 + Refresh Token Redis 저장")
    void login_passwordEmail_success() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        LoginResponseDTO result =
                authService.login(loginRequest("PASSWORD", "user@example.com", "password123!", null, null));

        assertNotNull(result);
        assertEquals(501L, result.getUserId().longValue());
        assertEquals("홍길동", result.getName());

        // token_info (snake_case JSON 직렬화는 Controller 테스트에서 확인)
        LoginResponseDTO.TokenInfo tokenInfo = result.getTokenInfo();
        assertNotNull(tokenInfo);
        assertEquals("Bearer", tokenInfo.getGrantType());
        assertNotNull(tokenInfo.getAccessToken());
        assertEquals(900L, tokenInfo.getAccessTokenExpiresIn());

        // Access Token 검증 — sub == userId, tokenType == ACCESS, 개인정보 없음
        Claims accessClaims = jwtTokenProvider.parseAccessToken(tokenInfo.getAccessToken());
        assertEquals("501", accessClaims.getSubject());
        assertFalse(accessClaims.containsKey("email"));
        assertFalse(accessClaims.containsKey("phoneNumber"));
        assertFalse(accessClaims.containsKey("password"));
        assertFalse(accessClaims.containsKey("pin"));

        // Refresh Token — 쿠키용 값 + Redis 에 SHA-256 hash 저장 (원문 저장 금지)
        String refreshToken = result.getRefreshToken();
        assertNotNull(refreshToken);
        assertEquals(1209600L, result.getRefreshTokenMaxAgeSeconds());
        assertEquals(sha256(refreshToken), refreshTokenStore.saved.get(501L));
        assertNotEquals(refreshToken, refreshTokenStore.saved.get(501L));
        assertEquals(Long.valueOf(1209600L), refreshTokenStore.savedTtls.get(501L));

        // Refresh Token 검증 — sub == userId, tokenType == REFRESH
        Claims refreshClaims = jwtTokenProvider.parseRefreshToken(refreshToken);
        assertEquals("501", refreshClaims.getSubject());
    }

    @Test
    @DisplayName("PASSWORD 이메일 로그인 - trim + lowercase 정규화 후 email_hash 로만 조회")
    void login_passwordEmail_normalizesAndUsesEmailHash() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        authService.login(loginRequest("PASSWORD", "  USER@EXAMPLE.COM  ", "password123!", null, null));

        // email_encrypt(원문) 조회가 아니라 email_hash(SHA-256) 로만 조회했는지 확인
        assertEquals(1, authMapper.emailHashLoginLookups.size());
        assertEquals(sha256("user@example.com"), authMapper.emailHashLoginLookups.get(0));
        assertTrue(authMapper.phoneHashLoginLookups.isEmpty());
    }

    @Test
    @DisplayName("PASSWORD 휴대폰 로그인 성공 - 하이픈 제거 후 phone_hash 로만 조회")
    void login_passwordPhone_success() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        LoginResponseDTO result =
                authService.login(loginRequest("PASSWORD", "010-3456-7890", "password123!", null, null));

        assertNotNull(result);
        assertEquals(501L, result.getUserId().longValue());
        assertEquals("홍길동", result.getName());

        // phone_number_encrypt(원문) 조회가 아니라 phone_hash(SHA-256) 로만 조회했는지 확인
        assertEquals(1, authMapper.phoneHashLoginLookups.size());
        assertEquals(sha256("01034567890"), authMapper.phoneHashLoginLookups.get(0));
        assertTrue(authMapper.emailHashLoginLookups.isEmpty());
    }

    @Test
    @DisplayName("PASSWORD 실패 - 잘못된 password → INVALID_CREDENTIALS + 토큰/Redis 저장 없음")
    void login_passwordWrongPassword_throws() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PASSWORD", "user@example.com", "wrong-password!", null, null)));

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(refreshTokenStore.saved.isEmpty());
    }

    @Test
    @DisplayName("PASSWORD 실패 - 존재하지 않는 이메일 → INVALID_CREDENTIALS (원인 비노출)")
    void login_passwordUnknownUser_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PASSWORD", "unknown@example.com", "password123!", null, null)));

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(refreshTokenStore.saved.isEmpty());
    }

    @Test
    @DisplayName("PIN 로그인 성공 - deviceId 조회 + pin 검증 + 실패 횟수 초기화 + 토큰 발급")
    void login_pin_success() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        // 이전 실패 이력이 있던 사용자 — 성공 시 초기화되어야 한다
        loginFailCounter.counts.put(501L, 2);

        LoginResponseDTO result =
                authService.login(loginRequest("PIN", null, null, "123456", "device-uuid-1"));

        assertNotNull(result);
        assertEquals(501L, result.getUserId().longValue());
        assertEquals(0, loginFailCounter.getCount(501L));
        assertEquals(sha256(result.getRefreshToken()), refreshTokenStore.saved.get(501L));
    }

    @Test
    @DisplayName("PIN 실패 - 잘못된 pin → INVALID_CREDENTIALS + 실패 횟수 1 증가")
    void login_pinWrongPin_throws() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PIN", null, null, "000000", "device-uuid-1")));

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertEquals(1, loginFailCounter.getCount(501L));
        assertTrue(refreshTokenStore.saved.isEmpty());
    }

    @Test
    @DisplayName("PIN 실패 - 미등록 deviceId → INVALID_CREDENTIALS")
    void login_pinUnknownDevice_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PIN", null, null, "123456", "unknown-device")));

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(refreshTokenStore.saved.isEmpty());
    }

    @Test
    @DisplayName("PIN 잠금 - 실패 횟수 5회 이상 → PIN_LOCK_EXCEEDED + 토큰 미발급")
    void login_pinLockExceeded_throws() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        loginFailCounter.counts.put(501L, 5);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PIN", null, null, "123456", "device-uuid-1")));

        assertEquals(AuthErrorCode.PIN_LOCK_EXCEEDED, ex.getErrorCode());
        // 잠금 상태에서는 실패 횟수가 더 증가하지 않는다
        assertEquals(5, loginFailCounter.getCount(501L));
        assertTrue(refreshTokenStore.saved.isEmpty());
    }

    @Test
    @DisplayName("잘못된 loginType - PASSWORD/PIN 외 값 → INVALID_LOGIN_TYPE")
    void login_invalidLoginType_throws() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("FACE_ID", "user@example.com", "password123!", null, null)));

        assertEquals(AuthErrorCode.INVALID_LOGIN_TYPE, ex.getErrorCode());
    }

    @Test
    @DisplayName("필수 값 누락 - loginType/loginId/password/pinNumber/deviceId → INVALID_LOGIN_REQUEST")
    void login_missingRequired_throws() {
        // loginType 누락
        BusinessException typeEx = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest(null, "user@example.com", "password123!", null, null)));
        assertEquals(AuthErrorCode.INVALID_LOGIN_REQUEST, typeEx.getErrorCode());

        // PASSWORD - loginId 누락
        assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PASSWORD", "", "password123!", null, null)));
        // PASSWORD - password 누락
        assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PASSWORD", "user@example.com", null, null, null)));
        // PIN - pinNumber 누락
        assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PIN", null, null, "", "device-uuid-1")));
        // PIN - deviceId 누락
        assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PIN", null, null, "123456", "  ")));
        // null 요청
        assertThrows(BusinessException.class, () -> authService.login(null));
    }

    @Test
    @DisplayName("비활성 회원 - WITHDRAWN 상태는 로그인 불가 (INVALID_CREDENTIALS)")
    void login_withdrawnUser_throws() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "WITHDRAWN");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PASSWORD", "user@example.com", "password123!", null, null)));

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(refreshTokenStore.saved.isEmpty());
    }

    @Test
    @DisplayName("보안 - 요청 DTO toString 에 password/pinNumber 원문 미노출")
    void login_requestToStringHidesSecrets() {
        LoginRequestDTO request =
                loginRequest("PASSWORD", "user@example.com", "password123!", "123456", "device-uuid-1");

        String text = request.toString();
        assertFalse(text.contains("password123!"));
        assertFalse(text.contains("123456"));
    }

    // ---------- 로그인 토큰 재발급 (Refresh Token → Access Token) ----------

    /**
     * 로그인까지 거쳐 유효한 Refresh Token 세션(Redis hash 저장 포함)을 만든다.
     * - Service 가 발급한 refreshToken 과 Redis(refresh:token:{userId}) hash 가 일치하는 상태
     */
    private String issueRefreshTokenSession(Long userId) {
        registerLoginUser(userId, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        LoginResponseDTO loginResult =
                authService.login(loginRequest("PASSWORD", "user@example.com", "password123!", null, null));
        return loginResult.getRefreshToken();
    }

    @Test
    @DisplayName("정상 재발급 - 신규 Access Token 발급 + Refresh Token Rotation + Redis hash 교체")
    void refresh_success() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        // 명시적 만료(120초) 토큰으로 세션 구성 — Service 가 발급하는 기본 만료(14일) 토큰과 exp 가
        // 항상 달라 JWT 문자열이 동일해지는 타이밍 문제 없이 Rotation 을 결정적으로 검증한다
        String refreshToken = jwtTokenProvider.createRefreshToken(501L,
                new Date(System.currentTimeMillis() + 120_000L));
        refreshTokenStore.save(501L, sha256(refreshToken), 1209600L);
        String oldHash = refreshTokenStore.saved.get(501L);

        RefreshTokenResponseDTO result = authService.refreshAccessToken(refreshToken);

        // token_info — 신규 Access Token (sub == userId, tokenType == ACCESS, 개인정보 없음)
        assertNotNull(result);
        LoginResponseDTO.TokenInfo tokenInfo = result.getTokenInfo();
        assertNotNull(tokenInfo);
        assertEquals("Bearer", tokenInfo.getGrantType());
        assertNotNull(tokenInfo.getAccessToken());
        assertEquals(900L, tokenInfo.getAccessTokenExpiresIn());

        Claims accessClaims = jwtTokenProvider.parseAccessToken(tokenInfo.getAccessToken());
        assertEquals("501", accessClaims.getSubject());
        assertFalse(accessClaims.containsKey("email"));
        assertFalse(accessClaims.containsKey("password"));
        assertFalse(accessClaims.containsKey("pin"));

        // Rotation — 신규 Refresh Token 발급, exp 가 달라 이전 토큰과 다르고 tokenType == REFRESH
        String newRefreshToken = result.getRefreshToken();
        assertNotNull(newRefreshToken);
        assertNotEquals(refreshToken, newRefreshToken);
        Claims newRefreshClaims = jwtTokenProvider.parseRefreshToken(newRefreshToken);
        assertEquals("501", newRefreshClaims.getSubject());

        // Redis — 신규 hash 로 교체, TTL 은 refresh 만료와 동일
        assertEquals(sha256(newRefreshToken), refreshTokenStore.saved.get(501L));
        assertNotEquals(oldHash, refreshTokenStore.saved.get(501L));
        assertEquals(Long.valueOf(1209600L), refreshTokenStore.savedTtls.get(501L));
    }

    @Test
    @DisplayName("Rotation 후 이전 Refresh Token 재사용 - 세션 revoke + INVALID_REFRESH_TOKEN")
    void refresh_reuseAfterRotation_revokesSession() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        // 명시적 만료(120초) 토큰으로 1차 세션 구성 — Service 가 발급하는 기본 만료(14일) 토큰과
        // exp 가 항상 달라, 회전 후 이전 토큰 재사용 감지가 결정적으로 검증된다
        String refreshToken = jwtTokenProvider.createRefreshToken(501L,
                new Date(System.currentTimeMillis() + 120_000L));
        refreshTokenStore.save(501L, sha256(refreshToken), 1209600L);

        // 1차 재발급(Rotation) — 이전 refreshToken 은 더 이상 유효하지 않다
        RefreshTokenResponseDTO rotated = authService.refreshAccessToken(refreshToken);
        assertNotEquals(refreshToken, rotated.getRefreshToken());

        // 2차 요청에 회전 전 Refresh Token 사용 → 재사용 감지 → 세션 전체 revoke
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
        // 세션 revoke — Redis 저장 hash 삭제
        assertFalse(refreshTokenStore.saved.containsKey(501L));
        assertTrue(refreshTokenStore.deletedUserIds.contains(501L));
    }

    @Test
    @DisplayName("Redis hash 불일치 - 세션 revoke + INVALID_REFRESH_TOKEN")
    void refresh_hashMismatch_revokesSession() {
        String refreshToken = issueRefreshTokenSession(501L);
        // 저장된 hash 를 다른 값으로 덮어쓴다 (클라이언트 토큰 != 저장소 토큰 = 탈취/재사용 의심)
        refreshTokenStore.saved.put(501L, "forged-hash-value");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
        // 세션 revoke — 저장 hash 삭제
        assertFalse(refreshTokenStore.saved.containsKey(501L));
        assertTrue(refreshTokenStore.deletedUserIds.contains(501L));
    }

    @Test
    @DisplayName("Redis 저장 hash 없음(로그아웃/TTL 만료) - INVALID_REFRESH_TOKEN")
    void refresh_noStoredHash_throws() {
        String refreshToken = issueRefreshTokenSession(501L);
        // 로그아웃 등으로 Redis 세션이 삭제된 상태
        refreshTokenStore.delete(501L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("만료된 Refresh Token - INVALID_REFRESH_TOKEN")
    void refresh_expiredToken_throws() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        String expired = jwtTokenProvider.createRefreshToken(501L,
                new Date(System.currentTimeMillis() - 60_000L));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(expired));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("위변조된 Refresh Token - INVALID_REFRESH_TOKEN")
    void refresh_tamperedToken_throws() {
        String refreshToken = issueRefreshTokenSession(501L);
        // 끝에서 두 번째 base64 글자를 바꾼다 (signup 위변조 테스트와 동일한 방식)
        String tampered = refreshToken.substring(0, refreshToken.length() - 2)
                + (refreshToken.charAt(refreshToken.length() - 2) == 'a' ? "b" : "a")
                + refreshToken.charAt(refreshToken.length() - 1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(tampered));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("Access Token 으로 재발급 요청 - 용도 오류 → INVALID_REFRESH_TOKEN")
    void refresh_accessTokenMisuse_throws() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        String accessToken = jwtTokenProvider.createAccessToken(501L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(accessToken));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("탈퇴 회원 - ACTIVE 가 아니면 INVALID_REFRESH_TOKEN")
    void refresh_withdrawnUser_throws() {
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "WITHDRAWN");
        // 탈퇴 회원의 세션이 Redis 에 남아있더라도 재발급은 거부되어야 한다
        String refreshToken = jwtTokenProvider.createRefreshToken(501L);
        refreshTokenStore.save(501L, sha256(refreshToken), 1209600L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("존재하지 않는 회원 - INVALID_REFRESH_TOKEN")
    void refresh_unknownUser_throws() {
        String refreshToken = jwtTokenProvider.createRefreshToken(999L);
        refreshTokenStore.save(999L, sha256(refreshToken), 1209600L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("쿠키 누락 - null/blank Refresh Token → INVALID_REFRESH_TOKEN")
    void refresh_blankToken_throws() {
        assertThrows(BusinessException.class, () -> authService.refreshAccessToken(null));
        assertThrows(BusinessException.class, () -> authService.refreshAccessToken(""));
        assertThrows(BusinessException.class, () -> authService.refreshAccessToken("   "));
    }

    @Test
    @DisplayName("보안 - 응답 DTO toString 에 refreshToken 원문 미노출 (JWT 로그 유출 방지)")
    void refresh_responseToStringHidesToken() {
        RefreshTokenResponseDTO dto = RefreshTokenResponseDTO.builder()
                .tokenInfo(LoginResponseDTO.TokenInfo.of("Bearer", "access-token-jwt", 900))
                .refreshToken("secret-refresh-token-value")
                .refreshTokenMaxAgeSeconds(1209600)
                .build();

        String text = dto.toString();
        assertFalse(text.contains("secret-refresh-token-value"));
    }
}

