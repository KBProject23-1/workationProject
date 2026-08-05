package com.workit.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

// 로그인 토큰 재발급 응답
// API 스펙(docs): 재발급 → data: { token_info: { grant_type, access_token, access_token_expires_in } }
//
// - token_info 구조는 로그인(LoginResponseDTO.TokenInfo)과 동일하므로 기존 클래스를 재사용한다 (중복 생성 금지)
// - JSON 네이밍은 docs/task 스펙 그대로 snake_case 로 고정한다
//
// 보안 규칙:
// - 신규 Refresh Token(Rotation 값)은 JSON 본문에 절대 포함하지 않는다 (@JsonIgnore)
//   → Controller 가 HttpOnly Cookie 로만 내려주고, 다음 재발급에서 쿠키로 받는다
@Getter
@Builder
@ToString
public class RefreshTokenResponseDTO {

    /** Access Token 정보 (token_info) — 로그인 응답과 동일 구조 재사용 */
    @JsonProperty("token_info")
    private LoginResponseDTO.TokenInfo tokenInfo;

    /** 신규 Refresh Token — Rotation 으로 갱신된 값. HttpOnly Cookie 전용 (JSON 제외) */
    @JsonIgnore
    @ToString.Exclude
    private String refreshToken;

    /** Refresh Token Cookie Max-Age / Redis TTL (초) — 쿠키 생성 전용 (JSON 제외) */
    @JsonIgnore
    private long refreshTokenMaxAgeSeconds;
}
