package com.workit.domain.recommendation.offices.dto.response;

import com.workit.domain.recommendation.offices.vo.OfficeRecommendationRequestVO;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OfficeRecommendationResponseDTO {

    private final Long recommendationRequestId;
    private final String recommendationType;
    private final OfficeRecommendationReferenceResponseDTO reference;
    private final List<OfficeRecommendationResultItemResponseDTO> content;
    private final OfficeRecommendationPageInfoDTO pageInfo;
    private final LocalDateTime createdAt;

    public static OfficeRecommendationResponseDTO of(OfficeRecommendationRequestVO requestVO,
													 List<OfficeRecommendationResultItemResponseDTO> content,
													 OfficeRecommendationPageInfoDTO pageInfo,
													 String recommendationType,
													 OfficeRecommendationReferenceResponseDTO reference) {
        return OfficeRecommendationResponseDTO.builder()
                .recommendationRequestId(requestVO == null ? null : requestVO.getId())
                .recommendationType(recommendationType == null ? null : recommendationType)
                .reference(reference)
                .content(content)
                .pageInfo(pageInfo)
                .createdAt(requestVO == null ? null : requestVO.getCreatedAt())
                .build();
    }
}

