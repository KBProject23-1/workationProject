package com.workit.domain.recommendation.dto.response;

import com.workit.domain.recommendation.enums.MealType;
import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.enums.ReferenceType;
import com.workit.domain.recommendation.vo.RecommendationMerchantVO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RestaurantReferenceResponseDTO {
    private RecommendationType recommendationType;
    private MealType mealType;
    private ReferenceType referenceType;
    private Long merchantId;
    private Long secondaryMerchantId;
    private String merchantName;
    private String secondaryMerchantName;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public static RestaurantReferenceResponseDTO regionOnly(MealType mealType) {
        return new RestaurantReferenceResponseDTO(RecommendationType.RESTAURANT, mealType,
                ReferenceType.REGION_ONLY, null, null, null, null, null, null);
    }

    public static RestaurantReferenceResponseDTO merchant(MealType mealType, RecommendationMerchantVO merchant) {
        return new RestaurantReferenceResponseDTO(RecommendationType.RESTAURANT, mealType,
                ReferenceType.AUTO_MERCHANT, merchant.getMerchantId(), null, merchant.getMerchantName(),
                null, merchant.getLatitude(), merchant.getLongitude());
    }

    public static RestaurantReferenceResponseDTO midpoint(RecommendationMerchantVO stay,
                                                            RecommendationMerchantVO office,
                                                            BigDecimal latitude,
                                                            BigDecimal longitude) {
        return new RestaurantReferenceResponseDTO(RecommendationType.RESTAURANT, MealType.DINNER,
                ReferenceType.AUTO_MIDPOINT, stay.getMerchantId(), office.getMerchantId(), stay.getMerchantName(),
                office.getMerchantName(), latitude, longitude);
    }
}
