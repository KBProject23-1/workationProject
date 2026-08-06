package com.workit.domain.recommendation.offices.dto.response;

import com.workit.domain.recommendation.offices.vo.OfficeRecommendationRequestVO;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class OfficeRecommendationReferenceResponseDTO {

    private final String referenceType;
    private final Long primaryMerchantId;
    private final String primaryMerchantName;
    private final Long secondaryMerchantId;
    private final String secondaryMerchantName;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String description;

    public static OfficeRecommendationReferenceResponseDTO from(OfficeRecommendationRequestVO requestVO, String description) {
        if (requestVO == null) {
            return null;
        }

        return OfficeRecommendationReferenceResponseDTO.builder()
                .referenceType(requestVO.getReferenceType())
                .primaryMerchantId(requestVO.getReferenceMerchantId())
                .primaryMerchantName(requestVO.getReferenceMerchantName())
                .secondaryMerchantId(requestVO.getSecondaryReferenceMerchantId())
                .secondaryMerchantName(requestVO.getSecondaryReferenceMerchantName())
                .latitude(requestVO.getReferenceLatitude())
                .longitude(requestVO.getReferenceLongitude())
                .description(description)
                .build();
    }
}
