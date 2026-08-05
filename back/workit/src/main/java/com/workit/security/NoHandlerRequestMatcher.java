package com.workit.security;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

// 실제 핸들러가 없는 '알 수 없는 경로'를 판별하는 RequestMatcher
//
// 배경:
//   - Security 필터 체인은 DispatcherServlet 보다 먼저 동작하므로,
//     anyRequest().authenticated() 만으로는 미인증 요청이 '알 수 없는 경로'인지
//     '보호 경로'인지 구분하지 못해 알 수 없는 경로에도 401 을 반환하게 된다.
//   - 기존 동작(핸들러 없음 → NoHandlerFoundException → 404 COMMON_NO_HANDLER)을
//     미인증 상태에서도 유지하기 위해, 핸들러가 없는 경로는 permitAll 로 통과시킨다.
//     통과된 요청은 DispatcherServlet 에 도달해 404 로 처리된다.
//
// 판별 방식:
//   - 요청 시점에 DispatcherServlet 의 WebApplicationContext(ServletContext 속성)를 찾아
//     등록된 모든 HandlerMapping 을 순서대로 조회하고, 어느 매핑도 핸들러를 반환하지 않으면
//     '알 수 없는 경로'로 판단한다.
//   - 컨텍스트를 찾지 못하거나 조회 중 오류가 나면 안전하게 인증 대상(false)으로 처리한다.
//
// 알려진 경계:
//   - 알 수 없는 경로에 위변조/만료된 Bearer 토큰이 함께 오는 경우는 JwtAuthenticationFilter 가
//     먼저 실행되어 400/401 로 응답한다 (인증 토큰 자체가 유효하지 않은 경우).
//   - 경로는 존재하지만 HTTP 메서드가 맞지 않는 경우는 lookupHandlerMethod 가 예외를 던져
//     인증 대상으로 처리된다 (미인증 시 401 — 기존 405 와 다른 경계, 허용).
public class NoHandlerRequestMatcher implements RequestMatcher {

    /** FrameworkServlet 이 ServletContext 에 저장하는 속성 접두어 (DispatcherServlet 포함) */
    private static final String FRAMEWORK_SERVLET_CONTEXT_PREFIX =
            "org.springframework.web.servlet.FrameworkServlet.CONTEXT.";

    /** 첫 요청 시 해석 후 캐시 (단일 WAR 내 컨텍스트는 불변) */
    private static volatile List<HandlerMapping> cachedMappings;

    private final HandlerMappingResolver resolver;

    public NoHandlerRequestMatcher() {
        this(NoHandlerRequestMatcher::resolveMappings);
    }

    /** 테스트에서 매핑 조회를 주입할 수 있도록 분리한 생성자 */
    NoHandlerRequestMatcher(HandlerMappingResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean matches(HttpServletRequest request) {
        List<HandlerMapping> mappings = resolver.resolve(request);
        if (mappings == null || mappings.isEmpty()) {
            return false; // 판별 불가 → 인증 대상으로 처리 (안전 우선)
        }
        try {
            for (HandlerMapping mapping : mappings) {
                if (mapping.getHandler(request) != null) {
                    return false; // 핸들러 존재 → 보호 경로 (인증 대상)
                }
            }
        } catch (Exception e) {
            return false; // 조회 중 오류(메서드 불일치 등) → 인증 대상으로 처리
        }
        return true; // 어떤 핸들러도 없음 → 알 수 없는 경로 (permitAll → MVC 404)
    }

    /**
     * DispatcherServlet 컨텍스트의 HandlerMapping 목록을 반환한다.
     * 첫 요청 시 해석 후 정적으로 캐시한다. 컨텍스트를 찾지 못한 경우(null)는 캐시하지 않아
     * 다음 요청에서 재시도한다 (콜드 스타트 시 DispatcherServlet 초기화 전 1회 보완).
     */
    private static List<HandlerMapping> resolveMappings(HttpServletRequest request) {
        List<HandlerMapping> cached = cachedMappings;
        if (cached != null) {
            return cached;
        }
        WebApplicationContext dispatcherContext = findDispatcherServletContext(request.getServletContext());
        if (dispatcherContext == null) {
            return null;
        }
        List<HandlerMapping> mappings = new ArrayList<>(dispatcherContext.getBeansOfType(HandlerMapping.class).values());
        mappings.sort(AnnotationAwareOrderComparator.INSTANCE);
        cachedMappings = mappings;
        return mappings;
    }

    /** ServletContext 속성에서 DispatcherServlet 의 WebApplicationContext(자식 컨텍스트)를 찾는다 */
    private static WebApplicationContext findDispatcherServletContext(ServletContext servletContext) {
        Enumeration<String> names = servletContext.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            if (name.startsWith(FRAMEWORK_SERVLET_CONTEXT_PREFIX)) {
                Object attribute = servletContext.getAttribute(name);
                if (attribute instanceof WebApplicationContext) {
                    return (WebApplicationContext) attribute;
                }
            }
        }
        return null;
    }

    @FunctionalInterface
    interface HandlerMappingResolver {
        List<HandlerMapping> resolve(HttpServletRequest request);
    }
}
