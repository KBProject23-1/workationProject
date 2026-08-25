package com.workit.domain.recommendation.activities.dto.response;

import com.workit.domain.recommendation.activities.vo.ActivityCandidateVO;
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

    public static RecommendationCandidateResponseDTO from(ActivityCandidateVO candidate) {
        if (candidate == null) {
            return new RecommendationCandidateResponseDTO(null, null, null, null, null, false);
        }
        return new RecommendationCandidateResponseDTO(
                candidate.getMerchantId(),
                candidate.getMerchantName(),
                candidate.getCategory(),
                candidate.getLatitude(),
                candidate.getLongitude(),
                candidate.isReserved()
        );
    }
}
