package com.workit.domain.recommendation.activities.dto.response;

import com.workit.domain.recommendation.activities.vo.ReferenceType;
import com.workit.domain.recommendation.activities.vo.RecommendationMerchantVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RecommendationReferenceResponseDTO {
    private String recommendationType;
    private String referenceType;
    private Long merchantId;
    private Long secondaryMerchantId;
    private String merchantName;
    private String secondaryMerchantName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String description;

    public static RecommendationReferenceResponseDTO regionOnly() {
        return new RecommendationReferenceResponseDTO("ACTIVITY", ReferenceType.REGION_ONLY.name(),
                null, null, null, null, null, null,
                "추천 기준 장소가 없어 워케이션 지역을 기준으로 추천했어요.");
    }

    public static RecommendationReferenceResponseDTO auto(RecommendationMerchantVO merchant) {
        if (merchant == null) {
            return regionOnly();
        }
        return new RecommendationReferenceResponseDTO("ACTIVITY", ReferenceType.AUTO_MERCHANT.name(),
                merchant.getMerchantId(), null, merchant.getMerchantName(), null,
                merchant.getLatitude(), merchant.getLongitude(),
                "확정된 숙소를 기준으로 추천했어요.");
    }

    public static RecommendationReferenceResponseDTO userSelected(RecommendationMerchantVO merchant,
                                                                  BigDecimal latitude, BigDecimal longitude) {
        if (merchant == null) {
            return regionOnly();
        }
        BigDecimal safeLatitude = latitude == null ? merchant.getLatitude() : latitude;
        BigDecimal safeLongitude = longitude == null ? merchant.getLongitude() : longitude;
        return new RecommendationReferenceResponseDTO("ACTIVITY", ReferenceType.USER_SELECTED.name(),
                merchant.getMerchantId(), null, merchant.getMerchantName(), null,
                safeLatitude, safeLongitude, "선택된 장소를 기준으로 추천했어요.");
    }
}
