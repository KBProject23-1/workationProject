package com.workit.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.Map;

// 팀 공통 예외 처리

// 응답 형식은 전 도메인 공통으로 { "message": "..." } 를 사용함
// 도메인은 BusinessException 만 던지면 되고, 상태 코드와 메시지는 함께 전달한 ErrorCode 가 결정함

@RestControllerAdvice
@Order(0)
@Log4j2
public class GlobalExceptionAdvice {

    // 도메인 비즈니스 예외 - 상태 코드와 메시지를 ErrorCode 가 결정
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException e) {
        log.warn("비즈니스 예외 [{}] {}", e.getErrorCode().getStatus(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(message(e.getMessage()));
    }

    /** 400 - 파라미터 유효성 검증 실패 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        log.warn("유효성 오류 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(message(e.getMessage()));
    }

    /** 400 - 요청 본문 형식 오류 (JSON 파싱 실패, 날짜 형식 오류 등) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 형식 오류 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(message("요청 형식이 올바르지 않습니다."));
    }

    /** 500 - 예상하지 못한 오류 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("처리 중 오류 발생", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(message("서버 오류가 발생했습니다."));
    }

    private Map<String, String> message(String message) {
        return Collections.singletonMap("message", message);
    }
}
