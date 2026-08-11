package com.workit.domain.recommendation.restaurant.dto.request;

import com.workit.domain.recommendation.restaurant.vo.MealType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class RecommendationRecalculateRequestDTO {
    private Long referenceMerchantId;
    private MealType mealType;
}
