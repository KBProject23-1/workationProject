package com.workit.domain.recommendation.accommodation.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AccommodationConditionVO {
    private Long workationId;
    private Long regionId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal accommodationBudget;
    private String priorityOptionCode;
    private String priorityOptionName;
}
