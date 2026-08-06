package com.workit.security;

import com.workit.domain.auth.exception.AuthErrorCode;
import com.workit.exception.BusinessException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

// @CurrentUser Long userId 파라미터를 SecurityContext 의 인증 객체(WorkitPrincipal)에서 해석하는 리졸버
//
// - JwtAuthenticationFilter 가 검증한 WorkitPrincipal 에서 userId 만 추출한다 (개인정보 미노출)
// - 보호 API는 필터가 이미 인증을 보장하므로 정상 흐름에서는 항상 존재하지만,
//   인증 객체가 없는 요청(개발 실수로 공개 API에 @CurrentUser 사용 등)은
//   AUTH_TOKEN_NOT_FOUND(401) 로 명확히 실패시킨다
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(CurrentUser.class)) {
            return false;
        }
        Class<?> type = parameter.getParameterType();
        return Long.class.equals(type) || long.class.equals(type);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof WorkitPrincipal) {
            return ((WorkitPrincipal) authentication).getUserId();
        }
        throw new BusinessException(AuthErrorCode.AUTH_TOKEN_NOT_FOUND);
    }
}
