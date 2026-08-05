package com.workit.security;

import com.workit.domain.auth.util.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkitPrincipalTest {

    @Test
    @DisplayName("userId와 role로 인증 객체 생성 — 권한 목록은 role 하나")
    void create_success() {
        WorkitPrincipal principal = new WorkitPrincipal(7L, JwtTokenProvider.DEFAULT_ROLE);

        assertEquals(7L, principal.getUserId());
        assertEquals("7", principal.getName());
        assertTrue(principal.isAuthenticated());
        assertEquals(principal, principal.getPrincipal());
        List<String> authorities = principal.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toList());
        assertEquals(Collections.singletonList("ROLE_USER"), authorities);
    }

    @Test
    @DisplayName("role이 null/빈 값이면 기본 권한 ROLE_USER 부여")
    void create_nullRole_usesDefault() {
        assertEquals(JwtTokenProvider.DEFAULT_ROLE,
                new WorkitPrincipal(1L, null).getAuthorities().iterator().next().getAuthority());
        assertEquals(JwtTokenProvider.DEFAULT_ROLE,
                new WorkitPrincipal(1L, "  ").getAuthorities().iterator().next().getAuthority());
    }

    @Test
    @DisplayName("userId가 null/양수가 아니면 생성 실패")
    void create_invalidUserId_throws() {
        assertThrows(IllegalArgumentException.class, () -> new WorkitPrincipal(null, JwtTokenProvider.DEFAULT_ROLE));
        assertThrows(IllegalArgumentException.class, () -> new WorkitPrincipal(0L, JwtTokenProvider.DEFAULT_ROLE));
        assertThrows(IllegalArgumentException.class, () -> new WorkitPrincipal(-1L, JwtTokenProvider.DEFAULT_ROLE));
    }

    @Test
    @DisplayName("외부에서 인증 상태 변경 차단")
    void setAuthenticated_throws() {
        WorkitPrincipal principal = new WorkitPrincipal(1L, JwtTokenProvider.DEFAULT_ROLE);
        assertThrows(IllegalArgumentException.class, () -> principal.setAuthenticated(false));
    }

    @Test
    @DisplayName("자격 증명/상세 정보는 보관하지 않음 (JWT·개인정보 미보관)")
    void noSensitiveData() {
        WorkitPrincipal principal = new WorkitPrincipal(1L, JwtTokenProvider.DEFAULT_ROLE);
        assertEquals(null, principal.getCredentials());
        assertEquals(null, principal.getDetails());
    }
}
