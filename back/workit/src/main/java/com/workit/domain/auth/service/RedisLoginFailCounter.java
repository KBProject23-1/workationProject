package com.workit.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

// PIN 로그인 실패 횟수 Redis 저장소
// - RedisTemplate 은 기존 RedisConfig 의 String/String 템플릿 재사용 (중복 생성 금지)
//
// Key 규약 (knowledge.md): auth:fail:{userId}
// 설계:
// - INCR 로 원자적으로 증가 (동시 요청에도 횟수가 정확히 집계)
// - 영구 잠금 정책: TTL 을 설정하지 않아 자동 해제되지 않는다
//   (잠금 해제는 PIN 로그인 성공 시 reset() 또는 추후 PASS 본인인증 후 PIN 재설정 API 에서 처리)
@Repository
@RequiredArgsConstructor
public class RedisLoginFailCounter implements LoginFailCounter {

    /** knowledge.md Redis Key Convention: auth:fail:{userId} */
    private static final String KEY_PREFIX = "auth:fail:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public int getCount(Long userId) {
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // 손상된 값은 0 으로 간주 (로그 외 노출 금지)
            return 0;
        }
    }

    @Override
    public void increment(Long userId) {
        // 영구 잠금 — TTL 미설정 (자동 해제 없음)
        redisTemplate.opsForValue().increment(KEY_PREFIX + userId);
    }

    @Override
    public void reset(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }
}
