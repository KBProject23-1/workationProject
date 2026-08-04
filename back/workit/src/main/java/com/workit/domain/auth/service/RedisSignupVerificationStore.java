package com.workit.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workit.domain.auth.util.SignupTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

// Redis 기반 회원가입 임시 데이터 저장소
//
// Key 규약 (knowledge.md Redis Key Convention 스타일):
//   signup:verification:{temporaryUserKey}
// Value: SignupVerificationData 를 JSON 직렬화한 문자열
// TTL : signup.token.ttl.minutes (기본 10분) — 회원가입 완료 시점까지의 짧은 유지 시간
//
// 규칙:
// - RedisTemplate 은 기존 RedisConfig 의 String/String 템플릿 재사용 (중복 생성 금지)
// - 회원가입 임시 데이터만 보관 — 금융 거래/회원 원본 데이터 저장 금지
// - 회원가입 완료 API(#66)에서 find 후 사용이 끝나면 delete 로 폐기한다
@Component
public class RedisSignupVerificationStore implements SignupVerificationStore {

    private static final String KEY_PREFIX = "signup:verification:";

    // 설정 후 스레드 안전 (JVM 당 인스턴스 하나만 사용)
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration ttl;

    // TTL 프로퍼티 키는 SignupTokenProvider 와 공유 (JWT exp 와 Redis TTL 이 분산되지 않도록 단일 소스)
    public RedisSignupVerificationStore(RedisTemplate<String, String> redisTemplate,
                                        @Value("${" + SignupTokenProvider.TTL_MINUTES_PROPERTY + ":10}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public void save(String temporaryUserKey, SignupVerificationData data) {
        try {
            redisTemplate.opsForValue()
                    .set(KEY_PREFIX + temporaryUserKey, OBJECT_MAPPER.writeValueAsString(data), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("회원가입 임시 데이터 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public SignupVerificationData find(String temporaryUserKey) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + temporaryUserKey);
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, SignupVerificationData.class);
        } catch (IOException e) {
            throw new IllegalStateException("회원가입 임시 데이터 역직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public void delete(String temporaryUserKey) {
        redisTemplate.delete(KEY_PREFIX + temporaryUserKey);
    }
}
