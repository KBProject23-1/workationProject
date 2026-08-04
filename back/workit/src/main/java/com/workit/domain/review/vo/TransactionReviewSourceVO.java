package com.workit.domain.review.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// 결제 기반 리뷰의 소유자·가맹점·작성 기한 검증 정보
@Getter
@Setter
@ToString
public class TransactionReviewSourceVO {

    private Long transactionId;
    private Long userId;
    private Long merchantId;
    private String merchantCategory;
    private String transactionType;
    private String transactionStatus;
    private LocalDateTime approvedAt;
    private Long reviewId;
}
