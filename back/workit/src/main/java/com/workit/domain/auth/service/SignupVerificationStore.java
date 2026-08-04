package com.workit.domain.auth.service;

// 회원가입 임시 데이터 저장소 인터페이스
//
// Redis 구현(RedisSignupVerificationStore)과 테스트용 인메모리 Fake 를 교체할 수 있도록 분리
// (Mockito 미사용 프로젝트 특성상 수동 Fake 주입 용이)
//
// Redis Key 규약 (knowledge.md Redis Key Convention):
//   signup:verification:{temporaryUserKey}
public interface SignupVerificationStore {

    /** 임시 데이터 저장 — TTL 만료 시 자동 삭제 */
    void save(String temporaryUserKey, SignupVerificationData data);

    /** 임시 데이터 조회 — 없거나 TTL 만료로 삭제된 경우 null */
    SignupVerificationData find(String temporaryUserKey);

    /** 임시 데이터 삭제 — 회원가입 완료 후 폐기 */
    void delete(String temporaryUserKey);
}
