package com.workit.domain.user.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 이메일 인증번호 발송 Mock 데이터 (개발 환경용 임시 인증 정보)
// POST /api/v1/users/me/email/verification → Redis(mock:email-verification:{emailHash}) 저장
// → 이후 이메일 인증번호 확인 API 에서 조회/검증에 사용
//
// 관리 정보 (docs: Mock 인증번호 저장):
//   email / verificationCode / expiresAt / verified 여부
//
// knowledge.md Personal Information / Redis Rules 준수:
// - email 은 개인정보 → 원문 저장 금지, AES-256 암호화본만 저장 (MockPassSession 과 동일 패턴)
// - verificationCode 는 6자리 숫자 1회성 값 — 짧은 TTL(5분) 동안만 유지되므로 평문 보관
//   (docs: 인증번호를 평문으로 장기간 저장하지 않는다 — TTL 만료로 자동 삭제)
// - verified: 인증번호 확인 API 성공 시 true — 이후 이메일 변경 API 에서 인증 완료 여부 확인에 사용
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationSession {

    /** 인증 대상 이메일 AES-256 암호화본 (개인정보 — Redis 원문 저장 금지) */
    private String encryptedEmail;

    /** 6자리 숫자 인증번호 (5분 TTL 임시값) */
    private String verificationCode;

    /** 인증번호 만료 시각 (epoch millis) — 유효시간 5분 (docs) */
    private long expiresAt;

    /** 이메일 인증 완료 여부 — 인증번호 확인 성공 시 true */
    private boolean verified;
}
