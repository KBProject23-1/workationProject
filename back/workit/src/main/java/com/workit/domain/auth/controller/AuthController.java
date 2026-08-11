package com.workit.domain.auth.controller;

import com.workit.domain.auth.dto.request.FindIdRequestDTO;
import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.PasswordResetRequestDTO;
import com.workit.domain.auth.dto.request.PasswordVerifyRequestDTO;
import com.workit.domain.auth.dto.request.PinResetRequestDTO;
import com.workit.domain.auth.dto.request.PinSetupRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.request.VerifyIdentityRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.FindIdResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.PasswordVerifyResponseDTO;
import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.service.AuthService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import com.workit.security.JwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    // Access Token Cookie 명은 JwtAuthenticationFilter 가 읽는 상수와 동일해야 한다 (공용 상수 공유)
    // - 회원가입 자동 로그인 시 발급 — HttpOnly Secure Cookie
    private static final String ACCESS_TOKEN_COOKIE_NAME = JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE_NAME;

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

    // 1.4 최종 회원가입 완료 (회원가입 2단계 — DB 최종 저장 + 자동 로그인)
    // - docs: 최종 회원가입 완료(DB 최종 저장) (POST /api/v1/auth/signup)
    // - 비로그인 공개 API: 본인인증(verify-identity)과 이메일 중복 확인(check-email) 완료 후 호출
    // - Controller 에는 비즈니스 로직 없음 — Service 에서 JWT 검증/Redis 조회/중복 검증/DB 저장/토큰 발급 수행
    // - 자동 로그인(knowledge.md Signup Flow): Service 가 발급한 Access/Refresh Token 을
    //   HttpOnly Cookie 로 내려준다 — 프론트는 별도 로그인 API 를 호출하지 않는다
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<LoginResponseDTO>> signupPost(
            @RequestBody SignupRequestDTO request,
            HttpServletResponse servletResponse) {

        LoginResponseDTO result = authService.signup(request);

        // Access Token Cookie (knowledge.md: ACCESS_TOKEN, HttpOnly Secure Cookie)
        // - 회원가입 직후 자동 로그인 상태 유지 — 이후 보호 API 호출 시 이 Cookie 로 인증
        ResponseCookie accessCookie = ResponseCookie.from(
                        ACCESS_TOKEN_COOKIE_NAME, result.getTokenInfo().getAccessToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(result.getTokenInfo().getAccessTokenExpiresIn())
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // Refresh Token Cookie (login 과 동일 속성 — refreshToken 은 JSON 본문에 포함하지 않는다)
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, result.getRefreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path("/")
                .maxAge(result.getRefreshTokenMaxAgeSeconds())
                .build();
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return GlobalResponseFactory.success(result, "회원가입이 완료되었습니다.");
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

    // 1.8 아이디 찾기 (PASS 본인인증 기반)
    // - docs: 아이디 찾기 (POST /api/v1/auth/find-id)
    // - 비로그인 공개 API: 아이디를 분실한 유저가 PASS 본인인증을 완료한 뒤 호출
    // - Service 에서 Provider 검증/CI hash 조회/이메일 복호화·마스킹/가입일 포맷을 수행하고,
    //   Controller 는 요청 수신과 CommonResponse 반환만 담당한다 (DB 조회/복호화 금지)
    // - 인증 실패: INVALID_VERIFICATION_ID(400), 가입 회원 없음: USER_NOT_FOUND(404)
    @PostMapping("/find-id")
    public ResponseEntity<CommonResponse<FindIdResponseDTO>> findIdPost(
            @RequestBody FindIdRequestDTO request) {

        return GlobalResponseFactory.success(
                authService.findId(request.getIdentityVerificationId()),
                "가입된 이메일을 찾았습니다.");
    }

    // 1.9 비밀번호 재설정 1단계 - 본인 확인 및 인증 토큰 발급
    // - docs: 비밀번호 재설정 - 본인 확인 및 인증 토큰 발급 (POST /api/v1/auth/password/verify)
    // - 비로그인 공개 API: 비밀번호를 잃어버린 유저가 PASS 본인인증을 완료한 뒤 호출
    // - Service 에서 회원 조회/Provider 검증/CI 대조/토큰 발급·Redis 저장을 수행하고,
    //   Controller 는 요청 수신과 CommonResponse 반환만 담당한다 (DB 조회/Redis 접근 금지)
    // - 회원 없음: USER_NOT_FOUND(404), CI 불일치: VERIFICATION_FAILED(400)
    @PostMapping("/password/verify")
    public ResponseEntity<CommonResponse<PasswordVerifyResponseDTO>> passwordVerifyPost(
            @RequestBody PasswordVerifyRequestDTO request) {

        return GlobalResponseFactory.success(
                authService.verifyPasswordReset(request),
                "본인 확인이 완료되었습니다. 5분 이내에 비밀번호를 재설정해 주세요.");
    }

    // 1.10 비밀번호 재설정 2단계 - 비밀번호 변경
    // - docs: 비밀번호 변경 (PATCH /api/v1/auth/password/reset)
    // - 비로그인 공개 API: 1단계에서 발급받은 passwordResetToken(5분 유효)으로 비밀번호를 변경한다
    // - Service 에서 Redis 토큰 검증/비밀번호 정책 검증/BCrypt 암호화/DB 갱신/토큰 폐기를 수행하고,
    //   Controller 는 요청 수신과 CommonResponse 반환만 담당한다 (Redis 접근/암호화 금지)
    // - 토큰 만료·무효: RESET_TIMEOUT_OR_INVALID_TOKEN(400), 약한 비밀번호: WEAK_PASSWORD(422)
    @PatchMapping("/password/reset")
    public ResponseEntity<CommonResponse<Void>> passwordResetPatch(
            @RequestBody PasswordResetRequestDTO request) {

        authService.resetPassword(request);

        return GlobalResponseFactory.success(
                null, "비밀번호가 성공적으로 변경되었습니다. 새로운 비밀번호로 로그인해 주세요.");
    }

    // 1.11 PIN 번호 최초 설정 (로그인 사용자 전용)
    // - docs: PIN 번호 설정 (POST /api/v1/users/me/pin-number → 본 프로젝트 경로: /api/v1/auth/me/pin)
    // - 로그인 사용자 전용 API: JWT 인증 필터 + @CurrentUser 로 userId 를 주입받는다
    //   (인증 없이 접근하면 AUTH_TOKEN_NOT_FOUND 401 — CurrentUserArgumentResolver)
    // - Service 에서 회원 확인/기존 PIN 등록 여부/PIN 형식 검증/BCrypt 암호화/DB 저장을 수행하고,
    //   Controller 는 요청 수신과 CommonResponse 반환만 담당한다 (암호화/DB 접근 금지)
    // - PIN 등록됨: PIN_ALREADY_EXISTS(409), 형식 오류: INVALID_PIN_FORMAT(400)
    @PostMapping("/me/pin")
    public ResponseEntity<CommonResponse<Void>> pinSetupPost(
            @CurrentUser Long userId,
            @RequestBody PinSetupRequestDTO request) {

        authService.setupPin(userId, request);

        return GlobalResponseFactory.success(null, "핀번호가 성공적으로 설정되었습니다.");
    }

    // 1.12 보안 PIN 번호 재설정 (로그인 사용자 전용)
    // - docs: PIN 번호 변경 (PATCH /api/v1/auth/me/pin/reset)
    // - 로그인 사용자 전용 API: JWT 인증 필터 + @CurrentUser 로 userId 를 주입받는다
    //   (인증 없이 접근하면 AUTH_TOKEN_NOT_FOUND 401 — CurrentUserArgumentResolver)
    // - Service 에서 회원 확인/PASS 재인증 검증/CI 대조/PIN 형식 검증/BCrypt 암호화/DB 갱신을 수행하고,
    //   Controller 는 요청 수신과 CommonResponse 반환만 담당한다 (인증/암호화/DB 접근 금지)
    // - 본인확인 실패: VERIFICATION_FAILED(400), 형식 오류: INVALID_PIN_FORMAT(400)
    @PatchMapping("/me/pin/reset")
    public ResponseEntity<CommonResponse<Void>> pinResetPatch(
            @CurrentUser Long userId,
            @RequestBody PinResetRequestDTO request) {

        authService.resetPin(userId, request);

        return GlobalResponseFactory.success(null, "보안 PIN 번호가 성공적으로 변경되었습니다.");
    }
}
