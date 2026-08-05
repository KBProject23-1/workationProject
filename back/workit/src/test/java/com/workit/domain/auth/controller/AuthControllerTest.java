package com.workit.domain.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.auth.dto.request.LoginRequestDTO;
import com.workit.domain.auth.dto.request.SignupRequestDTO;
import com.workit.domain.auth.dto.response.EmailAvailabilityResponseDTO;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.LoginResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.service.AuthService;
import com.workit.exception.BusinessException;
import com.workit.exception.CommonExceptionAdvice;
import com.workit.global.util.EmailValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AuthController (본인인증 검증 API) 테스트
// - Mockito 의존성이 없으므로 AuthService 를 수동 Stub 으로 주입한다 (Controller 계층 검증에 집중)
// - json-path 의존성 없이 Jackson ObjectMapper 로 응답 JSON 을 검증한다
class AuthControllerTest {

    private MockMvc mockMvc;

    // AuthService 수동 Stub - 실제 Service 의 계약(검증/중복 판단)을 그대로 흉내낸다
    private static class StubAuthService implements AuthService {

        // Controller 테스트용 이메일 검증 — Service 와 동일한 EmailValidator 공통 정책 사용
        // (실제 검증 로직은 Service 테스트에서 검증)

        @Override
        public TermsListResponseDTO getTermsList() {
            return TermsListResponseDTO.of(Collections.emptyList());
        }

        @Override
        public IdentityVerificationResponseDTO verifyIdentity(String identityVerificationId) {
            if (identityVerificationId == null || identityVerificationId.trim().isEmpty()
                    || "invalid".equals(identityVerificationId)) {
                throw new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID);
            }
            if ("imp_ver_9999999999".equals(identityVerificationId)) {
                throw new BusinessException(AuthErrorCode.DUPLICATE_USER);
            }
            return IdentityVerificationResponseDTO.of("encrypted-identity-token", "홍길동");
        }

        @Override
        public EmailAvailabilityResponseDTO checkEmailAvailability(String email) {
            try {
                String normalized = EmailValidator.normalize(email);
                // "used@example.com" 은 이미 가입된 이메일로 간주
                if ("used@example.com".equals(normalized)) {
                    return EmailAvailabilityResponseDTO.of(false);
                }
                return EmailAvailabilityResponseDTO.of(true);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(AuthErrorCode.INVALID_EMAIL_FORMAT);
            }
        }

        @Override
        public void signup(SignupRequestDTO request) {
            if (request == null
                    || request.getIdentityToken() == null || request.getIdentityToken().trim().isEmpty()
                    || request.getEmail() == null || request.getEmail().trim().isEmpty()
                    || request.getPassword() == null || request.getPassword().trim().isEmpty()
                    || request.getNickname() == null || request.getNickname().trim().isEmpty()) {
                throw new BusinessException(AuthErrorCode.INVALID_SIGNUP_REQUEST);
            }

            // 약관 동의 검증 (Stub — 실제 검증은 Service 테스트에서 검증)
            // 검증 순서는 Service 와 동일: 빈 배열 → 존재 여부 → 필수 누락
            List<Long> agreedTermsIds = request.getAgreedTermsIds();
            if (agreedTermsIds == null || agreedTermsIds.isEmpty()) {
                throw new BusinessException(AuthErrorCode.MISSING_REQUIRED_TERMS);
            }

            // 존재하지 않는 약관 ID 검증 (Stub — terms 에는 1, 2, 3 만 존재한다고 가정)
            for (Long termId : agreedTermsIds) {
                if (termId == null || (termId != 1L && termId != 2L && termId != 3L)) {
                    throw new BusinessException(AuthErrorCode.INVALID_TERM_ID);
                }
            }

            // 필수 약관은 1, 2 로 가정 — 누락 시 MISSING_REQUIRED_TERMS
            if (!agreedTermsIds.containsAll(Arrays.asList(1L, 2L))) {
                throw new BusinessException(AuthErrorCode.MISSING_REQUIRED_TERMS);
            }

            String token = request.getIdentityToken();
            if ("expired-token".equals(token)) {
                throw new BusinessException(AuthErrorCode.EXPIRED_SIGNUP_TOKEN);
            }
            if ("tampered-token".equals(token)) {
                throw new BusinessException(AuthErrorCode.INVALID_SIGNUP_TOKEN);
            }
            if ("no-redis-token".equals(token)) {
                throw new BusinessException(AuthErrorCode.SIGNUP_VERIFICATION_NOT_FOUND);
            }
            if ("dup-ci-token".equals(token)) {
                throw new BusinessException(AuthErrorCode.DUPLICATE_USER);
            }
            // 이메일 형식/길이 검증 (Stub — 실제 검증은 Service 테스트에서 검증)
            String normalizedEmail;
            try {
                normalizedEmail = EmailValidator.normalize(request.getEmail());
            } catch (IllegalArgumentException e) {
                throw new BusinessException(AuthErrorCode.INVALID_EMAIL_FORMAT);
            }
            if ("used@example.com".equals(normalizedEmail)) {
                throw new BusinessException(AuthErrorCode.DUPLICATE_EMAIL);
            }
            if ("taken".equals(request.getNickname().trim())) {
                throw new BusinessException(AuthErrorCode.DUPLICATE_NICKNAME);
            }
        }

        @Override
        public LoginResponseDTO login(LoginRequestDTO request) {
            // Controller 테스트용 인증 판정 Stub — 실제 검증 로직은 Service 테스트에서 검증
            if (request == null || request.getLoginType() == null || request.getLoginType().trim().isEmpty()) {
                throw new BusinessException(AuthErrorCode.INVALID_LOGIN_REQUEST);
            }
            String type = request.getLoginType().trim();
            if (!"PASSWORD".equals(type) && !"PIN".equals(type)) {
                throw new BusinessException(AuthErrorCode.INVALID_LOGIN_TYPE);
            }
            if ("PASSWORD".equals(type)) {
                if (request.getLoginId() == null || request.getLoginId().trim().isEmpty()
                        || request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                    throw new BusinessException(AuthErrorCode.INVALID_LOGIN_REQUEST);
                }
                if ("unknown@example.com".equals(request.getLoginId().trim())) {
                    throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
                }
                if (!"password123!".equals(request.getPassword())) {
                    throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
                }
            } else {
                if (request.getPinNumber() == null || request.getPinNumber().trim().isEmpty()
                        || request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
                    throw new BusinessException(AuthErrorCode.INVALID_LOGIN_REQUEST);
                }
                if (!"123456".equals(request.getPinNumber())) {
                    throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
                }
                if ("locked-device".equals(request.getDeviceId())) {
                    throw new BusinessException(AuthErrorCode.PIN_LOCK_EXCEEDED);
                }
            }
            return LoginResponseDTO.builder()
                    .userId(501L)
                    .name("홍길동")
                    .tokenInfo(LoginResponseDTO.TokenInfo.of("Bearer", "access-token-jwt", 900))
                    .refreshToken("refresh-token-jwt")
                    .refreshTokenMaxAgeSeconds(1209600)
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        // refreshCookieSecure=true / sameSite=Lax — 운영(HTTPS) 쿠키 스펙 그대로 검증
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(new StubAuthService(), true, "Lax"))
                .setControllerAdvice(new CommonExceptionAdvice())
                .build();
    }

    private JsonNode parse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(body);
    }

    @Test
    @DisplayName("본인인증 검증 성공 - 200 + SUCCESS + identityToken/name 반환")
    void verifyIdentity_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"imp_ver_1234567890\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("본인인증 성공. 가입을 진행합니다.", json.get("message").asText());

        // 성공 응답에는 errorCode 가 없어야 한다 (CommonResponse NON_NULL)
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertEquals("encrypted-identity-token", data.get("identityToken").asText());
        assertEquals("홍길동", data.get("name").asText());
    }

    @Test
    @DisplayName("유효하지 않은 인증 ID - 400 + INVALID_VERIFICATION_ID")
    void verifyIdentity_invalidId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_VERIFICATION_ID", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("빈 identityVerificationId - 400 + INVALID_VERIFICATION_ID")
    void verifyIdentity_blankId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_VERIFICATION_ID", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("identityVerificationId 누락 - 400 + INVALID_VERIFICATION_ID")
    void verifyIdentity_missingId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_VERIFICATION_ID", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("중복 가입 - 409 + DUPLICATE_USER")
    void verifyIdentity_duplicateUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"imp_ver_9999999999\"}"))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_USER", json.get("errorCode").asText());
        assertEquals("이미 가입된 회원입니다. 로그인을 진행해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("잘못된 JSON 본문 - 400 (공통 형식 오류)")
    void verifyIdentity_malformedBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup/verify-identity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertFalse(json.get("errorCode").isNull());
    }

    // ---------- 회원가입 이메일 중복 확인 ----------

    @Test
    @DisplayName("이메일 중복 확인 - 사용 가능한 이메일 (200 + SUCCESS + available=true)")
    void checkEmail_available() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", "new@example.com"))
                .andExpect(status().isOk())
                .andReturn();

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
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", "used@example.com"))
                .andExpect(status().isOk())
                .andReturn();

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
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_EMAIL_FORMAT", json.get("errorCode").asText());
        assertEquals("올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 잘못된 이메일 형식 (400 + INVALID_EMAIL_FORMAT)")
    void checkEmail_invalidFormat() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", "not-an-email"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_EMAIL_FORMAT", json.get("errorCode").asText());
        assertEquals("올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 최대 길이(254자) 정상 (200 + SUCCESS + available=true)")
    void checkEmail_maxLength() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", buildLongEmail(254)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("사용 가능한 이메일입니다.", json.get("message").asText());
        assertTrue(json.get("data").get("available").asBoolean());
    }

    @Test
    @DisplayName("이메일 중복 확인 - 255자 이상 이메일 (400 + INVALID_EMAIL_FORMAT)")
    void checkEmail_tooLong() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/signup/check-email")
                        .param("email", buildLongEmail(255)))
                .andExpect(status().isBadRequest())
                .andReturn();

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
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("회원가입이 완료되었습니다.", json.get("message").asText());

        // data 는 null (CommonResponse NON_NULL 로 JSON 에서 제외될 수 있음)
        assertTrue(json.get("data") == null || json.get("data").isNull());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());
    }

    @Test
    @DisplayName("회원가입 완료 - JWT 만료 (401 + EXPIRED_SIGNUP_TOKEN)")
    void signup_expiredToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("expired-token", "new@example.com", "tester")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("EXPIRED_SIGNUP_TOKEN", json.get("errorCode").asText());
        assertEquals("본인인증 유효 시간이 만료되었습니다. 인증을 다시 진행해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - JWT 위변조 (400 + INVALID_SIGNUP_TOKEN)")
    void signup_tamperedToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("tampered-token", "new@example.com", "tester")))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_SIGNUP_TOKEN", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - Redis 임시 데이터 없음 (400 + SIGNUP_VERIFICATION_NOT_FOUND)")
    void signup_verificationNotFound() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("no-redis-token", "new@example.com", "tester")))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("SIGNUP_VERIFICATION_NOT_FOUND", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - CI 중복 (409 + DUPLICATE_USER)")
    void signup_duplicateCi() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("dup-ci-token", "new@example.com", "tester")))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_USER", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 이메일 중복 (409 + DUPLICATE_EMAIL)")
    void signup_duplicateEmail() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "used@example.com", "tester")))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_EMAIL", json.get("errorCode").asText());
        assertEquals("이미 사용 중인 이메일입니다.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 닉네임 중복 (409 + DUPLICATE_NICKNAME)")
    void signup_duplicateNickname() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "taken")))
                .andExpect(status().isConflict())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("DUPLICATE_NICKNAME", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 255자 이상 이메일 (400 + INVALID_EMAIL_FORMAT)")
    void signup_emailTooLong() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", buildLongEmail(255), "tester")))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_EMAIL_FORMAT", json.get("errorCode").asText());
        assertEquals("올바르지 않은 이메일 형식입니다. 이메일을 다시 확인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 필수 값 누락 (400 + INVALID_SIGNUP_REQUEST)")
    void signup_missingRequired() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_SIGNUP_REQUEST", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - agreedTermsIds 포함 정상 요청 (200 + SUCCESS)")
    void signup_withAgreedTerms_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester",
                                ",\"agreedTermsIds\":[1,2,3]")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("회원가입이 완료되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());
    }

    @Test
    @DisplayName("회원가입 완료 - agreedTermsIds 누락 (400 + MISSING_REQUIRED_TERMS)")
    void signup_missingAgreedTerms() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester", "")))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("MISSING_REQUIRED_TERMS", json.get("errorCode").asText());
        assertEquals("필수 약관에 모두 동의해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 존재하지 않는 약관 ID (400 + INVALID_TERM_ID)")
    void signup_invalidTermId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester",
                                ",\"agreedTermsIds\":[1,2,99]")))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_TERM_ID", json.get("errorCode").asText());
        assertEquals("존재하지 않는 약관이 포함되어 있습니다. 다시 확인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 필수 약관 누락 (400 + MISSING_REQUIRED_TERMS)")
    void signup_missingRequiredTerms() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("valid-token", "new@example.com", "tester",
                                ",\"agreedTermsIds\":[1]")))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("MISSING_REQUIRED_TERMS", json.get("errorCode").asText());
        assertEquals("필수 약관에 모두 동의해주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("회원가입 완료 - 잘못된 JSON 본문 (400 공통 형식 오류)")
    void signup_malformedBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertFalse(json.get("errorCode").isNull());
    }

    // ---------- 통합 로그인 ----------

    @Test
    @DisplayName("PASSWORD 로그인 성공 - 200 + SUCCESS + userId/name/token_info + Set-Cookie(HttpOnly)")
    void login_password_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"loginId\":\"user@example.com\",\"password\":\"password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

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
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"loginId\":\"010-1234-5678\",\"password\":\"password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals(501, json.get("data").get("userId").asInt());
        assertEquals("홍길동", json.get("data").get("name").asText());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    @DisplayName("PIN 로그인 성공 - 200 + SUCCESS + userId + 쿠키 발급")
    void login_pin_success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PIN\",\"pinNumber\":\"123456\",\"deviceId\":\"9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals(501, json.get("data").get("userId").asInt());
        assertTrue(result.getResponse().getHeader("Set-Cookie").contains("HttpOnly"));
    }

    @Test
    @DisplayName("잘못된 password - 401 + INVALID_CREDENTIALS")
    void login_wrongPassword() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"loginId\":\"user@example.com\",\"password\":\"wrong!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_CREDENTIALS", json.get("errorCode").asText());
        assertEquals("인증 정보가 올바르지 않습니다. 다시 확인 후 시도해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("잘못된 PIN - 401 + INVALID_CREDENTIALS")
    void login_wrongPin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PIN\",\"pinNumber\":\"000000\",\"deviceId\":\"device-uuid\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_CREDENTIALS", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 - 401 + INVALID_CREDENTIALS")
    void login_unknownUser() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\",\"loginId\":\"unknown@example.com\",\"password\":\"password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("INVALID_CREDENTIALS", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("잘못된 loginType - 400 + INVALID_LOGIN_TYPE")
    void login_invalidLoginType() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"FACE_ID\",\"loginId\":\"user@example.com\",\"password\":\"password123!\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_LOGIN_TYPE", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("필수 값 누락 - 400 + INVALID_LOGIN_REQUEST")
    void login_missingRequired() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PASSWORD\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_LOGIN_REQUEST", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("PIN 잠금 - 403 + PIN_LOCK_EXCEEDED")
    void login_pinLocked() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loginType\":\"PIN\",\"pinNumber\":\"123456\",\"deviceId\":\"locked-device\"}"))
                .andExpect(status().isForbidden())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("PIN_LOCK_EXCEEDED", json.get("errorCode").asText());
        assertEquals("핀번호 입력 횟수가 5회 초과하여 계정이 잠겼습니다. PASS 본인인증을 통해 핀번호를 재설정해 주세요.",
                json.get("message").asText());
    }

    @Test
    @DisplayName("잘못된 JSON 본문 - 400 (공통 형식 오류)")
    void login_malformedBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertFalse(json.get("errorCode").isNull());
    }
}
