package com.workit.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Controller 메서드 파라미터에서 인증된 사용자 ID(userId)를 주입받기 위한 어노테이션
//
// 사용 예:
//   @GetMapping("/me")
//   public ResponseEntity<...> getMyInfo(@CurrentUser Long userId) { ... }
//
// 동작:
//   - CurrentUserArgumentResolver 가 SecurityContext 의 WorkitPrincipal 에서 userId 를 추출해 주입한다
//   - 인증되지 않은 요청이면 AUTH_TOKEN_NOT_FOUND(401) 예외를 던진다
//
// 보안:
//   - 식별자(userId)만 주입하며 name/email/phone 등 개인정보는 절대 주입하지 않는다 (knowledge.md)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
