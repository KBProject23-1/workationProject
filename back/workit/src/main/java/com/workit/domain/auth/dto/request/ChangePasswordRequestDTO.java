package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 로그인 사용자 비밀번호 변경 요청
// API 스펙(docs): PATCH /api/v1/users/me/password
// 필드: currentPassword, newPassword
//
// - 로그인 사용자가 현재 비밀번호를 재입력하여 본인 인증을 수행한 뒤 새 비밀번호로 변경한다
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값 검증은 Service Layer 에서 수행한다
//   (PasswordResetRequestDTO 와 동일 방식)
// - currentPassword/newPassword 는 민감정보 — 로그/toString 노출 금지
//   (LoginRequestDTO.password / PasswordResetRequestDTO.newPassword 와 동일 정책)
@Getter
@Setter
@ToString
public class ChangePasswordRequestDTO {

    /** 현재 사용 중인 비밀번호 원문 — 로그/toString 노출 금지 */
    @ToString.Exclude
    private String currentPassword;

    /** 변경할 새 비밀번호 원문 — 로그/toString 노출 금지 */
    @ToString.Exclude
    private String newPassword;
}
