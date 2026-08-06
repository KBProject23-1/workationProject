package com.workit.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 인증 없이 보호 API 접근 시 401 진입점 단위 테스트
class RestAuthenticationEntryPointTest {

    @Test
    @DisplayName("인증 실패 → 401 + CommonResponse(ERROR/AUTH_TOKEN_NOT_FOUND) JSON")
    void commence_writes401() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("anonymous"));

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":\"ERROR\""));
        assertTrue(body.contains("\"errorCode\":\"AUTH_TOKEN_NOT_FOUND\""));
        assertTrue(body.contains("\"message\":"));
    }
}
