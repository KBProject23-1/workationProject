package com.workit.security;

// 보안 경로 상수 — SecurityConfig(permitAll)와 JwtAuthenticationFilter(공개 경로 통과)가
// 동일한 경로 정의를 공유하도록 중앙화한다. 둘이 어긋나면
// (예: 새 공개 API를 SecurityConfig 에만 추가) 필터가 만료된 토큰을 거부해
// 공개 API 흐름이 깨질 수 있으므로 반드시 이 클래스를 통해서만 참조한다.
public final class SecurityPath {

    private SecurityPath() {
    }

    /** 공개 인증 API 접두어 — /api/v1/auth/ 로 시작하는 경로는 인증 없이 호출 가능 */
    public static final String PUBLIC_AUTH_PREFIX = "/api/v1/auth/";

    /** 공개 인증 API 패턴 — SecurityConfig 의 permitAll 에 사용 */
    public static final String PUBLIC_AUTH_PATTERN = PUBLIC_AUTH_PREFIX + "**";
}
