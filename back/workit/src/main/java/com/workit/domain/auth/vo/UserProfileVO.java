package com.workit.domain.auth.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// user_profile 테이블 매핑 VO (회원가입 완료 시 insert 용도)
// ERD 기준 컬럼: id, user_id, nickname, company_name, created_at, updated_at
// - nickname: UNIQUE — 회원가입 완료 시 중복 체크 후 저장
// - company_name: 선택 입력 — 회원가입 시점에는 저장하지 않는다
@Getter
@Setter
@ToString
public class UserProfileVO {

    /** 프로필 고유 번호(PK) — insert 후 MyBatis useGeneratedKeys 로 채워진다 */
    private Long id;

    /** 회원 고유 번호 (FK, users.id 참조, 1:1) */
    private Long userId;

    /** 닉네임 (UNIQUE) */
    private String nickname;

    /** 소속 회사명 (선택) */
    private String companyName;

    /** 프로필 생성 일시 */
    private LocalDateTime createdAt;

    /** 프로필 수정 일시 */
    private LocalDateTime updatedAt;
}
