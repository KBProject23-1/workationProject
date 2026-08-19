package com.workit.domain.account.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum AccountErrorCode implements ErrorCode {

    ACCOUNT_LINKABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 연동 가능 계좌입니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 계좌입니다."),
    ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 연동된 계좌입니다."),
    ACCOUNT_PRIMARY_DELETE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "주 계좌는 삭제할 수 없습니다. 다른 계좌를 먼저 주 계좌로 설정해주세요.");

    private final HttpStatus status;
    private final String message;

    AccountErrorCode(HttpStatus status, String message) {
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