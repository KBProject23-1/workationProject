package com.workit.domain.auth.controller;

import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.request.VerifyIdentityRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.service.AuthService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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

    // 1.6 로그인 토큰 재발급 (Refresh Token → Access Token)
    // - docs: 로그인 토큰 재발급 (POST /api/v1/auth/refresh)
    // - 비로그인 공개 API: Access Token 만료 시 Vue3 Axios Interceptor 가 호출
    // - Refresh Token 은 HttpOnly Cookie(refreshToken)에서만 받는다 (Body 없음)
    // - Service 에서 검증/회원 상태 확인/Redis hash 비교(재사용 감지)/재발급(Rotation)을 수행하고,
    //   Controller 는 Rotation 으로 갱신된 신규 Refresh Token 을 HttpOnly Cookie 로 다시 구워주는 HTTP 처리만 담당한다
    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<RefreshTokenResponseDTO>> refreshPost(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse servletResponse) {

        RefreshTokenResponseDTO result = authService.refreshAccessToken(refreshToken);

        // Rotation 으로 갱신된 신규 Refresh Token Cookie (login 과 동일한 속성)
        // - Max-Age 는 Refresh Token 만료와 동일(초) — jwt.refresh-token-expiration 기준
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, result.getRefreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(result.getRefreshTokenMaxAgeSeconds())
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return GlobalResponseFactory.success(result, "액세스 토큰이 성공적으로 재발급되었습니다.");
    }

    // 1.7 로그아웃
    // - docs: 로그아웃 (POST /api/v1/auth/logout)
    // - 비로그인 공개 API: Refresh Token 이 HttpOnly Cookie(refreshToken)에 있는 상태에서 호출
    // - Service 에서 Refresh Token 검증/Redis hash 비교/세션 삭제를 수행하고,
    //   Controller 는 Cookie 를 즉시 만료(Max-Age=0)시키는 HTTP 처리만 담당한다
    //   (JWT 검증/Redis 접근/Token 삭제 로직은 Controller 금지 — 전부 Service 책임)
    // - 실패 시(쿠키 누락/위변조/만료/Redis 부재) INVALID_REFRESH_TOKEN(401) — 원인 비노출
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logoutPost(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse servletResponse) {

        authService.logout(refreshToken);

        // Refresh Token Cookie 즉시 만료 (docs: refreshToken=; Max-Age=0; HttpOnly; Path=/; SameSite=None; Secure)
        // - Max-Age=0 으로 브라우저가 즉시 삭제 — 기존 Cookie 이름/HttpOnly/Secure/Path/SameSite 속성 유지
        ResponseCookie expiredCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(0)
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, expiredCookie.toString());

        return GlobalResponseFactory.success(null, "성공적으로 로그아웃되었습니다.");
    }
}
