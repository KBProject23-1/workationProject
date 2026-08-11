package com.workit.domain.recommendation.accommodation.service;

import com.workit.domain.recommendation.accommodation.dto.request.RecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.accommodation.dto.response.AccommodationRecommendationResponseDTO;
import com.workit.domain.recommendation.accommodation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.accommodation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.common.dto.response.RecommendationListResponseDTO;

public interface AccommodationRecommendationService {
    RecommendationReferenceResponseDTO findAccommodationReferencePlace(Long userId);

    RecommendationCandidateListResponseDTO findAccommodationReferencePlaceCandidates(Long userId);

    RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item> addAccommodationRecommendation(Long userId);

    RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item> findAccommodationRecommendation(Long userId,
                                                                         Long referenceMerchantId,
                                                                         String cursor,
                                                                         int size);

    RecommendationListResponseDTO<AccommodationRecommendationResponseDTO.Item> recalculateAccommodation(Long userId,
                                                                  Long recommendationRequestId,
                                                                  RecommendationRecalculateRequestDTO request);
}
