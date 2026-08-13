package com.workit.domain.auth.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 비밀번호 재설정 - 아이디 존재 확인 요청
// API 스펙(docs): POST /api/v1/auth/password/check-id
// 필드: loginId
//
// - 비밀번호 찾기 첫 화면에서 입력한 로그인 ID(이메일 또는 휴대폰 번호)가
//   DB 에 등록된 ACTIVE 회원인지 확인한다
// - 존재하면 200 SUCCESS 로 PASS 본인인증 단계로 진행하고,
//   존재하지 않으면 USER_NOT_FOUND(404) 로 아이디 입력 화면에 안내한다
// - javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값 검증은 Service Layer 에서 수행한다
//   (find-id Request DTO 와 동일 방식)
// - password 원문이 없으므로 @ToString 노출 대상 민감정보 없음
@Getter
@Setter
@ToString
public class PasswordCheckIdRequestDTO {

    /** 로그인 ID — 이메일 또는 하이픈 없는 휴대폰 번호 둘 다 허용 (필수) */
    private String loginId;
}
