package com.workit.domain.auth.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 회원가입 임시 검증 데이터
// verify-identity → Redis(signup:verification:{temporaryUserKey}) 임시 저장 → 회원가입 완료 시 복원
//
// knowledge.md Personal Information / Redis Rules:
// - CI 원문 저장 금지 → AES-256 암호화본(encryptedCi)만 저장
// - name(개인정보) 원문 저장 금지 → AES-256 암호화본(encryptedName)만 저장
// - verificationId(PortOne 인증 고유 번호), ciHash(SHA-256)는 개인정보가 아니므로 평문 보관
// - Redis TTL(기본 10분) 동안만 유지되는 1회성 데이터 (영구 저장 금지)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupVerificationData {

    /** 포트원 본인인증 고유 번호 (imp_ver_...) */
    private String verificationId;

    /** CI SHA-256 해시 — 중복 가입 재검증용 (회원가입 완료 시 DB 저장 전 재검증에 사용) */
    private String ciHash;

    /** CI AES-256 암호화본 (Redis 에 CI 원문 저장 금지) */
    private String encryptedCi;

    /** 이름 AES-256 암호화본 (Redis 에 개인정보 원문 저장 금지) */
    private String encryptedName;

    /** 휴대폰 번호 AES-256 암호화본 (Redis 에 개인정보 원문 저장 금지) */
    private String encryptedPhone;
}
