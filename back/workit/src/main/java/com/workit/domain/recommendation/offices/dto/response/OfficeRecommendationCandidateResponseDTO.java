package com.workit.domain.recommendation.offices.dto.response;

import com.workit.domain.recommendation.offices.vo.OfficeCandidateVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OfficeRecommendationCandidateResponseDTO {
    private Long merchantId;
    private String merchantName;
    private String category;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean reserved;

    public static OfficeRecommendationCandidateResponseDTO from(OfficeCandidateVO officeCandidate) {
        if (officeCandidate == null) {
            return new OfficeRecommendationCandidateResponseDTO(null, null, null, null, null, false);
        }
        return new OfficeRecommendationCandidateResponseDTO(
                officeCandidate.getMerchantId(),
                officeCandidate.getName(),
                officeCandidate.getCategory(),
                officeCandidate.getLatitude(),
                officeCandidate.getLongitude(),
                officeCandidate.getCategory() != null
        );
    }
}
