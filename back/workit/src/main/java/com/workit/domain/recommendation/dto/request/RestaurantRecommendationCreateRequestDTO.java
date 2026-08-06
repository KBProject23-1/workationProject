package com.workit.domain.recommendation.dto.request;

import com.workit.domain.recommendation.enums.MealType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RestaurantRecommendationCreateRequestDTO {
    private MealType mealType;
}
