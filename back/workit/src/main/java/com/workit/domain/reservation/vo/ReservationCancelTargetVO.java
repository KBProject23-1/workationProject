package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// 예약 취소 검증과 환불 처리에 필요한 잠금 조회 정보
@Getter
@Setter
public class ReservationCancelTargetVO {

    private Long reservationId;
    private String reservationCode;
    private LocalDate startDate;
    private BigDecimal totalAmount;
    private ReservationStatus status;
    private Long paymentTransactionId;
}
