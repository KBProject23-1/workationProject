package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 본인인증(PASS) 검증 요청
// API 스펙(docs): identityVerificationId
// - javax.validation 의존성이 없는 프로젝트 구조이므로 @NotBlank 같은 애노테이션 대신
//   Service Layer 에서 null/빈 값 검증을 수행한다 (MockIdentityVerificationProvider 와 동일 방식)
@Getter
@Setter
@ToString
public class VerifyIdentityRequestDTO {

    /** 포트원 본인인증 고유 번호 (imp_ver_...) */
    private String identityVerificationId;
}
