package com.workit.domain.recommendation.service;

import com.workit.domain.recommendation.dto.response.RecommendationCandidateListResponseDTO;
import com.workit.domain.recommendation.dto.response.RecommendationReferenceResponseDTO;
import com.workit.domain.recommendation.enums.RecommendationType;

public interface RecommendationService {
    RecommendationReferenceResponseDTO findReferencePlace(Long userId, RecommendationType recommendationType);

    RecommendationCandidateListResponseDTO findReferencePlaceCandidates(Long userId);
}
