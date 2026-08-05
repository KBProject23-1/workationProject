package com.workit.domain.recommendation.vo;

import com.workit.domain.recommendation.enums.RecommendationType;
import com.workit.domain.recommendation.enums.ReferenceType;
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
    private Long referenceMerchantId;
    private BigDecimal referenceLatitude;
    private BigDecimal referenceLongitude;
    private String referenceMerchantName;
    private LocalDateTime createdAt;
}
