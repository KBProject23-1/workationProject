package com.workit.domain.recommendation.activities.service;

import com.workit.domain.recommendation.activities.dto.request.ActivityRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.activities.dto.response.ActivityRecommendationResponseDTO;
import com.workit.domain.recommendation.dto.request.RecommendationRecalculateRequestDTO;

public interface ActivityRecommendationService {
    ActivityRecommendationResponseDTO addActivityRecommendation(Long userId,
                                                               ActivityRecommendationCreateRequestDTO request);

    ActivityRecommendationResponseDTO findActivityRecommendation(Long userId,
                                                                Long referenceMerchantId,
                                                                String cursor,
                                                                int size);

    ActivityRecommendationResponseDTO recalculateActivityRecommendation(Long userId,
                                                                       Long recommendationRequestId,
                                                                       RecommendationRecalculateRequestDTO request);
}
