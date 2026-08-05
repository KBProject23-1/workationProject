package com.workit.domain.recommendation.service;

import com.workit.domain.recommendation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.dto.response.AccommodationRecommendationResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.enums.RecommendationType;

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
}
