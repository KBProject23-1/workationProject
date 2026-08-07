package com.workit.domain.user.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 프로필 최초 등록 요청 DTO
// API 스펙(docs): POST /api/v1/users/me/onboarding → body { nickname, companyName }
//
// 처리 규칙:
// - name/phoneNumber 는 Request Body 에서 받지 않는다 — 회원가입 시 PASS 본인인증으로
//   DB 에 이미 저장된 값(users.name_encrypt/phone_number_encrypt)을 그대로 사용한다
// - nickname: 필수 (user_profile.nickname VARCHAR(50) UNIQUE)
// - companyName: 선택 (user_profile.company_name VARCHAR(100))
//
// javax.validation 의존성이 없는 프로젝트 구조이므로 필수 값/길이 검증은 Service Layer 에서 수행한다
// (Auth 도메인 DTO 와 동일 방식)
@Getter
@Setter
@ToString
public class ProfileOnboardingRequestDTO {

    /** 유저 닉네임 (필수) */
    private String nickname;

    /** 소속 회사명 (선택) */
    private String companyName;
}
