package com.workit.domain.recommendation.activities.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecommendationCandidateListResponseDTO {
    private List<RecommendationCandidateResponseDTO> content;
}
