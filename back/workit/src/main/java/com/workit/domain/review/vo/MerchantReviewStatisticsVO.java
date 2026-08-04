package com.workit.domain.review.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

// 가맹점의 활성 리뷰 평점 통계 조회 결과
@Getter
@Setter
@ToString
public class MerchantReviewStatisticsVO {

    private BigDecimal averageRating;
    private Long reviewCount;
    private Long oneStarCount;
    private Long twoStarCount;
    private Long threeStarCount;
    private Long fourStarCount;
    private Long fiveStarCount;
}
