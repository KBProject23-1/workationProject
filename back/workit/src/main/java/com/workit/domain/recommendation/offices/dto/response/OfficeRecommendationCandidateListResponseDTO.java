package com.workit.domain.recommendation.offices.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OfficeRecommendationCandidateListResponseDTO {
    private List<OfficeRecommendationCandidateResponseDTO> content;
}
