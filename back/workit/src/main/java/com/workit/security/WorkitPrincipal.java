package com.workit.security;

import com.workit.domain.auth.util.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

// SecurityContext 에 저장되는 인증 객체 (JWT 검증 결과)
//
// knowledge.md Personal Information Policy 준수:
//   - userId, role 만 보유 — 개인정보(name/email/phone/CI 등) 절대 저장 금지
//   - JWT Payload(role claim)와 동일하게 role 은 DB role 컬럼 도입 전까지 ROLE_USER
//
// 구현 방식:
//   - org.springframework.security.core.Authentication 직접 구현
//   - 별도 UserDetails/토큰 변환 없이 WorkitPrincipal 그 자체를 SecurityContext 에 저장
//   - Controller(추후 @CurrentUser 단계)에서 getUserId() 로 식별자만 사용
public class WorkitPrincipal implements Authentication {

    private final Long userId;
    private final String role;
    private final Collection<? extends GrantedAuthority> authorities;

    public WorkitPrincipal(Long userId, String role) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId는 양수여야 합니다.");
        }
        this.userId = userId;
        this.role = (role == null || role.trim().isEmpty())
                ? JwtTokenProvider.DEFAULT_ROLE
                : role.trim();
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(this.role));
    }

    /** API 식별자 — Service 호출 시 사용 (개인정보 아님) */
    public Long getUserId() {
        return userId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return null; // JWT는 재사용하지 않으므로 자격 증명 원문을 보관하지 않는다
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return this;
    }

    @Override
    public boolean isAuthenticated() {
        return true; // 필터 검증을 통과한 경우에만 생성되므로 항상 인증된 상태
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        // 필터 외부에서 인증 상태를 변경할 수 없도록 차단한다
        throw new IllegalArgumentException("WorkitPrincipal의 인증 상태는 변경할 수 없습니다.");
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
