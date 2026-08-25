package com.workit.domain.recommendation.offices.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OfficeRecommendationResultItemResponseDTO {

    private final Long recommendationResultId;
    private final Integer ranking;
    private final Long merchantId;
    private final String merchantName;
    private final String thumbnailUrl;
    private final String address;
    private final Long price;
    private final BigDecimal rating;
    private final Long reviewCount;
    private final String noiseLevel;
    private final Integer distanceMeters;
    private final List<String> atmosphereTags;
    private final OfficeRecommendationScoreResponseDTO score;
    private final String recommendationReason;
    private final Boolean bookmarked;
    private final LocalDateTime calculatedAt;
}
