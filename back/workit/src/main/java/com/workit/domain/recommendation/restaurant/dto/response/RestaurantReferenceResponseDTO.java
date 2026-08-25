package com.workit.domain.recommendation.restaurant.dto.response;

import com.workit.domain.recommendation.restaurant.vo.MealType;
import com.workit.domain.recommendation.restaurant.vo.ReferenceType;
import com.workit.domain.recommendation.restaurant.vo.RecommendationType;
import com.workit.domain.recommendation.restaurant.vo.RecommendationMerchantVO;
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

    public static RestaurantReferenceResponseDTO userSelected(MealType mealType, RecommendationMerchantVO merchant,
                                                             BigDecimal latitude, BigDecimal longitude) {
        BigDecimal safeLatitude = latitude == null ? merchant.getLatitude() : latitude;
        BigDecimal safeLongitude = longitude == null ? merchant.getLongitude() : longitude;
        return new RestaurantReferenceResponseDTO(RecommendationType.RESTAURANT, mealType,
                ReferenceType.USER_SELECTED, merchant.getMerchantId(), null, merchant.getMerchantName(),
                null, safeLatitude, safeLongitude);
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
