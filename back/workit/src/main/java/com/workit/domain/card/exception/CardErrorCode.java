package com.workit.domain.card.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CardErrorCode implements ErrorCode {

    LINKABLE_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 연동 가능 카드입니다."),
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카드입니다."),
    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "별칭을 입력해주세요."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "별칭은 100자를 초과할 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    CardErrorCode(HttpStatus status, String message) {
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