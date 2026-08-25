package com.workit.domain.user.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 로그인 사용자 휴대폰 번호 변경 요청
// API 스펙: PATCH /api/v1/users/me/phone
// 필드: identityVerificationId
//
// - Mock PASS 본인인증(POST /api/v1/auth/pass) 완료 후 백엔드가 발급한 본인인증 ID 만 전달한다
// - 변경할 휴대폰 번호(phoneNumber)는 Request Body 에서 받지 않는다 —
//   백엔드가 identityVerificationId 를 기준으로 PASS 인증 결과에서 인증된 휴대폰 번호를 조회해 사용한다
//   (프론트가 전달한 phoneNumber 는 신뢰하지 않는다 — identityVerificationId 는 백엔드 발급 값만 유효)
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값 검증은 Service Layer 에서 수행한다
//   (PinResetRequestDTO 와 동일 방식)
//
// 보안 규칙 (knowledge.md: identityVerificationId 원문 로그 출력 금지):
// - identityVerificationId 는 PASS 인증 요청용 임시값이므로 @ToString.Exclude 로 처리
//   (PinResetRequestDTO 와 동일 정책 — 로그 노출 시 인증 흐름 추적/재사용 시도 가능성 방지)
@Getter
@Setter
@ToString
public class PhoneChangeRequestDTO {

    /** Mock PASS 인증 완료 후 백엔드가 발급한 본인인증 ID (필수). 로그 출력 금지 */
    @ToString.Exclude
    private String identityVerificationId;
}
