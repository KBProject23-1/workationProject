package com.workit.domain.recommendation.activities.service;

import com.workit.domain.recommendation.activities.dto.request.ActivityRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.activities.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.activities.dto.response.ActivityRecommendationResponseDTO;
import com.workit.domain.recommendation.common.dto.RecommendationListResponseDTO;

public interface ActivityRecommendationService {
    RecommendationListResponseDTO<ActivityRecommendationResponseDTO.Item> addActivityRecommendation(
            Long userId,
            ActivityRecommendationCreateRequestDTO request);

    RecommendationListResponseDTO<ActivityRecommendationResponseDTO.Item> findActivityRecommendation(
            Long userId,
            Long referenceMerchantId,
            String cursor,
            int size);

    RecommendationListResponseDTO<ActivityRecommendationResponseDTO.Item> recalculateActivityRecommendation(
            Long userId,
            Long recommendationRequestId,
            RecommendationRecalculateRequestDTO request);
}
