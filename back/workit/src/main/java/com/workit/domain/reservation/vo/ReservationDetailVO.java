package com.workit.domain.reservation.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 예약·가맹점·상품·결제·리뷰를 조인한 상세 조회 결과 VO
 */
@Getter
@Setter
@ToString
public class ReservationDetailVO {

    private Long reservationId;
    private String reservationCode;
    private Long workationId;
    private Long merchantId;
    private String merchantName;
    private String merchantCategory;
    private String merchantAddress;
    private String merchantPhoneNumber;
    private String merchantThumbnailUrl;
    private Long productId;
    private String productName;
    private ReservationProductDetailType productDetailType;
    private String productThumbnailUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer headcount;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private Long reviewId;
    private String reviewStatus;
}
