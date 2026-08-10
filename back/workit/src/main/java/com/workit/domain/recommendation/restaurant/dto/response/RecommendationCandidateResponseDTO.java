package com.workit.domain.recommendation.restaurant.dto.response;

import com.workit.domain.recommendation.restaurant.vo.RecommendationMerchantVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RecommendationCandidateResponseDTO {
    private Long merchantId;
    private String merchantName;
    private String category;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean reserved;

    public static RecommendationCandidateResponseDTO from(RecommendationMerchantVO merchant) {
        return new RecommendationCandidateResponseDTO(
                merchant.getMerchantId(),
                merchant.getMerchantName(),
                merchant.getCategory(),
                merchant.getLatitude(),
                merchant.getLongitude(),
                merchant.isReserved()
        );
    }
}
