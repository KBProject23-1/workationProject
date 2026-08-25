package com.workit.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 권한 부족 시 403 핸들러 단위 테스트
class RestAccessDeniedHandlerTest {

    @Test
    @DisplayName("권한 없음 → 403 + CommonResponse(ERROR/AUTH_ACCESS_DENIED) JSON")
    void handle_writes403() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/wallets/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"status\":\"ERROR\""));
        assertTrue(body.contains("\"errorCode\":\"AUTH_ACCESS_DENIED\""));
        assertTrue(body.contains("\"message\":"));
    }
}
