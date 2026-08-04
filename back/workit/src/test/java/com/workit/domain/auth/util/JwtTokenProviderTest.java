package com.workit.domain.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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

// 로그인 이후 공통 JWT Provider 단위 테스트
//
// 필수 테스트 (요구사항):
// 1. Access Token 생성 성공
// 2. Refresh Token 생성 성공
// 3. Parsing 성공
// 4. userId 추출
// 5. role 추출
// 6. Signature 변조 실패
// 7. Secret 변경 실패
// 8. Access Token 만료
// 9. Refresh Token 만료
//
// 추가 검증:
// - tokenType claim 으로 Access/Refresh 용도 구분 및 상호 오용 차단
// - 개인정보가 Payload 에 포함되지 않는지 검증
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef"; // 32바이트
    private static final long USER_ID = 501L;

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(TEST_SECRET, 15, 20160);
    }

    @Test
    @DisplayName("Access Token 생성 성공 - JWT 3부분 형식, exp 는 약 15분")
    void createAccessToken_success() {
        String token = provider.createAccessToken(USER_ID);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        Claims claims = provider.parse(token);
        assertEquals(String.valueOf(USER_ID), claims.getSubject());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());

        // 만료 시각이 now + 15분 (설정값) 근처인지 확인
        long diff = claims.getExpiration().getTime() - System.currentTimeMillis();
        assertTrue(diff > 14 * 60_000L, "Access Token 만료는 약 15분이어야 합니다. 실제: " + diff + "ms");
        assertTrue(diff <= 15 * 60_000L, "Access Token 만료는 약 15분이어야 합니다. 실제: " + diff + "ms");
    }

    @Test
    @DisplayName("Refresh Token 생성 성공 - JWT 3부분 형식, exp 는 약 14일")
    void createRefreshToken_success() {
        String token = provider.createRefreshToken(USER_ID);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);

        Claims claims = provider.parse(token);
        assertEquals(String.valueOf(USER_ID), claims.getSubject());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());

        // 만료 시각이 now + 14일(20160분) 근처인지 확인
        long diff = claims.getExpiration().getTime() - System.currentTimeMillis();
        assertTrue(diff > 13 * 24 * 60 * 60_000L, "Refresh Token 만료는 약 14일이어야 합니다. 실제: " + diff + "ms");
        assertTrue(diff <= 14 * 24 * 60 * 60_000L, "Refresh Token 만료는 약 14일이어야 합니다. 실제: " + diff + "ms");
    }

    @Test
    @DisplayName("Parsing 성공 - 서명/만료 검증 통과 후 claims 반환")
    void parse_success() {
        String token = provider.createAccessToken(USER_ID);

        Claims claims = provider.parse(token);

        assertEquals(String.valueOf(USER_ID), claims.getSubject());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());
    }

    @Test
    @DisplayName("userId 추출 성공 - sub claim 을 Long 으로 반환")
    void extractUserId_success() {
        String token = provider.createAccessToken(USER_ID);

        assertEquals(USER_ID, provider.extractUserId(token).longValue());
    }

    @Test
    @DisplayName("role 추출 성공 - 기본값은 ROLE_USER, 지정 시 지정값 반환")
    void extractRole_success() {
        String defaultToken = provider.createAccessToken(USER_ID);
        assertEquals(JwtTokenProvider.DEFAULT_ROLE, provider.extractRole(defaultToken));

        String customToken = provider.createAccessToken(USER_ID, "ROLE_ADMIN");
        assertEquals("ROLE_ADMIN", provider.extractRole(customToken));
    }

    @Test
    @DisplayName("tokenType 구분 - Access 는 ACCESS, Refresh 는 REFRESH claim")
    void tokenType_claim() {
        String accessToken = provider.createAccessToken(USER_ID);
        String refreshToken = provider.createRefreshToken(USER_ID);

        assertEquals(JwtTokenProvider.TOKEN_TYPE_ACCESS,
                provider.parse(accessToken).get("tokenType", String.class));
        assertEquals(JwtTokenProvider.TOKEN_TYPE_REFRESH,
                provider.parse(refreshToken).get("tokenType", String.class));
    }

    @Test
    @DisplayName("용도 검증 성공 - parseAccessToken/parseRefreshToken 은 각 용도 토큰만 통과")
    void parseByType_success() {
        String accessToken = provider.createAccessToken(USER_ID);
        String refreshToken = provider.createRefreshToken(USER_ID);

        assertEquals(String.valueOf(USER_ID), provider.parseAccessToken(accessToken).getSubject(), "Access Token 검증");
        assertEquals(String.valueOf(USER_ID), provider.parseRefreshToken(refreshToken).getSubject(), "Refresh Token 검증");
    }

    @Test
    @DisplayName("용도 검증 실패 - Access Token 을 Refresh Token 으로 검증하면 거부")
    void parseRefreshToken_rejectsAccessToken() {
        String accessToken = provider.createAccessToken(USER_ID);

        // raw parse 는 통과하지만 (서명/만료 정상) Refresh Token 검증은 거부된다
        assertNotNull(provider.parse(accessToken));
        assertThrows(JwtException.class, () -> provider.parseRefreshToken(accessToken));
    }

    @Test
    @DisplayName("용도 검증 실패 - Refresh Token 을 Access Token 으로 검증하면 거부")
    void parseAccessToken_rejectsRefreshToken() {
        String refreshToken = provider.createRefreshToken(USER_ID);

        assertNotNull(provider.parse(refreshToken));
        assertThrows(JwtException.class, () -> provider.parseAccessToken(refreshToken));
    }

    @Test
    @DisplayName("만료 확인 - isExpired 는 만료 토큰에 대해 true 반환")
    void isExpired_true() {
        Date past = new Date(System.currentTimeMillis() - 60_000L);
        String expiredAccess = provider.createAccessToken(USER_ID, past);
        String expiredRefresh = provider.createRefreshToken(USER_ID, past);

        assertTrue(provider.isExpired(expiredAccess));
        assertTrue(provider.isExpired(expiredRefresh));
    }

    @Test
    @DisplayName("만료 확인 - 유효 토큰에 대해 isExpired 는 false 반환")
    void isExpired_false() {
        String accessToken = provider.createAccessToken(USER_ID);
        String refreshToken = provider.createRefreshToken(USER_ID);

        assertFalse(provider.isExpired(accessToken));
        assertFalse(provider.isExpired(refreshToken));
    }

    @Test
    @DisplayName("Access Token 만료 - 과거 만료 시각으로 발급한 토큰은 검증 시 예외 발생")
    void accessToken_expired_throws() {
        Date past = new Date(System.currentTimeMillis() - 60_000L);
        String token = provider.createAccessToken(USER_ID, past);

        assertThrows(ExpiredJwtException.class, () -> provider.parse(token));
        assertThrows(ExpiredJwtException.class, () -> provider.parseAccessToken(token));
        assertThrows(JwtException.class, () -> provider.extractUserId(token));
    }

    @Test
    @DisplayName("Refresh Token 만료 - 과거 만료 시각으로 발급한 토큰은 검증 시 예외 발생")
    void refreshToken_expired_throws() {
        Date past = new Date(System.currentTimeMillis() - 60_000L);
        String token = provider.createRefreshToken(USER_ID, past);

        assertThrows(ExpiredJwtException.class, () -> provider.parse(token));
        assertThrows(ExpiredJwtException.class, () -> provider.parseRefreshToken(token));
        assertThrows(JwtException.class, () -> provider.extractUserId(token));
    }

    @Test
    @DisplayName("Signature 변조 실패 - 서명 부분을 변경하면 검증 시 예외 발생")
    void parse_tampered_throws() {
        String token = provider.createAccessToken(USER_ID);
        // 끝에서 두 번째 base64 글자를 바꾼다.
        // 마지막 글자는 256비트 서명의 패딩 비트만 담고 있어 'a'→'b' 교체 시
        // 복호화된 서명이 동일해질 수 있어(플레이크) 반드시 유효 비트를 바꾸는 위치를 사용한다
        String tampered = token.substring(0, token.length() - 2)
                + (token.charAt(token.length() - 2) == 'a' ? "b" : "a")
                + token.charAt(token.length() - 1);

        assertThrows(JwtException.class, () -> provider.parse(tampered));
    }

    @Test
    @DisplayName("Secret 변경 실패 - 다른 시크릿으로 서명된 토큰은 검증 시 예외 발생")
    void parse_wrongSecret_throws() {
        JwtTokenProvider otherProvider =
                new JwtTokenProvider("fedcba9876543210fedcba9876543210", 15, 20160);
        String token = otherProvider.createAccessToken(USER_ID);

        assertThrows(JwtException.class, () -> provider.parse(token));
    }

    @Test
    @DisplayName("Token 형식 검증 - JWT 형식이 아닌 문자열은 예외 발생")
    void parse_malformedFormat_throws() {
        assertThrows(JwtException.class, () -> provider.parse("not-a-jwt"));
        assertThrows(JwtException.class, () -> provider.parse("header.payload"));
        // 빈 문자열은 jjwt 0.11.5 에서 IllegalArgumentException 을 던진다
        assertThrows(IllegalArgumentException.class, () -> provider.parse(""));
    }

    @Test
    @DisplayName("userId 추출 실패 - 서명은 유효하지만 sub 가 없는 토큰은 거부")
    void extractUserId_missingSub_throws() {
        // 같은 시크릿으로 서명했지만 sub claim 이 없는 토큰
        String tokenWithoutSub = Jwts.builder()
                .claim("role", JwtTokenProvider.DEFAULT_ROLE)
                .claim("tokenType", JwtTokenProvider.TOKEN_TYPE_ACCESS)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)),
                        SignatureAlgorithm.HS256)
                .compact();

        // raw parse 는 통과하지만 (서명/만료 정상) userId 추출은 거부된다
        assertNotNull(provider.parse(tokenWithoutSub));
        assertThrows(JwtException.class, () -> provider.extractUserId(tokenWithoutSub));
    }

    @Test
    @DisplayName("개인정보 미포함 - Payload 에 email/phoneNumber/ci/password 등이 없다")
    void payload_containsNoPersonalInfo() {
        String accessToken = provider.createAccessToken(USER_ID);
        String refreshToken = provider.createRefreshToken(USER_ID);

        Claims accessClaims = provider.parse(accessToken);
        Claims refreshClaims = provider.parse(refreshToken);

        for (Claims claims : new Claims[]{accessClaims, refreshClaims}) {
            assertFalse(claims.containsKey("name"));
            assertFalse(claims.containsKey("email"));
            assertFalse(claims.containsKey("phoneNumber"));
            assertFalse(claims.containsKey("birthDate"));
            assertFalse(claims.containsKey("ci"));
            assertFalse(claims.containsKey("encryptedCi"));
            assertFalse(claims.containsKey("password"));
            assertFalse(claims.containsKey("pin"));
        }

        // 허용 claims 만 존재 (sub, role, tokenType, iat, exp)
        assertEquals(5, accessClaims.size());
        assertEquals(5, refreshClaims.size());
    }

    @Test
    @DisplayName("시크릿 미설정 - fail-fast (빈 값 + 환경변수 없음)")
    void secret_missing_throws() {
        // 명시적으로 빈 시크릿을 주입했을 때 예외가 발생하는지 검증한다
        // (JWT_SECRET 환경변수가 실제로 설정된 CI 환경에서는 이 테스트가 실패할 수 있다)
        String envSecret = System.getenv("JWT_SECRET");
        org.junit.jupiter.api.Assumptions.assumeTrue(
                envSecret == null || envSecret.trim().isEmpty(),
                "JWT_SECRET 환경변수가 설정되어 있어 빈 시크릿 검증을 건너뜁니다.");

        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", 15, 20160));
    }

    @Test
    @DisplayName("만료 설정 검증 - 0 이하 만료 시간은 허용되지 않는다")
    void invalidExpiration_throws() {
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(TEST_SECRET, 0, 20160));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(TEST_SECRET, -1, 20160));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(TEST_SECRET, 15, 0));
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(TEST_SECRET, 15, -1));
    }

    @Test
    @DisplayName("userId 검증 - null 또는 0 이하 userId 로는 발급 불가")
    void invalidUserId_throws() {
        assertThrows(IllegalArgumentException.class, () -> provider.createAccessToken(null));
        assertThrows(IllegalArgumentException.class, () -> provider.createAccessToken(0L));
        assertThrows(IllegalArgumentException.class, () -> provider.createAccessToken(-1L));
        assertThrows(IllegalArgumentException.class, () -> provider.createRefreshToken(null));
        assertThrows(IllegalArgumentException.class, () -> provider.createRefreshToken(0L));
    }
}
