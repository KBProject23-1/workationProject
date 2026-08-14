package com.workit.domain.user.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 로그인 사용자 이메일 인증번호 확인 요청
// API 스펙: POST /api/v1/users/me/email/verification/confirm
// 필드: email, verificationCode
//
// - 이메일 변경 전 발송된 인증번호가 올바른지 확인하기 위한 요청이다
//   (인증 성공 시 서버가 해당 이메일을 인증 완료 상태로 저장하고, 실제 이메일 변경은 별도 API 에서 처리)
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값/형식 검증은 Service Layer 에서 수행한다
//   (EmailVerificationRequestDTO 와 동일 방식 — EmailValidator 공통 정책)
// - email 은 개인정보, verificationCode 는 1회성 인증값 — 로그/toString 노출 금지
//   (knowledge.md: 개인정보 원문 로그 출력 금지 — EmailVerificationRequestDTO.email 과 동일 정책)
@Getter
@Setter
@ToString
public class EmailVerificationConfirmRequestDTO {

    /** 인증번호를 발송받은 이메일 (필수, 이메일 형식). 로그/toString 노출 금지 */
    @ToString.Exclude
    private String email;

    /** 이메일로 발송된 6자리 인증번호 (필수). 로그/toString 노출 금지 */
    @ToString.Exclude
    private String verificationCode;
}
