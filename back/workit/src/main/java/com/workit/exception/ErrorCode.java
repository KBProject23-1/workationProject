package com.workit.exception;

import org.springframework.http.HttpStatus;

// 도메인별 에러 코드 enum 이 구현하는 규격
//각 도메인은 이 인터페이스를 구현한 enum 하나만 만들면 되고 예외 클래스나 ControllerAdvice 를 따로 만들지 않음

public interface ErrorCode {

    /** 응답 HTTP 상태 코드 */
    HttpStatus getStatus();

    /** 클라이언트에 전달할 기본 메시지 */
    String getMessage();
}
