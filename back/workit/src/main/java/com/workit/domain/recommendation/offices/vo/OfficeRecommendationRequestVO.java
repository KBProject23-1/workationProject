package com.workit.domain.recommendation.offices.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class OfficeRecommendationRequestVO {

    private Long id;
    private Long userId;
    private Long workationId;
    private String recommendationType;
    private String referenceType;
    private String mealType;
    private Long referenceMerchantId;
    private String referenceMerchantName;
    private Long secondaryReferenceMerchantId;
    private String secondaryReferenceMerchantName;
    private BigDecimal referenceLatitude;
    private BigDecimal referenceLongitude;
    private LocalDateTime createdAt;
}
