package com.workit.domain.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.auth.dto.request.MockPassCompleteRequestDTO;
import com.workit.domain.auth.dto.response.MockPassStatusResponseDTO;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.domain.auth.service.MockPassService;
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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// MockPassController API 테스트
// - MockPassService 를 Mockito @Mock 으로 주입한다 (Controller 계층 검증에 집중)
// - json-path 의존성 없이 Jackson ObjectMapper 로 응답 JSON 을 검증한다 (AuthControllerTest 와 동일 방식)
@ExtendWith(MockitoExtension.class)
class MockPassControllerTest {

    @Mock
    private MockPassService mockPassService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MockPassController(mockPassService))
                .setControllerAdvice(new CommonExceptionAdvice())
                .build();
    }

    private JsonNode parse(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(body);
    }

    @Test
    @DisplayName("인증 완료 등록 성공 - 200 + SUCCESS + status VERIFIED/name 반환")
    void complete_success() throws Exception {
        // Given — Service 가 VERIFIED 세션 등록 결과를 반환한다
        when(mockPassService.complete(any(MockPassCompleteRequestDTO.class)))
                .thenReturn(MockPassStatusResponseDTO.of("mock-12345678", "VERIFIED", "홍길동"));

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"mock-12345678\",\"name\":\"홍길동\","
                                + "\"phoneNumber\":\"01012345678\"}"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("SUCCESS", json.get("status").asText());
        assertEquals("본인인증이 완료되었습니다.", json.get("message").asText());
        assertTrue(json.get("errorCode") == null || json.get("errorCode").isNull());

        JsonNode data = json.get("data");
        assertNotNull(data);
        assertEquals("mock-12345678", data.get("identityVerificationId").asText());
        assertEquals("VERIFIED", data.get("status").asText());
        assertEquals("홍길동", data.get("name").asText());

        // Service 가 완료 등록 요청을 전달받았는지 확인
        verify(mockPassService).complete(any(MockPassCompleteRequestDTO.class));
    }

    @Test
    @DisplayName("요청 값 오류 - 400 + INVALID_PASS_REQUEST")
    void complete_invalidRequest() throws Exception {
        // Given — Service 가 요청 값 오류를 거부한다
        doThrow(new BusinessException(AuthErrorCode.INVALID_PASS_REQUEST))
                .when(mockPassService).complete(any(MockPassCompleteRequestDTO.class));

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identityVerificationId\":\"mock-12345678\",\"name\":\"\","
                                + "\"phoneNumber\":\"01012345678\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_PASS_REQUEST", json.get("errorCode").asText());
        assertEquals("본인인증 요청 값이 올바르지 않습니다. 다시 확인해 주세요.", json.get("message").asText());
    }

    @Test
    @DisplayName("identityVerificationId 누락 - 400 + INVALID_VERIFICATION_ID")
    void complete_missingId() throws Exception {
        // Given — Service 가 누락된 인증 ID 를 거부한다
        doThrow(new BusinessException(AuthErrorCode.INVALID_VERIFICATION_ID))
                .when(mockPassService).complete(any(MockPassCompleteRequestDTO.class));

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/auth/pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"phoneNumber\":\"01012345678\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertEquals("INVALID_VERIFICATION_ID", json.get("errorCode").asText());
    }

    @Test
    @DisplayName("잘못된 JSON 본문 - 400 (공통 형식 오류)")
    void complete_malformedBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not-json"))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode json = parse(result);
        assertEquals("ERROR", json.get("status").asText());
        assertFalse(json.get("errorCode").isNull());
    }
}
