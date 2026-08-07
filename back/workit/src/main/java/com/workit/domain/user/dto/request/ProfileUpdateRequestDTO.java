package com.workit.domain.user.dto.request;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 프로필 수정 요청 DTO
// API 스펙(docs): 내 프로필 정보 수정 (PATCH /api/v1/users/me) → body { nickname, companyName }
//
// 처리 규칙:
// - PATCH 방식 — nickname 또는 companyName 중 하나만 전달 가능 (전달된 값만 수정)
// - name/phoneNumber/email 은 Request Body 에서 받지 않는다 —
//   PASS 본인인증으로 저장된 기존 값(users.name_encrypt/phone_number_encrypt/email_encrypt)을
//   그대로 유지한다 (개인정보 수정은 재인증 API 경유 — knowledge.md)
// - nickname: user_profile.nickname VARCHAR(50) UNIQUE
// - companyName: user_profile.company_name VARCHAR(100)
//
// javax.validation 의존성이 없는 프로젝트 구조이므로 수정 대상 필드 존재/길이 검증은
// Service Layer 에서 수행한다 (Auth 도메인 DTO 와 동일 방식)
@Getter
@Setter
@ToString
public class ProfileUpdateRequestDTO {

    /** 유저 닉네임 (선택 — PATCH: 전달 시에만 수정) */
    private String nickname;

    /** 소속 회사명 (선택 — PATCH: 전달 시에만 수정) */
    private String companyName;
}
