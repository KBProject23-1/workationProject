package com.workit.domain.recommendation.offices.service;

import com.workit.domain.recommendation.common.dto.response.RecommendationListResponseDTO;
import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationRecalculateRequestDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationResultItemResponseDTO;

public interface OfficeRecommendationService {

    OfficeRecommendationReferenceResponseDTO findOfficeReferencePlace(Long userId);

    OfficeRecommendationCandidateListResponseDTO findOfficeReferencePlaceCandidates(Long userId);

    RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> createOfficeRecommendation(
            Long userId,
            OfficeRecommendationCreateRequestDTO request);

    RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> getOfficeRecommendation(
            Long userId,
            Long referenceMerchantId,
            String cursor,
            Integer size);

    RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> getOfficeRecommendationByRequestId(
            Long userId,
            Long recommendationRequestId,
            String cursor,
            Integer size);

    RecommendationListResponseDTO<OfficeRecommendationResultItemResponseDTO> recalculateOfficeRecommendation(
            Long userId,
            Long recommendationRequestId,
            OfficeRecommendationRecalculateRequestDTO request);
}
