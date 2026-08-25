package com.workit.domain.recommendation.offices.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class OfficeRecommendationResultVO {

    private Long recommendationResultId;
    private Long recommendationRequestId;
    private Long merchantId;
    private String name;
    private String address;
    private String thumbnailUrl;
    private Long price;
    private BigDecimal rating;
    private String noiseLevel;
    private Long reviewCount;
    private Long quietReviewCount;
    private Long openReviewCount;
    private Long collabReviewCount;
    private Boolean bookmarked;

    private Integer ranking;

    private BigDecimal priceScore;
    private BigDecimal preferenceScore;
    private BigDecimal accessibilityScore;
    private BigDecimal ratingScore;
    private BigDecimal totalScore;
    private LocalDateTime calculatedAt;

    private Double latitude;
    private Double longitude;
    private Integer distanceMeters;
}
