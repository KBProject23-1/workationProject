package com.workit.domain.auth.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// users 테이블 매핑 VO (회원가입 완료 시 insert 용도)
// ERD 기준 컬럼: id, email_hash, email_encrypt, name_encrypt, phone_number_hash,
//                phone_number_encrypt, status, created_at, updated_at, deleted_at
//
// 개인정보 처리 규칙 (knowledge.md):
// - email_hash / phone_number_hash : SHA-256 해시 (검색용)
// - email_encrypt / name_encrypt / phone_number_encrypt : AES-256 암호화본
// - 원문 컬럼 없음 — 원문 검색/저장 금지
@Getter
@Setter
@ToString
public class UserVO {

    /** 회원 고유 번호(PK) — insert 후 MyBatis useGeneratedKeys 로 채워진다 */
    private Long id;

    /** 이메일 SHA-256 해시 (UNIQUE) */
    private String emailHash;

    /** 이메일 AES-256 암호화본 */
    private String emailEncrypt;

    /** 이름 AES-256 암호화본 */
    private String nameEncrypt;

    /** 휴대폰 번호 SHA-256 해시 (UNIQUE) */
    private String phoneNumberHash;

    /** 휴대폰 번호 AES-256 암호화본 */
    private String phoneNumberEncrypt;

    /** 회원 상태 (ACTIVE, PENDING, BLOCKED, WITHDRAWN) */
    private String status;

    /** 계정 생성 시간 */
    private LocalDateTime createdAt;

    /** 계정 수정 시간 */
    private LocalDateTime updatedAt;

    /** 계정 삭제 시간 */
    private LocalDateTime deletedAt;
}
