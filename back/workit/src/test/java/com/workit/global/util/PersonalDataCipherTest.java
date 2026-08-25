package com.workit.global.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PersonalDataCipherTest {

    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef"; // 32바이트

    @BeforeAll
    static void setUp() {
        System.setProperty("personal.data.aes.key", TEST_KEY);
        PersonalDataCipher.reloadKey();
    }

    @AfterAll
    static void tearDown() {
        System.clearProperty("personal.data.aes.key");
        PersonalDataCipher.reloadKey();
    }

    @Test
    @DisplayName("암호화 → 복호화 왕복 시 원문이 복원된다")
    void encryptDecrypt_roundTrip() {
        String plain = "010-1234-5678";

        String encrypted = PersonalDataCipher.encrypt(plain);

        assertNotEquals(plain, encrypted);
        assertEquals(plain, PersonalDataCipher.decrypt(encrypted));
    }

    @Test
    @DisplayName("같은 평문도 매번 다른 암호문이 생성된다 (IV 재사용 없음)")
    void encrypt_producesDifferentCiphertext() {
        String plain = "홍길동";

        String encrypted1 = PersonalDataCipher.encrypt(plain);
        String encrypted2 = PersonalDataCipher.encrypt(plain);

        assertNotEquals(encrypted1, encrypted2);
        assertEquals(plain, PersonalDataCipher.decrypt(encrypted1));
        assertEquals(plain, PersonalDataCipher.decrypt(encrypted2));
    }

    @Test
    @DisplayName("한글·특수문자를 포함한 개인정보도 정상 처리된다")
    void encryptDecrypt_koreanAndSpecialChars() {
        String plain = "김워케이션!@# $%^";

        assertEquals(plain, PersonalDataCipher.decrypt(PersonalDataCipher.encrypt(plain)));
    }

    @Test
    @DisplayName("null 입력은 IllegalArgumentException을 발생시킨다")
    void encryptDecrypt_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> PersonalDataCipher.encrypt(null));
        assertThrows(IllegalArgumentException.class, () -> PersonalDataCipher.decrypt(null));
    }

    @Test
    @DisplayName("변조된 암호문은 GCM 무결성 검증으로 복호화에 실패한다")
    void decrypt_failsOnTamperedCiphertext() {
        String encrypted = PersonalDataCipher.encrypt("010-1234-5678");

        char first = encrypted.charAt(0);
        String tampered = (first == 'A' ? 'B' : 'A') + encrypted.substring(1);

        assertThrows(IllegalStateException.class, () -> PersonalDataCipher.decrypt(tampered));
    }

    @Test
    @DisplayName("형식이 잘못된 암호문(짧은 Base64)은 IllegalArgumentException을 발생시킨다")
    void decrypt_throwsOnMalformedCiphertext() {
        assertThrows(IllegalArgumentException.class, () -> PersonalDataCipher.decrypt("abcd"));
    }

    @Test
    @DisplayName("Base64로 인코딩된 32바이트 키도 정상 동작한다")
    void reloadKey_supportsBase64EncodedKey() {
        String base64Key = Base64.getEncoder()
                .encodeToString(TEST_KEY.getBytes(StandardCharsets.UTF_8));
        System.setProperty("personal.data.aes.key", base64Key);
        PersonalDataCipher.reloadKey();

        String plain = "identity-ci-value";
        assertEquals(plain, PersonalDataCipher.decrypt(
                PersonalDataCipher.encrypt(plain)));

        // 이후 테스트를 위해 원래 키로 복원
        System.setProperty("personal.data.aes.key", TEST_KEY);
        PersonalDataCipher.reloadKey();
    }

    @Test
    @DisplayName("32바이트가 아닌 키는 로드 시 IllegalStateException을 발생시킨다")
    void reloadKey_rejectsWrongLengthKey() {
        System.setProperty("personal.data.aes.key", "too-short-key");
        assertThrows(IllegalStateException.class, PersonalDataCipher::reloadKey);

        // 이후 테스트를 위해 원래 키로 복원
        System.setProperty("personal.data.aes.key", TEST_KEY);
        PersonalDataCipher.reloadKey();
    }

    @Test
    @DisplayName("키 미설정 시 사용 시점에 IllegalStateException을 발생시킨다")
    void encrypt_throwsWhenKeyMissing() {
        // 개발 머신에 AES 관련 환경변수가 설정돼 있으면 이 케이스를 검증할 수 없으므로 스킵
        String envPersonalKey = System.getenv("PERSONAL_DATA_AES_KEY");
        String envKey = System.getenv("AES_KEY");
        assumeTrue(isBlank(envPersonalKey) && isBlank(envKey),
                "AES 관련 환경변수가 설정되어 있어 키 미설정 케이스를 검증할 수 없음");

        System.clearProperty("personal.data.aes.key");
        PersonalDataCipher.reloadKey();

        assertThrows(IllegalStateException.class, () -> PersonalDataCipher.encrypt("010-0000-0000"));

        System.setProperty("personal.data.aes.key", TEST_KEY);
        PersonalDataCipher.reloadKey();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
