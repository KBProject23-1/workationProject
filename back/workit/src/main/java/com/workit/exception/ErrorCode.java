package com.workit.exception;

import org.springframework.http.HttpStatus;

// 도메인별 에러 코드 enum 이 구현하는 규격
//각 도메인은 이 인터페이스를 구현한 enum 하나만 만들면 되고 예외 클래스나 ControllerAdvice 를 따로 만들지 않음

public interface ErrorCode {

    /** 응답 HTTP 상태 코드 */
    HttpStatus getStatus();

    /** 클라이언트에 전달할 기본 메시지 */
    String getMessage();

    /**
     * 클라이언트에 전달할 에러 코드명 (예: INVALID_CREDENTIALS)
     *
     * 모든 구현체가 enum 이므로 enum 이름(name)을 그대로 코드명으로 사용한다.
     * default 메서드로 제공되어 도메인별 enum 에서 별도 구현이 필요 없다.
     */
    default String getErrorCode() {
        return ((Enum<?>) this).name();
    }
    return getClass().getSimpleName();
}
