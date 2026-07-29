package com.workit.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

// 팀 공통 예외 처리
//
// 응답 형식은 전 도메인 공통으로 { "message": "..." } 를 사용한다
// 도메인 규칙 위반은 BusinessException 만 던지면 되고,
// 상태 코드와 메시지는 함께 전달한 ErrorCode 가 결정한다
//
// 프론트를 별도 서버로 배포하므로 이 서버는 API 만 응답한다 (HTML 반환 없음)
@RestControllerAdvice
@Slf4j
public class CommonExceptionAdvice {

    // 도메인 비즈니스 예외 - 상태 코드와 메시지를 ErrorCode 가 결정
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException e) {
        log.warn("비즈니스 예외 [{}] {}", e.getErrorCode().getStatus(), e.getMessage());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(message(e.getMessage()));
    }

    // 400 - 파라미터 유효성 검증 실패
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        log.warn("유효성 오류 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(message(e.getMessage()));
    }

    // 400 - 요청 본문 형식 오류 (JSON 파싱 실패, 날짜 형식 오류 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 형식 오류 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(message("요청 형식이 올바르지 않습니다."));
    }

    // 400 - 필수 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락 - {}", e.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(message("필수 파라미터가 누락되었습니다: " + e.getParameterName()));
    }

    // 400 - 파라미터 타입 불일치 (숫자 자리에 문자가 온 경우 등)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 오류 - {} = {}", e.getName(), e.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(message("요청 값의 형식이 올바르지 않습니다: " + e.getName()));
    }

    // 404 - 조회 대상 없음
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElement(NoSuchElementException e) {
        log.warn("조회 실패 - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(message("해당 ID의 요소가 없습니다."));
    }

    // 404 - 존재하지 않는 API 경로
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoHandler(NoHandlerFoundException e) {
        log.warn("존재하지 않는 경로 - {} {}", e.getHttpMethod(), e.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(message("존재하지 않는 API 경로입니다."));
    }

    // 405 - 지원하지 않는 HTTP 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 메서드 - {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(message("지원하지 않는 요청 방식입니다."));
    }

    // 500 - DB 처리 오류
    // 예외 메시지에 테이블·제약조건명이 들어 있어 응답에 노출하지 않고 로그에만 남긴다
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccess(DataAccessException e) {
        log.error("DB 처리 오류", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(message("데이터 처리 중 오류가 발생했습니다."));
    }

    // 500 - 예상하지 못한 오류
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