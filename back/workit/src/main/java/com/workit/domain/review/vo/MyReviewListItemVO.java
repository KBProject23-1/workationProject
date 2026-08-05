package com.workit.domain.review.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// 로그인 사용자가 작성한 리뷰 목록 조회 결과
@Getter
@Setter
@ToString
public class MyReviewListItemVO {

    private Long reviewId;
    private Long merchantId;
    private String merchantName;
    private String merchantCategory;
    private String merchantThumbnailUrl;
    private Integer rating;
    private String content;
    private String atmosphere;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long reservationId;
    private Long transactionId;
}
