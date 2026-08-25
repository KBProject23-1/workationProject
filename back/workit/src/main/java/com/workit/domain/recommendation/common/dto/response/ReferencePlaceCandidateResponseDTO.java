package com.workit.domain.recommendation.common.dto.response;

import com.workit.domain.recommendation.common.vo.ReferencePlaceCandidateVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ReferencePlaceCandidateResponseDTO {
    private Long merchantId;
    private String merchantName;
    private String category;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean reserved;

    public static ReferencePlaceCandidateResponseDTO from(ReferencePlaceCandidateVO candidate) {
        if (candidate == null) {
            return new ReferencePlaceCandidateResponseDTO(null, null, null, null, null, false);
        }
        return new ReferencePlaceCandidateResponseDTO(
                candidate.getMerchantId(),
                candidate.getMerchantName(),
                candidate.getCategory(),
                candidate.getLatitude(),
                candidate.getLongitude(),
                candidate.isReserved()
        );
    }
}

