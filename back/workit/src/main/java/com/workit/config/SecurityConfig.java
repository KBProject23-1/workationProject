package com.workit.config;

import com.workit.domain.auth.util.JwtTokenProvider;
import com.workit.security.JwtAuthenticationFilter;
import com.workit.security.NoHandlerRequestMatcher;
import com.workit.security.RestAccessDeniedHandler;
import com.workit.security.RestAuthenticationEntryPoint;
import com.workit.security.SecurityPath;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
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
//   - STATELESS + HttpOnly Cookie 세션 정책이므로 CSRF 비활성
//   - 이 설정은 ROOT 컨텍스트(RootConfig @Import)에 등록된다.
//     DelegatingFilterProxy("springSecurityFilterChain")가 ROOT 컨텍스트에서 빈을 찾기 때문.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // STATELESS + HttpOnly Cookie — CSRF 공격면적 없음
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
                        .antMatchers("/", "/index.html", "/index.jsp").permitAll()
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
        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:5173")); // 배포 시 도메인 변경
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
