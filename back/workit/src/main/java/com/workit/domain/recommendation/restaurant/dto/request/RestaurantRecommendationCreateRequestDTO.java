package com.workit.domain.recommendation.restaurant.dto.request;

import com.workit.domain.recommendation.restaurant.vo.MealType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RestaurantRecommendationCreateRequestDTO {
    private MealType mealType;
}
