package com.workit.domain.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.service.AuthService;
import com.workit.exception.BusinessException;
import com.workit.exception.CommonExceptionAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AuthController (본인인증 검증 API) 테스트
// - Mockito 의존성이 없으므로 AuthService 를 수동 Stub 으로 주입한다 (Controller 계층 검증에 집중)
// - json-path 의존성 없이 Jackson ObjectMapper 로 응답 JSON 을 검증한다
class AuthControllerTest {

    private MockMvc mockMvc;

    // AuthService 수동 Stub - 실제 Service 의 계약(검증/중복 판단)을 그대로 흉내낸다
    private static class StubAuthService implements AuthService {

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
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(new StubAuthService()))
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
}
