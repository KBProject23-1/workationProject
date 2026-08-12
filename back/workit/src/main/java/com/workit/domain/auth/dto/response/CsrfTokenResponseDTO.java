package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

// CSRF Token 발급 응답
// API 스펙(knowledge.md): GET /api/v1/auth/csrf → data.csrfToken
//
// Cookie 기반 인증의 CSRF 방어는 Spring Security 의 CookieCsrfTokenRepository 가
// XSRF-TOKEN Cookie(비 HttpOnly) 를 자동 발급하고, 프론트가 X-XSRF-TOKEN Header 로
// 같은 값을 다시 보내는 것으로 검증한다.
//
// 이 엔드포인트가 필요한 이유:
// - 프론트(5173)와 백엔드(8080)가 서로 다른 Origin(CORS + withCredentials)이면
//   브라우저가 Set-Cookie 는 저장하지만 JS(document.cookie)로는 백엔드 Origin 의
//   Cookie 를 읽을 수 없다.
// - 따라서 Cookie 와 동일한 값을 JSON 본문으로도 내려주어, 프론트가 메모리에 보관해
//   X-XSRF-TOKEN Header 로 전송할 수 있게 한다. (같은 Origin/프록시 환경에서는
//   프론트가 Cookie 를 직접 읽어 사용하므로 이 값은 무시해도 된다)
//
// 보안 규칙:
// - CSRF Token 은 인증 정보가 아니며, 유출되어도 Access/Refresh Token 을 대신할 수 없다 (knowledge.md)
// - csrfToken 이 JSON 으로 노출되는 것은 설계상 의도된 동작이다 (HttpOnly=false 와 동일 정책)
@Getter
@Builder
public class CsrfTokenResponseDTO {

    /** XSRF-TOKEN Cookie 와 동일한 값 — 프론트가 X-XSRF-TOKEN Header 로 다시 보낸다 */
    private String csrfToken;

    public static CsrfTokenResponseDTO of(String csrfToken) {
        return CsrfTokenResponseDTO.builder()
                .csrfToken(csrfToken)
                .build();
    }
}
