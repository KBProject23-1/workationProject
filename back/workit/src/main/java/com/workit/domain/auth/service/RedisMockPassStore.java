package com.workit.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.Duration;

// Redis 기반 Mock PASS 인증 세션 저장소
//
// Key 규약 (knowledge.md Redis Key Convention 스타일):
//   mock:pass:{identityVerificationId}
// Value: MockPassSession 을 JSON 직렬화한 문자열
// TTL : mock.pass.ttl.minutes (기본 10분) — 본인인증 흐름 완료까지의 짧은 유지 시간
//
// 규칙:
// - RedisTemplate 은 기존 RedisConfig 의 String/String 템플릿 재사용 (중복 생성 금지)
// - Mock 인증 세션만 보관 — 금융 거래/회원 원본 데이터 저장 금지
// - MockPassSession 은 개인정보(name/생년월일/휴대폰/CI)를 AES-256 암호화본으로만 저장한다
@Repository
public class RedisMockPassStore implements MockPassStore {

    /** Mock PASS 세션 TTL 프로퍼티 키 (단위: 분, 기본 10분) */
    public static final String TTL_MINUTES_PROPERTY = "mock.pass.ttl.minutes";

    /** knowledge.md Redis Key Convention 스타일: mock:pass:{identityVerificationId} */
    private static final String KEY_PREFIX = "mock:pass:";

    // 설정 후 스레드 안전 (JVM 당 인스턴스 하나만 사용)
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration ttl;

    public RedisMockPassStore(RedisTemplate<String, String> redisTemplate,
                              @Value("${" + TTL_MINUTES_PROPERTY + ":10}") long ttlMinutes) {
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("Mock PASS 세션 TTL은 양수여야 합니다.");
        }
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public void save(String identityVerificationId, MockPassSession session) {
        try {
            redisTemplate.opsForValue()
                    .set(KEY_PREFIX + identityVerificationId, OBJECT_MAPPER.writeValueAsString(session), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Mock PASS 세션 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public MockPassSession find(String identityVerificationId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + identityVerificationId);
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, MockPassSession.class);
        } catch (IOException e) {
            // 손상된 값은 없는 것으로 간주 (Redis 내부 데이터)
            return null;
        }
    }

    @Override
    public void markUsed(String identityVerificationId) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + identityVerificationId);
        if (json == null) {
            // 세션 없음(사용 전 만료/삭제) — 처리할 대상이 없다 (no-op)
            return;
        }
        try {
            MockPassSession session = OBJECT_MAPPER.readValue(json, MockPassSession.class);
            session.setUsed(true);
            // used=true 반영 후 동일 TTL 로 재저장 — 1회성 인증이므로 TTL 갱신은 문제되지 않는다
            redisTemplate.opsForValue().set(KEY_PREFIX + identityVerificationId,
                    OBJECT_MAPPER.writeValueAsString(session), ttl);
        } catch (IOException e) {
            throw new IllegalStateException("Mock PASS 세션 사용 완료 처리에 실패했습니다.", e);
        }
    }

    @Override
    public void delete(String identityVerificationId) {
        redisTemplate.delete(KEY_PREFIX + identityVerificationId);
    }
}
