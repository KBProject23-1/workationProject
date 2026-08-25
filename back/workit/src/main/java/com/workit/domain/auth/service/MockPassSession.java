package com.workit.domain.auth.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Mock PASS 인증 세션 데이터
// POST /api/v1/auth/pass → Redis(mock:pass:{identityVerificationId}) 저장 → 회원가입/아이디 찾기 등에서 사용
//
// - identityVerificationId 는 MockPassService 가 생성한다 (프론트 생성/전달 금지)
// - CI 도 MockPassService 가 생성해 세션에 보관한다 (실제 PASS 응답 흉내)
//
// knowledge.md Personal Information / Redis Rules 준수:
// - name / phoneNumber / CI 는 개인정보 → 원문 저장 금지, AES-256 암호화본만 저장
// - identityVerificationId / status / used 는 개인정보가 아니므로 평문 보관
// - Redis TTL(기본 10분) 동안만 유지되는 1회성 데이터 (영구 저장 금지)
// - used: 회원가입 등으로 사용 완료 처리된 세션은 재사용 불가 (1회성 인증 보장)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockPassSession {

    /** Mock 인증 고유 번호 (MockPassService 가 UUID 로 생성) */
    private String identityVerificationId;

    /** 세션 상태 (MockPassStatus enum name — Jackson 직렬화 단순화를 위해 String) */
    private String status;

    /** 이름 AES-256 암호화본 (Redis 에 개인정보 원문 저장 금지) */
    private String encryptedName;

    /** 휴대폰 번호 AES-256 암호화본 (개인정보) */
    private String encryptedPhone;

    /** CI AES-256 암호화본 (개인식별값 — 개인정보) */
    private String encryptedCi;

    /** 사용 완료 여부 — true 면 재사용 불가 (1회성 인증) */
    private boolean used;
}
