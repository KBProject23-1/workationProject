package com.workit.domain.auth.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// user_auth 테이블 매핑 VO (회원가입 완료 시 insert 용도)
// ERD 기준 컬럼: id, user_id, password_hash, identity_ci_hash, identity_ci_encrypt,
//                created_at, updated_at
//
// 개인정보 처리 규칙 (knowledge.md):
// - password_hash : BCrypt 단방향 해시 (복호화 불가)
// - identity_ci_hash : CI SHA-256 해시 (UNIQUE, 1인1계정 검증)
// - identity_ci_encrypt : CI AES-256 암호화본
@Getter
@Setter
@ToString
public class UserAuthVO {

    /** 인증 정보 고유 번호(PK) — insert 후 MyBatis useGeneratedKeys 로 채워진다 */
    private Long id;

    /** 회원 고유 번호 (FK, users.id 참조) */
    private Long userId;

    /** 비밀번호 BCrypt 해시 */
    private String passwordHash;

    /** CI SHA-256 해시 (UNIQUE) */
    private String identityCiHash;

    /** CI AES-256 암호화본 */
    private String identityCiEncrypt;

    /** 생성 시간 */
    private LocalDateTime createdAt;

    /** 수정 시간 */
    private LocalDateTime updatedAt;
}
