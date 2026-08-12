package com.workit.security;

import com.workit.domain.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// API 요청 인증 필터 단위 테스트
// - JwtTokenProvider 를 실제 구현(테스트 시크릿)으로 사용해 발급/검증 흐름을 검증한다
// - knowledge.md 필수 테스트: 정상 토큰 / 만료 / 위변조 / 토큰 없음 / 용도 오류(Refresh Token)
// - Cookie 기반 인증 전용: accessToken HttpOnly Cookie 에서만 토큰을 추출한다
//   (Authorization: Bearer 헤더는 사용하지 않는다 — 프론트도 쿠키로만 인증)
class JwtAuthenticationFilterTest {

    /** HS256 최소 32바이트 테스트용 시크릿 (JwtTokenProviderTest 와 동일 정책) */
    private static final String TEST_SECRET = "test-secret-key-for-jwt-hs256-32bytes!!";

    private JwtTokenProvider provider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, 15, 20160);
        filter = new JwtAuthenticationFilter(provider);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** accessToken Cookie 가 없는 기본 요청 */
    private MockHttpServletRequest requestWithoutCookie() {
        return new MockHttpServletRequest("GET", "/api/v1/wallets/me");
    }

    /** accessToken Cookie 를 설정한 요청 (Cookie 기반 인증) */
    private MockHttpServletRequest requestWithCookie(String cookieValue) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/me");
        if (cookieValue != null) {
            request.setCookies(new javax.servlet.http.Cookie(
                    JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, cookieValue));
        }
        return request;
    }

    @Test
    @DisplayName("accessToken Cookie 없음 → 필터 통과 (인증 없이 진행, 보호 경로는 진입점이 처리)")
    void noCookie_passesThrough() throws Exception {
        MockHttpServletRequest request = requestWithoutCookie();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "필터 체인은 계속 진행되어야 한다");
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "인증 객체가 설정되면 안 된다");
    }

    @Test
    @DisplayName("Authorization Bearer 헤더는 무시된다 — Cookie 가 없으면 인증되지 않는다")
    void authorizationHeader_ignored() throws Exception {
        // Authorization 헤더만 있고 accessToken Cookie 가 없는 요청 — 헤더 기반 인증은 사용하지 않는다
        MockHttpServletRequest request = requestWithoutCookie();
        request.addHeader("Authorization", "Bearer some.jwt.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "필터 체인은 계속 진행되어야 한다");
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Bearer 헤더로는 인증되지 않는다");
    }

    @Test
    @DisplayName("정상 Access Token Cookie → SecurityContext 에 WorkitPrincipal(userId, ROLE_USER) 저장")
    void validAccessTokenCookie_authenticates() throws Exception {
        String token = provider.createAccessToken(42L);
        MockHttpServletRequest request = requestWithCookie(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "검증 성공 시에도 필터 체인은 계속 진행되어야 한다");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertInstanceOf(WorkitPrincipal.class, authentication);
        WorkitPrincipal principal = (WorkitPrincipal) authentication;
        assertEquals(42L, principal.getUserId());
        assertEquals("ROLE_USER", principal.getAuthorities().iterator().next().getAuthority());
        assertTrue(principal.isAuthenticated());
    }

    @Test
    @DisplayName("accessToken Cookie 가 위변조 → 400 INVALID_TOKEN 응답 (체인 중단)")
    void accessTokenCookie_invalid_returns400() throws Exception {
        MockHttpServletRequest request = requestWithCookie("invalid.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"INVALID_TOKEN\""));
        assertNull(chain.getRequest());
    }

    @Test
    @DisplayName("만료된 Access Token Cookie → 401 EXPIRED_TOKEN 응답 (체인 중단)")
    void expiredToken_returns401() throws Exception {
        String token = provider.createAccessToken(42L, new Date(System.currentTimeMillis() - 60_000L));
        MockHttpServletRequest request = requestWithCookie(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"EXPIRED_TOKEN\""));
        assertNull(chain.getRequest(), "오류 응답 시 필터 체인은 중단되어야 한다");
    }

    @Test
    @DisplayName("위변조된 토큰 → 400 INVALID_TOKEN 응답 (체인 중단)")
    void invalidToken_returns400() throws Exception {
        MockHttpServletRequest request = requestWithCookie("invalid.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"INVALID_TOKEN\""));
        assertNull(chain.getRequest());
    }

    @Test
    @DisplayName("Refresh Token 을 API 인증에 사용 → 400 INVALID_TOKEN (용도 오류)")
    void refreshTokenRejected() throws Exception {
        String refreshToken = provider.createRefreshToken(42L);
        MockHttpServletRequest request = requestWithCookie(refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"INVALID_TOKEN\""));
    }

    @Test
    @DisplayName("Cookie 값이 빈 문자열 → 400 INVALID_TOKEN")
    void emptyCookie_returns400() throws Exception {
        MockHttpServletRequest request = requestWithCookie("   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"INVALID_TOKEN\""));
    }

    @Test
    @DisplayName("공개 인증 API(/api/v1/auth/**)는 토큰 검증 없이 통과 — 로그인/재발급 흐름 보호")
    void publicAuthPath_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/refresh");
        request.setCookies(new javax.servlet.http.Cookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, "invalid.token.value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "공개 경로는 오류 응답 없이 통과해야 한다");
        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("컨텍스트 패스 배포 - 공개 인증 API는 토큰 검증 없이 통과 (재발급 흐름 보호)")
    void publicAuthPath_withContextPath_passesThrough() throws Exception {
        // WAR 배포 시 getRequestURI() 에 컨텍스트 패스가 붙어도(예: /workit/api/v1/auth/refresh)
        // 공개 경로 판별이 실패하지 않아야 한다 — 만료/위변조 토큰이 재발급 흐름을 막지 않는다
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/workit/api/v1/auth/refresh");
        request.setContextPath("/workit");
        request.setCookies(new javax.servlet.http.Cookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, "invalid.token.value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "컨텍스트 패스가 있어도 공개 경로는 오류 응답 없이 통과해야 한다");
        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("컨텍스트 패스 배포 + 만료 Access Token - 재발급 API 통과 (401 EXPIRED_TOKEN 금지)")
    void publicAuthPath_withContextPath_expiredToken_passesThrough() throws Exception {
        // 실제 장애 재현: Access Token 만료 + 컨텍스트 패스 배포에서 재발급 요청이
        // 401 EXPIRED_TOKEN 으로 차단되던 버그 — 쿠키의 Refresh Token 과 무관하게 필터 단계에서 막힌다
        String expiredToken = provider.createAccessToken(42L, new Date(System.currentTimeMillis() - 60_000L));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/workit/api/v1/auth/refresh");
        request.setContextPath("/workit");
        request.setCookies(new javax.servlet.http.Cookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, expiredToken));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus(), "만료 토큰이 재발급 요청을 막으면 안 된다");
        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("컨텍스트 패스 배포 - 보호 경로는 여전히 토큰 검증 (공개 범위 확대 없음)")
    void protectedPath_withContextPath_stillValidates() throws Exception {
        // 정규화가 과하게 적용되어 보호 경로가 공개로 분류되지 않아야 한다
        String expiredToken = provider.createAccessToken(42L, new Date(System.currentTimeMillis() - 60_000L));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/workit/api/v1/wallets/me");
        request.setContextPath("/workit");
        request.setCookies(new javax.servlet.http.Cookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, expiredToken));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"EXPIRED_TOKEN\""));
        assertNull(chain.getRequest());
    }

    @Test
    @DisplayName("로그인 사용자 전용 경로(/api/v1/auth/me/pin) - 정상 토큰 Cookie → 인증 처리")
    void authenticatedAuthPath_withValidToken_authenticates() throws Exception {
        String token = provider.createAccessToken(77L);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/me/pin");
        request.setCookies(new javax.servlet.http.Cookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, token));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "필터 체인은 계속 진행되어야 한다");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertInstanceOf(WorkitPrincipal.class, authentication);
        assertEquals(77L, ((WorkitPrincipal) authentication).getUserId());
    }

    @Test
    @DisplayName("로그인 사용자 전용 경로(/api/v1/auth/me/pin) - 잘못된 토큰 → 400 거부")
    void authenticatedAuthPath_invalidToken_rejected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/me/pin");
        request.setCookies(new javax.servlet.http.Cookie(
                JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME, "invalid.token.value"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"INVALID_TOKEN\""));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
