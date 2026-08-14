package com.workit.domain.user.service;

import java.time.Duration;

// 이메일 인증번호 Mock 저장소 인터페이스
//
// Redis 구현(RedisEmailVerificationStore)과 테스트용 인메모리 Fake 를 교체할 수 있도록 분리
// (MockPassStore / PasswordResetTokenStore 와 동일 구조)
//
// Redis Key 규약 (knowledge.md Redis Key Convention 스타일):
//   mock:email-verification:{emailHash}  — emailHash 는 정규화된 이메일의 SHA-256 해시
//   (이메일 원문을 Redis key 에 노출하지 않는다 — knowledge.md: 검색용 개인정보는 hash)
//
// Value: EmailVerificationSession 을 JSON 직렬화한 문자열 (email 은 AES-256 암호화본만 보관)
//
// 규칙 (knowledge.md Redis Rules):
// - 영구 데이터 저장 금지 — 반드시 TTL 부여 (기본 5분 — docs: 인증번호 유효시간 5분)
// - 같은 이메일(key)에 다시 save 하면 기존 인증번호가 덮어써져 무효화된다 (docs: 재발급 시 기존 인증번호 폐기)
// - 개인정보 원문 저장 금지 — EmailVerificationSession 은 이메일 AES-256 암호화본만 보관
public interface EmailVerificationStore {

    /**
     * 인증번호 세션 저장 — 같은 이메일의 기존 인증번호는 덮어써져 무효화된다
     * - TTL 은 구현체(RedisMockPassStore 패턴)가 설정값(기본 5분)을 내부 적용한다
     *   — 호출부가 TTL 을 알 필요가 없어 Service 와 TTL 정책이 분산되지 않는다
     *
     * @param email   인증 대상 이메일 (정규화된 값 — 구현체가 key 용 hash 를 생성)
     * @param session 발급한 인증번호 세션
     */
    void save(String email, EmailVerificationSession session);

    /**
     * 이메일 기준 인증번호 세션 조회
     * - 저장된 값이 없으면(TTL 만료/미발급) null 반환
     *
     * @param email 인증 대상 이메일 (정규화된 값)
     * @return 저장된 인증번호 세션, 없으면 null
     */
    EmailVerificationSession find(String email);

    /** 인증번호 세션 삭제 — 이메일 변경 완료 등 흐름 종료 시 1회성 폐기 */
    void delete(String email);

    /**
     * 저장소가 적용하는 인증번호 TTL (기본 5분)
     * - 만료 시각(expiresAt) 계산용 — TTL 정책은 저장소가 소유하므로 Service 가 하드코딩하지 않는다
     *
     * @return 인증번호 유효 시간
     */
    Duration getTtl();
}
