package com.workit.global.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// BCrypt 기반 단방향 해시 유틸 (비밀번호 / PIN 공용)
//
// 보안 규칙 (knowledge.md):
// - 비밀번호·PIN 원문 저장 및 복호화 금지
// - BCrypt strength 기본값 사용 (BCryptPasswordEncoder 기본 strength = 10)
//
// 사용 위치: 반드시 Service Layer에서만 사용
// Controller/Mapper에서 직접 호출 금지
//
// 비밀번호 불일치 판정(예: INVALID_PASSWORD)은 호출한 Service에서
// matches() 결과를 보고 BusinessException으로 처리한다.
public final class PasswordEncryptor {

    // BCryptPasswordEncoder 는 thread-safe 하여 static 공유 가능
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordEncryptor() {
    }

    /** 평문(비밀번호/PIN)을 BCrypt 해시 문자열로 변환한다. */
    public static String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("암호화 대상 값이 null입니다.");
        }
        return ENCODER.encode(rawPassword);
    }

    /** 평문이 해시와 일치하는지 검증한다. null 입력은 일치하지 않는 것으로 간주한다. */
    public static boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return ENCODER.matches(rawPassword, hashedPassword);
    }
}
