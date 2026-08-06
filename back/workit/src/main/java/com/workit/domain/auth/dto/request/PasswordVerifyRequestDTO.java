package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 비밀번호 재설정 1단계 - 본인 확인 및 인증 토큰 발급 요청
// API 스펙(docs): POST /api/v1/auth/password/verify
// 필드: loginId, identityVerificationId
//
// - 비밀번호를 잃어버린 유저가 입력한 로그인 ID(이메일 또는 휴대폰 번호)와
//   PASS 본인인증 고유 번호를 받아 대조한다
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값 검증은 Service Layer 에서 수행한다
//   (find-id Request DTO 와 동일 방식)
// - password 원문이 없으므로 @ToString 노출 대상 민감정보 없음
@Getter
@Setter
@ToString
public class PasswordVerifyRequestDTO {

    /** 로그인 ID — 이메일 또는 하이픈 없는 휴대폰 번호 둘 다 허용 (필수) */
    private String loginId;

    /** 포트원 본인인증 고유 번호 (imp_ver_...) (필수) */
    private String identityVerificationId;
}
