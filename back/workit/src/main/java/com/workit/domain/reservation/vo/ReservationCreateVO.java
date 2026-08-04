package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

// reservations 테이블에 저장할 확정 예약 정보
@Getter
@Setter
public class ReservationCreateVO {

    private Long id;
    private Long userId;
    private Long workationId;
    private Long productId;
    private String reservationCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer headcount;
    private Integer quantity;
    private BigDecimal totalAmount;
    private ReservationStatus status;
}
