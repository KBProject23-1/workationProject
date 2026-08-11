package com.workit.domain.auth.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Mock PASS 인증 세션 데이터
// POST /api/v1/auth/pass → Redis(mock:pass:{identityVerificationId}) 저장 → verify-identity 가 사용
//
// knowledge.md Personal Information / Redis Rules 준수:
// - name / phoneNumber 는 개인정보 → 원문 저장 금지, AES-256 암호화본만 저장
// - identityVerificationId / status 는 개인정보가 아니므로 평문 보관
// - Redis TTL(기본 10분) 동안만 유지되는 1회성 데이터 (영구 저장 금지)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockPassSession {

    /** Mock 인증 고유 번호 (mock-xxxxxxxx — 프론트가 생성해 백엔드로 전송) */
    private String identityVerificationId;

    /** 세션 상태 (MockPassStatus enum name — Jackson 직렬화 단순화를 위해 String) */
    private String status;

    /** 이름 AES-256 암호화본 (Redis 에 개인정보 원문 저장 금지) */
    private String encryptedName;

    /** 휴대폰 번호 AES-256 암호화본 (개인정보) */
    private String encryptedPhone;
}
