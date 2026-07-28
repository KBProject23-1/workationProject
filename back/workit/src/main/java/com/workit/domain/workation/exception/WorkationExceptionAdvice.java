package com.workit.domain.workation.exception;

import com.workit.domain.workation.controller.WorkationController;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Collections;
import java.util.Map;

/**
 * 워케이션 도메인 전용 예외 처리
 *
 * 공용 ApiExceptionAdvice 는 String 을 그대로 반환하지만
 * API 명세서는 { "message": "..." } JSON 을 규정하고 있어
 * 워케이션 컨트롤러에 한해 JSON 으로 응답한다.
 * 공용 Advice 보다 먼저 잡히도록 @Order(0) 을 지정한다.
 */
@RestControllerAdvice(assignableTypes = WorkationController.class)
@Order(0)
@Log4j2
public class WorkationExceptionAdvice {

    /** 400 - 유효성 검증 실패 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        log.warn("워케이션 유효성 오류 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message(e.getMessage()));
    }

    /** 404 - 대상 없음 */
    @ExceptionHandler(WorkationNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(WorkationNotFoundException e) {
        log.warn("워케이션 조회 실패 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(message(e.getMessage()));
    }

    /** 409 - 상태 충돌 */
    @ExceptionHandler(WorkationConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(WorkationConflictException e) {
        log.warn("워케이션 상태 충돌 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(message(e.getMessage()));
    }

    /** 500 - 그 외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        log.error("워케이션 처리 중 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(message("워케이션 처리 중 오류가 발생했습니다."));
    }

    private Map<String, String> message(String message) {
        return Collections.singletonMap("message", message);
    }
}
