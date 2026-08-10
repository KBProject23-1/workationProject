package com.workit.domain.recommendation.dto.response;

import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.enums.ReferenceType;
import com.workit.domain.recommendation.vo.RecommendationMerchantVO;
import com.workit.domain.recommendation.vo.RecommendationRequestVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RecommendationReferenceResponseDTO {
    private RecommendationType recommendationType;
    private ReferenceType referenceType;
    private Long merchantId;
    private String merchantName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String description;

    public static RecommendationReferenceResponseDTO from(RecommendationRequestVO request) {
        String description = request.getReferenceType() == ReferenceType.REGION_ONLY
                ? "확정된 공유오피스가 없어 지역을 기준으로 추천했어요."
                : "선택된 공유오피스를 기준으로 추천했어요.";
        return new RecommendationReferenceResponseDTO(request.getRecommendationType(), request.getReferenceType(),
                request.getReferenceMerchantId(), request.getReferenceMerchantName(),
                request.getReferenceLatitude(), request.getReferenceLongitude(), description);
    }

    public static RecommendationReferenceResponseDTO automatic(RecommendationMerchantVO merchant) {
        if (merchant == null) {
            return new RecommendationReferenceResponseDTO(RecommendationType.ACCOMMODATION,
                    ReferenceType.REGION_ONLY, null, null, null, null, "추천 기준 장소가 없습니다.");
        }
        return new RecommendationReferenceResponseDTO(RecommendationType.ACCOMMODATION,
                ReferenceType.AUTO_MERCHANT, merchant.getMerchantId(), merchant.getMerchantName(),
                merchant.getLatitude(), merchant.getLongitude(), "확정 예약된 공유오피스입니다.");
    }
}
