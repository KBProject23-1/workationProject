package com.workit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.global.dto.CommonResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// 보호 API에 인증 없이 접근했을 때(401) 공통 응답 포맷으로 응답하는 진입점
//
// 필터 체인 단계에서 발생한 예외는 DispatcherServlet 을 거치지 않으므로
// @RestControllerAdvice(CommonExceptionAdvice)가 처리하지 못한다.
// 따라서 CommonResponse JSON 을 직접 직렬화해 응답 포맷을 유지한다.
//
// 응답: 401 + { "status": "ERROR", "errorCode": "AUTH_TOKEN_NOT_FOUND", "message": "..." }
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(AuthErrorCode.AUTH_TOKEN_NOT_FOUND.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                CommonResponse.error(AuthErrorCode.AUTH_TOKEN_NOT_FOUND.getErrorCode(),
                        AuthErrorCode.AUTH_TOKEN_NOT_FOUND.getMessage()));
    }
}
