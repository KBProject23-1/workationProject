package com.workit.domain.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 회원가입 전용 임시 JWT Provider 단위 테스트
//
// 필수 테스트 (요구사항):
// 1. 정상 JWT 발급
// 2. JWT 검증 성공
// 3. 만료 JWT 실패
// 4. 잘못된 JWT 실패 (위변조 / 다른 시크릿 서명)
// 5. 개인정보가 Payload 에 포함되지 않는지 검증
class SignupTokenProviderTest {

    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef"; // 32바이트

    private SignupTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SignupTokenProvider(TEST_SECRET, 10);
    }

    @Test
    @DisplayName("정상 발급 - JWT 형식(3부분)으로 생성된다")
    void issue_success_format() {
        String token = provider.issue("temp-key-1");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("검증 성공 - 서명/만료 검증 통과, claims 에 temporaryUserKey/exp/sub 포함")
    void verify_success() {
        String token = provider.issue("temp-key-1");

        Claims claims = provider.verify(token);

        assertEquals(SignupTokenProvider.SUBJECT_SIGNUP_VERIFICATION, claims.getSubject());
        assertEquals("temp-key-1", claims.get("temporaryUserKey", String.class));
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(new Date()));
        assertNotNull(claims.getIssuedAt());
    }

    @Test
    @DisplayName("회원가입 전용 검증 성공 - verifySignupToken 은 sub 가 signup-verification 인 토큰만 통과")
    void verifySignupToken_success() {
        String token = provider.issue("temp-key-1");

        Claims claims = provider.verifySignupToken(token);

        assertEquals("temp-key-1", claims.get("temporaryUserKey", String.class));
    }

    @Test
    @DisplayName("회원가입 전용 검증 실패 - sub 가 다른 토큰(예: Access Token)은 거부된다")
    void verifySignupToken_rejectsNonSignupToken() {
        // 같은 시크릿으로 서명했지만 sub 만 다른 토큰 (Access Token 경로 오용 시나리오)
        String otherToken = Jwts.builder()
                .setSubject("access-token")
                .claim("temporaryUserKey", "temp-key-1")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        // raw verify 는 통과하지만 (서명/만료 정상) 회원가입 전용 검증은 거부된다
        assertNotNull(provider.verify(otherToken));
        assertThrows(JwtException.class, () -> provider.verifySignupToken(otherToken));
    }

    @Test
    @DisplayName("만료 JWT 실패 - 과거 만료 시각으로 발급한 토큰은 검증 시 예외 발생")
    void verify_expired_throws() {
        Date past = new Date(System.currentTimeMillis() - 60_000L);
        String token = provider.issue("temp-key-1", past);

        assertThrows(JwtException.class, () -> provider.verify(token));
    }

    @Test
    @DisplayName("위변조 JWT 실패 - 서명 부분을 변경하면 검증 시 예외 발생")
    void verify_tampered_throws() {
        String token = provider.issue("temp-key-1");
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThrows(JwtException.class, () -> provider.verify(tampered));
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 JWT 실패 - 위조 토큰은 검증 시 예외 발생")
    void verify_wrongSecret_throws() {
        SignupTokenProvider otherProvider =
                new SignupTokenProvider("fedcba9876543210fedcba9876543210", 10);
        String token = otherProvider.issue("temp-key-1");

        assertThrows(JwtException.class, () -> provider.verify(token));
    }

    @Test
    @DisplayName("개인정보 미포함 - Payload 에 name/phoneNumber/ci/encryptedCi 가 없다")
    void payload_containsNoPersonalInfo() {
        String token = provider.issue("temp-key-1");

        Claims claims = provider.verify(token);

        assertFalse(claims.containsKey("name"));
        assertFalse(claims.containsKey("phoneNumber"));
        assertFalse(claims.containsKey("birthDate"));
        assertFalse(claims.containsKey("ci"));
        assertFalse(claims.containsKey("encryptedCi"));
        assertFalse(claims.containsKey("password"));

        // 허용 claims 만 존재 (sub, temporaryUserKey, iat, exp)
        assertEquals(4, claims.size());
        assertTrue(claims.containsKey("temporaryUserKey"));
    }

    @Test
    @DisplayName("시크릿 미설정 - fail-fast (빈 값 + 환경변수 없음)")
    void secret_missing_throws() {
        // 비어 있는 jwt.secret 과 JWT_SECRET 환경변수가 없는 환경이라 가정할 수 없으므로,
        // 명시적으로 빈 시크릿을 주입했을 때 예외가 발생하는지 검증한다
        // (JWT_SECRET 환경변수가 실제로 설정된 CI 환경에서는 이 테스트가 실패할 수 있다)
        String envSecret = System.getenv("JWT_SECRET");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                envSecret == null || envSecret.trim().isEmpty(),
                "JWT_SECRET 환경변수가 설정되어 있어 빈 시크릿 검증을 건너뜁니다.");

        assertThrows(IllegalStateException.class, () -> new SignupTokenProvider("", 10));
    }

    @Test
    @DisplayName("TTL 검증 - 0 이하 TTL 은 허용되지 않는다")
    void invalidTtl_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SignupTokenProvider(TEST_SECRET, 0));
        assertThrows(IllegalArgumentException.class, () -> new SignupTokenProvider(TEST_SECRET, -1));
    }
}
