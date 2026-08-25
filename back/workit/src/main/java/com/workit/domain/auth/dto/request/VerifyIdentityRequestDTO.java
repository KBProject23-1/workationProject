package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 회원가입 본인인증 검증 및 회원 중복 체크 요청
// API 스펙(docs): POST /api/v1/auth/signup/verify-identity
// 필드: identityVerificationId
// - PASS 인증(POST /auth/pass) 완료 후 백엔드가 발급한 값 (프론트 생성 금지)
// - javax.validation 의존성이 없는 프로젝트 구조이므로 @NotBlank 같은 애노테이션 대신
//   Service Layer 에서 null/빈 값 검증을 수행한다 (기존 Request DTO 와 동일 방식)
@Getter
@Setter
@ToString
public class VerifyIdentityRequestDTO {

    /** Mock PASS 인증 완료 후 백엔드가 발급한 본인인증 고유 번호 */
    private String identityVerificationId;
}
