package com.workit.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

// 로그인 토큰 재발급 응답 (Cookie 기반 인증)
// API 스펙(docs): 재발급 → data: null (JWT 는 응답 본문에 포함하지 않는다)
//
// 보안 규칙 (Cookie 기반 인증 통일):
// - 신규 Access Token(Rotation 발급) 은 HttpOnly Cookie(accessToken) 로만 내려준다 (JSON 제외)
// - 신규 Refresh Token(Rotation 값) 은 HttpOnly Cookie(refreshToken) 로만 내려준다 (JSON 제외)
// - DTO 내부 필드는 Controller 가 Cookie 를 생성하기 위해 Service → Controller 로 전달하는 용도
// - @ToString.Exclude: JWT 가 toString() 로그에 노출되지 않도록 차단
@Getter
@Builder
@ToString
public class RefreshTokenResponseDTO {

    /** 신규 Access Token — HttpOnly Cookie(accessToken) 전용 (JSON 제외) */
    @JsonIgnore
    @ToString.Exclude
    private String accessToken;

    /** Access Token Cookie Max-Age (초) — 쿠키 생성 전용 (JSON 제외) */
    @JsonIgnore
    private long accessTokenMaxAgeSeconds;

    /** 신규 Refresh Token — Rotation 으로 갱신된 값. HttpOnly Cookie(refreshToken) 전용 (JSON 제외) */
    @JsonIgnore
    @ToString.Exclude
    private String refreshToken;

    /** Refresh Token Cookie Max-Age / Redis TTL (초) — 쿠키 생성 전용 (JSON 제외) */
    @JsonIgnore
    private long refreshTokenMaxAgeSeconds;
}
