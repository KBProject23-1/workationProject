package com.workit.domain.auth.service;

// PIN 로그인 실패 횟수 저장소 인터페이스
//
// PIN 실패 횟수 제한 정책 (knowledge.md PIN Policy):
// - Redis 기반 실패 횟수 관리 필수
// - 최대 실패 횟수 초과 시 PIN 로그인 잠금
//
// Redis 구현(RedisLoginFailCounter)과 테스트용 인메모리 Fake 를 교체할 수 있도록 분리
//
// Redis Key 규약 (knowledge.md Redis Key Convention):
//   auth:fail:{userId}
public interface LoginFailCounter {

    /** 현재 실패 횟수 조회 (기록 없으면 0) */
    int getCount(Long userId);

    /**
     * 실패 횟수 1 증가 (원자적 INCR)
     * - 영구 잠금 정책: TTL 을 설정하지 않아 자동 해제되지 않는다
     *   (잠금 해제는 PIN 로그인 성공 시 초기화 또는 PASS 본인인증 후 PIN 재설정)
     */
    void increment(Long userId);

    /** 실패 횟수 초기화 — PIN 검증 성공 시 호출 */
    void reset(Long userId);
}
