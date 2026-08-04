package com.workit.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncryptorTest {

    @Test
    @DisplayName("encode는 평문과 다른 BCrypt 해시를 생성한다")
    void encode_producesHashDifferentFromPlain() {
        String hash = PasswordEncryptor.encode("password123!");

        assertNotNull(hash);
        assertNotEquals("password123!", hash);
        assertTrue(hash.startsWith("$2")); // $2a$/$2b$/$2y$ BCrypt 포맷
    }

    @Test
    @DisplayName("matches는 올바른 평문에 대해 true를 반환한다")
    void matches_returnsTrue_forCorrectPassword() {
        String hash = PasswordEncryptor.encode("password123!");

        assertTrue(PasswordEncryptor.matches("password123!", hash));
    }

    @Test
    @DisplayName("matches는 잘못된 평문에 대해 false를 반환한다")
    void matches_returnsFalse_forWrongPassword() {
        String hash = PasswordEncryptor.encode("password123!");

        assertFalse(PasswordEncryptor.matches("wrong-password", hash));
    }

    @Test
    @DisplayName("같은 평문도 salt가 달라 매번 서로 다른 해시가 생성된다")
    void encode_generatesDifferentHashPerCall() {
        String hash1 = PasswordEncryptor.encode("same-password");
        String hash2 = PasswordEncryptor.encode("same-password");

        assertNotEquals(hash1, hash2);
        assertTrue(PasswordEncryptor.matches("same-password", hash1));
        assertTrue(PasswordEncryptor.matches("same-password", hash2));
    }

    @Test
    @DisplayName("PIN 6자리 숫자도 동일하게 해시/검증된다")
    void encode_matches_forPin() {
        String pinHash = PasswordEncryptor.encode("123456");

        assertNotEquals("123456", pinHash);
        assertTrue(PasswordEncryptor.matches("123456", pinHash));
        assertFalse(PasswordEncryptor.matches("654321", pinHash));
    }

    @Test
    @DisplayName("null 입력은 IllegalArgumentException을 발생시키고 matches는 false를 반환한다")
    void nullInput_handledSafely() {
        assertThrows(IllegalArgumentException.class, () -> PasswordEncryptor.encode(null));

        assertFalse(PasswordEncryptor.matches(null, "hash"));
        assertFalse(PasswordEncryptor.matches("raw", null));
    }

    @Test
    @DisplayName("BCrypt 형식이 아닌 해시(DB 손상 등)에 대한 matches는 예외 없이 false를 반환한다")
    void matches_withMalformedHash_returnsFalse() {
        // 주의: Spring Security 5.7.x 는 $2[ayb]$ 패턴이 아닌 해시에 대해 false 를 반환한다.
        // 버전 업그레이드 시 throws 로 바뀔 수 있으므로 서비스 호출부에서 예외 처리(400)에 유의한다.
        assertFalse(PasswordEncryptor.matches("password123!", "not-a-bcrypt-hash"));
        assertFalse(PasswordEncryptor.matches("password123!", "plaintext"));
    }
}
