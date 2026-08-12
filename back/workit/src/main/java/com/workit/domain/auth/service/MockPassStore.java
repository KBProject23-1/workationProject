package com.workit.domain.auth.service;

// Mock PASS 인증 세션 저장소 인터페이스
//
// Redis 구현(RedisMockPassStore)과 테스트용 인메모리 Fake 를 교체할 수 있도록 분리
// (PasswordResetTokenStore / RefreshTokenStore 와 동일 구조)
//
// Redis Key 규약 (knowledge.md Redis Key Convention 스타일):
//   mock:pass:{identityVerificationId}
//
// 규칙 (knowledge.md Redis Rules):
// - 영구 데이터 저장 금지 — 반드시 TTL 부여 (기본 10분)
// - 개인정보 원문 저장 금지 — MockPassSession 은 AES-256 암호화본만 보관
public interface MockPassStore {

    /** 인증 세션 저장 — TTL 만료 시 자동 삭제 */
    void save(String identityVerificationId, MockPassSession session);

    /** 인증 세션 조회 — 없거나 TTL 만료로 삭제된 경우 null */
    MockPassSession find(String identityVerificationId);

    /**
     * 인증 세션 사용 완료 처리 (used = true)
     * - 회원가입 등 인증 사용이 끝난 세션을 재사용할 수 없도록 한다 (1회성 인증)
     * - 세션이 없거나 이미 만료된 경우 no-op
     */
    void markUsed(String identityVerificationId);

    /** 인증 세션 삭제 */
    void delete(String identityVerificationId);
}
