package com.workit.domain.notification.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

// Notification 도메인 에러 코드
// ErrorCode 규약(DOMAIN_REASON)에 따라 NOTIFICATION_ 접두사를 사용한다
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "요청한 size가 허용된 최대값을 초과했습니다."),
    NOTIFICATION_INVALID_CURSOR(HttpStatus.BAD_REQUEST, "유효하지 않은 cursor 값입니다.");

    private final HttpStatus status;
    private final String message;

    NotificationErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
