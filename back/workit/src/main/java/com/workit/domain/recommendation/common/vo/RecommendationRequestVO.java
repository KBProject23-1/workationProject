package com.workit.domain.recommendation.vo;

import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.enums.ReferenceType;
import com.workit.domain.recommendation.enums.MealType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class RecommendationRequestVO {
    private Long id;
    private Long userId;
    private Long workationId;
    private RecommendationType recommendationType;
    private ReferenceType referenceType;
    private MealType mealType;
    private Long referenceMerchantId;
    private Long secondaryReferenceMerchantId;
    private BigDecimal referenceLatitude;
    private BigDecimal referenceLongitude;
    private String referenceMerchantName;
    private String secondaryReferenceMerchantName;
    private LocalDateTime createdAt;
}
