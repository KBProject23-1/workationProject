package com.workit.domain.user.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 로그인 사용자 계정 설정 진입용 비밀번호 재인증 요청
// API 스펙: POST /api/v1/users/me/account/verify
// 필드: password
//
// - 로그인 사용자가 계정 설정 화면에 진입하기 전 현재 비밀번호를 한 번 더 입력하여 본인임을 확인한다
//   (knowledge.md: Sensitive Action Verification — 계정 설정 진입 본인 확인)
// - 재인증 성공 여부는 Redis/DB/Session 등에 별도로 저장하지 않으며, Access/Refresh Token 을
//   새로 발급하지도 않는다 — 계정 설정 화면 진입 확인 용도
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값 검증은 Service Layer 에서 수행한다
//   (UserWithdrawalRequestDTO 와 동일 방식)
// - password 는 민감정보 — 로그/toString 노출 금지
//   (LoginRequestDTO.password / UserWithdrawalRequestDTO.password 와 동일 정책)
@Getter
@Setter
@ToString
public class AccountPasswordVerifyRequestDTO {

    /** 현재 사용 중인 비밀번호 원문 — 로그/toString 노출 금지 */
    @ToString.Exclude
    private String password;
}
