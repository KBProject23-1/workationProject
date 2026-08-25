package com.workit.domain.review.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ReviewVO {

    private Long reviewId;
    private Long userId;
    private Long reservationId;
    private Long merchantId;
    private Long transactionId;
    private Integer rating;
    private String content;
    private ReviewAtmosphere atmosphere;
    private String imageUrl;
}
