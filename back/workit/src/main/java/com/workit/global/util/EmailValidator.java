package com.workit.global.util;

import java.util.Locale;
import java.util.regex.Pattern;

// 이메일 검증·정규화 공통 유틸
//
// 목적 (knowledge.md):
// - check-email(이메일 중복 확인) / signup(회원가입 완료) 이 동일한 이메일 검증 정책을
//   사용하도록 단일화한다. 검증 정책이 API 마다 달라지는 것을 방지한다.
//
// 검증 순서 (단일 정책):
//   null 체크 → blank 체크 → trim → 최대 길이 검증 → lowercase 정규화 → 이메일 형식 검증
//
// 보안 규칙:
// - 이메일 원문 로그 출력 금지 (호출 측 Service 가 로그에 값을 포함하지 않아야 한다)
// - email_encrypt(AES 원문) 조회·복호화 후 검증 금지 — 검증은 항상 원문 기준
// - 검증 실패 시 IllegalArgumentException 을 던지고, 도메인 에러 변환(예: INVALID_EMAIL_FORMAT)은
//   Service 계층에서 처리한다 (global.util 이 도메인 ErrorCode 에 의존하지 않도록)
public final class EmailValidator {

    private EmailValidator() {
    }

    /** 이메일 형식 검증 패턴 (일반적인 이메일 주소 규칙) */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** 이메일 전체 최대 길이 (RFC 5321 기준 최대 254자) */
    public static final int MAX_EMAIL_LENGTH = 254;

    /**
     * 이메일 검증 + 정규화
     * - null/blank → trim → 최대 길이 검증 → lowercase 정규화 → 형식 검증 순서로 처리
     *
     * @param email 검증할 이메일 (null 허용)
     * @return 정규화된 이메일 (trim + lowercase)
     * @throws IllegalArgumentException null/blank, 최대 길이 초과, 형식 오류 시
     */
    public static String normalize(String email) {
        // 1. null 체크 후 trim 처리
        String trimmed = email == null ? null : email.trim();

        // 2. blank 체크
        if (trimmed == null || trimmed.isEmpty()) {
            throw new IllegalArgumentException("이메일이 입력되지 않았습니다.");
        }

        // 3. 최대 길이 검증 (RFC 5321 기준 254자 초과 차단)
        if (trimmed.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("이메일 최대 길이(" + MAX_EMAIL_LENGTH + "자)를 초과했습니다.");
        }

        // 4. lowercase 정규화 (email_hash 가 대소문자 무관하게 동작하도록)
        String normalized = trimmed.toLowerCase(Locale.ROOT);

        // 5. 이메일 형식 검증
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("올바르지 않은 이메일 형식입니다.");
        }

        return normalized;
    }
}
