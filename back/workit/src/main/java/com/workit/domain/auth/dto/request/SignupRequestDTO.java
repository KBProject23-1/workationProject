package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

// 최종 회원가입 완료 요청
// API 스펙(docs): POST /api/v1/auth/signup
// 필드: identityVerificationId, email, password, pin, agreedTermsIds
// - identityVerificationId 는 POST /auth/pass 에서 백엔드가 발급한 값 (프론트 생성 금지)
//   → 백엔드가 Redis(mock:pass:{id}) 세션에서 인증 정보(name/phone/CI)를 복원한다
// - nickname 필드는 제거됨 (닉네임 입력 기능 삭제 — user_profile.nickname 은 서버가 기본값 생성)
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

    /** Mock PASS 인증 완료 후 백엔드가 발급한 본인인증 고유 번호 */
    private String identityVerificationId;

    /** 유저 이메일 (로그인 ID) */
    private String email;

    /** 유저 비밀번호 (BCrypt 해시 저장) — 로그 출력 금지 */
    @ToString.Exclude
    private String password;

    /** 6자리 PIN — 이번 API 범위 제외, 별도 PIN 등록 API 에서 처리. 로그 출력 금지 */
    @ToString.Exclude
    private String pin;

    /** 유저가 동의한 약관 ID 배열 — 필수 약관 전부 포함되어야 가입 가능 (검증은 Service Layer) */
    private List<Long> agreedTermsIds;
}
