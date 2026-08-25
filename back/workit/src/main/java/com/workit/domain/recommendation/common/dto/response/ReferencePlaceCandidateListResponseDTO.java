package com.workit.domain.recommendation.common.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ReferencePlaceCandidateListResponseDTO {
    private List<ReferencePlaceCandidateResponseDTO> content;
}

