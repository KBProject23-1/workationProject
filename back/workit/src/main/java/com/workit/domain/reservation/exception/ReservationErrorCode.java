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

//    404
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다."),
    RESERVATION_CANCELLATION_NOT_FOUND(HttpStatus.NOT_FOUND, "예약 취소 내역이 존재하지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
