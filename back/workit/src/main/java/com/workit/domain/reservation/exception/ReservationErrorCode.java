package com.workit.domain.reservation.exception;

import com.workit.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;


// 예약 도메인에서 사용하는 오류 코드
@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements ErrorCode {

//    404
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다.");

    private final HttpStatus status;
    private final String message;
}
