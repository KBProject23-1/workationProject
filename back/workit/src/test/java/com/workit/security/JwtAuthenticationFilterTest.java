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

    private MockHttpServletRequest requestWithAuthorization(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/me");
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }

    @Test
    @DisplayName("인증 헤더 없음 → 필터 통과 (인증 없이 진행, 보호 경로는 진입점이 처리)")
    void noAuthorizationHeader_passesThrough() throws Exception {
        MockHttpServletRequest request = requestWithAuthorization(null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "필터 체인은 계속 진행되어야 한다");
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "인증 객체가 설정되면 안 된다");
    }

    @Test
    @DisplayName("정상 Access Token → SecurityContext 에 WorkitPrincipal(userId, ROLE_USER) 저장")
    void validAccessToken_authenticates() throws Exception {
        String token = provider.createAccessToken(42L);
        MockHttpServletRequest request = requestWithAuthorization("Bearer " + token);
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
    @DisplayName("만료된 Access Token → 401 EXPIRED_TOKEN 응답 (체인 중단)")
    void expiredToken_returns401() throws Exception {
        String token = provider.createAccessToken(42L, new Date(System.currentTimeMillis() - 60_000L));
        MockHttpServletRequest request = requestWithAuthorization("Bearer " + token);
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
        MockHttpServletRequest request = requestWithAuthorization("Bearer invalid.token.value");
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
        MockHttpServletRequest request = requestWithAuthorization("Bearer " + refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(400, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"errorCode\":\"INVALID_TOKEN\""));
    }

    @Test
    @DisplayName("Bearer 접두어 없음 → 토큰 검증 없이 통과 (인증 없음 취급)")
    void nonBearerHeader_passesThrough() throws Exception {
        MockHttpServletRequest request = requestWithAuthorization("Basic abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Bearer 뒤 토큰이 빈 값 → 400 INVALID_TOKEN")
    void emptyToken_returns400() throws Exception {
        MockHttpServletRequest request = requestWithAuthorization("Bearer   ");
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
        request.addHeader("Authorization", "Bearer invalid.token.value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertNotNull(chain.getRequest(), "공개 경로는 오류 응답 없이 통과해야 한다");
        assertEquals(200, response.getStatus());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
