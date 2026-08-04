package com.workit.domain.merchant.offices.exception;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MerchantErrorCode implements ErrorCode {

    INVALID_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "조회 조건이 올바르지 않습니다."),
    INVALID_OFFICE_CURSOR(HttpStatus.BAD_REQUEST, "공유오피스 조회 커서가 올바르지 않습니다."),
    INVALID_MERCHANT_ID(HttpStatus.BAD_REQUEST, "올바르지 않은 상품 ID입니다."),
    OFFICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공유오피스 상품을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    MerchantErrorCode(HttpStatus status, String message) {
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
