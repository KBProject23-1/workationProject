package com.workit.domain.user.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 내 프로필 조회 전용 VO (MyBatis ResultType — users + user_profile LEFT JOIN)
//
// users(이메일/이름/전화/상태)와 user_profile(닉네임/회사명)을 함께 조회한다.
// 개인정보 규칙 (knowledge.md):
// - email_encrypt/name_encrypt/phone_number_encrypt 는 AES 암호화본 — 복호화는 Service Layer 에서만 수행
// - 프로필 미등록 회원은 nickname/companyName 이 null (LEFT JOIN 결과)
// - Mapper 는 조회만 담당 (상태 판단/복호화는 Service)
@Getter
@Setter
@ToString
public class MyProfileVO {

    /** 회원 고유 번호 (users.id) */
    private Long userId;

    /** 회원 상태 (ACTIVE, PENDING, BLOCKED, WITHDRAWN) — ACTIVE 만 서비스 이용 허용 */
    private String status;

    /** 이메일 AES-256 암호화본 (응답은 Service 에서 복호화) */
    private String emailEncrypt;

    /** 이름 AES-256 암호화본 (응답은 Service 에서 복호화) */
    private String nameEncrypt;

    /** 휴대폰 번호 AES-256 암호화본 (응답은 Service 에서 복호화) */
    private String phoneNumberEncrypt;

    /** 닉네임 (user_profile.nickname — 프로필 미등록 시 null) */
    private String nickname;

    /** 소속 회사명 (user_profile.company_name — 미입력 시 null) */
    private String companyName;

    /** 프로필 고유 번호 (user_profile.id — 프로필 미등록 시 null) */
    private Long profileId;
}
