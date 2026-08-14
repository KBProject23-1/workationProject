package com.workit.domain.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

// 이메일 인증번호 Mock 저장소 Redis 구현
//
// Key 규약 (knowledge.md Redis Key Convention 스타일): mock:email-verification:{emailHash}
//   - emailHash 는 정규화된 이메일의 SHA-256 해시 — 이메일 원문을 Redis key 에 노출하지 않는다
//     (knowledge.md: 검색용 개인정보는 hash — users.email_hash 와 동일 원칙)
// Value: EmailVerificationSession 을 JSON 직렬화한 문자열 (email 은 AES-256 암호화본만 보관)
// TTL : mock.email-verification.ttl-minutes (기본 5분) — docs: 인증번호 유효시간 5분
//
// 규칙:
// - RedisTemplate 은 기존 RedisConfig 의 String/String 템플릿 재사용 (중복 생성 금지)
// - 같은 key 에 다시 save 하면 기존 인증번호가 덮어써져 무효화된다 (docs: 재발급 시 기존 인증번호 폐기)
// - Mock 인증 정보만 보관 — 회원 원본 데이터 저장 금지
// - EmailVerificationSession 은 이메일(개인정보)을 AES-256 암호화본으로만 저장한다
@Repository
public class RedisEmailVerificationStore implements EmailVerificationStore {

    /** 이메일 인증번호 TTL 프로퍼티 키 (단위: 분, 기본 5분) */
    public static final String TTL_MINUTES_PROPERTY = "mock.email-verification.ttl.minutes";

    /** knowledge.md Redis Key Convention 스타일: mock:email-verification:{emailHash} */
    private static final String KEY_PREFIX = "mock:email-verification:";

    // 설정 후 스레드 안전 (JVM 당 인스턴스 하나만 사용)
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration ttl;

    public RedisEmailVerificationStore(RedisTemplate<String, String> redisTemplate,
                                       @Value("${" + TTL_MINUTES_PROPERTY + ":5}") long ttlMinutes) {
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("이메일 인증번호 TTL은 양수여야 합니다.");
        }
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Override
    public void save(String email, EmailVerificationSession session) {
        try {
            redisTemplate.opsForValue()
                    .set(key(email), OBJECT_MAPPER.writeValueAsString(session), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이메일 인증번호 세션 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public EmailVerificationSession find(String email) {
        String json = redisTemplate.opsForValue().get(key(email));
        if (json == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, EmailVerificationSession.class);
        } catch (IOException e) {
            // 손상된 값은 없는 것으로 간주 (Redis 내부 데이터)
            return null;
        }
    }

    @Override
    public void delete(String email) {
        redisTemplate.delete(key(email));
    }

    @Override
    public Duration getTtl() {
        return ttl;
    }

    /**
     * Redis key 생성 — 정규화된 이메일의 SHA-256 hash 사용 (이메일 원문 key 노출 금지)
     * - users.email_hash 와 동일한 hex 인코딩 (knowledge.md: 검색용 개인정보는 hash)
     */
    private String key(String email) {
        return KEY_PREFIX + sha256Hex(email);
    }

    private static String sha256Hex(String value) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
