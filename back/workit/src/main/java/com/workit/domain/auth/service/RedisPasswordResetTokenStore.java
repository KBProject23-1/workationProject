package com.workit.domain.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

// 비밀번호 재설정 임시 토큰 Redis 저장소
// - RedisTemplate 은 기존 RedisConfig 의 String/String 템플릿 재사용 (중복 생성 금지)
//
// Key 규약 (knowledge.md Redis Key Convention 스타일): password:reset:{passwordResetToken}
// Value: userId (회원 번호)
// TTL : auth.password-reset-token.ttl-minutes (기본 5분) — docs: 5분 유효
//
// 규칙:
// - 비밀번호 변경 1회성 허가증이므로 반드시 짧은 TTL 을 설정한다 (영구 저장 금지)
// - 회원 정보 원본 저장 금지 — token→userId 매핑만 보관
@Repository
public class RedisPasswordResetTokenStore implements PasswordResetTokenStore {

    /** 비밀번호 재설정 토큰 TTL 프로퍼티 키 (단위: 분, 기본 5분) */
    public static final String TTL_MINUTES_PROPERTY = "auth.password-reset-token.ttl-minutes";

    /** knowledge.md Redis Key Convention 스타일: password:reset:{token} */
    private static final String KEY_PREFIX = "password:reset:";

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration ttl;

    public RedisPasswordResetTokenStore(RedisTemplate<String, String> redisTemplate,
                                        @Value("${" + TTL_MINUTES_PROPERTY + ":5}") long ttlMinutes) {
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("비밀번호 재설정 토큰 TTL은 양수여야 합니다.");
        }
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public void save(String passwordResetToken, Long userId) {
        // TTL 은 생성자 설정값(기본 5분)을 내부 적용 — 호출부가 TTL 을 알 필요가 없다
        // (RedisMockPassStore 와 동일 패턴)
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + passwordResetToken, String.valueOf(userId), ttl);
    }

    @Override
    public Long find(String passwordResetToken) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + passwordResetToken);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            // 손상된 값은 없는 것으로 간주 (Redis 내부 데이터 — 로그 외 노출 금지)
            return null;
        }
    }

    @Override
    public void delete(String passwordResetToken) {
        redisTemplate.delete(KEY_PREFIX + passwordResetToken);
    }
}
