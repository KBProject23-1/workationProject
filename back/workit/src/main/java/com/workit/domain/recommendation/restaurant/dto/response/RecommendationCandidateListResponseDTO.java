package com.workit.domain.recommendation.restaurant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecommendationCandidateListResponseDTO {
    private List<RecommendationCandidateResponseDTO> content;
}
