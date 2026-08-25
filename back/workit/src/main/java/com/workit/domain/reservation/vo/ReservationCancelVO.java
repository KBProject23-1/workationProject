package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 예약 취소와 환불 완료 이력 저장 정보
@Getter
@Setter
public class ReservationCancelVO {

    private Long reservationId;
    private BigDecimal cancelFee;
    private BigDecimal refundAmount;
    private LocalDateTime canceledAt;
    private LocalDateTime refundedAt;
}
