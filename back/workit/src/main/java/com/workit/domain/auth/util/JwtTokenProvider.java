package com.workit.domain.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// 로그인 이후 공통으로 사용하는 JWT Provider
//
// Access Token / Refresh Token 발급·검증을 담당한다.
// - Access Token : API 인증 (Authorization: Bearer {accessToken}), 짧은 만료(기본 15분)
// - Refresh Token: Access Token 재발급, 긴 만료(기본 14일), HttpOnly Cookie 저장 예정
//                  (Provider 는 Token 생성만 담당 — Cookie 생성은 로그인 API 단계에서 처리)
//
// SignupTokenProvider(회원가입 전용 임시 토큰)와 역할이 분리되어 있다.
// - SignupTokenProvider: 회원가입 플로우의 임시 인증 토큰 (sub = signup-verification)
// - JwtTokenProvider    : 로그인 이후 인증 토큰 (sub = userId, tokenType = ACCESS/REFRESH)
//
// Claim 정책 (knowledge.md JWT Rules — Payload 최소화, 개인정보 금지):
//   sub      : userId (Long → String)
//   role     : 권한 (DB role 컬럼 도입 전까지 DEFAULT_ROLE = "ROLE_USER" 사용)
//   tokenType: 토큰 용도 구분 (ACCESS / REFRESH) — Refresh Token 이 Access Token 으로
//              오용되는 것을 차단하기 위해 추가 (SignupTokenProvider 의 sub 용도 구분 패턴과 동일)
//   iat / exp: 발급/만료 시각
//
// 절대 포함 금지 (knowledge.md Personal Information Policy):
//   password, pin, email, phoneNumber, CI 및 기타 개인정보는 Payload 에 넣지 않는다.
//
// Secret 관리 (SignupTokenProvider 와 동일 정책):
//   - 하드코딩 금지 — application-secret.properties 의 jwt.secret 값을 우선 사용
//   - 값이 없으면 환경변수 JWT_SECRET 로 대체, 둘 다 없으면 fail-fast
//   - HS256 서명이므로 최소 32바이트(256-bit) 키 필요
@Component
public class JwtTokenProvider {

    /** 기본 권한 — DB role 컬럼 도입 전까지 모든 사용자에게 부여 */
    public static final String DEFAULT_ROLE = "ROLE_USER";

    /** Access Token 용도 구분용 tokenType 값 */
    public static final String TOKEN_TYPE_ACCESS = "ACCESS";

    /** Refresh Token 용도 구분용 tokenType 값 */
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    /** JWT 시크릿 프로퍼티 키 (application-secret.properties, gitignored) — SignupTokenProvider 와 공유 */
    public static final String JWT_SECRET_PROPERTY = "jwt.secret";

    /** Access Token 만료(분) 프로퍼티 키 — 기본 15분 */
    public static final String ACCESS_TOKEN_EXPIRATION_PROPERTY = "jwt.access-token-expiration";

    /** Refresh Token 만료(분) 프로퍼티 키 — 기본 14일(20160분) */
    public static final String REFRESH_TOKEN_EXPIRATION_PROPERTY = "jwt.refresh-token-expiration";

    private final SecretKey signingKey;
    private final long accessTokenExpirationMinutes;
    private final long refreshTokenExpirationMinutes;

    public JwtTokenProvider(@Value("${" + JWT_SECRET_PROPERTY + ":}") String secret,
                            @Value("${" + ACCESS_TOKEN_EXPIRATION_PROPERTY + ":15}") long accessTokenExpirationMinutes,
                            @Value("${" + REFRESH_TOKEN_EXPIRATION_PROPERTY + ":20160}") long refreshTokenExpirationMinutes) {
        if (accessTokenExpirationMinutes <= 0) {
            throw new IllegalArgumentException("Access Token 만료 시간은 양수여야 합니다.");
        }
        if (refreshTokenExpirationMinutes <= 0) {
            throw new IllegalArgumentException("Refresh Token 만료 시간은 양수여야 합니다.");
        }
        this.accessTokenExpirationMinutes = accessTokenExpirationMinutes;
        this.refreshTokenExpirationMinutes = refreshTokenExpirationMinutes;
        this.signingKey = Keys.hmacShaKeyFor(resolveSecret(secret).getBytes(StandardCharsets.UTF_8));
    }

    /** Access Token 발급 — 만료 시각 = now + 설정된 TTL(기본 15분), 권한은 기본값(ROLE_USER) */
    public String createAccessToken(Long userId) {
        return createAccessToken(userId, DEFAULT_ROLE);
    }

    /** Access Token 발급 — 권한을 지정한다 (향후 role 컬럼 도입 시 사용) */
    public String createAccessToken(Long userId, String role) {
        return createToken(userId, role, TOKEN_TYPE_ACCESS,
                new Date(System.currentTimeMillis() + accessTokenExpirationMinutes * 60_000L));
    }

    /**
     * 만료 시각을 직접 지정해 Access Token 을 발급한다.
     * 주 용도는 createAccessToken(userId) 의 내부 구현이지만,
     * 테스트에서 만료 토큰(과거 시각)을 만들어 검증할 때도 사용한다.
     */
    public String createAccessToken(Long userId, Date expiration) {
        return createToken(userId, DEFAULT_ROLE, TOKEN_TYPE_ACCESS, expiration);
    }

    /** Refresh Token 발급 — 만료 시각 = now + 설정된 TTL(기본 14일), 권한은 기본값(ROLE_USER) */
    public String createRefreshToken(Long userId) {
        return createRefreshToken(userId, DEFAULT_ROLE);
    }

    /** Refresh Token 발급 — 권한을 지정한다 */
    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, TOKEN_TYPE_REFRESH,
                new Date(System.currentTimeMillis() + refreshTokenExpirationMinutes * 60_000L));
    }

    /**
     * 만료 시각을 직접 지정해 Refresh Token 을 발급한다.
     * 테스트에서 만료 토큰(과거 시각)을 만들어 검증할 때 사용한다.
     */
    public String createRefreshToken(Long userId, Date expiration) {
        return createToken(userId, DEFAULT_ROLE, TOKEN_TYPE_REFRESH, expiration);
    }

    private String createToken(Long userId, String role, String tokenType, Date expiration) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId는 양수여야 합니다.");
        }
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role == null || role.trim().isEmpty() ? DEFAULT_ROLE : role)
                .claim(CLAIM_TOKEN_TYPE, tokenType)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰 서명 및 만료를 검증하고 claims 를 반환한다.
     * (Signature / SecretKey / Token 형식 / 만료 여부 / Claim 추출 가능 여부 검증 포함)
     *
     * @throws JwtException 서명 불일치, 만료(ExpiredJwtException), 형식 오류 등 검증 실패 시
     *                      (로그인/필터/재발급 API에서 ExpiredJwtException → EXPIRED_TOKEN 매핑)
     */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Access Token 검증 — 서명/만료 검증에 더해 tokenType claim 이 ACCESS 인지 확인한다.
     * 인증 Filter 등 Access Token 경로는 반드시 이 메서드를 사용해야 한다.
     * (Refresh Token 을 API 인증에 오용하는 것을 차단)
     *
     * @throws JwtException 검증 실패 또는 Access Token 이 아닌 경우
     */
    public Claims parseAccessToken(String token) {
        Claims claims = parse(token);
        if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("Access Token이 아닙니다.");
        }
        return claims;
    }

    /**
     * Refresh Token 검증 — 서명/만료 검증에 더해 tokenType claim 이 REFRESH 인지 확인한다.
     * Refresh Token 재발급 API 는 반드시 이 메서드를 사용해야 한다.
     * (Access Token 으로 재발급을 요청하는 것을 차단)
     *
     * @throws JwtException 검증 실패 또는 Refresh Token 이 아닌 경우
     */
    public Claims parseRefreshToken(String token) {
        Claims claims = parse(token);
        if (!TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("Refresh Token이 아닙니다.");
        }
        return claims;
    }

    /**
     * 검증 후 토큰에서 userId(sub) 를 추출한다.
     *
     * @throws JwtException 검증 실패 또는 sub 가 없거나 숫자 형식이 아닌 경우
     */
    public Long extractUserId(String token) {
        String subject = parse(token).getSubject();
        if (subject == null || subject.trim().isEmpty()) {
            throw new JwtException("JWT Payload에 userId(sub)가 없습니다.");
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new JwtException("JWT Payload의 userId(sub) 형식이 올바르지 않습니다.");
        }
    }

    /** 검증 후 토큰에서 role claim 을 추출한다. */
    public String extractRole(String token) {
        return parse(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * 토큰 만료 여부 확인.
     * - 만료된 토큰이면 true 반환
     * - 서명/형식 오류는 예외로 전파 (만료 여부와 별개로 처리)
     */
    public boolean isExpired(String token) {
        try {
            Date expiration = parse(token).getExpiration();
            if (expiration == null) {
                throw new JwtException("JWT Payload에 만료 시각(exp)이 없습니다.");
            }
            return !expiration.after(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
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
