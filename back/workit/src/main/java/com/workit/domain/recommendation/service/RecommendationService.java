package com.workit.domain.recommendation.service;

import com.workit.domain.recommendation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.dto.response.AccommodationRecommendationResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.enums.MealType;
import com.workit.domain.recommendation.dto.request.RestaurantRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.dto.response.RestaurantRecommendationResponseDTO;
import com.workit.domain.recommendation.dto.response.RestaurantReferenceResponseDTO;

public interface RecommendationService {
    AccommodationRecommendationResponseDTO addAccommodationRecommendation(Long userId);

    AccommodationRecommendationResponseDTO findAccommodationRecommendation(Long userId,
                                                                             Long referenceMerchantId,
                                                                             String cursor,
                                                                             int size);

    RecommendationReferenceResponseDTO findReferencePlace(Long userId, RecommendationType recommendationType);

    RecommendationCandidateListResponseDTO findReferencePlaceCandidates(Long userId);

    AccommodationRecommendationResponseDTO recalculateAccommodation(Long userId, Long recommendationRequestId,
                                                                     RecommendationRecalculateRequestDTO request);

    RestaurantRecommendationResponseDTO addRestaurantRecommendation(Long userId,
                                                                     RestaurantRecommendationCreateRequestDTO request);

    RestaurantRecommendationResponseDTO findRestaurantRecommendation(Long userId, Long referenceMerchantId,
                                                                      MealType mealType, String cursor, int size);

    RestaurantReferenceResponseDTO findRestaurantReferencePlace(Long userId, MealType mealType);

    RecommendationCandidateListResponseDTO findRestaurantReferencePlaceCandidates(Long userId);

    Object recalculateRecommendation(Long userId, Long recommendationRequestId,
                                     RecommendationRecalculateRequestDTO request);
}
