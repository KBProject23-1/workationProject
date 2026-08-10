package com.workit.domain.recommendation.offices.service;

import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationResponseDTO;
import com.workit.domain.recommendation.common.dto.RecommendationListResponseDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationResultItemResponseDTO;

public interface OfficeRecommendationService {

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
}
