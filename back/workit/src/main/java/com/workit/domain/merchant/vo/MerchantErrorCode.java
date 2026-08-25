package com.workit.domain.merchant.vo;

import com.workit.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum MerchantErrorCode implements ErrorCode {

    INVALID_SORT(HttpStatus.BAD_REQUEST, "정렬 조건이 올바르지 않습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "카테고리가 올바르지 않습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "커서 값이 올바르지 않습니다."),
    INVALID_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "조회 조건이 올바르지 않습니다."),
    INVALID_OFFICE_CURSOR(HttpStatus.BAD_REQUEST, "공유오피스 조회 커서가 올바르지 않습니다."),
    INVALID_MERCHANT_ID(HttpStatus.BAD_REQUEST, "올바르지 않은 상품 ID입니다."),
    ACCOMMODATION_NOT_FOUND(HttpStatus.NOT_FOUND, "숙소를 찾을 수 없습니다."),
    OFFICE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공유오피스 상품을 찾을 수 없습니다."),
    RESTAURANT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 음식점 상품을 찾을 수 없습니다."),
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 여가 상품을 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_PERIOD(HttpStatus.BAD_REQUEST, "시작 날짜나 종료 날짜가 유효하지 않습니다.");

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
