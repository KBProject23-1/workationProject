package com.workit.security;

import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.IdentityVerificationProvider;
import com.workit.domain.auth.service.AuthService;
import com.workit.domain.auth.service.AuthServiceImpl;
import com.workit.domain.auth.service.LoginFailCounter;
import com.workit.domain.auth.service.MockPassStore;
import com.workit.domain.auth.service.PasswordResetTokenStore;
import com.workit.domain.auth.service.RefreshTokenStore;
import com.workit.domain.auth.util.JwtTokenProvider;
import com.workit.domain.auth.vo.LoginUserVO;
import com.workit.domain.notification.mapper.NotificationMapper;
import com.workit.domain.wallet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

// Access Token 15분 만료 → /refresh 재발급 → 신규 Access Token 인증 성공 흐름 검증 테스트
//
// 사용자가 실제로 겪는 시나리오를 한 번의 테스트로 그대로 재현한다:
//   1. 로그인 직후 — 15분짜리 Access Token + Refresh Token 발급 (Refresh Token 은 Redis hash 로 저장)
//   2. 15분 경과 — 보호 API 호출 시 JwtAuthenticationFilter 가 401 EXPIRED_TOKEN 으로 응답 (체인 중단)
//   3. 프론트 axios interceptor 의 /refresh 호출과 동일하게 refreshAccessToken(refreshToken) 실행
//      (AuthController.refreshPost → AuthServiceImpl.refreshAccessToken 경로 — Refresh Token Rotation)
//   4. Rotation 으로 발급된 신규 Access Token 으로 보호 API 재호출 → 정상 인증 (SecurityContext 설정)
//
// 실제 구현(운영과 동일한 15분/14일 설정)을 사용하되, DB(Mapper)/Redis(Store) 만 Mock 으로 대체한다.
@ExtendWith(MockitoExtension.class)
class AuthRefreshFlowTest {

    /** HS256 최소 32바이트 테스트용 시크릿 (JwtTokenProviderTest/AuthServiceImplTest 와 동일 정책) */
    private static final String TEST_JWT_SECRET = "fedcba9876543210fedcba9876543210";

    private static final Long USER_ID = 501L;

    @Mock
    private AuthMapper authMapper;

    @Mock
    private IdentityVerificationProvider identityVerificationProvider;

    @Mock
    private MockPassStore mockPassStore;

    @Mock
    private WalletService walletService;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private LoginFailCounter loginFailCounter;

    @Mock
    private PasswordResetTokenStore passwordResetTokenStore;

    @Mock
    private NotificationMapper notificationMapper;

    private JwtTokenProvider jwtTokenProvider;
    private AuthService authService;
    private JwtAuthenticationFilter filter;

    /** RefreshTokenStore 상태형 Mock 의 백킹 맵 — Redis refresh:token:{userId} 를 인메모리로 대체 */
    private final Map<Long, String> savedRefreshTokens = new HashMap<>();

    @BeforeEach
    void setUp() {
        // 실제 JWT Provider — 운영 설정(jwt.access-token-expiration=15, jwt.refresh-token-expiration=20160) 과 동일
        jwtTokenProvider = new JwtTokenProvider(TEST_JWT_SECRET, 15, 20160);
        authService = new AuthServiceImpl(
                authMapper, identityVerificationProvider, mockPassStore, walletService,
                jwtTokenProvider, refreshTokenStore, loginFailCounter, passwordResetTokenStore,
                notificationMapper);
        filter = new JwtAuthenticationFilter(jwtTokenProvider);

        // ACTIVE 회원 조회 — refreshAccessToken 의 회원 상태 확인 단계용
        LoginUserVO user = new LoginUserVO();
        user.setId(USER_ID);
        user.setStatus("ACTIVE");
        lenient().when(authMapper.findUserById(USER_ID)).thenReturn(user);

        // RefreshTokenStore 상태형 Mock — 저장/조회/삭제를 인메모리 맵으로 흉내낸다 (AuthServiceImplTest 와 동일 패턴)
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

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** accessToken Cookie 를 실은 보호 API 요청 (GET /api/v1/wallets/me — 인증 필수 경로) */
    private MockHttpServletRequest protectedRequest(String cookieValue) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/me");
        request.setCookies(new javax.servlet.http.Cookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, cookieValue));
        return request;
    }

    /** 테스트용 SHA-256 hex — Service 의 hash 로직과 동일 규칙으로 기대값을 계산한다 */
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
    @DisplayName("Access Token 15분 만료 → 401 EXPIRED_TOKEN → /refresh 재발급 → 신규 Access Token 으로 정상 인증")
    void expiredAccessToken_refresh_authenticatesWithNewToken() throws Exception {
        // 1. 로그인 직후 상태 — 15분 TTL Access Token + Refresh Token 발급, Refresh Token 은 Redis hash 로 저장
        //    (Refresh Token 은 Service 가 발급하는 기본 만료(14일) 토큰과 exp 가 항상 달라야
        //     JWT iat/exp 가 초 단위로 잘려 동일 문자열이 되는 타이밍 문제 없이
        //     Rotation 을 결정적으로 검증할 수 있다 — AuthServiceImplTest.refresh_success 와 동일 패턴)
        String refreshToken = jwtTokenProvider.createRefreshToken(USER_ID,
                new Date(System.currentTimeMillis() + 120_000L));
        savedRefreshTokens.put(USER_ID, sha256(refreshToken));
        String accessToken = jwtTokenProvider.createAccessToken(USER_ID);

        // 2. 15분 경과 시뮬레이션 — exp 가 과거인 Access Token (운영에서 15분이 지난 것과 동일 상태)
        String expiredAccessToken = jwtTokenProvider.createAccessToken(USER_ID,
                new Date(System.currentTimeMillis() - 60_000L));
        assertNotEquals(accessToken, expiredAccessToken);

        // 3. 만료된 Access Token 으로 보호 API 호출 → 필터가 401 EXPIRED_TOKEN 응답 (체인 중단)
        //    → 프론트 axios interceptor 가 401 을 받아 /refresh 를 호출하는 트리거
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockFilterChain firstChain = new MockFilterChain();
        filter.doFilter(protectedRequest(expiredAccessToken), firstResponse, firstChain);

        assertEquals(401, firstResponse.getStatus());
        assertTrue(firstResponse.getContentAsString().contains("\"errorCode\":\"EXPIRED_TOKEN\""));
        assertNull(firstChain.getRequest(), "만료 토큰은 필터 체인을 통과하지 못한다");

        // 4. 프론트 axios interceptor 의 /refresh 호출과 동일한 재발급 — refreshToken 쿠키로 신규 토큰 발급
        //    (AuthController.refreshPost → AuthServiceImpl.refreshAccessToken — Refresh Token Rotation)
        RefreshTokenResponseDTO refreshed = authService.refreshAccessToken(refreshToken);
        String newAccessToken = refreshed.getAccessToken();
        assertNotNull(newAccessToken);
        assertNotEquals(expiredAccessToken, newAccessToken, "신규 Access Token 은 만료 토큰과 달라야 한다");
        assertNotEquals(expiredAccessToken, refreshed.getRefreshToken());

        // Rotation — 신규 Refresh Token 발급 + Redis hash 교체 (기존 refreshToken 은 재사용 불가)
        assertNotEquals(refreshToken, refreshed.getRefreshToken());
        assertEquals(sha256(refreshed.getRefreshToken()), savedRefreshTokens.get(USER_ID));

        // 5. 신규 Access Token 으로 보호 API 재호출 → 정상 인증 (SecurityContext 에 WorkitPrincipal 저장)
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        MockFilterChain secondChain = new MockFilterChain();
        filter.doFilter(protectedRequest(newAccessToken), secondResponse, secondChain);

        assertNotNull(secondChain.getRequest(), "신규 토큰은 필터 체인을 통과해야 한다");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(USER_ID, ((WorkitPrincipal) authentication).getUserId());
    }
}
