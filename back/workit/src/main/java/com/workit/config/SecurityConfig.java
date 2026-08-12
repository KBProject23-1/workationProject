package com.workit.config;

import com.workit.domain.auth.util.JwtTokenProvider;
import com.workit.security.JwtAuthenticationFilter;
import com.workit.security.NoHandlerRequestMatcher;
import com.workit.security.RestAccessDeniedHandler;
import com.workit.security.RestAuthenticationEntryPoint;
import com.workit.security.SecurityPath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

// Spring Security 인증 계층 설정 (knowledge.md 기준)
//
// 담당:
//   - 요청 인증 검증 (JWT 필터 체인 게이트)
//   - SecurityContext 관리
//   - 권한 처리 기반 제공 (현재 ROLE_USER 단일 — @EnableMethodSecurity 미사용)
//   - 인증 실패 응답 처리 (CommonResponse 포맷 유지)
//
// 담당하지 않음:
//   - 로그인/회원가입/Refresh Token 발급/JWT 생성 — 기존 AuthService 그대로 유지
//
// 주의사항:
//   - WebSecurityConfigurerAdapter 사용 금지 — SecurityFilterChain 빈 기반 (5.7 표준)
//   - STATELESS 세션 정책이지만 HttpOnly Cookie 기반 인증이므로 CSRF 방어를 적용한다
//     (knowledge.md CSRF 정책: XSRF-TOKEN Cookie + X-XSRF-TOKEN Header, 상태 변경 메서드만 검증)
//   - 이 설정은 ROOT 컨텍스트(RootConfig @Import)에 등록된다.
//     DelegatingFilterProxy("springSecurityFilterChain")가 ROOT 컨텍스트에서 빈을 찾기 때문.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /** CSRF Token Cookie(XSRF-TOKEN) Secure 속성 — docs: 운영 Secure, 로컬 http 개발 false (jwt.refresh-cookie-secure 와 동일 패턴) */
    private final boolean csrfCookieSecure;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider,
                          @Value("${jwt.csrf-cookie-secure:true}") boolean csrfCookieSecure) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.csrfCookieSecure = csrfCookieSecure;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 방어 (Cookie 기반 인증 필수)
                // - CookieCsrfTokenRepository: XSRF-TOKEN Cookie (HttpOnly=false — JS 가 읽어야 하므로)
                //   + X-XSRF-TOKEN Request Header 로 토큰 검증 (knowledge.md CSRF 정책)
                // - Spring Security 5.7 기본 동작: Cookie 에 토큰이 없으면 응답마다 XSRF-TOKEN Cookie 를
                //   자동 발급하고, 상태 변경 요청(POST/PUT/PATCH/DELETE)만 헤더 토큰과 비교한다
                //   (GET/HEAD/OPTIONS 는 검증 대상에서 제외 — DEFAULT_CSRF_MATCHER)
                // - 5.7 의 CsrfFilter 는 Cookie 원문과 Header 를 직접 비교하므로 별도 핸들러 불필요
                //   (XorCsrfTokenRequestAttributeHandler 는 5.8+ 전용 — SPA 원문 전송과 호환)
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository()))
                .cors(Customizer.withDefaults()) // corsConfigurationSource() 빈 참조 (preflight 401 방지)
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 미사용
                .and()
                .exceptionHandling()
                    .authenticationEntryPoint(restAuthenticationEntryPoint())
                    .accessDeniedHandler(restAccessDeniedHandler())
                .and()
                .authorizeHttpRequests(authorize -> authorize
                        // 공개 경로 — 인증 불필요
                        .antMatchers("/", "/index.html", "/index.jsp", "/uploads/**").permitAll()
                        .antMatchers("/api/v1/recommendations/offices/**", "/api/v1/recommendations/activities/**").authenticated()
                        .antMatchers("/api/v1/bookmarks/**", "/api/v1/surveys/**").authenticated()
                        // 로그인 사용자 전용 인증 API(/api/v1/auth/me/**) — 공개 예외보다 먼저 평가되어 인증 필수
                        // (예: PIN 최초 설정 — JWT 없이 접근 시 401, SecurityPath 와 동일 경로 정의 공유)
                        .antMatchers(SecurityPath.AUTHENTICATED_AUTH_PATTERN).authenticated()
                        // 공개 인증 API — 필터(SecurityPath)와 동일 경로 정의 공유
                        .antMatchers(SecurityPath.PUBLIC_AUTH_PATTERN).permitAll()
                        // 알 수 없는 경로(핸들러 없음)는 인증 없이 통과 → MVC에서 404 처리
                        // (미인증 상태에서도 알 수 없는 경로는 401 대신 404 유지)
                        .requestMatchers(noHandlerRequestMatcher()).permitAll()
                        // 그 외 모든 경로는 인증 필수 (knowledge.md: 보호 API)
                        .anyRequest().authenticated())
                // JWT 검증 필터 — UsernamePasswordAuthenticationFilter 앞에 삽입
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CSRF Token 저장소 — XSRF-TOKEN Cookie 기반 (knowledge.md CSRF 정책)
     *
     * Cookie 스펙:
     *   - Cookie Name: XSRF-TOKEN (기본값) / Request Header: X-XSRF-TOKEN (기본값)
     *   - HttpOnly=false: 브라우저 JS 가 Cookie 를 읽어 Header 로 전송해야 한다 (docs)
     *   - Path=/: Access/Refresh Token Cookie 와 동일하게 전체 경로 적용
     *   - Secure: 운영(true) / 로컬 http 개발(false) — jwt.csrf-cookie-secure
     *   - SameSite: spring-security-web 5.7.11 의 CookieCsrfTokenRepository 는
     *     SameSite 속성을 지원하지 않아 브라우저 기본값(Lax)으로 발급된다 (docs 의 Strict 는 미적용)
     */
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setSecure(csrfCookieSecure);
        return repository;
    }

    /** Access Token 검증 후 SecurityContext 를 설정하는 필터 */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider);
    }

    /** 실제 핸들러가 없는 경로(알 수 없는 경로) 판별 — 미인증 요청도 404 로 통과시키기 위함 */
    @Bean
    public NoHandlerRequestMatcher noHandlerRequestMatcher() {
        return new NoHandlerRequestMatcher();
    }

    /** 인증 없이 보호 API 접근 시 401 (CommonResponse JSON) */
    @Bean
    public RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new RestAuthenticationEntryPoint();
    }

    /** 권한 부족 시 403 (CommonResponse JSON) */
    @Bean
    public RestAccessDeniedHandler restAccessDeniedHandler() {
        return new RestAccessDeniedHandler();
    }

    /**
     * Security 필터 체인용 CORS 설정
     * - 기존 ServletConfig 의 MVC CORS(/api/**, localhost:5173)와 동일 정책을 필터 레벨에 반영
     * - 필터 체인이 MVC 보다 먼저 동작하므로 여기서 preflight(OPTIONS) 를 처리해야 한다
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173", "http://localhost:4173")); // 배포 시 도메인 변경
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // CSRF Header(X-XSRF-TOKEN) 명시 허용 — allowCredentials(true) 와 함께 브라우저 preflight 통과를 보장
        // (와일드카드 "*" 만으로는 credentials 모드에서 비표준 헤더가 차단될 수 있어 명시한다)
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "X-XSRF-TOKEN", "*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
