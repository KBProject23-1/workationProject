package com.workit.domain.review.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 리뷰와 작성자, 가맹점, 작성 근거를 조인한 상세 조회 결과
@Getter
@Setter
@ToString
public class ReviewDetailVO {

    private Long reviewId;
    private Long userId;
    private String nickname;
    private Integer rating;
    private String content;
    private String atmosphere;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long merchantId;
    private String merchantName;
    private String merchantCategory;
    private String merchantAddress;
    private String merchantThumbnailUrl;
    private Long reservationId;
    private String reservationCode;
    private LocalDate reservationStartDate;
    private LocalDate reservationEndDate;
    private Long transactionId;
    private String transactionNumber;
    private BigDecimal transactionAmount;
    private LocalDateTime transactionApprovedAt;
}
