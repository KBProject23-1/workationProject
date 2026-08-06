package com.workit.domain.recommendation.offices.service;

import com.workit.domain.recommendation.offices.dto.request.OfficeRecommendationCreateRequestDTO;
import com.workit.domain.recommendation.offices.dto.response.OfficeRecommendationResponseDTO;

public interface OfficeRecommendationService {

    void createOfficeRecommendation(Long userId, OfficeRecommendationCreateRequestDTO request);

    OfficeRecommendationResponseDTO getOfficeRecommendation(Long userId, Long referenceMerchantId, String cursor, Integer size);
}
