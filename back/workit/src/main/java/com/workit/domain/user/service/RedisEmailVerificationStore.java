package com.workit.domain.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.Duration;

// 이메일 인증번호 Mock 저장소 Redis 구현
//
// Key 규약 (docs: "서버가 userId + email + 인증번호 저장"): mock:email-verification:{userId}
//   - userId 는 로그인 사용자 고유 번호 — 이메일 변경 API 는 Request Body 를 받지 않으므로
//     현재 사용자의 인증 완료 정보를 userId 로만 조회할 수 있어야 한다 (docs)
//   - 인증 대상 이메일 원문은 key 에 노출하지 않는다 (세션 내부 AES-256 암호화본으로만 보관)
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

    /** docs: mock:email-verification:{userId} */
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
    public void save(Long userId, EmailVerificationSession session) {
        try {
            redisTemplate.opsForValue()
                    .set(key(userId), OBJECT_MAPPER.writeValueAsString(session), ttl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이메일 인증번호 세션 직렬화에 실패했습니다.", e);
        }
    }

    @Override
    public EmailVerificationSession find(Long userId) {
        String json = redisTemplate.opsForValue().get(key(userId));
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
    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    @Override
    public Duration getTtl() {
        return ttl;
    }

    /**
     * Redis key 생성 — 로그인 사용자 id 기반 (docs: 서버가 userId + email + 인증번호 저장)
     * - 이메일 변경 API 는 Request Body 를 받지 않으므로 userId 로만 조회할 수 있어야 한다
     * - 인증 대상 이메일 원문은 key 에 노출하지 않는다 (세션 내부 암호화본만 보관)
     */
    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
