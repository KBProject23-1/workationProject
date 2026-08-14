package com.workit.domain.user.service;

import java.time.Duration;

// 이메일 인증번호 Mock 저장소 인터페이스
//
// Redis 구현(RedisEmailVerificationStore)과 테스트용 인메모리 Fake 를 교체할 수 있도록 분리
// (MockPassStore / PasswordResetTokenStore 와 동일 구조)
//
// Redis Key 규약 (docs: "서버가 userId + email + 인증번호 저장" — 사용자 기준 단일 세션):
//   mock:email-verification:{userId}  — userId 는 로그인 사용자 고유 번호
//   (이메일 변경 API 는 Request Body 를 받지 않으므로, 현재 사용자의 인증 완료 정보를
//    userId 로만 조회할 수 있어야 한다 — docs: EmailVerificationStore 에서 현재 사용자 조회)
//   인증 대상 이메일 원문은 key 에 노출하지 않으며, 세션(EmailVerificationSession) 내부에
//   AES-256 암호화본으로만 보관한다 (knowledge.md: Redis 개인정보 원문 저장 금지)
//
// Value: EmailVerificationSession 을 JSON 직렬화한 문자열 (email 은 AES-256 암호화본만 보관)
//
// 규칙 (knowledge.md Redis Rules):
// - 영구 데이터 저장 금지 — 반드시 TTL 부여 (기본 5분 — docs: 인증번호 유효시간 5분)
// - 같은 사용자(key)에 다시 save 하면 기존 인증번호가 덮어써져 무효화된다
//   (docs: 재발급 시 기존 인증번호 폐기 — 사용자당 유효한 인증 세션은 하나)
// - 개인정보 원문 저장 금지 — EmailVerificationSession 은 이메일 AES-256 암호화본만 보관
public interface EmailVerificationStore {

    /**
     * 인증번호 세션 저장 — 같은 사용자의 기존 인증번호는 덮어써져 무효화된다
     * - TTL 은 구현체(RedisEmailVerificationStore 패턴)가 설정값(기본 5분)을 내부 적용한다
     *   — 호출부가 TTL 을 알 필요가 없어 Service 와 TTL 정책이 분산되지 않는다
     * - 인증 대상 이메일은 세션(encryptedEmail)에만 보관하며, key 는 userId 로만 구성한다
     *
     * @param userId  로그인 사용자 id (Redis key — docs: 서버가 userId + email + 인증번호 저장)
     * @param session 발급한 인증번호 세션 (이메일 AES-256 암호화본 포함)
     */
    void save(Long userId, EmailVerificationSession session);

    /**
     * 사용자 기준 인증번호 세션 조회
     * - 저장된 값이 없으면(TTL 만료/미발급) null 반환
     *
     * @param userId 로그인 사용자 id
     * @return 저장된 인증번호 세션, 없으면 null
     */
    EmailVerificationSession find(Long userId);

    /** 인증번호 세션 삭제 — 이메일 변경 완료 등 흐름 종료 시 1회성 폐기 (재사용 방지) */
    void delete(Long userId);

    /**
     * 저장소가 적용하는 인증번호 TTL (기본 5분)
     * - 만료 시각(expiresAt) 계산용 — TTL 정책은 저장소가 소유하므로 Service 가 하드코딩하지 않는다
     *
     * @return 인증번호 유효 시간
     */
    Duration getTtl();
}
