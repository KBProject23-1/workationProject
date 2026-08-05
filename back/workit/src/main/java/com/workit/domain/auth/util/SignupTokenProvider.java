package com.workit.domain.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// 회원가입 전용 임시 인증 토큰(JWT) Provider
//
// Access/Refresh Token 과 분리된 회원가입 플로우 전용 토큰을 발급/검증한다.
// - 용도: PASS 본인인증 통과 후 회원가입 완료 API(#66)까지의 유효성 증명
// - Access Token 과 별개이며, 회원가입 완료 시 검증용으로만 사용한다
//
// Payload 최소화 (knowledge.md JWT Rules):
//   sub             : 용도 구분 마커 (signup-verification) — 추후 Access Token 경로에서
//                     회원가입 토큰이 인증 토큰으로 오용되는 것을 차단하기 위함
//   temporaryUserKey: Redis(signup:verification:{key})에서 임시 데이터를 찾기 위한 키
//   iat / exp       : 발급/만료 시각
//
//   (CI 해시를 포함한 인증 관련 데이터는 Payload 에 담지 않고 Redis 임시 저장소에 보관한다)
//
// 금지 (knowledge.md Personal Information Policy):
//   - name, phoneNumber, CI 원문, AES 암호화된 CI 등 어떤 개인정보도 Payload 에 넣지 않는다
//   - CI 복원에 필요한 데이터는 Redis(RedisSignupVerificationStore)에 암호화본으로 임시 저장한다
//
// Secret 관리:
//   - 하드코딩 금지 — application-secret.properties 의 jwt.secret 값을 우선 사용
//   - 값이 없으면 환경변수 JWT_SECRET 로 대체, 둘 다 없으면 fail-fast
//   - HS256 서명이므로 최소 32바이트(256-bit) 키 필요
@Component
public class SignupTokenProvider {

    /** 토큰 용도 구분용 sub 값 (회원가입 임시 인증) */
    public static final String SUBJECT_SIGNUP_VERIFICATION = "signup-verification";

    private static final String CLAIM_TEMPORARY_USER_KEY = "temporaryUserKey";

    /** JWT 시크릿 프로퍼티 키 (application-secret.properties, gitignored) */
    public static final String JWT_SECRET_PROPERTY = "jwt.secret";

    /** 회원가입 임시 토큰 TTL 공용 프로퍼티 키 — JWT exp 와 Redis TTL 이 동일 값을 쓰도록 단일 소스로 관리 */
    public static final String TTL_MINUTES_PROPERTY = "signup.token.ttl.minutes";

    private final SecretKey signingKey;
    private final long ttlMinutes;

    public SignupTokenProvider(@Value("${" + JWT_SECRET_PROPERTY + ":}") String secret,
                               @Value("${" + TTL_MINUTES_PROPERTY + ":10}") long ttlMinutes) {
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("회원가입 임시 토큰 TTL은 양수여야 합니다.");
        }
        this.ttlMinutes = ttlMinutes;
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(secret).getBytes(StandardCharsets.UTF_8));
    }

    /** 회원가입 임시 토큰 발급 — 만료 시각 = now + 설정된 TTL(기본 10분) */
    public String issue(String temporaryUserKey) {
        long now = System.currentTimeMillis();
        return issue(temporaryUserKey, new Date(now + ttlMinutes * 60_000L));
    }

    /**
     * 만료 시각을 직접 지정해 발급한다.
     * 주 용도는 issue(temporaryUserKey) 의 내부 구현이지만,
     * 테스트에서 만료 토큰(과거 시각)을 만들어 검증할 때도 사용한다.
     */
    public String issue(String temporaryUserKey, Date expiration) {
        return Jwts.builder()
                .setSubject(SUBJECT_SIGNUP_VERIFICATION)
                .claim(CLAIM_TEMPORARY_USER_KEY, temporaryUserKey)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 서명 및 만료를 검증하고 claims 를 반환한다.
     *
     * @throws JwtException 서명 불일치, 만료(ExpiredJwtException), 형식 오류 등 검증 실패 시
     *                      (회원가입 완료 API에서 ExpiredJwtException → EXPIRED_IDENTITY_TOKEN 매핑)
     */
    public Claims verify(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 회원가입 전용 토큰 검증 — 서명/만료 검증에 더해 sub claim 이
     * {@link #SUBJECT_SIGNUP_VERIFICATION} 인지 확인한다.
     * Access Token 경로에서 회원가입 토큰이 오용되는 것을 방지하기 위해
     * 회원가입 완료 API(#66)는 반드시 이 메서드를 사용해야 한다.
     *
     * @throws JwtException 검증 실패 또는 회원가입 전용 토큰이 아닌 경우
     */
    public Claims verifySignupToken(String token) {
        Claims claims = verify(token);
        if (!SUBJECT_SIGNUP_VERIFICATION.equals(claims.getSubject())) {
            throw new JwtException("회원가입 전용 임시 토큰이 아닙니다.");
        }
        return claims;
    }

    private static String resolveSecret(String secret) {
        String value = secret == null ? "" : secret.trim();
        if (value.isEmpty()) {
            value = System.getenv("JWT_SECRET");
        }
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT 시크릿이 설정되지 않았습니다. application-secret.properties 의 jwt.secret 또는 JWT_SECRET 환경변수를 확인하세요.");
        }
        value = value.trim();
        if (value.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT 시크릿은 HS256 서명을 위해 최소 32바이트(256-bit)여야 합니다.");
        }
        return value;
    }
}
