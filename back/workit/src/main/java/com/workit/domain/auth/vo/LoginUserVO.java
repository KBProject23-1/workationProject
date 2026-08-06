package com.workit.domain.auth.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// 통합 로그인 전용 조회 VO (MyBatis ResultType)
//
// 로그인은 원문(email_encrypt/phone_encrypt)을 절대 조회하지 않고
// 검색용 SHA-256 hash(email_hash/phone_number_hash)와 device_id 만으로 회원을 찾는다
// (knowledge.md: email_encrypt/phone_encrypt 조회 금지).
//
// 조회 쿼리별로 채워지는 필드가 다르다:
// - findUserByEmailHash   : password_hash 채움 (user_auth JOIN)
// - findUserByPhoneHash   : password_hash 채움 (user_auth JOIN)
// - findUserByDeviceId    : pin_hash 채움 (user_device JOIN)
// 사용되지 않는 쪽의 필드는 null 이며 Service 는 해당 경로의 필드만 사용한다
//
// 보안 규칙:
// - password_hash / pin_hash 는 BCrypt 해시이므로 평문이 아니다 (로그 주의는 동일하게 요구됨)
// - name_encrypt 는 Service Layer 에서만 복호화한다 (Controller/Mapper 금지)
@Getter
@Setter
@ToString
public class LoginUserVO {

    /** 회원 고유 번호 (users.id) */
    private Long id;

    /** 회원 상태 (ACTIVE, PENDING, BLOCKED, WITHDRAWN) — ACTIVE 만 로그인 허용 */
    private String status;

    /** 이름 AES-256 암호화본 (응답 name 은 Service 에서 복호화) */
    private String nameEncrypt;

    /** 비밀번호 BCrypt 해시 (PASSWORD 로그인 검증용) */
    private String passwordHash;

    /** PIN BCrypt 해시 (PIN 로그인 검증용) */
    private String pinHash;

    /**
     * PASS 본인인증 CI SHA-256 해시 (user_auth.identity_ci_hash)
     * - 비밀번호 재설정(verify) 시 loginId 로 조회한 회원과 PASS 인증 결과의 CI 를 대조하기 위한 용도
     * - findUserByEmailHash / findUserByPhoneHash 에서 함께 조회된다 (로그인 검증에는 사용하지 않는다)
     */
    private String identityCiHash;
}
