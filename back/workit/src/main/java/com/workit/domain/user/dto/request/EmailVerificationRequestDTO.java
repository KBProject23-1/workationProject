package com.workit.domain.user.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 로그인 사용자 이메일 인증번호 발송 요청
// API 스펙: POST /api/v1/users/me/email/verification
// 필드: email
//
// - 이메일 변경 전, 변경할 새 이메일 주소로 인증번호를 발송하기 위한 요청이다
//   (실제 이메일은 발송하지 않으며 Mock 방식으로 서버에 인증번호를 저장/로그 출력)
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값/형식 검증은 Service Layer 에서 수행한다
//   (PhoneChangeRequestDTO 와 동일 방식 — EmailValidator 공통 정책)
// - email 은 개인정보 — 로그/toString 노출 금지
//   (knowledge.md: 개인정보 원문 로그 출력 금지 — AccountPasswordVerifyRequestDTO.password 와 동일 정책)
@Getter
@Setter
@ToString
public class EmailVerificationRequestDTO {

    /** 인증번호를 받을 변경 예정 이메일 (필수, 이메일 형식). 로그/toString 노출 금지 */
    @ToString.Exclude
    private String email;
}
