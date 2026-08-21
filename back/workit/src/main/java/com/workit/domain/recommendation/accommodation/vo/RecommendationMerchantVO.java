package com.workit.domain.recommendation.accommodation.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class RecommendationMerchantVO {
    private Long merchantId;
    private String merchantName;
    private String category;
    private String address;
    private String thumbnailUrl;
    private BigDecimal price;
    private BigDecimal rating;
    private Long reviewCount;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean reserved;
}
