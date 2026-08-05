package com.workit.domain.auth.service;

// Refresh Token 저장소 인터페이스
//
// 로그인/재발급/로그아웃에서 공통으로 사용한다.
// Redis 구현(RedisRefreshTokenStore)과 테스트용 인메모리 Fake 를 교체할 수 있도록 분리
// (Mockito 미사용 프로젝트 특성상 수동 Fake 주입 용이)
//
// Redis Key 규약 (knowledge.md Redis Key Convention):
//   refresh:token:{userId}
//
// 저장 규칙 (knowledge.md Refresh Token Security):
// - value 는 Refresh Token 원문이 아닌 SHA-256 hash 만 저장한다
// - TTL 은 Refresh Token 만료 시간과 동일하게 설정한다
public interface RefreshTokenStore {

    /**
     * Refresh Token hash 저장 (기존 값은 덮어쓴다)
     *
     * @param userId          회원 번호
     * @param refreshTokenHash SHA-256(refreshToken)
     * @param ttlSeconds      만료 TTL(초) — jwt.refresh-token-expiration(분) 변환 값
     */
    void save(Long userId, String refreshTokenHash, long ttlSeconds);

    /**
     * 저장된 Refresh Token hash 조회
     * - 재발급 시 클라이언트가 보낸 토큰의 hash 와 비교해 재사용(reuse) 여부를 판단한다
     * - 저장된 값이 없으면(로그아웃/TTL 만료) null 반환
     *
     * @return 저장된 SHA-256 hash, 없으면 null
     */
    String find(Long userId);

    /** Refresh Token 삭제 — 로그아웃/탈퇴/재사용 감지 시 세션 무효화 */
    void delete(Long userId);
}
