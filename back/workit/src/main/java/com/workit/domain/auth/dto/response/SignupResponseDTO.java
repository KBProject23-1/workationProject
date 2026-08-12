package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

// 최종 회원가입 완료 응답
// API 스펙(docs): 회원가입 → data: { userId, name }
//
// 토큰 미발급 정책:
// - 회원가입 완료 후 자동 로그인을 제거했다 (완료 후 로그인 화면으로 이동해 다시 로그인).
// - 따라서 Access/Refresh Token Cookie 를 포함한 어떤 토큰도 발급·반환하지 않는다.
//   (Cookie 발급은 login API 만 담당한다)
@Getter
@Builder
@ToString
public class SignupResponseDTO {

    /** 회원 고유 번호 */
    private Long userId;

    /** 회원 이름 (users.name_encrypt 복호화 값 — 완료 화면 안내용) */
    private String name;

    public static SignupResponseDTO of(Long userId, String name) {
        return SignupResponseDTO.builder()
                .userId(userId)
                .name(name)
                .build();
    }
}
