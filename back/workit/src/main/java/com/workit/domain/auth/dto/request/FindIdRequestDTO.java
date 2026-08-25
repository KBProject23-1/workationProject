package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 아이디 찾기 요청
// API 스펙(docs): POST /api/v1/auth/find-id
// 필드: identityVerificationId
//
// - 아이디(이메일)를 분실한 유저가 PASS 본인인증을 완료한 뒤,
//   본인인증 고유 번호(identityVerificationId)로 가입 이메일을 조회한다
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값 검증은 Service Layer 에서 수행한다
// - identityVerificationId 는 POST /auth/pass 가 발급한 값 (Mock PASS — 백엔드 생성)
@Getter
@Setter
@ToString
public class FindIdRequestDTO {

    /** Mock PASS 인증 후 백엔드가 발급한 본인인증 고유 번호 — 필수 */
    private String identityVerificationId;
}
