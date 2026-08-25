package com.workit.global.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

// AES-256(GCM) 기반 개인정보 양방향 암호화 유틸
//
// 대상 개인정보 (knowledge.md): name, phone_number, birth_date, identity_ci
//
// 보안 설계:
// - AES/GCM/NoPadding: 암호화 + 무결성 검증(AEAD)을 한 번에 처리
// - 매 호출마다 12바이트 새 IV 생성 → 동일 평문도 매번 다른 암호문 (IV 재사용 금지 원칙)
// - 출력 형식: Base64(IV(12B) + ciphertext(포함 tag))
//
// 키 로드 규칙 (우선순위):
//   1. 시스템 프로퍼티  personal.data.aes.key   (테스트용)
//   2. 환경변수         PERSONAL_DATA_AES_KEY
//   3. 환경변수         AES_KEY
// 키 값: 32바이트(256-bit) raw 문자열 또는 이를 Base64 로 인코딩한 값
// 이 유틸은 properties 파일을 읽지 않는다 — 파일 존재 여부와 무관하게 동작
//
// 사용 규칙 (knowledge.md):
// - Controller/Mapper에서 암호화/복호화 직접 호출 금지 — 반드시 Service Layer에서 사용
// - 복호화한 평문·CI 로그 출력 금지
// - 키 하드코딩 금지 (Git 추적 금지)
public final class PersonalDataCipher {

    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int AES_KEY_LENGTH_BYTES = 32;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static volatile byte[] secretKeyBytes;

    static {
        reloadKey();
    }

    private PersonalDataCipher() {
    }

    // ---------- 공개 API ----------

    /** 평문을 AES-256/GCM 으로 암호화하여 Base64(IV+ciphertext) 문자열로 반환한다. */
    public static String encrypt(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("암호화 대상 값이 null입니다.");
        }
        byte[] key = requireKey();
        try {
            byte[] iv = newIv();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("개인정보 암호화에 실패했습니다.", e);
        }
    }

    /** Base64(IV+ciphertext) 문자열을 복호화하여 평문으로 반환한다. */
    public static String decrypt(String encryptedText) {
        if (encryptedText == null) {
            throw new IllegalArgumentException("복호화 대상 값이 null입니다.");
        }
        byte[] key = requireKey();
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);
            if (combined.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("암호문 형식이 올바르지 않습니다.");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            byte[] cipherBytes = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("개인정보 복호화에 실패했습니다.", e);
        }
    }

    // ---------- 키 로딩 ----------
    // public: 시스템 프로퍼티로 키를 주입하는 외부 패키지 테스트(AuthServiceImplTest 등)에서도 호출할 수 있도록 공개

    public static void reloadKey() {
        String keyValue = System.getProperty("personal.data.aes.key");
        if (isBlank(keyValue)) {
            keyValue = System.getenv("PERSONAL_DATA_AES_KEY");
        }
        if (isBlank(keyValue)) {
            keyValue = System.getenv("AES_KEY");
        }
        if (isBlank(keyValue)) {
            secretKeyBytes = null; // 사용 시점에 fail-fast
            return;
        }
        secretKeyBytes = decodeKey(keyValue);
    }

    private static byte[] decodeKey(String keyValue) {
        byte[] raw = keyValue.getBytes(StandardCharsets.UTF_8);

        // 32바이트 raw 문자열이면 그대로 사용
        if (raw.length == AES_KEY_LENGTH_BYTES) {
            return raw;
        }

        // Base64 인코딩된 32바이트 키 (44자) 지원
        try {
            byte[] decoded = Base64.getDecoder().decode(keyValue);
            if (decoded.length == AES_KEY_LENGTH_BYTES) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Base64 아님 → 아래에서 형식 오류 처리
        }
        throw new IllegalStateException(
                "AES 키는 32바이트(256-bit) 또는 이를 Base64 로 인코딩한 값이어야 합니다.");
    }

    private static byte[] requireKey() {
        byte[] key = secretKeyBytes;
        if (key == null) {
            throw new IllegalStateException(
                    "AES 키가 설정되지 않았습니다. PERSONAL_DATA_AES_KEY 환경변수를 확인하세요.");
        }
        return key;
    }

    private static byte[] newIv() {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
