package com.workit.domain.auth.service;

// 비밀번호 재설정 임시 토큰 저장소 인터페이스
//
// 비밀번호 재설정 1단계(verify)에서 발급한 passwordResetToken(UUID)과
// 해당 토큰의 소유자(userId)를 5분 TTL 동안 보관한다.
// Redis 구현(RedisPasswordResetTokenStore)과 테스트용 인메모리 Mock 을 교체할 수 있도록 분리
// (RefreshTokenStore / SignupVerificationStore 와 동일 구조)
//
// Redis Key 규약 (knowledge.md Redis Key Convention 스타일):
//   password:reset:{passwordResetToken}
//
// Value 규약:
//   userId (회원 번호) — 개인정보가 아니며, 비밀번호 변경 대상 조회용
//
// 규칙 (knowledge.md Redis Rules):
// - 영구 데이터 저장 금지 — 반드시 TTL(5분) 부여
// - 회원 정보 원본 저장 금지 — token→userId 매핑만 보관
public interface PasswordResetTokenStore {

    /**
     * passwordResetToken → userId 매핑 저장 (기존 값은 덮어쓴다)
     *
     * @param passwordResetToken 발급한 1회성 UUID (key: password:reset:{token})
     * @param userId             토큰 소유자 회원 번호
     * @param ttlSeconds         만료 TTL(초) — docs 기준 5분 (300초)
     */
    void save(String passwordResetToken, Long userId, long ttlSeconds);

    /**
     * 토큰으로 소유자 userId 조회
     * - 저장된 값이 없으면(TTL 만료/사용 완료/위조 토큰) null 반환
     *
     * @return 저장된 userId, 없으면 null
     */
    Long find(String passwordResetToken);

    /** 토큰 삭제 — 비밀번호 변경 완료 후 1회성 폐기 */
    void delete(String passwordResetToken);

    /**
     * 재설정 토큰 TTL(초) — 구현체 설정값 노출
     * - Service 가 Redis 저장 시 이 값을 그대로 사용해 JWT 와 Redis 의 TTL 이
     *   분산되지 않도록 한다 (JwtTokenProvider.getRefreshTokenExpirationSeconds 와 동일 패턴)
     */
    long getTtlSeconds();
}
