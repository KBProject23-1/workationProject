package com.workit.domain.reservation.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


// 예약 도메인에서 사용하는 오류 코드
@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

//    400
    INVALID_RESERVATION_ID(HttpStatus.BAD_REQUEST, "예약 정보가 올바르지 않습니다."),
    RESERVATION_STATUS_REQUIRED(HttpStatus.BAD_REQUEST, "예약 상태를 한 개 이상 선택해야 합니다."),
    INVALID_RESERVATION_REQUEST(HttpStatus.BAD_REQUEST, "예약 정보가 올바르지 않습니다."),
    INVALID_RESERVATION_DATE(HttpStatus.BAD_REQUEST, "예약 기간이 올바르지 않습니다."),
    RESERVATION_DATE_OUT_OF_WORKATION(HttpStatus.BAD_REQUEST, "워케이션 기간 안에서 예약해야 합니다."),
    RESERVATION_HEADCOUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "예약 가능한 인원을 초과했습니다."),

//    404
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다."),
    RESERVATION_CANCELLATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약 취소 내역이 존재하지 않습니다."),
    WORKATION_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "예약 가능한 워케이션이 존재하지 않습니다."),
    RESERVATION_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예약 상품입니다."),

//    409
    RESERVATION_REGION_MISMATCH(HttpStatus.CONFLICT, "워케이션 지역과 예약 상품 지역이 일치하지 않습니다."),
    RESERVATION_CATEGORY_OVERLAP(HttpStatus.CONFLICT, "같은 카테고리의 예약 기간이 중복됩니다."),
    RESERVATION_INVENTORY_UNAVAILABLE(HttpStatus.CONFLICT, "선택한 기간의 예약 가능 재고가 부족합니다."),
    RESERVATION_ALREADY_CANCELED(HttpStatus.CONFLICT, "이미 취소된 예약입니다."),
    RESERVATION_CANCEL_NOT_ALLOWED(HttpStatus.CONFLICT, "취소할 수 없는 예약입니다."),
    RESERVATION_CANCEL_PERIOD_EXPIRED(HttpStatus.CONFLICT, "이용 시작일 당일부터는 예약을 취소할 수 없습니다."),
    RESERVATION_PAYMENT_NOT_FOUND(HttpStatus.CONFLICT, "취소 가능한 예약 결제 내역이 없습니다."),

//    500
    RESERVATION_PRODUCT_CONFIGURATION_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "예약 상품 설정이 올바르지 않습니다."),
    RESERVATION_CODE_EXHAUSTED(HttpStatus.INTERNAL_SERVER_ERROR, "예약 번호를 생성할 수 없습니다."),
    RESERVATION_REFUND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "예약 결제 환불에 실패했습니다."),
    RESERVATION_INVENTORY_RESTORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "예약 재고 복구에 실패했습니다."),
    RESERVATION_CANCEL_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "예약 취소 처리에 실패했습니다."),
    RESERVATION_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "예약 처리에 실패했습니다.");

    private final HttpStatus status;
    private final String message;
}
