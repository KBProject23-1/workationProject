package com.workit.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

import javax.servlet.http.Cookie;

import org.springframework.security.web.csrf.CsrfToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// CSRF 방어 (Cookie 기반 인증) 테스트
//
// SecurityConfig 가 등록하는 CsrfFilter + CookieCsrfTokenRepository 조합을
// 실제 스프링 컨텍스트 없이 그대로 재현해 검증한다 (SecurityConfig 의 csrfTokenRepository() 와 동일 설정).
//
// 검증 포인트 (knowledge.md CSRF 정책):
//   1. 상태 변경(POST) 요청에 CSRF Token 이 없으면 403 으로 거부된다
//   2. XSRF-TOKEN Cookie 는 HttpOnly=false, Path=/ 로 발급된다 (JS 가 읽을 수 있어야 함)
//   3. XSRF-TOKEN Cookie 값과 X-XSRF-TOKEN Header 가 일치하면 통과한다
//   4. GET 요청은 토큰 없이도 통과한다 (CSRF 검증 대상 제외)
//   5. GET 응답에 XSRF-TOKEN Cookie 가 자동 발급된다 (최초 방문 시 토큰 생성)
class CsrfCookieTest {

    /** SecurityConfig.csrfTokenRepository() 와 동일한 설정으로 필터를 구성한다 */
    private CsrfFilter buildFilter() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setSecure(false); // dev 설정 (운영 true — jwt.csrf-cookie-secure)
        return new CsrfFilter(repository);
    }

    @Test
    @DisplayName("POST - CSRF Token 없이 요청 → 403 거부 + 하위 필터 미호출")
    void post_withoutCsrfToken_denied() throws Exception {
        // Given — 상태 변경 요청 (Cookie/Header 토큰 없음)
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/wallets/charge");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        buildFilter().doFilter(request, response, chain);

        // Then — CSRF 검증 실패 → 403, 컨트롤러(하위 필터)로 진행되지 않는다
        assertEquals(403, response.getStatus(), "CSRF Token 이 없으면 403 이어야 한다");
        assertTrue(chain.getRequest() == null, "CSRF 검증 실패 시 하위 필터 체인이 호출되면 안 된다");
    }

    @Test
    @DisplayName("XSRF-TOKEN Cookie 발급 - HttpOnly=false + Path=/ + GET 은 토큰 없이 통과")
    void get_issuesXsrfCookie_andPasses() throws Exception {
        // Given — 조회 요청 (GET)
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/terms");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        buildFilter().doFilter(request, response, chain);

        // Then — GET 은 CSRF 검증 대상이 아니므로 통과
        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest(), "GET 요청은 CSRF 검증 없이 통과해야 한다");

        // CSRF Token Cookie 자동 발급 — JS 가 읽을 수 있어야 하므로 HttpOnly=false, Path=/
        Cookie xsrfCookie = response.getCookie("XSRF-TOKEN");
        assertNotNull(xsrfCookie, "XSRF-TOKEN Cookie 가 발급되어야 한다");
        assertTrue(!xsrfCookie.isHttpOnly(), "XSRF-TOKEN Cookie 는 HttpOnly=false 여야 한다 (JS 읽기 가능)");
        assertEquals("/", xsrfCookie.getPath(), "XSRF-TOKEN Cookie Path 는 / 이어야 한다");
        assertTrue(xsrfCookie.getValue() != null && !xsrfCookie.getValue().isEmpty(),
                "XSRF-TOKEN Cookie 값이 비어 있으면 안 된다");
    }

    @Test
    @DisplayName("POST - XSRF-TOKEN Cookie + X-XSRF-TOKEN Header 일치 → 통과")
    void post_withMatchingToken_passes() throws Exception {
        CsrfFilter filter = buildFilter();

        // Given — 1) GET 요청으로 XSRF-TOKEN Cookie 발급 (최초 방문 시나리오)
        MockHttpServletResponse issueResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/auth/terms"),
                issueResponse, new MockFilterChain());
        String token = issueResponse.getCookie("XSRF-TOKEN").getValue();

        // Given — 2) 상태 변경 요청에 Cookie + Header 토큰 포함
        MockHttpServletRequest postRequest = new MockHttpServletRequest("POST", "/api/v1/wallets/charge");
        postRequest.setCookies(new Cookie("XSRF-TOKEN", token));
        postRequest.addHeader("X-XSRF-TOKEN", token);
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(postRequest, postResponse, chain);

        // Then — 헤더 토큰이 Cookie(저장소) 토큰과 일치 → 통과
        assertEquals(200, postResponse.getStatus());
        assertNotNull(chain.getRequest(), "CSRF 검증 통과 시 하위 필터 체인이 호출되어야 한다");
    }

    @Test
    @DisplayName("XSRF-TOKEN Cookie 가 이미 있어도 request attribute 에 토큰이 설정된다 (GET /auth/csrf 반복 호출 안전)")
    void tokenAttribute_setEvenWhenCookieExists() throws Exception {
        CsrfFilter filter = buildFilter();

        // Given — 1차 GET: 토큰 생성 + XSRF-TOKEN Cookie 발급
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/auth/csrf"),
                firstResponse, new MockFilterChain());
        String token = firstResponse.getCookie("XSRF-TOKEN").getValue();

        // When — 2차 GET: Cookie 가 이미 존재하는 상태 (GET /auth/csrf 재호출 시나리오)
        MockHttpServletRequest secondRequest = new MockHttpServletRequest("GET", "/api/v1/auth/csrf");
        secondRequest.setCookies(new Cookie("XSRF-TOKEN", token));
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        // Then — Controller(AuthController.csrfTokenGet) 가 읽는 request attribute 가 항상 설정된다
        // (spring-security-web 5.7.11 의 CsrfFilter 는 Cookie 존재 여부와 무관하게 attribute 를 세팅한다)
        CsrfToken attribute = (CsrfToken) secondRequest.getAttribute(CsrfToken.class.getName());
        assertNotNull(attribute, "Cookie 가 이미 있어도 request attribute 에 토큰이 설정되어야 한다");
        assertEquals(token, attribute.getToken(), "attribute 토큰은 Cookie 값과 동일해야 한다");
    }

    @Test
    @DisplayName("POST - Cookie 는 있지만 Header 불일치 → 403 거부")
    void post_withMismatchedHeader_denied() throws Exception {
        CsrfFilter filter = buildFilter();

        // Given — Cookie 는 유효하지만 Header 는 다른 값
        MockHttpServletResponse issueResponse = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/auth/terms"),
                issueResponse, new MockFilterChain());
        String token = issueResponse.getCookie("XSRF-TOKEN").getValue();

        MockHttpServletRequest postRequest = new MockHttpServletRequest("POST", "/api/v1/wallets/charge");
        postRequest.setCookies(new Cookie("XSRF-TOKEN", token));
        postRequest.addHeader("X-XSRF-TOKEN", "forged-token");
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // When
        filter.doFilter(postRequest, postResponse, chain);

        // Then — 위조된 Header 는 거부된다
        assertEquals(403, postResponse.getStatus());
        assertTrue(chain.getRequest() == null, "불일치 Header 는 거부되어야 한다");
    }
}
