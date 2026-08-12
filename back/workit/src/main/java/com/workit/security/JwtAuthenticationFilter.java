package com.workit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.util.JwtTokenProvider;
import com.workit.global.dto.CommonResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// 요청마다 Access Token(JWT)을 검증해 SecurityContext 를 설정하는 필터
//
// 책임 (knowledge.md — Spring Security 는 로그인/회원가입/토큰 발급을 담당하지 않음):
//   - HttpOnly accessToken Cookie 에서 Access Token 추출 (Cookie 기반 인증 전용)
//   - JwtTokenProvider.parseAccessToken() 검증 (서명 + 만료 + tokenType==ACCESS)
//   - 검증 성공 → WorkitPrincipal(userId, role) 을 SecurityContext 에 저장
//   - 검증 실패 → CommonResponse JSON 으로 즉시 응답 (필터 단계라 @RestControllerAdvice 미동작)
//
// 보안 요구사항:
//   - Access Token 은 accessToken HttpOnly Cookie 로만 전달된다 — Authorization: Bearer 헤더 미사용
//   - JWT 원문/개인정보 로그 출력 금지 — 로그에는 errorCode 만 기록
//   - Refresh Token 은 API 인증에 사용 불가 (parseAccessToken 이 tokenType 검증)
//
// 공개 경로(/api/v1/auth/**) 예외:
//   - 로그인/재발급 등 인증 없이 호출되는 공개 API 는 토큰 검증을 수행하지 않고 통과시킨다.
//   - 클라이언트가 공개 API 호출에도 이전(만료된) Access Token 을 실어 보내는 경우가 있어,
//     이를 거부하면 로그인/재발급 흐름이 깨질 수 있기 때문이다.
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String CLAIM_ROLE = "role";

    /**
     * Access Token Cookie 명 (Cookie 기반 인증)
     * - knowledge.md Cookie Security: accessToken / HttpOnly / Path=/
     * - 로그인/재발급 API 가 이 이름으로 발급한 Cookie 만 인증에 사용한다.
     * - AuthController 의 ACCESS_TOKEN_COOKIE_NAME 과 동일해야 한다 (이름 어긋나면 인증 불가)
     */
    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Access Token 추출 — accessToken HttpOnly Cookie 에서만 추출 (Cookie 기반 인증)
        String token = extractAccessToken(request);

        // 2. 토큰 없음 → 인증 없이 진행 (보호 경로는 이후 진입점이 401 처리)
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. 공개 인증 API → 토큰이 있어도 검증하지 않고 통과 (로그인/재발급 흐름 보호)
        if (isPublicPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (token.isEmpty()) {
            writeError(response, AuthErrorCode.INVALID_TOKEN);
            return;
        }

        // 3. JWT 검증 (서명/만료/tokenType) 후 SecurityContext 에 인증 객체 저장
        try {
            Claims claims = jwtTokenProvider.parseAccessToken(token);
            Long userId = extractUserId(claims);
            String role = claims.get(CLAIM_ROLE, String.class);
            WorkitPrincipal principal = new WorkitPrincipal(userId, role);
            SecurityContextHolder.getContext().setAuthentication(principal);
        } catch (ExpiredJwtException e) {
            log.warn("API 인증 실패 - EXPIRED_TOKEN");
            writeError(response, AuthErrorCode.EXPIRED_TOKEN);
            return;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("API 인증 실패 - INVALID_TOKEN");
            writeError(response, AuthErrorCode.INVALID_TOKEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Access Token 추출
     * - accessToken HttpOnly Cookie 에서만 추출한다 (Authorization: Bearer 헤더 미사용)
     * - Cookie 가 없으면 null 반환 → 인증 없이 진행
     */
    private String extractAccessToken(HttpServletRequest request) {
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (javax.servlet.http.Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    String value = cookie.getValue();
                    return value == null ? null : value.trim();
                }
            }
        }
        return null;
    }

    /** sub(userId) 추출 — 누락/빈 값/비숫자 형식은 위변조로 간주 */
    private Long extractUserId(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.trim().isEmpty()) {
            throw new JwtException("JWT Payload에 userId(sub)가 없습니다.");
        }
        return Long.valueOf(subject); // NumberFormatException → IllegalArgumentException 계열로 포착
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        // 컨텍스트 패스 제거 — getRequestURI() 는 컨텍스트 패스를 포함하므로
        // (예: /workit/api/v1/auth/refresh) WAR 배포(비루트 컨텍스트)에서 공개 경로 판별이
        // 실패해 만료된 Access Token 이 재발급/로그인 흐름을 막지 않도록 컨텍스트 기준 경로로 정규화한다.
        // - 루트 컨텍스트는 getContextPath() 가 "" 를 반환하므로 제거할 것이 없다.
        // - "/" 컨텍스트는 이론상 가능하나 서블릿 명세상 발생하지 않으며, 안전하게 제외한다.
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)
                && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        // 로그인 사용자 전용 경로(/api/v1/auth/me/**)는 공개 예외에서 제외 — JWT 검증 필수
        // (SecurityConfig 의 authenticated 규칙과 동일한 SecurityPath 정의를 공유한다)
        if (uri.startsWith(SecurityPath.AUTHENTICATED_AUTH_PREFIX)) {
            return false;
        }
        return uri.startsWith(SecurityPath.PUBLIC_AUTH_PREFIX);
    }

    /** 필터 단계 응답 — CommonResponse 형상 유지 (status/errorCode/message) */
    private void writeError(HttpServletResponse response, AuthErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                CommonResponse.error(errorCode.getErrorCode(), errorCode.getMessage()));
    }
}
