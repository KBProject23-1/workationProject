package com.workit.domain.user.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// user_profile 테이블 매핑 VO (프로필 최초 등록 insert 용도)
// ERD 기준 컬럼: user_id(PK), nickname, company_name, created_at, updated_at
// - nickname: VARCHAR(50) UNIQUE — 최초 등록 시 중복 체크 후 저장
// - company_name: VARCHAR(100) NULL — 선택 입력
// - created_at/updated_at: DB 기본값(CURRENT_TIMESTAMP) 사용 — insert 하지 않는다
@Getter
@Setter
@ToString
public class UserProfileVO {

    /** 회원 고유 번호 (PK, FK, users.id 참조, 1:1) */
    private Long userId;

    /** 닉네임 (UNIQUE) */
    private String nickname;

    /** 소속 회사명 (선택) — 수정 시 null 이면 NULL(삭제) 저장 */
    private String companyName;

    /**
     * company_name UPDATE 포함 여부 (프로필 수정 전용)
     * - false: company_name 은 UPDATE 문에서 제외 (기존 값 유지 — PATCH 부분 수정)
     * - true: companyName 값(또는 null → NULL)으로 저장
     */
    private boolean updateCompanyName;
}
