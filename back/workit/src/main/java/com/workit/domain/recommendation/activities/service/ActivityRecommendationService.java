package com.workit.domain.recommendation.activities.service;

import com.workit.domain.recommendation.activities.dto.request.ActivityRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.activities.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.activities.dto.response.ActivityRecommendationResponseDTO;
import com.workit.domain.recommendation.activities.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.activities.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.common.dto.response.RecommendationListResponseDTO;

public interface ActivityRecommendationService {
    RecommendationReferenceResponseDTO findActivityReferencePlace(Long userId);

    RecommendationCandidateListResponseDTO findActivityReferencePlaceCandidates(Long userId);

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
