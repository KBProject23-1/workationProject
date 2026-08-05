package com.workit.domain.recommendation.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RecommendationResultVO extends RecommendationMerchantVO {
    private Long recommendationResultId;
    private Long recommendationRequestId;
    private BigDecimal priceScore;
    private BigDecimal accessibilityScore;
    private BigDecimal ratingScore;
    private BigDecimal totalScore;
    private Integer ranking;
    private BigDecimal distance;
    private boolean bookmarked;
}
