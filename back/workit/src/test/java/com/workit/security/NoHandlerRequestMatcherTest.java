package com.workit.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 알 수 없는 경로(핸들러 없음) 판별 매처 단위 테스트
// - 실제 DispatcherServlet 컨텍스트 대신 매핑 해석기(resolver)를 주입해 검증한다
class NoHandlerRequestMatcherTest {

    /** /known 경로만 핸들러를 가진 목 매핑 */
    private HandlerMapping stubMapping() {
        return request -> {
            if ("/known".equals(request.getRequestURI())) {
                return new HandlerExecutionChain(new Object());
            }
            return null;
        };
    }

    @Test
    @DisplayName("핸들러가 없는 경로 → true (알 수 없는 경로 — 404 대상)")
    void unknownPath_matches() {
        NoHandlerRequestMatcher matcher =
                new NoHandlerRequestMatcher(request -> Collections.singletonList(stubMapping()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/nonexistent");

        assertTrue(matcher.matches(request));
    }

    @Test
    @DisplayName("핸들러가 존재하는 경로 → false (보호 경로 — 인증 대상)")
    void knownPath_doesNotMatch() {
        NoHandlerRequestMatcher matcher =
                new NoHandlerRequestMatcher(request -> Collections.singletonList(stubMapping()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/known");

        assertFalse(matcher.matches(request));
    }

    @Test
    @DisplayName("여러 매핑 중 하나라도 핸들러를 반환하면 → false")
    void anyMappingWithHandler_doesNotMatch() {
        NoHandlerRequestMatcher matcher = new NoHandlerRequestMatcher(request ->
                java.util.Arrays.asList(stubMapping(), request2 -> new HandlerExecutionChain(new Object())));

        assertFalse(matcher.matches(new MockHttpServletRequest("GET", "/api/v1/nonexistent")));
    }

    @Test
    @DisplayName("매핑 목록이 비어 있으면 → false (판별 불가 — 인증 대상으로 안전 처리)")
    void emptyMappings_doesNotMatch() {
        NoHandlerRequestMatcher matcher =
                new NoHandlerRequestMatcher(request -> Collections.emptyList());

        assertFalse(matcher.matches(new MockHttpServletRequest("GET", "/api/v1/nonexistent")));
    }

    @Test
    @DisplayName("매핑 목록이 null 이면 → false (판별 불가 — 인증 대상으로 안전 처리)")
    void nullMappings_doesNotMatch() {
        NoHandlerRequestMatcher matcher = new NoHandlerRequestMatcher(request -> null);

        assertFalse(matcher.matches(new MockHttpServletRequest("GET", "/api/v1/nonexistent")));
    }

    @Test
    @DisplayName("핸들러 조회 중 예외 발생 → false (인증 대상으로 안전 처리)")
    void getHandlerThrows_doesNotMatch() {
        HandlerMapping throwing = request -> {
            throw new IllegalStateException("조회 오류");
        };
        NoHandlerRequestMatcher matcher =
                new NoHandlerRequestMatcher(request -> Collections.singletonList(throwing));

        assertFalse(matcher.matches(new MockHttpServletRequest("GET", "/api/v1/nonexistent")));
    }
}
