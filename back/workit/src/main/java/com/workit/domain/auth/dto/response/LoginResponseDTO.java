package com.workit.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

// 통합 로그인 응답 (Cookie 기반 인증)
// API 스펙(docs): 로그인 → data: { userId, name, pinSetupRequired }
//
// 보안 규칙 (Cookie 기반 인증 통일):
// - Access Token / Refresh Token 은 JSON 본문에 절대 포함하지 않는다 (@JsonIgnore)
// - Controller 가 두 토큰을 모두 HttpOnly Cookie(accessToken / refreshToken)로만 내려준다
// - DTO 내부 필드는 Controller 가 Cookie 를 생성하기 위해 Service → Controller 로 전달하는 용도
// - @ToString.Exclude: JWT 가 toString() 로그에 노출되지 않도록 차단
@Getter
@Builder
@ToString
public class LoginResponseDTO {

    /** 회원 고유 번호 */
    private Long userId;

    /** 회원 이름 (user_profile 아님 — users.name_encrypt 복호화 값) */
    private String name;

    /**
     * 기기 최초 로그인 여부 — user_device 에 해당 deviceId 가 등록되어 있지 않으면 true
     * - 로그인 화면에서 PIN 등록 유도 분기용 (docs: 로그인 성공 후 deviceId 기반 기기 확인 → PIN 등록 안내)
     * - PASSWORD 로그인에서 deviceId 를 함께 받은 경우에만 판별 (deviceId 미전달 시 false)
     * - PIN 로그인은 등록된 기기에서만 성공하므로 항상 false
     */
    private boolean pinSetupRequired;

    /** Access Token — HttpOnly Cookie(accessToken) 전용 (JSON 제외) */
    @JsonIgnore
    @ToString.Exclude
    private String accessToken;

    /** Access Token Cookie Max-Age (초) — 쿠키 생성 전용 (JSON 제외) */
    @JsonIgnore
    private long accessTokenMaxAgeSeconds;

    /** Refresh Token — HttpOnly Cookie(refreshToken) 전용 (JSON 제외) */
    @JsonIgnore
    @ToString.Exclude
    private String refreshToken;

    /** Refresh Token Cookie Max-Age / Redis TTL (초) — 쿠키 생성 전용 (JSON 제외) */
    @JsonIgnore
    private long refreshTokenMaxAgeSeconds;
}
