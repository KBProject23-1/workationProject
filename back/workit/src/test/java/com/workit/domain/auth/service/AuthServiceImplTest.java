package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.PasswordResetRequestDTO;
import com.workit.domain.auth.dto.request.PasswordVerifyRequestDTO;
import com.workit.domain.auth.dto.request.PinResetRequestDTO;
import com.workit.domain.auth.dto.request.PinSetupRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.FindIdResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.PasswordVerifyResponseDTO;
import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.IdentityVerificationProvider;
import com.workit.domain.auth.provider.IdentityVerificationResult;
import com.workit.domain.auth.provider.MockIdentityVerificationProvider;
import com.workit.domain.auth.util.JwtTokenProvider;
import com.workit.domain.auth.util.SignupTokenProvider;
import com.workit.domain.auth.vo.LoginUserVO;
import com.workit.domain.auth.vo.TermsVO;
import com.workit.domain.auth.vo.UserAuthVO;
import com.workit.domain.auth.vo.UserDeviceVO;
import com.workit.domain.auth.vo.UserProfileVO;
import com.workit.domain.auth.vo.UserVO;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// AuthServiceImpl 단위 테스트
// - AuthMapper/저장소/Provider 를 Mockito @Mock 으로 주입한다
// - JwtTokenProvider/SignupTokenProvider 는 실제 인스턴스로 사용한다 (실제 JWT 발급·검증 검증)
// - Redis 저장소/카운터는 Mockito Answer 로 상태형 인메모리 Mock 을 구성해 수동 Fake 를 대체한다
// - @InjectMocks 를 사용하지 않는 이유: JWT Provider 는 실제 인스턴스가 필요하고(Mock 대체 시
//   토큰 발급·검증 검증 불가), 상태형 Answer 를 함께 구성해야 하므로 생성자 직접 주입을 사용한다
// - 상태형 Mock 의 백킹 맵은 인스턴스 필드로, JUnit 기본 PER_METHOD 라이프사이클이 테스트마다 초기화한다
// - API 응답 구조(CommonResponse, data.termsList)는 Controller 계층에서 확인하도록 유지
@ExtendWith(MockitoExtension.class)
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

    @Mock
    private AuthMapper authMapper;

    @Mock
    private IdentityVerificationProvider identityVerificationProvider;

    @Mock
    private SignupVerificationStore signupVerificationStore;

    @Mock
    private WalletService walletService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private LoginFailCounter loginFailCounter;

    @Mock
    private PasswordResetTokenStore passwordResetTokenStore;

    // 실제 JWT Provider — 토큰 발급/검증은 Mock 대신 실제 구현으로 검증한다
    private JwtTokenProvider jwtTokenProvider;
    private SignupTokenProvider signupTokenProvider;

    private AuthService authService;

    // 상태형 Mock 의 백킹 저장소 — Mockito Answer 로 수동 Fake 를 대체한다
    // (signupVerificationStore.data / refreshTokenStore.saved / loginFailCounter.counts 에 대응)
    private final Map<String, SignupVerificationData> savedSignupData = new HashMap<>();
    private final Map<Long, String> savedRefreshTokens = new HashMap<>();
    private final Map<Long, Integer> failCounts = new HashMap<>();
    private final Map<String, Long> savedPasswordResetTokens = new HashMap<>();

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

    /** 약관 목록 조회 테스트 기본 데이터 — 필수 2건 + 선택 1건 */
    private List<TermsVO> defaultTerms() {
        return Arrays.asList(
                term(1L, "서비스 이용약관", true),
                term(2L, "개인정보 수집 및 이용 동의", true),
                term(3L, "마케팅 정보 수신 동의", false)
        );
    }

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(TEST_JWT_SECRET, 15, 20160);
        signupTokenProvider = new SignupTokenProvider(TEST_JWT_SECRET, 10);
        authService = new AuthServiceImpl(
                authMapper,
                identityVerificationProvider,
                signupTokenProvider,
                signupVerificationStore,
                walletService,
                jwtTokenProvider,
                refreshTokenStore,
                loginFailCounter,
                passwordResetTokenStore
        );

        // SignupVerificationStore 상태형 Mock — 저장/조회/삭제를 인메모리 맵으로 흉내낸다
        // (기존 InMemorySignupVerificationStore 를 Mockito Answer 로 대체)
        lenient().doAnswer(invocation -> {
            savedSignupData.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(signupVerificationStore).save(anyString(), any(SignupVerificationData.class));
        lenient().when(signupVerificationStore.find(anyString()))
                .thenAnswer(invocation -> savedSignupData.get(invocation.getArgument(0)));
        lenient().doAnswer(invocation -> {
            savedSignupData.remove(invocation.getArgument(0));
            return null;
        }).when(signupVerificationStore).delete(anyString());

        // RefreshTokenStore 상태형 Mock — Redis 세션을 인메모리 맵으로 흉내낸다
        // (기존 FakeRefreshTokenStore 를 Mockito Answer 로 대체)
        lenient().doAnswer(invocation -> {
            savedRefreshTokens.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(refreshTokenStore).save(anyLong(), anyString(), anyLong());
        lenient().when(refreshTokenStore.find(anyLong()))
                .thenAnswer(invocation -> savedRefreshTokens.get(invocation.getArgument(0)));
        lenient().doAnswer(invocation -> {
            savedRefreshTokens.remove(invocation.getArgument(0));
            return null;
        }).when(refreshTokenStore).delete(anyLong());

        // LoginFailCounter 상태형 Mock — PIN 실패 횟수를 인메모리 맵으로 흉내낸다
        // (기존 FakeLoginFailCounter 를 Mockito Answer 로 대체)
        lenient().when(loginFailCounter.getCount(anyLong()))
                .thenAnswer(invocation -> failCounts.getOrDefault(invocation.getArgument(0), 0));
        lenient().doAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            failCounts.put(userId, failCounts.getOrDefault(userId, 0) + 1);
            return null;
        }).when(loginFailCounter).increment(anyLong());
        lenient().doAnswer(invocation -> {
            failCounts.remove(invocation.getArgument(0));
            return null;
        }).when(loginFailCounter).reset(anyLong());

        // PasswordResetTokenStore 상태형 Mock — 비밀번호 재설정 토큰→userId 매핑을 인메모리 맵으로 흉내낸다
        // (Redis password:reset:{token} 구현을 Mockito Answer 로 대체 — TTL 은 저장소 내부 관리이므로
        //  save() 는 token/userId 만 받고, TTL 검증은 저장소 구현 책임으로 서비스 테스트에서 제외)
        lenient().doAnswer(invocation -> {
            savedPasswordResetTokens.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(passwordResetTokenStore).save(anyString(), anyLong());
        lenient().when(passwordResetTokenStore.find(anyString()))
                .thenAnswer(invocation -> savedPasswordResetTokens.get(invocation.getArgument(0)));
        lenient().doAnswer(invocation -> {
            savedPasswordResetTokens.remove(invocation.getArgument(0));
            return null;
        }).when(passwordResetTokenStore).delete(anyString());

        // users PK 자동 증가 흉내 — insertUser 호출 시 id 를 채운다 (기존 Fake Mapper 대체)
        lenient().when(authMapper.insertUser(any(UserVO.class)))
                .thenAnswer(invocation -> {
                    invocation.<UserVO>getArgument(0).setId(1L);
                    return 1;
                });
    }

    /** Mock IdentityVerificationProvider 결과 — 실제 Mock 구현체와 동일한 규칙으로 CI/name/phone 을 만든다 */
    private IdentityVerificationResult mockProviderResult(String identityVerificationId) {
        return IdentityVerificationResult.builder()
                .ci("MOCK-CI-" + identityVerificationId)
                .name("홍길동")
                .phoneNumber(mockPhoneNumber(identityVerificationId))
                .build();
    }

    /** Mock Provider 의 전화번호 유도 규칙 — 입력 id 의 숫자 뒤 8자리로 010XXXXXXXX 형식을 만든다 */
    private static String mockPhoneNumber(String identityVerificationId) {
        StringBuilder digits = new StringBuilder();
        for (char c : identityVerificationId.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        while (digits.length() < 8) {
            digits.insert(0, '0');
        }
        return "010" + digits.substring(digits.length() - 8);
    }

    // ---------- 약관 목록 조회 ----------

    @Test
    @DisplayName("정상 약관 목록 조회 - 전체 약관을 DTO로 변환해 반환")
    void getTermsList_success() {
        // Given — Mapper 가 필수 2건 + 선택 1건을 반환한다
        when(authMapper.selectTermsList()).thenReturn(defaultTerms());

        // When
        TermsListResponseDTO result = authService.getTermsList();

        // Then
        assertNotNull(result);
        assertNotNull(result.getTermsList());
        assertEquals(3, result.getTermsList().size());
    }

    @Test
    @DisplayName("Mapper 조회 결과가 Service 반환값에 그대로 반영되는지 검증")
    void getTermsList_mapsMapperResult() {
        // Given
        when(authMapper.selectTermsList()).thenReturn(defaultTerms());

        // When
        TermsListResponseDTO result = authService.getTermsList();

        // Then
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
        // Given — Mapper 가 빈 목록을 반환한다
        when(authMapper.selectTermsList()).thenReturn(Collections.emptyList());

        // When
        TermsListResponseDTO result = authService.getTermsList();

        // Then
        assertNotNull(result);
        assertNotNull(result.getTermsList());
        assertTrue(result.getTermsList().isEmpty());
    }

    // ---------- 본인인증 검증 ----------

    @Test
    @DisplayName("본인인증 성공 - 회원가입 전용 JWT(identityToken)와 name 반환 + 임시 데이터 Redis 저장")
    void verifyIdentity_success() {
        // Given — Provider 는 인증 성공 결과를 반환하고, CI 중복은 없다
        when(identityVerificationProvider.verify("imp_ver_1234567890"))
                .thenReturn(mockProviderResult("imp_ver_1234567890"));
        when(authMapper.countByCiHash(CI_HASH_1234567890)).thenReturn(0);

        // When
        IdentityVerificationResponseDTO result = authService.verifyIdentity("imp_ver_1234567890");

        // Then
        assertNotNull(result);
        assertEquals("홍길동", result.getName());

        // identityToken 은 JWT 형식 (헤더.페이로드.서명 3부분)
        String token = result.getIdentityToken();
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        // Payload 검증 — 회원가입용 claims 만 포함 (개인정보 없음)
        Claims claims = signupTokenProvider.verifySignupToken(token);
        assertEquals(SignupTokenProvider.SUBJECT_SIGNUP_VERIFICATION, claims.getSubject());
        assertFalse(claims.containsKey("name"));
        assertFalse(claims.containsKey("phoneNumber"));
        assertFalse(claims.containsKey("ci"));
        assertFalse(claims.containsKey("encryptedCi"));

        // Redis(상태형 Mock 저장소)에 저장된 임시 데이터 검증
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
        // Given — Provider 는 유효하지 않은 인증 ID 를 거부한다
        when(identityVerificationProvider.verify(MockIdentityVerificationProvider.INVALID_IDENTIFIER))
                .thenThrow(new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID));

        // When & Then
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
        // Given — 동일 CI(SHA-256 해시) 로 이미 가입된 회원이 있다
        // Mock Provider 는 CI = "MOCK-CI-" + identityVerificationId 를 반환한다.
        when(identityVerificationProvider.verify("imp_ver_9999999999"))
                .thenReturn(mockProviderResult("imp_ver_9999999999"));
        when(authMapper.countByCiHash(CI_HASH_9999999999)).thenReturn(1);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.verifyIdentity("imp_ver_9999999999"));

        // Then
        assertEquals(AuthErrorCode.DUPLICATE_USER, ex.getErrorCode());

        // 중복 감지 시 임시 데이터가 저장되지 않아야 한다
        assertTrue(savedSignupData.isEmpty());
    }

    @Test
    @DisplayName("중복 미해당 - CI가 없으면 정상 진행, JWT와 임시 데이터 저장 확인")
    void verifyIdentity_noDuplicate_success() {
        // Given — CI 중복이 없다
        when(identityVerificationProvider.verify("imp_ver_5555555555"))
                .thenReturn(mockProviderResult("imp_ver_5555555555"));
        when(authMapper.countByCiHash(CI_HASH_5555555555)).thenReturn(0);

        // When
        IdentityVerificationResponseDTO result = authService.verifyIdentity("imp_ver_5555555555");

        // Then
        assertNotNull(result);
        assertEquals(3, result.getIdentityToken().split("\\.").length);

        Claims claims = signupTokenProvider.verifySignupToken(result.getIdentityToken());

        SignupVerificationData saved =
                signupVerificationStore.find(claims.get("temporaryUserKey", String.class));
        assertNotNull(saved);
        assertEquals(CI_HASH_5555555555, saved.getCiHash());
    }

    // ---------- 아이디 찾기 ----------

    @Test
    @DisplayName("아이디 찾기 성공 - CI 기준 가입 회원의 마스킹 이메일 + 가입일(yyyy-MM-dd) 반환")
    void findId_success() {
        // Given — PASS 인증 성공 + CI 로 가입된 회원 존재 (email_encrypt/created_at 보유)
        when(identityVerificationProvider.verify("imp_ver_1234567890"))
                .thenReturn(mockProviderResult("imp_ver_1234567890"));
        UserVO user = new UserVO();
        user.setId(501L);
        user.setStatus("ACTIVE");
        user.setEmailEncrypt(PersonalDataCipher.encrypt("user1234@example.com"));
        user.setCreatedAt(LocalDateTime.of(2026, 7, 24, 10, 30, 0));
        when(authMapper.selectUserByCiHash(CI_HASH_1234567890)).thenReturn(user);

        // When
        FindIdResponseDTO result = authService.findId("imp_ver_1234567890");

        // Then — 마스킹 이메일 + yyyy-MM-dd 가입일 (docs 응답 스펙)
        assertNotNull(result);
        assertEquals("user****@example.com", result.getEmail());
        assertEquals("2026-07-24", result.getCreatedAt());

        // CI hash 로 조회했는지 검증 (원문 CI 로 조회 금지)
        verify(authMapper).selectUserByCiHash(CI_HASH_1234567890);
    }

    @Test
    @DisplayName("아이디 찾기 - 인증 ID 누락/빈 값 → INVALID_VERIFICATION_ID")
    void findId_blankId_throws() {
        // When & Then — Provider 호출 없이 Service Layer 에서 즉시 거부
        assertThrows(BusinessException.class, () -> authService.findId(null));
        assertThrows(BusinessException.class, () -> authService.findId(""));
        assertThrows(BusinessException.class, () -> authService.findId("   "));
        verify(identityVerificationProvider, never()).verify(any());
    }

    @Test
    @DisplayName("아이디 찾기 - PASS 인증 실패 → INVALID_VERIFICATION_ID")
    void findId_invalidVerification_throws() {
        // Given — Provider 가 인증 실패를 던진다
        when(identityVerificationProvider.verify(MockIdentityVerificationProvider.INVALID_IDENTIFIER))
                .thenThrow(new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.findId(MockIdentityVerificationProvider.INVALID_IDENTIFIER));

        // Then
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, ex.getErrorCode());
    }

    @Test
    @DisplayName("아이디 찾기 - CI 로 가입된 회원 없음 → USER_NOT_FOUND (404)")
    void findId_userNotFound_throws() {
        // Given — PASS 인증은 성공하지만 해당 CI 로 가입된 회원이 없다
        when(identityVerificationProvider.verify("imp_ver_9999999999"))
                .thenReturn(mockProviderResult("imp_ver_9999999999"));
        when(authMapper.selectUserByCiHash(CI_HASH_9999999999)).thenReturn(null);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.findId("imp_ver_9999999999"));

        // Then
        assertEquals(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("아이디 찾기 - 탈퇴(WITHDRAWN) 회원 → USER_NOT_FOUND (계정 존재 여부 노출 방지)")
    void findId_withdrawnUser_throws() {
        // Given — PASS 인증은 성공하지만 해당 CI 의 회원은 탈퇴 상태
        when(identityVerificationProvider.verify("imp_ver_1234567890"))
                .thenReturn(mockProviderResult("imp_ver_1234567890"));
        UserVO withdrawn = new UserVO();
        withdrawn.setId(501L);
        withdrawn.setStatus("WITHDRAWN");
        withdrawn.setEmailEncrypt(PersonalDataCipher.encrypt("user1234@example.com"));
        withdrawn.setCreatedAt(LocalDateTime.of(2026, 7, 24, 10, 30, 0));
        when(authMapper.selectUserByCiHash(CI_HASH_1234567890)).thenReturn(withdrawn);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.findId("imp_ver_1234567890"));

        // Then — 탈퇴 회원도 USER_NOT_FOUND 로 통일 (이메일 미노출)
        assertEquals(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // ---------- 회원가입 이메일 중복 확인 ----------

    @Test
    @DisplayName("이메일 중복 확인 - 존재하지 않는 email_hash → available=true")
    void checkEmailAvailability_notExists_available() {
        // Given — users.email_hash 에 매칭이 없다
        when(authMapper.countByEmailHash(EMAIL_HASH_NEW)).thenReturn(0);

        // When
        EmailAvailabilityResponseDTO result = authService.checkEmailAvailability("new@example.com");

        // Then
        assertNotNull(result);
        assertTrue(result.isAvailable());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 존재하는 email_hash → available=false")
    void checkEmailAvailability_exists_duplicate() {
        // Given — users.email_hash 에 매칭이 있다
        when(authMapper.countByEmailHash(EMAIL_HASH_USED)).thenReturn(1);

        // When
        EmailAvailabilityResponseDTO result = authService.checkEmailAvailability("used@example.com");

        // Then
        assertNotNull(result);
        assertFalse(result.isAvailable());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 대소문자 무관 (소문자 정규화 후 hash) → 동일 결과")
    void checkEmailAvailability_caseInsensitive() {
        // Given — SHA-256("used@example.com") 이 이미 가입된 상태에서
        when(authMapper.countByEmailHash(EMAIL_HASH_USED)).thenReturn(1);

        // When — 대문자 입력("USED@EXAMPLE.COM")도 소문자 정규화 후 hash 되므로 중복으로 판단해야 한다
        EmailAvailabilityResponseDTO upper = authService.checkEmailAvailability("USED@EXAMPLE.COM");
        EmailAvailabilityResponseDTO spaced = authService.checkEmailAvailability("  used@example.com  ");

        // Then
        assertFalse(upper.isAvailable());
        assertFalse(spaced.isAvailable());
    }

    @Test
    @DisplayName("이메일 중복 확인 - null/blank → INVALID_EMAIL_FORMAT 예외")
    void checkEmailAvailability_blank_throws() {
        // When & Then — null/blank 는 hash 조회 전에 차단된다
        assertThrows(BusinessException.class, () -> authService.checkEmailAvailability(null));
        assertThrows(BusinessException.class, () -> authService.checkEmailAvailability(""));
        assertThrows(BusinessException.class, () -> authService.checkEmailAvailability("   "));
    }

    @Test
    @DisplayName("이메일 중복 확인 - 잘못된 형식 → INVALID_EMAIL_FORMAT 예외")
    void checkEmailAvailability_invalidFormat_throws() {
        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.checkEmailAvailability("not-an-email"));

        // Then
        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 최대 길이(254자) 초과 → INVALID_EMAIL_FORMAT + DB 조회 미발생")
    void checkEmailAvailability_tooLong_throws() {
        // When — 255자 이상 이메일은 검증 단계에서 차단된다
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.checkEmailAvailability(buildLongEmail(255)));

        // Then
        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());
        // 길이 검증 실패 시 DB(hash) 조회가 발생하지 않아야 한다
        verify(authMapper, never()).countByEmailHash(anyString());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 최대 길이(254자) 경계 → 정상 처리 + hash 조회 1회")
    void checkEmailAvailability_maxLengthBoundary_success() {
        // Given — 254자 경계는 정상 처리된다
        when(authMapper.countByEmailHash(anyString())).thenReturn(0);

        // When
        EmailAvailabilityResponseDTO result = authService.checkEmailAvailability(buildLongEmail(254));

        // Then
        assertNotNull(result);
        assertTrue(result.isAvailable());
        // 길이 검증 통과 시 DB(hash) 조회가 1회 발생한다
        verify(authMapper).countByEmailHash(anyString());
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

    /**
     * verifyIdentity 를 거쳐 발급된 유효한 identityToken 을 만든다.
     * - Provider 성공 결과와 CI 중복 없음 상태를 Stub 한 뒤 실제 verifyIdentity 를 호출하므로
     *   토큰의 temporaryUserKey 에 대응하는 임시 데이터가 상태형 저장소에 저장된다
     */
    private String issueValidIdentityToken() {
        when(identityVerificationProvider.verify("imp_ver_1234567890"))
                .thenReturn(mockProviderResult("imp_ver_1234567890"));
        when(authMapper.countByCiHash(CI_HASH_1234567890)).thenReturn(0);
        IdentityVerificationResponseDTO result =
                authService.verifyIdentity("imp_ver_1234567890");
        return result.getIdentityToken();
    }

    /**
     * signup 정상 경로 Stub — 이메일 hash / 닉네임 중복 없음 + 약관 마스터(필수 1, 2) 조회 성공.
     * 동의 약관이 기본 필수(1, 2)와 다른 테스트는 직접 Stub 한다.
     */
    private void stubSignupSuccessPath(String emailHash) {
        when(authMapper.countByEmailHash(emailHash)).thenReturn(0);
        when(authMapper.countByNickname("tester")).thenReturn(0);
        when(authMapper.selectExistingTermIds(anyList())).thenReturn(Arrays.asList(1L, 2L));
        when(authMapper.selectRequiredTermsIds()).thenReturn(Arrays.asList(1L, 2L));
    }

    @Test
    @DisplayName("정상 회원가입 - users/user_auth/user_profile insert + 지갑 생성 + Redis 삭제 + 암호화 저장")
    void signup_success() {
        // Given — 유효한 인증 토큰 + 중복 없음 + 약관 마스터 정상
        String token = issueValidIdentityToken();
        stubSignupSuccessPath(EMAIL_HASH_TEST);

        // When
        authService.signup(signupRequest(token, "test@example.com", "tester"));

        // Then — users insert 검증
        ArgumentCaptor<UserVO> userCaptor = ArgumentCaptor.forClass(UserVO.class);
        verify(authMapper).insertUser(userCaptor.capture());
        UserVO user = userCaptor.getValue();
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
        ArgumentCaptor<UserAuthVO> userAuthCaptor = ArgumentCaptor.forClass(UserAuthVO.class);
        verify(authMapper).insertUserAuth(userAuthCaptor.capture());
        UserAuthVO userAuth = userAuthCaptor.getValue();
        assertEquals(user.getId(), userAuth.getUserId());
        // password 는 BCrypt 해시 (원문과 다르고 matches 검증 통과)
        assertNotEquals("password123!", userAuth.getPasswordHash());
        assertTrue(PasswordEncryptor.matches("password123!", userAuth.getPasswordHash()));
        // CI hash / encrypt
        assertEquals(CI_HASH_1234567890, userAuth.getIdentityCiHash());
        assertEquals("MOCK-CI-imp_ver_1234567890",
                PersonalDataCipher.decrypt(userAuth.getIdentityCiEncrypt()));

        // user_profile insert 검증
        ArgumentCaptor<UserProfileVO> profileCaptor = ArgumentCaptor.forClass(UserProfileVO.class);
        verify(authMapper).insertUserProfile(profileCaptor.capture());
        UserProfileVO profile = profileCaptor.getValue();
        assertEquals(user.getId(), profile.getUserId());
        assertEquals("tester", profile.getNickname());

        // user_terms_agreements insert 검증 (약관 동의 저장 — 필수 약관 1, 2)
        verify(authMapper).insertUserTerms(eq(user.getId()), eq(Arrays.asList(1L, 2L)));

        // 전자지갑 생성 검증
        verify(walletService).createWallet(user.getId());

        // 회원가입 완료 후 Redis 임시 데이터 삭제 검증
        assertTrue(savedSignupData.isEmpty());
    }

    @Test
    @DisplayName("CI 중복 실패 - 최종 가입 시점에 동일 CI 가입자가 있으면 DUPLICATE_USER")
    void signup_duplicateCi_throws() {
        // Given — verifyIdentity 단계를 통과한 뒤, 동일 CI 해시가 이미 가입된 상태로 변경
        String token = issueValidIdentityToken();
        // (Mockito 연속 Stub 큐잉: verifyIdentity 중 조회는 0, signup 중 재검증은 1 이 반환된다)
        when(authMapper.countByCiHash(CI_HASH_1234567890)).thenReturn(1);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester")));

        // Then
        assertEquals(AuthErrorCode.DUPLICATE_USER, ex.getErrorCode());

        // 중복 감지 시 어떤 insert 도 발생하지 않아야 한다
        verify(authMapper, never()).insertUser(any(UserVO.class));
        verify(authMapper, never()).insertUserAuth(any(UserAuthVO.class));
        verify(authMapper, never()).insertUserProfile(any(UserProfileVO.class));
        // Redis 데이터는 삭제되지 않고 남아 있어야 재시도 가능
        assertFalse(savedSignupData.isEmpty());
    }

    @Test
    @DisplayName("이메일 중복 실패 - 동일 email_hash 가 있으면 DUPLICATE_EMAIL")
    void signup_duplicateEmail_throws() {
        // Given — 동일 email_hash 가 이미 가입된 상태
        String token = issueValidIdentityToken();
        when(authMapper.countByEmailHash(EMAIL_HASH_TEST)).thenReturn(1);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester")));

        // Then
        assertEquals(AuthErrorCode.DUPLICATE_EMAIL, ex.getErrorCode());
        verify(authMapper, never()).insertUser(any(UserVO.class));
    }

    @Test
    @DisplayName("닉네임 중복 실패 - 동일 nickname 이 있으면 DUPLICATE_NICKNAME")
    void signup_duplicateNickname_throws() {
        // Given — 동일 nickname 이 이미 가입된 상태
        String token = issueValidIdentityToken();
        when(authMapper.countByNickname("tester")).thenReturn(1);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester")));

        // Then
        assertEquals(AuthErrorCode.DUPLICATE_NICKNAME, ex.getErrorCode());
        verify(authMapper, never()).insertUser(any(UserVO.class));
    }

    @Test
    @DisplayName("JWT 만료 - 만료된 identityToken 은 EXPIRED_SIGNUP_TOKEN")
    void signup_expiredToken_throws() {
        // Given — verifyIdentity 는 유효한 임시 데이터를 저장하지만, 토큰은 과거 만료 시각으로 발급한다
        SignupVerificationData data = SignupVerificationData.builder()
                .verificationId("imp_ver_1234567890")
                .ciHash(CI_HASH_1234567890)
                .encryptedCi(PersonalDataCipher.encrypt("MOCK-CI-imp_ver_1234567890"))
                .encryptedName(PersonalDataCipher.encrypt("홍길동"))
                .encryptedPhone(PersonalDataCipher.encrypt(MOCK_PHONE_NUMBER))
                .build();
        signupVerificationStore.save("expired-key", data);

        String expiredToken = signupTokenProvider.issue("expired-key",
                new Date(System.currentTimeMillis() - 60_000L));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(expiredToken, "test@example.com", "tester")));

        // Then
        assertEquals(AuthErrorCode.EXPIRED_SIGNUP_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("JWT 위변조 - 서명이 틀린 identityToken 은 INVALID_SIGNUP_TOKEN")
    void signup_tamperedToken_throws() {
        // Given — 유효한 토큰을 발급받아 위변조한다
        String token = issueValidIdentityToken();
        // 끝에서 두 번째 base64 글자를 바꾼다.
        // 마지막 글자는 256비트 서명의 패딩 비트만 담고 있어 'a'→'b' 교체 시
        // 복호화된 서명이 동일해질 수 있어(플레이크) 반드시 유효 비트를 바꾸는 위치를 사용한다
        String tampered = token.substring(0, token.length() - 2)
                + (token.charAt(token.length() - 2) == 'a' ? "b" : "a")
                + token.charAt(token.length() - 1);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(tampered, "test@example.com", "tester")));

        // Then
        assertEquals(AuthErrorCode.INVALID_SIGNUP_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("용도 오류 - 회원가입 전용 토큰이 아닌 JWT(sub 불일치)는 INVALID_SIGNUP_TOKEN")
    void signup_wrongSubjectToken_throws() {
        // Given — 같은 시크릿으로 서명했지만 sub 만 다른 토큰 (회원가입 토큰 오용 방지 검증)
        String otherToken = Jwts.builder()
                .setSubject("access-token")
                .claim("temporaryUserKey", "temp-key-1")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(otherToken, "test@example.com", "tester")));

        // Then
        assertEquals(AuthErrorCode.INVALID_SIGNUP_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("Redis 데이터 없음 - 토큰은 유효하지만 임시 데이터가 없으면 SIGNUP_VERIFICATION_NOT_FOUND")
    void signup_verificationNotFound_throws() {
        // Given — 임시 데이터 저장 없이 유효한 토큰만 발급한다
        String token = signupTokenProvider.issue("no-data-key");

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester")));

        // Then
        assertEquals(AuthErrorCode.SIGNUP_VERIFICATION_NOT_FOUND, ex.getErrorCode());
        verify(authMapper, never()).insertUser(any(UserVO.class));
    }

    @Test
    @DisplayName("필수 값 누락 - identityToken/email/password/nickname 누락은 INVALID_SIGNUP_REQUEST")
    void signup_missingRequired_throws() {
        // When & Then — identityToken 누락
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
        // Given — 유효한 인증 토큰
        String token = issueValidIdentityToken();

        // When — 잘못된 이메일 형식은 hash/DB 조회 전에 차단된다
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "not-an-email", "tester")));

        // Then
        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());
    }

    @Test
    @DisplayName("약관 동의 - 모든 필수 약관 동의 시 가입 성공 + 약관 동의 저장 (필수 + 선택)")
    void signup_allRequiredTermsAgreed_success() {
        // Given — 유효한 인증 토큰 + 필수(1, 2) + 선택(3) 전부 동의 상태
        String token = issueValidIdentityToken();
        when(authMapper.countByEmailHash(EMAIL_HASH_TEST)).thenReturn(0);
        when(authMapper.countByNickname("tester")).thenReturn(0);
        when(authMapper.selectExistingTermIds(anyList())).thenReturn(Arrays.asList(1L, 2L, 3L));
        when(authMapper.selectRequiredTermsIds()).thenReturn(Arrays.asList(1L, 2L));

        // When
        authService.signup(signupRequest(token, "test@example.com", "tester",
                Arrays.asList(1L, 2L, 3L)));

        // Then — 동의한 약관이 그대로 저장된다
        ArgumentCaptor<UserVO> userCaptor = ArgumentCaptor.forClass(UserVO.class);
        verify(authMapper).insertUser(userCaptor.capture());
        verify(authMapper).insertUserTerms(eq(userCaptor.getValue().getId()),
                eq(Arrays.asList(1L, 2L, 3L)));
    }

    @Test
    @DisplayName("약관 동의 - 필수 약관 일부 누락 시 MISSING_REQUIRED_TERMS")
    void signup_missingRequiredTerms_throws() {
        // Given — 유효한 인증 토큰 + 필수 약관 2 번을 누락하고 1 번만 동의
        String token = issueValidIdentityToken();
        when(authMapper.countByEmailHash(EMAIL_HASH_TEST)).thenReturn(0);
        when(authMapper.countByNickname("tester")).thenReturn(0);
        when(authMapper.selectExistingTermIds(anyList())).thenReturn(Collections.singletonList(1L));
        when(authMapper.selectRequiredTermsIds()).thenReturn(Arrays.asList(1L, 2L));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester",
                        Collections.singletonList(1L))));

        // Then
        assertEquals(AuthErrorCode.MISSING_REQUIRED_TERMS, ex.getErrorCode());

        // 실패 시 어떤 insert 도 발생하지 않아야 한다
        verify(authMapper, never()).insertUser(any(UserVO.class));
        verify(authMapper, never()).insertUserAuth(any(UserAuthVO.class));
        verify(authMapper, never()).insertUserProfile(any(UserProfileVO.class));
        verify(authMapper, never()).insertUserTerms(anyLong(), anyList());
    }

    @Test
    @DisplayName("약관 동의 - 존재하지 않는 약관 ID 포함 시 INVALID_TERM_ID")
    void signup_invalidTermId_throws() {
        // Given — 유효한 인증 토큰 + 필수(1, 2)는 동의했지만 terms 에 존재하지 않는 99 번이 섞여 있음
        String token = issueValidIdentityToken();
        when(authMapper.countByEmailHash(EMAIL_HASH_TEST)).thenReturn(0);
        when(authMapper.countByNickname("tester")).thenReturn(0);
        // 존재하는 약관은 1, 2 만 조회되므로 입력([1, 2, 99])과 크기가 달라진다
        when(authMapper.selectExistingTermIds(anyList())).thenReturn(Arrays.asList(1L, 2L));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester",
                        Arrays.asList(1L, 2L, 99L))));

        // Then
        assertEquals(AuthErrorCode.INVALID_TERM_ID, ex.getErrorCode());

        // 실패 시 어떤 insert 도 발생하지 않아야 한다 (FK 위반 500 대신 400)
        verify(authMapper, never()).insertUser(any(UserVO.class));
        verify(authMapper, never()).insertUserTerms(anyLong(), anyList());
    }

    @Test
    @DisplayName("약관 동의 - null 요소 포함 시에도 INVALID_TERM_ID")
    void signup_nullTermId_throws() {
        // Given — 유효한 인증 토큰 + [1, 2, null] — null 요소는 terms 에 존재할 수 없다
        String token = issueValidIdentityToken();
        when(authMapper.countByEmailHash(EMAIL_HASH_TEST)).thenReturn(0);
        when(authMapper.countByNickname("tester")).thenReturn(0);
        when(authMapper.selectExistingTermIds(anyList())).thenReturn(Arrays.asList(1L, 2L));

        List<Long> agreedWithNull = new ArrayList<>(Arrays.asList(1L, 2L, null));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester",
                        agreedWithNull)));

        // Then
        assertEquals(AuthErrorCode.INVALID_TERM_ID, ex.getErrorCode());
        verify(authMapper, never()).insertUser(any(UserVO.class));
    }

    @Test
    @DisplayName("약관 동의 - 선택 약관 미동의 시에도 가입 성공")
    void signup_optionalTermsNotAgreed_success() {
        // Given — 유효한 인증 토큰 + 필수(1, 2)만 동의하고 선택(3)은 미동의
        String token = issueValidIdentityToken();
        stubSignupSuccessPath(EMAIL_HASH_TEST);

        // When
        authService.signup(signupRequest(token, "test@example.com", "tester",
                Arrays.asList(1L, 2L)));

        // Then
        ArgumentCaptor<UserVO> userCaptor = ArgumentCaptor.forClass(UserVO.class);
        verify(authMapper).insertUser(userCaptor.capture());
        verify(authMapper).insertUserTerms(eq(userCaptor.getValue().getId()),
                eq(Arrays.asList(1L, 2L)));
    }

    @Test
    @DisplayName("약관 동의 - agreedTermsIds 누락/빈 배열 시 MISSING_REQUIRED_TERMS")
    void signup_agreedTermsMissing_throws() {
        // Given — 유효한 인증 토큰
        String token = issueValidIdentityToken();

        // When & Then — null
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester", null)));
        assertEquals(AuthErrorCode.MISSING_REQUIRED_TERMS, nullEx.getErrorCode());

        // 빈 배열
        BusinessException emptyEx = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, "test@example.com", "tester",
                        Collections.emptyList())));
        assertEquals(AuthErrorCode.MISSING_REQUIRED_TERMS, emptyEx.getErrorCode());

        verify(authMapper, never()).insertUser(any(UserVO.class));
        verify(authMapper, never()).insertUserTerms(anyLong(), anyList());
    }

    @Test
    @DisplayName("약관 동의 - 중복 ID 전달 시 중복 제거 후 저장")
    void signup_duplicateTermIds_deduplicated() {
        // Given — 유효한 인증 토큰 + 중복 ID([1, 1, 2, 2]) 전달
        String token = issueValidIdentityToken();
        stubSignupSuccessPath(EMAIL_HASH_TEST);

        // When
        authService.signup(signupRequest(token, "test@example.com", "tester",
                Arrays.asList(1L, 1L, 2L, 2L)));

        // Then — 중복이 제거된 [1, 2] 만 저장된다
        ArgumentCaptor<UserVO> userCaptor = ArgumentCaptor.forClass(UserVO.class);
        verify(authMapper).insertUser(userCaptor.capture());
        verify(authMapper).insertUserTerms(eq(userCaptor.getValue().getId()),
                eq(Arrays.asList(1L, 2L)));
    }

    @Test
    @DisplayName("이메일 소문자 정규화 - 대문자 이메일도 소문자 hash 로 저장된다")
    void signup_emailLowercased() {
        // Given — 유효한 인증 토큰 + 중복 없음
        String token = issueValidIdentityToken();
        stubSignupSuccessPath(EMAIL_HASH_TEST);

        // When — 대문자 이메일로 가입
        authService.signup(signupRequest(token, "TEST@EXAMPLE.COM", "tester"));

        // Then — SHA-256("test@example.com") 과 동일해야 한다 (소문자 정규화)
        ArgumentCaptor<UserVO> userCaptor = ArgumentCaptor.forClass(UserVO.class);
        verify(authMapper).insertUser(userCaptor.capture());
        assertEquals(EMAIL_HASH_TEST, userCaptor.getValue().getEmailHash());
        assertEquals("test@example.com",
                PersonalDataCipher.decrypt(userCaptor.getValue().getEmailEncrypt()));
    }

    @Test
    @DisplayName("회원가입 완료 - 최대 길이(254자) 이메일 정상 가입 (trim/lowercase 후 암호화·hash 저장)")
    void signup_emailMaxLength_success() {
        // Given — 유효한 인증 토큰 + 최대 길이 이메일
        String token = issueValidIdentityToken();
        String maxLengthEmail = buildLongEmail(254);
        when(authMapper.countByEmailHash(anyString())).thenReturn(0);
        when(authMapper.countByNickname("tester")).thenReturn(0);
        when(authMapper.selectExistingTermIds(anyList())).thenReturn(Arrays.asList(1L, 2L));
        when(authMapper.selectRequiredTermsIds()).thenReturn(Arrays.asList(1L, 2L));

        // When
        authService.signup(signupRequest(token, maxLengthEmail, "tester"));

        // Then
        ArgumentCaptor<UserVO> userCaptor = ArgumentCaptor.forClass(UserVO.class);
        verify(authMapper).insertUser(userCaptor.capture());
        UserVO user = userCaptor.getValue();
        // 원문은 AES 암호화 저장 — 복호화 시 입력값과 동일
        assertEquals(maxLengthEmail, PersonalDataCipher.decrypt(user.getEmailEncrypt()));
        // email_hash 는 SHA-256 hex (64자)
        assertEquals(64, user.getEmailHash().length());
        // 전자지갑 생성까지 정상
        verify(walletService).createWallet(user.getId());
    }

    @Test
    @DisplayName("회원가입 완료 - 255자 이상 이메일 → INVALID_EMAIL_FORMAT + hash 조회/insert 미발생")
    void signup_emailTooLong_throws() {
        // Given — 유효한 인증 토큰
        String token = issueValidIdentityToken();

        // When — 255자 이상 이메일은 검증 단계에서 차단된다
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.signup(signupRequest(token, buildLongEmail(255), "tester")));

        // Then
        assertEquals(AuthErrorCode.INVALID_EMAIL_FORMAT, ex.getErrorCode());
        // 길이 검증은 hash 생성 전에 수행되므로 DB(email_hash) 조회가 없어야 한다
        verify(authMapper, never()).countByEmailHash(anyString());
        // 어떤 insert 도 발생하지 않아야 한다 (DB insert 이전 차단)
        verify(authMapper, never()).insertUser(any(UserVO.class));
        verify(authMapper, never()).insertUserAuth(any(UserAuthVO.class));
        verify(authMapper, never()).insertUserProfile(any(UserProfileVO.class));
        verify(authMapper, never()).insertUserTerms(anyLong(), anyList());
    }

    // ---------- 통합 로그인 ----------

    /**
     * 로그인 조회용 회원 등록 — Mock Mapper 의 조회 경로(email_hash/phone_hash/deviceId/userId)별로
     * 동일 회원을 Stub 한다. 사용되지 않는 조회 경로의 Stub 은 lenient 로 무시된다.
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
        lenient().when(authMapper.findUserByEmailHash(sha256(email))).thenReturn(user);
        lenient().when(authMapper.findUserByPhoneHash(sha256(phoneNumber))).thenReturn(user);
        lenient().when(authMapper.findUserByDeviceId(deviceId)).thenReturn(user);
        lenient().when(authMapper.findUserById(userId)).thenReturn(user);
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
        // Given — email_hash 로 조회되는 ACTIVE 회원
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        // When
        LoginResponseDTO result =
                authService.login(loginRequest("PASSWORD", "user@example.com", "password123!", null, null));

        // Then
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
        assertEquals(sha256(refreshToken), savedRefreshTokens.get(501L));
        assertNotEquals(refreshToken, savedRefreshTokens.get(501L));

        // TTL 은 refresh 만료와 동일 (1209600초)
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(refreshTokenStore).save(eq(501L), anyString(), ttlCaptor.capture());
        assertEquals(Long.valueOf(1209600L), ttlCaptor.getValue());

        // Refresh Token 검증 — sub == userId, tokenType == REFRESH
        Claims refreshClaims = jwtTokenProvider.parseRefreshToken(refreshToken);
        assertEquals("501", refreshClaims.getSubject());
    }

    @Test
    @DisplayName("PASSWORD 이메일 로그인 - trim + lowercase 정규화 후 email_hash 로만 조회")
    void login_passwordEmail_normalizesAndUsesEmailHash() {
        // Given — email_hash 로 조회되는 ACTIVE 회원
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        // When — 대소문자/공백이 섞인 이메일로 로그인
        authService.login(loginRequest("PASSWORD", "  USER@EXAMPLE.COM  ", "password123!", null, null));

        // Then — email_encrypt(원문) 조회가 아니라 email_hash(SHA-256) 로만 조회했는지 확인
        verify(authMapper).findUserByEmailHash(sha256("user@example.com"));
        verify(authMapper, never()).findUserByPhoneHash(anyString());
    }

    @Test
    @DisplayName("PASSWORD 휴대폰 로그인 성공 - 하이픈 제거 후 phone_hash 로만 조회")
    void login_passwordPhone_success() {
        // Given — phone_hash 로 조회되는 ACTIVE 회원
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        // When — 하이픈이 포함된 휴대폰 번호로 로그인
        LoginResponseDTO result =
                authService.login(loginRequest("PASSWORD", "010-3456-7890", "password123!", null, null));

        // Then
        assertNotNull(result);
        assertEquals(501L, result.getUserId().longValue());
        assertEquals("홍길동", result.getName());

        // phone_number_encrypt(원문) 조회가 아니라 phone_hash(SHA-256) 로만 조회했는지 확인
        verify(authMapper).findUserByPhoneHash(sha256("01034567890"));
        verify(authMapper, never()).findUserByEmailHash(anyString());
    }

    @Test
    @DisplayName("PASSWORD 실패 - 잘못된 password → INVALID_CREDENTIALS + 토큰/Redis 저장 없음")
    void login_passwordWrongPassword_throws() {
        // Given — 등록된 회원 (password: password123!)
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        // When — 잘못된 비밀번호
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PASSWORD", "user@example.com", "wrong-password!", null, null)));

        // Then
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(savedRefreshTokens.isEmpty());
    }

    @Test
    @DisplayName("PASSWORD 실패 - 존재하지 않는 이메일 → INVALID_CREDENTIALS (원인 비노출)")
    void login_passwordUnknownUser_throws() {
        // When — 미등록 이메일 (Mock 조회 결과 null)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PASSWORD", "unknown@example.com", "password123!", null, null)));

        // Then
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(savedRefreshTokens.isEmpty());
    }

    @Test
    @DisplayName("PIN 로그인 성공 - deviceId 조회 + pin 검증 + 실패 횟수 초기화 + 토큰 발급")
    void login_pin_success() {
        // Given — device_id 로 조회되는 ACTIVE 회원 + 이전 실패 이력 2회
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        failCounts.put(501L, 2);

        // When
        LoginResponseDTO result =
                authService.login(loginRequest("PIN", null, null, "123456", "device-uuid-1"));

        // Then
        assertNotNull(result);
        assertEquals(501L, result.getUserId().longValue());
        // 성공 시 실패 횟수 초기화
        assertEquals(0, loginFailCounter.getCount(501L));
        assertEquals(sha256(result.getRefreshToken()), savedRefreshTokens.get(501L));
    }

    @Test
    @DisplayName("PIN 실패 - 잘못된 pin → INVALID_CREDENTIALS + 실패 횟수 1 증가")
    void login_pinWrongPin_throws() {
        // Given — device_id 로 조회되는 ACTIVE 회원 (pin: 123456)
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");

        // When — 잘못된 PIN
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PIN", null, null, "000000", "device-uuid-1")));

        // Then
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertEquals(1, loginFailCounter.getCount(501L));
        assertTrue(savedRefreshTokens.isEmpty());
    }

    @Test
    @DisplayName("PIN 실패 - 미등록 deviceId → INVALID_CREDENTIALS")
    void login_pinUnknownDevice_throws() {
        // When — 미등록 deviceId (Mock 조회 결과 null)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PIN", null, null, "123456", "unknown-device")));

        // Then
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(savedRefreshTokens.isEmpty());
    }

    @Test
    @DisplayName("PIN 잠금 - 실패 횟수 5회 이상 → PIN_LOCK_EXCEEDED + 토큰 미발급")
    void login_pinLockExceeded_throws() {
        // Given — device_id 로 조회되는 ACTIVE 회원 + 실패 횟수 5회 (잠금 상태)
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        failCounts.put(501L, 5);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PIN", null, null, "123456", "device-uuid-1")));

        // Then
        assertEquals(AuthErrorCode.PIN_LOCK_EXCEEDED, ex.getErrorCode());
        // 잠금 상태에서는 실패 횟수가 더 증가하지 않는다
        assertEquals(5, loginFailCounter.getCount(501L));
        assertTrue(savedRefreshTokens.isEmpty());
    }

    @Test
    @DisplayName("잘못된 loginType - PASSWORD/PIN 외 값 → INVALID_LOGIN_TYPE")
    void login_invalidLoginType_throws() {
        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("FACE_ID", "user@example.com", "password123!", null, null)));

        // Then
        assertEquals(AuthErrorCode.INVALID_LOGIN_TYPE, ex.getErrorCode());
    }

    @Test
    @DisplayName("필수 값 누락 - loginType/loginId/password/pinNumber/deviceId → INVALID_LOGIN_REQUEST")
    void login_missingRequired_throws() {
        // When & Then — loginType 누락
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
        // Given — 탈퇴 상태 회원 (비밀번호는 일치)
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "WITHDRAWN");

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("PASSWORD", "user@example.com", "password123!", null, null)));

        // Then
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(savedRefreshTokens.isEmpty());
    }

    @Test
    @DisplayName("보안 - 요청 DTO toString 에 password/pinNumber 원문 미노출")
    void login_requestToStringHidesSecrets() {
        // Given
        LoginRequestDTO request =
                loginRequest("PASSWORD", "user@example.com", "password123!", "123456", "device-uuid-1");

        // When
        String text = request.toString();

        // Then
        assertFalse(text.contains("password123!"));
        assertFalse(text.contains("123456"));
    }

    // ---------- 로그인 토큰 재발급 (Refresh Token → Access Token) ----------

    /**
     * 로그인까지 거쳐 유효한 Refresh Token 세션을 만든다.
     * - Service 가 발급한 refreshToken 이 상태형 저장소(savedRefreshTokens)에 hash 로 저장된 상태
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
        // Given — 명시적 만료(120초) 토큰으로 세션 구성
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        // Service 가 발급하는 기본 만료(14일) 토큰과 exp 가 항상 달라
        // JWT 문자열이 동일해지는 타이밍 문제 없이 Rotation 을 결정적으로 검증한다
        String refreshToken = jwtTokenProvider.createRefreshToken(501L,
                new Date(System.currentTimeMillis() + 120_000L));
        savedRefreshTokens.put(501L, sha256(refreshToken));
        String oldHash = savedRefreshTokens.get(501L);

        // When
        RefreshTokenResponseDTO result = authService.refreshAccessToken(refreshToken);

        // Then — token_info (신규 Access Token, sub == userId, tokenType == ACCESS, 개인정보 없음)
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
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(refreshTokenStore).save(eq(501L), hashCaptor.capture(), ttlCaptor.capture());
        assertEquals(sha256(newRefreshToken), hashCaptor.getValue());
        assertNotEquals(oldHash, hashCaptor.getValue());
        assertEquals(Long.valueOf(1209600L), ttlCaptor.getValue());
    }

    @Test
    @DisplayName("Rotation 후 이전 Refresh Token 재사용 - 세션 revoke + INVALID_REFRESH_TOKEN")
    void refresh_reuseAfterRotation_revokesSession() {
        // Given — 명시적 만료(120초) 토큰으로 1차 세션 구성
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        // Service 가 발급하는 기본 만료(14일) 토큰과 exp 가 항상 달라,
        // 회전 후 이전 토큰 재사용 감지가 결정적으로 검증된다
        String refreshToken = jwtTokenProvider.createRefreshToken(501L,
                new Date(System.currentTimeMillis() + 120_000L));
        savedRefreshTokens.put(501L, sha256(refreshToken));

        // When — 1차 재발급(Rotation) — 이전 refreshToken 은 더 이상 유효하지 않다
        RefreshTokenResponseDTO rotated = authService.refreshAccessToken(refreshToken);
        assertNotEquals(refreshToken, rotated.getRefreshToken());

        // 2차 요청에 회전 전 Refresh Token 사용 → 재사용 감지 → 세션 전체 revoke
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
        // 세션 revoke — Redis 저장 hash 삭제
        assertFalse(savedRefreshTokens.containsKey(501L));
        verify(refreshTokenStore).delete(501L);
    }

    @Test
    @DisplayName("Redis hash 불일치 - 세션 revoke + INVALID_REFRESH_TOKEN")
    void refresh_hashMismatch_revokesSession() {
        // Given — 저장된 hash 를 다른 값으로 덮어쓴다 (클라이언트 토큰 != 저장소 토큰 = 탈취/재사용 의심)
        String refreshToken = issueRefreshTokenSession(501L);
        savedRefreshTokens.put(501L, "forged-hash-value");

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
        // 세션 revoke — 저장 hash 삭제
        assertFalse(savedRefreshTokens.containsKey(501L));
        verify(refreshTokenStore).delete(501L);
    }

    @Test
    @DisplayName("Redis 저장 hash 없음(로그아웃/TTL 만료) - INVALID_REFRESH_TOKEN")
    void refresh_noStoredHash_throws() {
        // Given — 로그아웃 등으로 Redis 세션이 삭제된 상태
        String refreshToken = issueRefreshTokenSession(501L);
        savedRefreshTokens.remove(501L);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("만료된 Refresh Token - INVALID_REFRESH_TOKEN")
    void refresh_expiredToken_throws() {
        // Given — 만료된 Refresh Token
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        String expired = jwtTokenProvider.createRefreshToken(501L,
                new Date(System.currentTimeMillis() - 60_000L));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(expired));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("위변조된 Refresh Token - INVALID_REFRESH_TOKEN")
    void refresh_tamperedToken_throws() {
        // Given — 유효한 세션의 토큰을 위변조한다
        String refreshToken = issueRefreshTokenSession(501L);
        // 끝에서 두 번째 base64 글자를 바꾼다 (signup 위변조 테스트와 동일한 방식)
        String tampered = refreshToken.substring(0, refreshToken.length() - 2)
                + (refreshToken.charAt(refreshToken.length() - 2) == 'a' ? "b" : "a")
                + refreshToken.charAt(refreshToken.length() - 1);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(tampered));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("Access Token 으로 재발급 요청 - 용도 오류 → INVALID_REFRESH_TOKEN")
    void refresh_accessTokenMisuse_throws() {
        // Given — Access Token (용도 오류 검증)
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        String accessToken = jwtTokenProvider.createAccessToken(501L);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(accessToken));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("탈퇴 회원 - ACTIVE 가 아니면 INVALID_REFRESH_TOKEN")
    void refresh_withdrawnUser_throws() {
        // Given — 탈퇴 회원의 세션이 Redis 에 남아있는 상태
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "WITHDRAWN");
        String refreshToken = jwtTokenProvider.createRefreshToken(501L);
        savedRefreshTokens.put(501L, sha256(refreshToken));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("존재하지 않는 회원 - INVALID_REFRESH_TOKEN")
    void refresh_unknownUser_throws() {
        // Given — 미등록 userId 로 발급된 토큰 + 세션
        String refreshToken = jwtTokenProvider.createRefreshToken(999L);
        savedRefreshTokens.put(999L, sha256(refreshToken));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("쿠키 누락 - null/blank Refresh Token → INVALID_REFRESH_TOKEN")
    void refresh_blankToken_throws() {
        // When & Then
        assertThrows(BusinessException.class, () -> authService.refreshAccessToken(null));
        assertThrows(BusinessException.class, () -> authService.refreshAccessToken(""));
        assertThrows(BusinessException.class, () -> authService.refreshAccessToken("   "));
    }

    @Test
    @DisplayName("보안 - 응답 DTO toString 에 refreshToken 원문 미노출 (JWT 로그 유출 방지)")
    void refresh_responseToStringHidesToken() {
        // Given
        RefreshTokenResponseDTO dto = RefreshTokenResponseDTO.builder()
                .tokenInfo(LoginResponseDTO.TokenInfo.of("Bearer", "access-token-jwt", 900))
                .refreshToken("secret-refresh-token-value")
                .refreshTokenMaxAgeSeconds(1209600)
                .build();

        // When
        String text = dto.toString();

        // Then
        assertFalse(text.contains("secret-refresh-token-value"));
    }

    // ---------- 로그아웃 ----------

    @Test
    @DisplayName("정상 로그아웃 - Refresh Token 세션 삭제 (Redis hash 제거)")
    void logout_success() {
        // Given — 유효한 세션
        String refreshToken = issueRefreshTokenSession(501L);
        assertTrue(savedRefreshTokens.containsKey(501L));

        // When
        authService.logout(refreshToken);

        // Then — Redis(refresh:token:{userId}) 저장 hash 삭제 — 이후 재발급 불가
        assertFalse(savedRefreshTokens.containsKey(501L));
        verify(refreshTokenStore).delete(501L);
    }

    @Test
    @DisplayName("로그아웃 후 Refresh Token 재사용 불가 - refreshAccessToken 이 INVALID_REFRESH_TOKEN")
    void logout_afterLogout_refreshFails() {
        // Given — 유효한 세션
        String refreshToken = issueRefreshTokenSession(501L);

        // When — 로그아웃으로 세션 폐기
        authService.logout(refreshToken);

        // Then — 기존 Refresh Token 으로 Access Token 재발급 불가
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.refreshAccessToken(refreshToken));

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("쿠키 누락 - null/blank Refresh Token → INVALID_REFRESH_TOKEN")
    void logout_blankToken_throws() {
        // When & Then
        assertThrows(BusinessException.class, () -> authService.logout(null));
        assertThrows(BusinessException.class, () -> authService.logout(""));
        assertThrows(BusinessException.class, () -> authService.logout("   "));
    }

    @Test
    @DisplayName("위변조된 Refresh Token - INVALID_REFRESH_TOKEN + 세션 유지")
    void logout_tamperedToken_throws() {
        // Given — 유효한 세션의 토큰을 위변조한다
        String refreshToken = issueRefreshTokenSession(501L);
        // 끝에서 두 번째 base64 글자를 바꾼다 (signup/refresh 위변조 테스트와 동일한 방식)
        String tampered = refreshToken.substring(0, refreshToken.length() - 2)
                + (refreshToken.charAt(refreshToken.length() - 2) == 'a' ? "b" : "a")
                + refreshToken.charAt(refreshToken.length() - 1);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.logout(tampered));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
        // JWT 서명 검증 실패 단계에서 차단 — 정상 세션은 그대로 유지된다
        assertTrue(savedRefreshTokens.containsKey(501L));
        verify(refreshTokenStore, never()).delete(anyLong());
    }

    @Test
    @DisplayName("만료된 Refresh Token - INVALID_REFRESH_TOKEN")
    void logout_expiredToken_throws() {
        // Given — 만료된 Refresh Token
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        String expired = jwtTokenProvider.createRefreshToken(501L,
                new Date(System.currentTimeMillis() - 60_000L));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.logout(expired));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("Access Token 으로 로그아웃 요청 - 용도 오류 → INVALID_REFRESH_TOKEN")
    void logout_accessTokenMisuse_throws() {
        // Given — Access Token (용도 오류 검증)
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        String accessToken = jwtTokenProvider.createAccessToken(501L);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.logout(accessToken));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("Redis 저장 hash 없음(이미 로그아웃/TTL 만료) - INVALID_REFRESH_TOKEN + 추가 삭제 없음")
    void logout_noStoredHash_throws() {
        // Given — 이미 로그아웃 되어 Redis 세션이 없는 상태
        String refreshToken = issueRefreshTokenSession(501L);
        savedRefreshTokens.remove(501L);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.logout(refreshToken));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
        // 저장 hash 가 없으므로 추가 delete 호출이 발생하지 않는다
        verify(refreshTokenStore, never()).delete(anyLong());
    }

    @Test
    @DisplayName("Redis hash 불일치 - 세션 revoke + INVALID_REFRESH_TOKEN")
    void logout_hashMismatch_revokesSession() {
        // Given — 저장된 hash 를 다른 값으로 덮어쓴다 (클라이언트 토큰 != 저장소 토큰 = 위변조/재사용 의심)
        String refreshToken = issueRefreshTokenSession(501L);
        savedRefreshTokens.put(501L, "forged-hash-value");

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.logout(refreshToken));

        // Then
        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, ex.getErrorCode());
        // refreshAccessToken 의 재사용 감지와 동일 — 세션 revoke 로 저장 hash 삭제
        assertFalse(savedRefreshTokens.containsKey(501L));
        verify(refreshTokenStore).delete(501L);
    }

    // ---------- 비밀번호 재설정 1단계 (본인 확인 및 인증 토큰 발급) ----------

    /**
     * 비밀번호 재설정(verify) 조회용 회원 등록 — email_hash/phone_hash 조회 경로에 동일 회원을 Stub 한다.
     * - identityCiHash 는 PASS 인증 결과(CI = "MOCK-CI-{identityVerificationId}") 의 SHA-256 해시와 일치해야 한다
     * - findUserById 는 재설정 플로우에서 사용하지 않으므로 Stub 하지 않는다
     */
    private void registerPasswordResetUser(Long userId, String email, String phoneNumber,
                                           String identityCiHash, String status) {
        LoginUserVO user = new LoginUserVO();
        user.setId(userId);
        user.setStatus(status);
        user.setNameEncrypt(PersonalDataCipher.encrypt("홍길동"));
        user.setPasswordHash(PasswordEncryptor.encode("password123!"));
        user.setIdentityCiHash(identityCiHash);
        lenient().when(authMapper.findUserByEmailHash(sha256(email))).thenReturn(user);
        lenient().when(authMapper.findUserByPhoneHash(sha256(phoneNumber))).thenReturn(user);
    }

    /** 비밀번호 재설정 1단계 요청 DTO 생성 헬퍼 */
    private PasswordVerifyRequestDTO passwordVerifyRequest(String loginId, String identityVerificationId) {
        PasswordVerifyRequestDTO request = new PasswordVerifyRequestDTO();
        request.setLoginId(loginId);
        request.setIdentityVerificationId(identityVerificationId);
        return request;
    }

    /** 비밀번호 재설정 2단계 요청 DTO 생성 헬퍼 */
    private PasswordResetRequestDTO passwordResetRequest(String token, String newPassword) {
        PasswordResetRequestDTO request = new PasswordResetRequestDTO();
        request.setPasswordResetToken(token);
        request.setNewPassword(newPassword);
        return request;
    }

    @Test
    @DisplayName("재설정 토큰 발급 성공 - email hash 조회 + CI 대조 + Redis 저장 + UUID 반환")
    void passwordVerify_success() {
        // Given — PASS 인증 성공 + loginId 이메일과 CI 가 일치하는 ACTIVE 회원
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPasswordResetUser(501L, "user@example.com", "01034567890",
                sha256("MOCK-CI-imp_ver_9876543210"), "ACTIVE");

        // When
        PasswordVerifyResponseDTO result = authService.verifyPasswordReset(
                passwordVerifyRequest("user@example.com", "imp_ver_9876543210"));

        // Then — UUID 토큰 발급 + Redis(password:reset:{token})에 userId 매핑 저장
        // (TTL 5분은 저장소가 설정값으로 내부 적용 — Service 는 TTL 을 알지 못한다)
        assertNotNull(result);
        assertNotNull(result.getPasswordResetToken());
        assertFalse(result.getPasswordResetToken().trim().isEmpty());

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordResetTokenStore).save(tokenCaptor.capture(), eq(501L));
        assertEquals(result.getPasswordResetToken(), tokenCaptor.getValue());

        // 저장된 토큰으로 userId 복원 가능 (2단계 reset 에서 사용)
        assertEquals(Long.valueOf(501L), savedPasswordResetTokens.get(result.getPasswordResetToken()));

        // 이메일 원문이 아니라 email_hash 로만 조회했는지 확인
        verify(authMapper).findUserByEmailHash(sha256("user@example.com"));
        verify(authMapper, never()).findUserByPhoneHash(anyString());
    }

    @Test
    @DisplayName("재설정 토큰 발급 성공 - 휴대폰 loginId 는 하이픈 제거 후 phone_hash 로 조회")
    void passwordVerify_phoneLoginId_success() {
        // Given — PASS 인증 성공 + CI 일치 회원
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPasswordResetUser(501L, "user@example.com", "01034567890",
                sha256("MOCK-CI-imp_ver_9876543210"), "ACTIVE");

        // When — 하이픈 포함 휴대폰 번호로 요청
        PasswordVerifyResponseDTO result = authService.verifyPasswordReset(
                passwordVerifyRequest("010-3456-7890", "imp_ver_9876543210"));

        // Then — phone_number_encrypt(원문)이 아니라 phone_hash 로만 조회
        assertNotNull(result.getPasswordResetToken());
        verify(authMapper).findUserByPhoneHash(sha256("01034567890"));
        verify(authMapper, never()).findUserByEmailHash(anyString());
    }

    @Test
    @DisplayName("재설정 토큰 발급 - loginId 로 조회되는 회원 없음 → USER_NOT_FOUND (404)")
    void passwordVerify_userNotFound_throws() {
        // When — 미가입 loginId (Mock 조회 결과 null)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.verifyPasswordReset(
                        passwordVerifyRequest("unknown@example.com", "imp_ver_9876543210")));

        // Then — Provider 호출 없이 404 (계정 존재 여부 노출 최소화)
        assertEquals(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(identityVerificationProvider, never()).verify(any());
    }

    @Test
    @DisplayName("재설정 토큰 발급 - 탈퇴(WITHDRAWN) 회원 → USER_NOT_FOUND (계정 존재 여부 노출 방지)")
    void passwordVerify_withdrawnUser_throws() {
        // Given — CI 는 일치하지만 탈퇴 상태
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPasswordResetUser(501L, "user@example.com", "01034567890",
                sha256("MOCK-CI-imp_ver_9876543210"), "WITHDRAWN");

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.verifyPasswordReset(
                        passwordVerifyRequest("user@example.com", "imp_ver_9876543210")));

        // Then — 탈퇴 회원도 USER_NOT_FOUND 로 통일 (토큰 미발급)
        assertEquals(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        assertTrue(savedPasswordResetTokens.isEmpty());
    }

    @Test
    @DisplayName("재설정 토큰 발급 - PASS 인증 CI 와 회원 CI 불일치 → VERIFICATION_FAILED (400)")
    void passwordVerify_ciMismatch_throws() {
        // Given — 회원의 identity_ci_hash 가 PASS 인증 결과(CI)와 다르다
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPasswordResetUser(501L, "user@example.com", "01034567890",
                sha256("MOCK-CI-OTHER-USER"), "ACTIVE");

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.verifyPasswordReset(
                        passwordVerifyRequest("user@example.com", "imp_ver_9876543210")));

        // Then
        assertEquals(AuthErrorCode.VERIFICATION_FAILED, ex.getErrorCode());
        // CI 불일치 시 토큰이 발급되지 않아야 한다
        assertTrue(savedPasswordResetTokens.isEmpty());
    }

    @Test
    @DisplayName("재설정 토큰 발급 - PASS 인증 실패 → INVALID_VERIFICATION_ID (Provider 예외 전파)")
    void passwordVerify_invalidVerification_throws() {
        // Given — 회원은 존재하지만 Provider 가 인증 실패를 던진다
        registerPasswordResetUser(501L, "user@example.com", "01034567890",
                sha256("MOCK-CI-imp_ver_9876543210"), "ACTIVE");
        when(identityVerificationProvider.verify("invalid"))
                .thenThrow(new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.verifyPasswordReset(
                        passwordVerifyRequest("user@example.com", "invalid")));

        // Then
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, ex.getErrorCode());
    }

    @Test
    @DisplayName("재설정 토큰 발급 - loginId/identityVerificationId 누락 → INVALID_PASSWORD_RESET_REQUEST (400)")
    void passwordVerify_blankRequest_throws() {
        // When & Then — null 요청
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> authService.verifyPasswordReset(null));
        assertEquals(AuthErrorCode.INVALID_PASSWORD_RESET_REQUEST, nullEx.getErrorCode());

        // loginId 누락
        assertThrows(BusinessException.class,
                () -> authService.verifyPasswordReset(passwordVerifyRequest("", "imp_ver_9876543210")));
        // identityVerificationId 누락
        assertThrows(BusinessException.class,
                () -> authService.verifyPasswordReset(passwordVerifyRequest("user@example.com", "  ")));

        // 요청 값 검증 실패 시 Provider 호출 없이 차단된다
        verify(identityVerificationProvider, never()).verify(any());
    }

    // ---------- 비밀번호 재설정 2단계 (비밀번호 변경) ----------

    @Test
    @DisplayName("비밀번호 변경 성공 - BCrypt 해시로 password_hash 갱신 + Redis 토큰 1회성 삭제")
    void passwordReset_success() {
        // Given — 1단계에서 발급된 유효한 토큰 (userId 매핑 저장) + 갱신 성공
        savedPasswordResetTokens.put("reset-token-1", 501L);
        when(authMapper.updatePasswordHash(eq(501L), anyString())).thenReturn(1);

        // When
        authService.resetPassword(passwordResetRequest("reset-token-1", "NewPassword123!"));

        // Then — BCrypt 해시로 갱신 (원문과 다르고 matches 검증 통과)
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(authMapper).updatePasswordHash(eq(501L), hashCaptor.capture());
        assertNotEquals("NewPassword123!", hashCaptor.getValue());
        assertTrue(PasswordEncryptor.matches("NewPassword123!", hashCaptor.getValue()));

        // 사용 완료 후 Redis 토큰 삭제 — 재사용 불가
        assertTrue(savedPasswordResetTokens.isEmpty());
        verify(passwordResetTokenStore).delete("reset-token-1");
    }

    @Test
    @DisplayName("비밀번호 변경 - 만료/존재하지 않는 토큰 → RESET_TIMEOUT_OR_INVALID_TOKEN (400)")
    void passwordReset_expiredOrInvalidToken_throws() {
        // When — Redis 에 저장된 토큰이 없다 (TTL 만료/사용 완료/위조)
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(passwordResetRequest("expired-token", "NewPassword123!")));

        // Then
        assertEquals(AuthErrorCode.RESET_TIMEOUT_OR_INVALID_TOKEN, ex.getErrorCode());
        // DB 갱신이 발생하지 않아야 한다
        verify(authMapper, never()).updatePasswordHash(anyLong(), anyString());
    }

    @Test
    @DisplayName("비밀번호 변경 - 약한 비밀번호 → WEAK_PASSWORD (422) + DB 갱신/토큰 삭제 없음")
    void passwordReset_weakPassword_throws() {
        // Given — 유효한 토큰
        savedPasswordResetTokens.put("reset-token-1", 501L);

        // When & Then — 특수문자 누락 (영문+숫자만)
        BusinessException noSpecial = assertThrows(BusinessException.class,
                () -> authService.resetPassword(passwordResetRequest("reset-token-1", "password123")));
        assertEquals(AuthErrorCode.WEAK_PASSWORD, noSpecial.getErrorCode());

        // 숫자 누락 (영문+특수문자만)
        assertThrows(BusinessException.class,
                () -> authService.resetPassword(passwordResetRequest("reset-token-1", "Password!")));
        // 영문 누락 (숫자+특수문자만)
        assertThrows(BusinessException.class,
                () -> authService.resetPassword(passwordResetRequest("reset-token-1", "12345678!")));
        // 8자 미만
        assertThrows(BusinessException.class,
                () -> authService.resetPassword(passwordResetRequest("reset-token-1", "Abc12!")));

        // 정책 검증 실패 시 DB 갱신이 발생하지 않고, 토큰은 삭제되지 않는다 (재시도 가능)
        verify(authMapper, never()).updatePasswordHash(anyLong(), anyString());
        assertEquals(Long.valueOf(501L), savedPasswordResetTokens.get("reset-token-1"));
    }

    @Test
    @DisplayName("비밀번호 변경 - newPassword 누락/빈 값 → WEAK_PASSWORD (422)")
    void passwordReset_blankPassword_throws() {
        // Given — 유효한 토큰
        savedPasswordResetTokens.put("reset-token-1", 501L);

        // When & Then — null/빈 값은 정책 미달로 간주
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> authService.resetPassword(passwordResetRequest("reset-token-1", null)));
        assertEquals(AuthErrorCode.WEAK_PASSWORD, nullEx.getErrorCode());

        assertThrows(BusinessException.class,
                () -> authService.resetPassword(passwordResetRequest("reset-token-1", "")));
    }

    @Test
    @DisplayName("비밀번호 변경 - passwordResetToken 누락/빈 값 → RESET_TIMEOUT_OR_INVALID_TOKEN (400)")
    void passwordReset_blankToken_throws() {
        // When & Then — null 요청
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> authService.resetPassword(null));
        assertEquals(AuthErrorCode.RESET_TIMEOUT_OR_INVALID_TOKEN, nullEx.getErrorCode());

        // token 누락
        BusinessException blankEx = assertThrows(BusinessException.class,
                () -> authService.resetPassword(passwordResetRequest("", "NewPassword123!")));
        assertEquals(AuthErrorCode.RESET_TIMEOUT_OR_INVALID_TOKEN, blankEx.getErrorCode());
    }

    @Test
    @DisplayName("비밀번호 변경 - 갱신 행 수 0 (회원 인증 정보 없음) → RESET_TIMEOUT_OR_INVALID_TOKEN + 토큰 1회성 폐기")
    void passwordReset_updateAffectedZero_throws() {
        // Given — 유효한 토큰이지만 갱신 대상 행이 없다 (회원 탈퇴 등)
        savedPasswordResetTokens.put("reset-token-1", 501L);
        when(authMapper.updatePasswordHash(eq(501L), anyString())).thenReturn(0);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPassword(passwordResetRequest("reset-token-1", "NewPassword123!")));

        // Then
        assertEquals(AuthErrorCode.RESET_TIMEOUT_OR_INVALID_TOKEN, ex.getErrorCode());
        // 일회성 토큰 정책 — 갱신 대상이 없으면(회원 탈퇴 등) 재시도를 막기 위해 토큰도 즉시 폐기한다
        assertFalse(savedPasswordResetTokens.containsKey("reset-token-1"));
        verify(passwordResetTokenStore).delete("reset-token-1");
    }

    @Test
    @DisplayName("보안 - PasswordResetRequestDTO toString 에 newPassword 원문 미노출")
    void passwordReset_requestToStringHidesSecret() {
        // Given
        PasswordResetRequestDTO request = passwordResetRequest("reset-token-1", "NewPassword123!");

        // When
        String text = request.toString();

        // Then
        assertFalse(text.contains("NewPassword123!"));
    }

    // ---------- PIN 번호 최초 설정 ----------

    /** PIN 설정 요청 DTO 생성 헬퍼 */
    private PinSetupRequestDTO pinSetupRequest(String pinNumber, String deviceId, String deviceName) {
        PinSetupRequestDTO request = new PinSetupRequestDTO();
        request.setPinNumber(pinNumber);
        request.setDeviceId(deviceId);
        request.setDeviceName(deviceName);
        return request;
    }

    @Test
    @DisplayName("PIN 최초 설정 성공 - BCrypt 암호화 + 원문 미저장 + user_device 저장")
    void pinSetup_success() {
        // Given — ACTIVE 회원 + 해당 기기 미등록
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        when(authMapper.countByUserIdAndDeviceId(eq(501L), eq("device-uuid-1"))).thenReturn(0);

        // When
        authService.setupPin(501L, pinSetupRequest("123456", "device-uuid-1", "Chrome / Windows"));

        // Then — 기기 등록 여부 확인 후 user_device insert (BCrypt 해시 저장, 원문 미저장)
        verify(authMapper).countByUserIdAndDeviceId(eq(501L), eq("device-uuid-1"));
        ArgumentCaptor<UserDeviceVO> deviceCaptor = ArgumentCaptor.forClass(UserDeviceVO.class);
        verify(authMapper).insertUserDevice(deviceCaptor.capture());
        UserDeviceVO saved = deviceCaptor.getValue();
        assertEquals(501L, saved.getUserId());
        assertEquals("device-uuid-1", saved.getDeviceId());
        assertEquals("Chrome / Windows", saved.getDeviceName());
        assertNotEquals("123456", saved.getPinHash(), "PIN 원문 저장 금지");
        assertTrue(PasswordEncryptor.matches("123456", saved.getPinHash()), "BCrypt 해시로 검증 가능해야 한다");
    }

    @Test
    @DisplayName("PIN 형식 오류 - 5자리 → INVALID_PIN_FORMAT + 저장 미수행")
    void pinSetup_pinTooShort_throws() {
        // Given — ACTIVE 회원 + 기기 미등록
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        when(authMapper.countByUserIdAndDeviceId(eq(501L), eq("device-uuid-1"))).thenReturn(0);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest("12345", "device-uuid-1", "Chrome / Windows")));
        assertEquals(AuthErrorCode.INVALID_PIN_FORMAT, ex.getErrorCode());

        verify(authMapper, never()).insertUserDevice(any(UserDeviceVO.class));
    }

    @Test
    @DisplayName("PIN 형식 오류 - 7자리 → INVALID_PIN_FORMAT + 저장 미수행")
    void pinSetup_pinTooLong_throws() {
        // Given — ACTIVE 회원 + 기기 미등록
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        when(authMapper.countByUserIdAndDeviceId(eq(501L), eq("device-uuid-1"))).thenReturn(0);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest("1234567", "device-uuid-1", "Chrome / Windows")));
        assertEquals(AuthErrorCode.INVALID_PIN_FORMAT, ex.getErrorCode());

        verify(authMapper, never()).insertUserDevice(any(UserDeviceVO.class));
    }

    @Test
    @DisplayName("PIN 형식 오류 - 문자 포함 → INVALID_PIN_FORMAT + 저장 미수행")
    void pinSetup_pinContainsLetter_throws() {
        // Given — ACTIVE 회원 + 기기 미등록
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        when(authMapper.countByUserIdAndDeviceId(eq(501L), eq("device-uuid-1"))).thenReturn(0);

        // When & Then — 숫자+문자 혼합 / 전부 문자 모두 형식 오류
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest("12ab56", "device-uuid-1", "Chrome / Windows")));
        assertEquals(AuthErrorCode.INVALID_PIN_FORMAT, ex.getErrorCode());

        assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest("abcdef", "device-uuid-1", "Chrome / Windows")));

        verify(authMapper, never()).insertUserDevice(any(UserDeviceVO.class));
    }

    @Test
    @DisplayName("이미 PIN 등록된 기기 - PIN_ALREADY_EXISTS(409) + 저장 미수행")
    void pinSetup_alreadyRegistered_throws() {
        // Given — ACTIVE 회원 + 이미 등록된 기기 (count > 0)
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        when(authMapper.countByUserIdAndDeviceId(eq(501L), eq("device-uuid-1"))).thenReturn(1);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest("123456", "device-uuid-1", "Chrome / Windows")));
        assertEquals(AuthErrorCode.PIN_ALREADY_EXISTS, ex.getErrorCode());

        verify(authMapper, never()).insertUserDevice(any(UserDeviceVO.class));
    }

    @Test
    @DisplayName("PIN 최초 설정 - 회원 없음(탈퇴 등) → USER_NOT_FOUND(404)")
    void pinSetup_userNotFound_throws() {
        // Given — Mock Mapper 에 회원이 등록되지 않음 (findUserById → null)

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.setupPin(999L, pinSetupRequest("123456", "device-uuid-1", "Chrome / Windows")));
        assertEquals(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        verify(authMapper, never()).insertUserDevice(any(UserDeviceVO.class));
    }

    @Test
    @DisplayName("PIN 최초 설정 - 필수 값 누락/빈 값 → INVALID_PIN_SETUP_REQUEST(400)")
    void pinSetup_missingRequiredField_throws() {
        // When & Then — deviceName 누락
        BusinessException missingName = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest("123456", "device-uuid-1", null)));
        assertEquals(AuthErrorCode.INVALID_PIN_SETUP_REQUEST, missingName.getErrorCode());

        // deviceId 빈 값
        BusinessException missingDeviceId = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest("123456", "  ", "Chrome / Windows")));
        assertEquals(AuthErrorCode.INVALID_PIN_SETUP_REQUEST, missingDeviceId.getErrorCode());

        // pinNumber 누락
        BusinessException missingPin = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest(null, "device-uuid-1", "Chrome / Windows")));
        assertEquals(AuthErrorCode.INVALID_PIN_SETUP_REQUEST, missingPin.getErrorCode());

        // 요청 자체가 null
        BusinessException nullRequest = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, null));
        assertEquals(AuthErrorCode.INVALID_PIN_SETUP_REQUEST, nullRequest.getErrorCode());

        verify(authMapper, never()).insertUserDevice(any(UserDeviceVO.class));
    }

    @Test
    @DisplayName("보안 - PinSetupRequestDTO toString 에 pinNumber 원문 미노출")
    void pinSetup_requestToStringHidesPin() {
        // Given
        PinSetupRequestDTO request = pinSetupRequest("123456", "device-uuid-1", "Chrome / Windows");

        // When
        String text = request.toString();

        // Then — PIN 원문이 로그에 노출되지 않도록 @ToString.Exclude 처리 확인
        assertFalse(text.contains("123456"));
    }

    /** 길이 검증 테스트용 반복 문자열 생성 헬퍼 (Java 8 — String.repeat 미사용) */
    private static String repeatChar(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    @Test
    @DisplayName("PIN 최초 설정 - deviceId/deviceName 100자 초과 → INVALID_PIN_SETUP_REQUEST(400)")
    void pinSetup_deviceInfoTooLong_throws() {
        // When & Then — deviceId 101자
        BusinessException longDeviceId = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest("123456", repeatChar('i', 101), "Chrome / Windows")));
        assertEquals(AuthErrorCode.INVALID_PIN_SETUP_REQUEST, longDeviceId.getErrorCode());

        // deviceName 101자
        BusinessException longDeviceName = assertThrows(BusinessException.class,
                () -> authService.setupPin(501L, pinSetupRequest("123456", "device-uuid-1", repeatChar('d', 101))));
        assertEquals(AuthErrorCode.INVALID_PIN_SETUP_REQUEST, longDeviceName.getErrorCode());

        verify(authMapper, never()).insertUserDevice(any(UserDeviceVO.class));
    }

    @Test
    @DisplayName("PIN 최초 설정 - deviceName 100자(경계값) 정상 허용")
    void pinSetup_deviceInfoMaxLengthBoundary_success() {
        // Given — ACTIVE 회원 + 기기 미등록 + deviceName 이 정확히 100자
        registerLoginUser(501L, "user@example.com", "01034567890",
                "password123!", "123456", "device-uuid-1", "ACTIVE");
        when(authMapper.countByUserIdAndDeviceId(eq(501L), eq("device-uuid-1"))).thenReturn(0);

        // When
        authService.setupPin(501L, pinSetupRequest("123456", "device-uuid-1", repeatChar('d', 100)));

        // Then — 100자까지는 저장 허용 (500 오류 없이 정상 흐름)
        ArgumentCaptor<UserDeviceVO> deviceCaptor = ArgumentCaptor.forClass(UserDeviceVO.class);
        verify(authMapper).insertUserDevice(deviceCaptor.capture());
        assertEquals(100, deviceCaptor.getValue().getDeviceName().length());
    }

    // ---------- 보안 PIN 번호 재설정 (로그인 사용자 + PASS 재인증) ----------

    /**
     * PIN 재설정 조회용 회원 등록 — JWT userId 조회 경로(selectUserAuthById)에 회원을 Stub 한다.
     * - identityCiHash 는 PASS 인증 결과(CI = "MOCK-CI-{identityVerificationId}") 의 SHA-256 해시와 일치해야 한다
     */
    private void registerPinResetUser(Long userId, String identityCiHash, String status) {
        LoginUserVO user = new LoginUserVO();
        user.setId(userId);
        user.setStatus(status);
        user.setIdentityCiHash(identityCiHash);
        lenient().when(authMapper.selectUserAuthById(userId)).thenReturn(user);
    }

    /** PIN 재설정 요청 DTO 생성 헬퍼 */
    private PinResetRequestDTO pinResetRequest(String identityVerificationId, String pinNumber) {
        PinResetRequestDTO request = new PinResetRequestDTO();
        request.setIdentityVerificationId(identityVerificationId);
        request.setPinNumber(pinNumber);
        return request;
    }

    @Test
    @DisplayName("PIN 재설정 성공 - PASS 재인증 + CI 대조 후 BCrypt 해시로 pin_hash 갱신 (원문 미저장) + 잠금 해제")
    void pinReset_success() {
        // Given — JWT 로그인 사용자 + PASS 인증 CI 일치 + 기존 PIN(123456)과 다른 신규 PIN + 갱신 성공 + 잠금 상태
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPinResetUser(501L, sha256("MOCK-CI-imp_ver_9876543210"), "ACTIVE");
        // 기존 등록 기기의 pin_hash 는 123456 의 BCrypt 해시 (신규 PIN 654321 과 다름 — SAME_AS_CURRENT_PIN 통과)
        when(authMapper.selectPinHashesByUserId(501L))
                .thenReturn(Collections.singletonList(PasswordEncryptor.encode("123456")));
        when(authMapper.updateUserDevicePinHash(eq(501L), anyString())).thenReturn(1);
        failCounts.put(501L, 5);

        // When
        authService.resetPin(501L, pinResetRequest("imp_ver_9876543210", "654321"));

        // Then — BCrypt 해시로 갱신 (신규/기존 PIN 원문 미저장 + matches 검증 통과)
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(authMapper).updateUserDevicePinHash(eq(501L), hashCaptor.capture());
        assertNotEquals("654321", hashCaptor.getValue(), "신규 PIN 원문 저장 금지");
        assertNotEquals("123456", hashCaptor.getValue(), "기존 PIN 원문 저장 금지");
        assertTrue(PasswordEncryptor.matches("654321", hashCaptor.getValue()),
                "신규 PIN 의 BCrypt 해시로 검증 가능해야 한다");
        assertFalse(PasswordEncryptor.matches("123456", hashCaptor.getValue()),
                "기존 PIN 으로는 검증 실패해야 한다 (변경 확인)");

        // JWT userId 기준으로 조회해 CI hash 로 대조했는지 검증 (원문 CI 로 비교 금지)
        verify(authMapper).selectUserAuthById(501L);
        // 기존 PIN 대조가 해시 조회 후 BCrypt matches 로만 수행됐는지 검증 (원문 비교 금지)
        verify(authMapper).selectPinHashesByUserId(501L);

        // PASS 재인증 기반 재설정은 잠금 해제 수단 — 실패 횟수 5회(잠금) 상태도 초기화되어야 한다
        verify(loginFailCounter).reset(501L);
        assertEquals(0, loginFailCounter.getCount(501L));
    }

    @Test
    @DisplayName("PIN 재설정 - 신규 PIN 이 기존 PIN 과 동일 → SAME_AS_CURRENT_PIN + update 미호출")
    void pinReset_sameAsCurrentPin_throws() {
        // Given — PASS 인증 CI 일치 + 기존 등록 기기의 pin_hash 가 신규 PIN(654321) 과 동일한 해시
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPinResetUser(501L, sha256("MOCK-CI-imp_ver_9876543210"), "ACTIVE");
        when(authMapper.selectPinHashesByUserId(501L))
                .thenReturn(Collections.singletonList(PasswordEncryptor.encode("654321")));

        // When — 기존 PIN 과 동일한 번호로 재설정 시도
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("imp_ver_9876543210", "654321")));

        // Then
        assertEquals(AuthErrorCode.SAME_AS_CURRENT_PIN, ex.getErrorCode());
        // 동일 PIN 차단 시 DB 갱신/잠금 해제가 발생하지 않아야 한다
        verify(authMapper, never()).updateUserDevicePinHash(anyLong(), anyString());
        verify(loginFailCounter, never()).reset(anyLong());
    }

    @Test
    @DisplayName("PIN 재설정 - 여러 기기 중 하나라도 기존 PIN 과 동일 → SAME_AS_CURRENT_PIN")
    void pinReset_sameAsCurrentPin_anyDevice_throws() {
        // Given — PASS 인증 CI 일치 + 기기 2대 중 1대의 pin_hash 가 신규 PIN(654321) 과 동일
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPinResetUser(501L, sha256("MOCK-CI-imp_ver_9876543210"), "ACTIVE");
        when(authMapper.selectPinHashesByUserId(501L))
                .thenReturn(Arrays.asList(
                        PasswordEncryptor.encode("111111"),
                        PasswordEncryptor.encode("654321")));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("imp_ver_9876543210", "654321")));

        // Then — 어느 기기든 동일 PIN 이 있으면 재설정 불가
        assertEquals(AuthErrorCode.SAME_AS_CURRENT_PIN, ex.getErrorCode());
        verify(authMapper, never()).updateUserDevicePinHash(anyLong(), anyString());
    }

    @Test
    @DisplayName("PIN 재설정 - PASS 인증 실패(잘못된 identityVerificationId) → INVALID_VERIFICATION_ID + update 미호출")
    void pinReset_invalidVerification_throws() {
        // Given — 로그인 사용자 존재 + Provider 가 인증 실패를 던진다
        registerPinResetUser(501L, sha256("MOCK-CI-imp_ver_9876543210"), "ACTIVE");
        when(identityVerificationProvider.verify("invalid"))
                .thenThrow(new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID));

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("invalid", "654321")));

        // Then
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, ex.getErrorCode());
        verify(authMapper, never()).updateUserDevicePinHash(anyLong(), anyString());
    }

    @Test
    @DisplayName("PIN 재설정 - PASS 인증 CI 와 로그인 사용자 CI 불일치 → VERIFICATION_FAILED + update 미호출")
    void pinReset_ciMismatch_throws() {
        // Given — PASS 인증은 성공하지만 로그인 사용자의 identity_ci_hash 와 다르다 (타인 인증 사용)
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPinResetUser(501L, sha256("MOCK-CI-OTHER-USER"), "ACTIVE");

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("imp_ver_9876543210", "654321")));

        // Then
        assertEquals(AuthErrorCode.VERIFICATION_FAILED, ex.getErrorCode());
        verify(authMapper, never()).updateUserDevicePinHash(anyLong(), anyString());
    }

    @Test
    @DisplayName("PIN 재설정 - 잘못된 PIN 형식 → INVALID_PIN_FORMAT + update 미호출")
    void pinReset_invalidPinFormat_throws() {
        // Given — PASS 인증 CI 일치
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPinResetUser(501L, sha256("MOCK-CI-imp_ver_9876543210"), "ACTIVE");

        // When & Then — 5자리 / 7자리 / 문자 포함 모두 형식 오류
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("imp_ver_9876543210", "12345")));
        assertEquals(AuthErrorCode.INVALID_PIN_FORMAT, ex.getErrorCode());

        assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("imp_ver_9876543210", "1234567")));
        assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("imp_ver_9876543210", "12ab56")));
        assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("imp_ver_9876543210", "")));

        // 형식 검증 실패 시 DB 갱신이 발생하지 않아야 한다 (PIN 변경 실패 시 update 미호출)
        verify(authMapper, never()).updateUserDevicePinHash(anyLong(), anyString());
    }

    @Test
    @DisplayName("PIN 재설정 - 로그인 사용자 없음(탈퇴 등) → USER_NOT_FOUND(404) + Provider/update 미호출")
    void pinReset_userNotFound_throws() {
        // Given — Mock Mapper 에 회원이 등록되지 않음 (selectUserAuthById → null)

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPin(999L, pinResetRequest("imp_ver_9876543210", "654321")));
        assertEquals(AuthErrorCode.USER_NOT_FOUND, ex.getErrorCode());

        // 회원 조회 실패 시 Provider 호출 없이 차단된다 (계정 존재 여부 노출 최소화)
        verify(identityVerificationProvider, never()).verify(any());
        verify(authMapper, never()).updateUserDevicePinHash(anyLong(), anyString());
    }

    @Test
    @DisplayName("PIN 재설정 - 등록된 PIN(기기)이 없는 회원 → PIN_NOT_REGISTERED(400)")
    void pinReset_noRegisteredPin_throws() {
        // Given — PASS 인증 CI 일치 + 기존 pin_hash 조회 결과 없음(등록 기기 없음) + 갱신 대상 행 없음
        when(identityVerificationProvider.verify("imp_ver_9876543210"))
                .thenReturn(mockProviderResult("imp_ver_9876543210"));
        registerPinResetUser(501L, sha256("MOCK-CI-imp_ver_9876543210"), "ACTIVE");
        when(authMapper.selectPinHashesByUserId(501L)).thenReturn(Collections.emptyList());
        when(authMapper.updateUserDevicePinHash(eq(501L), anyString())).thenReturn(0);

        // When
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("imp_ver_9876543210", "654321")));

        // Then
        assertEquals(AuthErrorCode.PIN_NOT_REGISTERED, ex.getErrorCode());
        // PIN 변경이 실패하면 잠금 해제(실패 횟수 초기화)도 수행되지 않는다
        verify(loginFailCounter, never()).reset(anyLong());
    }

    @Test
    @DisplayName("PIN 재설정 - identityVerificationId 누락/빈 값 → INVALID_VERIFICATION_ID + Provider/update 미호출")
    void pinReset_blankVerificationId_throws() {
        // When & Then — null 요청
        BusinessException nullEx = assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, null));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, nullEx.getErrorCode());

        // identityVerificationId 빈 값
        BusinessException blankEx = assertThrows(BusinessException.class,
                () -> authService.resetPin(501L, pinResetRequest("  ", "654321")));
        assertEquals(AuthErrorCode.INVALID_VERIFICATION_ID, blankEx.getErrorCode());

        // 요청 값 검증 실패 시 Provider 호출/DB 갱신 없이 차단된다
        verify(identityVerificationProvider, never()).verify(any());
        verify(authMapper, never()).updateUserDevicePinHash(anyLong(), anyString());
    }

    @Test
    @DisplayName("보안 - PinResetRequestDTO toString 에 PIN/identityVerificationId 원문 미노출")
    void pinReset_requestToStringHidesSecret() {
        // Given — identityVerificationId 에 pinNumber 문자열이 우연히 포함되지 않도록 구분되는 값 사용
        PinResetRequestDTO request = pinResetRequest("imp_ver_1111111111", "654321");

        // When
        String text = request.toString();

        // Then — PIN 원문과 identityVerificationId 모두 로그에 노출되지 않도록 @ToString.Exclude 처리 확인
        assertFalse(text.contains("654321"));
        assertFalse(text.contains("imp_ver_1111111111"));
    }
}
