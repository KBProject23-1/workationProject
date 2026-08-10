package com.workit.domain.recommendation.accommodation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecommendationCandidateListResponseDTO {
    private List<RecommendationCandidateResponseDTO> content;
}
