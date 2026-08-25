package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약·상품·가맹점·취소 내역을 조인한 목록 조회 결과 VO
 */
@Getter
@Setter
@ToString
public class ReservationListItemVO {

    private Long reservationId;
    private String reservationCode;
    private String merchantName;
    private String productName;
    private ReservationProductDetailType productDetailType;
    private String thumbnailUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer headcount;
    private Integer quantity;
    private BigDecimal totalAmount;
    private ReservationStatus status;
    private LocalDateTime canceledAt;
    private BigDecimal refundAmount;
}
