package com.workit.domain.user.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 프로필 수정 요청 DTO
// API 스펙(docs): 내 프로필 정보 수정 (PATCH /api/v1/users/me) → body { nickname, companyName }
//
// 처리 규칙:
// - PATCH 방식 — nickname 또는 companyName 중 전달된 값만 수정 (전달되지 않은 필드는 기존 값 유지)
// - name/phoneNumber/email 은 Request Body 에서 받지 않는다 —
//   PASS 본인인증으로 저장된 기존 값(users.name_encrypt/phone_number_encrypt/email_encrypt)을
//   그대로 유지한다 (개인정보 수정은 재인증 API 경유 — knowledge.md)
// - nickname: user_profile.nickname VARCHAR(50) UNIQUE
// - companyName: user_profile.company_name VARCHAR(100)
//   - 값 전달: 해당 값으로 저장
//   - null/빈 값 전달: NULL 로 저장 (소속 회사 삭제) — companyNameProvided 로 전달 여부 구분
//
// javax.validation 의존성이 없는 프로젝트 구조이므로 수정 대상 필드 존재/길이 검증은
// Service Layer 에서 수행한다 (Auth 도메인 DTO 와 동일 방식)
@Getter
@Setter
@ToString
public class ProfileUpdateRequestDTO {

    /** 유저 닉네임 (선택 — PATCH: 전달 시에만 수정) */
    private String nickname;

    /** 소속 회사명 (선택 — PATCH: 전달 시에만 수정, null/빈 값이면 NULL 로 저장) */
    private String companyName;

    /**
     * companyName 이 요청 본문에 포함되었는지 여부 (명시적 null 포함).
     * - PATCH 부분 수정 구분용: 포함 → company_name 을 companyName 값(또는 NULL)으로 UPDATE,
     *   미포함 → 기존 company_name 유지.
     * - Jackson 역직렬화 시 setCompanyName 가 호출되면서 세팅되므로
     *   요청 본문에서 이 필드 자체는 받지 않는다 (@JsonIgnore).
     */
    @JsonIgnore
    private boolean companyNameProvided;

    /**
     * companyName 커스텀 setter — 값이 null 이어도(명시적 null) "전달됨"으로 기록한다.
     * Lombok @Setter 는 이미 정의된 메서드가 있으면 생성하지 않는다.
     */
    @JsonProperty("companyName")
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
        this.companyNameProvided = true;
    }
}
