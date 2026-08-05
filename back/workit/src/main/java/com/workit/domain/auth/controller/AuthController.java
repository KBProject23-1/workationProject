package com.workit.domain.auth.controller;

import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.request.VerifyIdentityRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.service.AuthService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

// 인증 도메인 컨트롤러
// 약관 조회/로그인/회원가입은 비로그인 접근 가능한 공개 API
@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    /** Refresh Token Cookie 명 (docs: refreshToken) — 재발급/로그아웃 API 와 이름 통일 */
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;

    /** Refresh Token Cookie Secure 속성 (docs: 운영에서는 Secure) */
    private final boolean refreshCookieSecure;

    /** Refresh Token Cookie SameSite 속성 (과제 스펙: SameSite=Lax) */
    private final String refreshCookieSameSite;

    public AuthController(AuthService authService,
                          @Value("${jwt.refresh-cookie-secure:true}") boolean refreshCookieSecure,
                          @Value("${jwt.refresh-cookie-samesite:Lax}") String refreshCookieSameSite) {
        this.authService = authService;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
    }

    // 1.1 필수/선택 약관 목록 조회
    @GetMapping("/terms")
    public ResponseEntity<CommonResponse<TermsListResponseDTO>> termsListGet() {

        return GlobalResponseFactory.success(
                authService.getTermsList(), "약관 목록 조회가 완료되었습니다.");
    }

    // 1.2 회원가입 이메일 중복 확인
    // - docs: 이메일 중복 확인 (GET /api/v1/auth/signup/check-email?email=...)
    // - 비로그인 공개 API: 회원가입 화면에서 입력한 이메일의 사용 가능 여부를 실시간으로 조회
    // - 중복 여부는 200 SUCCESS + data.available 로 반환 (available=false 시 프론트에서 가입 진행 차단)
    // - required=false: email 누락 시 Service Layer 에서 AuthErrorCode.INVALID_EMAIL_FORMAT 로 처리
    @GetMapping("/signup/check-email")
    public ResponseEntity<CommonResponse<EmailAvailabilityResponseDTO>> checkEmailGet(
            @RequestParam(value = "email", required = false) String email) {

        EmailAvailabilityResponseDTO result = authService.checkEmailAvailability(email);

        return GlobalResponseFactory.success(result,
                result.isAvailable() ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다.");
    }

    // 1.3 PASS 본인인증 검증 및 회원 중복 체크 (회원가입 1단계)
    // - docs: 본인인증 검증 및 회원 중복 체크 (POST /api/v1/auth/signup/verify-identity)
    // - 비로그인 공개 API: 회원가입 화면에서 PASS 인증 완료 후 호출
    @PostMapping("/signup/verify-identity")
    public ResponseEntity<CommonResponse<IdentityVerificationResponseDTO>> verifyIdentityPost(
            @RequestBody VerifyIdentityRequestDTO request) {

        return GlobalResponseFactory.success(
                authService.verifyIdentity(request.getIdentityVerificationId()),
                "본인인증 성공. 가입을 진행합니다.");
    }

    // 1.4 최종 회원가입 완료 (회원가입 2단계 — DB 최종 저장)
    // - docs: 최종 회원가입 완료(DB 최종 저장) (POST /api/v1/auth/signup)
    // - 비로그인 공개 API: 본인인증(verify-identity)과 이메일 중복 확인(check-email) 완료 후 호출
    // - Controller 에는 비즈니스 로직 없음 — Service 에서 JWT 검증/Redis 조회/중복 검증/DB 저장 수행
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<Void>> signupPost(
            @RequestBody SignupRequestDTO request) {

        authService.signup(request);

        return GlobalResponseFactory.success(null, "회원가입이 완료되었습니다.");
    }

    // 1.5 통합 로그인 (PASSWORD / PIN)
    // - docs: 로그인 (POST /api/v1/auth/login)
    // - 비로그인 공개 API: 이메일/휴대폰 + 비밀번호(PASSWORD) 또는 PIN + deviceId(PIN) 로 로그인
    // - 인증/토큰 발급/Redis 저장은 Service 에서 수행하고, Controller 는
    //   Refresh Token 을 HttpOnly Cookie 로 내려주는 HTTP 처리만 담당한다
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponseDTO>> loginPost(
            @RequestBody LoginRequestDTO request,
            HttpServletResponse servletResponse) {

        LoginResponseDTO result = authService.login(request);

        // Refresh Token Cookie (docs: refreshToken=...; Max-Age=...; HttpOnly; Path=/; SameSite=None; Secure)
        // - refreshToken 은 JSON 본문에 포함하지 않고 HttpOnly Cookie 로만 전달 (XSS 탈취 방지)
        // - Max-Age 는 Refresh Token 만료와 동일(초) — jwt.refresh-token-expiration 기준
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, result.getRefreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(result.getRefreshTokenMaxAgeSeconds())
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return GlobalResponseFactory.success(result, "로그인에 성공했습니다.");
    }
}
