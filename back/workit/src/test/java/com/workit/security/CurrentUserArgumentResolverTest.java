package com.workit.security;

import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// @CurrentUser 파라미터 해석 리졸버 단위 테스트
class CurrentUserArgumentResolverTest {

    private CurrentUserArgumentResolver resolver;

    /** 테스트용 컨트롤러 시그니처 */
    public static class TestController {
        public void withCurrentUser(@CurrentUser Long userId) {
        }

        public void withoutAnnotation(Long userId) {
        }

        public void wrongType(@CurrentUser String userId) {
        }
    }

    @BeforeEach
    void setUp() {
        resolver = new CurrentUserArgumentResolver();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MethodParameter parameter(String methodName, int index) {
        for (Method m : TestController.class.getMethods()) {
            if (m.getName().equals(methodName)) {
                return new MethodParameter(m, index);
            }
        }
        throw new IllegalStateException("테스트 시그니처가 존재하지 않습니다: " + methodName);
    }

    @Test
    @DisplayName("@CurrentUser Long 파라미터만 지원")
    void supportsParameter() throws NoSuchMethodException {
        assertTrue(resolver.supportsParameter(parameter("withCurrentUser", 0)));
        assertFalse(resolver.supportsParameter(parameter("withoutAnnotation", 0)));
        assertFalse(resolver.supportsParameter(parameter("wrongType", 0)));
    }

    @Test
    @DisplayName("SecurityContext 의 WorkitPrincipal 에서 userId 추출")
    void resolveArgument_returnsUserId() throws NoSuchMethodException {
        SecurityContextHolder.getContext()
                .setAuthentication(new WorkitPrincipal(99L, "ROLE_USER"));

        Object resolved = resolver.resolveArgument(parameter("withCurrentUser", 0), null, null, null);

        assertEquals(99L, resolved);
    }

    @Test
    @DisplayName("인증 객체가 없으면 AUTH_TOKEN_NOT_FOUND(401) 예외")
    void resolveArgument_withoutAuthentication_throws() throws NoSuchMethodException {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> resolver.resolveArgument(parameter("withCurrentUser", 0), null, null, null));

        assertEquals(AuthErrorCode.AUTH_TOKEN_NOT_FOUND.getErrorCode(),
                exception.getErrorCode().getErrorCode());
    }
}
