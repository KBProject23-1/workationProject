package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

// 최종 회원가입 완료 요청
// API 스펙(docs): POST /api/v1/auth/signup
// 필드: identityToken, email, password, pin, nickname, agreedTermsIds
// - javax.validation 의존성이 없는 프로젝트 구조이므로 @NotBlank 같은 애노테이션 대신
//   Service Layer 에서 null/빈 값/형식 검증을 수행한다 (기존 Request DTO 와 동일 방식)
// - pin 은 이번 회원가입 API 범위 제외 (별도 PIN 등록 API 에서 처리) — 요청 본문에는 받지만 저장하지 않는다
//
// 보안 규칙 (knowledge.md: 비밀번호 원문 로그 출력 금지):
// - password/pin 은 @ToString.Exclude 로 처리 — DTO 를 로그에 남겨도 비밀번호·PIN 원문이 노출되지 않는다
@Getter
@Setter
@ToString
public class SignupRequestDTO {

    /** 본인인증 검증 단계에서 발급받은 회원가입 전용 임시 JWT */
    private String identityToken;

    /** 유저 이메일 (로그인 ID) */
    private String email;

    /** 유저 비밀번호 (BCrypt 해시 저장) — 로그 출력 금지 */
    @ToString.Exclude
    private String password;

    /** 6자리 PIN — 이번 API 범위 제외, 별도 PIN 등록 API 에서 처리. 로그 출력 금지 */
    @ToString.Exclude
    private String pin;

    /** 유저 닉네임 (user_profile.nickname, UNIQUE) */
    private String nickname;

    /** 유저가 동의한 약관 ID 배열 — 필수 약관 전부 포함되어야 가입 가능 (검증은 Service Layer) */
    private List<Long> agreedTermsIds;
}
