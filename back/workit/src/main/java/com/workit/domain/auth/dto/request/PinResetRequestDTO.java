package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 보안 PIN 번호 재설정 요청 (로그인 사용자 전용)
// API 스펙(docs): PATCH /api/v1/auth/me/pin/reset
// 필드: identityVerificationId, pinNumber
//
// - 로그인 사용자가 PASS 본인인증을 다시 수행한 뒤, 신규 PIN 으로 즉시 변경한다
// - 비밀번호 재설정과 달리 임시 토큰(Redis passwordResetToken)을 사용하지 않는 단일 요청 구조
// - javax.validation 미사용 프로젝트 구조이므로 필수 값/형식 검증은 Service Layer 에서 수행한다
//   (기존 Request DTO 와 동일 방식)
//
// 보안 규칙 (knowledge.md: PIN 원문/identityVerificationId 원문 로그 출력 금지):
// - pinNumber 는 @ToString.Exclude 로 처리 — DTO 를 로그에 남겨도 PIN 원문이 노출되지 않는다
// - identityVerificationId 도 PASS 인증 요청용 임시값이므로 @ToString.Exclude 로 처리
//   (로그 노출 시 인증 흐름 추적/재사용 시도 가능성 방지)
@Getter
@Setter
@ToString
public class PinResetRequestDTO {

    /** 포트원 본인인증 고유 번호 (imp_ver_...) — PASS 재인증 결과 (필수). 로그 출력 금지 */
    @ToString.Exclude
    private String identityVerificationId;

    /** 새로 설정할 6자리 PIN — BCrypt 해시로 저장. 로그 출력 금지 */
    @ToString.Exclude
    private String pinNumber;
}
