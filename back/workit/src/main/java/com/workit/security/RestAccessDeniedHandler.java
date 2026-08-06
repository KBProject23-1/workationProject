package com.workit.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.global.dto.CommonResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// 인증은 되었지만 권한이 없는 리소스에 접근했을 때(403) 공통 응답 포맷으로 응답하는 핸들러
//
// 필터 체인 단계에서 발생한 예외는 @RestControllerAdvice 가 처리하지 못하므로
// CommonResponse JSON 을 직접 직렬화한다. (RestAuthenticationEntryPoint 와 동일 정책)
//
// 현재는 모든 사용자가 ROLE_USER 로 단일 권한이라 실질적으로 발생하지 않지만,
// 향후 role 컬럼/관리자 권한 도입 시 대비해 필터 체인에 등록한다.
//
// 응답: 403 + { "status": "ERROR", "errorCode": "AUTH_ACCESS_DENIED", "message": "..." }
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(AuthErrorCode.AUTH_ACCESS_DENIED.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                CommonResponse.error(AuthErrorCode.AUTH_ACCESS_DENIED.getErrorCode(),
                        AuthErrorCode.AUTH_ACCESS_DENIED.getMessage()));
    }
}
