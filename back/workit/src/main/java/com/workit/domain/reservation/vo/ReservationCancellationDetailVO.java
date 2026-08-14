package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약·가맹점·상품·취소 내역을 조인한 취소 상세 조회 결과 VO
 */
@Getter
@Setter
@ToString
public class ReservationCancellationDetailVO {

    private Long reservationId;
    private String reservationCode;
    private Long merchantId;
    private String merchantCategory;
    private String merchantName;
    private String productName;
    private ReservationProductDetailType productDetailType;
    private String thumbnailUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalAmount;
    private LocalDateTime canceledAt;
    private BigDecimal cancelFee;
    private BigDecimal refundAmount;
}
