package com.workit.domain.recommendation.restaurant.service;

import com.workit.domain.recommendation.restaurant.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.restaurant.dto.request.RestaurantRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RestaurantReferenceResponseDTO;
import com.workit.domain.recommendation.common.dto.response.RecommendationListResponseDTO;
import com.workit.domain.recommendation.restaurant.dto.response.RestaurantRecommendationResponseDTO;
import com.workit.domain.recommendation.restaurant.vo.MealType;

public interface RestaurantRecommendationService {
    RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item> addRestaurantRecommendation(Long userId, RestaurantRecommendationCreateRequestDTO request);

    RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item> findRestaurantRecommendation(Long userId,
                                                                    Long referenceMerchantId,
                                                                    MealType mealType,
                                                                    String cursor,
                                                                    int size);

    RestaurantReferenceResponseDTO findRestaurantReferencePlace(Long userId, MealType mealType);

    RecommendationCandidateListResponseDTO findRestaurantReferencePlaceCandidates(Long userId);

    RecommendationListResponseDTO<RestaurantRecommendationResponseDTO.Item> recalculateRestaurant(Long userId,
                                                              Long recommendationRequestId,
                                                              RecommendationRecalculateRequestDTO request);
}
