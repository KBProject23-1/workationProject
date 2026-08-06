package com.workit.domain.recommendation.offices.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OfficeRecommendationScoreResponseDTO {

    private final BigDecimal priceScore;
    private final BigDecimal preferenceScore;
    private final BigDecimal accessibilityScore;
    private final BigDecimal ratingScore;
    private final BigDecimal totalScore;
}

