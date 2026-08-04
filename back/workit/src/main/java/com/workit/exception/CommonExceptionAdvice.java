package com.workit.exception;

import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

// 팀 공통 예외 처리
//
// 응답 형식은 전 도메인 공통으로 { "status": "ERROR", "errorCode": "...", "message": "..." } 를 사용한다
// 도메인 규칙 위반은 BusinessException 만 던지면 되고,
// 상태 코드와 메시지는 함께 전달한 ErrorCode 가 결정한다
//
// 프론트를 별도 서버로 배포하므로 이 서버는 API 만 응답한다 (HTML 반환 없음)
@RestControllerAdvice
@Slf4j
public class CommonExceptionAdvice {

    // 도메인 비즈니스 예외 - 상태 코드와 메시지를 ErrorCode 가 결정
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("비즈니스 예외 [{}] [{}] {}",
                e.getErrorCode().getStatus(), e.getErrorCode().getErrorCode(), e.getMessage());
        return GlobalResponseFactory.error(
                e.getErrorCode().getStatus(), e.getErrorCode().getErrorCode(), e.getMessage());
    }

    // 400 - 파라미터 유효성 검증 실패
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        log.warn("유효성 오류 - {}", e.getMessage());
        return GlobalResponseFactory.error(
                HttpStatus.BAD_REQUEST, CommonErrorCode.COMMON_INVALID_REQUEST, e.getMessage());
    }

    // 400 - Request DTO @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + defaultMessage(fieldError))
                .collect(Collectors.joining(", "));
        // field error 가 없는 경우(글로벌 에러만 있는 경우 등) 기본 메시지로 대체
        if (detail.isEmpty()) {
            detail = CommonErrorCode.COMMON_VALIDATION_FAILED.getMessage();
        }
        log.warn("요청 검증 실패 - {}", detail);
        return GlobalResponseFactory.error(
                HttpStatus.BAD_REQUEST, CommonErrorCode.COMMON_VALIDATION_FAILED, detail);
    }

    // 400 - 요청 본문 형식 오류 (JSON 파싱 실패, 날짜 형식 오류 등)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 형식 오류 - {}", e.getMessage());
        return GlobalResponseFactory.error(HttpStatus.BAD_REQUEST, CommonErrorCode.COMMON_INVALID_FORMAT);
    }

    // 400 - 필수 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<CommonResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("필수 파라미터 누락 - {}", e.getParameterName());
        return GlobalResponseFactory.error(
                HttpStatus.BAD_REQUEST, CommonErrorCode.COMMON_MISSING_PARAMETER,
                CommonErrorCode.COMMON_MISSING_PARAMETER.getMessage() + ": " + e.getParameterName());
    }

    // 400 - 파라미터 타입 불일치 (숫자 자리에 문자가 온 경우 등)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("파라미터 타입 오류 - {} = {}", e.getName(), e.getValue());
        return GlobalResponseFactory.error(
                HttpStatus.BAD_REQUEST, CommonErrorCode.COMMON_TYPE_MISMATCH,
                CommonErrorCode.COMMON_TYPE_MISMATCH.getMessage() + ": " + e.getName());
    }

    // 404 - 조회 대상 없음
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoSuchElement(NoSuchElementException e) {
        log.warn("조회 실패 - {}", e.getMessage());
        return GlobalResponseFactory.error(HttpStatus.NOT_FOUND, CommonErrorCode.COMMON_NOT_FOUND);
    }

    // 404 - 존재하지 않는 API 경로
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNoHandler(NoHandlerFoundException e) {
        log.warn("존재하지 않는 경로 - {} {}", e.getHttpMethod(), e.getRequestURL());
        return GlobalResponseFactory.error(HttpStatus.NOT_FOUND, CommonErrorCode.COMMON_NO_HANDLER);
    }

    // 405 - 지원하지 않는 HTTP 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 메서드 - {}", e.getMethod());
        return GlobalResponseFactory.error(HttpStatus.METHOD_NOT_ALLOWED, CommonErrorCode.COMMON_METHOD_NOT_ALLOWED);
    }

    // 500 - DB 처리 오류
    // 예외 메시지에 테이블·제약조건명이 들어 있어 응답에 노출하지 않고 로그에만 남긴다
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<CommonResponse<Void>> handleDataAccess(DataAccessException e) {
        log.error("DB 처리 오류", e);
        return GlobalResponseFactory.error(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.COMMON_DATA_ACCESS_ERROR);
    }

    // 500 - 예상하지 못한 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception e) {
        log.error("처리 중 오류 발생", e);
        return GlobalResponseFactory.error(HttpStatus.INTERNAL_SERVER_ERROR, CommonErrorCode.COMMON_SERVER_ERROR);
    }

    private String defaultMessage(FieldError fieldError) {
        return fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "값이 올바르지 않습니다.";
    }
}
