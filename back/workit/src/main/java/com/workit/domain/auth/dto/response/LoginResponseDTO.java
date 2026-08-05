package com.workit.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

// 통합 로그인 응답
// API 스펙(docs): 로그인 → data: { userId, name, token_info }
//
// JSON 네이밍은 docs/task 스펙 그대로 snake_case 로 고정한다:
//   token_info.grant_type, token_info.access_token, token_info.access_token_expires_in
//
// 보안 규칙:
// - refreshToken 은 JSON 본문에 절대 포함하지 않는다 (@JsonIgnore)
//   → Controller 가 HttpOnly Cookie 로만 내려주고, 재발급 API 에서 쿠키로 받는다
// - access_token_expires_in 은 OAuth2 관례에 따라 초(seconds) 단위 반환
//   (application.properties jwt.access-token-expiration=15(분) → 900초)
@Getter
@Builder
@ToString
public class LoginResponseDTO {

    /** 회원 고유 번호 */
    private Long userId;

    /** 회원 이름 (user_profile 아님 — users.name_encrypt 복호화 값) */
    private String name;

    /** Access Token 정보 (token_info) */
    @JsonProperty("token_info")
    private TokenInfo tokenInfo;

    /** Refresh Token — HttpOnly Cookie 전용 (JSON 제외) */
    @JsonIgnore
    @ToString.Exclude
    private String refreshToken;

    /** Refresh Token Cookie Max-Age / Redis TTL (초) — 쿠키 생성 전용 (JSON 제외) */
    @JsonIgnore
    private long refreshTokenMaxAgeSeconds;

    /** token_info 내부 구조 (docs 스펙: grant_type, access_token, access_token_expires_in) */
    @Getter
    @Builder
    public static class TokenInfo {

        /** 토큰 인증 방식 (Bearer) */
        @JsonProperty("grant_type")
        private String grantType;

        /** Access Token JWT — JSON 에는 포함되지만 toString 에서는 제외 (로그 유출 방지) */
        @JsonProperty("access_token")
        @ToString.Exclude
        private String accessToken;

        /** Access Token 만료 시각(초) — OAuth2 관례 */
        @JsonProperty("access_token_expires_in")
        private long accessTokenExpiresIn;

        public static TokenInfo of(String grantType, String accessToken, long accessTokenExpiresIn) {
            return TokenInfo.builder()
                    .grantType(grantType)
                    .accessToken(accessToken)
                    .accessTokenExpiresIn(accessTokenExpiresIn)
                    .build();
        }
    }
}
