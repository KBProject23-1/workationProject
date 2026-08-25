package com.workit.domain.user.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 로그인 사용자 회원 탈퇴 요청
// API 스펙: DELETE /api/v1/users/me
// 필드: password
//
// - 로그인 사용자가 현재 비밀번호를 재입력하여 본인 인증(민감 작업 추가 인증)을 수행한다
//   (knowledge.md: Sensitive Action Verification — 회원 탈퇴는 현재 비밀번호 재입력 필요)
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값 검증은 Service Layer 에서 수행한다
//   (ChangePasswordRequestDTO 와 동일 방식)
// - password 는 민감정보 — 로그/toString 노출 금지
//   (LoginRequestDTO.password / ChangePasswordRequestDTO.currentPassword 와 동일 정책)
@Getter
@Setter
@ToString
public class UserWithdrawalRequestDTO {

    /** 현재 사용 중인 비밀번호 원문 — 로그/toString 노출 금지 */
    @ToString.Exclude
    private String password;
}
