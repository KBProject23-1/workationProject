package com.workit.domain.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.PasswordResetRequestDTO;
import com.workit.domain.auth.dto.request.PasswordVerifyRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.FindIdResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.PasswordVerifyResponseDTO;
import com.workit.domain.auth.dto.response.RefreshTokenResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.service.AuthService;
import com.workit.exception.BusinessException;
import com.workit.exception.CommonExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AuthController (본인인증 검증 API) 테스트
// - AuthService 를 Mockito @Mock 으로 주입한다 (Controller 계층 검증에 집중)
// - @InjectMocks 를 사용하지 않는 이유: AuthController 생성자에 primitive @Value(boolean/String)
//   파라미터가 있어 Mock 주입이 불가하므로 setUp 에서 직접 생성한다
// - json-path 의존성 없이 Jackson ObjectMapper 로 응답 JSON 을 검증한다
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // refreshCookieSecure=true / sameSite=Lax — 운영(HTTPS) 쿠키 스펙 그대로 검증
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService, true, "Lax"))
                .setControllerAdvice(new CommonExceptionAdvice())
                .build();
    }

    private JsonNode parse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(body);
    }

    // ---------- Stub 헬퍼 (Mockito when / doThrow) ----------

    /** 본인인증 성공 Stub — Service 검증 결과와 무관하게 Controller 전달값만 검증 */
    private void stubVerifyIdentitySuccess() {
        when(authService.verifyIdentity(any()))
                .thenReturn(IdentityVerificationResponseDTO.of("encrypted-identity-token", "홍길동"));
    }

    /** 본인인증 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubVerifyIdentityError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(authService).verifyIdentity(any());
    }

    /** 이메일 중복 확인 Stub — available 여부 지정 */
    private void stubCheckEmail(boolean available) {
        when(authService.checkEmailAvailability(any()))
                .thenReturn(EmailAvailabilityResponseDTO.of(available));
    }

    /** 이메일 중복 확인 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubCheckEmailError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(authService).checkEmailAvailability(any());
    }

    /** 회원가입 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubSignupError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(authService).signup(any(SignupRequestDTO.class));
    }

    /** 로그인 성공 Stub — Service 가 userId/name/token_info/refreshToken 을 반환한다 */
    private void stubLoginSuccess() {
        when(authService.login(any(LoginRequestDTO.class)))
                .thenReturn(LoginResponseDTO.builder()
                        .userId(501L)
                        .name("홍길동")
                        .tokenInfo(LoginResponseDTO.TokenInfo.of("Bearer", "access-token-jwt", 900))
                        .refreshToken("refresh-token-jwt")
                        .refreshTokenMaxAgeSeconds(1209600)
                        .build());
    }

    /** 로그인 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubLoginError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(authService).login(any(LoginRequestDTO.class));
    }

    /** 재발급 성공 Stub — Service 가 신규 token_info/refreshToken 을 반환한다 */
    private void stubRefreshSuccess() {
        when(authService.refreshAccessToken(any()))
                .thenReturn(RefreshTokenResponseDTO.builder()
                        .tokenInfo(LoginResponseDTO.TokenInfo.of("Bearer", "new-access-token-jwt", 900))
                        .refreshToken("new-refresh-token-jwt")
                        .refreshTokenMaxAgeSeconds(1209600)
                        .build());
    }

    /** 재발급 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubRefreshError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(authService).refreshAccessToken(any());
    }

    /** 로그아웃 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubLogoutError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(authService).logout(any());
    }

    /** 아이디 찾기 성공 Stub — Service 가 마스킹 이메일/가입일을 반환한다 */
    private void stubFindIdSuccess() {
        when(authService.findId(any()))
                .thenReturn(FindIdResponseDTO.of("user****@example.com", "2026-07-24"));
    }

    /** 아이디 찾기 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubFindIdError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(authService).findId(any());
    }

    /** 비밀번호 재설정 1단계 성공 Stub — Service 가 passwordResetToken 을 반환한다 */
    private void stubPasswordVerifySuccess() {
        when(authService.verifyPasswordReset(any(PasswordVerifyRequestDTO.class)))
                .thenReturn(PasswordVerifyResponseDTO.of("9f1d7c3a-82ab-4d32-a5dd-000000000000"));
    }

    /** 비밀번호 재설정 1단계 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubPasswordVerifyError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(authService).verifyPasswordReset(any(PasswordVerifyRequestDTO.class));
    }

    /** 비밀번호 재설정 2단계 실패 Stub — Service 가 지정 에러를 던진다 */
    private void stubPasswordResetError(AuthErrorCode errorCode) {
        doThrow(new BusinessException(errorCode)).when(authService).resetPassword(any(PasswordResetRequestDTO.class));
    }

    // ---------- PASS 본인인증 검증 ----------

    @Test
    @DisplayName("본인인증 검증 성공 - 200 + SUCCESS + identityToken/name 반환")
    void verifyIdentity_success() throws Exception {
        // Given — Service 는 인증 성공 결과를 반환한다
        stubVerifyIdentitySuccess();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"imp_ver_1234567890\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("본인인증 성공. 가입을 진행합니다.", json.get("message").asText());

        // 성공 응답에는 errorCode 가 없어야 한다 (CommonResponse NON_NULL)
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertEquals("encrypted-identity-token", data.get("identityToken").asText());
        assertEquals("홍길동", data.get("name").asText());

        // Service 가 인증 ID 를 그대로 전달받았는지 확인
        verify(authService).verifyIdentity("imp_ver_1234567890");
    }

    @Test
    @DisplayName("유효하지 않은 인증 ID - 400 + INVALID_VERIFICATION_ID")
    void verifyIdentity_invalidId() throws Exception {
        // Given — Service 는 유효하지 않은 인증 ID 를 거부한다
        stubVerifyIdentityError(AuthErrorCode.INVALID_VERIFICATION_ID);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_VERIFICATION_ID", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("빈 identityVerificationId - 400 + INVALID_VERIFICATION_ID")
    void verifyIdentity_blankId() throws Exception {
        // Given — Service 는 빈 인증 ID 를 거부한다
        stubVerifyIdentityError(AuthErrorCode.INVALID_VERIFICATION_ID);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_VERIFICATION_ID", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("identityVerificationId 누락 - 400 + INVALID_VERIFICATION_ID")
    void verifyIdentity_missingId() throws Exception {
        // Given — Service 는 누락된 인증 ID 를 거부한다
        stubVerifyIdentityError(AuthErrorCode.INVALID_VERIFICATION_ID);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_VERIFICATION_ID", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("중복 가입 - 409 + DUPLICATE_USER")
    void verifyIdentity_duplicateUser() throws Exception {
        // Given — Service 는 중복 가입을 거부한다
        stubVerifyIdentityError(AuthErrorCode.DUPLICATE_USER);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"imp_ver_9999999999\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_USER", json.get("errorCode").asText());
        assertEquals("이미 가입된 회원입니다. 로그인을 진행해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("잘못된 JSON 본문 - 400 (공통 형식 오류)")
    void verifyIdentity_malformedBody() throws Exception {
        // When — JSON 파싱 실패는 Service 호출 전에 차단된다
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertFalse(json.get("errorCode").isNull());
    }

    // ---------- 회원가입 이메일 중복 확인 ----------

    @Test
    @DisplayName("이메일 중복 확인 - 사용 가능한 이메일 (200 + SUCCESS + available=true)")
    void checkEmail_available() throws Exception {
        // Given — Service 는 사용 가능한 이메일로 판단한다
        stubCheckEmail(true);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", "new@example.com"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("사용 가능한 이메일입니다.", json.get("message").asText());

        // 성공 응답에는 errorCode 가 없어야 한다 (CommonResponse NON_NULL)
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertTrue(data.get("available").asBoolean());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 이미 사용 중인 이메일 (200 + SUCCESS + available=false)")
    void checkEmail_duplicate() throws Exception {
        // Given — Service 는 중복 이메일로 판단한다
        stubCheckEmail(false);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", "used@example.com"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("이미 사용 중인 이메일입니다.", json.get("message").asText());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertFalse(data.get("available").asBoolean());
    }

    @Test
    @DisplayName("이메일 중복 확인 - email 파라미터 누락 (400 + INVALID_EMAIL_FORMAT)")
    void checkEmail_missingParam() throws Exception {
        // Given — Service 는 누락된 이메일을 형식 오류로 거부한다
        stubCheckEmailError(AuthErrorCode.INVALID_EMAIL_FORMAT);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_EMAIL_FORMAT", json.get("errorCode").asText());
        assertEquals("올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 잘못된 이메일 형식 (400 + INVALID_EMAIL_FORMAT)")
    void checkEmail_invalidFormat() throws Exception {
        // Given — Service 는 잘못된 형식의 이메일을 거부한다
        stubCheckEmailError(AuthErrorCode.INVALID_EMAIL_FORMAT);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", "not-an-email"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_EMAIL_FORMAT", json.get("errorCode").asText());
        assertEquals("올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 최대 길이(254자) 정상 (200 + SUCCESS + available=true)")
    void checkEmail_maxLength() throws Exception {
        // Given — Service 는 최대 길이 이메일을 사용 가능으로 판단한다
        stubCheckEmail(true);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", buildLongEmail(254)))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("사용 가능한 이메일입니다.", json.get("message").asText());
        assertTrue(json.get("data").get("available").asBoolean());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 255자 이상 이메일 (400 + INVALID_EMAIL_FORMAT)")
    void checkEmail_tooLong() throws Exception {
        // Given — Service 는 초과 길이 이메일을 형식 오류로 거부한다
        stubCheckEmailError(AuthErrorCode.INVALID_EMAIL_FORMAT);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", buildLongEmail(255)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_EMAIL_FORMAT", json.get("errorCode").asText());
        assertEquals("올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요.", json.get("message").asText());
    }

    /**
     * 지정한 전체 길이의 이메일 생성 — 로컬파트 64자 + @ + 도메인 + .com (RFC 5321 형식 유지)
     * - Java 8 호환을 위해 String.repeat 대신 반복문 사용
     */
    private String buildLongEmail(int totalLength) {
        int domainLength = totalLength - 65; // 로컬파트 64자 + @ 1자 제외
        StringBuilder sb = new StringBuilder(totalLength);
        for (int i = 0; i < 64; i++) {
            sb.append('a');
        }
        sb.append('@');
        for (int i = 0; i < domainLength - 4; i++) {
            sb.append('b');
        }
        sb.append(".com");
        return sb.toString();
    }

    // ---------- 최종 회원가입 완료 ----------

    /** 필수 약관(1, 2) 전체 동의 기본 본문 생성 */
    private String signupBody(String identityToken, String email, String nickname) {
        return signupBody(identityToken, email, nickname, ",\"agreedTermsIds\":[1,2]");
    }

    /** agreedTermsIds JSON 조각(빈 문자열이면 누락)을 지정하는 본문 생성 */
    private String signupBody(String identityToken, String email, String nickname,
                              String agreedTermsIdsJson) {
        return "{\"identityToken\":\"" + identityToken
                + "\",\"email\":\"" + email
                + "\",\"password\":\"password123!\""
                + ",\"nickname\":\"" + nickname + "\""
                + agreedTermsIdsJson + "}";
    }

    @Test
    @DisplayName("회원가입 완료 성공 - 200 + SUCCESS + 회원가입이 완료되었습니다.")
    void signup_success() throws Exception {
        // Given — Service 는 정상 가입을 허용한다 (void — 별도 Stub 불필요)

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester")))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("회원가입이 완료되었습니다.", json.get("message").asText());

        // data 는 null (CommonResponse NON_NULL 로 JSON 에서 제외될 수 있음)
        assertTrue(json.get("data") == null || json.get("data").isNull());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        // Service 가 가입 요청을 전달받았는지 확인
        verify(authService).signup(any(SignupRequestDTO.class));
    }

    @Test
    @DisplayName("회원가입 완료 - JWT 만료 (401 + EXPIRED_SIGNUP_TOKEN)")
    void signup_expiredToken() throws Exception {
        // Given — Service 는 만료된 JWT 를 거부한다
        stubSignupError(AuthErrorCode.EXPIRED_SIGNUP_TOKEN);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("expired-token", "new@example.com", "tester")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("EXPIRED_SIGNUP_TOKEN", json.get("errorCode").asText());
        assertEquals("본인인증 유효 시간이 만료되었습니다. 인증을 다시 진행해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - JWT 위변조 (400 + INVALID_SIGNUP_TOKEN)")
    void signup_tamperedToken() throws Exception {
        // Given — Service 는 위변조된 JWT 를 거부한다
        stubSignupError(AuthErrorCode.INVALID_SIGNUP_TOKEN);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("tampered-token", "new@example.com", "tester")))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_SIGNUP_TOKEN", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - Redis 임시 데이터 없음 (400 + SIGNUP_VERIFICATION_NOT_FOUND)")
    void signup_verificationNotFound() throws Exception {
        // Given — Service 는 Redis 임시 데이터가 없음을 거부한다
        stubSignupError(AuthErrorCode.SIGNUP_VERIFICATION_NOT_FOUND);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("no-redis-token", "new@example.com", "tester")))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("SIGNUP_VERIFICATION_NOT_FOUND", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - CI 중복 (409 + DUPLICATE_USER)")
    void signup_duplicateCi() throws Exception {
        // Given — Service 는 CI 중복을 거부한다
        stubSignupError(AuthErrorCode.DUPLICATE_USER);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("dup-ci-token", "new@example.com", "tester")))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_USER", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 이메일 중복 (409 + DUPLICATE_EMAIL)")
    void signup_duplicateEmail() throws Exception {
        // Given — Service 는 이메일 중복을 거부한다
        stubSignupError(AuthErrorCode.DUPLICATE_EMAIL);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "used@example.com", "tester")))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_EMAIL", json.get("errorCode").asText());
        assertEquals("이미 사용 중인 이메일입니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 닉네임 중복 (409 + DUPLICATE_NICKNAME)")
    void signup_duplicateNickname() throws Exception {
        // Given — Service 는 닉네임 중복을 거부한다
        stubSignupError(AuthErrorCode.DUPLICATE_NICKNAME);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "taken")))
                .andExpect(status().isConflict())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_NICKNAME", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 255자 이상 이메일 (400 + INVALID_EMAIL_FORMAT)")
    void signup_emailTooLong() throws Exception {
        // Given — Service 는 초과 길이 이메일을 거부한다
        stubSignupError(AuthErrorCode.INVALID_EMAIL_FORMAT);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", buildLongEmail(255), "tester")))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_EMAIL_FORMAT", json.get("errorCode").asText());
        assertEquals("올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 필수 값 누락 (400 + INVALID_SIGNUP_REQUEST)")
    void signup_missingRequired() throws Exception {
        // Given — Service 는 필수 값 누락을 거부한다
        stubSignupError(AuthErrorCode.INVALID_SIGNUP_REQUEST);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_SIGNUP_REQUEST", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - agreedTermsIds 포함 정상 요청 (200 + SUCCESS)")
    void signup_withAgreedTerms_success() throws Exception {
        // Given — Service 는 정상 가입을 허용한다 (void — 별도 Stub 불필요)

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester",
                                ",\"agreedTermsIds\":[1,2,3]")))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("회원가입이 완료되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        // Service 가 가입 요청을 전달받았는지 확인
        verify(authService).signup(any(SignupRequestDTO.class));
    }

    @Test
    @DisplayName("회원가입 완료 - agreedTermsIds 누락 (400 + MISSING_REQUIRED_TERMS)")
    void signup_missingAgreedTerms() throws Exception {
        // Given — Service 는 약관 동의 누락을 거부한다
        stubSignupError(AuthErrorCode.MISSING_REQUIRED_TERMS);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester", "")))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("MISSING_REQUIRED_TERMS", json.get("errorCode").asText());
        assertEquals("필수 약관에 모두 동의해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 존재하지 않는 약관 ID (400 + INVALID_TERM_ID)")
    void signup_invalidTermId() throws Exception {
        // Given — Service 는 존재하지 않는 약관 ID 를 거부한다
        stubSignupError(AuthErrorCode.INVALID_TERM_ID);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester",
                                ",\"agreedTermsIds\":[1,2,99]")))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_TERM_ID", json.get("errorCode").asText());
        assertEquals("존재하지 않는 약관이 포함되어 있습니다. 다시 확인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 필수 약관 누락 (400 + MISSING_REQUIRED_TERMS)")
    void signup_missingRequiredTerms() throws Exception {
        // Given — Service 는 필수 약관 누락을 거부한다
        stubSignupError(AuthErrorCode.MISSING_REQUIRED_TERMS);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester",
                                ",\"agreedTermsIds\":[1]")))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("MISSING_REQUIRED_TERMS", json.get("errorCode").asText());
        assertEquals("필수 약관에 모두 동의해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 잘못된 JSON 본문 (400 공통 형식 오류)")
    void signup_malformedBody() throws Exception {
        // When — JSON 파싱 실패는 Service 호출 전에 차단된다
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertFalse(json.get("errorCode").isNull());
    }

    // ---------- 통합 로그인 ----------

    @Test
    @DisplayName("PASSWORD 로그인 성공 - 200 + SUCCESS + userId/name/token_info + Set-Cookie(HttpOnly)")
    void login_password_success() throws Exception {
        // Given — Service 는 로그인 성공 결과를 반환한다
        stubLoginSuccess();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"loginId\":\"user@example.com\",\"password\":\"password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("로그인에 성공했습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertEquals(501, data.get("userId").asInt());
        assertEquals("홍길동", data.get("name").asText());

        // token_info 는 docs 스펙대로 snake_case 필드명을 사용한다
        JsonNode tokenInfo = data.get("token_info");
        assertNotNull(tokenInfo);
        assertEquals("Bearer", tokenInfo.get("grant_type").asText());
        assertEquals("access-token-jwt", tokenInfo.get("access_token").asText());
        assertEquals(900, tokenInfo.get("access_token_expires_in").asInt());

        // Refresh Token 은 JSON 본문에 포함되지 않는다 (HttpOnly Cookie 로만 전달)
        assertTrue(data.get("refreshToken") == null);

        // Set-Cookie (과제 스펙: refreshToken=...; Max-Age=1209600; HttpOnly; Path=/; SameSite=Lax; Secure=운영)
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=refresh-token-jwt"), "쿠키명/값: " + setCookie);
        assertTrue(setCookie.contains("HttpOnly"), "HttpOnly 속성: " + setCookie);
        assertTrue(setCookie.contains("Path=/"), "Path 속성: " + setCookie);
        assertTrue(setCookie.contains("Max-Age=1209600"), "Max-Age 속성: " + setCookie);
        assertTrue(setCookie.contains("Secure"), "Secure 속성: " + setCookie);
        assertTrue(setCookie.contains("SameSite=Lax"), "SameSite 속성: " + setCookie);
    }

    @Test
    @DisplayName("PASSWORD 휴대폰 로그인 성공 - 200 + SUCCESS + userId/name + 쿠키 발급")
    void login_phone_success() throws Exception {
        // Given — Service 는 로그인 성공 결과를 반환한다
        stubLoginSuccess();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"loginId\":\"010-1234-5678\",\"password\":\"password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals(501, json.get("data").get("userId").asInt());
        assertEquals("홍길동", json.get("data").get("name").asText());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    @DisplayName("PIN 로그인 성공 - 200 + SUCCESS + userId + 쿠키 발급")
    void login_pin_success() throws Exception {
        // Given — Service 는 로그인 성공 결과를 반환한다
        stubLoginSuccess();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PIN\",\"pinNumber\":\"123456\",\"deviceId\":\"9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals(501, json.get("data").get("userId").asInt());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    @DisplayName("잘못된 password - 401 + INVALID_CREDENTIALS")
    void login_wrongPassword() throws Exception {
        // Given — Service 는 잘못된 비밀번호를 거부한다
        stubLoginError(AuthErrorCode.INVALID_CREDENTIALS);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"loginId\":\"user@example.com\",\"password\":\"wrong!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_CREDENTIALS", json.get("errorCode").asText());
        assertEquals("인증 정보가 올바르지 않습니다. 다시 확인 후 시도해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("잘못된 PIN - 401 + INVALID_CREDENTIALS")
    void login_wrongPin() throws Exception {
        // Given — Service 는 잘못된 PIN 을 거부한다
        stubLoginError(AuthErrorCode.INVALID_CREDENTIALS);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PIN\",\"pinNumber\":\"000000\",\"deviceId\":\"device-uuid\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_CREDENTIALS", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 - 401 + INVALID_CREDENTIALS")
    void login_unknownUser() throws Exception {
        // Given — Service 는 존재하지 않는 사용자를 거부한다
        stubLoginError(AuthErrorCode.INVALID_CREDENTIALS);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"loginId\":\"unknown@example.com\",\"password\":\"password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("INVALID_CREDENTIALS", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("잘못된 loginType - 400 + INVALID_LOGIN_TYPE")
    void login_invalidLoginType() throws Exception {
        // Given — Service 는 잘못된 loginType 을 거부한다
        stubLoginError(AuthErrorCode.INVALID_LOGIN_TYPE);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"FACE_ID\",\"loginId\":\"user@example.com\",\"password\":\"password123!\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_LOGIN_TYPE", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("필수 값 누락 - 400 + INVALID_LOGIN_REQUEST")
    void login_missingRequired() throws Exception {
        // Given — Service 는 필수 값 누락을 거부한다
        stubLoginError(AuthErrorCode.INVALID_LOGIN_REQUEST);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_LOGIN_REQUEST", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("PIN 잠금 - 403 + PIN_LOCK_EXCEEDED")
    void login_pinLocked() throws Exception {
        // Given — Service 는 PIN 잠금 상태를 거부한다
        stubLoginError(AuthErrorCode.PIN_LOCK_EXCEEDED);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PIN\",\"pinNumber\":\"123456\",\"deviceId\":\"locked-device\"}"))
                .andExpect(status().isForbidden())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("PIN_LOCK_EXCEEDED", json.get("errorCode").asText());
        assertEquals("핀번호 입력 횟수가 5회 초과하여 계정이 잠겼습니다. PASS 본인인증을 통해 핀번호를 재설정해 주세요.",
                json.get("message").asText());
    }

    @Test
    @DisplayName("잘못된 JSON 본문 - 400 (공통 형식 오류)")
    void login_malformedBody() throws Exception {
        // When — JSON 파싱 실패는 Service 호출 전에 차단된다
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertFalse(json.get("errorCode").isNull());
    }

    // ---------- 로그인 토큰 재발급 ----------

    @Test
    @DisplayName("재발급 성공 - 200 + SUCCESS + token_info + 신규 Refresh Token Set-Cookie")
    void refresh_success() throws Exception {
        // Given — Service 는 재발급 성공 결과를 반환한다
        stubRefreshSuccess();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token-jwt")))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("액세스 토큰이 성공적으로 재발급되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        // docs 스펙: data.token_info.{grant_type, access_token, access_token_expires_in}
        JsonNode tokenInfo = json.get("data").get("token_info");
        assertNotNull(tokenInfo);
        assertEquals("Bearer", tokenInfo.get("grant_type").asText());
        assertEquals("new-access-token-jwt", tokenInfo.get("access_token").asText());
        assertEquals(900, tokenInfo.get("access_token_expires_in").asInt());

        // 신규 Refresh Token 은 JSON 본문에 포함되지 않는다 (HttpOnly Cookie 로만 전달 — Rotation)
        assertTrue(json.get("data").get("refreshToken") == null);

        // Rotation 으로 갱신된 Refresh Token Cookie (login 과 동일 속성)
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=new-refresh-token-jwt"), "쿠키명/값: " + setCookie);
        assertTrue(setCookie.contains("HttpOnly"), "HttpOnly 속성: " + setCookie);
        assertTrue(setCookie.contains("Path=/"), "Path 속성: " + setCookie);
        assertTrue(setCookie.contains("Max-Age=1209600"), "Max-Age 속성: " + setCookie);
        assertTrue(setCookie.contains("Secure"), "Secure 속성: " + setCookie);
        assertTrue(setCookie.contains("SameSite=Lax"), "SameSite 속성: " + setCookie);
    }

    @Test
    @DisplayName("Refresh Token 쿠키 누락 - 401 + INVALID_REFRESH_TOKEN")
    void refresh_missingCookie() throws Exception {
        // Given — Service 는 쿠키 누락을 거부한다
        stubRefreshError(AuthErrorCode.INVALID_REFRESH_TOKEN);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_REFRESH_TOKEN", json.get("errorCode").asText());
        assertEquals("세션이 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token - 401 + INVALID_REFRESH_TOKEN")
    void refresh_invalidToken() throws Exception {
        // Given — Service 는 유효하지 않은 Refresh Token 을 거부한다
        stubRefreshError(AuthErrorCode.INVALID_REFRESH_TOKEN);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "invalid-refresh")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_REFRESH_TOKEN", json.get("errorCode").asText());
        assertEquals("세션이 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요.", json.get("message").asText());
    }

    // ---------- 로그아웃 ----------

    @Test
    @DisplayName("로그아웃 성공 - 200 + SUCCESS + Refresh Token Cookie 즉시 만료(Max-Age=0)")
    void logout_success() throws Exception {
        // Given — Service 는 정상 로그아웃을 허용한다 (void — 별도 Stub 불필요)

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refreshToken", "valid-refresh-token-jwt")))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("성공적으로 로그아웃되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());
        assertTrue(json.get("data") == null || json.get("data").isNull());

        // Set-Cookie (docs: refreshToken=; Max-Age=0; HttpOnly; Path=/; SameSite=None; Secure)
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken="), "쿠키명/값: " + setCookie);
        assertTrue(setCookie.contains("Max-Age=0"), "Max-Age 속성: " + setCookie);
        assertTrue(setCookie.contains("HttpOnly"), "HttpOnly 속성: " + setCookie);
        assertTrue(setCookie.contains("Path=/"), "Path 속성: " + setCookie);
        assertTrue(setCookie.contains("Secure"), "Secure 속성: " + setCookie);
        assertTrue(setCookie.contains("SameSite=Lax"), "SameSite 속성: " + setCookie);

        // Service 가 Refresh Token 을 전달받았는지 확인
        verify(authService).logout("valid-refresh-token-jwt");
    }

    @Test
    @DisplayName("Refresh Token 쿠키 누락 - 401 + INVALID_REFRESH_TOKEN")
    void logout_missingCookie() throws Exception {
        // Given — Service 는 쿠키 누락을 거부한다
        stubLogoutError(AuthErrorCode.INVALID_REFRESH_TOKEN);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_REFRESH_TOKEN", json.get("errorCode").asText());
        assertEquals("세션이 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token - 401 + INVALID_REFRESH_TOKEN")
    void logout_invalidToken() throws Exception {
        // Given — Service 는 유효하지 않은 Refresh Token 을 거부한다
        stubLogoutError(AuthErrorCode.INVALID_REFRESH_TOKEN);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refreshToken", "invalid-refresh")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_REFRESH_TOKEN", json.get("errorCode").asText());
        assertEquals("세션이 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요.", json.get("message").asText());
    }

    // ---------- 아이디 찾기 ----------

    @Test
    @DisplayName("아이디 찾기 성공 - 200 + SUCCESS + 마스킹 이메일/가입일 반환")
    void findId_success() throws Exception {
        // Given — Service 가 마스킹 이메일과 가입일을 반환한다
        stubFindIdSuccess();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/find-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"imp_ver_9876543210\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("가입된 이메일을 찾았습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertEquals("user****@example.com", data.get("email").asText());
        assertEquals("2026-07-24", data.get("createdAt").asText());

        // Service 가 인증 ID 를 그대로 전달받았는지 확인
        verify(authService).findId("imp_ver_9876543210");
    }

    @Test
    @DisplayName("아이디 찾기 - identityVerificationId 누락 → 400 + INVALID_VERIFICATION_ID")
    void findId_missingId() throws Exception {
        // Given — Service 는 누락된 인증 ID 를 거부한다
        stubFindIdError(AuthErrorCode.INVALID_VERIFICATION_ID);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/find-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_VERIFICATION_ID", json.get("errorCode").asText());
        assertEquals("PASS 인증이 유효하지 않습니다. 다시 시도해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("아이디 찾기 - 해당 CI 로 가입된 계정 없음 → 404 + USER_NOT_FOUND")
    void findId_userNotFound() throws Exception {
        // Given — Service 는 가입 회원 없음을 거부한다
        stubFindIdError(AuthErrorCode.USER_NOT_FOUND);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/find-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"imp_ver_0000000000\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_NOT_FOUND", json.get("errorCode").asText());
        assertEquals("해당 본인인증 정보로 가입된 계정이 존재하지 않습니다.", json.get("message").asText());
    }

    // ---------- 비밀번호 재설정 1단계 (본인 확인 및 인증 토큰 발급) ----------

    @Test
    @DisplayName("비밀번호 재설정 토큰 발급 성공 - 200 + SUCCESS + passwordResetToken 반환")
    void passwordVerify_success() throws Exception {
        // Given — Service 가 passwordResetToken 을 반환한다
        stubPasswordVerifySuccess();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"user@example.com\",\"identityVerificationId\":\"imp_ver_9876543210\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("본인 확인이 완료되었습니다. 5분 이내에 비밀번호를 재설정해 주세요.",
                json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        // docs: data.passwordResetToken (UUID)
        JsonNode data = json.get("data");
        assertNotNull(data);
        assertEquals("9f1d7c3a-82ab-4d32-a5dd-000000000000", data.get("passwordResetToken").asText());

        // Service 가 요청 DTO 를 전달받았는지 확인
        verify(authService).verifyPasswordReset(any(PasswordVerifyRequestDTO.class));
    }

    @Test
    @DisplayName("비밀번호 재설정 - loginId 로 조회되는 회원 없음 → 404 + USER_NOT_FOUND")
    void passwordVerify_userNotFound() throws Exception {
        // Given — Service 는 회원 없음을 거부한다
        stubPasswordVerifyError(AuthErrorCode.USER_NOT_FOUND);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"unknown@example.com\",\"identityVerificationId\":\"imp_ver_9876543210\"}"))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("USER_NOT_FOUND", json.get("errorCode").asText());
        assertEquals("해당 본인인증 정보로 가입된 계정이 존재하지 않습니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("비밀번호 재설정 - 본인확인(CI) 불일치 → 400 + VERIFICATION_FAILED")
    void passwordVerify_verificationFailed() throws Exception {
        // Given — Service 는 CI 불일치를 거부한다
        stubPasswordVerifyError(AuthErrorCode.VERIFICATION_FAILED);

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginId\":\"user@example.com\",\"identityVerificationId\":\"imp_ver_0000000000\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("VERIFICATION_FAILED", json.get("errorCode").asText());
        assertEquals("입력하신 계정 정보와 본인인증(PASS) 정보가 일치하지 않습니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("비밀번호 재설정 - 요청 값 누락 → 400 + INVALID_PASSWORD_RESET_REQUEST")
    void passwordVerify_invalidRequest() throws Exception {
        // Given — Service 는 요청 값 누락을 거부한다
        stubPasswordVerifyError(AuthErrorCode.INVALID_PASSWORD_RESET_REQUEST);

        // When — loginId 누락
        MvcResult result = mockMvc.perform(post("/api/v1/auth/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"imp_ver_9876543210\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_PASSWORD_RESET_REQUEST", json.get("errorCode").asText());
    }

    // ---------- 비밀번호 재설정 2단계 (비밀번호 변경) ----------

    @Test
    @DisplayName("비밀번호 변경 성공 - 200 + SUCCESS + 안내 메시지 (data null)")
    void passwordReset_success() throws Exception {
        // Given — Service 는 정상 변경을 허용한다 (void — 별도 Stub 불필요)

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordResetToken\":\"9f1d7c3a-82ab-4d32-a5dd-000000000000\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("비밀번호가 성공적으로 변경되었습니다. 새로운 비밀번호로 로그인해 주세요.",
                json.get("message").asText());
        assertTrue(json.get("data") == null || json.get("data").isNull());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        // Service 가 변경 요청을 전달받았는지 확인
        verify(authService).resetPassword(any(PasswordResetRequestDTO.class));
    }

    @Test
    @DisplayName("비밀번호 변경 - 만료/무효 토큰 → 400 + RESET_TIMEOUT_OR_INVALID_TOKEN")
    void passwordReset_invalidToken() throws Exception {
        // Given — Service 는 만료/무효 토큰을 거부한다
        stubPasswordResetError(AuthErrorCode.RESET_TIMEOUT_OR_INVALID_TOKEN);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordResetToken\":\"expired-token\",\"newPassword\":\"NewPassword123!\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("RESET_TIMEOUT_OR_INVALID_TOKEN", json.get("errorCode").asText());
        assertEquals("비밀번호 변경 유효시간(5분)이 만료되었거나 올바르지 않은 접근입니다. 처음부터 다시 진행해 주세요.",
                json.get("message").asText());
    }

    @Test
    @DisplayName("비밀번호 변경 - 약한 비밀번호 → 422 + WEAK_PASSWORD")
    void passwordReset_weakPassword() throws Exception {
        // Given — Service 는 정책 미달 비밀번호를 거부한다
        stubPasswordResetError(AuthErrorCode.WEAK_PASSWORD);

        // When
        MvcResult result = mockMvc.perform(patch("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordResetToken\":\"9f1d7c3a-82ab-4d32-a5dd-000000000000\",\"newPassword\":\"password\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("WEAK_PASSWORD", json.get("errorCode").asText());
        assertEquals("비밀번호는 영문, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.", json.get("message").asText());
    }
}
