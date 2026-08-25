package com.workit.domain.auth.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// EmailMasker 단위 테스트 — docs 아이디 찾기 마스킹 정책 고정
// 규칙: 로컬파트 앞 4자리 유지 + 나머지 마스킹, 4자 이하이면 마지막 2자리 마스킹
class EmailMaskerTest {

    @Test
    @DisplayName("로컬파트 앞 4자리 유지 + 나머지 '*' 마스킹 (docs 예시 형태)")
    void mask_keepsFirst4() {
        assertEquals("user****@example.com", EmailMasker.mask("user1234@example.com"));
    }

    @Test
    @DisplayName("로컬파트가 4자 이하 → 마지막 2자리 마스킹 (짧은 로컬파트도 최소 마스킹)")
    void mask_shortLocalPart() {
        assertEquals("us**@example.com", EmailMasker.mask("user@example.com"));
        assertEquals("n**@example.com", EmailMasker.mask("new@example.com"));
        assertEquals("a*@example.com", EmailMasker.mask("ab@example.com"));
    }

    @Test
    @DisplayName("도메인은 마스킹하지 않는다")
    void mask_keepsDomain() {
        assertEquals("user****@example.co.kr", EmailMasker.mask("user1234@example.co.kr"));
    }

    @Test
    @DisplayName("null 입력 → null 반환")
    void mask_null() {
        assertNull(EmailMasker.mask(null));
    }

    @Test
    @DisplayName("'@' 가 없는 비정상 입력 → 원문 그대로 반환")
    void mask_noAtSign() {
        assertEquals("no-at-sign", EmailMasker.mask("no-at-sign"));
    }
}
