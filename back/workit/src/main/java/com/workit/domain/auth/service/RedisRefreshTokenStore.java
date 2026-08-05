package com.workit.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

// Refresh Token Redis 저장소
// - RedisTemplate 은 기존 RedisConfig 의 String/String 템플릿 재사용 (중복 생성 금지)
//
// Key 규약 (knowledge.md): refresh:token:{userId}
// Value 규약: Refresh Token 의 SHA-256 hash (원문 저장 금지)
// TTL: Refresh Token 만료 시간과 동일 (jwt.refresh-token-expiration)
@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    /** knowledge.md Redis Key Convention: refresh:token:{userId} */
    private static final String KEY_PREFIX = "refresh:token:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(Long userId, String refreshTokenHash, long ttlSeconds) {
        redisTemplate.opsForValue()
                .set(KEY_PREFIX + userId, refreshTokenHash, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public void delete(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
