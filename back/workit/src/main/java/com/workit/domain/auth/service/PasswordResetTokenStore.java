package com.workit.domain.auth.service;

// 비밀번호 재설정 임시 토큰 저장소 인터페이스
//
// 비밀번호 재설정 1단계(verify)에서 발급한 passwordResetToken(UUID)과
// 해당 토큰의 소유자(userId)를 5분 TTL 동안 보관한다.
// Redis 구현(RedisPasswordResetTokenStore)과 테스트용 인메모리 Mock 을 교체할 수 있도록 분리
// (RefreshTokenStore / MockPassStore 와 동일 구조)
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
     * - TTL 은 구현체(RedisMockPassStore 패턴)가 설정값(기본 5분)을 내부 적용한다
     *   — 호출부가 TTL 을 알 필요가 없어 Service 와 TTL 정책이 분산되지 않는다
     *
     * @param passwordResetToken 발급한 1회성 UUID (key: password:reset:{token})
     * @param userId             토큰 소유자 회원 번호
     */
    void save(String passwordResetToken, Long userId);

    /**
     * 토큰으로 소유자 userId 조회
     * - 저장된 값이 없으면(TTL 만료/사용 완료/위조 토큰) null 반환
     *
     * @return 저장된 userId, 없으면 null
     */
    Long find(String passwordResetToken);

    /** 토큰 삭제 — 비밀번호 변경 완료 또는 흐름 무효 시 1회성 폐기 */
    void delete(String passwordResetToken);
}
